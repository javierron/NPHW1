import java.io.IOException;
import java.net.*;
import java.nio.ByteBuffer;
import java.nio.channels.Channel;
import java.nio.channels.Channels;
import java.nio.channels.SocketChannel;
import java.nio.channels.WritableByteChannel;
import java.util.UUID;

/* Gaming logic:
After both starting the client and server, users should input "login username password"
then "start game"
then make guessing following "guess xxx"
to exit the game, input "exit"
 */

public class Client {

    static class Credentials {
        protected final UUID uuid = UUID.randomUUID();
        protected String jwt = null;
    }

    public static void main(String[] args) {
        InetSocketAddress hostAddress = new InetSocketAddress("localhost", 8080);
        SocketChannel client;
        try {
            client = SocketChannel.open(hostAddress);
            client.configureBlocking(false);
            String threadName = Thread.currentThread().getName();

            // Send messages to server
            String messages = "LOGIN";

            System.out.println(threadName + " started");

            ByteBuffer buffer = ByteBuffer.allocate(74);
            buffer.put(messages.getBytes());
            buffer.flip();
            client.write(buffer);
            System.out.println(messages);
            
            buffer = ByteBuffer.allocate(1024);

            WritableByteChannel out = Channels.newChannel(System.out);

            while (buffer.hasRemaining() && client.read(buffer) != -1){
                buffer.flip();
                out.write(buffer);
            }
        } catch (Exception e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    public static void main2(String[] args) {
        System.out.println("Hangman game!");

        Credentials cred = new Credentials();

        boolean exit = false;

        while (!exit) {
            try {
                
                Socket clientSocket = new Socket("localhost", 8080);
                String request = Utils.readInput();

                //exit operation
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
        

            System.out.println(resp.responseCode);

            clientSocket.close();

            if (resp.responseCode == Response.ResponseCode.ERROR_RESPONSE){
                System.out.println("There was an error processing your request, please try again");
                return;
            }
            else if(resp.responseCode == Response.ResponseCode.INVALID_JWT_RESPONSE){
                System.out.println("There was an error, invalid jwt response, please try again");
                return;
            }
            else if(resp.responseCode == Response.ResponseCode.NOTLOGGED_RESPONSE){
                System.out.println("There was an error that you didn't logged in, please try again");
                return;
            }

            else if(resp.responseCode == Response.ResponseCode.ALREADY_LOGGED_RESPONSE){
                System.out.println("There was an error that you have already logged in, please try again");
                return;
            }

            else if (resp.responseCode == Response.ResponseCode.WIN_RESPONSE){
                System.out.println("Correct! The word was \"" + String.valueOf(resp.guessedLetters) +"\", your score is now: " + resp.score);
                return;
            }


            else if (resp.responseCode == Response.ResponseCode.LOSE_RESPONSE){
                System.out.println("Incorect! The word was \"" + String.valueOf(resp.guessedLetters) +"\", your score is now: " + resp.score);
                return;
            }

            else if(resp.responseCode == Response.ResponseCode.INVALID_PASSOWORD){
                System.out.println("There was an error, you need to use the password \"password\" ");
                return;
            }


            /* with the login response, login succeeds, jwt is stored

             */
            if (resp.responseCode == Response.ResponseCode.LOGIN_RESPONSE){
                System.out.println("Logged in! type 'start game' to start playing! \"");
                this.cred.jwt = String.valueOf(resp.guessedLetters);
                return;
            }
            
            // if no guessing yet, word is replaced by "-----"
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
