// Copyright 2012- Bill Campbell, Swami Iyer and Bahar Akbal-Delibas

package jminusminus;

import static jminusminus.CLConstants.*;
import java.util.Stack;


/**
 * The AST node for a break-statement.
 */
class JBreakStatement extends JStatement {


    private String breakLabel;
    private JStatement statement;
    /**
     * Constructs an AST node for a break-statement.
     *
     * @param line line in which the break-statement appears in the source file.
     */
    public JBreakStatement(int line) {
        super(line);
    }

    /**
     * {@inheritDoc}
     */
    public JBreakStatement analyze(Context context) {
        Stack<JStatement> stack = JMember.memberStack;
        if (stack.size() > 0) {
            statement = stack.pop();
            if (statement instanceof JIfStatement)
                JAST.compilationUnit.reportSemanticError(line(), "Found break inside an if statement.");
        }
        return this;
    }

    /**
     * {@inheritDoc}
     */
    public void codegen(CLEmitter output) {
        if (statement != null)
            output.addBranchInstruction(GOTO, statement.getBreakLabel());
    }

    /**
     * {@inheritDoc}
     */
    public void toJSON(JSONElement json) {
        JSONElement e = new JSONElement();
        json.addChild("JBreakStatement:" + line, e);
    }
}
