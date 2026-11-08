package rs.ac.bg.etf.pp1;
import java.util.Vector;
import rs.ac.bg.etf.pp1.ast.ArrayElemAccessDesignator;
import rs.ac.bg.etf.pp1.ast.ArrayName;
import rs.ac.bg.etf.pp1.ast.BoolFactor;
import rs.ac.bg.etf.pp1.ast.BreakStatement;
import rs.ac.bg.etf.pp1.ast.CaseHeader;
import rs.ac.bg.etf.pp1.ast.CharFactor;
import rs.ac.bg.etf.pp1.ast.Colon;
import rs.ac.bg.etf.pp1.ast.ColonSwitch;
import rs.ac.bg.etf.pp1.ast.CondFactExprOnly;
import rs.ac.bg.etf.pp1.ast.CondFactRelopExpr;
import rs.ac.bg.etf.pp1.ast.CondTerm;
import rs.ac.bg.etf.pp1.ast.Condition;
import rs.ac.bg.etf.pp1.ast.ContinueStatement;
import rs.ac.bg.etf.pp1.ast.DesignatorAssignopExpr;
import rs.ac.bg.etf.pp1.ast.DesignatorAssignopExprW;
import rs.ac.bg.etf.pp1.ast.DesignatorDecrement;
import rs.ac.bg.etf.pp1.ast.DesignatorDecrementW;
import rs.ac.bg.etf.pp1.ast.DesignatorFunction;
import rs.ac.bg.etf.pp1.ast.DesignatorFunctionW;
import rs.ac.bg.etf.pp1.ast.DesignatorIncrement;
import rs.ac.bg.etf.pp1.ast.DesignatorIncrementW;
import rs.ac.bg.etf.pp1.ast.DividedByMulop;
import rs.ac.bg.etf.pp1.ast.Else;
import rs.ac.bg.etf.pp1.ast.ElsePart;
import rs.ac.bg.etf.pp1.ast.FactorDesignator;
import rs.ac.bg.etf.pp1.ast.FactorFuncCall;
import rs.ac.bg.etf.pp1.ast.ForBegin;
import rs.ac.bg.etf.pp1.ast.ForParens;
import rs.ac.bg.etf.pp1.ast.ForStatement;
import rs.ac.bg.etf.pp1.ast.GreaterEqualRelop;
import rs.ac.bg.etf.pp1.ast.GreaterRelop;
import rs.ac.bg.etf.pp1.ast.HasCondition;
import rs.ac.bg.etf.pp1.ast.IfStatement;
import rs.ac.bg.etf.pp1.ast.IsEqualRelop;
import rs.ac.bg.etf.pp1.ast.Length;
import rs.ac.bg.etf.pp1.ast.LengthDesignator;
import rs.ac.bg.etf.pp1.ast.LessRelop;
import rs.ac.bg.etf.pp1.ast.MethodDecl;
import rs.ac.bg.etf.pp1.ast.MethodTypeName;
import rs.ac.bg.etf.pp1.ast.MinusTermsSumExpr;
import rs.ac.bg.etf.pp1.ast.MoreAddopTerms;
import rs.ac.bg.etf.pp1.ast.MoreFactorsTerm;
import rs.ac.bg.etf.pp1.ast.NewTypeFactor;
import rs.ac.bg.etf.pp1.ast.NotEqualRelop;
import rs.ac.bg.etf.pp1.ast.NumberFactor;
import rs.ac.bg.etf.pp1.ast.PlusAddop;
import rs.ac.bg.etf.pp1.ast.PrintStatementEmpty;
import rs.ac.bg.etf.pp1.ast.PrintStatementParams;
import rs.ac.bg.etf.pp1.ast.ProgName;
import rs.ac.bg.etf.pp1.ast.ReadStatement;
import rs.ac.bg.etf.pp1.ast.RetType;
import rs.ac.bg.etf.pp1.ast.ReturnNonVoidStatement;
import rs.ac.bg.etf.pp1.ast.ReturnVoidStatement;
import rs.ac.bg.etf.pp1.ast.SecondExpr;
import rs.ac.bg.etf.pp1.ast.Semi1For;
import rs.ac.bg.etf.pp1.ast.Semi2For;
import rs.ac.bg.etf.pp1.ast.SwitchAfterExpr;
import rs.ac.bg.etf.pp1.ast.SwitchBeginning;
import rs.ac.bg.etf.pp1.ast.SwitchEnd;
import rs.ac.bg.etf.pp1.ast.TimesMulop;
import rs.ac.bg.etf.pp1.ast.VisitorAdaptor;
import rs.etf.pp1.mj.runtime.Code;
import rs.etf.pp1.symboltable.Tab;
import rs.etf.pp1.symboltable.concepts.Obj;
import rs.etf.pp1.symboltable.concepts.Struct;

