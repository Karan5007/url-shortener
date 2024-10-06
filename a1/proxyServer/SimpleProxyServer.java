import java.io.*;
import java.net.*;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class SimpleProxyServer {

	public static void main(String[] args) throws IOException {
		try {
			String host = "127.0.0.1"; // commenting this out, since we want diffrent hosts
			int remoteport = 8082;
			int localport = 8081;
			// Print a start-up message
			// System.out.println("Starting proxy for " + host + ":" + remoteport + " on port " + localport); // old print statement
			System.out.println("Starting proxy for hosting  on port remote port: " + remoteport
					+ "and local port " + localport);

			

			// List<String> nodes = Arrays.asList("192.168.0.1", "192.168.0.2", "192.168.0.3");
			// ConsistentHashing ch = new ConsistentHashing();
			// ch.addNode("192.168.0.1");
			// ch.addNode("192.168.0.2");
			// ch.addNode("192.168.0.3");
			// String key = "my-data";
			// List<String> nodesForKey = ch.getNodes(key);

			// System.out.println("Replicated nodes for key " + key + ": " + nodesForKey);

			// System.out.println("Assigned nodes" + ch.getAssignedNodes());


			// And start running the server
			runServer(remoteport, localport); // never returns
		} catch (Exception e) {
			System.err.println(e);
		}
	}

	/**
	 * runs a single-threaded proxy server on
	 * the specified local port. It never returns.
	 */
	public static void runServer(int remoteport, int localport)
			throws IOException {
		// Create a ServerSocket to listen for connections with
		ServerSocket ss = new ServerSocket(localport);

		// final byte[] request = new byte[1024];
		// byte[] reply = new byte[4096];

		while (true) {
			Socket client = null, server = null;
			try {
				// Wait for a connection on the local port
				client = ss.accept();
				// =====================================
				// Start threads
				new Thread(new ProxyTask(client, remoteport)).start();

			} catch (IOException e){
				System.err.println(e);
			}
	 	} // end of while(true) loop.
	} // end of runServer function

	static class ProxyTask implements Runnable {
        private Socket client;
        private String host; //node
		private String replicationHost;
        private int remoteport; // port on node
		ConsistentHashing ch;


        public ProxyTask(Socket clientSocket, int remoteport) {
            this.client = clientSocket;
            this.host = null;
            this.remoteport = remoteport;
			this.ch = new ConsistentHashing();
        }

        @Override
        public void run() {
            Socket server = null;
            try {
                // Create a connection to the remote server
                
				// read from the client
                final InputStream streamFromClient = client.getInputStream();
                final OutputStream streamToClient = client.getOutputStream();
				
				//get input (request) from client
                BufferedReader reader = new BufferedReader(new InputStreamReader(streamFromClient));
                String inputLine = reader.readLine();

				//parse Request
                if (inputLine != null) {
                    ParsedRequest parsedRequest = parseRequest(inputLine);

					if (parsedRequest == null){
						PrintWriter out = new PrintWriter(streamToClient);
						out.print("HTTP/1.1 400 Bad Request\r\n");
						out.print("Content-Type: text/plain\r\n");
						out.print("\r\n");
						out.print("Invalid request format. The request could not be parsed.\r\n");
						out.flush();
						client.close();
						return;
												
					}
					
					// TODO: check if parsedRequest.method == add or remove (for adding servers/getting rid of them)
					if(parsedRequest.method == "add"){
						ch.addNode(parsedRequest.shortResource);
					}else if(parsedRequest.method == "remove"){ // TODO: account for data.
						ch.removeNode(parsedRequest.shortResource); // need to account for data moving
 					}


					// TODO: use parseRequest.shortResponse to hash and figure out server host
					String url = parsedRequest.shortResource;
					List<String> assignedNodes = ch.getNodes(url);
					this.host = assignedNodes.get(0);
					this.replicationHost = assignedNodes.get(1); // How to account if there is only 1 node??
					
					this.host = "127.0.0.1"; // set host to the orginal host for now
					
					// Add replication stuff
					// Make a connection to the real server.
					// If we cannot connect to the server, send an error to the
					// client, disconnect, and continue waiting for connections.
					try {
						server = new Socket(this.host, this.remoteport);
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
					
					// Forward the request based on the parsed type, we know this wont cause an error since we error checked this
					if (parsedRequest != null && parsedRequest.method.equals("PUT")) {
						handlePutRequest(parsedRequest, streamToServer);
					} else if (parsedRequest != null && parsedRequest.method.equals("GET")) {
						handleGetRequest(parsedRequest, streamToServer);
					} 

					// Read the server's responses
					// and pass them back to the client.
					byte[] reply = new byte[4096];
					int bytesRead;
					while ((bytesRead = streamFromServer.read(reply)) != -1) {
						streamToClient.write(reply, 0, bytesRead);
						streamToClient.flush();
					}
					// The server closed its connection to us, so we close our
					// connection to our client.
					
					// Close the streams and sockets
					streamToClient.close();
					streamToServer.close();
					client.close();
					server.close();
				}
            } catch (IOException e) {
                System.err.println(e);
            } finally {
                try {
                    if (server != null) server.close();
                    if (client != null) client.close();
                } catch (IOException e) {
                    System.err.println("Error closing sockets: " + e.getMessage());
                }
            }
        }
    } // end of mulithread class

	private static ParsedRequest parseRequest(String inputLine) {
    	//copying what we had form the node code.
        Pattern pput = Pattern.compile("^PUT\\s+/\\?short=(\\S+)&long=(\\S+)\\s+(\\S+)$");
        Pattern pget = Pattern.compile("^(GET)\\s+/(\\S+)\\s+(HTTP/\\S+)$");

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
        }

        return null; // Unknown request type, edit this later to add new servers/delete old servers.
    }

    // Handle PUT request
    private static void handlePutRequest(ParsedRequest parsedRequest, OutputStream streamToServer) throws IOException {
        PrintWriter outToServer = new PrintWriter(streamToServer);
        outToServer.println("PUT /?short=" + parsedRequest.shortResource + "&long=" + parsedRequest.longResource + " " + parsedRequest.httpVersion);
        outToServer.println("Host: " + parsedRequest.shortResource);
        outToServer.println(); // End of headers
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

} // end of SimpleProxyServer
