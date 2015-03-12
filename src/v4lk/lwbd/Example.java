package v4lk.lwbd;

import v4lk.lwbd.BeatDetector.AudioType;
import v4lk.lwbd.util.Beat;

import java.io.EOFException;
import java.io.File;
import java.io.FileNotFoundException;
import java.util.zip.DataFormatException;

/***
 * Runnable CLI example
 */
public class Example {

	public static void main(String[] args) throws FileNotFoundException, EOFException, DataFormatException {

        File audioFile = new File(args[0]);
        Beat[] beats = BeatDetector.detectBeats(audioFile, AudioType.FLAC);

        for (Beat b : beats) {
            System.out.println(b.toString());
        }
	}

}
