import java.io.*;
import java.net.InetAddress;
import java.net.Socket;
import java.util.ArrayList;

// this model is used in the dstore.
public class DstoreConnection {
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


    private int filesContained;
    private int controllerPort;
    private int timeOut;
    private Socket dStoreSocket;
    private PrintWriter printWriter;
    private BufferedReader bufferedReader;
    private ArrayList<String> incomingMessages = new ArrayList<>();

    public boolean isDstore = false;

    public Dstore dStore;

    public DstoreConnection(int timeOut, Socket dStoreSocket, Dstore dstore) {

        this.timeOut = timeOut;
        this.dStoreSocket = dStoreSocket;
        this.dStore = dstore;
        new Thread(() ->{
            try{
                printWriter = new PrintWriter(dStoreSocket.getOutputStream(), true);
                bufferedReader = new BufferedReader(new InputStreamReader(dStoreSocket.getInputStream()));
                while (true) {
                    if (bufferedReader != null){
                        if (bufferedReader.ready()) {
                            String s = bufferedReader.readLine();
                            if (s != null || s != "") {
                                System.out.println(s);
                                handleAllMessage(s.split(" "));
                            }
                        }
                    }

                }

            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }).start();

    }


        private void handleAllMessage(String[] messages) throws IOException {
            if((messages[0]).equals(Protocol.STORE_TOKEN)){
                store(messages);
            } else if((messages[0]).equals(Protocol.LOAD_DATA_TOKEN)){
                //sending the file
                OutputStream outputStream = dStoreSocket.getOutputStream();
                FileInputStream fileInputStream = new FileInputStream(dStore.folder.getAbsolutePath()+"/"+messages[1]);
                byte[] buffer = new byte[1024];
                int bytes;
                while ((bytes = fileInputStream.read(buffer)) != -1) {
                    outputStream.write(buffer);
                }
                fileInputStream.close();

            } else if((messages[0]).equals(Protocol.REMOVE_TOKEN)){

                File fileToRemove = new File(dStore.folder.getAbsolutePath() + "/"+messages[1]);
                if(fileToRemove.exists()){
                    fileToRemove.delete();
                }

                sendMessage(Protocol.REMOVE_ACK_TOKEN + " " + messages[1]);

            }  else {
                System.out.println("messages recieved are incorrect tokens/operations");
            }
        }

    public void sendMessage (String messageToSend){
        System.out.println("SENDING to " + dStoreSocket.getPort() + ": " + messageToSend);
        printWriter.println(messageToSend);
        printWriter.flush();
//            controllerLogger.info("Message: " + messageToSend + " has been sent to " + DestinationSocket.getPort());
    }


    private void store(String[] messages) throws IOException {
        sendMessage(Protocol.ACK_TOKEN);
        InputStream in = dStoreSocket.getInputStream();
        FileOutputStream fileOutputStream = new FileOutputStream(dStore.folder.getAbsolutePath()+"/"+messages[1]);
        byte[] buffer = new byte[1024];
        int bytes;
        int size = Integer.parseInt(messages[2]);
        while (size > 0 && (bytes = in.read(buffer, 0, (int)Math.min(buffer.length, size))) != -1) {
            fileOutputStream.write(buffer,0,bytes);
            size = size - 1024;
        }
        fileOutputStream.close();
        dStore.sendMessage(Protocol.STORE_ACK_TOKEN + " " + messages[1]);

    }



}

