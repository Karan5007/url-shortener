import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.*;


public class ConsistentHashing {
    int numVirtualNodes;
    TreeMap<Integer, String> circle = new TreeMap<>();
    Map<String, Set<Integer>> nodeCircleMap = new HashMap<>();

    public ConsistentHashing() {
        this.numVirtualNodes = 1;
    }

    private int hash(String key) {
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] digest = md.digest(key.getBytes());
            int hash = ((digest[3] & 0xFF) << 24) | ((digest[2] & 0xFF) << 16) | ((digest[1] & 0xFF) << 8) | (digest[0] & 0xFF);
            return hash & 0x7fffffff;  
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("MD5 algorithm not found");
        }
    }

    public void addNode(String nodeIp) {
        //nodeCircleMap.putIfAbsent(nodeIp, new HashSet<>());
        // for (int i = 0; i < numVirtualNodes; i++) {
        //     int hash = hash(nodeIp + i);
        //     circle.put(hash, nodeIp);
        //     nodeCircleMap.get(nodeIp).add(hash);
        // }

        int hash = hash(nodeIp);
        circle.putIfAbsent(hash, nodeIp);
    }

    public int addNodeWithExistingData(String nodeIp) {
        int hash = hash(nodeIp);
        SortedMap<Integer, String> tailMap = circle.tailMap(hash);
        
        int nextHash = tailMap.isEmpty() ? circle.firstKey() : tailMap.firstKey();
        circle.put(hash, nodeIp);

        return nextHash;

    }

    public List<Integer> removeNodeWithExistingData(String nodeIp) {
        int hash = hash(nodeIp);
        SortedMap<Integer, String> tailMap = circle.tailMap(hash);
        SortedMap<Integer, String> headMap = circle.headMap(hash);

        // Find the next node (successive node) and previous node
        int nextHash = tailMap.isEmpty() ? circle.firstKey() : tailMap.firstKey();
        int prevHash = headMap.isEmpty() ? circle.lastKey() : headMap.lastKey();
        circle.remove(hash);
        ArrayList<Integer> result = new ArrayList<>();
        result.add(prevHash);
        result.add(nextHash);
        return result;
    }

    public int getIpAddress(int hash){
        return circle.get(hash);
    }

    

    // public void addNode(String nodeIp) {
    //     int hash;
        
    //     // If the circle is empty, place the first node at Integer.MAX_VALUE
    //     if (circle.isEmpty()) {
    //         hash = Integer.MAX_VALUE;
    //     } else {
    //         // Find the largest gap between nodes and insert the new node in the middle
    //         hash = findLargestGap();
    //     }

    //     // Insert the node in the circle
    //     circle.put(hash, nodeIp);
    //     System.out.println("Node added at hash: " + hash + " for IP: " + nodeIp);
    // }

    // private int findLargestGap() {
    //     int maxGap = 0;
    //     int newHashPosition = 0;

    //     // Get the first and last entries to wrap around the circle
    //     Map.Entry<Integer, String> firstEntry = circle.firstEntry();
    //     Map.Entry<Integer, String> lastEntry = circle.lastEntry();

    //     // Handle wrapping around from the last node to the first node
    //     int wrapAroundGap = (Integer.MAX_VALUE - lastEntry.getKey()) + firstEntry.getKey();
    //     if (wrapAroundGap > maxGap) {
    //         maxGap = wrapAroundGap;
    //         newHashPosition = (lastEntry.getKey() + (wrapAroundGap / 2)) % Integer.MAX_VALUE;
    //     }

    //     // Iterate through the circle to find the largest gap between consecutive nodes
    //     Integer prev = null;
    //     for (Integer curr : circle.keySet()) {
    //         if (prev != null) {
    //             int gap = curr - prev;
    //             if (gap > maxGap) {
    //                 maxGap = gap;
    //                 newHashPosition = prev + (gap / 2);
    //             }
    //         }
    //         prev = curr;
    //     }

    //     return newHashPosition;
    // }


    public void removeNode(String nodeIp) {
        Set<Integer> hashes = nodeCircleMap.get(nodeIp);
        if (hashes != null) {
            for (int hash : hashes) {
                circle.remove(hash);
            }
            nodeCircleMap.remove(nodeIp);
        }
    }

    public List<String> getNodes(String key) {
        List<String> assignedNodes = new ArrayList<>();
        if (circle.isEmpty()) {
            return assignedNodes;
        }
        Integer hash = hash(key);
        Integer hash2 = -1;
        if (!circle.containsKey(hash)) {
            SortedMap<Integer, String> tailMap = circle.tailMap(hash);
            if(tailMap.isEmpty()){
                hash = circle.firstKey();
                hash2 = findNextHash(circle.get(hash), hash);
            }
            else{
                hash = tailMap.firstKey();
                hash2 = findNextHash(circle.get(hash), hash);
            }
        }
        System.out.println(circle.get(hash));
        System.out.println(circle.get(hash2));

        assignedNodes.add(circle.get(hash));
        assignedNodes.add(circle.get(hash2));
        return assignedNodes;
    }
    
    public int findNextHash(String first, Integer hash) {
        String node = first;
        while(node == first){
            hash = circle.higherKey(hash);
            if(hash == null){
                hash = circle.firstKey();
            }
            node = circle.get(hash);
        }
        return hash;
    }
    public Map<String, Set<Integer>> getAssignedNodes() {
        return Collections.unmodifiableMap(nodeCircleMap);
    }

}

