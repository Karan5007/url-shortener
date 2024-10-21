import java.io.*;
import java.net.*;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;



import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;


public class SimpleProxyServer {

	public static HostFileManager hostFileManager = new HostFileManager();

	public static void main(String[] args) throws IOException {

		InetAddress startAddress = InetAddress.getLocalHost();
		String addressAssString = startAddress.getHostAddress();
		System.out.println("I am running on: " + addressAssString);

		//TODO TEST MONITOR LOGIC
		System.out.println("start of if statement");
		boolean firstTime = false;
		if (args.length > 0) {
			// Parse the first argument as a boolean (true/false)
			firstTime = Boolean.parseBoolean(args[0]);
			System.out.println("inside if statement, boolean value is" + firstTime);
		}

		if (firstTime) {
			System.out.println("if statement for adding to hostfile");
			String addressAsString = "";
			try {
				InetAddress currentAddress = InetAddress.getLocalHost();
				addressAsString = currentAddress.getHostAddress();
				hostFileManager.addHost(addressAsString, "Proxy");
			} catch (IOException e) {
				System.err.println("Error reading hosts file: " + e.getMessage());
				return;
			}

		}

		 int MONITOR_INTERVAL = 10; // Monitor interval in seconds

		ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
		scheduler.scheduleAtFixedRate(SimpleProxyServer::monitorTheMonitor, 0, MONITOR_INTERVAL, TimeUnit.SECONDS);

		try {
			String host = "127.0.0.1"; // commenting this out, since we want diffrent hosts
			int remoteport = 8082;
			int localport = 8081;
			// Print a start-up message
			// System.out.println("Starting proxy for " + host + ":" + remoteport + " on
			// port " + localport); // old print statement
			System.out.println("Starting proxy for hosting  on port remote port: " + remoteport
					+ "and local port " + localport);

			// And start running the server

			runServer(remoteport, localport); // never returns
		} catch (Exception e) {
			System.err.println(e);
		}
	}

	public static void monitorTheMonitor() {
		String MonitorIp = "";
		HostFileManager hostFileManager = new HostFileManager();
		boolean restart = false;

		// read file
		Map<String, String> nodes;
		try {
			nodes = hostFileManager.readHosts(); // Retrieve nodes from hostFile with their statuses
		} catch (IOException e) {
			System.err.println("Error reading hosts file: " + e.getMessage());
			return;
		}
		// find monitor iP
		for (Map.Entry<String, String> entry : nodes.entrySet()) {
			// The key is in the format "IP:Service"
			String[] ipAndService = entry.getKey().split(":");
			String nodeIP = ipAndService[0]; // Extract IP
			String service = ipAndService[1]; // Extract Service type
			String status = entry.getValue(); // Get current status (alive/failed)

			// Only monitor nodes that are currently marked as "alive"
			if (service.equals("Monitor")) {
				MonitorIp = nodeIP;
				if (status.equals("failed")) { // this has already been identifed as failed, move on.
					return;
				}
				break; // we found the Ip of the monitor
			}
		}

		if (MonitorIp.equals("")) { // no montior found
			return; // TODO: maybe to the bottom instead?
			// restart = true;
		}

		// run bash script
		if (restart == false) { // try to ping monitor
			try {

				Process p = new ProcessBuilder("./Orchestration/monitorPing.bash", MonitorIp).start();
				boolean finished = p.waitFor(10, TimeUnit.SECONDS); // Wait for a maximum of 10 seconds
				if (!finished) {
					p.destroy();
					System.err.println("FATAL! Error running bash script: monitorPing Process Timeout");
					return;
				}
				int exitCode = p.exitValue();
				if (exitCode == 0) {
					System.out.println("Successfully Pinged Monitor App running @ " + MonitorIp);
					return;
				} else if (exitCode == 1) {
					System.out.println("Monitor is down! Attempting recovery");
					restart = true;
					hostFileManager.updateHostStatus(MonitorIp, "Monitor", "failed");
				}

			} catch (IOException | InterruptedException e) {
				System.err.println("Error running pingMonitor bash script: " + e.getMessage());
				return;
			}

		}

		if (restart == true) {
			String addressAsString;
			// Get available nodes from the hostFile
			Map<String, String> availableNodes;
			try {
				availableNodes = hostFileManager.readHosts(); // Retrieve available nodes from hostFile
				InetAddress currentAddress = InetAddress.getLocalHost();
				addressAsString = currentAddress.getHostAddress();

			} catch (IOException e) {
				System.err.println("Error reading hosts file: " + e.getMessage());
				return;
			}

			String og_monitor_ip = MonitorIp;
			// run bash scrip to restart Monitor
			for (Map.Entry<String, String> entry : availableNodes.entrySet()) {
				String[] ipAndService = entry.getKey().split(":");
				String nodeIP = ipAndService[0]; // Extract IP
				String service = ipAndService[1]; // Extract Service type
				String status = entry.getValue(); // Get current status (alive/failed)

				// TODO: add the case where there is only one
				if (!(nodeIP.equals(addressAsString))){
					if (startUpMonitorScript(nodeIP)) {
						System.out.println("Bash script executed on node: (" + nodeIP + ")");
						MonitorIp = nodeIP;
						System.out.println(addressAsString);
						try {
							hostFileManager.deleteHost(og_monitor_ip, "Monitor");
							hostFileManager.addHost(MonitorIp, "Monitor");

						} catch (IOException e) {

							System.err.println("Error editing hosts file: " + e.getMessage());
						}

						return; // Break after successfully running the script on one node
					} else {
						System.err.println("Failed to run bash script on node: " + nodeIP + ")");
					}
				}
				
				// Execute the bash script on the chosen node
				// In this case, we'll run the script locally for demonstration purposes

			}

			System.err.println("FATAL! Failed to run startup script on all nodes! No Monitor service is running!");

			// add new IP to host
			try {
				hostFileManager.addHost(MonitorIp, "Monitor");
			} catch (IOException e) {
				System.err.println("Failed to add to hostFile" + e.getMessage());
			}

			// run bash scrip to restart
		}

		// if on the exit value of the bash script

		// CASE: if exit != 0 (the monitor is not running)

		// remove the monitor from the hostFile

		// look for an alternative place to revive the monitor

		// start the monitor

		// add the monitor to hostFile

	}

