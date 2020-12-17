// Copyright 2012- Bill Campbell, Swami Iyer and Bahar Akbal-Delibas

package jminusminus;

import java.util.ArrayList;
import static jminusminus.CLConstants.*;

/**
 * The AST node for a try-statement.
 */
class JTryStatement extends JStatement {
    // Try block
    private JBlock tryBlock;

    // Catch params
    private ArrayList<JFormalParameter> catchParams;

    // Catch block
    private ArrayList<JBlock> catchBlocks;

    // Finally block
    private JBlock finallyBlock;
    

    /**
     * Constructs an AST node for a try-statement.
     *
     * @param line      line in which the try-statement occurs in the source file.
     * @param tryBlock  "try" block of code
     * @param catchParams parameters of the catch block
     * @param catchBlocks list of formal parameters
     * @param finallyBlock "finally" block of code
     */
    public JTryStatement(int line, JBlock tryBlock, ArrayList<JFormalParameter> catchParams, ArrayList<JBlock> catchBlocks, JBlock finallyBlock) {
        super(line);
        this.tryBlock = tryBlock;
        this.catchParams = catchParams;
        this.catchBlocks = catchBlocks;
        this.finallyBlock = finallyBlock;
    }

    /**
     * {@inheritDoc}
     */
    public JTryStatement analyze(Context context) {
        LocalContext tryContext = new LocalContext(context);
        tryBlock = (JBlock) tryBlock.analyze(tryContext);
        for (int i = 0; i < catchParams.size(); i++) {
            LocalContext catchContext = new LocalContext(context);
            JFormalParameter param = catchParams.get(i);
            param.setType(param.type().resolve(catchContext));
            Type type = param.type();
            int offset = catchContext.nextOffset();
            LocalVariableDefn defn = new LocalVariableDefn(type, offset);
            defn.initialize();
            catchContext.addEntry(param.line(), param.name(), defn);
            catchBlocks.set(i, ((JBlock) catchBlocks.get(i)).analyze(catchContext));
        }
        if (finallyBlock != null) {
            finallyBlock = (JBlock) finallyBlock.analyze(context);
        }
        return this;
    }

    /**
     * {@inheritDoc}
     */
    public void codegen(CLEmitter output) {
        String startTryLabel = output.createLabel();
        String endTryLabel = output.createLabel();
        String endCatchLabel = output.createLabel();
        String startFinallyLabel = output.createLabel();
        String startFinallyPlusOne = output.createLabel();
        String endFinallyLabel = output.createLabel();
        
        output.addLabel(startTryLabel);
        tryBlock.codegen(output);
        if (finallyBlock != null) {
            finallyBlock.codegen(output);
        }
        output.addBranchInstruction(GOTO, endFinallyLabel);
        output.addLabel(endTryLabel);

        ArrayList<String> catchLabels = new ArrayList<String>();
        for (int i = 0; i < catchBlocks.size(); i++) {
            String catchLabel = output.createLabel();
            catchLabels.add(catchLabel);
            output.addExceptionHandler(startTryLabel, endTryLabel, catchLabel, catchParams.get(i).type().jvmName());
            JBlock catchBlock = catchBlocks.get(i);
            output.addLabel(catchLabel);
            output.addNoArgInstruction(ASTORE_1);
            catchBlock.codegen(output);
            if (finallyBlock != null) {
                finallyBlock.codegen(output);
            }
            output.addBranchInstruction(GOTO, endFinallyLabel);
        }
        output.addExceptionHandler(startTryLabel, endTryLabel, startFinallyLabel, null);

        output.addLabel(startFinallyLabel);
        if (finallyBlock != null) {
            output.addOneArgInstruction(ASTORE, catchLabels.size() + 2);
            output.addLabel(startFinallyPlusOne);
            finallyBlock.codegen(output);
            output.addOneArgInstruction(ALOAD, catchLabels.size() + 2);
            output.addNoArgInstruction(ATHROW);
        }
        output.addLabel(endFinallyLabel);
        for (int i = 0; i < catchLabels.size(); i++) {
            if (i < catchLabels.size()-1) {
                output.addExceptionHandler(catchLabels.get(i), catchLabels.get(i+1), startFinallyLabel, null);
            } else {
                output.addExceptionHandler(catchLabels.get(i), startFinallyLabel, startFinallyLabel, null);
            }
        }        

        if (finallyBlock != null) {
            output.addExceptionHandler(startFinallyLabel, startFinallyPlusOne, startFinallyLabel, null);
        }
    }

    /**
     * {@inheritDoc}
     */
    public void toJSON(JSONElement json) {
        JSONElement e = new JSONElement();
        json.addChild("JTryStatement:" + line, e);
        if (tryBlock != null) {
            JSONElement e1 = new JSONElement();
            e.addChild("TryBlock", e1);
            tryBlock.toJSON(e1);
        }
        for (int i = 0; i < catchBlocks.size(); i++) {
            JSONElement e2 = new JSONElement();
            e.addChild("CatchBlock", e2);
            JFormalParameter param = catchParams.get(i);
            String value = String.format("[\"%s\", \"%s\"]", param.name(), param.type() == null ? "" : param.type().toString());
            e2.addAttribute("parameter", value);
            catchBlocks.get(i).toJSON(e2);
        }
        if (finallyBlock != null) {
            JSONElement e3 = new JSONElement();
            e.addChild("FinallyBlock", e3);
            finallyBlock.toJSON(e3);
        }
    }
}
