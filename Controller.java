import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.*;
import java.util.logging.Logger;

public class Controller {
    protected static int cport;
    protected static int copies;
    protected static int timeout;
    protected static int rebalancePeriod;
    protected static ServerSocket serverSocket;
    // integer is the port number of the actual dstore that points to it's dstore model
    public static ArrayList<ControllerConnection> connectedDstores = new ArrayList<>();
    //there should be no replicates files in the same set.
    protected static final Set<Index> allFiles = new HashSet<>();
    // string is the filename maps to index which is the file with all the information in the index class
    public static final Map<String, Index> filenameIndex = new HashMap<>();
//    public static SimpleBooleanProperty currentlyRebalancing;

    public Controller(int cport, int copies, int timeout, int rebalancePeriod) {
        this.cport = cport;
        this.copies = copies;
        this.timeout = timeout;
        this.rebalancePeriod = rebalancePeriod;
//        connectedDstores = Collections.synchronizedMap(new HashMap<>());
    }

    public static void main(String[] args) throws IOException {
        try {
            int cport = Integer.parseInt(args[0]);
            int copies = Integer.parseInt(args[1]);
            int timeout = Integer.parseInt(args[2]);
            int rebalancePeriod = Integer.parseInt(args[3]);
            // give the server socket the controller port so now can use serverSocket.accept() in line 48
            serverSocket = new ServerSocket(cport);

            Controller controller = new Controller(cport, copies, timeout, rebalancePeriod);
            controller.clientConnection();

        } catch (NumberFormatException e) {
            System.out.println("There is an error parsing the arguments: " + e.getMessage());
            System.out.println(" ");
            return;
        }
    }

    protected void clientConnection() throws IOException {
        while (true) {
            try {
                Socket client = serverSocket.accept();
                new Thread(() -> {
                    ControllerConnection dStoreConcept = new ControllerConnection(timeout, client, client.getPort());
                    System.out.println("Connection has been made from port : " + client.getPort());
                }).start();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }



}