public class CodeGenerator extends VisitorAdaptor {

	private int mainPc;
	private Obj program;
	private Vector<Integer> patchAdrCondFact = new Vector<Integer>();
	private Vector<Integer> patchAdrCondTerm = new Vector<Integer>();
	private Vector<Integer> patchAdrCondition = new Vector<Integer>();
	private Vector<Integer> patchAdrColon = new Vector<Integer>();
	private Vector<Integer> patchAdrElse = new Vector<Integer>();
	private Vector<Integer> stJumpForCond = new Vector<>();
	private Vector<Integer> stUpdateStart = new Vector<>();
	private Vector<Integer> stCondFalsePatch = new Vector<>();
	private Vector<Vector<Integer>> stBreakPatches = new Vector<>();
	private Vector<Boolean> stInForCond = new Vector<>();
	private Vector<Integer> stSwitchJmpToDispatch = new Vector<>();
	private Vector<Vector<Integer>> stSwitchCaseVals = new Vector<>();
	private Vector<Vector<Integer>> stSwitchCaseEntry = new Vector<>();
	private Vector<Integer> stSwitchJmpOverDispatch = new Vector<>();

	public int getMainPc() {
		return mainPc;
	}

	public CodeGenerator() {
		Obj chr = Tab.find("chr");
		chr.setAdr(Code.pc);
		Code.put(Code.enter);
		Code.put(1);
		Code.put(1);
		Obj xParam = new Obj(Obj.Var, "x", Tab.intType);
	    xParam.setAdr(0); xParam.setLevel(1);
	    Code.load(new Obj(Obj.Var, "i", Tab.intType, 0, 1));
		Code.put(Code.exit);
		Code.put(Code.return_);

		Obj ord = Tab.find("ord");
		ord.setAdr(Code.pc);
		Code.put(Code.enter);
		Code.put(1);
		Code.put(1);
		Code.load(new Obj(Obj.Var, "ch", Tab.charType, 0, 1));
		Code.put(Code.exit);
		Code.put(Code.return_);

	    Obj len = Tab.find("len");
	    len.setAdr(Code.pc);
	    Code.put(Code.enter);
	    Code.put(1);
	    Code.put(1);
	    Code.put(Code.load_n);
	    Code.put(Code.arraylength);
	    Code.put(Code.exit);
	    Code.put(Code.return_);
	}

	public void visit(ProgName progName) {
		program = progName.obj;
		for (Obj o: program.getLocalSymbols()) {
			if (Obj.Var == o.getKind()) {
				o.setAdr(Code.dataSize++);
				if (o.getType().getKind()==Struct.Array) {
					Code.loadConst(-1);
				}
				else {
					Code.loadConst(0);
				}
				Code.put(Code.putstatic);
				Code.put2(o.getAdr());
			}
		}
	}

	public void visit(PrintStatementParams printStatement) {
		Code.loadConst(printStatement.getNumConst());
		int instruction;
		if (printStatement.getExpr().struct == Tab.intType || printStatement.getExpr().struct == TabExtension.boolType) {
			instruction = Code.print;
		}
		else {
			instruction = Code.bprint;
		}
		Code.put(instruction);
	}

