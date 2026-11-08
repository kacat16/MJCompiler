package rs.ac.bg.etf.pp1;
import java.util.Collection;
import java.util.HashSet;
import java.util.Stack;
import java.util.Vector;

import org.apache.log4j.Logger;

import rs.ac.bg.etf.pp1.ast.*;
import rs.ac.bg.etf.pp1.ast.Enum;
import rs.etf.pp1.symboltable.*;

import rs.etf.pp1.symboltable.concepts.*;
import rs.etf.pp1.symboltable.structure.HashTableDataStructure;
import rs.etf.pp1.symboltable.structure.SymbolDataStructure;

public class SemanticAnalyzer extends VisitorAdaptor {

	int printCallCount = 0;
	int varDeclCount = 0;
	boolean errorDetected;
	Obj currentMethod = null;
	Struct currType = null;
	int numVal;
	char charVal;
	boolean boolVal;
	boolean array;
	int lastEnumVal = -1;
	String designatorName = null;
	Obj mainMeth = null;
	int relop;
	int forLoop = 0;
	int nVars;
	enum Breakable {
		FOR,
		SWITCH
	}
	Vector<Breakable> stackB = new Vector<Breakable>();

	Vector<Vector<Struct>> actPars = new Vector<Vector<Struct>>();
	Stack<HashSet<Integer>> caseNumbersCurrentSwitch = new Stack<>();
	SymbolDataStructure enumMembers = null;
	Obj enumObj = null;
	Logger log = Logger.getLogger(getClass());


	public void report_error(String message, SyntaxNode info) {
		errorDetected = true;
		StringBuilder msg = new StringBuilder(message);
		int line;
		if (info == null) line = 0;
		else line = info.getLine();
		if (line != 0) msg.append(" at line ").append(line);
		log.error(msg.toString());
	}

	public void report_info(String message, SyntaxNode info) {
		StringBuilder msg = new StringBuilder(message);
		int line;
		if (info == null) line = 0;
		else line = info.getLine();
		if (line != 0) msg.append(" at line ").append(line);
		log.info(msg.toString());
	}
	public void visit(ProgName progName) {
		progName.obj = Tab.insert(Obj.Prog, progName.getProgName(), Tab.noType);
		Tab.openScope();
	}

	public void visit(Program program) {
		nVars = Tab.currentScope.getnVars();
		Tab.chainLocalSymbols(program.getProgName().obj);
		Tab.closeScope();
		if (mainMeth == null) {
			report_error("The program has no main method.", program);
		}
		else if (mainMeth.getLevel() > 0) report_error("The main method must not have arguments.", program);
	}

	public void visit(Type type) {
		Obj typeNode = Tab.find(type.getTypeName());
		if (typeNode == Tab.noObj) {
			report_error("Type not found: " + type.getTypeName() + " in the symbol table.", type);
			type.struct = Tab.noType;
		}
		else {
			if (typeNode.getKind() != Obj.Type) {
				report_error("Name " + type.getTypeName() + " is not a type.", type);
				type.struct = Tab.noType;
			}
			else {
				type.struct = typeNode.getType();
			}
		}
	}

	public void visit(TypeConstDecl typeConstDecl) {
		if (typeConstDecl.getType().struct != Tab.intType && typeConstDecl.getType().struct != Tab.charType && typeConstDecl.getType().struct != TabExtension.boolType) {
			report_error("The declared constant has an unsupported type.", typeConstDecl);
			return;
		}
		currType = typeConstDecl.getType().struct;

	}

	public void visit(NumberConst number) {
		numVal = number.getNumVal();
	}

	public void visit(CharConst character) {
		charVal = character.getCharVal();
	}

	public void visit(BoolConst bool) {
		boolVal = bool.getBoolVal();
	}

