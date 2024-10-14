import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.LinkedHashMap;
import java.util.Map;
import java.sql.PreparedStatement;

public class URLShortnerDB {
	
	private Connection mainConn=null;
	private Connection replicaConn=null;

    // Cache for Main DB
    private Map<String, String> mainCache;
    
    // Cache for Replica DB
    private Map<String, String> replicaCache;
    
    private static final int CACHE_SIZE = 100; 


	public URLShortnerDB(){ 
		this("jdbc:sqlite:/virtual/409a1db/database.db", "jdbc:sqlite:/virtual/409a1db/replica_database.db");
	}
	
	public URLShortnerDB(String mainUrl, String replicaUrl){ 
		mainConn = URLShortnerDB.connect(mainUrl);
        replicaConn = URLShortnerDB.connect(replicaUrl);

        mainCache = new LinkedHashMap<>(CACHE_SIZE, 0.75f, true) {
            protected boolean removeEldestEntry(Map.Entry<String, String> eldest) {
                return size() > CACHE_SIZE;
            }
        };
        
        replicaCache = new LinkedHashMap<>(CACHE_SIZE, 0.75f, true) {
            protected boolean removeEldestEntry(Map.Entry<String, String> eldest) {
                return size() > CACHE_SIZE;  
            }
        };
	}

    private static Connection connect(String url) {
		Connection conn = null;
		try {
			conn = DriverManager.getConnection(url);
			/**
			 * pragma locking_mode=EXCLUSIVE;
			 * pragma temp_store = memory;
			 * pragma mmap_size = 30000000000;
			 **/
			String sql = """
			pragma synchronous = normal;
			pragma journal_mode = WAL;
			""";
			Statement stmt  = conn.createStatement();
			stmt.executeUpdate(sql);

		} catch (SQLException e) {
			System.out.println(e.getMessage());
		}
		return conn;
	}
	
	public String findInMain(String shortURL) {
        if (mainCache.containsKey(shortURL)) {
            return mainCache.get(shortURL);
        }

        String longURL = find(shortURL, mainConn);

        if (longURL != null) {
            mainCache.put(shortURL, longURL);
        }
        return longURL;
    }

    public String findInReplica(String shortURL) {
        if (replicaCache.containsKey(shortURL)) {
            return replicaCache.get(shortURL);
        }

        String longURL = find(shortURL, replicaConn);

        if (longURL != null) {
            replicaCache.put(shortURL, longURL);
        }
        return longURL;
    }

	private String find(String shortURL, Connection conn) {
        try {
            String sql = "SELECT longurl FROM bitly WHERE shorturl=?;";
            PreparedStatement ps = conn.prepareStatement(sql);
            ps.setString(1, shortURL);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                return rs.getString("longurl");
            } else {
                return null;
            }
        } catch (SQLException e) {
            System.out.println(e.getMessage());
            return null;
        }
    }

    public Map<String, String> findUrlsInHashRange(String hashStart, String hashEnd, boolean useMainDb) {
        Connection conn = useMainDb ? mainConn : replicaConn;
        Map<String, String> results = new LinkedHashMap<>();
    
        try {
            String sql = "SELECT shorturl, longurl FROM bitly WHERE hash >= ? AND hash <= ?;";
            PreparedStatement ps = conn.prepareStatement(sql);
            ps.setString(1, hashStart);
            ps.setString(2, hashEnd);
            ResultSet rs = ps.executeQuery();
    
            while (rs.next()) {
                results.put(rs.getString("shorturl"), rs.getString("longurl"));
            }
        } catch (SQLException e) {
            System.out.println(e.getMessage());
        }
        return results;
    }

	public boolean saveToMain(String shortURL, String longURL, String hash) {
		boolean result = save(shortURL, longURL, hash, mainConn);
        if (result) {
            mainCache.put(shortURL, longURL);
        }
        return result;
	}
	
	public boolean saveToReplica(String shortURL, String longURL, String hash) {
		boolean result = save(shortURL, longURL, hash, replicaConn);
        if (result) {
            replicaCache.put(shortURL, longURL);
        }
        return result;
	}

	private boolean save(String shortURL, String longURL, String hash, Connection conn) {
        try {
            String insertSQL = "INSERT INTO bitly(shorturl,longurl,hash) VALUES(?,?,?) ON CONFLICT(shorturl) DO UPDATE SET longurl=?, hash=?;";
            PreparedStatement ps = conn.prepareStatement(insertSQL);
            ps.setString(1, shortURL);
            ps.setString(2, longURL);
            ps.setString(3, hash);  // Save the hash
            ps.setString(4, longURL);
            ps.setString(5, hash);  // Update hash on conflict
            ps.execute();
            return true;
        } catch (SQLException e) {
            System.out.println(e.getMessage());
            return false;
        }
    }

	public void closeConnections() {
        try {
            if (mainConn != null) {
                mainConn.close();
            }
            if (replicaConn != null) {
                replicaConn.close();
            }
        } catch (SQLException e) {
            System.out.println("Error closing database connections: " + e.getMessage());
        }
    }
}
