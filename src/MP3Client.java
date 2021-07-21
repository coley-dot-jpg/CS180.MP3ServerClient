import java.io.*;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Scanner;

/**
 * Project 4 - MP3Client.java
 *
 * This program is a client that requests mp3 files from a serve and receives them over a socket connection. It has a
 *  main method which connects to the server then prompts the user for the type of request they would like to send. The
 *  program then creates a request based off of their input and send it to the server.  A ResponseListener thread is
 *  then created which receives the incoming information appropriately.
 *
 * @author Emelie Coleman - colem109, sec. L17
 * @author Jason Bao - bao43, sec. L17
 *
 * @version April 12, 2019
 *
 */

public class MP3Client {

    public static void main(String[] args) {

        Socket serverConnection = null;
        ObjectOutputStream outServer;
        Scanner inUser = new Scanner(System.in);

        try {
            boolean exit = false;
            do {
                serverConnection = new Socket( "localhost", 50000);
                outServer = new ObjectOutputStream(serverConnection.getOutputStream());
                ResponseListener listener;
                boolean status = true;
                System.out.println("\nWhat do you want to do?\n------------\n(view): see the list of available songs" +
                        "\n(download): download a song");
                String userResponse = inUser.nextLine();
                switch (userResponse) {
                    case "view":
                        try {
                            outServer.writeObject(new SongRequest(false));
                            outServer.flush();
                            listener = new ResponseListener(serverConnection);
                            Thread myThread = new Thread(listener);
                            myThread.start();
                            while (status) {
                                status = myThread.isAlive();
                            }
                        } catch (IOException e) {
                            System.out.println("<An unexpected exception occurred>");
                            System.out.printf("<Exception message: %s>\n", e.getMessage());
                        }
                        break;
                    case "download":
                        System.out.println("Song name?");
                        String songName = inUser.nextLine();
                        System.out.println("Artist name?");
                        String artistName = inUser.nextLine();
                        try {
                            outServer.writeObject(new SongRequest(true, songName, artistName));
                            outServer.flush();
                            listener = new ResponseListener(serverConnection);
                            Thread myThread = new Thread(listener);
                            myThread.start();
                            while (status) {
                                status = myThread.isAlive();
                            }
                        } catch (IOException e) {
                            System.out.println("<An unexpected exception occurred>");
                            System.out.printf("<Exception message: %s>\n", e.getMessage());
                        }
                        break;
                    case "exit":
                        outServer.writeObject(userResponse);
                        outServer.flush();
                        outServer.close();
                        inUser.close();
                        exit = true;
                        break;
                    default:
                        System.out.println("Invalid choice");
                }
                serverConnection.close();
            } while (exit == false);

        } catch (IOException e) {
            System.out.println("<An unexpected exception occurred>");
            System.out.printf("<Exception message: %s>\n", e.getMessage());
            if (serverConnection != null) {
                try {
                    serverConnection.close();
                } catch (IOException i) {
                    i.printStackTrace();
                }
            }
            return;
        }
        System.out.println("<Lost the connection with the server>");
    }
}

/**
 * Project 4 - ResponseListener.java
 *
 * This class implements Runnable, and will contain the logic for listening for
 * server responses. The threads you create in MP3Server will be constructed using
 * instances of this class.
 *
 * @author Emelie Coleman - colem109, sec. L17
 * @author Jason Bao - bao43, sec. L17
 *
 * @version April 12, 2019
 *
 */

final class ResponseListener implements Runnable {

    private ObjectInputStream ois;

    public ResponseListener(Socket clientSocket) {
        try {
            this.ois = new ObjectInputStream(clientSocket.getInputStream());
        } catch (IOException e) {
            System.out.println("<An unexpected exception occurred>");
            System.out.printf("<Exception message: %s>\n", e.getMessage());
        }
    }

    /**
     * Listens for a response from the server.
     * <p>
     * Continuously tries to read a SongHeaderMessage. Gets the artist name, song name, and file size from that header,
     * and if the file size is not -1, that means the file exists. If the file does exist, the method then subsequently
     * waits for a series of SongDataMessages, takes the byte data from those data messages and writes it into a
     * properly named file.
     */
    public void run() {

        try {
            Object input;
            while (true) {
                input = this.ois.readObject();
                if (input != null && input instanceof SongHeaderMessage) {
                    SongHeaderMessage request = (SongHeaderMessage) input;
                    String songName = request.getSongName();
                    String artistName = request.getArtistName();
                    String fileName = artistName + " - " + songName + ".mp3";
                    int fileSize = request.getFileSize();
                    if (request.isSongHeader()) {
                        if (fileSize == -1) {
                            System.out.println("File not on record");
                            this.ois.close();
                            return;
                        } else if (fileSize > -1) {
                            ArrayList<Byte> myBytes = new ArrayList<>();
                            System.out.println("Receiving data...");
                            byte[] fileBytes = new byte[fileSize];
                            Object newData = this.ois.readObject();
                            if (newData != null && newData instanceof SongDataMessage) {
                                byte[] newBytes = ((SongDataMessage) newData).getData();
                                for (int i = 0; i < newBytes.length; i++) {
                                    myBytes.add(newBytes[i]);
                                }
                            }
                            while (newData != null && newData instanceof SongDataMessage) {
                                newData = this.ois.readObject();
                                byte[] newBytes = ((SongDataMessage) newData).getData();
                                for (int i = 0; i < newBytes.length; i++) {
                                    myBytes.add(newBytes[i]);
                                }
                                //newData = this.ois.readObject();
                                if (fileSize == myBytes.size()) {
                                    break;
                                }
                            }
                            for (int i = 0; i < myBytes.size(); i++) {
                                fileBytes[i] = myBytes.get(i);
                            }
                            System.out.println("Saving song...");
                            writeByteArrayToFile(fileBytes, fileName);
                            System.out.println("Process complete!");
                            this.ois.close();
                            return;
                        }
                    } else {
                        Object newData = this.ois.readObject();
                        while (newData != null && newData instanceof String) {
                            System.out.println((String) newData);
                            newData = this.ois.readObject();
                            if (newData.equals("") || newData.equals(null)) {
                                this.ois.close();
                                return;
                            }
                        }
                    }
                }
            }
        } catch (IOException e) {
            System.out.println("<An unexpected exception occurred>");
            System.out.printf("<Exception message: %s>\n", e.getMessage());
        } catch (ClassNotFoundException e) {
            System.out.println("<An unexpected exception occurred>");
            System.out.printf("<Exception message: %s>\n", e.getMessage());
        }
    }

    /**
     * Writes the given array of bytes to a file whose name is given by the fileName argument.
     *
     * @param songBytes the byte array to be written
     * @param fileName  the name of the file to which the bytes will be written
     */
    private void writeByteArrayToFile(byte[] songBytes, String fileName) {

        try {
            File file = new File("savedSongs\\" + fileName);
            FileOutputStream writer = new FileOutputStream(file);
            writer.write(songBytes);
            writer.close();
        } catch (IOException e) {
            System.out.println("<An unexpected exception occurred>");
            System.out.printf("<Exception message: %s>\n", e.getMessage());
        }

    }
}