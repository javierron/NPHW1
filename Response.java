import java.io.Serializable;

public class Response implements Serializable {

    private static final long serialVersionUID = 1L;
    public enum ResponseCode{
        WIN_RESPONSE,
        ERROR_RESPONSE,
        LOSE_RESPONSE,
        LOGIN_RESPONSE,
        INVALID_JWT_RESPONSE,
        NOTLOGGED_RESPONSE,
        ALREADY_LOGGED_RESPONSE,
        GUESS_RESPONSE

    }

    public Response(ResponseCode reponseCode, int wordLength, char[] guessedLetters, int[] guessedPositions, int score, int remainingAtempts) {
        this.responseCode = reponseCode;
        this.wordLength = wordLength;
        this.guessedLetters = guessedLetters;
        this.guessedPositions = guessedPositions;
        this.remainingAtempts = remainingAtempts;
        this.score = score;
    }

    public final ResponseCode responseCode;
    public final int wordLength;  
    public final char[] guessedLetters;
    public final int[] guessedPositions;
    public final int remainingAtempts;
    public final int score;


}