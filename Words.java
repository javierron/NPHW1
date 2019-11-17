import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.List;
import java.util.Random;
import java.nio.channels.FileChannel;
import java.io.FileInputStream;
import java.nio.ByteBuffer;
import java.util.Arrays;

public class Words {
    List<String> list;
    Random rand;

    //initialize the word list
    public Words() throws FileNotFoundException, IOException {
        this.rand = new Random();

        ByteBuffer buffer = ByteBuffer.allocate(1024 * 8);
        FileInputStream fis = new FileInputStream("./words.txt");
        FileChannel inChannel = fis.getChannel();

        String words = "";
        
        int c = 0;
        while((c = inChannel.read(buffer)) != -1){
            buffer.flip();
            words = words + new String(buffer.array());
            buffer.clear();
        }

        list = Arrays.asList(words.split("\\n"));
    }

    public String getRandomWord(){
        return list.get(rand.nextInt(list.size()));
    }
    
}