import java.io.*;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Objects;

// this model is used in the controller.
public class ControllerConnection {
    public int getFilesContained() {
        return filesContained;
    }

    public void setFilesContained(int filesContained) {
        this.filesContained = filesContained;
    }

    public Socket getdStoreSocket() {
        return dStoreSocket;
    }

    public void setdStoreSocket(Socket dStoreSocket) {
        this.dStoreSocket = dStoreSocket;
    }

    public int getdStorePort() {
        return dStorePort;
    }

    public void setdStorePort(int dStorePort) {
        this.dStorePort = dStorePort;
    }

    private int filesContained;
    private int controllerPort;
    private File folder;
    private int timeOut;
    private Socket dStoreSocket;
    private int dStorePort;
    private int dstoreServerPort;
    private PrintWriter printWriter;
    private BufferedReader bufferedReader;
    private ArrayList<String> incomingMessages = new ArrayList<>();

    public boolean isDstore = false;

    public ControllerConnection(int timeOut, Socket dStoreSocket, int dStorePort) {

        this.timeOut = timeOut;
        this.dStoreSocket = dStoreSocket;
        this.dStorePort = dStorePort;

        try{
            printWriter = new PrintWriter(dStoreSocket.getOutputStream(), true);
            bufferedReader = new BufferedReader(new InputStreamReader(dStoreSocket.getInputStream()));
            new Thread(new Runnable() {
                @Override
                public void run() {
                    try{
                        while (true) {
                            String message = bufferedReader.readLine();

                            if (!Objects.isNull(message) && !message.equals("")) {
//                            incomingMessages.add(message);
                                System.out.println(message);
                                handleMessage(message.split(" "));

                            }
                        }
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                }
            }).start();

        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public void handleMessage(String[] messages){
        if((messages[0]).equals(Protocol.JOIN_TOKEN)){
            //Controller.rebalance();
            System.out.println("handling Dstore message");
            isDstore = true;
            dstoreServerPort = Integer.parseInt(messages[1]);
            Controller.connectedDstores.add(this);
        } else if((messages[0]).equals(Protocol.STORE_TOKEN)){
            store(messages[1], messages[2]);
        } else if((messages[0]).equals(Protocol.STORE_ACK_TOKEN)) {
            Index index = Controller.filenameIndex.get(messages[1]);
            index.ACKstorage = index.ACKstorage + 1;

            if(index.ACKstorage == Controller.copies){
                Index.dStoreLog.get(messages[1]).sendMessage(Protocol.STORE_COMPLETE_TOKEN);
            }
        } else if((messages[0]).equals(Protocol.LIST_TOKEN)){
            System.out.println("Listing");
            String s = Protocol.LIST_TOKEN;
            for (String filename: Index.dStoreLog.keySet()) {
                Index index = Controller.filenameIndex.get(filename);
                if (!(index.status.equals(Index.Status.REMOVE_IN_PROGRESS) || index.status.equals(Index.Status.REMOVE_COMPLETE))) {
                    s = s + " " + filename;
                }
            }
            sendMessage(s);
        }  else if((messages[0]).equals(Protocol.LOAD_TOKEN)){
            if(Controller.filenameIndex.containsKey(messages[1])){
                Index index = Controller.filenameIndex.get(messages[1]);
                //sending the load response with the dstore to load from and the filesize.
                sendMessage(Protocol.LOAD_FROM_TOKEN + " " + index.dStores.get(index.lastIndexLoaded).dstoreServerPort + " " + index.getFilesize());
            } else {
                sendMessage(Protocol.ERROR_FILE_DOES_NOT_EXIST_TOKEN);
            }
        } else if((messages[0]).equals(Protocol.RELOAD_TOKEN)){
            Index index = Controller.filenameIndex.get(messages[1]);
            //incrementing to get the next dstore to load from
            index.lastIndexLoaded += 1;
            try {
                sendMessage(Protocol.LOAD_FROM_TOKEN + " " + index.dStores.get(index.lastIndexLoaded).dstoreServerPort + " " + index.getFilesize());
            } catch (Exception e) {
                sendMessage(Protocol.ERROR_LOAD_TOKEN);
            }
        }  else if((messages[0]).equals(Protocol.REMOVE_TOKEN)){
            if(Controller.filenameIndex.containsKey(messages[1]) && !Controller.filenameIndex.get(messages[1]).status.equals(Index.Status.REMOVE_IN_PROGRESS)){
                //replacing the creator client mapping to the remove client mapping
                Index.dStoreLog.replace(messages[1], this);
                Index index = Controller.filenameIndex.get(messages[1]);
                index.status = Index.Status.REMOVE_IN_PROGRESS;
                for (ControllerConnection controllerConnection : index.dStores) {
                    controllerConnection.sendMessage(Protocol.REMOVE_TOKEN + " " + messages[1]);
                }
            } else {
                sendMessage(Protocol.ERROR_FILE_DOES_NOT_EXIST_TOKEN);
            }
        } else if((messages[0]).equals(Protocol.REMOVE_ACK_TOKEN)) {
            Index index = Controller.filenameIndex.get(messages[1]);
            index.ACKstorage = index.ACKstorage - 1;
            if (index.ACKstorage == 0) {
                Index.dStoreLog.get(messages[1]).sendMessage(Protocol.REMOVE_COMPLETE_TOKEN);
            }

        } else {
            System.out.println("messages recieved are incorrect tokens/operations");
        }

    }
    public void sendMessage (String messageToSend){
        System.out.println("SENDING to " + dStorePort + ": " + messageToSend);
        printWriter.println(messageToSend);
        printWriter.flush();
//            controllerLogger.info("Message: " + messageToSend + " has been sent to " + DestinationSocket.getPort());

    }


    protected void store(String fileName, String fileSize){

        Long filesize = Long.parseLong(fileSize);
        try {
            if (filesize < 1) {
                System.out.println("The client is trying to store a filesize less than 1");
            }
        } catch (NumberFormatException e) {
            System.out.println("The client has not given a number for fileSize");
        }
        if(Controller.connectedDstores.size() < Controller.copies) {
            System.out.println(Protocol.ERROR_NOT_ENOUGH_DSTORES_TOKEN);
            sendMessage(Protocol.ERROR_NOT_ENOUGH_DSTORES_TOKEN);
            return;
        };
        Index newFile;
        // this is checking if the file already exists
        if (Controller.filenameIndex.containsKey(fileName)) {
            //aslong as the file is not already being currently removed or already removed then ...
            if (!(Controller.filenameIndex.get(fileName).status.equals(Index.Status.REMOVE_IN_PROGRESS)) || (Controller.filenameIndex.get(fileName).status.equals(Index.Status.REMOVE_COMPLETE)))
            {
                System.out.println("The filename already exists");
                sendMessage(Protocol.ERROR_FILE_ALREADY_EXISTS_TOKEN);
                return;
            } else{
                // if the file is not currently being used use the file object created before
                if (!(Controller.filenameIndex.get(fileName).status.equals(Index.Status.REMOVE_IN_PROGRESS))) {
                    newFile = Controller.filenameIndex.get(fileName);
                    newFile.filesize = Long.parseLong(fileSize);
                } else {
                    //otherwise if its currently being removed, send error message
                    sendMessage("ERROR_FILE_DOES_NOT_EXIST");
                    return;
                }
            }
        } else {
            //creating new file object and add it to controllers index
            newFile = new Index(fileName, Long.parseLong(fileSize));
            Controller.filenameIndex.put(fileName, newFile);

        }
        //creating a map of the file name to the client that created this file so we can respond.
        Index.dStoreLog.put(fileName,this);
        newFile.status = Index.Status.STORE_IN_PROGRESS;
        //dstores that we want to use to store
        ArrayList<ControllerConnection> dstoreList = new ArrayList<>();
        // all dstores available
        ControllerConnection[] dstoreList2 = Controller.connectedDstores.toArray(new ControllerConnection[0]);
        for (int i = 0; i < Controller.copies ; i++) {
            dstoreList.add(dstoreList2[i]);
        }
        // looking through list of all connected dstores and finding the smallest dstore to use.
        for (ControllerConnection dstore : Controller.connectedDstores) {
            for (ControllerConnection dstore2 : dstoreList) {
                if(!dstoreList.contains(dstore2)){
                    if(dstore.getFilesContained() < dstore2.getFilesContained()){
                        //swapping out the dstores to get the emptiest dstore
                        dstoreList.remove(dstore2);
                        dstoreList.add(dstore);
                    }
                }
            }
        }


        // We have the best dstores to store our files

        String s = Protocol.STORE_TO_TOKEN;
        //Sending all ports
        for (ControllerConnection dstore: dstoreList) {
            newFile.dStores.add(dstore);
            s = s + " " + dstore.dstoreServerPort;
        }
        sendMessage(s);
    }

}