	private static boolean startUpMonitorScript(String nodeIP) {
		try {

			Process p = new ProcessBuilder("./Orchestration/startMonitorRemote.bash", nodeIP).start();
			boolean finished = p.waitFor(10, TimeUnit.SECONDS); // Wait for a maximum of 10 seconds
			if (!finished) {
				// System.out.println("!finsih loop, not enough time to execute");
				p.destroy();
				return false;
			}
			int exitCode = p.exitValue();
			// System.out.println("exit value = " + exitCode);
			return exitCode == 0;

		} catch (IOException | InterruptedException e) {
			System.err.println("Error running bash script: " + e.getMessage());
			return false;
		}
	}

	public static void runServer(int remoteport, int localport)
			throws IOException {
		ConsistentHashing ch;

		// Create a ServerSocket to listen for connections with
		ServerSocket ss = new ServerSocket(localport);

		// final byte[] request = new byte[1024];
		// byte[] reply = new byte[4096];
		ch = loadObject("savedConsistentHashing");
		if (ch == null) {
			ch = new ConsistentHashing();
		}
		ch.printCircle();
		while (true) {
			Socket client = null, server = null;
			try {
				// Wait for a connection on the local port
				System.out.println("waiting....  ");

				client = ss.accept();
				// =====================================
				// Start threads
				System.out.println("received connection:  " + client);
				new Thread(new ProxyTask(client, remoteport, ch)).start();

			} catch (IOException e) {
				System.err.println(e);
			}
		} // end of while(true) loop.
	} // end of runServer function

	static class ProxyTask implements Runnable {
		private Socket client = null;
		private String host = null; // node
		private String replicationHost = null;
		private int remoteport; // port on node
		ConsistentHashing ch;

		public ProxyTask(Socket clientSocket, int remoteport, ConsistentHashing ch) {
			this.client = clientSocket;
			this.host = null;
			this.remoteport = remoteport;
			this.ch = ch;
		}

