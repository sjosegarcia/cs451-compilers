// Copyright 2012- Bill Campbell, Swami Iyer and Bahar Akbal-Delibas

package jminusminus;

import java.util.ArrayList;

import static jminusminus.CLConstants.*;

/**
 * The AST node for a method declaration.
 */
class JMethodDeclaration extends JAST implements JMember {
    /**
     * Method modifiers.
     */
    protected ArrayList<String> mods;

    /**
     * Method name.
     */
    protected String name;

    /**
     * Return type.
     */
    protected Type returnType;

    /**
     * The formal parameters.
     */
    protected ArrayList<JFormalParameter> params;

    /**
     * Method body.
     */
    protected JBlock body;

    /**
     * Method context (built in analyze()).
     */
    protected MethodContext context;

    /**
     * Method descriptor (computed in preAnalyze()).
     */
    protected String descriptor;

    /**
     * Is this method abstract?
     */
    protected boolean isAbstract;

    /**
     * Is this method static?
     */
    protected boolean isStatic;

    /**
     * Is this method private?
     */
    protected boolean isPrivate;

    /**
     * A list of exceptions
     */
    protected ArrayList<Type> exceptions;

    private ArrayList<String> exceptionNames;

    /**
     * Constructs an AST node for a method declaration.
     *
     * @param line       line in which the method declaration occurs in the source file.
     * @param mods       modifiers.
     * @param name       method name.
     * @param returnType return type.
     * @param params     the formal parameters.
     * @param body       method body.
     * @param exceptions exceptions wrapped around the method.
     */
    public JMethodDeclaration(int line, ArrayList<String> mods, String name, Type returnType,
                              ArrayList<JFormalParameter> params, JBlock body, ArrayList<Type> exceptions) {
        super(line);
        this.mods = mods;
        this.name = name;
        this.returnType = returnType;
        this.params = params;
        this.body = body;
        if (body == null) {
            mods.add("abstract");
        }
        this.exceptions = exceptions;
        isAbstract = mods.contains("abstract");
        isStatic = mods.contains("static");
        isPrivate = mods.contains("private");
    }

    /**
     * {@inheritDoc}
     */
    public void preAnalyze(Context context, CLEmitter partial) {
        // Resolve types of the formal parameters.
        for (JFormalParameter param : params) {
            param.setType(param.type().resolve(context));
        }

        if (exceptions != null) {
            exceptionNames = new ArrayList<String>();
            for (int i = 0; i < exceptions.size(); i++) {
                exceptions.set(i, (Type) exceptions.get(i).resolve(context));
                exceptionNames.add(exceptions.get(i).jvmName());
            }
        }
        // Resolve return type.
        returnType = returnType.resolve(context);

        // Check proper local use of abstract
        if (isAbstract && body != null) {
            JAST.compilationUnit.reportSemanticError(line(), "abstract method cannot have a body");
        } else if (body == null && !isAbstract) {
            JAST.compilationUnit.reportSemanticError(line(),
                    "Method without body must be abstract");
        } else if (isAbstract && isPrivate) {
            JAST.compilationUnit.reportSemanticError(line(), "private method cannot be abstract");
        } else if (isAbstract && isStatic) {
            JAST.compilationUnit.reportSemanticError(line(), "static method cannot be abstract");
        }

        // Compute descriptor.
        descriptor = "(";
        for (JFormalParameter param : params) {
            descriptor += param.type().toDescriptor();
        }
        descriptor += ")" + returnType.toDescriptor();

        // Generate the method with an empty body (for now).
        partialCodegen(context, partial);
    }

    /**
     * {@inheritDoc}
     */
    public JAST analyze(Context context) {
        MethodContext methodContext = new MethodContext(context, isStatic, returnType);
        this.context = methodContext;

        if (!isStatic) {
            // Offset 0 is used to address "this".
            this.context.nextOffset();
        }

        // Declare the parameters. We consider a formal parameter to be always initialized, via a
        // method call.
        for (JFormalParameter param : params) {
            Type type = param.type();
            int offset = this.context.nextOffset();
            if (type == Type.DOUBLE || type == Type.LONG) {
                this.context.nextOffset(); // Offset is used to address doubles and long
            }
            LocalVariableDefn defn = new LocalVariableDefn(type, offset);
            defn.initialize();
            this.context.addEntry(param.line(), param.name(), defn);
        }
        
        if (body != null) {
            body = body.analyze(this.context);
            if (returnType != Type.VOID && !methodContext.methodHasReturn()) {
                JAST.compilationUnit.reportSemanticError(line(),
                        "Non-void method must have a return statement");
            }
        }
        memberStack.clear();
        return this;
    }

    /**
     * {@inheritDoc}
     */
    public void partialCodegen(Context context, CLEmitter partial) {
        partial.addMethod(mods, name, descriptor, exceptionNames, false);
        if (returnType == Type.VOID) {
            partial.addNoArgInstruction(RETURN);
        } else if (returnType == Type.INT || returnType == Type.BOOLEAN ||
                returnType == Type.CHAR) {
            partial.addNoArgInstruction(ICONST_0);
            partial.addNoArgInstruction(IRETURN);
        } else if (returnType == Type.DOUBLE) {
            partial.addNoArgInstruction(DCONST_0);
            partial.addNoArgInstruction(DRETURN);
        } else if (returnType == Type.LONG) {
            partial.addNoArgInstruction(LCONST_0);
            partial.addNoArgInstruction(LRETURN);
        } else {
            partial.addNoArgInstruction(ACONST_NULL);
            partial.addNoArgInstruction(ARETURN);
        }
    }

    /**
     * {@inheritDoc}
     */
    public void codegen(CLEmitter output) {
        output.addMethod(mods, name, descriptor, exceptionNames, false);
        if (body != null) {
            body.codegen(output);
        }
        if (returnType == Type.VOID) {
            output.addNoArgInstruction(RETURN);
        }
    }

    /**
     * {@inheritDoc}
     */
    public void toJSON(JSONElement json) {
        JSONElement e = new JSONElement();
        json.addChild("JMethodDeclaration:" + line, e);
        e.addAttribute("name", name);
        e.addAttribute("returnType", returnType.toString());
        if (mods != null) {
            ArrayList<String> value = new ArrayList<String>();
            for (String mod : mods) {
                value.add(String.format("\"%s\"", mod));
            }
            e.addAttribute("modifiers", value);
        }
        if (params != null) {
            ArrayList<String> value = new ArrayList<String>();
            for (JFormalParameter param : params) {
                value.add(String.format("[\"%s\", \"%s\"]", param.name(),
                        param.type() == null ? "" : param.type().toString()));
            }
            e.addAttribute("parameters", value);
        }
        if (exceptions != null) {
            ArrayList<String> value = new ArrayList<String>();
            for (Type exception : exceptions) {
                value.add(String.format("\"%s\"", exception.jvmName()));
            }
            e.addAttribute("throws", value);
        }
        if (context != null) {
            context.toJSON(e);
        }
        if (body != null) {
            body.toJSON(e);
        }
    }
}
