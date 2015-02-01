package org.newcashel.meta.util;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.RandomAccessFile;
import java.lang.reflect.Constructor;
import java.net.InetAddress;
import java.net.URI;
import java.net.URL;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.PosixFilePermissions;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.UUID;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.filefilter.TrueFileFilter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.ObjectMapper;


public class UTIL {
	
	private final static Logger logger = LoggerFactory.getLogger(UTIL.class);
	
	// for use by startup/shutdown process which is always serial processing (no concurrent use)
	public static ObjectMapper mapper = new ObjectMapper();
	
	private static UTIL util;
	private static String runtimeDir = null;
	
	private static Map<String, String> env = null;
	static Properties properties = new Properties();

	public static final int LOC_NETWORK = 1;
	public static final int LOC_ABSOLUTE = 2;
	public static final int LOC_USER = 3;
	public static final int LOC_HTTP = 4;
	
	
	// provided by user during install, includes keyStore location needed to start the service
	private static HashMap<String, String> configProps = new HashMap<String, String>();
	
	public static Properties getProperties() { return properties; }
	
	
	public static String[] convertCSVStringToList(String str, boolean convertToLC) {
		if (str == null || str.length() < 1) {
			logger.error("UTIL.convertStringToList receiveed null or empty value");
			return null;
		}
		String breakVal = "\\s*,\\s*";
		if (!convertToLC) {
			return str.split(breakVal);
		}
		// drop thru, convert to LC
		String[] strs = str.split(breakVal);
		for (String s : strs) {
			s = s.toLowerCase();
		}
		return strs;
	}
	
	public static long ipToLong(String ipAddress) {

		if (ipAddress == null) {
			logger.error("ip address not provided ");
			return 0;
		}
		String[] ipAddressInArray = ipAddress.split("\\.");

		long result = 0;
		for (int i = 0; i < ipAddressInArray.length; i++) {

			int power = 3 - i;
			int ip = Integer.parseInt(ipAddressInArray[i]);
			result += ip * Math.pow(256, power);

		}

		return result;
	}

	
	public static String getPropertyVal(String key) throws Exception {
		Object obj = properties.getProperty(key);
		if (obj == null) {
			String msg = "requested property not defined " + key;
			logger.error(msg);
			throw new Exception(msg);
		}
		return obj.toString();
	}
	
	public static String checkPropertyVal(String key) throws Exception {
		Object obj = properties.getProperty(key);
		if (obj == null) return null;
		return obj.toString();
	}
	
	
	public static void addPropertyVal(String key, String value) {
		properties.setProperty(key, value);
	}

	public static void loadProperties(String propertyFile) throws FileNotFoundException, IOException {
		
		FileInputStream input;
			input = new FileInputStream(propertyFile);
			// load a properties file
			properties.load(input);
			//System.out.println("DF");
	}
	