	public void visit(Const constPart) {
		if (Tab.find(constPart.getConstName()) != Tab.noObj) {
			report_error("A constant is already declared with the name " + constPart.getConstName() + ".", constPart);
			return;
		}
		if (constPart.getConstType() instanceof NumberConst) {
			if (currType == Tab.intType) {
				Obj constObj = Tab.insert(Obj.Con, constPart.getConstName(), currType);
				constObj.setAdr(numVal);
				report_info("Declared constant named " + constPart.getConstName(), constPart);
			}
			else {
				report_error("Constant " + constPart.getConstName() + " has the wrong type.", constPart);
				return;
			}
		}
		else if (constPart.getConstType() instanceof CharConst) {
			if (currType == Tab.charType) {
				Obj constObj = Tab.insert(Obj.Con, constPart.getConstName(), currType);
				constObj.setAdr(charVal);
				report_info("Declared constant named " + constPart.getConstName(), constPart);
			}
			else {
				report_error("Constant " + constPart.getConstName() + " has the wrong type.", constPart);
				return;
			}
		}
		else {
			if (currType == TabExtension.boolType) {
				Obj constObj = Tab.insert(Obj.Con, constPart.getConstName(), currType);
				constObj.setAdr(boolVal?1:0);
				report_info("Declared constant named " + constPart.getConstName(), constPart);
			}
			else {
				report_error("Constant " + constPart.getConstName() + " has the wrong type.", constPart);
				return;
			}
		}
	}

	public void visit(ConstDeclaration constDecl) {
		currType = null;
	}

	public void visit(TypeVarDecl typeVarDecl) {
		currType = typeVarDecl.getType().struct;
	}

	public void visit(VarElem var) {
		if (Tab.currentScope.findSymbol(var.getVarName()) != null) {
			report_error("A variable is already declared with the name " + var.getVarName() + ".", var);
			return;
		}
		Obj varObj = Tab.insert(Obj.Var, var.getVarName(), array == true ? new Struct(Struct.Array, currType) : currType);
		if (currentMethod != null) {
			report_info("Declared local " + (array ? "array":"variable") + " named " + var.getVarName(), var);
			varObj.setFpPos(Tab.currentScope.getnVars()-1);
			varObj.setLevel(1);
		}
		else {
			varObj.setLevel(0);
			report_info("Declared global " + (array ? "array":"variable") + " named " + var.getVarName(), var);
		}
		array = false;
	}

	public void visit (VarDeclaration varDecl) {
		varDeclCount++;
		currType = null;
	}

	public void visit(Enum enumeration) {
		Obj enumConstObj = new Obj(Obj.Con, enumeration.getI1(), Tab.intType);
		int value;
		if (enumeration.getEnumMemberDef() instanceof EnumMemberDefinition) {
			value = ((EnumMemberDefinition)enumeration.getEnumMemberDef()).getN1();
			lastEnumVal = value;
		}
		else value = ++lastEnumVal;
		enumConstObj.setAdr(value);
		Collection<Obj> nodes = enumMembers.symbols();
		for(Obj o: nodes) {
			if (o.getAdr() == value) {
				report_error("An enum constant is already declared with the value " + value + ".", enumeration);
				return;
			}
		}
		if (!enumMembers.insertKey(enumConstObj)) {
			report_error("An enum constant is already declared with the name " + enumeration.getI1() + ".", enumeration);
		}
    }

	public void visit(EnumDeclaration enumDecl) {
		if (Tab.find(enumDecl.getI1())!= Tab.noObj) {
			report_error("Name is already declared: " + enumDecl.getI1() + ".", enumDecl);
			return;
		}
		enumObj = Tab.insert(Obj.Type, enumDecl.getI1(), new Struct(Struct.Enum));
		enumMembers = new HashTableDataStructure();
	}

	public void visit(OneEnum oneEnum) {
		enumObj.getType().setMembers(enumMembers);
	}

	// Functions
	public void visit(MethodTypeName methodTypeName) {
		currentMethod = Tab.insert(Obj.Meth, methodTypeName.getMethodName(), methodTypeName.getTypeOrVoid().struct);
		if ("main".equalsIgnoreCase(methodTypeName.getMethodName())) mainMeth = currentMethod;
		methodTypeName.obj = currentMethod;
		Tab.openScope();
		report_info("Declared function " + methodTypeName.getMethodName(), methodTypeName);
	}

