import java.io.Serializable;
import java.util.UUID;

public class Request implements Serializable {

    private static final long serialVersionUID = 1L;

    public Request(UUID id, String jwt, String requestString) {
        this.id = id;
        this.jwt = jwt;
        this.requestString = requestString;
    }

    public final UUID id;
    public final String jwt;
    public final String requestString;
}
/* This is the object class for client to send message to server */
