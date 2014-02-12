/**
 * @author fengchen, Dongyun Jin, Patrick Meredith, Michael Ilseman
 *
 *         To change the template for this generated type comment go to
 *         Window&gt;Preferences&gt;Java&gt;Code Generation&gt;Code and Comments
 */

package llvmmop;

import java.io.File;
import java.io.FileWriter;
import java.io.FilenameFilter;
import java.util.ArrayList;
import java.util.List;

import com.runtimeverification.rvmonitor.java.rvj.Main;
import llvmmop.parser.ast.MOPSpecFile;
import llvmmop.util.Tool;
import llvmmop.util.AJFileCombiner;

class JavaFileFilter implements FilenameFilter {
	public boolean accept(File dir, String name) {
		return name.endsWith(".java");
	}
}

class MOPFileFilter implements FilenameFilter {
	public boolean accept(File dir, String name) {
		return name.endsWith(".mop");
	}
}

public class LLVMMOPMain {

	static File outputDir = null;
	public static boolean debug = false;
	public static boolean noopt1 = false;
	public static boolean toJavaLib = false;
	public static boolean statistics = false;
	public static boolean statistics2 = false;
	public static String aspectname = null;
	public static boolean specifiedAJName = false;
	public static boolean isJarFile = false;
	public static String jarFilePath = null;

	public static final int NONE = 0;
	public static final int HANDLERS = 1;
	public static final int EVENTS = 2;
	public static int logLevel = NONE;

	public static boolean dacapo = false;
	public static boolean dacapo2 = false;
	public static boolean silent = false;
	public static boolean empty_advicebody = false;
	public static boolean translate2RV = true;

	public static boolean merge = false;
	public static boolean inline = false;

	public static boolean scalable = false;
	public static boolean keepRVFiles = false;

        public static List<String []> listFilePairs = new ArrayList<String []>();
        public static List<String> listRVMFiles = new ArrayList<String>();

	static private File getTargetDir(ArrayList<File> specFiles) throws MOPException{
		if(LLVMMOPMain.outputDir != null){
			return outputDir;
		}

		boolean sameDir = true;
		File parentFile = null;
		
		for(File file : specFiles){
			if(parentFile == null){
				parentFile = file.getAbsoluteFile().getParentFile();
			} else {
				if(file.getAbsoluteFile().getParentFile().equals(parentFile)){
					continue;
				} else {
					sameDir = false;
					break;
				}
			}
		}
		
		if(sameDir){
			return parentFile;
		} else {
			return new File(".");
		}
	}
	
	
	/**
	 * Process a java file including mop annotations to generate an aspect file. The path argument should be an existing java file name. The location
	 * argument should contain the original file name, But it may have a different directory.
	 * 
	 * @param path
	 *            an absolute path of a specification file
	 * @param location
	 *            an absolute path for result file
	 */
	public static void processJavaFile(File file, String location) throws MOPException {
		MOPNameSpace.init();
		String specStr = SpecExtractor.process(file);
		MOPSpecFile spec =  SpecExtractor.parse(specStr);
		
		if (LLVMMOPMain.aspectname == null) {
			LLVMMOPMain.aspectname = Tool.getFileName(file.getAbsolutePath());
		}
		MOPProcessor processor = new MOPProcessor(LLVMMOPMain.aspectname);

		String aspect = processor.process(spec);
		writeFile(aspect, location, "MonitorAspect.aj");
	}

	/**
	 * Process a specification file to generate an aspect file. The path argument should be an existing specification file name. The location
	 * argument should contain the original file name, But it may have a different directory.
	 * 
	 * @param path
	 *            an absolute path of a specification file
	 * @param location
	 *            an absolute path for result file
	 */
	public static void processSpecFile(File file, String location) throws MOPException {
		MOPNameSpace.init();
		String specStr = SpecExtractor.process(file);
		MOPSpecFile spec =  SpecExtractor.parse(specStr);

		if (LLVMMOPMain.aspectname == null) {
			LLVMMOPMain.aspectname = Tool.getFileName(file.getAbsolutePath());
		}
		
		MOPProcessor processor = new MOPProcessor(LLVMMOPMain.aspectname);

		String output = processor.process(spec);
		
		if (translate2RV) {
			writeFile(processor.translate2RV(spec), file.getAbsolutePath(), ".rvm");
		}
		
		if (toJavaLib) {
			writeFile(output, location, "JavaLibMonitor.java");
		} else {
			writeFile(output, location, "MonitorAspect.aj");
		}
	}
	
