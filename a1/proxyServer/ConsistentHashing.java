import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.*;


public class ConsistentHashing {
    int MAX_HASH;
    TreeMap<Integer, String> circle = new TreeMap<>();
    HashMap<String, Integer> ipToHash = new HashMap<>();  

    public ConsistentHashing() {
        this.MAX_HASH = 10000;
    }

    private int hash(String key) {
        int hash = 0;
        int prime = 31;  // Use a prime number for better distribution
        for (int i = 0; i < key.length(); i++) {
            hash = prime * hash + key.charAt(i);
        }
        
        int scaledHash = Math.abs(hash % 10000);
        
        return scaledHash;
    }


    public int addNodeWithExistingData(String nodeIp) {
        addNode(nodeIp);
        int hash = ipToHash.get(nodeIp);
        SortedMap<Integer, String> tailMap = circle.tailMap(hash);
        
        int nextHash = tailMap.isEmpty() ? circle.firstKey() : tailMap.firstKey();
        return nextHash;

    }

    public List<Integer> removeNodeWithExistingData(String nodeIp) {
        int hash = ipToHash.get(nodeIp);
        SortedMap<Integer, String> tailMap = circle.tailMap(hash);
        SortedMap<Integer, String> headMap = circle.headMap(hash);

        // Find the next node (successive node) and previous node
        int nextHash = tailMap.isEmpty() ? circle.firstKey() : tailMap.firstKey();
        int prevHash = headMap.isEmpty() ? circle.lastKey() : headMap.lastKey();
        ArrayList<Integer> result = new ArrayList<>();
        result.add(prevHash);
        result.add(nextHash);
        removeNode(nodeIp);
        return result;
    }

    public String getIpAddress(int hash){
        return circle.get(hash);
    }

    

    public void addNode(String nodeIp) {
        if (circle.isEmpty()) {
            // First node starts at 10000
            circle.putIfAbsent(MAX_HASH, nodeIp);
            ipToHash.put(nodeIp, MAX_HASH);

        } else {
            // Find the largest gap between existing nodes
            int largestGap = 0;
            int newPosition = 0;

            // Iterate over the keys (node positions) in the circle
            Integer previousKey = circle.firstKey();
            for (Map.Entry<Integer, String> entry : circle.entrySet()) {
                int currentKey = entry.getKey();

                // Calculate gap between previous and current key
                int gap = currentKey - previousKey;
                if (gap > largestGap) {
                    largestGap = gap;
                    newPosition = (previousKey + currentKey) / 2;
                }

                previousKey = currentKey;
            }

            // Handle wrap-around gap (last node to the first node in the circle)
            int wrapAroundGap = (MAX_HASH - circle.lastKey()) + circle.firstKey();
            if (wrapAroundGap > largestGap) {
                largestGap = wrapAroundGap;
                newPosition = (circle.lastKey() + wrapAroundGap / 2) % MAX_HASH;
            }

            // Insert the new node in the largest gap's midpoint
            circle.put(newPosition, nodeIp);
            ipToHash.put(nodeIp, newPosition);
            System.out.println("Added node " + nodeIp + " at hash " + newPosition);
        }
    }


    public void removeNode(String nodeIp) {
        int hash = ipToHash.get(nodeIp);
        circle.remove(hash);
        ipToHash.remove(nodeIp);
    }

    public int getNode(String url) {
        int hashValue = hash(url);
        Map.Entry<Integer, String> entry = circle.ceilingEntry(hashValue);
        
        // If no node has a hash greater than or equal to the URL hash, wrap around to the first node
        if (entry == null) {
            entry = circle.firstEntry();
        }
        
        return entry.getKey();  
    }

    public int getReplicationNode(int nodeHash) {
        if(circle.size()<=1){
            return -1;
        }
        Map.Entry<Integer, String> entry = circle.higherEntry(nodeHash);
        
        // If there is no higher node, wrap around to the first node
        if (entry == null) {
            entry = circle.firstEntry();
        }
        
        return entry.getKey();  
    }

    public void printCircle() {
        System.out.println("Hash Circle:");
        for (Map.Entry<Integer, String> entry : circle.entrySet()) {
            System.out.println("Hash: " + entry.getKey() + " -> Node: " + entry.getValue());
        }
        
        System.out.println("\nIP to Hash Map:");
        for (Map.Entry<String, Integer> entry : ipToHash.entrySet()) {
            System.out.println("IP: " + entry.getKey() + " -> Hash: " + entry.getValue());
        }
    }

}