	public void visit(PrintStatementEmpty printStatement) {
		Code.loadConst(1);
		int instruction;
		if (printStatement.getExpr().struct == Tab.intType || printStatement.getExpr().struct == TabExtension.boolType) {
			instruction = Code.print;
		}
		else {
			instruction = Code.bprint;
		}
		Code.put(instruction);
	}

	public void visit(ReadStatement readStatement) {
		int instruction;
		if (readStatement.getDesignator().obj.getType() == Tab.intType || readStatement.getDesignator().obj.getType() == TabExtension.boolType) {
			instruction = Code.read;
		}
		else {
			instruction = Code.bread;
		}
		Code.put(instruction);
		Code.store(readStatement.getDesignator().obj);
	}

	public void visit(NumberFactor numFactor) {
		Code.loadConst(numFactor.getVal());
	}

	public void visit(CharFactor charFactor) {
		Code.loadConst(charFactor.getVal());
	}

	public void visit(BoolFactor boolFactor)
	{
		Code.loadConst(boolFactor.getVal() ? 1 : 0);
	}

	public void visit(MoreFactorsTerm moreFactorsTerm) {
		if (moreFactorsTerm.getMulop() instanceof TimesMulop) Code.put(Code.mul);
		else if (moreFactorsTerm.getMulop() instanceof DividedByMulop) Code.put(Code.div);
		else Code.put(Code.rem);
	}

	public void visit(MoreAddopTerms moreAddopTerms) {
		if (moreAddopTerms.getAddop() instanceof PlusAddop) Code.put(Code.add);
		else Code.put(Code.sub);
	}

	public void visit(MethodTypeName methodName) {
		if (!(methodName.getParent().getParent() instanceof MethodDecl)) {
			return;
		}
		if ("main".equalsIgnoreCase(methodName.getMethodName())) {
			mainPc = Code.pc;
			Code.mainPc = Code.pc;
		}
		methodName.obj.setAdr(Code.pc);
		Code.put(Code.enter);
		Code.put(methodName.obj.getLevel());
		Code.put(methodName.obj.getLocalSymbols().size());
	}

	public void visit(MethodDecl methodDecl) {
		if (methodDecl.getMethodSignature().getMethodTypeName().getTypeOrVoid() instanceof RetType) {
			Code.put(Code.trap);
			Code.put(0);
		}
		Code.put(Code.exit);
		Code.put(Code.return_);
	}

	public void visit(ReturnNonVoidStatement returnStatement) {
		Code.put(Code.exit);
		Code.put(Code.return_);
	}

	public void visit(ReturnVoidStatement returnStatement) {
		Code.put(Code.exit);
		Code.put(Code.return_);
	}

	public void visit(DesignatorAssignopExpr designatorStatement) {
		Code.store(designatorStatement.getDesignator().obj);
	}

	public void visit(DesignatorAssignopExprW designatorStatement) {
		Code.store(designatorStatement.getDesignator().obj);
	}

	public void visit(FactorFuncCall funcCall) {
		int s = -(Code.pc - funcCall.getFunctionName().getDesignator().obj.getAdr());
		Code.put(Code.call);
		Code.put2(s);

	}

	public void visit(NewTypeFactor newTypeFactor) {
		Struct s = newTypeFactor.getType().struct;
		Code.put(Code.newarray);
		if (s == Tab.intType || s == TabExtension.boolType) {
			Code.put(1);
		}
		else {
			Code.put(0);
		}
	}

	public void visit(FactorDesignator factorDesignator) {
		if (!(factorDesignator.getDesignator() instanceof Length)) Code.load(factorDesignator.getDesignator().obj);
	}

	public void visit(ArrayName arrayName) {
		Code.load(arrayName.obj);
	}

	public void visit(ArrayElemAccessDesignator designator) {

	}


