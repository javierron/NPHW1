import java.util.HashMap;
import java.util.UUID;

public class Store {

    HashMap<UUID, State> map = new HashMap<UUID, State>();
    
    public State getState(UUID uuid) {
        return map.get(uuid);
    }

    public void setState(UUID uuid, State state) {
        map.put(uuid, state);
    }

}