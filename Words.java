import java.io.File;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.Random;
import java.util.Scanner;

public class Words {
    ArrayList<String> list;
    Random rand;

    //initialize the word list
    public Words() throws FileNotFoundException {
        this.list = new ArrayList<String>();
        this.rand = new Random();

        Scanner s = new Scanner(new File("./words.txt"));
        while (s.hasNext()){
            this.list.add(s.next());
        }
        s.close();
    }

    public String getRandomWord(){
        return list.get(rand.nextInt(list.size()));
    }
    
}