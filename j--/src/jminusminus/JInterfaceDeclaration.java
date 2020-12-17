// Copyright 2011 Bill Campbell, Swami Iyer and Bahar Akbal-Delibas

package jminusminus;

import java.util.ArrayList;
import static jminusminus.CLConstants.*;

/**
 * The AST node for a method declaration.
 */

class JInterfaceDeclaration extends JAST implements JTypeDecl {

    // Method modifiers.
    protected ArrayList<String> mods;

    // Interface name.
    protected String name;

    // Super class type.
    private Type superType;

    // Interface block.
    private ArrayList<JMember> interfaceBlocks;

    // Built in analyze().
    protected ClassContext context;

    // This interface type.
    private Type thisType;

    protected ArrayList<Type> identifiers;

    // Instance fields of this class.
    private ArrayList<JFieldDeclaration> staticFieldInitializations;

    /**
     * Constructs an AST node for a class declaration.
     *
     * @param line       line in which the class declaration occurs in the source file.
     * @param mods       interface modifiers.
     * @param name       interface name.
     * @param identifiers list of identifiers.
     * @param interfaceBlock  list of interface members.
     */
    public JInterfaceDeclaration(int line, ArrayList<String> mods, String name, ArrayList<Type> identifiers, ArrayList<JMember> interfaceBlocks) {
        super(line);
        this.mods = mods;
        this.mods.add("interface");
        this.mods.add("abstract");
        this.name = name;
        this.identifiers = identifiers;
        this.interfaceBlocks = interfaceBlocks;
        staticFieldInitializations = new ArrayList<JFieldDeclaration>();
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
  	public void declareThisType(Context context) {
        String qualifiedName = JAST.compilationUnit.packageName() == "" ?
                name : JAST.compilationUnit.packageName() + "/" + name;
        CLEmitter partial = new CLEmitter(false);
        partial.addClass(mods, qualifiedName, Type.OBJECT.jvmName(), null, false);
        thisType = Type.typeFor(partial.toClass());
        context.addType(line, thisType);
  	}

    /**
     * {@inheritDoc}
     */
    public void preAnalyze(Context context) {
        // Construct a class context.
        this.context = new ClassContext(this, context);
        if (identifiers != null) {
            for (int i = 0; i < identifiers.size(); i++) {
                identifiers.set(i, (Type) identifiers.get(i).resolve(this.context));
            }
        }

        // Create the (partial) class.
        CLEmitter partial = new CLEmitter(false);

        // Add the class header to the partial class
        String qualifiedName = JAST.compilationUnit.packageName() == "" ?
                name : JAST.compilationUnit.packageName() + "/" + name;
        partial.addClass(mods, qualifiedName, Type.OBJECT.jvmName(), null, false);

        // Pre-analyze the members and add them to the partial class.
        for (JMember member : interfaceBlocks)
            member.preAnalyze(this.context, partial);


        // Get the ClassRep for the (partial) class and make it the representation for this type.
        Type id = this.context.lookupType(name);
        if (id != null && !JAST.compilationUnit.errorHasOccurred()) {
            id.setClassRep(partial.toClass());
        }
    }

    /**
     * {@inheritDoc}
     */
    public JAST analyze(Context context) {
        for (JMember iblocks : interfaceBlocks)
            ((JAST) iblocks).analyze(this.context);

        // Separate declared fields for purposes of initialization.
        for (JMember member : interfaceBlocks) {
            if (member instanceof JFieldDeclaration) {
                JFieldDeclaration fieldDecl = (JFieldDeclaration) member;
                if (fieldDecl.mods().contains("static")) {
                    staticFieldInitializations.add(fieldDecl);
                }
            }
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
        output.addClass(mods, qualifiedName, Type.OBJECT.jvmName(), null, false);

        // The members.
        for (JMember member : interfaceBlocks) {
            ((JAST) member).codegen(output);
        }
        if (staticFieldInitializations.size() > 0) {
            codegenClassInit(output);
        }
    }

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

    /**
     * {@inheritDoc}
     */
    public void toJSON(JSONElement json) {
        JSONElement e = new JSONElement();
        json.addChild("JInterfaceDeclaration:" + line, e);
        if (mods != null) {
            ArrayList<String> value = new ArrayList<String>();
            for (String mod : mods) {
                value.add(String.format("\"%s\"", mod));
            }
            e.addAttribute("modifiers", value);
        }
        e.addAttribute("name", name);
        if (interfaceBlocks != null)
            for (JMember member : interfaceBlocks)
                ((JAST) member).toJSON(e);
    }

}