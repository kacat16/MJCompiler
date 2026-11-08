package rs.ac.bg.etf.pp1;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.io.StringReader;
import java.io.StringWriter;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.apache.log4j.PatternLayout;
import org.apache.log4j.WriterAppender;

import rs.ac.bg.etf.pp1.ast.Program;

public class DiagnosticsTest {
	private static void expect(String output, String message) {
		if (!output.contains(message)) {
			throw new AssertionError("Expected \"" + message + "\" in:\n" + output);
		}
	}

	public static void main(String[] args) throws Exception {
		StringWriter messages = new StringWriter();
		WriterAppender appender = new WriterAppender(new PatternLayout("%m%n"), messages);
		Logger root = Logger.getRootLogger();
		root.addAppender(appender);
		root.setLevel(Level.INFO);
		try {
			ByteArrayOutputStream lexicalOutput = new ByteArrayOutputStream();
			PrintStream originalError = System.err;
			try {
				System.setErr(new PrintStream(lexicalOutput, true, "UTF-8"));
				new Yylex(new StringReader("@")).next_token();
			} finally {
				System.setErr(originalError);
			}
			expect(lexicalOutput.toString("UTF-8"), "Lexical error (@) at line 1, column 1");

			try {
				new MJParser(new Yylex(new StringReader("program p { void main( }"))).parse();
			} catch (Exception expected) {
				// An unrecoverable syntax error may abort parsing.
			}
			expect(messages.toString(), "Syntax error at line 1");

			messages.getBuffer().setLength(0);
			try {
				new MJParser(new Yylex(new StringReader(
						"program p const int n = ; { void main() { } }"))).parse();
			} catch (Exception expected) {
				// Parsing may still abort after error recovery.
			}
			expect(messages.toString(), "Recovered from an error in the constant definition");

			messages.getBuffer().setLength(0);
			String source = "program p\n"
					+ "int global;\n"
					+ "int globalArray[];\n"
					+ "{\n"
					+ "void f(int x) { }\n"
					+ "void main()\n"
					+ "int local;\n"
					+ "int localArray[];\n"
					+ "{ f(1, 2); print(missing); }\n"
					+ "}";
			Program program = (Program) new MJParser(new Yylex(new StringReader(source))).parse().value;
			TabExtension.init();
			SemanticAnalyzer analyzer = new SemanticAnalyzer();
			program.traverseBottomUp(analyzer);
			String output = messages.toString();
			expect(output, "Declared global variable named global");
			expect(output, "Declared global array named globalArray");
			expect(output, "Declared local variable named local");
			expect(output, "Declared local array named localArray");
			expect(output, "Function f has 2 arguments; the definition expects 1.");
			expect(output, "Undeclared name: missing. at line 9");
			if (analyzer.passed()) {
				throw new AssertionError("Expected semantic errors");
			}
			System.out.println("Diagnostic tests passed.");
		} finally {
			root.removeAppender(appender);
			appender.close();
		}
	}
}
