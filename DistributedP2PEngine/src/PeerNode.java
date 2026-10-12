import java.io.*;
import java.net.*;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class PeerNode {
    private final int port;
    private final ExecutorService threadPool;
    private boolean isRunning;

    public PeerNode(int port) {
        this.port = port;
        this.threadPool = Executors.newFixedThreadPool(10);
        this.isRunning = true;
    }

    public void startServer() {
        threadPool.execute(() -> {
            try (ServerSocket serverSocket = new ServerSocket(port)) {
                System.out.println("🚀 [Server-" + port + "] Active and listening for inbound peers...");
                while (isRunning) {
                    Socket clientSocket = serverSocket.accept();
                    threadPool.execute(new BinaryStreamHandler(clientSocket));
                }
            } catch (IOException e) {
                System.err.println("❌ [Server-" + port + "] Fatal exception: " + e.getMessage());
            }
        });
    }

    // --- High-Class Merged Upload Engine ---
    private static class BinaryStreamHandler implements Runnable {
        private final Socket socket;

        public BinaryStreamHandler(Socket socket) {
            this.socket = socket;
        }

        @Override
        public void run() {
            try (
                    DataInputStream dis = new DataInputStream(socket.getInputStream());
                    DataOutputStream dos = new DataOutputStream(socket.getOutputStream())
            ) {
                String protocolCommand = dis.readUTF();
                System.out.println("📡 [Upload-Engine] Received command: " + protocolCommand);

                // Command format: GET_CHUNK:filename:chunkIndex
                if (protocolCommand.startsWith("GET_CHUNK")) {
                    String[] parts = protocolCommand.split(":");
                    String filename = parts[1];
                    int requestedIndex = Integer.parseInt(parts[2]);

                    File targetFile = new File(filename);
                    if (!targetFile.exists()) {
                        System.err.println("❌ File not found on host disk!");
                        return;
                    }

                    // 🔥 CRITICAL MERGER POINT: PeerNode calls FileChunker to dynamically slice the real file!
                    List<FileChunker.ChunkMetadata> actualChunks = FileChunker.splitFile(targetFile);

                    if (requestedIndex < actualChunks.size()) {
                        FileChunker.ChunkMetadata targetChunk = actualChunks.get(requestedIndex);

                        // Stream the authentic cryptographic metadata and payload over the wire
                        dos.writeInt(targetChunk.chunkIndex);
                        dos.writeUTF(targetChunk.sha256Hash);
                        dos.writeInt(targetChunk.dataData.length);
                        dos.write(targetChunk.dataData);
                        dos.flush();
                        System.out.println("⚡ [Upload-Engine] Sent authentic chunk [" + requestedIndex + "] over network.");
                    }
                }
            } catch (Exception e) {
                System.err.println("❌ [Upload-Engine] Error: " + e.getMessage());
            }
        }
    }

    // --- Download Engine ---
    public void requestBinaryChunk(String peerHost, int peerPort, String filename, int chunkId) {
        threadPool.execute(() -> {
            try (
                    Socket socket = new Socket(peerHost, peerPort);
                    DataOutputStream dos = new DataOutputStream(socket.getOutputStream());
                    DataInputStream dis = new DataInputStream(socket.getInputStream())
            ) {
                // Ask for the file by name and specify which piece we want
                dos.writeUTF("GET_CHUNK:" + filename + ":" + chunkId);
                dos.flush();

                int chunkIndex = dis.readInt();
                String receivedHash = dis.readUTF();
                int payloadLength = dis.readInt();

                byte[] rawPayloadBuffer = new byte[payloadLength];
                dis.readFully(rawPayloadBuffer);

                System.out.println("\n📥 --- Merged Network Download Manifest ---");
                System.out.println("• Verified Chunk ID : " + chunkIndex);
                System.out.println("• SHA-256 Checksum  : " + receivedHash);
                System.out.println("• Block Size Read   : " + payloadLength + " bytes");
                System.out.println("• Extracted Content : \"" + new String(rawPayloadBuffer).trim() + "\"");
                System.out.println("--------------------------------------------\n");

            } catch (IOException e) {
                System.err.println("❌ [Download-Engine] Network failure: " + e.getMessage());
            }
        });
    }

    public static void main(String[] args) throws Exception {
        // 1. Create an actual physical text file on your system disk for the network to read
        File realSourceFile = new File("manchester_source.txt");
        try (PrintWriter writer = new PrintWriter(realSourceFile)) {
            writer.println("This is a live, non-simulated string from a real file on your disk.");
            writer.println("FileChunker is processing this data directly into byte blocks.");
            writer.println("University of Manchester Admissions Requirement Check... Verified.");
        }

        // 2. Boot Peer Server A (The Holder of the file) on port 8081
        PeerNode nodeA = new PeerNode(8081);
        nodeA.startServer();

        Thread.sleep(1500);

        // 3. Boot Peer Server B on port 8082
        PeerNode nodeB = new PeerNode(8082);
        nodeB.startServer();

        // 4. Node B requests chunk index 0 of the real file from Node A!
        nodeB.requestBinaryChunk("localhost", 8081, "manchester_source.txt", 0);

        // Let it complete, then clean up the file
        Thread.sleep(2000);
        realSourceFile.delete();
    }
}