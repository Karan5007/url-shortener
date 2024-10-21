import java.io.IOException;
import java.net.InetSocketAddress;
// import java.net.ServerSocket;
import java.net.Socket;
import java.net.InetAddress;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.io.BufferedReader;
import java.io.FileReader;


// import a1.hostfilemanager.HostFileManager;

public class MonitorApp {

    private static final int MONITOR_INTERVAL = 10; // Monitor interval in seconds
    private static HostFileManager hostFileManager = new HostFileManager();
    private static String ProxyIp = "";
    public static boolean firstTime = false;




    public static void main(String[] args) {
        if (args.length > 0) {
            // Parse the first argument as a boolean (true/false)
            firstTime = Boolean.parseBoolean(args[0]);
        }
        
        if(firstTime){
            String addressAsString = "";    
            try {
                InetAddress currentAddress = InetAddress.getLocalHost();
                addressAsString = currentAddress.getHostAddress();
                hostFileManager.addHost(addressAsString, "Monitor");    
            } catch (IOException e) {
                System.err.println("Error reading hosts file: " + e.getMessage());
                return;
            }

            
        }
        // Use a ScheduledExecutor to periodically check the nodes
        ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
        scheduler.scheduleAtFixedRate(MonitorApp::checkNodes, 0, MONITOR_INTERVAL, TimeUnit.SECONDS);
    }
        

    // Method to periodically check all nodes in the hostFile
private static void checkNodes() {
    // Load the nodes and their statuses from the hostFile
    Map<String, String> nodes;
    try {
        nodes = hostFileManager.readHosts(); // Retrieve nodes from hostFile with their statuses
    } catch (IOException e) {
        System.err.println("Error reading hosts file: " + e.getMessage());
        return;
    }
    // String filePath = "hosts.properties"; // Adjust the path if needed

    //     System.out.println("starting to output file: ");
    //     // Use BufferedReader to read the file
    //     try (BufferedReader br = new BufferedReader(new FileReader(filePath))) {
    //         String line;
    //         // Read the file line by line and print each line
    //         while ((line = br.readLine()) != null) {
    //             System.out.println(line);
    //         }
    //     } catch (IOException e) {
    //         // Handle any IO exceptions (e.g., file not found)
    //         System.out.println("Error reading file: " + e.getMessage());
    //     }

    //     System.out.println("end of output file: ");
    // Iterate over the nodes and monitor each one
    for (Map.Entry<String, String> entry : nodes.entrySet()) {
        // The key is in the format "IP:Service"
        String[] ipAndService = entry.getKey().split(":");
        String nodeIP = ipAndService[0];  // Extract IP
        String service = ipAndService[1]; // Extract Service type
        String status = entry.getValue(); // Get current status (alive/failed)

        // Only monitor nodes that are currently marked as "alive"
        if (status.equals("alive") && !(service.equals("Monitor"))) {
            monitorNode(nodeIP, service);  // Monitor this IP:Service combination
        } else if (service.equals( "Monitor")) {
            
            System.out.println("Monitor not to be monitored! @ " + nodeIP);

        }else {
            // Skip nodes marked as "failed"
            System.out.println("Skipping " + service + " (" + nodeIP + ") as it is already marked as failed.");
        }
    }
}

// Method to monitor a node and notify the proxy if it fails
private static void monitorNode(String ip, String name) {
    boolean isAlive = checkNodeAlive(ip, name);

    if (!isAlive) {
        
        // Mark the node as "failed" to prevent further monitoring
        try {
            hostFileManager.updateHostStatus(ip, name, "failed");
        } catch (IOException e) {
            System.err.println("Error updating host status: " + e.getMessage());
        }
        // Notify the proxy and mark the node as "failed"
        if (name.equals("DBnode")) {
            handleDBNodeFailure (ip);
        } else if (name.equals("Proxy")) {
            handleProxyFailure(ip);
        }

    } else {
        // Log that the node is alive
        System.out.println(name + " (" + ip + ") is alive!");

        // Update ProxyIp if the service is Proxy
        // if (name.equals("Proxy")) {
        //     ProxyIp = ip;
        // }
    }
}

