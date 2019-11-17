import java.io.*;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;
import java.util.UUID;

public class Utils {
    //data stream to object stream
    public static Object deserialize(byte[] data) throws IOException, ClassNotFoundException {
        ByteArrayInputStream in = new ByteArrayInputStream(data);
        ObjectInputStream is = new ObjectInputStream(in);
        return is.readObject();
    }

    // object stream to data stream
    public static byte[] serialize(Object obj) throws IOException {
        
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ObjectOutputStream os = new ObjectOutputStream(out);
        os.writeObject(obj);
        return out.toByteArray();
    }

    static String readInput() throws IOException {
        BufferedReader consoleReader = new BufferedReader(new InputStreamReader(System.in));
        return consoleReader.readLine().toLowerCase();
    }



    static void sendRequest(String request, UUID uuid, String jwt, SocketChannel clientSocket) throws IOException {

        Request reqObj = new Request(uuid, jwt, request);
        byte[] serialized = Utils.serialize(reqObj);
        ByteBuffer out= ByteBuffer.wrap(serialized);
        
        clientSocket.write(out);
    }

    static void sendResponse(Response response, SocketChannel clientSocket) throws IOException {
        byte[] serialized = Utils.serialize(response);
        clientSocket.write(ByteBuffer.wrap(serialized));
    }
 
    static Response receiveResponse(SocketChannel clientSocket) throws IOException, ClassNotFoundException {
        ByteBuffer input= ByteBuffer.allocate(1024);

        while(input.hasRemaining()){
            if(clientSocket.read(input) <= 0) break;
        }
        input.flip();
        
        if(input.limit() == 0) return null;
        return (Response) Utils.deserialize(input.array());
    }

    static Request receiveRequest(SocketChannel clientSocket, SelectionKey key) throws IOException, ClassNotFoundException {


        ByteBuffer readBuffer = ByteBuffer.allocate(1024);
        readBuffer.clear();

        int read = -1;
        try {
            read = clientSocket.read(readBuffer);
        } catch (Exception e) {
            key.cancel();
            clientSocket.close();
            return null;
        }

        if (read == -1) {
            System.out.println("Nothing was there to be read, closing connection");
            clientSocket.close();
            key.cancel();
            return null;
        }

        readBuffer.flip();
        byte[] data = new byte[1000];
        readBuffer.get(data, 0, read);

        // System.out.println("Received: " + new String(data));

        return (Request) Utils.deserialize(data);
    }
}
