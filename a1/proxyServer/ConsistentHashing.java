import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.*;

public class ConsistentHashing {
    int numVirtualNodes;
    TreeMap<Integer, String> circle = new TreeMap<>();
    Map<String, Set<Integer>> nodeCircleMap = new HashMap<>();

    public ConsistentHashing() {
        this.numVirtualNodes = 3;
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
        nodeCircleMap.putIfAbsent(nodeIp, new HashSet<>());
        for (int i = 0; i < numVirtualNodes; i++) {
            int hash = hash(nodeIp + i);
            circle.put(hash, nodeIp);
            nodeCircleMap.get(nodeIp).add(hash);
        }
    }

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

