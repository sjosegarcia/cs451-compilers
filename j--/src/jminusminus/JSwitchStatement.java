// Copyright 2012- Bill Campbell, Swami Iyer and Bahar Akbal-Delibas

package jminusminus;

import java.util.ArrayList;
import java.util.TreeMap;
import static jminusminus.CLConstants.*;

/**
 * The AST node for a switch-statement.
 * 
Define AST representation JSwitchStatement with instance variables for the condition, and
the list of SwitchStatementGroup objects, each storing a list of switch labels and a list of
statements
 */
class JSwitchStatement extends JStatement {
    // Test expression.
    private JExpression condition;

    // The list switch groups
    private ArrayList<SwitchBlockStatementGroup> switchGroups;

    private int lo;

    private int hi;
    private int nLabels;

    /**
     * Constructs an AST node for a switch-statement.
     *
     * @param line      line in which the switch-statement occurs in the source file.
     * @param condition test expression.
     * @param switchGroups list of switch groups
     */
    public JSwitchStatement(int line, JExpression condition, ArrayList<SwitchBlockStatementGroup> switchGroups) {
        super(line);
        this.condition = condition;
        this.switchGroups = switchGroups;
    }

    /**
     * {@inheritDoc}
     */
    public JSwitchStatement analyze(Context context) {
        condition = (JExpression) condition.analyze(context);
        condition.type().mustMatchExpected(line(), Type.INT);
        ArrayList<JExpression> switchLabels = new ArrayList<>();
        nLabels = 0;
        for (SwitchBlockStatementGroup group : switchGroups) {
            LocalContext switchContext = new LocalContext(context);
            for (int i = 0; i < group.getSwitchLabels().size(); i++) {
                JExpression label = group.getSwitchLabels().get(i);
                if (label != null) {
                    nLabels++;
                    group.getSwitchLabels().set(i, (JLiteralInt) label.analyze(switchContext));
                    label.type().mustMatchExpected(line(), Type.INT);
                    switchLabels.add(label);
                }
            }
            for (int i = 0; i < group.getStatements().size(); i++) {
                JStatement statement = group.getStatements().get(i);
                if (statement instanceof JBreakStatement)
                    JMember.memberStack.push(this);
                group.getStatements().set(i, (JStatement) statement.analyze(switchContext));
            }
        }
        findLowestAndHighest(switchLabels);
        return this;
    }

    /**
     * Finds the lowest and highest label in the entire switch
     */

    private void findLowestAndHighest(ArrayList<JExpression> switchLabels) {
         lo = hi = ((JLiteralInt) switchLabels.get(0)).getInt();
         for (int i = 0; i < switchLabels.size(); i++) {
             int current = ((JLiteralInt) switchLabels.get(i)).getInt();
             if (hi < current) {
                 hi = current;
             }
             if (lo > current) {
                 lo = current;
             }
         }
    }

    /**
     * Finds the correct operation code for the switch.
     *
     */
    private int findCorrectOperation() {
        long tableSpaceCost = 4 + ((long) hi - lo + 1);
        long tableTimeCost = 3;
        long lookupSpaceCost = 3 + 2 * (long) nLabels;
        long lookupTimeCost = nLabels;
        int opcode = (nLabels > 0 && tableSpaceCost + 3 * tableTimeCost <= lookupSpaceCost + 3 * lookupTimeCost) ? TABLESWITCH : LOOKUPSWITCH;
        return opcode;
    }

    /**
     * {@inheritDoc}
     */
    public void codegen(CLEmitter output) {
        String defaultLabel = output.createLabel();
        breakLabel = output.createLabel();
        condition.codegen(output);
        int opCode = findCorrectOperation();
        boolean containsDefault = false;
        if (opCode == TABLESWITCH) {
            containsDefault = codegenTableSwitch(output, defaultLabel);
        } else if (opCode == LOOKUPSWITCH) {
            containsDefault = codgenLookupSwitch(output, defaultLabel);
        }

        if (!containsDefault) {
            output.addLabel(defaultLabel);
        }
        output.addLabel(breakLabel);
    } 

