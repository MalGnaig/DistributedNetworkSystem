import java.net.Socket;
import java.util.*;

// 1 Index is 1 file
public class Index {
    
    private String filename;
    public long filesize;
    public int ACKstorage = 0;
    // use countdown latch unless use synchronise
    public Status status;
    // Integer is the Dstore's that store this file, Socket is the Dstore socket
    public static Map<String, ControllerConnection> dStoreLog = Collections.synchronizedMap(new HashMap<String, ControllerConnection>());

    public int lastIndexLoaded = 0;

    public ArrayList<ControllerConnection> dStores = new ArrayList<>();
    
    public enum Status {
        STORE_IN_PROGRESS,
        STORE_COMPLETE,
        REMOVE_IN_PROGRESS,
        REMOVE_COMPLETE
    }
    public Index(String filename, long filesize) {
        this.status = Status.STORE_IN_PROGRESS;
        this.filesize = filesize;
        this.ACKstorage = 0;
        this.filename = filename;
    }

    public void setFilesize(Long filesize) { this.filesize = filesize;}

    public Long getFilesize() {
        return filesize;
    }

    public String getFilename(){return filename;}

    public void setFilename(String filename){
        this.filename = filename;
    }
    
    public void setStatus(Status status) {
        this.status = status;
    }




    public int getACKstorage() {return ACKstorage;}

    public void setACKstorage(int ACKstorage) {this.ACKstorage = ACKstorage;}

}