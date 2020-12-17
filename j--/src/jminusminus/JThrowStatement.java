// Copyright 2012- Bill Campbell, Swami Iyer and Bahar Akbal-Delibas

package jminusminus;

import static jminusminus.CLConstants.*;

/**
 * The AST node for a throw-statement.
 */
class JThrowStatement extends JStatement {
    // Throw expression.
    private JExpression throwExpression;


    /**
     * Constructs an AST node for a throw-statement.
     *
     * @param line      line in which the throw-statement occurs in the source file.
     * @param throwExpression throw expression.
     */
    public JThrowStatement(int line, JExpression throwExpression) {
        super(line);
        this.throwExpression = throwExpression;
    }

    /**
     * {@inheritDoc}
     */
    public JThrowStatement analyze(Context context) {
        throwExpression = (JExpression) throwExpression.analyze(context);
        return this;
    }

    /**
     * {@inheritDoc}
     */
    public void codegen(CLEmitter output) {
        throwExpression.codegen(output);
        output.addNoArgInstruction(ATHROW);
    }

    /**
     * {@inheritDoc}
     */
    public void toJSON(JSONElement json) {
        JSONElement e = new JSONElement();
        json.addChild("JThrowStatement:" + line, e);
        JSONElement e1 = new JSONElement();
        e.addChild("Expression", e1);
        throwExpression.toJSON(e1);
    }
}
