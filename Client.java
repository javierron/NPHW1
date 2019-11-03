import java.net.*;
import java.util.UUID;
import java.io.*;

/**
 * Client
 */
public class Client {

    public static void main(String[] args) {
        System.out.println("Initiated client!");

        UUID uuid = UUID.randomUUID();
        boolean exit = false;
        try {

            System.out.println("Hangman game!");
            
            while (!exit) {
                Socket clientSocket = new Socket("localhost", 8080);
                System.out.println("Write your action: ");

                String request = readInput();

                Thread t = new Thread(new RequestThread(uuid, clientSocket, request));
                t.start();

                if ("exit".equals(request.toLowerCase())) {
                    exit = true;
                    break;
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    static String readInput() throws IOException {
        BufferedReader consoleReader = new BufferedReader(new InputStreamReader(System.in));
        return consoleReader.readLine();
    }

    static void sendMessage(String request, UUID uuid, Socket clientSocket) throws IOException {

        Request reqObj = new Request(uuid, "jwt", request);
        byte[] serialized = Utils.serialize(reqObj);
        
        DataOutputStream out = new DataOutputStream(clientSocket.getOutputStream());
        
        out.writeInt(serialized.length);
        out.write(serialized);
        out.flush();
    }
 
    static Response receiveMessage(Socket clientSocket) throws IOException, ClassNotFoundException{
        ObjectInputStream in = new ObjectInputStream(clientSocket.getInputStream());
        return (Response)in.readObject();
    }
}


class RequestThread implements Runnable {

    UUID uuid;
    Socket clientSocket;
    String request;

    public RequestThread(UUID uuid, Socket clientSocket, String request) {
        this.uuid = uuid;
        this.clientSocket = clientSocket;
        this.request = request;
    }

    @Override
    public void run() {

        try {
            Client.sendMessage(request, uuid, clientSocket);
            Response resp = Client.receiveMessage(clientSocket);
        
            clientSocket.close();

            if (resp.wordLength < 0){
                System.out.println("There was an error processing your request, please try again");
                return;
            } 
            
            char[] word = new char[resp.wordLength];
            for (int i = 0; i < word.length; i++) {
                word[i] = '-';
            }

            for (int i = 0; i < resp.guessedPositions.length; i++) {
                word[resp.guessedPositions[i]] = resp.guessedLetters[i];
            }

            System.out.println("WORD:" + String.valueOf(word) + " | " + "SCORE:" + resp.score + " | " + "ATTEMPTS:" + resp.remainingAtempts);
        
        } catch (Exception e) {
            e.printStackTrace();
        }

    }
    
}
