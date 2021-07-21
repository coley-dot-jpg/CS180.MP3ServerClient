import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;

/**
 * Project 4 - MP3Server.java
 *
 * This program is a server for sending mp3 files over a socket connection. It has a main method which creates the
 *  connection, accepts clients, and creates a thread for each client. The thread used listens for the client's requests
 *  and sends back the appropriate response.
 *
 * @author Emelie Coleman - colem109, sec. L17
 * @author Jason Bao - bao43, sec. L17
 *
 * @version April 12, 2019
 *
 */

public class MP3Server {

    public static void main(String[] args) {

        ServerSocket serverSocket;
        Socket clientSocket;

        try {
            serverSocket = new ServerSocket(50000);
        } catch (IOException e) {
            System.out.println("<An unexpected exception occurred>");

            System.out.printf("<Exception message: %s>\n", e.getMessage());

            System.out.println("<Stopping the server>");

            return;
        }
        System.out.println("<Starting the server>");
        while (true) {
            try {
                clientSocket = serverSocket.accept();
            } catch (IOException e) {
                System.out.println("<An unexpected exception occurred>");

                System.out.printf("<Exception message: %s>\n", e.getMessage());

                System.out.println("<Stopping the server>");

                try {
                    serverSocket.close();
                } catch (IOException i) {
                    i.printStackTrace();
                }
                return;
            }
            System.out.println("<Connected and receiving a request>");
            ClientHandler handler = new ClientHandler(clientSocket);
            Thread runIt = new Thread(handler);
            runIt.start();
            boolean status = runIt.isAlive();
            while (status) {
                status = runIt.isAlive();
            }
            System.out.println("<Client has disconnected>");
        }

    }

}

/**
 * Project 4 - ClientHandler.java
 *
 * This class implements Runnable, and will contain the logic for handling responses and requests to
 * and from a given client. The threads you create in MP3Server will be constructed using instances
 * of this class.
 *
 * @author Emelie Coleman - colem109, sec. L17
 * @author Jason Bao - bao43, sec. L17
 *
 * @version April 12, 2019
 *
 */

final class ClientHandler implements Runnable {

    private ObjectInputStream inputStream;
    private ObjectOutputStream outputStream;

    public ClientHandler(Socket clientSocket) {

        try {
            this.outputStream = new ObjectOutputStream(clientSocket.getOutputStream());
            this.inputStream = new ObjectInputStream(clientSocket.getInputStream());
        } catch (IOException e) {
            System.out.println("<An unexpected exception occurred>");
            System.out.printf("<Exception message: %s>\n", e.getMessage());
        }

    }

    /**
     * This method is the start of execution for the thread. See the handout for more details on what
     * to do here.
     */
    public void run() {
        try {
            Object input = "";
            while (!input.equals("exit")) {
                if (input != null && input instanceof SongRequest) {
                    if (((SongRequest) input).isDownloadRequest()) {
                        String artist = ((SongRequest) input).getArtistName();
                        String song = ((SongRequest) input).getSongName();
                        String fileName = artist + " - " + song + ".mp3";
                        if (fileInRecord(fileName)) {
                            this.outputStream.writeObject(new SongHeaderMessage(true, song, artist,
                                    readSongData(fileName).length));
                            this.outputStream.flush();
                            byte[] songData = readSongData(fileName);
                            sendByteArray(songData);
                        } else {
                            // Send a SongHeaderMessage back to the client with a fileSize of -1
                            this.outputStream.writeObject(new SongHeaderMessage(true, "", "", -1));
                            this.outputStream.flush();
                        }
                    } else {
                        this.outputStream.writeObject(new SongHeaderMessage(false));
                        this.outputStream.flush();
                        sendRecordData();
                    }
                }
                input = this.inputStream.readObject();
            }
        } catch (IOException e) {
            System.out.println("<An unexpected exception occurred>");
            System.out.printf("<Exception message: %s>\n", e.getMessage());
        } catch (ClassNotFoundException e) {
            System.out.println("<An unexpected exception occurred>");
            System.out.printf("<Exception message: %s>\n", e.getMessage());
        } finally {
            try {
                this.inputStream.close();
                this.outputStream.close();
            } catch (IOException e) {
                System.out.println("<An unexpected exception occurred>");
                System.out.printf("<Exception message: %s>\n", e.getMessage());
            }
        }
    }

