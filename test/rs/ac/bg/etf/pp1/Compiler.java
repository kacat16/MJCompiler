package rs.ac.bg.etf.pp1;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import java.util.ArrayList;
import java.util.List;

import java_cup.runtime.Symbol;

import org.apache.log4j.Logger;
import org.apache.log4j.xml.DOMConfigurator;

import rs.ac.bg.etf.pp1.ast.*;
import rs.ac.bg.etf.pp1.util.Log4JUtils;
import rs.etf.pp1.mj.runtime.Code;
import rs.etf.pp1.symboltable.*;
import rs.etf.pp1.symboltable.concepts.Obj;
import rs.etf.pp1.symboltable.concepts.Struct;

public class Compiler {

	static {
		DOMConfigurator.configure(Log4JUtils.instance().findLoggerConfigFile());
		Log4JUtils.instance().prepareLogFile(Logger.getRootLogger());
	}

	public static void main(String[] args) throws Exception {

		Logger log = Logger.getLogger(Compiler.class);

		Reader br = null;
		try {
			File sourceCode = new File("test/program.mj");
			log.info("Compiling source file: " + sourceCode.getAbsolutePath());

			br = new BufferedReader(new FileReader(sourceCode));
			Yylex lexer = new Yylex(br);

			/* Build the AST. */
			MJParser p = new MJParser(lexer);
	        Symbol s = p.parse();

	        Program prog = (Program)(s.value);

			/* Print the AST. */
			log.info(prog.toString(""));
			log.info("=====================================================================");

			/* Initialize the symbol table. */
			TabExtension.init();
			//Struct boolType = new Struct(Struct.Bool);
			//Obj boolObj = Tab.insert(Obj.Type, "bool", boolType);
			//boolObj.setAdr(-1);
			//boolObj.setLevel(-1);
			List<String> uni_meths = new ArrayList<>();
			uni_meths.add("chr");
			uni_meths.add("ord");
			uni_meths.add("len");
			for(String meth: uni_meths)
				for(Obj fp: Tab.find(meth).getLocalSymbols())
					fp.setFpPos(1);

			/* Perform semantic analysis. */
			SemanticAnalyzer sa = new SemanticAnalyzer();
			prog.traverseBottomUp(sa);

			/* Print the symbol table. */
			log.info("=====================================================================");
			Tab.dump();

			if(!p.errorDetected && sa.passed()){
				/* Generate bytecode. */
				File objFile = new File("test/program.obj");
				if(objFile.exists()) objFile.delete();

				CodeGenerator cg = new CodeGenerator();
				prog.traverseBottomUp(cg);
				Code.dataSize = sa.nVars;
				Code.mainPc = cg.getMainPc();
				Code.write(new FileOutputStream(objFile));

				log.info("Generisanje uspesno zavrseno!");
			}else{
				log.error("Parsiranje NIJE uspesno zavrseno!");
			}

		}
		finally {
			if (br != null) try { br.close(); } catch (IOException e1) { log.error(e1.getMessage(), e1); }
		}

	}


}
