import java.io.*;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;
import java.util.UUID;

public class Utils {
    //data stream to object stream
    public static Object deserialize(byte[] data) throws IOException, ClassNotFoundException {
        System.out.println(data.length);
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
        
        //DataOutputStream out = new DataOutputStream(clientSocket.getOutputStream());
        
        //out.writeInt(serialized.length);
        clientSocket.write(out);
        //out.flush();
    }

    static void sendResponse(Response response, SocketChannel clientSocket) throws IOException {



        byte[] serialized = Utils.serialize(response);
        
        //DataOutputStream out = new DataOutputStream(clientSocket.getOutputStream());
        
        //out.writeInt(serialized.length);

        System.out.println("ASD");
        System.out.println(response.responseCode);
        System.out.println(serialized.length);

        clientSocket.write(ByteBuffer.wrap(serialized));
        //out.flush();
    }
 
    static Response receiveResponse(SocketChannel clientSocket) throws IOException, ClassNotFoundException {

        //DataInputStream in = new DataInputStream(clientSocket.getInputStream());

        //byte[] serialized = Utils.serialize(reqObj);
        ByteBuffer input= ByteBuffer.allocate(1024);

        while(input.hasRemaining() && clientSocket.read(input) != -1){
            input.flip();
            //clientSocket.write(input);

        }

        System.out.println(input.limit());
        System.out.println(input.position());
        return (Response) Utils.deserialize(input.array());
    }

    static Request receiveRequest(SocketChannel clientSocket, SelectionKey key) throws IOException, ClassNotFoundException {


        ByteBuffer readBuffer = ByteBuffer.allocate(1024);
        readBuffer.clear();

        int read = -1;
        try {
            read = clientSocket.read(readBuffer);
        } catch (Exception e) {
            //            System.out.println("Reading problem, closing connection");
            // e.printStackTrace();
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



       /* DataInputStream in = new DataInputStream(clientSocket.getInputStream());

        int reqLength = in.readInt();
        byte[] bytes = new byte[reqLength];
        int read = 0;
        while(read < reqLength){
            read += in.read(bytes, read, reqLength - read);
        }

        */
        
        return (Request) Utils.deserialize(data);
    }
}