    /**
     * Searches the record file for the given filename.
     *
     * @param fileName the fileName to search for in the record file
     * @return true if the fileName is present in the record file, false if the fileName is not
     */
    private static boolean fileInRecord(String fileName) {
        try (BufferedReader reader = new BufferedReader(new FileReader("record.txt"))) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.equals(fileName.replace("\n", ""))) {
                    return true;
                }
            }
        } catch (IOException e) {
            System.out.println("<An unexpected exception occurred>");
            System.out.printf("<Exception message: %s>\n", e.getMessage());
        }
        return false;
    }

    /**
     * Read the bytes of a file with the given name into a byte array.
     *
     * @param fileName the name of the file to read
     * @return the byte array containing all bytes of the file, or null if an error occurred
     */
    private static byte[] readSongData(String fileName) {
        File file = new File("songDatabase\\" + fileName);
        byte[] fileBytes = new byte[((int) file.length())];
        try {
            FileInputStream fis = new FileInputStream(file);
            fis.read(fileBytes);
            fis.close();
            return fileBytes;
        } catch (FileNotFoundException e) {
            System.out.println("<An unexpected exception occurred>");
            System.out.printf("<Exception message: %s>\n", e.getMessage());
        } catch (IOException e) {
            System.out.println("<An unexpected exception occurred>");
            System.out.printf("<Exception message: %s>\n", e.getMessage());
        }
        return fileBytes;
    }

    /**
     * Split the given byte array into smaller arrays of size 1000, and send the smaller arrays
     * to the client using SongDataMessages.
     *
     * @param songData the byte array to send to the client
     */
    private void sendByteArray(byte[] songData) {

        try {
            long myLong = songData.length / 1000;
            int loops = Math.round(myLong);
            int plusOne = songData.length % 1000;
            if (plusOne < 500) {
                loops++;
            }
            for (int i = 0; i < loops; i++) {
                byte[] newBytes;
                if (i == (loops - 1)) {
                    newBytes = new byte[plusOne];
                    for (int j = 0; j < plusOne; j++) {
                        newBytes[j] = songData[(i * 1000) + j];
                    }
                } else {
                    newBytes = new byte[1000];
                    for (int j = 0; j < 1000; j++) {
                        newBytes[j] = songData[(i * 1000) + j];
                    }
                }
                this.outputStream.writeObject(new SongDataMessage(newBytes));
                this.outputStream.flush();
            }
        } catch (IOException e) {
            System.out.println("<An unexpected exception occurred>");
            System.out.printf("<Exception message: %s>\n", e.getMessage());
        }
    }

    /**
     * Read ''record.txt'' line by line again, this time formatting each line in a readable
     * format, and sending it to the client. Send a ''null'' value to the client when done, to
     * signal to the client that you've finished sending the record data.
     */
    private void sendRecordData() {

        try (BufferedReader reader = new BufferedReader(new FileReader("record.txt"))) {
            String line;
            while ((line = reader.readLine()) != null) {
                String[] info = line.split(" - ");
                info[1] = info[1].replace(".mp3", "");
                String recordData = String.format("\"%s\" by: %s", info[1], info[0]);
                this.outputStream.writeObject(recordData);
                this.outputStream.flush();
            }
            this.outputStream.writeObject("");
            this.outputStream.flush();
        } catch (IOException e) {
            System.out.println("<An unexpected exception occurred>");
            System.out.printf("<Exception message: %s>\n", e.getMessage());
        }
    }
}