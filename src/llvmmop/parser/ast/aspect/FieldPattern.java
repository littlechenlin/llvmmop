package llvmmop.parser.ast.aspect;

import llvmmop.parser.ast.Node;
import llvmmop.parser.ast.visitor.GenericVisitor;
import llvmmop.parser.ast.visitor.VoidVisitor;

public class FieldPattern extends Node {

	int modifiers;
	int not_modifiers;
	TypePattern type;
	TypePattern owner;
	String name;
	
	public FieldPattern(int line, int column, int modifiers, int not_modifiers, TypePattern type, TypePattern owner, String name){
		super(line, column);
		this.modifiers = modifiers;
		this.not_modifiers = not_modifiers;
		if (type != null)
			this.type = type;
		else
			this.type = new BaseTypePattern(line, column, "*");
		
		this.owner = owner;
		this.name = name;
	}
	
	public int getModifiers() { return modifiers; }
	public int getNotModifiers() { return not_modifiers; }
	public TypePattern getType() { return type; }
	public TypePattern getOwner() { return owner; }
	public String getMemberName() { return name; }
	
    @Override
    public <A> void accept(VoidVisitor<A> v, A arg) {
        v.visit(this, arg);
    }

    @Override
    public <R, A> R accept(GenericVisitor<R, A> v, A arg) {
        return v.visit(this, arg);
    }
}
