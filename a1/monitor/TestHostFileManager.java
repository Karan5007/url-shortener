import java.io.IOException;
import java.util.Map;

public class TestHostFileManager {
    public static void main(String[] args) {
        // Create an instance of HostFileManager
        HostFileManager hostFileManager = new HostFileManager();
        
        try {
            // Test adding a host
            System.out.println("Testing addHost()...");
            hostFileManager.addHost("192.168.1.10", "Proxy");
            Map<String, String> hosts = hostFileManager.readHosts();
            if ("alive".equals(hosts.get("192.168.1.10:Proxy"))) {
                System.out.println("addHost test passed.");
            } else {
                System.out.println("addHost test failed.");
            }
            
            // Test updating a host's status
            System.out.println("Testing updateHostStatus()...");
            hostFileManager.updateHostStatus("192.168.1.10", "Proxy", "failed");
            hosts = hostFileManager.readHosts();
            if ("failed".equals(hosts.get("192.168.1.10:Proxy"))) {
                System.out.println("updateHostStatus test passed.");
            } else {
                System.out.println("updateHostStatus test failed.");
            }

            // Test deleting a host
            System.out.println("Testing deleteHost()...");
            hostFileManager.deleteHost("192.168.1.10", "Proxy");
            hosts = hostFileManager.readHosts();
            if (!hosts.containsKey("192.168.1.10:Proxy")) {
                System.out.println("deleteHost test passed.");
            } else {
                System.out.println("deleteHost test failed.");
            }

            // Test reading hosts from an empty file
            System.out.println("Testing readHosts() with empty file...");
            Map<String, String> emptyHosts = hostFileManager.readHosts();
            if (emptyHosts.isEmpty()) {
                System.out.println("readHosts test passed.");
            } else {
                System.out.println("readHosts test failed.");
            }

        } catch (IOException e) {
            System.err.println("An error occurred during testing: " + e.getMessage());
        }
        return;
    }

}
