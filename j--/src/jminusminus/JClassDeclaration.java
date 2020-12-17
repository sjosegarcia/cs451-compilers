// Copyright 2012- Bill Campbell, Swami Iyer and Bahar Akbal-Delibas

package jminusminus;

import java.util.ArrayList;

import static jminusminus.CLConstants.*;

/**
 * A representation of a class declaration.
 */
class JClassDeclaration extends JAST implements JTypeDecl {
    // Class modifiers.
    private ArrayList<String> mods;

    // Class name.
    private String name;

    // Class block.
    private ArrayList<JMember> classBlock;

    // This class type.
    private Type thisType;

    // Super class type.
    private Type superType;

    // Context for this class.
    private ClassContext context;

    // Whether this class has an explicit constructor.
    private boolean hasExplicitConstructor;

    // Instance fields of this class.
    private ArrayList<JFieldDeclaration> instanceFieldInitializations;

    // Static (class) fields of this class.
    private ArrayList<JFieldDeclaration> staticFieldInitializations;

    // List of objects the class extends.
    private ArrayList<Type> impl;

    private ArrayList<String> implNames;

    /**
     * Constructs an AST node for a class declaration.
     *
     * @param line       line in which the class declaration occurs in the source file.
     * @param mods       class modifiers.
     * @param name       class name.
     * @param superType  super class type.
     * @param classBlock class block.
     * @param impl list of objects that the class extends
     */
    public JClassDeclaration(int line, ArrayList<String> mods, String name, Type superType,
                             ArrayList<JMember> classBlock, ArrayList<Type> impl) {
        super(line);
        this.mods = mods;
        this.name = name;
        this.superType = superType;
        this.classBlock = classBlock;
        this.impl = impl;
        hasExplicitConstructor = false;
        instanceFieldInitializations = new ArrayList<JFieldDeclaration>();
        staticFieldInitializations = new ArrayList<JFieldDeclaration>();
    }

    /**
     * Returns the initializations for instance fields (expressed as assignment statements).
     *
     * @return the initializations for instance fields (expressed as assignment statements).
     */
    public ArrayList<JFieldDeclaration> instanceFieldInitializations() {
        return instanceFieldInitializations;
    }

    /**
     * {@inheritDoc}
     */
    public void declareThisType(Context context) {
        String qualifiedName = JAST.compilationUnit.packageName() == "" ?
                name : JAST.compilationUnit.packageName() + "/" + name;
        CLEmitter partial = new CLEmitter(false);
        if (impl != null) {
            implNames = new ArrayList<String>();
            for (Type implType : impl) {
                implNames.add(implType.jvmName());
            }
        }
        partial.addClass(mods, qualifiedName, Type.OBJECT.jvmName(), implNames, false);
        thisType = Type.typeFor(partial.toClass());
        context.addType(line, thisType);
    }

    /**
     * {@inheritDoc}
     */
    public void preAnalyze(Context context) {
        // Construct a class context.
        this.context = new ClassContext(this, context);
        if (impl != null) {
            for (int i = 0; i < impl.size(); i++) {
                impl.set(i, (Type) impl.get(i).resolve(this.context));
            }
        }
        // Resolve superclass.
        superType = superType.resolve(this.context);

        // Creating a partial class in memory can result in a java.lang.VerifyError if the
        // semantics below are violated, so we can't defer these checks to analyze().
        thisType.checkAccess(line, superType);
        if (superType.isFinal()) {
            JAST.compilationUnit.reportSemanticError(line, "Cannot extend a final type: %s",
                    superType.toString());
        }

        // Create the (partial) class.
        CLEmitter partial = new CLEmitter(false);

        // Add the class header to the partial class
        String qualifiedName = JAST.compilationUnit.packageName() == "" ?
                name : JAST.compilationUnit.packageName() + "/" + name;
        partial.addClass(mods, qualifiedName, superType.jvmName(), implNames, false);

        // Pre-analyze the members and add them to the partial class.
        for (JMember member : classBlock) {
            member.preAnalyze(this.context, partial);
            hasExplicitConstructor =
                    hasExplicitConstructor || member instanceof JConstructorDeclaration;
        }

        // Add the implicit empty constructor?
        if (!hasExplicitConstructor) {
            codegenPartialImplicitConstructor(partial);
        }

        // Get the ClassRep for the (partial) class and make it the representation for this type.
        Type id = this.context.lookupType(name);
        if (id != null && !JAST.compilationUnit.errorHasOccurred()) {
            id.setClassRep(partial.toClass());
        }
    }

    /**
     * {@inheritDoc}
     */
    public String name() {
        return name;
    }

