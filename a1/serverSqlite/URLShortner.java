import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.PrintWriter;
import java.lang.reflect.Proxy;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.URL;
import java.util.Date;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.net.InetAddress;
import java.text.SimpleDateFormat;
import java.util.Date;

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
	static Socket server = null;

	static final int MAX_THREADS = 10;
	static File logDir = new File("thread_logs");
	static String ipAddress;

	static PrintWriter serverLogger = null;
	private static SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");


	// verbose mode
	static final boolean verbose = false;

	public static void main(String[] args) {
		ExecutorService threadPool = Executors.newFixedThreadPool(MAX_THREADS);  
		database = new URLShortnerDB();
		ipAddress = args[0];
		
		String host = "142.1.46.25"; // Ip address of simply proxy server.
		
		
		if (args.length > 1) {
			// Parse the second argument as, ipadder for server.
			host = args[1];
			System.out.println("ipadder of proxy: " + host);
		}
		
		
		
		initServerLogger();
		logInfo("Server is starting...");
		int proxyPort = 8081;
		connectToProxy(host, proxyPort, ipAddress);
	
		//open up our port to listen
		try {
			serverConnect = new ServerSocket(PORT);

			// we listen until user halts server execution
			while (true) {

				final Socket clientSocket = serverConnect.accept();
				threadPool.execute(()-> handle(clientSocket));
			}
		} catch (IOException e) {
			logError("Server Connection error : " + e.getMessage());
			closeSocket();
			cleanUpLogs();
		}
	}


	private static void connectToProxy(String host, int port, String ipAddress) {
		try  {
			server = new Socket(host, port);
			logInfo("Connected to proxy server at " + host + ":" + port);

			InputStream streamFromServer = null;
			OutputStream streamToServer = null;
			streamFromServer = server.getInputStream();
			streamToServer = server.getOutputStream();
			PrintWriter outToServer = new PrintWriter(streamToServer);
	
			outToServer.println("PUT /?method=simpleAddNode&ipAddr=" + ipAddress);
			outToServer.println(); 
			outToServer.flush();
	
			logInfo("Sent awake signal to proxy server at " + host + ":" + port);
		} catch (IOException e) {
			logError("Unable to connect to proxy server at " + host + ":" + port + ". Will retry...");
		} finally {
			// Ensure resources are closed in case of an exception
			if (server != null) {
				try {
					server.close();
				} catch (IOException e) {
					logError("Error closing Proxy connection socket: " + e.getMessage());
				}
			}
		}
	}
	

	public static void handle(Socket connect) {
		BufferedReader in = null;
		PrintWriter out = null;
		BufferedOutputStream dataOut = null;
		PrintWriter logWriter = null;
		String hostInfo=ipAddress+"_"+ getHostname();
	
		try {
			in = new BufferedReader(new InputStreamReader(connect.getInputStream()));
			out = new PrintWriter(connect.getOutputStream());
			dataOut = new BufferedOutputStream(connect.getOutputStream());
	
			String threadName = Thread.currentThread().getName();
			String logFileName = "thread_logs/"+hostInfo+"thread_" + threadName + "_log.txt";
			if (!new File("thread_logs").exists()) {
				new File("thread_logs").mkdir();
			}
	
			logWriter = new PrintWriter(new BufferedWriter(new FileWriter(logFileName, true)));
			String input = in.readLine();
	
			if (verbose) {
				logWriter.println("First line: " + input);
				logWriter.println("Handling request from: " + connect.getInetAddress() + " on thread: " + threadName);
			}
	
			logInfo("Handling request from: " + connect.getInetAddress() + " on thread: " + input);

			if (input == null ){
				return;
			}		
			
			// Handle node addition
			Pattern pAddNode = Pattern.compile("^PUT\\s+/\\?method=addedNode&hash=(\\d+)&ipAddress=(\\S+)\\s+(\\S+)$");
			Matcher mAddNode = pAddNode.matcher(input);
			if (mAddNode.matches()) {
				int hash = Integer.parseInt(mAddNode.group(1));
				String ipAddress = mAddNode.group(2);
				String httpVersion = mAddNode.group(3);
	
				// Handle adding a new node
				String output = handleAddNode(hash, ipAddress);
	
				sendResponse(out, dataOut, output+"HTTP/1.1 200 OK", REDIRECT_RECORDED);
				return;
			}
	
			// Handle node removal as previous Node
			Pattern pRemoveNextNode = Pattern.compile("^PUT\\s+/\\?method=removedNode&nextIpAddr=(\\S+)\\s+(\\S+)$");
			Matcher mRemoveNextNode = pRemoveNextNode.matcher(input);
			if (mRemoveNextNode.matches()) {
				String nextIpAddr = mRemoveNextNode.group(1);
				String httpVersion = mRemoveNextNode.group(2);
	
				// Handle node removal logic
				String result = handleRemoveNode(nextIpAddr);

				out.println("HTTP/1.1 200 OK");
				
				out.println("Server: Java HTTP Server/Shortner : 1.0");
				out.println("Date: " + new Date());
				out.println("Content-type: text/html");
				out.println("Content-length: " + result.length());
				
				out.println();
				out.flush();
	
				return;
			}

			// Handle node removal as previous Node
			Pattern pRemovePrevNode = Pattern.compile("^PUT\\s+/\\?method=removedPrevNode&nextIpAddr=(\\S+)\\s+(\\S+)$");
			Matcher mRemovePrevNode = pRemovePrevNode.matcher(input);
			if (mRemovePrevNode.matches()) {
				String nextIpAddr = mRemovePrevNode.group(1);
				String httpVersion = mRemovePrevNode.group(2);
	
				handleRemovePrevNode(nextIpAddr);

				out.println("HTTP/1.1 200 OK");
				
				out.println("Server: Java HTTP Server/Shortner : 1.0");
				out.println("Date: " + new Date());
				out.println("Content-type: text/html");
				out.println("Content-length: 51");
				out.println();
				out.flush();
	
				return;
			}
	
			// Handle PUT request for data
			Pattern pput = Pattern.compile("^PUT\\s+/\\?short=(\\S+)&long=(\\S+)&hash=(\\d+)(?:&db=(M|R))?\\s+(\\S+)$");
			Matcher mput = pput.matcher(input);
			if (mput.matches()) {
				String shortResource = mput.group(1);
				String longResource = mput.group(2);
				String hash = mput.group(3);
				String dbTarget = mput.group(4);
				String httpVersion = mput.group(5);
	
				if (dbTarget == null || "M".equalsIgnoreCase(dbTarget)) {
					synchronized (database) {
						logInfo("input:   " + input);

						database.saveToMain(shortResource, longResource, hash);  // Save to main DB with hash
					}
				} else if ("R".equalsIgnoreCase(dbTarget)) {
					synchronized (database) {
						database.saveToReplica(shortResource, longResource, hash);  // Save to replica DB with hash
					}
				}
	
				sendResponse(out, dataOut, "HTTP/1.1 201 OK", REDIRECT_RECORDED);

				try {
					if (in != null) in.close();
					if (out != null) out.close();
					if (dataOut != null) dataOut.close();
					if (connect != null) connect.close();
					if (logWriter != null) {
						logWriter.println("Connection closed on thread: " + Thread.currentThread().getName());
						logWriter.close();
					}
				} catch (Exception e2) {
					System.err.println("Error closing streams or socket: " + e2.getMessage());
				}
				return;
			}
	
			// Handle GET request
			Pattern pget = Pattern.compile("^(\\S+)\\s+/(\\S+)(?:&db=(M|R))?\\s+(\\S+)$");
			Matcher mget = pget.matcher(input);
			if (mget.matches()) {
				String method = mget.group(1);
				String shortResource = mget.group(2);
				String dbTarget = mget.group(3);
				String httpVersion = mget.group(4);
	
				String longResource;
				if (dbTarget == null || "M".equalsIgnoreCase(dbTarget)) {
					longResource = database.findInMain(shortResource);  // Fetch from main DB
				} else if ("R".equalsIgnoreCase(dbTarget)) {
					longResource = database.findInReplica(shortResource);  // Fetch from replica DB
				} else {
					longResource = null;
				}
				
				if (longResource != null) {
					sendResponse(out, dataOut, "HTTP/1.1 307 Temporary Redirect", REDIRECT, longResource);
				} else {
					sendResponse(out, dataOut, "HTTP/1.1 404 File Not Found", FILE_NOT_FOUND);
				}

				try {
					if (in != null) in.close();
					if (out != null) out.close();
					if (dataOut != null) dataOut.close();
					if (connect != null) connect.close();
					if (logWriter != null) {
						logWriter.println("Connection closed on thread: " + Thread.currentThread().getName());
						logWriter.close();
					}
				} catch (Exception e2) {
					System.err.println("Error closing streams or socket: " + e2.getMessage());
				}
			}

		} catch (Exception e) {

			if (logWriter != null) {
				logWriter.println("Server error: " + e.getMessage());
			}
			logError("Server error: " + e.getMessage());

			try {
				if (in != null) in.close();
				if (out != null) out.close();
				if (dataOut != null) dataOut.close();
				if (connect != null) connect.close();
				if (logWriter != null) {
					logWriter.println("Connection closed on thread: " + Thread.currentThread().getName());
					logWriter.close();
				}
			} catch (Exception e2) {
				logError("Error closing streams or socket: " + e2.getMessage());
			}
			closeSocket();  
			cleanUpLogs();  

		}
		
		finally{
			try {
				if (in != null) in.close();
				if (out != null) out.close();
				if (dataOut != null) dataOut.close();
				if (connect != null) connect.close();
				if (logWriter != null) {
					logWriter.println("Connection closed on thread: " + Thread.currentThread().getName());
					logWriter.close();
				}
			} catch (Exception e2) {
				System.err.println("Error closing streams or socket: " + e2.getMessage());
			}
			 
		}
	}
	
	private static void sendResponse(PrintWriter out, BufferedOutputStream dataOut, String status, String filePath) throws IOException {
		sendResponse(out, dataOut, status, filePath, null);
	}
	
	private static void sendResponse(PrintWriter out, BufferedOutputStream dataOut, String status, String filePath, String location) throws IOException {
		File file = new File(WEB_ROOT, filePath);
		int fileLength = (int) file.length();
		String contentMimeType = "text/html";
		byte[] fileData = readFileData(file, fileLength);
		
		out.println(status);
		if (location != null) {
			out.println("Location: " + location);
		}
		out.println("Server: Java HTTP Server/Shortner : 1.0");
		out.println("Date: " + new Date());
		out.println("Content-type: " + contentMimeType);
		out.println("Content-length: " + fileLength);
		out.println();
		out.flush();
		
		dataOut.write(fileData, 0, fileLength);
		dataOut.flush();
	}
	
	private static String handleAddNode(int hash, String ipAddress) {
		String output = moveReplicaDataToNewNode(ipAddress);
		String output2 = moveDataToNewNode(hash, ipAddress);
		return output2;
	}
	
	private static String handleRemoveNode(String nextIpAddr) {
		String result = moveMainDataToNextNode(nextIpAddr);
		
		serverLogger.println("Handling node removal. Next node IP: " + nextIpAddr);
		serverLogger.flush();
		return nextIpAddr;
	}
	
	private static void handleRemovePrevNode(String ipAddr) {
		moveReplicaDataToMainData(ipAddr);
		
		logInfo("Handling node removal.");
	}
	
	private static String moveReplicaDataToNewNode(String ipAddress) {
		logInfo("Transferring replica data to the newly added node at IP: " + ipAddress);
		
        // Fetch data from the replica DB and move it to the new node
        List<String[]> replicaData = database.fetchReplicaData();
        String result = "";
        for (String[] row : replicaData) {
			String shortURL = row[0];
            String longURL = row[1];
            String hash = row[2];
			
            // Send PUT request to the new node's replica DB
            result += sendPutRequest(ipAddress, shortURL, longURL, hash, "R");  // 'R' for replica
        }
		
        // Clear the current node's replica DB after transferring data
        database.clearReplicaData();
        return result;
    }
	
	private static String moveDataToNewNode(int hash, String ipAddress) {
		serverLogger.println("Transferring data based on hash to the newly added node at IP: " + ipAddress);
		
		// Fetch data from the main DB with hash <= specified hash
		List<String[]> mainData = database.fetchDataByHash(hash);
		String output = "";
		for (String[] row : mainData) {
			String shortURL = row[0];
			String longURL = row[1];
			String rowHash = row[2];
			// Send PUT requests to the new node's main and replica databases
			output += sendPutRequest(ipAddress, shortURL, longURL, rowHash, "M");  // 'M' for main
			database.saveToReplica(shortURL, longURL, rowHash);
		}
		
		// Delete transferred rows from the original node's main DB
		database.deleteRowsByHash(hash);
		serverLogger.flush();
		return output;
	}
	
	private static String moveMainDataToNextNode(String ipAddress) {
		serverLogger.println("Transferring main data to the next node at IP: " + ipAddress);
		serverLogger.flush();
		
		// Fetch data from the replica DB and move it to the new node
		List<String[]> mainData = database.fetchMainData();
		String result = "something";
		for (String[] row : mainData) {
			String shortURL = row[0];
			String longURL = row[1];
			String hash = row[2];
			
			// Send PUT request to the new node's replica DB
			String send = sendPutRequest(ipAddress, shortURL, longURL, hash, "R");  // 'R' for replica
			result += ipAddress;
		}
		return result;
	}

	private static void moveReplicaDataToMainData(String ipAddress) {
		serverLogger.println("Transferring Replica Data to Main Data: ");
		serverLogger.flush();
	
		// Fetch data from the replica DB and move it to the new node
		List<String[]> replicaData = database.fetchReplicaData();
		
		for (String[] row : replicaData) {
			String shortURL = row[0];
			String longURL = row[1];
			String hash = row[2];
			
			// Send PUT request to the new node's replica DB
			database.saveToMain(shortURL, longURL, hash);
			String send = sendPutRequest(ipAddress, shortURL, longURL, hash, "R");  // 'R' for replica
			
			
		}
		database.clearReplicaData();
	}
	
	private static void sendPersistentPutRequest(PrintWriter out, String ipAddress, String shortURL, String longURL, String hash, String dbTarget) {
		out.println("PUT /?short=" + shortURL + "&long=" + longURL + "&hash=" + hash + "&db=" + dbTarget + " HTTP/1.1");
		out.println("Host: " + ipAddress);
		out.println("Connection: keep-alive"); // Ensures the connection stays open
		out.println();  // End of headers
		out.flush();
			
		
		
	}
	
	private static String sendPutRequest(String ipAddress, String shortURL, String longURL, String hash, String dbTarget) {
		Socket newNodeSocket = null;
		try {
			newNodeSocket = new Socket(ipAddress, 8082);  // Assumes new node listens on port 8082
			PrintWriter out = new PrintWriter(newNodeSocket.getOutputStream());
			
			out.println("PUT /?short=" + shortURL + "&long=" + longURL + "&hash=" + hash + "&db=" + dbTarget + " HTTP/1.1");
			out.println("Host: " + ipAddress);
			out.println();  // End of headers
			out.flush();
			
			newNodeSocket.close();
			try {
				// Adding a delay of 100 milliseconds
				Thread.sleep(20);  // Delay in milliseconds
			} catch (InterruptedException e) {
				// Handle the InterruptedException if the thread is interrupted
				logError("Thread was interrupted: " + e.getMessage());
			}
			return "done successfully!";
		} catch (IOException e) {
			logError("Error sending PUT request to new node: " + e.getMessage());
			return e.getMessage();
		} finally{
			try{
				if(newNodeSocket != null){
					newNodeSocket.close();
				}
			}catch (IOException e) {
				return "IO exception, coukd not close newNodeSocket" + e.getMessage();
			}
			
		}
	}
	
	
	
	private static void cleanUpLogs() {
		if (logDir.exists() && logDir.isDirectory()) {
			File[] logFiles = logDir.listFiles();
            if (logFiles != null) {
				for (File logFile : logFiles) {
					logFile.delete();
                }
            }
            serverLogger.println("Log files and directory cleaned up.");
			serverLogger.flush();
        }
    }
	
	private static void closeSocket() {
		if (serverConnect != null && !serverConnect.isClosed()) {
			try {
				serverConnect.close();
		        serverLogger.println("Server socket closed due to an exception.");
				serverLogger.flush();
		    } catch (IOException closeEx) {
				logError("Error closing server socket after exception: " + closeEx.getMessage());
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
	private static String getHostname() {
		try{
			return InetAddress.getLocalHost().getHostName();
		} catch (Exception e){
			logError(e.getMessage());
			return "unknown hostname";
		}
	}

	private static void initServerLogger() {
		String hostname = getHostname();
		String logFileName = "server_logs/" + ipAddress + "_" + hostname + "_server_log.txt";
	
		try {
			File logDir = new File("server_logs");
			if (!logDir.exists()) {
				logDir.mkdir();
			}
			serverLogger = new PrintWriter(new BufferedWriter(new FileWriter(logFileName, true)));
		} catch (IOException e) {
			logError("Error initializing server logger: " + e.getMessage());
		}
	}

	private static void logError(String message) {
		if (serverLogger != null) {
			serverLogger.println("[" + sdf.format(new Date()) + "] [ERROR] " + message);
			serverLogger.flush();
		}
	}
	
	private static void logInfo(String message) {
		if (serverLogger != null) {
			serverLogger.println("[" + sdf.format(new Date()) + "] [INFO] " + message);
			serverLogger.flush();
		}
	}
}