	public void visit (RetType type) {
		type.struct = type.getType().struct;
	}

	public void visit (RetVoid typeVoid) {
		typeVoid.struct = Tab.noType;
	}

	public void visit(MethodDecl methodDecl) {
		Tab.chainLocalSymbols(currentMethod);
		Tab.closeScope();
		currentMethod = null;
	}

	public void visit(MethodSignature methodSignature) {
		currentMethod.setAdr(Tab.currentScope.getnVars());
		currentMethod.setLevel(Tab.currentScope.getnVars());
	}

	public void visit(FormParam formParameter) {
		if (Tab.find(formParameter.getFormParamName()) != Tab.noObj && Tab.currentScope.findSymbol(formParameter.getFormParamName()) != null) {
			report_error("A parameter is already declared with the name " + formParameter.getFormParamName() + ".", formParameter);
			return;
		}
		Obj objVar = Tab.insert(Obj.Var, formParameter.getFormParamName(), array == true ? new Struct(Struct.Array, formParameter.getType().struct) : formParameter.getType().struct);
		objVar.setFpPos(Tab.currentScope.getnVars()-1);
		array = false;
	}

	public void visit(Array arr) {
		array = true;
	}

	public void visit(FunctionName funcName) {
		actPars.add(new Vector<Struct>());
	}

	public void visit(OneActPar actPar) {
		actPars.lastElement().add(actPar.getExpr().struct);
	}

	public void visit(MoreActPars actParams) {
		actPars.lastElement().add(actParams.getExpr().struct);
	}

	public void visit(DesignatorFunction designatorFunc) {
		if (designatorFunc.getFunctionName().getDesignator().obj == Tab.noObj) {
			report_error("The function is not declared.", designatorFunc);
			return;
		}
		if (designatorFunc.getFunctionName().getDesignator().obj.getKind() != Obj.Meth) {
			report_error("Name " + designatorFunc.getFunctionName().getDesignator().obj.getName() + " is not a function.", designatorFunc);
			return;
		}
		int formParamsLen = designatorFunc.getFunctionName().getDesignator().obj.getLevel();
		Collection<Obj> formParameters = designatorFunc.getFunctionName().getDesignator().obj.getLocalSymbols();
		if (actPars.lastElement().size() != formParamsLen) {
			report_error("Function " + designatorFunc.getFunctionName().getDesignator().obj.getName() + " has " + actPars.lastElement().size() +" arguments; the definition expects " + formParamsLen + ".", designatorFunc);
			return;
		}
		int iterator = 0;
		for (Obj o: formParameters) {
			if (iterator == formParamsLen) break;
			if (!compareTypes(actPars.lastElement().get(iterator), o.getType())) {
				report_error("Argument and parameter at position " + iterator + " are incompatible.", designatorFunc);
				return;
			}
			iterator++;
		}
		int lastIndex = actPars.size()-1;
		actPars.remove(lastIndex);
		report_info("Called function " + designatorFunc.getFunctionName().getDesignator().obj.getName(), designatorFunc);
	}

	public void visit(DesignatorFunctionW designatorFunc) {
		if (designatorFunc.getFunctionName().getDesignator().obj == Tab.noObj) {
			report_error("The function is not declared.", designatorFunc);
			return;
		}
		if (designatorFunc.getFunctionName().getDesignator().obj.getKind() != Obj.Meth) {
			report_error("Name " + designatorFunc.getFunctionName().getDesignator().obj.getName() + " is not a function.", designatorFunc);
			return;
		}
		int formParamsLen = designatorFunc.getFunctionName().getDesignator().obj.getLevel();
		Collection<Obj> formParameters = designatorFunc.getFunctionName().getDesignator().obj.getLocalSymbols();
		if (actPars.lastElement().size() != formParamsLen) {
			report_error("Function " + designatorFunc.getFunctionName().getDesignator().obj.getName() + " has " + actPars.lastElement().size() +" arguments; the definition expects " + formParamsLen + ".", designatorFunc);
			return;
		}
		int iterator = 0;
		for (Obj o: formParameters) {
			if (iterator == formParamsLen) break;
			if (!compareTypes(actPars.lastElement().get(iterator), o.getType())) {
				report_error("Argument and parameter at position " + iterator + " are incompatible.", designatorFunc);
				return;
			}
			iterator++;
		}
		int lastIndex = actPars.size()-1;
		actPars.remove(lastIndex);
		report_info("Called function " + designatorFunc.getFunctionName().getDesignator().obj.getName(), designatorFunc);
	}

