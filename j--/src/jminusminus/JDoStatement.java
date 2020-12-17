// Copyright 2013 Bill Campbell, Swami Iyer and Bahar Akbal-Delibas

package jminusminus;

import static jminusminus.CLConstants.*;

/**
 * The AST node for a do-while-statement.
 */

class JDoStatement extends JStatement {

    // Test expression.
    private JExpression condition;

    // The body.
    private JStatement body;

    /**
     * Constructs an AST node for a do-while-statement.
     *
     * @param line      line in which the do-while-statement occurs in the source file.
     * @param condition test expression.
     * @param body      the body.
     */
    public JDoStatement(int line, JExpression condition, JStatement body) {
        super(line);
        this.condition = condition;
        this.body = body;
    }

    /**
     * {@inheritDoc}
     */
    public JDoStatement analyze(Context context) {
        JMember.memberStack.push(this);
        body = (JStatement) body.analyze(context);
        condition = condition.analyze(context);
        condition.type().mustMatchExpected(line(), Type.BOOLEAN);
        JMember.memberStack.push(this);
        return this;
    }

    /**
     * {@inheritDoc}
     */
    public void codegen(CLEmitter output) {
        String doWhile = output.createLabel();
        breakLabel = output.createLabel();
        continueLabel = output.createLabel();
        output.addLabel(doWhile);
        body.codegen(output);
        output.addLabel(continueLabel);
        condition.codegen(output, doWhile, true);
        output.addLabel(breakLabel);
    }

    /**
     * {@inheritDoc}
     */
    public void toJSON(JSONElement json) {
        JSONElement e = new JSONElement();
        json.addChild("JDoStatement:" + line, e);
        JSONElement e1 = new JSONElement();
        e.addChild("Body", e1);
        body.toJSON(e1);
        JSONElement e2 = new JSONElement();
        e.addChild("Condition", e2);
        condition.toJSON(e2);
    }
}