	public static void processMultipleFiles(ArrayList<File> specFiles) throws MOPException {
		String aspectName;

		if(outputDir == null){
			outputDir = getTargetDir(specFiles);
		}

		if(LLVMMOPMain.aspectname != null) {
			aspectName = LLVMMOPMain.aspectname;
		} else {
			if(specFiles.size() == 1) {
				aspectName = Tool.getFileName(specFiles.get(0).getAbsolutePath());
			} else {
				int suffixNumber = 0;
				// generate auto name like 'MultiMonitorApsect.aj'
				
				File aspectFile;
				do{
					suffixNumber++;
					aspectFile = new File(outputDir.getAbsolutePath() + File.separator + "MultiSpec_" + suffixNumber + "MonitorAspect.aj");
				} while(aspectFile.exists());
				
				aspectName = "MultiSpec_" + suffixNumber;
			}
			LLVMMOPMain.aspectname = aspectName;
		}
		MOPProcessor processor = new MOPProcessor(aspectName);
		MOPNameSpace.init();
		ArrayList<MOPSpecFile> specs = new ArrayList<MOPSpecFile>();
		for(File file : specFiles){
			String specStr = SpecExtractor.process(file);
			MOPSpecFile spec =  SpecExtractor.parse(specStr);
			if (translate2RV) {
				writeFile(processor.translate2RV(spec), file.getAbsolutePath(), ".rvm");
			}
			specs.add(spec);
		}
		MOPSpecFile combinedSpec = SpecCombiner.process(specs);
		String output = processor.process(combinedSpec);
		writeCombinedAspectFile(output, aspectName);
	}

	protected static void writeJavaFile(String javaContent, String location) throws MOPException {
		if ((javaContent == null) || (javaContent.length() == 0))
			throw new MOPException("Nothing to write as a java file");
		if (!Tool.isJavaFile(location))
			throw new MOPException(location + "should be a Java file!");

		try {
			FileWriter f = new FileWriter(location);
			f.write(javaContent);
			f.close();
		} catch (Exception e) {
			throw new MOPException(e.getMessage());
		}
	}
	
	protected static void writeCombinedAspectFile(String aspectContent, String aspectName) throws MOPException {
		if (aspectContent == null || aspectContent.length() == 0)
			return;

		try {
			FileWriter f = new FileWriter(outputDir.getAbsolutePath() + File.separator + aspectName + "MonitorAspect.aj");
			f.write(aspectContent);
			f.close();
		} catch (Exception e) {
			throw new MOPException(e.getMessage());
		}
		System.out.println(" " + aspectName + "MonitorAspect.aj is generated");
	}
	
	protected static void writeFile(String content, String location, String suffix) throws MOPException {
		if (content == null || content.length() == 0)
			return;

		int i = location.lastIndexOf(File.separator);
                String filePath = ""; 
		try {
                        filePath = location.substring(0, i + 1) + Tool.getFileName(location) + suffix;
			FileWriter f = new FileWriter(filePath);
			f.write(content);
			f.close();
		} catch (Exception e) {
			throw new MOPException(e.getMessage());
		}
		if (suffix.equals(".rvm")) {
			listRVMFiles.add(filePath);
		}
		System.out.println(" " + Tool.getFileName(location) + suffix + " is generated");
	}

	// PM
	protected static void writePluginOutputFile(String pluginOutput, String location) throws MOPException {
		int i = location.lastIndexOf(File.separator);

		try {
			FileWriter f = new FileWriter(location.substring(0, i + 1) + Tool.getFileName(location) + "PluginOutput.txt");
			f.write(pluginOutput);
			f.close();
		} catch (Exception e) {
			throw new MOPException(e.getMessage());
		}
		System.out.println(" " + Tool.getFileName(location) + "PluginOutput.txt is generated");
	}

	public static String polishPath(String path) {
		if (path.indexOf("%20") > 0)
			path = path.replaceAll("%20", " ");

		return path;
	}

	public static ArrayList<File> collectFiles(String[] files, String path) throws MOPException {
		ArrayList<File> ret = new ArrayList<File>();

		for (String file : files) {
			String fPath = path.length() == 0 ? file : path + File.separator + file;
			File f = new File(fPath);

			if (!f.exists()) {
				throw new MOPException("[Error] Target file, " + file + ", doesn't exsit!");
			} else if (f.isDirectory()) {
				ret.addAll(collectFiles(f.list(new JavaFileFilter()), f.getAbsolutePath()));
				ret.addAll(collectFiles(f.list(new MOPFileFilter()), f.getAbsolutePath()));
			} else {
				if (Tool.isSpecFile(file)) {
					ret.add(f);
				} else if (Tool.isJavaFile(file)) {
					ret.add(f);
				} else
					throw new MOPException("Unrecognized file type! The JavaMOP specification file should have .mop as the extension.");
			}
		}

		return ret;
	}