    private boolean codegenTableSwitch(CLEmitter output, String defaultLabel) {
        boolean containsDefault = false;
        ArrayList<String> labels = new ArrayList<>();
        for (SwitchBlockStatementGroup group : switchGroups) { 
            ArrayList<JExpression> switchLabels = group.getSwitchLabels();
            for (JExpression switchLabel : switchLabels) {
                if (switchLabel != null) {
                    int literal = ((JLiteralInt) switchLabel).getInt();
                    labels.add("TableSwitchCase" + literal);
                }
            }
        }
        output.addTABLESWITCHInstruction(defaultLabel, lo, hi, labels);
        int labelCounter = 0;
        for (SwitchBlockStatementGroup group : switchGroups) {
            ArrayList<JExpression> switchLabels = group.getSwitchLabels();
            ArrayList<JStatement> statements = group.getStatements();
            for (int i = 0; i < switchLabels.size(); i++) {
                JExpression switchLabel = switchLabels.get(i);
                output.addLabel(switchLabel != null ? labels.get(labelCounter++) : defaultLabel);
                if (switchLabel == null)
                    containsDefault = true;
            }
            for (JStatement statement : statements) {
                statement.codegen(output);
            }
        }
        return containsDefault;
    }

    private boolean codgenLookupSwitch(CLEmitter output, String defaultLabel) {
        boolean containsDefault = false;
        TreeMap<Integer, String> matchLabelPairs = new TreeMap<Integer, String>();
        for (SwitchBlockStatementGroup group : switchGroups) {
            ArrayList<JExpression> switchLabels = group.getSwitchLabels();
            for (JExpression switchLabel : switchLabels) {
                if (switchLabel != null) {
                    int literal = ((JLiteralInt) switchLabel).getInt();
                    matchLabelPairs.put(literal, "LookUpCase" + literal);
                }
            }
        }
        output.addLOOKUPSWITCHInstruction(defaultLabel, matchLabelPairs.size(), matchLabelPairs);
        for (SwitchBlockStatementGroup group : switchGroups) {
            ArrayList<JExpression> switchLabels = group.getSwitchLabels();
            for (int i = 0; i < switchLabels.size(); i++) {
                JExpression switchLabel = switchLabels.get(i);
                if (switchLabel != null) {
                    int literal = ((JLiteralInt) switchLabel).getInt();
                    output.addLabel(matchLabelPairs.get(literal));
                } else {
                    output.addLabel(defaultLabel);
                    containsDefault = true;
                }
            }
            for (JStatement statement : group.getStatements()) {
                statement.codegen(output);
            }
        }
        return containsDefault;
    }

    /**
     * {@inheritDoc}
     */
    public void toJSON(JSONElement json) {
        JSONElement e = new JSONElement();
        json.addChild("JSwitchStatement:" + line, e);
        JSONElement e1 = new JSONElement();
        e.addChild("Condition", e1);
        condition.toJSON(e1);
        for (SwitchBlockStatementGroup switchGroup : switchGroups) {
            JSONElement e2 = new JSONElement();
            e.addChild("SwitchStatementGroup", e2);
            for (int i = 0; i < switchGroup.getSwitchLabels().size(); i++) {
                JSONElement e3 = new JSONElement();
                JExpression switchLabel = switchGroup.getSwitchLabels().get(i);
                e2.addChild(switchLabel != null ? "Case" : "Default", e3);  
                if (switchLabel != null)
                    switchLabel.toJSON(e3);
                for (JStatement statement : switchGroup.getStatements())
                    statement.toJSON(e2);
            }
        }
    }
}

class SwitchBlockStatementGroup {
    /**
     * This is the structure that will hold the representation of
     *  list of switch cases and the list of statements
     */

    private ArrayList<JExpression> switchLabels;
    private ArrayList<JStatement> statements;

    /**
     * Constructs the switch block statement object
     * @param switchLabels list of case labels and default label
     * @param statements list of statements that precedes the label
     */
    public SwitchBlockStatementGroup(ArrayList<JExpression> switchLabels, ArrayList<JStatement> statements) {
        this.switchLabels = switchLabels;
        this.statements = statements;
    }

    /**
     * Returns a list of switch labels
     * @return switchLabels
     */
    public ArrayList<JExpression> getSwitchLabels() {
        return switchLabels;
    }

    /**
     * Returns a list of statements
     * @return statements
     */
    public ArrayList<JStatement> getStatements() {
        return statements;
    }
}