	public void visit(FactorFuncCall factorFunction) {
		if (factorFunction.getFunctionName().getDesignator().obj == Tab.noObj) {
			report_error("The function is not declared.", factorFunction);
			return;
		}
		if (factorFunction.getFunctionName().getDesignator().obj.getKind() != Obj.Meth) {
			report_error("Name " + factorFunction.getFunctionName().getDesignator().obj.getName() + " is not a function.", factorFunction);
			return;
		}
		if (factorFunction.getFunctionName().getDesignator().obj.getType() == Tab.noType) {
			report_error("Function used in an expression " + factorFunction.getFunctionName().getDesignator().obj.getName() + " returns void.", factorFunction);
			return;
		}
		int formParamsLen = factorFunction.getFunctionName().getDesignator().obj.getLevel();
		Collection<Obj> formParameters = factorFunction.getFunctionName().getDesignator().obj.getLocalSymbols();
		if (actPars.lastElement().size() != formParamsLen) {
			report_error("Function " + factorFunction.getFunctionName().getDesignator().obj.getName() + " has " + actPars.lastElement().size() +" arguments; the definition expects " + formParamsLen + ".", factorFunction);
			return;
		}
		int iterator = 0;
		for (Obj o: formParameters) {
			if (iterator == formParamsLen) break;
			if (!compareTypes(actPars.lastElement().get(iterator), o.getType())) {
				report_error("Argument and parameter at position " + iterator + " are incompatible.", factorFunction);
				return;
			}
			iterator++;
		}
		int lastIndex = actPars.size()-1;
		actPars.remove(lastIndex);
		factorFunction.struct = factorFunction.getFunctionName().getDesignator().obj.getType();
		report_info("Called function " + factorFunction.getFunctionName().getDesignator().obj.getName(), factorFunction);
	}

	private boolean compareTypes(Struct s1, Struct s2) {
		if (!s1.assignableTo(s2) && !s1.compatibleWith(s2)) return false;
		if (s1.getKind() == Struct.Array && s2.getKind() == Struct.Array) return compareTypes(s1.getElemType(), s2.getElemType());
		return true;
	}

	public void visit(OneDesignator designator) {
		if (Tab.find(designator.getI1()) == Tab.noObj) {
			report_error("Undeclared name: " + designator.getI1() + ".", designator);
			designator.obj = Tab.noObj;
			return;
		}
		designator.obj = Tab.find(designator.getI1());
		report_info("Accessing designator " + designator.getI1(), designator);

	}

	public void visit(ArrayElemAccessDesignator arrayDesignator) {
		arrayDesignator.obj = Tab.noObj;
		Struct structNode = arrayDesignator.getArrayName().obj.getType();
		if (structNode.getKind() != Struct.Array) {
			report_error("Declared name " + arrayDesignator.getArrayName().obj.getName() + " is not an array.", arrayDesignator);
			return;
		}
		if (arrayDesignator.getExpr().struct.getKind() != Struct.Int && arrayDesignator.getExpr().struct.getKind() != Struct.Enum) {
			report_error("The array index is not an integer.", arrayDesignator);
			return;
		}
		String arrayName = arrayDesignator.getArrayName().obj.getName();
		arrayDesignator.obj = new Obj(Obj.Elem, arrayName, structNode.getElemType());
		report_info("Accessing an element of array " + arrayName, arrayDesignator);
	}