	public void visit(LengthDesignator designator) {
		Code.load(designator.obj);
		Code.put(Code.arraylength);
	}

	public void visit(DesignatorIncrement designatorStatement) {
		int kind = designatorStatement.getDesignator().obj.getKind();
		if (kind == Obj.Elem) {
			Code.put(Code.dup2);
		}
		Code.load(designatorStatement.getDesignator().obj);
		Code.loadConst(1);
		Code.put(Code.add);
		Code.store(designatorStatement.getDesignator().obj);
	}

	public void visit(DesignatorIncrementW designatorStatement) {
		int kind = designatorStatement.getDesignator().obj.getKind();
		if (kind == Obj.Elem) {
			Code.put(Code.dup2);
		}
		Code.load(designatorStatement.getDesignator().obj);
		Code.loadConst(1);
		Code.put(Code.add);
		Code.store(designatorStatement.getDesignator().obj);
	}

	public void visit(DesignatorDecrement designatorStatement) {
		int kind = designatorStatement.getDesignator().obj.getKind();
		if (kind == Obj.Elem) {
			Code.put(Code.dup2);
		}
		Code.load(designatorStatement.getDesignator().obj);
		Code.loadConst(1);
		Code.put(Code.sub);
		Code.store(designatorStatement.getDesignator().obj);
	}

	public void visit(DesignatorDecrementW designatorStatement) {
		int kind = designatorStatement.getDesignator().obj.getKind();
		if (kind == Obj.Elem) {
			Code.put(Code.dup2);
		}
		Code.load(designatorStatement.getDesignator().obj);
		Code.loadConst(1);
		Code.put(Code.sub);
		Code.store(designatorStatement.getDesignator().obj);
	}

	public void visit(DesignatorFunction designatorStatement) {
		int s = -(Code.pc - designatorStatement.getFunctionName().getDesignator().obj.getAdr());
		Code.put(Code.call);
		Code.put2(s);
		if (designatorStatement.getFunctionName().getDesignator().obj.getType()!=Tab.noType) {
			Code.put(Code.pop);
		}
	}

	public void visit(DesignatorFunctionW designatorStatement) {
		int s = -(Code.pc - designatorStatement.getFunctionName().getDesignator().obj.getAdr());
		Code.put(Code.call);
		Code.put2(s);
		if (designatorStatement.getFunctionName().getDesignator().obj.getType()!=Tab.noType) {
			Code.put(Code.pop);
		}
	}

	public void visit(MinusTermsSumExpr negative) {
		Code.put(Code.neg);
	}

	public void visit(CondFactExprOnly condFact) {
		Code.put(Code.const_1);
		Code.putFalseJump(Code.eq, 0);
		patchAdrCondFact.add(Code.pc-2);
	}

	public void visit(CondFactRelopExpr condFact) {
		int relop;
		if (condFact.getRelop() instanceof IsEqualRelop) relop = Code.eq;
		else if (condFact.getRelop() instanceof NotEqualRelop) relop = Code.ne;
		else if (condFact.getRelop() instanceof GreaterRelop) relop = Code.gt;
		else if (condFact.getRelop() instanceof GreaterEqualRelop) relop = Code.ge;
		else if (condFact.getRelop() instanceof LessRelop) relop = Code.lt;
		else relop = Code.le;
		Code.putFalseJump(relop, 0);
		patchAdrCondFact.add(Code.pc-2); //false
	}

	public void visit(CondTerm condTerm) {
		Code.putJump(0);
		patchAdrCondTerm.add(Code.pc-2); //true
		while (patchAdrCondFact.size()!=0) {
			Code.fixup(patchAdrCondFact.removeLast()); //false
		}
	}

	public void visit(Condition cond) {
		Code.putJump(0);
		patchAdrCondition.add(Code.pc-2); //false

		if (stInForCond.size() != 0 && (cond.getParent() instanceof HasCondition)) {
	        return;
	    }

		while (patchAdrCondTerm.size()!=0) {
			Code.fixup(patchAdrCondTerm.removeLast()); //true
		}

	}

