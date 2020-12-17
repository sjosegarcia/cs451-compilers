// Copyright 2012- Bill Campbell, Swami Iyer and Bahar Akbal-Delibas

package jminusminus;

import static jminusminus.CLConstants.*;
import java.util.Stack;

/**
 * The AST node for a continue-statement.
 */
class JContinueStatement extends JStatement {

    private JStatement statement;

    /**
     * Constructs an AST node for a continue-statement.
     *
     * @param line line in which the continue-statement appears in the source file.
     */
    public JContinueStatement(int line) {
        super(line);
    }

    /**
     * {@inheritDoc}
     */
    public JContinueStatement analyze(Context context) {
        Stack<JStatement> stack = JMember.memberStack;
        if (stack.size() > 0) {
            statement = stack.pop();
            if (statement instanceof JIfStatement)
                JAST.compilationUnit.reportSemanticError(line(), "Found continue inside an if statement.");
        }
        return this;
    }

    /**
     * {@inheritDoc}
     */
    public void codegen(CLEmitter output) {
        if (statement != null)
            output.addBranchInstruction(GOTO, statement.getContinueLabel());
    }

    /**
     * {@inheritDoc}
     */
    public void toJSON(JSONElement json) {
        JSONElement e = new JSONElement();
        json.addChild("JContinueStatement:" + line, e);
    }
}
