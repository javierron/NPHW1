import java.net.*;
import java.util.UUID;

public class Client {

    static class Credentials {
        protected final UUID uuid = UUID.randomUUID();
        protected String jwt = null;
    }
    

    public static void main(String[] args) {
        System.out.println("Hangman game!");

        Credentials cred = new Credentials();

        boolean exit = false;

        while (!exit) {
            try {
                
                Socket clientSocket = new Socket("localhost", 8080);
                String request = Utils.readInput();

                if ("exit".equals(request)) {
                    break;
                }
                
                Thread t = new Thread(new RequestThread(cred, clientSocket, request));
                t.start();
            }catch (ConnectException e) {
                System.out.println("unable to connect to server");
                break;
            } 
            catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
}


class RequestThread implements Runnable {

    Client.Credentials cred;
    Socket clientSocket;
    String request;

    public RequestThread(Client.Credentials cred, Socket clientSocket, String request) {
        this.cred = cred;
        this.clientSocket = clientSocket;
        this.request = request;
    }

    @Override
    public void run() {

        try {
            Utils.sendRequest(request, cred.uuid, cred.jwt, clientSocket);
            Response resp = Utils.receiveResponse(clientSocket);
        
            clientSocket.close();

            System.out.println(resp.wordLength);

            if (resp.wordLength == -1 || resp.wordLength == -5 || resp.wordLength == -6 || resp.wordLength == -7){
                System.out.println("There was an error processing your request, please try again");
                return;
            } 

            if (resp.wordLength == -2){
                System.out.println("Correct! The word was \"" + String.valueOf(resp.guessedLetters) +"\", your score is now: " + resp.score);
                return;
            } 

            if (resp.wordLength == -3){
                System.out.println("Incorect! The word was \"" + String.valueOf(resp.guessedLetters) +"\", your score is now: " + resp.score);
                return;
            }

            if (resp.wordLength == -4){
                System.out.println("Logged in! type 'start game' to start playing! \"");
                this.cred.jwt = String.valueOf(resp.guessedLetters);
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
