public class State {

    String word;
    boolean[] guessedPositions;
    int remainingAtempts;
    int score;
    boolean playing = false;


    public String getWord(){
        return word;
    }

    public boolean[] getGuessed(){
        return this.guessedPositions;
    }
    
    public int getRemainingAttemps(){
        return this.remainingAtempts;
    }
    
    public int getScore(){
        return this.score;
    }
    
    public boolean isPlaying(){
        return this.playing;
    }

    public void startGame() {
        this.word = "word";
        int len = this.word.length();
        this.guessedPositions = new boolean[len];
        this.remainingAtempts = len;
        this.score = 0;
        this.playing = true;
    }

    public void guess(char x){
        for (int i = 0; i < word.length(); i++) {
            this.guessedPositions[i] = this.guessedPositions[i] || x == word.charAt(i);
        }

        this.remainingAtempts = this.remainingAtempts - 1;

        boolean win = true;

        for (int i = 0; i < guessedPositions.length; i++) {
            win = win && guessedPositions[i];
        }
        
        if(win){
            this.score = this.score + 1;
            this.playing = false;
        }

        boolean lose = this.remainingAtempts <= 0 && !win;

        if(lose){
            this.score = score - 1;
            this.playing = false;
        }
    }

    public void guess(String x){
        

        this.remainingAtempts = this.remainingAtempts - 1;

        boolean win = x.equals(this.word);
        
        if(win){
            this.score = score + 1;
            this.playing = false;
        }

        boolean lose = this.remainingAtempts <= 0;

        if(lose){
            this.score = score - 1;
            this.playing = false;
        }
    }


}