	public void visit(FieldAccessDesignator accessDesignator) {
		accessDesignator.obj = Tab.noObj;
		if (Tab.find(accessDesignator.getI1()) == Tab.noObj) {
			report_error("Undeclared name: " + accessDesignator.getI1() + ".", accessDesignator);
			return;
		}
		Obj designatorObj = Tab.find(accessDesignator.getI1());
		if (designatorObj.getType().getKind() != Struct.Enum) {
			report_error("Declared name " + designatorObj.getName() + " is not an enumeration.", accessDesignator);
			return;
		}
		if (designatorObj.getType().getMembersTable().searchKey(accessDesignator.getNxtDesignatorName()) == null) {
			report_error("Enumeration " + designatorObj.getName() + " has no constant named " + accessDesignator.getNxtDesignatorName() + ".", accessDesignator);
			return;
		}
		accessDesignator.obj = designatorObj.getType().getMembersTable().searchKey(accessDesignator.getNxtDesignatorName());
	}

	public void visit (ArrayAccessDesignator designator) {
		designator.obj = designator.getArrayElemAccessDesignator().obj;
	}

	public void visit(Length designator) {
		designator.obj = new Obj(Obj.Con, "duzina", Tab.intType);
	}

	public void visit(LengthDesignator lengthDesignator) {
		lengthDesignator.obj = Tab.noObj;
		if (Tab.find(lengthDesignator.getI1()) == Tab.noObj) {
			report_error("Undeclared name: " + lengthDesignator.getI1() + ".", lengthDesignator);
			return;
		}
		Obj designatorObj= Tab.find(lengthDesignator.getI1());
		if (designatorObj.getType().getKind() != Struct.Array) {
			report_error("Declared name " + designatorObj.getName() + " is not an array, so its length cannot be accessed.", lengthDesignator);
			return;
		}
		lengthDesignator.obj = designatorObj;
	}

	public void visit(ArrayName arrayName) {
		if (Tab.find(arrayName.getI1()) == Tab.noObj) {
			report_error("Undeclared name: " + arrayName.getI1() + ".", arrayName);
			arrayName.obj = Tab.noObj;
			return;
		}
		arrayName.obj = Tab.find(arrayName.getI1());
		report_info("Accessing designator " + arrayName.getI1(), arrayName);
	}

	public void visit(DesignatorAssignopExpr designatorStatement) {
		if (designatorStatement.getDesignator().obj.getKind() != Obj.Var && designatorStatement.getDesignator().obj.getKind() != Obj.Elem && designatorStatement.getDesignator().obj.getKind() != Obj.Fld) {
			report_error("Declared name " + designatorStatement.getDesignator().obj.getName() + " is not a variable, array element, or class field.", designatorStatement);
			return;
		}
		if (!compareTypes(designatorStatement.getExpr().struct, designatorStatement.getDesignator().obj.getType()) && !(designatorStatement.getDesignator().obj.getType().getKind()==Struct.Enum && designatorStatement.getExpr().struct.getKind() == Struct.Int)) {
			report_error("The types are incompatible.", designatorStatement);
			return;
		}
	}

	public void visit(DesignatorAssignopExprW designatorStatement) {
		if (designatorStatement.getDesignator().obj.getKind() != Obj.Var && designatorStatement.getDesignator().obj.getKind() != Obj.Elem && designatorStatement.getDesignator().obj.getKind() != Obj.Fld) {
			report_error("Declared name " + designatorStatement.getDesignator().obj.getName() + " is not a variable, array element, or class field.", designatorStatement);
			return;
		}
		if (!compareTypes(designatorStatement.getExpr().struct, designatorStatement.getDesignator().obj.getType())) {
			report_error("The types are incompatible.", designatorStatement);
			return;
		}
	}

	public void visit(DesignatorIncrement designatorStatement) {
		if (designatorStatement.getDesignator().obj.getType().getKind() != Struct.Int) {
			report_error("Declared name " + designatorStatement.getDesignator().obj.getName() + " is not an integer and cannot be incremented.", designatorStatement);
			return;
		}
		if (designatorStatement.getDesignator().obj.getKind() != Obj.Var && designatorStatement.getDesignator().obj.getKind() != Obj.Elem && designatorStatement.getDesignator().obj.getKind() != Obj.Fld) {
			report_error("Declared name " + designatorStatement.getDesignator().obj.getName() + " is not a variable, array element, or class field.", designatorStatement);
			return;
		}
	}

