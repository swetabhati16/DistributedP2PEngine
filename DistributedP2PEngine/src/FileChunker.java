import java.io.*;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;

public class FileChunker {

    // Define block size: 1KB (1024 bytes) per chunk
    private static final int CHUNK_SIZE = 1024;

    // Data structure to hold our chunk metadata for the network
    public static class ChunkMetadata {
        public int chunkIndex;
        public String sha256Hash;
        public byte[] dataData;

        public ChunkMetadata(int chunkIndex, String sha256Hash, byte[] dataData) {
            this.chunkIndex = chunkIndex;
            this.sha256Hash = sha256Hash;
            this.dataData = dataData;
        }
    }

    /**
     * High-Class Logic: Slices a physical file into binary arrays and hashes each piece.
     */
    public static List<ChunkMetadata> splitFile(File file) throws IOException, NoSuchAlgorithmException {
        List<ChunkMetadata> chunks = new ArrayList<>();
        MessageDigest digest = MessageDigest.getInstance("SHA-256");

        try (InputStream inputStream = new FileInputStream(file)) {
            byte[] buffer = new byte[CHUNK_SIZE];
            int bytesRead;
            int index = 0;

            // Read the file block by block
            while ((bytesRead = inputStream.read(buffer)) != -1) {
                // Handle the final chunk which might be smaller than 1KB
                byte[] actualChunkData = new byte[bytesRead];
                System.arraycopy(buffer, 0, actualChunkData, 0, bytesRead);

                // Compute the SHA-256 Hash of this specific binary chunk
                byte[] hashBytes = digest.digest(actualChunkData);
                StringBuilder hexString = new StringBuilder();
                for (byte b : hashBytes) {
                    String hex = Integer.toHexString(0xff & b);
                    if (hex.length() == 1) hexString.append('0');
                    hexString.append(hex);
                }
                String chunkHash = hexString.toString();

                // Save chunk structural information
                chunks.add(new ChunkMetadata(index, chunkHash, actualChunkData));
                index++;
            }
        }
        return chunks;
    }

    // --- Main method to test the cryptographic chunker locally inside IntelliJ ---
    public static void main(String[] args) {
        try {
            // 1. Create a dummy test file on your hard drive to slice up
            File testFile = new File("manchester_test.txt");
            try (PrintWriter writer = new PrintWriter(testFile)) {
                writer.println("University of Manchester - Master of Advanced Computer Science Application Strategy.");
                writer.println("This is a high-class distributed architecture pipeline designed by a CSE engineer.");
                writer.println("Testing the binary layout integrity over simulated socket channels... Success.");
            }

            System.out.println("📦 [Chunker] Slicing file: " + testFile.getName() + " (" + testFile.length() + " bytes)");

            // 2. Execute the splitting algorithm
            List<ChunkMetadata> slicedData = splitFile(testFile);

            System.out.println("\n🔍 --- Generated Cryptographic Hashes ---");
            for (ChunkMetadata chunk : slicedData) {
                System.out.println("Piece [" + chunk.chunkIndex + "] SHA-256 -> " + chunk.sha256Hash);
            }

            // Clean up the dummy test file from your system
            testFile.delete();

        } catch (Exception e) {
            System.err.println("❌ Chunker Error: " + e.getMessage());
        }
    }
}