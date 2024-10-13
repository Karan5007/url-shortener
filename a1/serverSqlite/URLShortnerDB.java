import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.ResultSet;
import java.sql.Statement;
import java.sql.PreparedStatement;

public class URLShortnerDB {
	
	private Connection mainConn=null;
	private Connection replicaConn=null;

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

	public URLShortnerDB(){ 
		this("jdbc:sqlite:/virtual/409a1db/database.db", "jdbc:sqlite:/virtual/409a1db/replica_database.db");
	}
	
	public URLShortnerDB(String mainUrl, String replicaUrl){ 
		mainConn = URLShortnerDB.connect(mainUrl);
		replicaConn = URLShortnerDB.connect(replicaUrl);
	}
	
	public String findInMain(String shortURL) {
        return find(shortURL, mainConn);
    }

    public String findInReplica(String shortURL) {
        return find(shortURL, replicaConn);
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

	public boolean saveToMain(String shortURL, String longURL) {
		return save(shortURL, longURL, mainConn);
	}
	
	public boolean saveToReplica(String shortURL, String longURL) {
		return save(shortURL, longURL, replicaConn);
	}

	private boolean save(String shortURL, String longURL, Connection conn) {
        try {
            String insertSQL = "INSERT INTO bitly(shorturl,longurl) VALUES(?,?) ON CONFLICT(shorturl) DO UPDATE SET longurl=?;";
            PreparedStatement ps = conn.prepareStatement(insertSQL);
            ps.setString(1, shortURL);
            ps.setString(2, longURL);
            ps.setString(3, longURL);
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