	public static void process(String[] files, String path) throws MOPException {
		ArrayList<File> specFiles = collectFiles(files, path);
		
		if(LLVMMOPMain.aspectname != null && files.length > 1){
			LLVMMOPMain.merge = true;
		}
		
		if (LLVMMOPMain.merge) {
			System.out.println("-Processing " + specFiles.size()
					+ " specification(s)");
			processMultipleFiles(specFiles);
			String javaFile = outputDir.getAbsolutePath() + File.separator
					+ LLVMMOPMain.aspectname + "RuntimeMonitor.java";
			String ajFile = outputDir.getAbsolutePath() + File.separator
					+ LLVMMOPMain.aspectname + "MonitorAspect.aj";
			String combinerArgs[] = new String[2];
			combinerArgs[0] = javaFile;
			combinerArgs[1] = ajFile;

			listFilePairs.add(combinerArgs);

		} else {
			for (File file : specFiles) {
				boolean needResetAspectName = LLVMMOPMain.aspectname == null;
				String location = outputDir == null ? file.getAbsolutePath() : outputDir.getAbsolutePath() + File.separator + file.getName();
				System.out.println("-Processing " + file.getPath());
				if (Tool.isSpecFile(file.getName())) {
					processSpecFile(file, location);
				} else if (Tool.isJavaFile(file.getName())) {
					processJavaFile(file, location);
				}
				
				File combineDir = outputDir == null ? file.getAbsoluteFile()
						.getParentFile() : outputDir;

				String javaFile = combineDir.getAbsolutePath() + File.separator
						+ LLVMMOPMain.aspectname + "RuntimeMonitor.java";
				String ajFile = combineDir.getAbsolutePath() + File.separator
						+ LLVMMOPMain.aspectname + "MonitorAspect.aj";
				String combinerArgs[] = new String[2];
				combinerArgs[0] = javaFile;
				combinerArgs[1] = ajFile;

				listFilePairs.add(combinerArgs);

				if (needResetAspectName) {
					LLVMMOPMain.aspectname = null;
				}
			}
		}
	}

	public static void process(String arg) throws MOPException {
		if(outputDir != null && !outputDir.exists())
			throw new MOPException("The output directory, " + outputDir.getPath() + " does not exist.");
		
		process(arg.split(";"), "");
	}

	// PM
	public static void print_help() {
		System.out.println("Usage: java [-cp javmaop_classpath] llvmmop.LLVMMOPMain [-options] files");
		System.out.println("");
		System.out.println("where options include:");
		System.out.println(" Options enabled by default are prefixed with \'+\'");
		System.out.println("    -h -help\t\t\t  print this help message");
		System.out.println("    -v | -verbose\t\t  enable verbose output");
		System.out.println("    -debug\t\t\t  enable verbose error message");
		System.out.println();

		System.out.println("    -local\t\t\t+ use local logic engine");
		System.out.println("    -remote\t\t\t  use default remote logic engine");
		System.out.println("\t\t\t\t  " + Configuration.getServerAddr());
		System.out.println("\t\t\t\t  (You can change the default address");
		System.out.println("\t\t\t\t   in llvmmop/config/remote_server_addr.properties)");
		System.out.println("    -remote:<server address>\t  use remote logic engine");
		System.out.println();

		System.out.println("    -d <output path>\t\t  select directory to store output files");
		System.out.println("    -n | -aspectname <aspect name>\t  use the given aspect name instead of source code name");
		System.out.println();

		System.out.println("    -showevents\t\t\t  show every event/handler occurrence");
		System.out.println("    -showhandlers\t\t\t  show every handler occurrence");
		System.out.println();

		System.out.println("    -s | -statistics\t\t  generate monitor with statistics");
		System.out.println("    -noopt1\t\t\t  don't use the enable set optimization");
		System.out.println("    -javalib\t\t\t  generate a java library rather than an AspectJ file");
		System.out.println();

		System.out.println("    -aspect:\"<command line>\"\t  compile the result right after it is generated");
		System.out.println();
	}