    // Method to check if a node is alive using TCP ping
    private static boolean checkNodeAlive(String ip, String name) {
        int port = 0;
        Socket server = null;
        try {
            // Assign port based on node type
            if (name.equals("DBnode")) {
                port = 8082; // Port for DB
            } else if (name.equals("Proxy")) {
                port = 8081; // Port for Proxy
            }

            // Create a new socket to ping the server
            server = new Socket();
            server.connect(new InetSocketAddress(ip, port), 5000); // 5 seconds timeout for connecting

            // If the connection is successful, the node is alive
            return true;

        } catch (IOException e) {
            // Connection failed, node is not responding
            System.err.println("Error connecting to " + name + " (" + ip + "): " + e.getMessage());
            return false;
        } finally {
            try {
                if (server != null) {
                    server.close(); // Close the connection after the check
                }
            } catch (IOException e) {
                System.err.println("Error closing connection: " + e.getMessage());
            }
        }
    }

    // Handle failure for DBnode: Send a message to the Proxy server
    private static void handleDBNodeFailure(String DBnodeIP) {

        String ProxyIP = null;
        //TODO: Find proxyIP
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
			String ListnodeIP = ipAndService[0]; // Extract IP
			String service = ipAndService[1]; // Extract Service type
			String status = entry.getValue(); // Get current status (alive/failed)
            System.out.println("IP " + ListnodeIP + " service: " + service );
			// Only monitor nodes that are currently marked as "alive"     
			if (service.equals("Proxy")) {
				ProxyIP = ListnodeIP ;
				break; // we found the Ip of the monitor
			}
		}


        System.out.println("DBnode failed. Notifying Proxy...");

        // String proxyIP = "192.168.1.11"; // Assume the Proxy's IP is known
        int proxyPort = 8081;
        System.out.println("sending to server at :" + ProxyIP + "and port: " + proxyPort);
        try (Socket proxySocket = new Socket(ProxyIP, proxyPort)) {

            String message =  "PUT /?method=failedNode&ipAddr=" + DBnodeIP + " HTTP/1.1";
            
            proxySocket.getOutputStream().write(message.getBytes());
            proxySocket.getOutputStream().flush();
            System.out.println("Message sent to Proxy: " + message);
        } catch (IOException e) {
            System.err.println("FATAL! Failed to notify Proxy: " + e.getMessage());
        }
    }
    //help its so over

    
    private static void handleProxyFailure(String og_proxyIP) {
        System.out.println("Proxy failed. Running recovery script on an available node...");
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

        // Pick an available node to restart ProxyServer
        for (Map.Entry<String, String> entry : availableNodes.entrySet()) {
            String[] ipAndService = entry.getKey().split(":");
            String nodeIP = ipAndService[0];  // Extract IP
            // String service = ipAndService[1]; // Extract Service type
            // String status = entry.getValue(); // Get current status (alive/failed)

            // TODO: add the case where there is only one

            if (!(nodeIP.equals(addressAsString))){
                if (runStartUpScript(nodeIP)) {
                    System.out.println("Bash script executed on node: (" + nodeIP + ")");
                    ProxyIp = nodeIP;
                    try {
                        System.err.println("og node adder: " + og_proxyIP);
                        hostFileManager.deleteHost(og_proxyIP,"Proxy");
                        System.out.println("old entry deleted.");
        
                        System.out.println("new node adder: " + nodeIP);
                        hostFileManager.addHost(nodeIP, "Proxy");
                        System.out.println("new entry added");
                    } catch (IOException e) {
            
                        System.err.println("Error editing hosts file: " + e.getMessage());
                        System.out.println("Error editing hosts file: " + e.getMessage());   
                    }
                    
                    return; // Break after successfully running the script on one node
                } else {
                    System.err.println("Failed to run bash script on node: (" + nodeIP + ")");
                }
            }else{
                System.out.println("Cannot run proxy and Montior on the same node");
            }
            
            // Execute the bash script on the chosen node
            // In this case, we'll run the script locally for demonstration purposes
            
        }
        System.err.println("FATAL! Failed to run startup script on all nodes! No Proxy Server is running!");
    }

    // Method to run a bash script locally 
    private static boolean runStartUpScript(String nodeIP) {
        try {

            Process p = new ProcessBuilder("./startProxyRemote.bash", nodeIP).start();
            boolean finished = p.waitFor(10, TimeUnit.SECONDS); // Wait for a maximum of 10 seconds
            if (!finished) {
                p.destroy(); 
                return false;
            }
            int exitCode = p.exitValue();
            return exitCode == 0;

        } catch (IOException | InterruptedException e) {
            System.err.println("Error running bash script: " + e.getMessage());
            return false;
        }
    }
}