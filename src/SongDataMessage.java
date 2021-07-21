import java.io.Serializable;

/**
 * Project 4 - SongDataMessage.java
 *
 * A message encapsulating the byte data of a song. Multiple of these messages should be sent for each song
 *  because songs should be broken up into segments of 1000 bytes or less.
 *
 * @author Emelie Coleman - colem109, sec. L17
 * @author Jason Bao - bao43, sec. L17
 *
 * @version April 12, 2019
 *
 */

public class SongDataMessage implements Serializable {
    private byte[] data;

    public SongDataMessage(byte[] data) {
        this.data = data;
    }

    public byte[] getData() {
        return data;
    }
}