		@Override
		public void run() {
			Socket server = null;
			Socket Replication_server = null;
			try {

				// Create a connection to the remote server

				// read from the client
				final InputStream streamFromClient = client.getInputStream();
				final OutputStream streamToClient = client.getOutputStream();

				// get input (request) from client
				BufferedReader reader = new BufferedReader(new InputStreamReader(streamFromClient));
				String inputLine = reader.readLine();

				System.out.println("connected to client (opened stream to/from client)");

				// parse Request
				if (inputLine != null) {
					System.out.println(inputLine);

					ParsedRequest parsedRequest = parseRequest(inputLine);

					if (parsedRequest == null) {
						PrintWriter out = new PrintWriter(streamToClient);
						out.print("HTTP/1.1 400 Bad Request\r\n");
						out.print("Content-Type: text/plain\r\n");
						out.print("\r\n");
						out.print("Invalid request format. The request could not be parsed.\r\n");
						out.flush();
						client.close();
						return;

					}

					System.out.println("Parsed request: Method = " + parsedRequest.method +
							", Short URL = " + parsedRequest.shortResource +
							", Long URL = " + parsedRequest.longResource +
							", HTTP Version = " + parsedRequest.httpVersion);

					// Parsing for Node add/removing
					if (parsedRequest.method == "add") {
						System.out.println("added node:  " + parsedRequest.shortResource);
						ch.addNode(parsedRequest.shortResource);
						saveObject(ch, "savedConsistentHashing");
						try {
							hostFileManager.addHost(parsedRequest.shortResource, "DBnode");
						} catch (IOException e) {
							System.err.println("Error in Hostfilemanager add : " + e.getMessage());	
							e.printStackTrace();
						} 
						ch.printCircle();
						return;
					} else if (parsedRequest.method == "remove") { 
						ch.removeNode(parsedRequest.shortResource); 
						saveObject(ch, "savedConsistentHashing");
						try {
							hostFileManager.deleteHost(parsedRequest.shortResource, "DBnode");
						} catch (IOException e) {
							System.err.println("Error in Hostfilemanager remove : " + e.getMessage());	
							e.printStackTrace();
						}  
						return;
					} else if (parsedRequest.method == "addWithExistingData") {
						System.out.println("added node:  " + parsedRequest.shortResource);
						int hash = ch.addNodeWithExistingData(parsedRequest.shortResource);
						int nextHash = ch.getNextHash(hash);
						saveObject(ch, "savedConsistentHashing");
						String ipAddress = ch.getIpAddress(nextHash);

						try {
							server = new Socket(ipAddress, this.remoteport);
						} catch (IOException e) {
							PrintWriter out = new PrintWriter(streamToClient);
							out.print("HTTP/1.1 502 Bad Gateway\r\n");
							out.print("Content-Type: text/plain\r\n");
							out.print("\r\n");
							out.print("Proxy server cannot connect to " + this.host + ":"
									+ this.remoteport + ":\n" + e + "\n");
							out.flush();
							client.close();
							return;
						}
						final InputStream streamFromServer = server.getInputStream();
						final OutputStream streamToServer = server.getOutputStream();
						sendAddNodeRequest(hash, parsedRequest.shortResource, streamToServer);

						BufferedReader inFromServer = new BufferedReader(new InputStreamReader(streamFromServer));

						String responseLine;
						while ((responseLine = inFromServer.readLine()) != null) {
							System.out.println("Response from server: " + responseLine);
							// Break if you've read all the headers (the server will send an empty line to
							// indicate the end of headers)
							if (responseLine.isEmpty()) {
								break;
							}
						}

						return;
					} else if (parsedRequest.method == "removeWithExistingData") {
						System.out.println("removed node:  " + parsedRequest.shortResource);
						List<Integer> hashes = ch.removeNodeWithExistingData(parsedRequest.shortResource);
						
						try {
							hostFileManager.deleteHost(parsedRequest.shortResource, "DBnode");
						} catch (IOException e) {
							System.err.println("Error in Hostfilemanager remove : " + e.getMessage());	
							e.printStackTrace();
						}  
						String ipAddress = ch.getIpAddress(hashes.get(0));
						String nextIPAddress = ch.getIpAddress(hashes.get(1));

						System.out.println("first next ip addr = " + hashes);

						try {
							server = new Socket(nextIPAddress, this.remoteport);
						} catch (IOException e) {
							PrintWriter out = new PrintWriter(streamToClient);
							out.print("HTTP/1.1 502 Bad Gateway\r\n");
							out.print("Content-Type: text/plain\r\n");
							out.print("\r\n");
							out.print("Proxy server cannot connect to " + nextIPAddress + ":"
									+ this.remoteport + ":\n" + e + "\n");
							out.flush();
							client.close();
							return;
						}
						final InputStream streamFromServer = server.getInputStream();
						final OutputStream streamToServer = server.getOutputStream();
						int nextHash = ch.getNextHash(hashes.get(1));
						String ipAddrSend = ch.getIpAddress(nextHash);
						sendRemovePrevNodeRequest(streamToServer, ipAddrSend);

						BufferedReader inFromServer = new BufferedReader(new InputStreamReader(streamFromServer));

						String responseLine;
						while ((responseLine = inFromServer.readLine()) != null) {
							System.out.println("Response from server: " + responseLine);
							// Break if you've read all the headers (the server will send an empty line to
							// indicate the end of headers)
							if (responseLine.isEmpty()) {
								break;
							}
						}

						try {
							server = new Socket(ipAddress, this.remoteport);
						} catch (IOException e) {
							PrintWriter out = new PrintWriter(streamToClient);
							out.print("HTTP/1.1 502 Bad Gateway\r\n");
							out.print("Content-Type: text/plain\r\n");
							out.print("\r\n");
							out.print("Proxy server cannot connect to " + this.host + ":"
									+ this.remoteport + ":\n" + e + "\n");
							out.flush();
							client.close();
							return;
						}
						final InputStream streamFromServer2 = server.getInputStream();
						final OutputStream streamToServer2 = server.getOutputStream();
						System.out.println("next ip addr = " + nextIPAddress);
						sendRemoveNodeRequest(nextIPAddress, streamToServer2);

						BufferedReader inFromServer2 = new BufferedReader(new InputStreamReader(streamFromServer2));

						responseLine = "";
						while ((responseLine = inFromServer2.readLine()) != null) {
							System.out.println("Response from server: " + responseLine);
							// Break if you've read all the headers (the server will send an empty line to
							// indicate the end of headers)
							if (responseLine.isEmpty()) {
								break;
							}
						}

						return;
					}

					// parsing for data (GET/PUT)
					String url = parsedRequest.shortResource;

					int mainNode = ch.getNode(url);
					this.host = ch.getIpAddress(mainNode);

					int replicationNode = ch.getReplicationNode(mainNode);
					if (replicationNode != -1) {
						this.replicationHost = ch.getIpAddress(replicationNode);
					}
					System.out.println("Forwarding request to URL shortener at " + this.host + ":" + this.remoteport);
					System.out.println("Forwarding replication to URL shortener at " + this.replicationHost + ":"
							+ this.remoteport);

					// try to connect with main host
					try {
						server = new Socket(this.host, this.remoteport); // create socket for Node with main.db
					} catch (IOException e) {
						PrintWriter out = new PrintWriter(streamToClient);
						out.print("HTTP/1.1 502 Bad Gateway\r\n");
						out.print("Content-Type: text/plain\r\n");
						out.print("\r\n");
						out.print("Proxy server cannot connect to " + this.host + ":"
								+ this.remoteport + ":\n" + e + "\n");
						out.flush();
						client.close();

						return;
					}

					// Get server streams
					final InputStream streamFromServer = server.getInputStream();
					final OutputStream streamToServer = server.getOutputStream();

					// Forward the request based on the parsed type, we know this wont cause an
					// error since we error checked this
					if (parsedRequest != null && parsedRequest.method.equals("PUT")) {
						handlePutRequest(parsedRequest, streamToServer, ch.hash(url), 'M');
						if (replicationNode != -1) {
							try {
								Replication_server = new Socket(this.replicationHost, this.remoteport);
							} catch (IOException e) {
								PrintWriter out = new PrintWriter(streamToClient);
								out.print("HTTP/1.1 502 Bad Gateway\r\n");
								out.print("Content-Type: text/plain\r\n");
								out.print("\r\n");
								out.print("Proxy server cannot connect to Replication Server" + this.host + ":"
										+ this.remoteport + ":\n" + e + "\n");
								out.flush();
								client.close();
								server.close();
								return;
							}

							// Get server streams
							final OutputStream streamToServer2 = Replication_server.getOutputStream();
							final InputStream streamFromServer2 = Replication_server.getInputStream();
							handlePutRequest(parsedRequest, streamToServer2, ch.hash(url), 'R');

							byte[] reply = new byte[4096];
							int bytesRead;
							// StringBuilder responseBuilder = new StringBuilder();
							// server.setSoTimeout(50);
							while ((bytesRead = streamFromServer2.read(reply)) != -1) {
								// reading response of the replica
								String chunk = new String(reply, 0, bytesRead);
								streamToClient.write(reply, 0, bytesRead);
								streamToClient.flush();

								// if (chunk.contains("</html>")) {
								// break;
								// }
							}
							System.out.println("while loop exited for replica");

						}

					} else if (parsedRequest != null && parsedRequest.method.equals("GET")) {
						handleGetRequest(parsedRequest, streamToServer);

					}

					System.out.println("Received for response from URL shortener.");
					// Read the server's responses
					// and pass them back to the client.
					byte[] reply = new byte[4096];
					int bytesRead;
					// StringBuilder responseBuilder = new StringBuilder();
					// server.setSoTimeout(50);
					while ((bytesRead = streamFromServer.read(reply)) != -1) {
						String chunk = new String(reply, 0, bytesRead);
						streamToClient.write(reply, 0, bytesRead);
						streamToClient.flush();
						// if (chunk.contains("</html>")) {
						// break;
						// }
					}
					System.out.println("while loop exited");
					// server.setSoTimeout(1);;
					// while ((bytesRead = streamFromServer.read(reply)) != -1) {
					// String chunk = new String(reply, 0, bytesRead);
					// streamToClient.write(reply, 0, bytesRead);
					// streamToClient.flush();
					// if (chunk.contains("</html>")) {
					// break;
					// }
					// }
					// while(true){
					// //System.out.println("Sending response back to client.");
					// }

					// The server closed its connection to us, so we close our
					// connection to our client.

					// Close the streams and sockets
					// streamToClient.close();
					// streamToServer.close();
					// client.close();
					// server.close();
				}
			} catch (IOException e) {
				System.err.println(e);
			} finally {
				try {
					if (server != null){
						server.close();
						// System.out.println("shut down server");
					}
						
					if (Replication_server != null){
						Replication_server.close();
						// System.out.println("shut down replica server");
					}
						
					if (client != null)
						client.close();
						// System.out.println("shut down client connection");
				} catch (IOException e) {
					System.err.println("Error closing sockets: " + e.getMessage());
				}
			}
		}
	} // end of mulithread class

