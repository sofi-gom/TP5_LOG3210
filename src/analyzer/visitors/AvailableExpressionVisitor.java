package analyzer.visitors;

import analyzer.ast.*;

import java.io.PrintWriter;
import java.util.*;
import java.util.stream.Collectors;

/**
 * This Java code defines a visitor class called AvailableExpressionVisitor for a custom parser.
 * The visitor class traverses an abstract syntax tree (AST) and generates optimised code (Common expression elimination).
 * The code includes implementations for various types of assignment statements, such as direct assignment,
 * unary assignment, and assignment with arithmetic operations.
 * */
public class AvailableExpressionVisitor implements ParserVisitor {
    private PrintWriter m_writer = null;
    private final ArrayList<String> RETURNS = new ArrayList<>();
    private final ArrayList<CodeLine> CODE = new ArrayList<>();

    public AvailableExpressionVisitor(PrintWriter writer) {
        m_writer = writer;
    }

    @Override
    public Object visit(SimpleNode node, Object data) {
        return null;
    }

    @Override
    public Object visit(ASTProgram node, Object data) {
        node.childrenAccept(this, null);

        computeAvailableExpr();
        eliminateCommonExpression();

        printCode();
        return null;
    }

    @Override
    public Object visit(ASTReturnStmt node, Object data) {
        for (int i = 0; i < node.jjtGetNumChildren(); i++) {
            RETURNS.add(((ASTIdentifier) node.jjtGetChild(i)).getValue());
        }
        return null;
    }

    @Override
    public Object visit(ASTBlock node, Object data) {
        node.childrenAccept(this, null);
        return null;
    }

    @Override
    public Object visit(ASTStmt node, Object data) {
        node.childrenAccept(this, null);
        return null;
    }

    @Override
    public Object visit(ASTAssignStmt node, Object data) {
        String assign = (String) node.jjtGetChild(0).jjtAccept(this, null);
        String left = (String) node.jjtGetChild(1).jjtAccept(this, null);
        String right = (String) node.jjtGetChild(2).jjtAccept(this, null);
        String op = node.getOp();

        CODE.add(new CodeLine(op, assign, left, right));

        return null;
    }

    @Override
    public Object visit(ASTAssignUnaryStmt node, Object data) {
        String assign = (String) node.jjtGetChild(0).jjtAccept(this, null);
        String left = (String) node.jjtGetChild(1).jjtAccept(this, null);

        CODE.add(new CodeLine("-", assign, left, ""));

        return null;
    }

    @Override
    public Object visit(ASTAssignDirectStmt node, Object data) {
        String assign = (String) node.jjtGetChild(0).jjtAccept(this, null);
        String left = (String) node.jjtGetChild(1).jjtAccept(this, null);

        CODE.add(new CodeLine("+", assign, left, ""));

        return null;
    }

    @Override
    public Object visit(ASTExpr node, Object data) {
        return node.jjtGetChild(0).jjtAccept(this, null);
    }

    @Override
    public Object visit(ASTIntValue node, Object data) {
        return "#" + node.getValue();
    }

    @Override
    public Object visit(ASTIdentifier node, Object data) {
        return node.getValue();
    }

    /**
     * Computes the GEN sets for each line of code.
     */
    public void computeGenSets(){
        for (CodeLine code : CODE) {
            Expression lineExpr = new Expression(code.left, code.op, code.right);
            if (code.ASSIGN.equals(code.left) || code.ASSIGN.equals(code.right)) {
                continue;
            }
            code.GEN.add(lineExpr);
        }
    }

    /**
     * Computes the KILL sets for each line of code.
     */
    public void computeKillSets() {
        for (CodeLine code : CODE) {
            for (CodeLine otherCode : CODE) {
                String assign = code.ASSIGN;
                if (otherCode != code && (assign.equals(otherCode.left) || assign.equals(otherCode.right))) {
                        Expression lineExpr = new Expression(code.left, code.op, code.right);
                        code.KILL.add(lineExpr);
                }
            }
        }
    }

    /**
     * Computes the Available Expression Analysis for the code.
     */
    private void computeAvailableExpr() {
        computeGenSets();
        computeKillSets();
        boolean changes = true;
        Set<Expression> tmpOut = new HashSet<>();
        while (changes) {
            int i = 0;
            changes = false;
            for (CodeLine code : CODE) {
                if(i != 0) {
                    if (!code.Avail_IN.containsAll(tmpOut)) {
                        code.Avail_IN.addAll(tmpOut);
                        changes = true;
                    }
                }
                code.Avail_OUT.addAll(code.Avail_IN);
                code.Avail_OUT.removeAll(code.KILL);
                code.Avail_OUT.addAll(code.GEN);
                tmpOut = code.Avail_OUT;
                i++;
            }
        }
    }

    /**
     * Eliminates common expressions in the code using the Available Expression Analysis.
     */
    private void eliminateCommonExpression() {
        for (CodeLine code : CODE) {
            Expression lineExpr = new Expression(code.left, code.op, code.right);
            if (code.Avail_IN.contains(lineExpr)) {
                int index = 0;
                CodeLine otherCode = CODE.get(index);
                while (otherCode != code) {
                    Expression otherLine = new Expression(otherCode.left, otherCode.op, otherCode.right);
                    if (lineExpr.equals(otherLine)) {
                        code.left = otherCode.ASSIGN;
                        code.right = "";
                    }
                    index++;
                    otherCode = CODE.get(index);
                }
            }
        }

    }

    public void printCode() {
        int i = 0;
        for (CodeLine code : CODE) {
            String line = code.ASSIGN + " = " + code.left;
            if (!code.right.isEmpty() && !code.right.equals("#0")) {
                line += " " + code.op + " " + code.right;
            }
            m_writer.println("// Bloc " + i);
            m_writer.println(line);
            m_writer.println("// Avail_IN  : " + sortedExpressions(code.Avail_IN));
            m_writer.println("// Avail_OUT : " + sortedExpressions(code.Avail_OUT));
            m_writer.println();
            i++;
        }
    }


    // Helper function to convert a set of Expression objects to a sorted list of strings
    private List<String> sortedExpressions(Set<Expression> expressions) {
        return expressions.stream()
                .map(Expression::toString)
                .sorted()
                .collect(Collectors.toList());
    }

    /**
     * A struct to store the data of a code line.
     */
    public class CodeLine {
        public String op;
        public String ASSIGN;
        public String left;
        public String right;
        public Set<Expression> GEN;
        public Set<Expression> KILL;
        public Set<Expression> Avail_IN;
        public Set<Expression> Avail_OUT;

        public CodeLine(String op, String ASSIGN, String left, String right) {
            this.op = op;
            this.ASSIGN = ASSIGN;
            this.left = left;
            this.right = right;
            this.GEN = new HashSet<>();
            this.KILL = new HashSet<>();
            this.Avail_IN = new HashSet<>();
            this.Avail_OUT = new HashSet<>();
        }
    }

    /**
     * A struct to store an arithmetic expression.
     */
    public static class Expression {
        String left;
        String op;
        String right;

        public Expression(String left, String op, String right) {
            this.left = left;
            this.op = op;
            this.right = right;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) return true;
            if (obj == null || getClass() != obj.getClass()) return false;
            Expression that = (Expression) obj;
            return Objects.equals(left, that.left) &&
                    Objects.equals(op, that.op) &&
                    Objects.equals(right, that.right);
        }

        @Override
        public int hashCode() {
            return Objects.hash(left, op, right);
        }

        @Override
        public String toString() {
            return left + op + right;
        }
    }
}