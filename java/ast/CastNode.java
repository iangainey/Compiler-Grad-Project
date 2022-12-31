package ast;

import ast.visitor.ASTVisitor;
import compiler.Scope;

//Not instructor supplied

public class CastNode extends ExpressionNode {
    
    private ExpressionNode expr;
    //private Scope.Type type;

    public CastNode(ExpressionNode expr, Scope.Type type) {
        setExpr(expr);
        setType(type);
    }

    @Override
    public <R> R accept(ASTVisitor<R> visitor) {
        return visitor.visit(this);
    }

    public ASTNode getExpr() {
        return expr;
    }

    public void setExpr(ExpressionNode castExpr) {
        expr = castExpr;
    }

}
