import java.net.*;
import java.util.UUID;
import java.io.*;

public class Server {
    public static void main(String[] args) {
        System.out.println("Initiated server!");
        boolean exit = false;

        Store store = new Store();

        try {
            ServerSocket serverSocket = new ServerSocket(8080);
            while (!exit) {

                Socket clientSocket = serverSocket.accept();

                Thread t = new Thread(new ResponseThread(clientSocket, store));
                t.start();
            }

            serverSocket.close();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static Response stateToResponse(State state){
        
        String word = state.getWord();
        int remaining = state.getRemainingAttemps();
        int score = state.getScore();
        boolean[] positions = state.getGuessed();

        int guessed = 0;

        for (int i = 0; i < positions.length; i++) {
            if(positions[i]) guessed++; 
        }

        int idx = 0;
        char[] guessedLetters = new char[guessed];
        int[] guessedPositions = new int[guessed];

        for (int i = 0; i < positions.length; i++) {
            if(positions[i]){
                guessedLetters[idx] = word.charAt(i);
                guessedPositions[idx] = i;
                idx++;
            }
        }

        
        return new Response(word.length(), guessedLetters, guessedPositions, score, remaining);
    }

    public static Response errorResponse(){
        return new Response(-1, null, null, 0, 0);
    }
}

class ResponseThread implements Runnable {

    Socket clientSocket;
    Store store;

    public ResponseThread(Socket clientSocket, Store store){
        this.clientSocket = clientSocket;
        this.store = store;
    }

    @Override
    public void run() {

        try {
            DataInputStream in = new DataInputStream(clientSocket.getInputStream());
            ObjectOutputStream out;
        
            out = new ObjectOutputStream(clientSocket.getOutputStream());

            int reqLength = in.readInt();
            byte[] bytes = new byte[reqLength];
            int read = 0;
            while(read < reqLength){
                read = in.read(bytes, read, reqLength - read);
                System.out.println(read);
            }

            Request reqObj = (Request) Utils.deserialize(bytes);
            String request = reqObj.requestString;
            UUID uuid = reqObj.id;

            State state = null;

            if("start game".equals(request.toLowerCase())){
                System.out.println("Start game!");
                state = new State();
                state.startGame();
                store.setState(uuid, state);

            } else {
                state = store.getState(uuid);
                
                if(state == null || !state.isPlaying()){ 
                    out.writeObject(Server.errorResponse());
                    return;
                }

                String[] guessRequest = request.split(" ");
                if(guessRequest.length != 2 || !"guess".equals(guessRequest[0].toLowerCase())){
                    System.out.println("unrecognized request");
                    out.writeObject(Server.errorResponse());
                    return;
                } else if(guessRequest[1].length() == 1){
                    System.out.println("single char guess request");
                    state.guess(guessRequest[1].charAt(0));
                } else {
                    System.out.println("whole word guess request");
                    state.guess(guessRequest[1]);
                }

            }
            
            out.writeObject(Server.stateToResponse(state));

            clientSocket.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}

