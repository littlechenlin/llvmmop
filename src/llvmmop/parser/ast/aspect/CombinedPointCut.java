package llvmmop.parser.ast.aspect;

import java.util.*;

import llvmmop.parser.ast.visitor.GenericVisitor;
import llvmmop.parser.ast.visitor.PointcutVisitor;
import llvmmop.parser.ast.visitor.VoidVisitor;

public class CombinedPointCut extends PointCut {
	
	List<PointCut> pointcuts;

	public CombinedPointCut(int line, int column, String type, List<PointCut> pointcuts){
		super(line, column, type);
		
		this.pointcuts = pointcuts;
	}
	
	public List<PointCut> getPointcuts() { return pointcuts; }
	
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
