import java.io.*;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

public class HostFileManager {

    private static final String FILE_PATH = System.getProperty("user.home") + "/a1group05/a1/monitor/hosts.properties";

    // Ensure the file exists before performing operations
    private void ensureFileExists() throws IOException {
        File file = new File(FILE_PATH);
        if (!file.exists()) {
            file.createNewFile(); // Create the file if it does not exist
        }
    }

    // Method to read the hosts and their statuses from the .properties file
    public synchronized Map<String, String> readHosts() throws IOException {
        ensureFileExists(); // Ensure the file exists before reading

        Properties properties = new Properties();
        Map<String, String> hosts = new HashMap<>();

        // Open the file and obtain a lock for reading
        try (FileInputStream inputStream = new FileInputStream(FILE_PATH);
                FileChannel fileChannel = inputStream.getChannel();
                FileLock lock = fileChannel.lock(0L, Long.MAX_VALUE, true)) { // Shared lock for reading

            properties.load(inputStream);
        } catch (IOException e) {
            System.err.println("Error reading hosts file: " + e.getMessage());
            throw e;
        }

        // Convert properties into a Map<String, String> with "IP:Service" as key and
        // "status" as value
        for (String ipAndService : properties.stringPropertyNames()) {
            hosts.put(ipAndService, properties.getProperty(ipAndService, "alive"));
        }

        return hosts;
    }

    // Method to update the status of a host:service in the .properties file
    // Method to update the status of a host:service in the .properties file
    public synchronized void updateHostStatus(String ip, String service, String status) throws IOException {
        ensureFileExists(); // Ensure the file exists before reading

        Properties properties = new Properties();

        // Open the file using RandomAccessFile for reading and writing
        try (RandomAccessFile raf = new RandomAccessFile(FILE_PATH, "rw");
                FileChannel fileChannel = raf.getChannel();
                FileLock lock = fileChannel.lock()) { // Exclusive lock for writing

            // Load the current properties
            properties.load(new FileInputStream(FILE_PATH));

            // Update the host's status (e.g., "alive" or "failed")
            String key = ip + ":" + service;
            properties.setProperty(key, status);

            // Save the updated properties back to the file
            try (FileOutputStream outputStream = new FileOutputStream(FILE_PATH)) {
                properties.store(outputStream, null);
            }
        }
    }

    // Method to delete a host with service from the .properties file
    // Method to delete a host with service from the .properties file
    public synchronized void deleteHost(String ip, String service) throws IOException {
        ensureFileExists(); // Ensure the file exists before reading

        Properties properties = new Properties();

        // Open the file using RandomAccessFile for reading and writing
        try (RandomAccessFile raf = new RandomAccessFile(FILE_PATH, "rw");
                FileChannel fileChannel = raf.getChannel();
                FileLock lock = fileChannel.lock()) { // Exclusive lock for writing

            // Load the current properties
            properties.load(new FileInputStream(FILE_PATH));

            // Remove the host:service
            String key = ip + ":" + service;
            properties.remove(key);

            // Save the updated properties back to the file
            try (FileOutputStream outputStream = new FileOutputStream(FILE_PATH)) {
                properties.store(outputStream, null);
            }
        }
    }

    // Method to add a new host with its service to the .properties file
    // Method to add a new host with its service to the .properties file
    public synchronized void addHost(String ip, String service) throws IOException {
        ensureFileExists(); // Ensure the file exists before reading

        Properties properties = new Properties();

        // Open the file using RandomAccessFile for reading and writing
        try (RandomAccessFile raf = new RandomAccessFile(FILE_PATH, "rw");
                FileChannel fileChannel = raf.getChannel();
                FileLock lock = fileChannel.lock()) { // Exclusive lock for writing

            // Load the current properties
            properties.load(new FileInputStream(FILE_PATH));

            // Add the new host with its service and set the initial status to "alive"
            String key = ip + ":" + service;
            properties.setProperty(key, "alive");

            // Save the updated properties back to the file
            try (FileOutputStream outputStream = new FileOutputStream(FILE_PATH)) {
                properties.store(outputStream, null);
            }
        }
    }

}
