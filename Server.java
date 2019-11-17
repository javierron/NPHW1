import java.io.IOException;
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

    private static Store store;
    private static Response resObj;
    private static JWTCreator.Builder jwtBuilder;

    public static void main(String[] args) {
        ServerSocketChannel serverChannel;
        Selector selector;

        try {
            //JWT
            jwtBuilder = JWT.create();

            System.out.println("Initiated server!");
            
            boolean exit = false;
            store = new Store();
            System.out.println("Created store!");


            serverChannel = ServerSocketChannel.open();
            serverChannel.configureBlocking(false);
            serverChannel.socket().bind(new InetSocketAddress("localhost", 8080));

            selector = Selector.open();
            serverChannel.register(selector, SelectionKey.OP_ACCEPT);

            while (!Thread.currentThread().isInterrupted()) {
                selector.select(50000);

                Iterator<SelectionKey> keys = selector.selectedKeys().iterator();

                while (keys.hasNext()) {
                    SelectionKey key = keys.next();
                    keys.remove();

                    if (!key.isValid()) {
                        continue;
                    }
                    if (key.isAcceptable()) {
                        System.out.println("Accepting connection");
                        //accept(key);
                        ServerSocketChannel server = (ServerSocketChannel) key.channel();
                        SocketChannel channel = server.accept();

                        channel.configureBlocking(false);
                        channel.register(selector, SelectionKey.OP_READ, ByteBuffer.allocate(1024));
                    }
                    if (key.isWritable()) {


                        SocketChannel channel = (SocketChannel) key.channel();

                        Utils.sendResponse(resObj, channel);
                        key.interestOps(SelectionKey.OP_READ);
                    }
                    if (key.isReadable()) {
                        SocketChannel channel = (SocketChannel) key.channel();
                        ByteBuffer readBuffer = ByteBuffer.allocate(1024);
                        readBuffer.clear();
                        resObj = responseAction(channel, key);
                        key.interestOps(SelectionKey.OP_WRITE);
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static Response stateToResponse(State state) {

        String word = state.getWord();
        int remaining = state.getRemainingAttemps();
        int score = state.getScore();
        boolean[] positions = state.getGuessed();
        Response.ResponseCode responseCode = Response.ResponseCode.GUESS_RESPONSE;
        int guessed = 0;

        for (int i = 0; i < positions.length; i++) {
            if (positions[i]) guessed++;
        }

        int idx = 0;
        char[] guessedLetters = new char[guessed];
        int[] guessedPositions = new int[guessed];

        for (int i = 0; i < positions.length; i++) {
            if (positions[i]) {
                guessedLetters[idx] = word.charAt(i);
                guessedPositions[idx] = i;
                idx++;
            }
        }


        return new Response(responseCode, word.length(), guessedLetters, guessedPositions, score, remaining);
    }

    public static Response errorResponse() {
        return new Response(Response.ResponseCode.ERROR_RESPONSE, 0, null, null, 0, 0);
    }

    public static Response winResponse(State state) {
        return new Response(Response.ResponseCode.WIN_RESPONSE, 0, state.word.toCharArray(), null, state.score, 0);
    }

    public static Response loseResponse(State state) {
        return new Response(Response.ResponseCode.LOSE_RESPONSE, 0, state.word.toCharArray(), null, state.score, 0);
    }

    public static Response loginResponse(String jwt) {
        return new Response(Response.ResponseCode.LOGIN_RESPONSE, 0, jwt.toCharArray(), null, 0, 0);
    }

    public static Response invalidJwtResponse() {
        return new Response(Response.ResponseCode.INVALID_JWT_RESPONSE, 0, null, null, 0, 0);
    }

    public static Response notLoggedInResponse() {
        return new Response(Response.ResponseCode.NOTLOGGED_RESPONSE, 0, null, null, 0, 0);
    }

    public static Response alreadyLoggedInResponse() {
        return new Response(Response.ResponseCode.ALREADY_LOGGED_RESPONSE, 0, null, null, 0, 0);
    }

    public static Response invalidPassword() {
        return new Response(Response.ResponseCode.INVALID_PASSOWORD, 0, null, null, 0, 0);
    }


    static Response responseAction(SocketChannel clientSocket, SelectionKey key) throws IOException, ClassNotFoundException {

        ByteBuffer readBuffer = ByteBuffer.allocate(1024);
        readBuffer.clear();


        Request reqObj = Utils.receiveRequest(clientSocket, key);

        if (reqObj == null) {
            return errorResponse();
        }

        String request = reqObj.requestString;
        String jwt = reqObj.jwt;
        UUID uuid = reqObj.id;

        State state = null;

        String correct_password = "password";

        if (request.toLowerCase().startsWith("login ")) {           // "login username password"

            String[] loginRequest = request.split(" ");

            //error response for wrong input
            if (loginRequest.length != 3) {
                return Server.errorResponse();
            }

            //check for already loggedin state
            state = store.getState(uuid);
            if (state != null) {
                return Server.alreadyLoggedInResponse();

            }

            String username = loginRequest[1];
            String password = loginRequest[2];

            //check for correct password
            if (!password.equals(correct_password)) {
                return Server.invalidPassword();

            }


            //we need to check the correct password
            String newjwt = jwtBuilder.withClaim("user", username).withIssuedAt(new Date(System.currentTimeMillis())).sign(Algorithm.HMAC256("secret"));

            //After successfully logged in, the state is created
            state = new State();
            store.setState(uuid, state);

            return Server.loginResponse(newjwt);


        } else if ("start game".equals(request.toLowerCase())) {   //state start game

            state = store.getState(uuid);

            //Make sure the state is created before start game.
            if (state == null || jwt == null) {
                return Server.notLoggedInResponse();

            }

            //jwt check before start game
            try {
                Algorithm algorithm = Algorithm.HMAC256("secret");
                JWTVerifier verifier = JWT.require(algorithm).build();
                verifier.verify(jwt);
            } catch (JWTVerificationException exception) {
                return Server.invalidJwtResponse();

            }

            System.out.println("Start game!");
            state.startGame(store.getWord());


        } else {        //playing state

            state = store.getState(uuid);

            //To make sure the state is created before playing, and jwt exists.
            if (jwt == null || state == null || !state.isPlaying()) {
                return Server.errorResponse();

            }

            //check for jwt
            try {
                Algorithm algorithm = Algorithm.HMAC256("secret");
                JWTVerifier verifier = JWT.require(algorithm).build();
                verifier.verify(jwt);
            } catch (JWTVerificationException exception) {
                return Server.invalidJwtResponse();

            }

            String[] guessRequest = request.split(" ");

            //check guess operation should always be "guess xxx"
            if (guessRequest.length != 2 || !"guess".equals(guessRequest[0].toLowerCase())) {
                System.out.println("unrecognized request");
                return Server.errorResponse();

            } else if (guessRequest[1].length() == 1) {
                System.out.println("single char guess request");
                state.guess(guessRequest[1].charAt(0));
            } else {
                System.out.println("whole word guess request");
                state.guess(guessRequest[1]);
            }
        }


        Response resp;
        if (state.isPlaying()) {
            resp = Server.stateToResponse(state);
        } else if (state.isWin()) {
            resp = Server.winResponse(state);
        } else if (state.isLose()) {
            resp = Server.loseResponse(state);
        } else {
            resp = Server.errorResponse();
        }

        //clientSocket.close();
        return resp;
    }

}