	private static ParsedRequest parseRequest(String inputLine) {
		// copying what we had form the node code.
		System.out.println("input:  " + inputLine);
		// Pattern premoveExisting =
		// Pattern.compile("^PUT\\s+/\\?method=failedNode&ipAddr=(\\S+)$");
		Pattern premoveExisting = Pattern.compile("^PUT\\s+/\\?method=failedNode&ipAddr=(\\S+)\\s+(\\S+)$");

		Pattern paddExisting = Pattern.compile("^PUT\\s+/\\?method=addedNode&ipAddr=(\\S+)\\s+(\\S+)$");
		Pattern padd = Pattern.compile("^PUT\\s+/\\?method=simpleAddNode&ipAddr=(\\S+)$");

		Pattern pput = Pattern.compile("^PUT\\s+/\\?short=(\\S+)&long=(\\S+)\\s+(\\S+)$");
		Pattern pget = Pattern.compile("^(GET)\\s+/(\\S+)\\s+(HTTP/\\S+)$");

		Matcher madd = padd.matcher(inputLine);
		Matcher maddExisting = paddExisting.matcher(inputLine);
		Matcher mremoveExisting = premoveExisting.matcher(inputLine);

		Matcher mput = pput.matcher(inputLine);
		Matcher mget = pget.matcher(inputLine);

		if (mput.matches()) {
			String shortResource = mput.group(1);
			String longResource = mput.group(2);
			String httpVersion = mput.group(3);
			return new ParsedRequest("PUT", shortResource, longResource, httpVersion);
		} else if (mget.matches()) {
			String shortResource = mget.group(2);
			String httpVersion = mget.group(3);
			return new ParsedRequest("GET", shortResource, null, httpVersion);
		} else if (madd.matches()) {
			String shortResource = madd.group(1);

			return new ParsedRequest("add", shortResource, null, null);
		} else if (maddExisting.matches()) {
			String shortResource = maddExisting.group(1);

			return new ParsedRequest("addWithExistingData", shortResource, null, null);
		} else if (mremoveExisting.matches()) {
			String shortResource = mremoveExisting.group(1);

			return new ParsedRequest("removeWithExistingData", shortResource, null, null);
		}

		return null; // Unknown request type, edit this later to add new servers/delete old servers.
	}

