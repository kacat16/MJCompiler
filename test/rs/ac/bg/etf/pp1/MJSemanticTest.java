package rs.ac.bg.etf.pp1;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;

import org.apache.log4j.Logger;
import org.apache.log4j.xml.DOMConfigurator;

import java_cup.runtime.Symbol;
import rs.ac.bg.etf.pp1.ast.Program;
import rs.ac.bg.etf.pp1.util.Log4JUtils;
import rs.etf.pp1.symboltable.Tab;

public class MJSemanticTest  {

	static {
		DOMConfigurator.configure(Log4JUtils.instance().findLoggerConfigFile());
		Log4JUtils.instance().prepareLogFile(Logger.getRootLogger());
	}

	public static void main(String[] args) throws Exception {

		Logger log = Logger.getLogger(MJParserTest.class);

		Reader br = null;
		try {
			File sourceCode = new File("test/program.mj");
			log.info("Compiling source file: " + sourceCode.getAbsolutePath());

			br = new BufferedReader(new FileReader(sourceCode));
			Yylex lexer = new Yylex(br);

			MJParser p = new MJParser(lexer);
	        Symbol s = p.parse();  // Start parsing.

	        Program prog = (Program)(s.value);


	        TabExtension.init();

//			 Print the syntax tree.
//			log.info(prog.toString(""));
//			log.info("===================================");

			// Print the recognized program constructs.
			SemanticAnalyzer v = new SemanticAnalyzer();

			prog.traverseBottomUp(v);

			log.info(" Print count calls = " + v.printCallCount);

			log.info(" Deklarisanih promenljivih ima = " + v.varDeclCount);
			log.info("===================================");
			Tab.dump();

			if(!p.errorDetected && v.passed()){
				log.info("Parsiranje uspesno zavrseno!");
			}else{
				log.error("Parsiranje NIJE uspesno zavrseno!");
			}

		}
		finally {
			if (br != null) try { br.close(); } catch (IOException e1) { log.error(e1.getMessage(), e1); }
		}

	}


}
