import java.io.*;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.HashMap;
import java.util.logging.Logger;

public class Dstore {
    // This is the port that the Dstore will listen on for incoming messages, the Dstores port
    protected static int dStorePort;
    // This is the controllers port
    protected static int controllerPort;
    // This is the time out in milliseoncds
    protected static int timeoutPeriod;
    // This is where to store the files
    protected File folder;
    //This is the amount of files that have been stored in the Dstore
    protected int filesStored;
    protected static Socket controllerSocket;

    protected static ServerSocket DstoreSocket;
    protected HashMap<Integer, File> dstores = new HashMap<>();
    protected static Logger DStoreLogger;
    public PrintWriter out;
    public BufferedReader in;
    public DstoreConnection dstore;
    public DstoreConnection dstore2;


    public void setDSocket(ServerSocket DstoreSocket) {
        this.DstoreSocket = DstoreSocket;
    }
    public Dstore(int dStorePort, int controllerPort, int timeoutPeriod, String folderName) throws IOException {
        this.dStorePort = dStorePort;
        this.controllerPort = controllerPort;
        this.timeoutPeriod = timeoutPeriod;
        this.folder = new File(folderName);

// maybe deleted this
        if (!folder.isDirectory() || !folder.exists()) {
            if (!folder.mkdir()) throw new IOException("Folder: " + folderName + "cannot be created.");
            System.out.println("Creating Folder " + folderName);
        }
    }

    public static void main(String[] args) {
        try {
            int dStorePort = Integer.parseInt(args[0]);
            int controllerPort = Integer.parseInt(args[1]);
            int timeoutPeriod = Integer.parseInt(args[2]);
            String folder = args[3];

            Dstore dStore = new Dstore(dStorePort, controllerPort, timeoutPeriod, folder);

            dStore.setDSocket(new ServerSocket(dStorePort));

            dStore.startServer();
            dStore.clientConnection();

        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
    private void startServer() {
            // this will join the Dstore to the controller and it will sort out messages between controller and Dstore
            String joinToken = Protocol.JOIN_TOKEN;
            //the controllerPort is the socket we are listening in for and storing it's value
            new Thread(() ->{
                try {
                    Socket controllerSocket = new Socket(InetAddress.getLoopbackAddress(), controllerPort);
                    dstore2 = new DstoreConnection(timeoutPeriod, controllerSocket, this);
                    dstore2.sendMessage(joinToken + " " + dStorePort);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }).start();
            // when a new thread (new Dstore) we begin by writing a message to the controller (via it's socket which is the destination)
//                writerToController.flush();
        }


    // This creates the client socket for the DStore to listen on in
    private void clientConnection() {
        while(true){
            try {
                Socket client = DstoreSocket.accept();
                // starting thread immediately after accepting the client
                new Thread(() ->{
                    System.out.println("Client has been connected from port : " + client.getPort());
                    dstore = new DstoreConnection(timeoutPeriod, client, this);
                }).start();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }




    protected void sendMessage(String messageToSend) {
        dstore2.sendMessage(messageToSend);
        //DStoreLogger.info("Message: " + messageToSend + " has been sent to " + DestinationSocket.getPort());
    }


}