	// Handle PUT request
	private static void handlePutRequest(ParsedRequest parsedRequest, OutputStream streamToServer, int hash, char DB)
			throws IOException {
		System.out.println("PUT /?short=" + parsedRequest.shortResource + "&long=" + parsedRequest.longResource
				+ "&hash=" + hash + "&db=" + DB + " " + parsedRequest.httpVersion);

		PrintWriter outToServer = new PrintWriter(streamToServer);
		outToServer.println("PUT /?short=" + parsedRequest.shortResource + "&long=" + parsedRequest.longResource
				+ "&hash=" + hash + "&db=" + DB + " " + parsedRequest.httpVersion);
		outToServer.println("Host: " + parsedRequest.shortResource);
		outToServer.println(); // End of headers
		outToServer.flush();
	}

	private static void sendAddNodeRequest(int hash, String ipAddress, OutputStream streamToServer) throws IOException {
		System.out.println("PUT /?method=addedNode" + "&hash=" + hash + "&ipAddress=" + ipAddress + " HTTP/1.1");
		PrintWriter outToServer = new PrintWriter(streamToServer);
		outToServer.println("PUT /?method=addedNode" + "&hash=" + hash + "&ipAddress=" + ipAddress + " HTTP/1.1");
		outToServer.println();
		outToServer.flush();
	}

