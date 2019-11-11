import java.net.*;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.Date;
import java.util.Iterator;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import com.auth0.jwt.*;
import com.auth0.jwt.algorithms.*;
import com.auth0.jwt.exceptions.JWTVerificationException;

public class Server {

    public static void main(String[] args) {
        ServerSocketChannel serverChannel;
        Selector selector;
   
        System.out.println("initializing server");
        
        try {
            // This is how you open a ServerSocketChannel
            serverChannel = ServerSocketChannel.open();
            // You MUST configure as non-blocking or else you cannot register the serverChannel to the Selector.
            serverChannel.configureBlocking(false);
            // bind to the address that you will use to Serve.
            serverChannel.socket().bind(new InetSocketAddress("localhost", 8080));

            // This is how you open a Selector
            selector = Selector.open();
            /*
             * Here you are registering the serverSocketChannel to accept connection, thus the OP_ACCEPT.
             * This means that you just told your selector that this channel will be used to accept connections.
             * We can change this operation later to read/write, more on this later.
             */
            serverChannel.register(selector, SelectionKey.OP_ACCEPT);
        
    
            System.out.println("Now accepting connections...");
            // A run the server as long as the thread is not interrupted.
            while (!Thread.currentThread().isInterrupted()) {
                /*
                 * selector.select(TIMEOUT) is waiting for an OPERATION to be ready and is a blocking call.
                 * For example, if a client connects right this second, then it will break from the select()
                 * call and run the code below it. The TIMEOUT is not needed, but its just so it doesn't
                 * block undefinable.
                 */
                selector.select(50000);

                /*
                 * If we are here, it is because an operation happened (or the TIMEOUT expired).
                 * We need to get the SelectionKeys from the selector to see what operations are available.
                 * We use an iterator for this.
                 */
                Iterator<SelectionKey> keys = selector.selectedKeys().iterator();

                while (keys.hasNext()) {
                    SelectionKey key = keys.next();
                    // remove the key so that we don't process this OPERATION again.
                    keys.remove();

                    // key could be invalid if for example, the client closed the connection.
                    if (!key.isValid()) {
                        continue;
                    }
                    /*
                     * In the server, we start by listening to the OP_ACCEPT when we register with the Selector.
                     * If the key from the keyset is Acceptable, then we must get ready to accept the client
                     * connection and do something with it. Go read the comments in the accept method.
                     */
                    if (key.isAcceptable()) {
                        System.out.println("Accepting connection");
                        //accept(key);
                        ServerSocketChannel server = (ServerSocketChannel) key.channel();
                        SocketChannel channel = server.accept();

                        channel.configureBlocking(false);
                        channel.register(selector, SelectionKey.OP_READ, ByteBuffer.allocate(1024));
                    }
                    /*
                     * If you already read the comments in the accept() method, then you know we changed
                     * the OPERATION to OP_WRITE. This means that one of these keys in the iterator will return
                     * a channel that is writable (key.isWritable()). The write() method will explain further.
                     */
                    if (key.isWritable()) {
                        SocketChannel channel = (SocketChannel) key.channel();
                        /*
                        * The HashMap contains the object SocketChannel along with the information in it to be written.
                        * In this example, we send the "Hello from server" String and also an echo back to the client.
                        * This is what the HashMap is for, to keep track of the messages to be written and their socketChannels.
                        */
                        byte[] data = "the response".getBytes();

                        // Something to notice here is that reads and writes in NIO go directly to the channel and in form of
                        // a buffer.

                        channel.write(ByteBuffer.wrap(data));
                        System.out.println("Wrote response");
                        // Since we wrote, then we should register to read next, since that is the most logical thing
                        // to happen next. YOU DO NOT HAVE TO DO THIS. But I am doing it for the purpose of the example
                        // Usually if you register once for a read/write/connect/accept, you never have to register
                        // again for that unless you register for none (0).
                        // Like it said, I am doing it here for the purpose of the example. The same goes for all others.
                        key.interestOps(SelectionKey.OP_READ);
                        System.out.println("exit write");
                    }
                    /*
                     * If you already read the comments in the write method then you understand that we registered
                     * the OPERATION OP_READ. That means that on the next Selector.select(), there is probably a key
                     * that is ready to read (key.isReadable()). The read() method will explain further.
                     */
                    if (key.isReadable()) {
                        SocketChannel channel = (SocketChannel) key.channel();
                        ByteBuffer readBuffer = ByteBuffer.allocate(1024);
                        readBuffer.clear();

                        int read = -1;
                        try {
                            read = channel.read(readBuffer);
                        } catch (Exception e) {
                //            System.out.println("Reading problem, closing connection");
                           // e.printStackTrace();
                            key.cancel();
                            channel.close();
                            continue;
                        }

                        if (read == -1) {
                            System.out.println("Nothing was there to be read, closing connection");
                            channel.close();
                            key.cancel();
                            continue;
                        }
                        // IMPORTANT - don't forget the flip() the buffer. It is like a reset without clearing it.
                        readBuffer.flip();
                        byte[] data = new byte[1000];
                        readBuffer.get(data, 0, read);
                        System.out.println("Received: " + new String(data));
                        key.interestOps(SelectionKey.OP_WRITE);

                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    public static void main2(String[] args) {



        ExecutorService pool = Executors.newFixedThreadPool(8);   //Set number of threads for reuse in threadpool

        try {

            JWTCreator.Builder jwtBuilder = JWT.create();   //JWT
            
            System.out.println("Initiated server!");
            
            boolean exit = false;
            Store store = new Store();
            ServerSocket serverSocket = new ServerSocket(8080);
            
            while (!exit) {
            
                try {
                    Socket clientSocket = serverSocket.accept();
                    
                    pool.execute(new ResponseThread(clientSocket, store, jwtBuilder));

                }catch(Exception e ){

                    System.out.println("Client disconnected");
                }

            }

            //close the server socket and threadpool when exit
            serverSocket.close();
            pool.shutdown();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static Response stateToResponse(State state){
        
        String word = state.getWord();
        int remaining = state.getRemainingAttemps();
        int score = state.getScore();
        boolean[] positions = state.getGuessed();
        Response.ResponseCode responseCode = Response.ResponseCode.GUESS_RESPONSE;
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

        
        return new Response(responseCode,word.length(), guessedLetters, guessedPositions, score, remaining);
    }

    public static Response errorResponse(){
        return new Response(Response.ResponseCode.ERROR_RESPONSE,0 , null, null, 0, 0);
    }
    
    public static Response winResponse(State state){
        return new Response(Response.ResponseCode.WIN_RESPONSE, 0,state.word.toCharArray(), null, state.score, 0);
    }
    
    public static Response loseResponse(State state){
        return new Response(Response.ResponseCode.LOSE_RESPONSE,0, state.word.toCharArray(), null, state.score, 0);
    }

    public static Response loginResponse(String jwt){
        return new Response(Response.ResponseCode.LOGIN_RESPONSE, 0,jwt.toCharArray(), null, 0, 0);
    }

    public static Response invalidJwtResponse(){
        return new Response(Response.ResponseCode.INVALID_JWT_RESPONSE,0,null, null, 0, 0);
    }

    public static Response notLoggedInResponse(){
        return new Response(Response.ResponseCode.NOTLOGGED_RESPONSE,0,null, null, 0, 0);
    }

    public static Response alreadyLoggedInResponse(){
        return new Response(Response.ResponseCode.ALREADY_LOGGED_RESPONSE,0, null,null, 0, 0);
    }

    public static Response invalidPassword(){
        return new Response(Response.ResponseCode.INVALID_PASSOWORD,0, null,null, 0, 0);
    }

}

class ResponseThread implements Runnable {

    Socket clientSocket;
    Store store;
    JWTCreator.Builder jwtBuilder;
    JWTVerifier verifier;

    public ResponseThread(Socket clientSocket, Store store, JWTCreator.Builder jwtBuilder){
        this.clientSocket = clientSocket;
        this.store = store;
        this.jwtBuilder = jwtBuilder;
    }

    @Override
    public void run() {

        try {
            
            Request reqObj = Utils.receiveRequest(clientSocket);

            String request = reqObj.requestString;
            String jwt = reqObj.jwt;
            UUID uuid = reqObj.id;

            State state = null;

            String correct_password = "password";

            if(request.toLowerCase().startsWith("login ")){           // "login username password"
                
                String[] loginRequest = request.split(" ");
                
                //error response for wrong input
                if(loginRequest.length != 3) {
                    Utils.sendResponse(Server.errorResponse(), clientSocket);
                    return;
                }
            
                //check for already loggedin state
                state = store.getState(uuid);
                if(state != null){
                    Utils.sendResponse(Server.alreadyLoggedInResponse(), clientSocket);
                    return;
                }
                
                String username = loginRequest[1];
                String password = loginRequest[2];

                //check for correct password
                if(!password.equals(correct_password)){
                    Utils.sendResponse(Server.invalidPassword(), clientSocket);
                    return;
                }


                //we need to check the correct password
                String newjwt = jwtBuilder.withClaim("user", username).withIssuedAt(new Date(System.currentTimeMillis())).sign(Algorithm.HMAC256("secret")); 

                //After successfully logged in, the state is created
                state = new State();
                store.setState(uuid, state);

                Utils.sendResponse(Server.loginResponse(newjwt), clientSocket);
                return;

            }else if("start game".equals(request.toLowerCase())){   //state start game

                state = store.getState(uuid);

                //Make sure the state is created before start game.
                if(state == null || jwt == null){
                    Utils.sendResponse(Server.notLoggedInResponse(), clientSocket);
                    return;
                }

                //jwt check before start game
                try {
                    Algorithm algorithm = Algorithm.HMAC256("secret");
                    JWTVerifier verifier = JWT.require(algorithm).build();
                    verifier.verify(jwt);
                } catch (JWTVerificationException exception){
                    Utils.sendResponse(Server.invalidJwtResponse(), clientSocket);
                    return;
                }
                
                System.out.println("Start game!");
                state.startGame(store.getWord());
                

            } else {        //playing state

                state = store.getState(uuid);

                //To make sure the state is created before playing, and jwt exists.
                if(jwt == null || state == null || !state.isPlaying()){ 
                    Utils.sendResponse(Server.errorResponse(), clientSocket);
                    return;
                }

                //check for jwt
                try {
                    Algorithm algorithm = Algorithm.HMAC256("secret");
                    JWTVerifier verifier = JWT.require(algorithm).build();
                    verifier.verify(jwt);
                } catch (JWTVerificationException exception){
                    Utils.sendResponse(Server.invalidJwtResponse(), clientSocket);
                    return;
                }

                String[] guessRequest = request.split(" ");
                
                //check guess operation should always be "guess xxx"
                if(guessRequest.length != 2 || !"guess".equals(guessRequest[0].toLowerCase())){
                    System.out.println("unrecognized request");
                    Utils.sendResponse(Server.errorResponse(), clientSocket);
                    return;
                } else if(guessRequest[1].length() == 1){
                    System.out.println("single char guess request");
                    state.guess(guessRequest[1].charAt(0));
                } else {
                    System.out.println("whole word guess request");
                    state.guess(guessRequest[1]);
                }
            }


            Response resp;
            if(state.isPlaying()){
                resp = Server.stateToResponse(state);
            }else if(state.isWin()){
                resp = Server.winResponse(state);
            }else if(state.isLose()){
                resp = Server.loseResponse(state);
            }else{
                resp = Server.errorResponse();
            }

            Utils.sendResponse(resp, clientSocket);

            clientSocket.close();
        } catch (Exception e) {
            System.out.println("client disconected");
            //e.printStackTrace(System.out);
        }
    }
}

