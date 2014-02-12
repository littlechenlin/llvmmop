package llvmmop.parser.astex.mopspec;

import llvmmop.parser.astex.visitor.GenericVisitor;
import llvmmop.parser.astex.visitor.VoidVisitor;
import llvmmop.parser.astex.ExtNode;

public abstract class PropertyExt extends ExtNode {
	
	String type;
	String propertyName; //soha
	
	public PropertyExt (int line, int column, String type, String propertyName){
		super(line, column);
		this.type = type;
		this.propertyName = propertyName;
	}
	
	public String getType() { return type; }
	
	public String getName() {return propertyName; } //soha 
	
    @Override
    public <A> void accept(VoidVisitor<A> v, A arg) {
        v.visit(this, arg);
    }

    @Override
    public <R, A> R accept(GenericVisitor<R, A> v, A arg) {
        return v.visit(this, arg);
    }
	
}