	public void visit(DesignatorIncrementW designatorStatement) {
		if (designatorStatement.getDesignator().obj.getType().getKind() != Struct.Int) {
			report_error("Declared name " + designatorStatement.getDesignator().obj.getName() + " is not an integer and cannot be incremented.", designatorStatement);
			return;
		}
		if (designatorStatement.getDesignator().obj.getKind() != Obj.Var && designatorStatement.getDesignator().obj.getKind() != Obj.Elem && designatorStatement.getDesignator().obj.getKind() != Obj.Fld) {
			report_error("Declared name " + designatorStatement.getDesignator().obj.getName() + " is not a variable, array element, or class field.", designatorStatement);
			return;
		}
	}

	public void visit(DesignatorDecrement designatorStatement) {
		if (designatorStatement.getDesignator().obj.getType().getKind() != Struct.Int) {
			report_error("Declared name " + designatorStatement.getDesignator().obj.getName() + " is not an integer and cannot be decremented.", designatorStatement);
			return;
		}
		if (designatorStatement.getDesignator().obj.getKind() != Obj.Var && designatorStatement.getDesignator().obj.getKind() != Obj.Elem && designatorStatement.getDesignator().obj.getKind() != Obj.Fld) {
			report_error("Declared name " + designatorStatement.getDesignator().obj.getName() + " is not a variable, array element, or class field.", designatorStatement);
			return;
		}
	}

	public void visit(DesignatorDecrementW designatorStatement) {
		if (designatorStatement.getDesignator().obj.getType().getKind() != Struct.Int) {
			report_error("Declared name " + designatorStatement.getDesignator().obj.getName() + " is not an integer and cannot be decremented.", designatorStatement);
			return;
		}
		if (designatorStatement.getDesignator().obj.getKind() != Obj.Var && designatorStatement.getDesignator().obj.getKind() != Obj.Elem && designatorStatement.getDesignator().obj.getKind() != Obj.Fld) {
			report_error("Declared name " + designatorStatement.getDesignator().obj.getName() + " is not a variable, array element, or class field.", designatorStatement);
			return;
		}
	}

	public void visit(PrintStatementParams printStatement) {
		printCallCount++;
		if (printStatement.getExpr().struct != Tab.intType && printStatement.getExpr().struct != Tab.charType && printStatement.getExpr().struct != TabExtension.boolType) {
			report_error("The expression passed to print is not an int, char, bool, or set.", printStatement);
			return;
		}
	}

	public void visit(PrintStatementEmpty printStatement) {
		printCallCount++;
		if (printStatement.getExpr().struct != Tab.intType && printStatement.getExpr().struct != Tab.charType && printStatement.getExpr().struct != TabExtension.boolType) {
			report_error("The expression passed to print is not an int, char, or bool.", printStatement);
			return;
		}
	}

	public void visit(ReadStatement readStatement) {
		if (readStatement.getDesignator().obj.getKind() != Obj.Var && readStatement.getDesignator().obj.getKind() != Obj.Elem && readStatement.getDesignator().obj.getKind() != Obj.Fld) {
			report_error("Declared name " + readStatement.getDesignator().obj.getName() + " is not a variable, array element, or class field.", readStatement);
			return;
		}
		if (readStatement.getDesignator().obj.getType() != Tab.intType && readStatement.getDesignator().obj.getType() != Tab.charType && readStatement.getDesignator().obj.getType() != TabExtension.boolType) {
			report_error("Declared name " + readStatement.getDesignator().obj.getName() + " is not an int, char, or bool.", readStatement);
		}
	}

	public void visit(ReturnNonVoidStatement returnStatement) {
		if (currentMethod == null) {
			report_error("A return statement cannot appear outside a method.", returnStatement);
			return;
		}
		if (currentMethod.getType().equals(returnStatement.getExpr().struct) == false) {
			report_error("The return expression type does not match the method's return type.", returnStatement);
			return;
		}
	}

