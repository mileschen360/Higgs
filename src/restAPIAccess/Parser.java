package restAPIAccess;

import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTParser;
import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.MethodInvocation;
import org.eclipse.jdt.core.dom.VariableDeclarationStatement;
import org.neo4j.kernel.StoreLockException;

import RestAPI.GraphServerAccess;

import java.io.*;

class Parser{

	private int flag = 0;
	private String input_file;
	private String input_oracle;
	private int cutype;
	private String codeString;
	private int bakerType;
	//added by nmeng to get the actual string which is used to generate CU
	private String parsedString;
	/*
	 * cutype = 0 => already has class body and method body
	 * 			1 => has a method wrapper but no class
	 * 			2 => missing both method and class wrapper (just a bunch of statements) 
	 */
	
	public Parser(String oracle, String arg, int flag) throws IOException 
	{
		//flag = 0 => filename as arg, flag = 1 => code string as arg 
		input_oracle = oracle;
		bakerType = flag;
		if(flag == 0)
		{
			String path = getPath();
			input_file = path + File.separator + arg;
		}
		else
			codeString = arg;
	}
	
	private String getPath() throws IOException 
	{
		Process p = Runtime.getRuntime().exec("pwd");
		BufferedReader stdInput = new BufferedReader(new InputStreamReader(p.getInputStream()));
		String path = "";
		String s = "";
		while ((s = stdInput.readLine()) != null) {
			path = s;
		}
		return path;
	}

	public GraphServerAccess getGraph() throws IOException
	{
		try
		{
			GraphServerAccess graphServer = new GraphServerAccess(input_oracle);
			return graphServer;
		}
		catch(StoreLockException e)
		{
			//System.out.println("Database Locked");
			return null;
		}
	}

	private ASTParser getASTParser(String sourceCode, int parserType) 
	{
		ASTParser parser = ASTParser.newParser(AST.JLS3);
		parser.setResolveBindings(true);
		parser.setStatementsRecovery(true);
		parser.setBindingsRecovery(true);
		parser.setKind(parserType);
		parser.setSource(sourceCode.toCharArray());
		return parser;
	}

	private String getCodefromSnippet() throws IOException 
	{
		BufferedReader br = new BufferedReader(new FileReader(input_file));
		StringBuilder codeBuilder = new StringBuilder();
		String codeText = null;
		try 
		{
			String strLine = br.readLine();
			while (strLine != null) 
			{
				codeBuilder.append(strLine);
				codeBuilder.append("\n");
				strLine = br.readLine();
			}
		}
		finally
		{
			br.close();
			codeText = codeBuilder.toString();
			codeText = codeText.replace("&lt;", "<");
			codeText = codeText.replace("&gt;", ">");
			codeText = codeText.replace("&amp;", "&");
		}
		return codeText;
	}

	public CompilationUnit getCompilationUnitFromFile() throws IOException,	NullPointerException, ClassNotFoundException 
	{
		String code = "";
		if(bakerType == 0)
			code = getCodefromSnippet();
		else if(bakerType == 1)
			code = codeString;
		ASTParser parser = getASTParser(code, ASTParser.K_COMPILATION_UNIT);
		ASTNode cu = (CompilationUnit) parser.createAST(null);
		cutype = 0;
		parsedString = code;//added by nmeng
		if(((CompilationUnit) cu).types().isEmpty()) 
		{
			flag = 1;
			// System.out.println("Missing class body, wrapper added");
			cutype = 1;
			String s1 = "public class sample{\n" + code + "\n}";
			//System.out.println(s1);
			parser = getASTParser(s1, ASTParser.K_STATEMENTS);
			cu = parser.createAST(null);
			cu.accept(new ASTVisitor() 
			{
				public boolean visit(MethodDeclaration node) 
				{
					flag = 2;
					return false;
				}
			});
			if (flag == 1)
			{
				//System.out.println("Missing method, wrapper added");
				s1 = "public class sample{\n public void foo(){\n" + code
						+ "\n}\n}";
				cutype = 2;
				parser = getASTParser(s1, ASTParser.K_COMPILATION_UNIT);
				cu = parser.createAST(null);
			}
			else if (flag == 2) 
			{
				s1 = "public class sample{\n" + code + "\n}";
				//System.out.println(s1);
				cutype = 1;
				parser = getASTParser(s1, ASTParser.K_COMPILATION_UNIT);
				cu = parser.createAST(null);
			}
			parsedString = s1; //added by nmeng
		} 
		else 
		{
			// System.out.println("Has complete class and method bodies, code not modified");
			cutype = 0;
			parser = getASTParser(code, ASTParser.K_COMPILATION_UNIT);
			cu = parser.createAST(null);
		}
		return (CompilationUnit) cu;
	}

	public CompilationUnit getCompilationUnitFromString(String code) throws IOException,
	NullPointerException, ClassNotFoundException 
	{
		ASTParser parser = getASTParser(code, ASTParser.K_COMPILATION_UNIT);
		ASTNode cu = (CompilationUnit) parser.createAST(null);
		//System.out.println(cu);
		parsedString = code;
		cutype = 0;
		if (((CompilationUnit) cu).types().isEmpty()) {
			flag = 1;
			// System.out.println("Missing class body, wrapper added");
			cutype = 1;
			String s1 = "public class sample{\n" + code + "\n}";
			parser = getASTParser(s1, ASTParser.K_COMPILATION_UNIT);
			cu = parser.createAST(null);
			cu.accept(new ASTVisitor() {
				public boolean visit(MethodDeclaration node) {
					flag = 2;
					return false;
				}
			});
			if (flag == 1) {
				// System.out.println("Missing method, wrapper added");
				s1 = "public class sample{\n public void foo(){\n" + code
						+ "\n}\n}";
				cutype = 2;
				parser = getASTParser(s1, ASTParser.K_COMPILATION_UNIT);
				cu = parser.createAST(null);				
			}
			if (flag == 2) {
				s1 = "public class sample{\n" + code + "\n}";
				cutype = 1;
				parser = getASTParser(s1, ASTParser.K_COMPILATION_UNIT);
				cu = parser.createAST(null);
				parsedString = s1; //added by nmeng
			}	
			parsedString = s1; //added by nmeng
		} else {
			// System.out.println("Has complete class and method bodies, code not modified");
			cutype = 0;
			parser = getASTParser(code, ASTParser.K_COMPILATION_UNIT);
			cu = parser.createAST(null);
		}
		return (CompilationUnit) cu;
	}

	int getCuType()
	{
		return cutype;
	}
	
	String getParsedString() {
		return parsedString;
	}
}
