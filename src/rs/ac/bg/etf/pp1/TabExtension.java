package rs.ac.bg.etf.pp1;

import rs.etf.pp1.symboltable.Tab;
import rs.etf.pp1.symboltable.concepts.Obj;
import rs.etf.pp1.symboltable.concepts.Scope;
import rs.etf.pp1.symboltable.concepts.Struct;

public abstract class TabExtension extends Tab {
	static Struct boolType = new Struct(Struct.Bool);
	public static void init() {
		Tab.init();
		Scope outerScope = currentScope;
		while (outerScope.getOuter()!=null) {
			outerScope = outerScope.getOuter();
		}
		outerScope.addToLocals(new Obj(Obj.Type, "bool", boolType));
	}

}