    /**
     * {@inheritDoc}
     */
    public Type thisType() {
        return thisType;
    }

    /**
     * {@inheritDoc}
     */
    public Type superType() {
        return superType;
    }

    /**
     * {@inheritDoc}
     */
    public JAST analyze(Context context) {
        // Analyze all members
        for (JMember member : classBlock) {
            ((JAST) member).analyze(this.context);
        }

        // Separate declared fields for purposes of initialization.
        for (JMember member : classBlock) {
            if (member instanceof JFieldDeclaration) {
                JFieldDeclaration fieldDecl = (JFieldDeclaration) member;
                if (fieldDecl.mods().contains("static")) {
                    staticFieldInitializations.add(fieldDecl);
                } else {
                    instanceFieldInitializations.add(fieldDecl);
                }
            }
        }

        // Finally, ensure that a non-abstract class has no abstract methods.
        if (!thisType.isAbstract() && thisType.abstractMethods().size() > 0) {
            String methods = "";
            for (Method method : thisType.abstractMethods()) {
                methods += "\n" + method;
            }
            JAST.compilationUnit.reportSemanticError(line,
                    "Class must be abstract since it defines abstract methods: %s", methods);
        }
        return this;
    }

    /**
     * {@inheritDoc}
     */
    public void codegen(CLEmitter output) {
        // The class header.
        String qualifiedName = JAST.compilationUnit.packageName() == "" ?
                name : JAST.compilationUnit.packageName() + "/" + name;
        output.addClass(mods, qualifiedName, superType.jvmName(), implNames, false);

        // The implicit empty constructor?
        if (!hasExplicitConstructor) {
            codegenImplicitConstructor(output);
        }

        // The members.
        for (JMember member : classBlock) {
            ((JAST) member).codegen(output);
        }

        // Generate a class initialization method?
        if (staticFieldInitializations.size() > 0) {
            codegenClassInit(output);
        }
    }

    /**
     * {@inheritDoc}
     */
    public void toJSON(JSONElement json) {
        JSONElement e = new JSONElement();
        json.addChild("JClassDeclaration:" + line, e);
        if (mods != null) {
            ArrayList<String> value = new ArrayList<String>();
            for (String mod : mods) {
                value.add(String.format("\"%s\"", mod));
            }
            e.addAttribute("modifiers", value);
        }
        e.addAttribute("name", name);
        e.addAttribute("super", superType == null ? "" : superType.toString());
        if (impl != null) {
            ArrayList<String> value = new ArrayList<String>();
            for (Type imp : impl) {
                value.add(String.format("\"%s\"", imp.jvmName()));
            }
            e.addAttribute("implements", value);
        }
        if (context != null) {
            context.toJSON(e);
        }
        if (classBlock != null) {
            for (JMember member : classBlock) {
                ((JAST) member).toJSON(e);
            }
        }
    }

    // Generates code for an implicit empty constructor (necessary only if there is not already
    // an explicit one).
    private void codegenPartialImplicitConstructor(CLEmitter partial) {
        ArrayList<String> mods = new ArrayList<String>();
        mods.add("public");
        partial.addMethod(mods, "<init>", "()V", null, false);
        partial.addNoArgInstruction(ALOAD_0);
        partial.addMemberAccessInstruction(INVOKESPECIAL, superType.jvmName(), "<init>", "()V");
        partial.addNoArgInstruction(RETURN);
    }

    // Generates code for an implicit empty constructor (necessary only if there is not already
    // an explicit one).
    private void codegenImplicitConstructor(CLEmitter output) {
        ArrayList<String> mods = new ArrayList<String>();
        mods.add("public");
        output.addMethod(mods, "<init>", "()V", null, false);
        output.addNoArgInstruction(ALOAD_0);
        output.addMemberAccessInstruction(INVOKESPECIAL, superType.jvmName(), "<init>", "()V");

        // If there are instance field initializations, generate code for them.
        for (JFieldDeclaration instanceField : instanceFieldInitializations) {
            instanceField.codegenInitializations(output);
        }

        output.addNoArgInstruction(RETURN);
    }

    // Generates code for class initialization (in j-- this means static field initializations.
    private void codegenClassInit(CLEmitter output) {
        ArrayList<String> mods = new ArrayList<String>();
        mods.add("public");
        mods.add("static");
        output.addMethod(mods, "<clinit>", "()V", null, false);

        // If there are static field initializations, generate code for them.
        for (JFieldDeclaration staticField : staticFieldInitializations) {
            staticField.codegenInitializations(output);
        }

        output.addNoArgInstruction(RETURN);
    }
}
