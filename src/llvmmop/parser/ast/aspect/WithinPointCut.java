package llvmmop.parser.ast.aspect;

import llvmmop.parser.ast.visitor.GenericVisitor;
import llvmmop.parser.ast.visitor.PointcutVisitor;
import llvmmop.parser.ast.visitor.VoidVisitor;

public class WithinPointCut extends PointCut {
	
	TypePattern pattern;
	
	public WithinPointCut(int line, int column, String type, TypePattern pattern){
		super(line, column, type);
		this.pattern = pattern;
	}
	
	public TypePattern getPattern() { return pattern; }
	
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