	public static void loadConfigProps(String configFile) throws FileNotFoundException, IOException {
		// open up the config file and get the remaining parms
		BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(configFile)));

		try {
			String line;
			while ((line = br.readLine()) != null) {
				String[] strArray = line.split("=");
				if (strArray.length != 2) continue;
				String prop = strArray[0]; 
				String val = strArray[1];
				configProps.put(prop,  val);	
			}
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			br.close();
		}
	}

	public static String getConfigProp(String name) throws Exception {
		Object val = configProps.get(name);
		if (val == null) {
			String msg = "The system startup configuration does not contain the requested attribute, " + name;
			throw new Exception(msg);
		}
		return val.toString();
	}
	
	public static String checkConfigProp(String name) {
		Object val = configProps.get(name);
		if (val == null) return null;
		return val.toString();
	}
	
	public static void replaceFileString(String fileIn, String old, String newStr) throws IOException {
	try {
	     String content = FileUtils.readFileToString(new File(fileIn), "UTF-8");
	     content = content.replaceAll(old, newStr);
	     File tempFile = new File("OutputFile");
	     FileUtils.writeStringToFile(tempFile, content, "UTF-8");
	  } catch (IOException e) {
	     //Simple exception handling, replace with what's necessary for your use case!
	     throw new RuntimeException("Generating file failed", e);
	  }
	}
	
	static public InputStream openWhenReady(Path path, int waitMills, boolean deleteFile) {

		File inputFile = null;
		int attempts = 0;
		while (++attempts < 5) {
			try {
				inputFile = path.toFile();
				if (!inputFile.exists()) {
					logger.info("locating file, attempt # " + attempts);
					//if (++attempts > 5) throw new Exception("File cannot be accessed " + path.toString());
					if (attempts > 5) return null;
					Thread.sleep(waitMills);
				}
				return new FileInputStream(inputFile);
			} catch (Exception e) {
				logger.info("locating file, exception, attempt # " + e.toString() + ", " + attempts);
				if (attempts > 5) return null;
				try {
					Thread.sleep(waitMills);
				} catch (InterruptedException e1) {
					e1.printStackTrace();
				}
			} finally {
				logger.info("openWhenReady completed in attempts " +  attempts);
				if (deleteFile) {
					inputFile.delete();
					logger.info("deleted input file " + path.toString());
				}
			}
		}
		return null;
	}
	
	static public Object getTypedProperty(String propVal, Class cls) throws Exception {
		String foundProp = getPropertyVal(propVal);
		if (foundProp == null) {
			logger.error("Invalid System Configuration, propety value could not be loaded " + propVal);
			System.exit(1);
		}
		Class[] cArg = new Class[1];
        cArg[0] = String.class;
        Constructor ct = cls.getDeclaredConstructor(cArg);
		Object nativeObj = ct.newInstance(foundProp);
		return nativeObj;
	}
	
	static public boolean convertBoolean(Object o) {
		if (o == null) return false;
		return Boolean.valueOf(o.toString());
	}
	
	static public int convertAnyNumberToInt(Object o) {
		if (o == null || o.toString().length() < 1) return 0;
		return new Double(o.toString()).intValue();
	}
	
	static public double convertAnyNumberToDouble(Object o) {
		if (o == null || o.toString().length() < 1) return 0;
		return new Double(o.toString()).doubleValue();
	}
	
	public static String createAbsoluteFileLocation(String path, String fileName) {
		return path + File.separator + fileName;
	}
	
	public static FileInputStream openFileInputStream(File file) {
		FileInputStream aFile = null;
		int count = -1;
		int maxCount = 3;	// try 3 time, wating 100 mills each
		while(++count < maxCount) {
			try {
				Thread.sleep(100);
				aFile = new FileInputStream(file);
				break;
			} catch (FileNotFoundException notFound) {
				continue;
			} catch (InterruptedException e) {
				logger.error("File could not be opened for processing " + file.toString());
			}
		}
		return aFile;
	}

	
	public static void createDirIfNotExists(String rootDir, String dirName) throws Exception {
		if (dirName == null || dirName.length() < 1) {
			throw new Exception("Cannot create local directory.  The folder name supplied is not valid: " + dirName);
		}
		
		if (rootDir != null) {
			dirName = rootDir + File.separator + dirName;
		}
		
		File rootFile = new File(rootDir);
		rootFile.setWritable(true);
		
		File dir = new File(dirName);
			
		Files.createLink(Paths.get(dirName), Paths.get("tmp"));
	}
	
	public static String getRuntimeDir() { return runtimeDir; }
	public static void setRuntimeDir(String runtimeDirectory) { 
		runtimeDir = runtimeDirectory; 
	}
		
	public static UTIL getUtil() {
		if (util == null) util = new UTIL();
		return util;
	}
	
	public static String getUNC(String fullFileName) {
		try {
			int index = fullFileName.indexOf(":");
			String subName = fullFileName.substring(index+2, fullFileName.length());
			String hostName = InetAddress.getLocalHost().getHostName();
			File file = new File("//" + hostName + "/" + subName);
			String nameStr = file.toString();
			return nameStr;
		} catch (Exception e) {
			System.out.println("getUNC fail on " + fullFileName);
			return null;
		}
	}
	
	public static byte[] stringToBytesASCII(String str) {
		char[] buffer = str.toCharArray();
		byte[] b = new byte[buffer.length];
		for (int i = 0; i < b.length; i++) {
			b[i] = (byte) buffer[i];
		}
		return b;
	}
		
	
	private static String getEnvVar(String key) {
		if (env == null) {
			env = System.getenv();
			for (String envName : env.keySet()) {
				//System.out.format("%s=%s%n", envName, env.get(envName));
			}	
		}
		return env.get(key);
	}

	public static String getUniqueId() {
		return UUID.randomUUID().toString().replaceAll("-", "");
		//return Strings.randomBase64UUID();
	}

	public static String getFileNameExtension(String fullFileName) {
		int index = fullFileName.lastIndexOf(".");
		if (index < 0) return null;
		return fullFileName.substring(index + 1, fullFileName.length());
	}
	
	// removes any directories preceeding the file name
	public static String stripFileNameExtension(String fullFileName) {
    	// extract the file name from the absolute string
    	int index = fullFileName.lastIndexOf(File.separator);
    	String fileName = fullFileName.substring(index+1, fullFileName.length());
    	index = fileName.lastIndexOf(".");
    	if (index < 0) return null;
    	return fileName.substring(0,index);
	}

	public static String getFileFolder(String fullFileName) {
    	// extract the file name from the absolute string
    	int index = fullFileName.lastIndexOf(File.separator);
    	return fullFileName.substring(0,index);
	}
	
	public static String getLeafFolder(String fullFolderName) {
    	// extract the file name from the absolute string
    	int index = fullFolderName.lastIndexOf(File.separator);
    	return fullFolderName.substring(index+1, fullFolderName.length());
	}
	
	public static boolean isRelative(String location) {
		// if windows (UNC, or drive letter)
		if (location.startsWith("\\\\") || location.indexOf(":\\") > 0 || location.indexOf(":/") > 0)	return false;
		return true;
	}
	
	public static String getUserAbsoluteLocation(String location) {
		if (isRelative(location)) {
			return System.getProperty("user.home") + File.separator + location;
		}
		return location;
	}
	
	// extension is between the last - and the following _
	public static String getFileName(String keyFile) throws Exception {
		if (keyFile == null) throw new Exception("null version file name passed to getVersionedFileExtension");
		
		int extensionStart = keyFile.lastIndexOf("-");
		if (extensionStart < 0) throw new Exception("Illegal version file name, no - separator");
		
		//int extensionEnd = keyFile.indexOf("_", extensionStart);
		//if (extensionStart < 0) throw new Exception("Illegal version file name, no _ separator");
		
		//return keyFile.substring(extensionStart, extensionEnd - 1);
		return keyFile.substring(0, extensionStart);
	}
	
	public static String getVersionedFileExtension(String keyFile) throws Exception {

		int extensionStart = keyFile.lastIndexOf("-");
		if (extensionStart < 0) throw new Exception("Illegal version file name, no - separator");
		
		int extensionEnd = keyFile.indexOf("_", extensionStart);
		if (extensionStart < 0) throw new Exception("Illegal version file name, no _ separator");
		
		return keyFile.substring(extensionStart + 1, extensionEnd);
	}
	
	
	// defined above
	//public static final int LOC_NETWORK = 1;
	//public static final int LOC_ABSOLUTE = 2;
	//public static final int LOC_USER = 3; scheme
	//public static final int LOC_HTTP = 4;
	
	
	public static String getUserFolder(String fullFolderName) {
    	// strip off the C:/User/USER. from the folder 
		String userHome = System.getProperty("user.home");
		if (fullFolderName.contains(userHome)) {
			// strip from the end of the matching value
			int index = fullFolderName.lastIndexOf(userHome) + userHome.length();
    		return fullFolderName.substring(index+1, fullFolderName.length());
		} else return fullFolderName;
	}

	// recursive
	public static List<File> getAllFiles(File directory) {
		List<File> files = (List<File>) FileUtils.listFiles(directory, TrueFileFilter.INSTANCE, TrueFileFilter.INSTANCE);
		//List<File> files = (List<File>) FileUtils.listFiles(dir, TrueFileFilter.INSTANCE, TrueFileFilter.INSTANCE);
		for (File file : files) {
			try {
				System.out.println("file: " + file.getCanonicalPath());
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		return files;
		//return FileUtils.listFiles(directory, null, true);
	}
		
	
	//JDK 7 variant, large files
	public static List<String> getFileList(String directory) {
		List<String> fileNames = new ArrayList<>();
		try (DirectoryStream<Path> directoryStream = Files.newDirectoryStream(Paths.get(directory))) {
			for (Path path : directoryStream) {
				fileNames.add(path.toString());
			}
		} catch (IOException ex) {}
		return fileNames;
	}

	public static File createTextFile(String fileName, String content) {
		
		FileOutputStream fop = null;
		File file = null;
		
		try {
			 
			file = new File(fileName);
			file.delete();
			fop = new FileOutputStream(file, false);
 
			// if file does not exist, create it
			//if (!file.exists()) {
			//	logger.info("created new text file " + fileName);
			//	file.createNewFile();
			//}
 
			// get the content in bytes
			byte[] contentInBytes = content.getBytes();
 
			fop.write(contentInBytes);
			fop.flush();
			//fop.close();
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			try {
				if (fop != null) {
					fop.close();
					logger.info("output file closed " + fileName);
				}
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		
		return file;
	}
	
	public static void cleanDirectory(String dirName, boolean deleteFolder) {
		File dir = new File(dirName);
		try {
			deleteFiles(dirName, false, 0);
			if (deleteFolder) {
				dir.delete();
			}
		} catch (Exception e) {
			logger.error("directory could not be cleaned " + dirName + ", " + e.toString()); 
		}
	}
	
	public static void deleteFiles(String url, boolean deleteFolder, int smallSize) {
		File folder = new File(url);
		File[] files = folder.listFiles();
	    if(files != null) { //some JVMs return null for empty dirs
	        for(File f: files) {
	            if(f.isDirectory()) {
	            	deleteFiles(folder.getAbsolutePath() + File.separator + f.getName(), deleteFolder, smallSize);
	            	deleteFile (f);
	            } else { // its a file
	            	if (smallSize == 0 || (f.length() < smallSize)) {
	            		deleteFile(f);
	            	}
	            }
	        }
	    }
	    
	    // delete the folder unless just purging small files
	    if (deleteFolder) {
	    	folder.delete();
	    	folder = null;
	    }
	    System.gc();
	}
	
	private static void deleteFile(File f) {
		if (f.exists() && f.canWrite()) {
			boolean success = f.delete();
			if (!success) {
				logger.info("accessible file cannot be deleted " + f.getName());
			} 
		} else {
			logger.error("cannot access file for delete " + f.getName());
		}
		f = null;
	}

	public static void deleteFile(String fileName) {
		File f = new File(fileName);
		if (f.exists()) {
			f.delete();
		}
	}

	public static void writeBytesToFile(byte[] bytes, String outputFileName) throws Exception {
    	try {
    		FileOutputStream outputFile = new FileOutputStream(outputFileName);
    		outputFile.write(bytes);
    		outputFile.flush();
    		outputFile.close();
    	} catch (Exception e) {
    		String msg = "Errror saving bytes[] to file " + outputFileName + ", " + e.toString();
    		logger.error(msg);
    		throw new Exception(msg);
    	}
    }

	public static byte[] byteFileRead(File file) throws IOException, Exception {
		if (!file.exists()) {
			String msg = "byteFileRead fail, byte file does not exist " + file.toString();
			logger.error(msg);
			throw new Exception(msg);
		}
		Path path = file.toPath();
		FileChannel fileChannel = null;
		ByteBuffer byteBuffer = null;
		try {
			fileChannel = new RandomAccessFile(file, "r").getChannel();
			byteBuffer = ByteBuffer.allocate((int)file.length());
			fileChannel.read(byteBuffer);
			fileChannel.close();
			return byteBuffer.array();
		} catch (Exception e) {
			logger.error("read file failed " + file.toString());
			logger.error("trace ", e);
			throw new Exception("read file failed " + file.toString());
		} finally {
			if (fileChannel != null) fileChannel.close();
		}
	}
	
	public static byte[] readAllBytesFromFile(String fileName) throws IOException {
		Path path = Paths.get(fileName);
		return Files.readAllBytes(path);
	}

	public static String readAllTextFromFile(String fileName) throws IOException {
		logger.info("THIS WAS CHANGED TO CONVERT FROM BYTE OUTPUT TO STRING");
		byte[] bytes = readAllBytesFromFile(fileName);
		return new String(bytes);
		//return bytes.toString();
	}
	
	public static void saveTextToFile(String fileName, String content) {
		if (content == null) {
			System.out.println("File not saved, content is null " + fileName);
			return;
		}
		FileOutputStream fop = null;
		File file;
		try {
 
			file = new File(fileName);
			fop = new FileOutputStream(file);
 
			// if file doesnt exists, then create it
			if (!file.exists()) {
				file.createNewFile();
			}
 
			// get the content in bytes
			byte[] contentInBytes = content.getBytes();
 
			fop.write(contentInBytes);
			fop.flush();
			fop.close();
 		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			try {
				if (fop != null) {
					fop.close();
				}
			} catch (IOException e) {
				e.printStackTrace();
			}
		}	
	}
	
	public static boolean isLocationAbsolute(String location) {
		String drive = location.substring(0, 2);
		if (drive.substring(1,2).equals(":")) {
			return true;
		}
		return false;
	}
	
	/*
	public static String[] convertStringToList(String str, String separator) {
		//String REGEX = "\s*,[,\s]*";
		
		//List<String> list = new ArrayList<String>(Arrays.asList(string.split(" , ")));
		//return new ArrayList<String>(Arrays.asList(string.split("separator")));
		//return Arrays.asList(str.split(separator));
		
		String[] values = str.split(separator);
		if (values == null) return null;
		for (int i = 0; i < values.length; i++) {
			values[i] = values[i].trim();
		}
		return values;
	}
	*/
	
	public static String convertDriveToPath(String drivePath) {
		if (drivePath.substring(1,2).equals(":")) {
			String driveLetter = drivePath.substring(0,1);
			return driveLetter + File.separator + drivePath.substring(2,drivePath.length());
		} 
		return null; 	// TODO, throw
	}
		/*
	public static void unZipJavaCryptoFiles(String zipFile) {

		byte[] buffer = new byte[1024];
		
		try{
			// get java_home for location of JRE files
			File javaHome = ClientNode.getJavaHome();
			
			ZipInputStream zis = new ZipInputStream(new FileInputStream(zipFile));
			ZipEntry ze = zis.getNextEntry();
			
			while(ze != null) {
				ze = zis.getNextEntry();
				
				String fileName = ze.getName();
				if (!(fileName.equals("UnlimitedJCEPolicy/local_policy.jar") || fileName.equals("UnlimitedJCEPolicy/US_export_policy.jar" ))) continue;
		
				int index = fileName.lastIndexOf("/");
				String fileNameOnly = fileName.substring(index+1, fileName.length());	
				File newFile = new File(javaHome.toString() + File.separator + "lib" + File.separator +
						"security" + File.separator + fileNameOnly);
				FileOutputStream fos = new FileOutputStream(newFile);             

				int len;
				while ((len = zis.read(buffer)) > 0) {
					fos.write(buffer, 0, len);
				}

				fos.close();   
				//ze = zis.getNextEntry();
			}

			zis.closeEntry();
			zis.close();

			System.out.println("Done");

		} catch(IOException ex){
			ex.printStackTrace(); 
		}
	} 
*/
	
	public static int executeCommandLine(final String commandLine, final boolean printOutput, final boolean printError,	final long timeout)	{

		Worker worker = null;
		Process process = null;

		try {

			// zero timeout is a forever process, verify not already running
			if (timeout == 0) {
				if (isAlreadyRunning(commandLine)) return 0;
			}
			
			
			Runtime runtime = Runtime.getRuntime();
			process = runtime.exec(commandLine);
			if (timeout == 0) return worker.exit;

			worker = new Worker(process);
			worker.start();
			
			worker.join(timeout);
			if (worker.exit != null)
				return worker.exit;
			else {
				logger.info("SystemUtility.executeCommandLine, timeout error, wait was: -1, " + commandLine + ", " + timeout);
				return -1;
			}
		} catch(InterruptedException ex) {
			worker.interrupt();
			Thread.currentThread().interrupt();
			logger.error("SystemUtility.executeCommandLine, interrupted exception: -2, " + commandLine + ", " + ex.toString());
			return -2;
		} catch (IOException io) {
			logger.error("SystemUtility.executeCommandLine, IOException: -3, " + commandLine + ", " + io.toString());
			return -3;
		}	finally {
			if (timeout == 0) return 0;		// zero timeout, indicates the process should continue to run
			process.destroy();
		}
	}
	
	private static class Worker extends Thread {
		private final Process process;
		private Integer exit;
		private Worker(Process process) {
			this.process = process;
		}
		public void run() {
			try { 
				exit = process.waitFor();
			} catch (InterruptedException ignore) {
				System.out.println("ignoring interruption");
				return;
			}
		}  
	}

	private static boolean isAlreadyRunning(String name) {

		try {
			String line;
			String pidInfo ="";

			Process p =Runtime.getRuntime().exec(System.getenv("windir") +"\\system32\\"+"tasklist.exe");

			BufferedReader input =  new BufferedReader(new InputStreamReader(p.getInputStream()));

			while ((line = input.readLine()) != null) {
				pidInfo+=line; 
			}

			input.close();

			if (pidInfo.contains(name)) {
				return true;// do what you want
			}
			
		} catch (Exception e) {
			System.out.println("already running fail " + e.toString());
		}
		return false;
	}

	// input fileSize, return shard size
	public static long getChunkSize(long fileSize) {
		
		if (fileSize < 2017) return 224;
		if (fileSize < 4065) return 480;
		if (fileSize < 8161) return 992;
		if (fileSize < 16353) return 2016;
		if (fileSize < 32737) return 4064;
		if (fileSize < 65505) return 8160;
		if (fileSize < 131041) return 16352;
		if (fileSize < 262113) return 32736;
		if (fileSize < 524257) return 65504;
		if (fileSize < 1048545) return 131040;
		if (fileSize < 2097121) return 262112;
		if (fileSize < 4194273) return 524256;
		if (fileSize < 8388577) return 1048544;
		if (fileSize < 16777185) return 2097120;
		if (fileSize < 33554401) return 4194272;
		if (fileSize < 67108833) return 4194272;
		if (fileSize < 134217697) return 4194272;
		if (fileSize < 268435425) return 4194272;
		if (fileSize < 536870881) return 4194272;
		if (fileSize < 1073741793) return 4194272;
		if (fileSize < 2147483617) return 4194272;
		return -1L;
	}
	
	/*
	// whatever main is running, met get and pass the runtime dir
	public static boolean init(MessageConfiguration msgConfig) {
		boolean loadHazel = false;
		try {
			runtimeDir = msgConfig.getRuntimeDir();
	    	
	    	int rr = msgConfig.getRole();
	    	String o = msgConfig.getEsClientNode();
			
	    	//loadConfigBook();

			// save the properties to a file for Camel
			String fileName = UTIL.getRuntimeDir() + File.separator + "camel.properties";
			UTIL.properties.store(new FileOutputStream(new File(fileName)), "Property file supporting Camel references");
			
			// verify and create REQUIRED folders included in the properties list
			String inputDir = UTIL.properties.get("INPUT_DIR").toString();
			String outputDir = UTIL.properties.get("OUTPUT_DIR").toString();
			UTIL.createDirIfNotExists(null, inputDir);
			UTIL.createDirIfNotExists(null, outputDir);
			
			UTIL.createDirIfNotExists(inputDir, UTIL.properties.get("WEB_INDEX_DIR").toString());
			UTIL.createDirIfNotExists(outputDir, UTIL.properties.get("WEB_INDEX_COMPLETE_DIR").toString());
			UTIL.createDirIfNotExists(inputDir, UTIL.properties.get("FILE_INDEX_DIR").toString());
			UTIL.createDirIfNotExists(outputDir, UTIL.properties.get("FILE_INDEX_COMPLETE_DIR").toString());
			
		} catch (Exception e) {
			System.out.println("error initializing Search " + e.toString());
		}
		return loadHazel;
	}
	*/

}
