package llvmmop.parser.ast.aspect;

import llvmmop.parser.ast.expr.*;
import llvmmop.parser.ast.visitor.GenericVisitor;
import llvmmop.parser.ast.visitor.PointcutVisitor;
import llvmmop.parser.ast.visitor.VoidVisitor;

public class CountCondPointCut extends PointCut {
	
	Expression expr;

	public CountCondPointCut(int line, int column, String type, Expression expr) {
		super(line, column, type);
		this.expr = expr;
	}
	
	public Expression getExpression() { return expr; }

    @Override
    public <A> void accept(VoidVisitor<A> v, A arg) {
        v.visit(this, arg);
    }

    @Override
    public <R, A> R accept(GenericVisitor<R, A> v, A arg) {
        return v.visit(this, arg);
    }

    @Override
    public <R, A> R accept(PointcutVisitor<R, A> v, A arg) {
        return v.visit(this, arg);
    }
}
