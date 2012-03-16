/*
 * Name: Hei Wong (Ian)
 * 
 * Sources:
 * ANTLR v4's BaseTest.java
 * https://github.com/parrt/antlr4/blob/master/tool/test/org/antlr/v4/test/BaseTest.java
 * 
 */

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.Arrays;

import javax.tools.DiagnosticCollector;
import javax.tools.JavaCompiler;
import javax.tools.JavaFileObject;
import javax.tools.StandardJavaFileManager;
import javax.tools.ToolProvider;

public class jInterp {

	public static Boolean DEBUG, suppressError;
	public static String pathSeparator, CLASSPATH, tmpDir = null;
	public static ClassLoader loader = null;

	public jInterp() {
		DEBUG = false;
	}

	/*
	 * Java compiler API to manually compile given .java file
	 * 
	 */
	public static boolean compile(String filename, Boolean suppressError) {		

		File f = new File(tmpDir, filename);
		JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
		StandardJavaFileManager fileManager = compiler.getStandardFileManager(null, null, null);
		
		DiagnosticCollector<JavaFileObject> diagnostics;
		if(suppressError){
			diagnostics = new DiagnosticCollector<JavaFileObject>();
		}else{
			diagnostics = null;
		}
		

		Iterable<? extends JavaFileObject> compilationUnits = fileManager.getJavaFileObjects(f);
		Iterable<String> options = Arrays.asList("-d", tmpDir, "-cp", tmpDir + pathSeparator + CLASSPATH, "-Xlint");

		JavaCompiler.CompilationTask task = compiler.getTask(null, fileManager, diagnostics, options, null, compilationUnits);

		// Suppress diagnostic messages to console
		if(suppressError)diagnostics.getDiagnostics();
		
		boolean compiled = task.call();

		try {
			
			fileManager.close();
			
		} catch(IOException ioe) {
			ioe.printStackTrace(System.err);
		}

		if(DEBUG) {
			if(compiled) System.out.println("Compiled");
			else System.out.println("Could not compiled");
		}

		return compiled;
	}
	
	
	/*
	 * Class loader to execute a statement
	 * 1 ) load the .java file from temp directory
	 * 2 ) load class from the this file with given classname
	 * 3 ) load method for execution
	 * 4 ) invoke the method  
	 */
	public static boolean execCommand(String className) {

		try {
			
			Class<?> loadedClass = (Class<?>)loader.loadClass(className);
			Method loadedMethod = loadedClass.getDeclaredMethod("exec");
			loadedMethod.invoke(null);

		} catch(Exception e) {
			e.printStackTrace();
		}

		return false;
	}
	

	/*
	 * Create a .java file for given statement from command line
	 */
	public static String writeToFile(String s, int classCounter, String classDef) {

		try{
			// create a .java file in temp directory
			String filename = getClassName(classCounter)+".java";
			File f = new File(tmpDir, filename);
			if(DEBUG) {
				System.out.println(f.getAbsolutePath());
			}
//			FileWriter fWriter = new FileWriter(tmpDir+filename);
			FileWriter fWriter = new FileWriter(f);
			BufferedWriter bWriter = new BufferedWriter(fWriter);

			// check for .java template
			String javaFile = "";
			if(classDef.equalsIgnoreCase("a")){
				javaFile = classDefA(s, classCounter);
			}else if (classDef.equalsIgnoreCase("b")){
				javaFile = classDefB(s, classCounter);
			}

			// write out to file
			bWriter.write(javaFile);
			bWriter.close();
			return filename;

		} catch(Exception e) {
			e.printStackTrace();
		}

		return null;

	}
	

	/*
	 * Template A for .java file creation (Declaration)
	 */
	public static String classDefA(String s, int classCounter) {

		String content = "import java.io.*;\nimport java.util.*;\n\n";
		
		content += "public class "+getClassName(classCounter);
		if (classCounter > 0) {
			content += " extends "+getClassName(classCounter-1);
		}
		content += " {\n";
		content += "\tpublic static " + s + "\n";
		content += "\tpublic static void exec() {}\n";
		content += "}";

		return content;
	}
	
	
	/*
	 * Template B for .java file creation (Statement)
	 */
	public static String classDefB(String s, int classCounter) {

		String content = "import java.io.*;\nimport java.util.*;\n\n";
		
		content += "public class "+getClassName(classCounter);
		if (classCounter > 0) {
			content += " extends "+getClassName(classCounter-1);
		}
		content += " {\n";
		content += "\tpublic static void exec() {\n"+getShortcut(s)+"\n}\n";
		content += "}";

		return content;
	}
	

	/*
	 * Helper function to make java file name
	 */
	public static String getClassName(int classCounter) {

		return "Interp_"+classCounter;
	}
	
	
	/*
	 * Helper method to create shortcut for "System.out.println()"
	 * Usage:
	 * $ int i = 3;
	 * $ print i;	// equals to System.out.println(i);
	 */
	public static String getShortcut(String s) {
		
		if(s.contains("print") && s.startsWith("print")){
			s = s.replace("print", "System.out.println(").replace(";", "");
			s += " );";
		}
		
		return s;
	}

	
	/*
	 * Helper function to print configuration
	 */
	public static void printConfig() {

		System.out.println("tmpDir = "+tmpDir);
		System.out.println("pathSeparator = "+pathSeparator);
		System.out.println("CLASSPATH = "+CLASSPATH);

	}

	
	/*
	 * Initialize variables
	 */
	public static void setUp() {
		
		pathSeparator = System.getProperty("path.separator");
		CLASSPATH = System.getProperty("java.class.path");
		tmpDir = System.getProperty("java.io.tmpdir");
		DEBUG = false;
		try {
			loader = new URLClassLoader(new URL[] { new File(tmpDir).toURI().toURL() }, ClassLoader.getSystemClassLoader());
		} catch (MalformedURLException e) {
			e.printStackTrace();
		}
		
	}
	

	/*
	 * Helper function to create a director
	 */
	public static void makeDir(String dir) {
		File f = new File(dir);
		f.mkdirs();
	}
	

	public static void main(String[] args) {

		try{
			setUp();
			makeDir(tmpDir);
			if (DEBUG) printConfig();

			BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
			System.out.println("--- jInterp ---\t\tnote: \"print i;\" == \"System.out.println(i);");
			System.out.print("> ");

			String s, filename, classname;
			int classCounter = 0;
			boolean compiled = false;

			while ( (s=br.readLine()) != null && s.length() != 0) {

				// write command line to file
				suppressError = true;
				classname = getClassName(classCounter);
				filename = writeToFile(s, classCounter, "a");
				compiled = compile(filename, suppressError);

				if(!compiled) {
					
					// need to do exec() instead
					suppressError = false;
					filename = writeToFile(s, classCounter, "b");
					compiled = compile(filename, suppressError);

					if(!compiled) {
						System.out.println("Syntax error.");
					} else {
						execCommand(classname);
					}

				}

				if (DEBUG) {
					System.out.println("filename = "+filename);
					System.out.println("classname = "+classname);
					System.out.println("You've entered : "+s);					
				}

				System.out.print("> ");
				if(compiled) {
					classCounter++;
					suppressError = true;
				}
			}

		} catch (IOException e) {
			e.printStackTrace();
		}

	}

}