	public static void main(String[] args) {
		ClassLoader loader = LLVMMOPMain.class.getClassLoader();
		String mainClassPath = loader.getResource("llvmmop/LLVMMOPMain.class").toString();
		if (mainClassPath.endsWith(".jar!/llvmmop/LLVMMOPMain.class") && mainClassPath.startsWith("jar:")) {
			isJarFile = true;

			jarFilePath = mainClassPath.substring("jar:file:".length(), mainClassPath.length() - "!/llvmmop/LLVMMOPMain.class".length());
			jarFilePath = polishPath(jarFilePath);
		}

		int i = 0;
		String files = "";

		while (i < args.length) {
			if (args[i].compareTo("-h") == 0 || args[i].compareTo("-help") == 0) {
				print_help();
				return;
			}

			if (args[i].compareTo("-d") == 0) {
				i++;
				outputDir = new File(args[i]);
			} else if (args[i].compareTo("-local") == 0) {
			} else if (args[i].compareTo("-remote") == 0) {
			} else if (args[i].startsWith("-remote:")) {
			} else if (args[i].compareTo("-v") == 0 || args[i].compareTo("-verbose") == 0) {
				MOPProcessor.verbose = true;
			} else if (args[i].compareTo("-javalib") == 0) {
				toJavaLib = true;
			} else if (args[i].compareTo("-debug") == 0) {
				LLVMMOPMain.debug = true;
			} else if (args[i].compareTo("-noopt1") == 0) {
				LLVMMOPMain.noopt1 = true;
			} else if (args[i].compareTo("-s") == 0 || args[i].compareTo("-statistics") == 0) {
				LLVMMOPMain.statistics = true;
			} else if (args[i].compareTo("-s2") == 0 || args[i].compareTo("-statistics2") == 0) {
				LLVMMOPMain.statistics2 = true;
			} else if (args[i].compareTo("-n") == 0 || args[i].compareTo("-aspectname") == 0) {
				i++;
				LLVMMOPMain.aspectname = args[i];
				LLVMMOPMain.specifiedAJName = true;
			} else if (args[i].compareTo("-showhandlers") == 0) {
				if (LLVMMOPMain.logLevel < LLVMMOPMain.HANDLERS)
					LLVMMOPMain.logLevel = LLVMMOPMain.HANDLERS;
			} else if (args[i].compareTo("-showevents") == 0) {
				if (LLVMMOPMain.logLevel < LLVMMOPMain.EVENTS)
					LLVMMOPMain.logLevel = LLVMMOPMain.EVENTS;
			} else if (args[i].compareTo("-dacapo") == 0) {
				LLVMMOPMain.dacapo = true;
			} else if (args[i].compareTo("-dacapo2") == 0) {
				LLVMMOPMain.dacapo2 = true;
			} else if (args[i].compareTo("-silent") == 0) {
				LLVMMOPMain.silent = true;
			} else if (args[i].compareTo("-merge") == 0) {
				LLVMMOPMain.merge = true;
			} else if (args[i].compareTo("-inline") == 0) {
				LLVMMOPMain.inline = true;
			} else if (args[i].compareTo("-noadvicebody") == 0) {
				LLVMMOPMain.empty_advicebody = true;
			} else if (args[i].compareTo("-scalable") == 0) {
				LLVMMOPMain.scalable = true;
			} else if (args[i].compareTo("-translate2RV") == 0) {
				LLVMMOPMain.translate2RV = true;
			} else if (args[i].compareTo("-keepRVFiles") == 0) {
				LLVMMOPMain.keepRVFiles = true;
			} else {
				if (files.length() != 0)
					files += ";";
				files += args[i];
			}
			++i;
		}

		if (files.length() == 0) {
			print_help();
			return;
		}
		
		// Generate .rvm files and .aj files
		try {
			process(files); 
		} catch (Exception e) {
			System.err.println(e.getMessage());
			if (LLVMMOPMain.debug)
				e.printStackTrace();
		}
		
		// replace mop with rvm and call rv-monitor
		int length = args.length;
		if (LLVMMOPMain.keepRVFiles) {
			length--;
		}
		String rvArgs[] = new String [length];
		int p = 0;
		for (int j = 0; j < args.length; j++) {
			if (args[j].compareTo("-keepRVFiles") == 0) {
				// Don't pass keepRVFiles to rvmonitor
				continue;
			}
			rvArgs[p] = args[j].replaceAll("\\.mop", "\\.rvm");
			p++;
		}
		Main.main(rvArgs);
		
		// Call AJFileCombiner here to combine these two
		// TODO
		for (String[] filePair : listFilePairs) {
			AJFileCombiner.main(filePair);
			File javaFile = new File(filePair[0]);
			try {
				if (!LLVMMOPMain.keepRVFiles) {
					boolean deleted = javaFile.delete();
					if (!deleted) {
						System.err.println("Failed to delete java file: "
								+ filePair[0]);
					}
				}
			} catch (Exception e) {

			}
		}

		for (String rvmFilePath : listRVMFiles) {
			File rvmFile = new File(rvmFilePath);
			try {				
				if (!LLVMMOPMain.keepRVFiles) {
					boolean deleted = rvmFile.delete();
					if (!deleted) {
						System.err.println("Failed to delete java file: "
								+ rvmFilePath);
					}
				}

			} catch (Exception e) {

			}
		}
        
	}
}
