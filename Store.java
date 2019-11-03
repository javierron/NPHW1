import java.io.FileNotFoundException;
import java.util.HashMap;
import java.util.UUID;

public class Store {

    private HashMap<UUID, State> map;
    private Words words;

    public Store() throws FileNotFoundException {
        this.map = new HashMap<UUID, State>();
        this.words = new Words();
    }
    
    public State getState(UUID uuid) {
        return map.get(uuid);
    }

    public void setState(UUID uuid, State state) {
        map.put(uuid, state);
    }

    public String getWord() {
        return words.getRandomWord();
    }

}