	public void visit(ReturnVoidStatement returnStatement) {
		if (currentMethod == null) {
			report_error("A return statement cannot appear outside a method.", returnStatement);
			return;
		}
		if (currentMethod.getType() != Tab.noType) {
			report_error("A non-void method must return a value.", returnStatement);
			return;
		}
	}

	public void visit(CondFactRelopExpr condFact) {
		if (condFact.getNonTernaryExpr().struct.compatibleWith(condFact.getNonTernaryExpr1().struct) == false) {
			condFact.struct = Tab.noType;
			report_error("The compared types are incompatible.", condFact);
			return;
		}
		if ((condFact.getNonTernaryExpr().struct.getKind() == Struct.Class || condFact.getNonTernaryExpr().struct.getKind() == Struct.Array) && relop!=0 && relop!=1) {
			condFact.struct = Tab.noType;
			report_error("Only != and == can compare classes and arrays.", condFact);
			return;
		}
		condFact.struct = TabExtension.boolType;
		relop = -1;
	}

	public void visit(IsEqualRelop isEqRelop) {
		relop = 0;
	}

	public void visit(NotEqualRelop notEqRelop) {
		relop = 1;
	}

	public void visit(GreaterRelop greaterRelop) {
		relop = 2;
	}

	public void visit(GreaterEqualRelop greaterEqRelop) {
		relop = 3;
	}

	public void visit(LessRelop lessRelop) {
		relop = 4;
	}

	public void visit(LessEqualRelop lessEqRelop) {
		relop = 5;
	}

	public void visit(CondFactExprOnly condFact) {
		if (condFact.getNonTernaryExpr().struct != TabExtension.boolType) {
			condFact.struct = Tab.noType;
			report_error("The condition is not a boolean.", condFact);
			return;
		}
		condFact.struct = TabExtension.boolType;
	}

	public void visit(ForBegin forStmt) {
		forLoop++;
		stackB.add(Breakable.FOR);
		report_info("Stack size: "+ stackB.size(),forStmt);
		report_info("Number of for loops: " + forLoop, forStmt);
	}

	public void visit(BreakStatement breakStatement) {
		if (stackB.size() < 1) {
			report_error("A break statement cannot appear outside a for loop or switch.", breakStatement);
			return;
		}
	}

	public void visit(ForStatement forStmt) {
		forLoop--;
		report_info("Stack size: "+ stackB.size(),forStmt);
		stackB.remove(stackB.size()-1);
		report_info("Number of for loops: " + forLoop, forStmt);
	}

	public void visit(ContinueStatement continueStatement) {
		report_info("Number of for loops: " + forLoop, continueStatement);
		if (forLoop < 1) {
			report_error("A continue statement cannot appear outside a for loop.", continueStatement);
			return;
		}
	}

	public void visit(SwitchStatement switchStatement) {
		if (switchStatement.getExpr().struct != Tab.intType) {
			report_error("The switch expression is not an integer.", switchStatement);
			return;
		}
		stackB.remove(stackB.size()-1);

	}

	public void visit(MoreCases cases) {
		if (caseNumbersCurrentSwitch.peek().contains(cases.getCaseItem().getCaseHeader().getN())) {
			report_error("Multiple case branches cannot have the same integer constant.", cases);
			return;
		}
		else {
			caseNumbersCurrentSwitch.peek().add(cases.getCaseItem().getCaseHeader().getN());
		}
	}

	public void visit (SwitchBeginning switchBeginning) {
		caseNumbersCurrentSwitch.push(new HashSet<>());
		stackB.add(Breakable.SWITCH);
	}

	public void visit(SwitchEnd end) {
		caseNumbersCurrentSwitch.pop();
	}

	public void visit(NewTypeFactor newFactor) {
		newFactor.struct = new Struct(Struct.Array, newFactor.getType().struct);
	}

	public void visit(ExprBrackets exprBrackets) {
		if (exprBrackets.getExpr().struct != Tab.intType) {
			report_error("The expression is not an int.", exprBrackets);
			return;
		}
	}