	private static void sendRemoveNodeRequest(String ipAddress, OutputStream streamToServer) throws IOException {
		PrintWriter outToServer = new PrintWriter(streamToServer);
		outToServer.println("PUT /?method=removedNode" + "&nextIpAddr=" + ipAddress + " HTTP/1.1");
		outToServer.println();
		outToServer.flush();
	}

	private static void sendRemovePrevNodeRequest(OutputStream streamToServer, String ipAddress) throws IOException {
		System.out.println("PUT /?method=removedPrevNode" + "&nextIpAddr=" + ipAddress + " HTTP/1.1");
		PrintWriter outToServer = new PrintWriter(streamToServer);
		outToServer.println("PUT /?method=removedPrevNode" + "&nextIpAddr=" + ipAddress + " HTTP/1.1");
		outToServer.println();
		outToServer.flush();
	}

	// Handle GET request
	private static void handleGetRequest(ParsedRequest parsedRequest, OutputStream streamToServer) throws IOException {
		PrintWriter outToServer = new PrintWriter(streamToServer);
		outToServer.println("GET /" + parsedRequest.shortResource + " " + parsedRequest.httpVersion);
		outToServer.println("Host: " + parsedRequest.shortResource);
		outToServer.println(); // End of headers
		outToServer.flush();
	}

	public static void saveObject(ConsistentHashing obj, String filename) {
		try (FileOutputStream fileOut = new FileOutputStream(filename);
				ObjectOutputStream out = new ObjectOutputStream(fileOut)) {
			out.writeObject(obj);
			System.out.println("Object saved to file.");
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	// Method to load the object from disk (deserialization)
	public static ConsistentHashing loadObject(String filename) {
		try (FileInputStream fileIn = new FileInputStream(filename);
				ObjectInputStream in = new ObjectInputStream(fileIn)) {
			return (ConsistentHashing) in.readObject();
		} catch (IOException | ClassNotFoundException e) {
			return null;
		}
	}

	// Class to store parsed request information, alt for adding new servers
	static class ParsedRequest {
		String method; // alt ADD/DESTORY
		String shortResource; // alt: new hostName (ip adder)
		String longResource; // alt: starting value
		String httpVersion; // alt: ending value

		ParsedRequest(String method, String shortResource, String longResource, String httpVersion) {
			this.method = method;
			this.shortResource = shortResource;
			this.longResource = longResource;
			this.httpVersion = httpVersion;
		}
	}

}