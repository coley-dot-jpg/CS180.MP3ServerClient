import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;

public class textRecords {
    public static void main(String[] args) {
        try (BufferedReader reader = new BufferedReader(new FileReader("record.txt"))) {
            String line;
            while ((line = reader.readLine()) != null) {
                System.out.println(line.contains("\n"));
                String[] info = line.split(" - ");
                info[1] = info[1].replace(".mp3", "");
                String recordData = String.format("'%s' by: %s", info[1], info[0]);
                System.out.println(recordData);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}