	public void visit(Colon colon) {
		Code.putJump(0);
		patchAdrColon.add(Code.pc-2);
		Code.fixup(patchAdrCondition.removeLast());
	}

	public void visit(Else elseSt) {
		Code.putJump(0);
		patchAdrElse.add(Code.pc-2);
		Code.fixup(patchAdrCondition.removeLast());
	}

	public void visit(SecondExpr secondExpr) {
		Code.fixup(patchAdrColon.removeLast());
	}

	public void visit(ElsePart elsePart) {
		Code.fixup(patchAdrElse.removeLast());
	}

	public void visit(IfStatement ifStmt) {
		Code.fixup(patchAdrCondition.removeLast());
	}

	public void visit(ForParens forParens) {
	    Code.putJump(stJumpForCond.lastElement());

	    while (patchAdrCondTerm.size() != 0) {
	        Code.fixup(patchAdrCondTerm.removeLast());
	    }
	}

	public void visit(Semi1For semi1) {
		stJumpForCond.add(Code.pc);
		stInForCond.add(true);
	}

	public void visit(Semi2For semi2) {
	    stInForCond.removeLast();
	    stCondFalsePatch.add(patchAdrCondition.removeLast());
	    stUpdateStart.add(Code.pc);
	}

	public void visit(ForStatement forStmt) {
	    Code.putJump(stUpdateStart.lastElement());
	    Code.fixup(stCondFalsePatch.removeLast());
	    Vector<Integer> breaks = stBreakPatches.removeLast();
	    while (breaks.size() != 0) {
	        Code.fixup(breaks.removeLast());
	    }
	    stUpdateStart.removeLast();
	    stJumpForCond.removeLast();
	}

	public void visit(BreakStatement breakStmt) {
	    Code.putJump(0);
	    stBreakPatches.lastElement().add(Code.pc - 2);
	}

	public void visit(ContinueStatement continueStmt) {
	    Code.putJump(stUpdateStart.lastElement());
	}

	public void visit(ForBegin forBegin) {
	    stBreakPatches.add(new Vector<>());
	}

	public void visit(SwitchBeginning sb) {
	    stSwitchJmpToDispatch.add(0);
	    stSwitchCaseVals.add(new Vector<>());
	    stSwitchCaseEntry.add(new Vector<>());
	    stBreakPatches.add(new Vector<>());
	}

	public void visit(SwitchAfterExpr sae) {
	    Code.putJump(0);
	    stSwitchJmpToDispatch.removeLast();
	    stSwitchJmpToDispatch.add(Code.pc - 2);
	}

	public void visit(CaseHeader ch) {
		stSwitchCaseVals.lastElement().add(ch.getN());
	}

	public void visit(ColonSwitch cs) {
	    stSwitchCaseEntry.lastElement().add(Code.pc);
	}

	public void visit(SwitchEnd end) {
	    Code.putJump(0);
	    stSwitchJmpOverDispatch.add(Code.pc - 2);

	    Code.fixup(stSwitchJmpToDispatch.removeLast());

	    Vector<Integer> vals  = stSwitchCaseVals.removeLast();
	    Vector<Integer> entry = stSwitchCaseEntry.removeLast();

	    for (int k = 0; k < vals.size(); k++) {
	        Code.put(Code.dup);
	        Code.loadConst(vals.get(k));

	        Code.putFalseJump(Code.eq, 0);
	        int skip = Code.pc - 2;

	        Code.put(Code.pop);
	        Code.putJump(entry.get(k));

	        Code.fixup(skip);
	    }
	    Code.put(Code.pop);

	    Vector<Integer> breaks = stBreakPatches.removeLast();
	    while (breaks.size() != 0) {
	        Code.fixup(breaks.removeLast());
	    }
	    Code.fixup(stSwitchJmpOverDispatch.removeLast());
	}


}
