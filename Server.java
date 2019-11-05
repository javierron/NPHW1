import java.net.*;
import java.util.Date;
import java.util.UUID;

import com.auth0.jwt.*;
import com.auth0.jwt.algorithms.*;
import com.auth0.jwt.exceptions.JWTVerificationException;

public class Server {
    public static void main(String[] args) {
        try {

            JWTCreator.Builder jwtBuilder = JWT.create();
            
            System.out.println("Initiated server!");
            
            boolean exit = false;
            Store store = new Store();
            ServerSocket serverSocket = new ServerSocket(8080);
            
            while (!exit) {
            
                try {
                    Socket clientSocket = serverSocket.accept();
                    
                    Thread t = new Thread(new ResponseThread(clientSocket, store, jwtBuilder));
                    t.start();
                }catch(Exception e ){

                    System.out.println("Client disconnected");
                }

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

            String password = "SuperSecurePassword";

            if(request.toLowerCase().startsWith("login ")){ // "login username password"
                
                String[] loginRequest = request.split(" ");
                if(loginRequest.length != 3) {
                    Utils.sendResponse(Server.errorResponse(), clientSocket);
                    return;
                }
            
                state = store.getState(uuid);
                if(state != null){
                    Utils.sendResponse(Server.alreadyLoggedInResponse(), clientSocket);
                    return;
                }
                String username = loginRequest[1];

                //assume correct password
                String newjwt = jwtBuilder.withClaim("user", username).withIssuedAt(new Date(System.currentTimeMillis())).sign(Algorithm.HMAC256("secret")); 
                
                state = new State();
                store.setState(uuid, state);

                Utils.sendResponse(Server.loginResponse(newjwt), clientSocket);
                return;

            }else if("start game".equals(request.toLowerCase())){

                state = store.getState(uuid);
                if(state == null || jwt == null){
                    Utils.sendResponse(Server.notLoggedInResponse(), clientSocket);
                    return;
                }

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
                

            } else {

                state = store.getState(uuid);
                
                if(jwt == null || state == null || !state.isPlaying()){ 
                    Utils.sendResponse(Server.errorResponse(), clientSocket);
                    return;
                }

                try {
                    Algorithm algorithm = Algorithm.HMAC256("secret");
                    JWTVerifier verifier = JWT.require(algorithm).build();
                    verifier.verify(jwt);
                } catch (JWTVerificationException exception){
                    Utils.sendResponse(Server.invalidJwtResponse(), clientSocket);
                    return;
                }

                String[] guessRequest = request.split(" ");
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

