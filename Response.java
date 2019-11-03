import java.io.Serializable;

public class Response implements Serializable {

    private static final long serialVersionUID = 1L;

    public Response(int wordLength, char[] guessedLetters, int[] guessedPositions, int score, int remainingAtempts) {
        this.wordLength = wordLength;
        this.guessedLetters = guessedLetters;
        this.guessedPositions = guessedPositions;
        this.remainingAtempts = remainingAtempts;
        this.score = score;
    }

    public final int wordLength;  
    public final char[] guessedLetters;
    public final int[] guessedPositions;
    public final int remainingAtempts;
    public final int score;


}