	public void visit(OneCondFactCondTerm condTerm) {
		condTerm.struct = condTerm.getCondFact().struct;
	}

	public void visit(MoreCondFactsCondTerm condTerm) {
		if (condTerm.getCondTermP().struct!=TabExtension.boolType || condTerm.getCondFact().struct!=TabExtension.boolType) {
			condTerm.struct = Tab.noType;
			report_error("An operand of the AND operation is not a bool.", condTerm);
			return;
		}
		condTerm.struct = TabExtension.boolType;
	}

	public void visit(CondTerm condTerm) {
		condTerm.struct = condTerm.getCondTermP().struct;
	}

	public void visit(OneCondTermCondition condition) {
		condition.struct = condition.getCondTerm().struct;
	}

	public void visit(MoreCondTermsCondition condition) {
		if (condition.getConditionP().struct!=TabExtension.boolType || condition.getCondTerm().struct!=TabExtension.boolType) {
			condition.struct = Tab.noType;
			report_error("An operand of the OR operation is not a bool.", condition);
			return;
		}
		condition.struct = TabExtension.boolType;
	}

	public void visit(Condition condition) {
		condition.struct = condition.getConditionP().struct;
	}

	public void visit(ExprNotTernary nonTernaryExpr) {
		nonTernaryExpr.struct = nonTernaryExpr.getNonTernaryExpr().struct;
	}

	public void visit(ExprTernary ternaryExpr) {
		ternaryExpr.struct = ternaryExpr.getTernaryExpr().struct;
	}

	public void visit(TernaryExpr ternaryExpr) {
		ternaryExpr.struct = ternaryExpr.getTernaryExpressions().getExpr().struct;
	}

	public void visit(MinusTermsSumExpr minusExpr) {
		minusExpr.struct = minusExpr.getAddopTermList().struct;
	}

	public void visit(TermsSumExpr sumExpr) {
		sumExpr.struct = sumExpr.getAddopTermList().struct;
	}

	public void visit(SingleTerm addopTerm) {
		addopTerm.struct = addopTerm.getTerm().struct;
	}

	public void visit(MoreAddopTerms addopTerms) {
		if ((addopTerms.getTerm().struct != Tab.intType && addopTerms.getTerm().struct.getKind()!=Struct.Enum) || (addopTerms.getAddopTermList().struct!=Tab.intType && addopTerms.getAddopTermList().struct.getKind()!=Struct.Enum)) {
			report_error("An addend is not an int.", addopTerms);
			return;
		}
		addopTerms.struct = Tab.intType;
	}

	public void visit(TernaryExpressions ternaryExpressions) {
		if (ternaryExpressions.getExpr().struct != ternaryExpressions.getSecondExpr().getExpr().struct) {
			report_error("The result expressions of the ternary operator must have the same type.", ternaryExpressions);
			return;
		}
	}

	public void visit(OneFactorTerm factor) {
		factor.struct = factor.getFactor().struct;
	}

	public void visit(MoreFactorsTerm mulopFactor) {
		if ((mulopFactor.getFactor().struct != Tab.intType && mulopFactor.getFactor().struct.getKind()!=Struct.Enum) || (mulopFactor.getTerm().struct != Tab.intType && mulopFactor.getTerm().struct.getKind()!=Struct.Enum)) {
			report_error("A factor is not an int.", mulopFactor);
			return;
		}
		mulopFactor.struct = Tab.intType;
	}

	public void visit(ExprParensFactor exprParensFactor) {
		exprParensFactor.struct = exprParensFactor.getExpr().struct;
	}

	public void visit(FactorDesignator factorDesignator) {
		factorDesignator.struct = factorDesignator.getDesignator().obj.getType();
	}

	public void visit(NumberFactor numFactor) {
		numFactor.struct = Tab.intType;
	}

	public void visit(CharFactor charFactor) {
		charFactor.struct = Tab.charType;
	}

	public void visit(BoolFactor boolFactor) {
		boolFactor.struct = TabExtension.boolType;
	}

	public boolean passed() {
		return !errorDetected;
	}


}
