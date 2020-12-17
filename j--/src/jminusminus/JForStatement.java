// Copyright 2013 Bill Campbell, Swami Iyer and Bahar Akbal-Delibas

package jminusminus;

import java.util.ArrayList;
import static jminusminus.CLConstants.*;

/**
 * The AST node for a for-statement.
 */

class JForStatement extends JStatement {

    // Initialization statements
    private ArrayList<JStatement> inits;

    // Test expression.
    private JExpression condition;

    // Update statements
    private ArrayList<JStatement> updates;

    // The body.
    private JStatement body;

    /**
     * Constructs an AST node for a for-statement.
     *
     * @param line      line in which the for-statement occurs in the source file.
     * @param condition test expression.
     * @param body      the body.
     */
    public JForStatement(int line, ArrayList<JStatement> inits, JExpression condition, ArrayList<JStatement> updates, JStatement body) {
        super(line);
        this.inits = inits;
        this.condition = condition;
        this.updates = updates;
        this.body = body;
    }

    /**
     * {@inheritDoc}
     */
    public JForStatement analyze(Context context) {
        LocalContext forLoopContext = new LocalContext(context);
        JMember.memberStack.push(this);
        if (inits != null)
            for (int i = 0; i < inits.size(); i++) {
                inits.set(i, (JStatement) inits.get(i).analyze(forLoopContext));
            }
        if (condition != null) {
            condition = (JExpression) condition.analyze(forLoopContext);
            condition.type().mustMatchExpected(line(), Type.BOOLEAN);
        }
        if (updates != null)
            for (int i = 0; i < updates.size(); i++) {
                updates.set(i, (JStatement) updates.get(i).analyze(forLoopContext));
            }
        body = (JStatement) body.analyze(forLoopContext);
        return this;
    }

    /**
     * {@inheritDoc}
     */
    public void codegen(CLEmitter output) {
        String startFor = output.createLabel();
        String endFor = output.createLabel();
        breakLabel = output.createLabel();
        continueLabel = output.createLabel();

        for (JStatement statement : inits)
            statement.codegen(output);
        
        output.addLabel(startFor);

        condition.codegen(output, endFor, false);

        body.codegen(output);
        
        output.addLabel(continueLabel);
        for (JStatement statement : updates)
            statement.codegen(output);
        
        
        output.addBranchInstruction(GOTO, startFor);
        
        output.addLabel(endFor);
        output.addLabel(breakLabel);
        
    }

    /**
     * {@inheritDoc}
     */
    public void toJSON(JSONElement json) {
        JSONElement e = new JSONElement();
        json.addChild("JForStatement:" + line, e);
        if (inits != null) {
            JSONElement e1 = new JSONElement();
            e.addChild("Init", e1);
            for (JStatement init : inits)
                init.toJSON(e1);
        }
        if (condition != null) {
            JSONElement e2 = new JSONElement();
            e.addChild("Condition", e2);
            condition.toJSON(e2);
        }
        if (updates != null) {
            JSONElement e3 = new JSONElement();
            e.addChild("Update", e3);
            for (JStatement update : updates)
                update.toJSON(e3);
        }
        JSONElement e4 = new JSONElement();
        e.addChild("Body", e4);
        body.toJSON(e4);
    }
}