import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.PrintWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.URL;
import java.util.Date;
import java.util.StringTokenizer;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class URLShortner { 
	
	static final File WEB_ROOT = new File(".");
	static final String DEFAULT_FILE = "index.html";
	static final String FILE_NOT_FOUND = "404.html";
	static final String METHOD_NOT_SUPPORTED = "not_supported.html";
	static final String REDIRECT_RECORDED = "redirect_recorded.html";
	static final String REDIRECT = "redirect.html";
	static final String NOT_FOUND = "notfound.html";
	static URLShortnerDB database=null;
	// port to listen connection
	static final int PORT = 8082;
    static ServerSocket serverConnect = null;

	static final int MAX_THREADS = 10;
	// verbose mode
	static final boolean verbose = true;

	public static void main(String[] args) {
		ExecutorService threadPool = Executors.newFixedThreadPool(MAX_THREADS);  // Thread pool
        File logDir = new File("thread_logs");
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            cleanUpLogs(logDir);
        }));

		database = new URLShortnerDB();
		
		try {
			serverConnect = new ServerSocket(PORT);
			System.out.println("Server started.\nListening for connections on port : " + PORT + " ...\n");
			
			// we listen until user halts server execution
			while (true) {
				if (verbose) { System.out.println("Connection opened. (" + new Date() + ")"); }

				final Socket clientSocket = serverConnect.accept();
				threadPool.execute(()-> handle(clientSocket));
			}
		} catch (IOException e) {
			System.err.println("Server Connection error : " + e.getMessage());
			closeSocket();
		}
	}



	public static void handle(Socket connect) {
		BufferedReader in = null; PrintWriter out = null; BufferedOutputStream dataOut = null; PrintWriter logWriter = null;
		
		try {
			in = new BufferedReader(new InputStreamReader(connect.getInputStream()));
			out = new PrintWriter(connect.getOutputStream());
			dataOut = new BufferedOutputStream(connect.getOutputStream());
			
			
            String threadName = Thread.currentThread().getName();
			String logFileName = "thread_logs/thread_" + threadName + "_log.txt";

			logWriter = new PrintWriter(new BufferedWriter(new FileWriter(logFileName, true))); // Open in append mode


			String input = in.readLine();
			
			if (verbose) {
                logWriter.println("First line: " + input);
                logWriter.println("Handling request from: " + connect.getInetAddress() + " on thread: " + threadName);
            }

			Pattern pput = Pattern.compile("^PUT\\s+/\\?short=(\\S+)&long=(\\S+)\\s+(\\S+)$");
			Matcher mput = pput.matcher(input);
			if(mput.matches()){
				String shortResource=mput.group(1);
				String longResource=mput.group(2);
				String httpVersion=mput.group(3);

				database.save(shortResource, longResource);

				File file = new File(WEB_ROOT, REDIRECT_RECORDED);
				int fileLength = (int) file.length();
				String contentMimeType = "text/html";
				//read content to return to client
				byte[] fileData = readFileData(file, fileLength);
					
				out.println("HTTP/1.1 200 OK");
				out.println("Server: Java HTTP Server/Shortner : 1.0");
				out.println("Date: " + new Date());
				out.println("Content-type: " + contentMimeType);
				out.println("Content-length: " + fileLength);
				out.println(); 
				out.flush(); 

				dataOut.write(fileData, 0, fileLength);
				dataOut.flush();
			} else {
				Pattern pget = Pattern.compile("^(\\S+)\\s+/(\\S+)\\s+(\\S+)$");
				Matcher mget = pget.matcher(input);
				if(mget.matches()){
					String method=mget.group(1);
					String shortResource=mget.group(2);
					String httpVersion=mget.group(3);

					String longResource = database.find(shortResource);
					if(longResource!=null){
						File file = new File(WEB_ROOT, REDIRECT);
						int fileLength = (int) file.length();
						String contentMimeType = "text/html";
	
						//read content to return to client
						byte[] fileData = readFileData(file, fileLength);
						
						// out.println("HTTP/1.1 301 Moved Permanently");
						out.println("HTTP/1.1 307 Temporary Redirect");
						out.println("Location: "+longResource);
						out.println("Server: Java HTTP Server/Shortner : 1.0");
						out.println("Date: " + new Date());
						out.println("Content-type: " + contentMimeType);
						out.println("Content-length: " + fileLength);
						out.println(); 
						out.flush(); 
	
						dataOut.write(fileData, 0, fileLength);
						dataOut.flush();
					} else {
						File file = new File(WEB_ROOT, FILE_NOT_FOUND);
						int fileLength = (int) file.length();
						String content = "text/html";
						byte[] fileData = readFileData(file, fileLength);
						
						out.println("HTTP/1.1 404 File Not Found");
						out.println("Server: Java HTTP Server/Shortner : 1.0");
						out.println("Date: " + new Date());
						out.println("Content-type: " + content);
						out.println("Content-length: " + fileLength);
						out.println(); 
						out.flush(); 
						
						dataOut.write(fileData, 0, fileLength);
						dataOut.flush();
					}
				}
			}
		} catch (Exception e) {
			if (logWriter != null) {
                logWriter.println("Server error: " + e.getMessage());
            }
            System.err.println("Server error: " + e.getMessage());
		} finally {
			try {
                if (in != null) in.close();
                if (out != null) out.close();
                if (connect != null) connect.close();
                if (logWriter != null) {
                    logWriter.println("Connection closed on thread: " + Thread.currentThread().getName());
                    logWriter.close();
                }
                if (verbose) {
                    System.out.println("Connection closed on thread: " + Thread.currentThread().getName());
                }
				closeSocket();
            } catch (Exception e) {
				closeSocket();
                System.err.println("Error closing stream: " + e.getMessage());
            }
		}
	}

	

	private static void cleanUpLogs(File logDir) {
        if (logDir.exists() && logDir.isDirectory()) {
            File[] logFiles = logDir.listFiles();
            if (logFiles != null) {
                for (File logFile : logFiles) {
                    logFile.delete();
                }
            }
            System.out.println("Log files and directory cleaned up.");
        }
    }

	private static void closeSocket() {
		if (serverConnect != null && !serverConnect.isClosed()) {
		    try {
		        serverConnect.close(); // Close the server socket to free the port
		        System.out.println("Server socket closed due to an exception.");
		    } catch (IOException closeEx) {
		        System.err.println("Error closing server socket after exception: " + closeEx.getMessage());
		    }
		}
	}

	private static byte[] readFileData(File file, int fileLength) throws IOException {
		FileInputStream fileIn = null;
		byte[] fileData = new byte[fileLength];
		
		try {
			fileIn = new FileInputStream(file);
			fileIn.read(fileData);
		} finally {
			if (fileIn != null) 
				fileIn.close();
		}
		
		return fileData;
	}
}
