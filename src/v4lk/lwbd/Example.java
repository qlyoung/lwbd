package v4lk.lwbd;

import v4lk.lwbd.BeatDetector.AudioType;
import v4lk.lwbd.util.Beat;

import java.io.File;
import java.io.IOException;

/***
 * Runnable CLI example
 */
public class Example {

    public static void main(String[] args) throws IOException, BeatDetector.UnsupportedPlatformException {

        File audioFile = new File(args[0]);
        Beat[] beats = BeatDetector.detectBeats(audioFile, AudioType.FLAC, BeatDetector.DetectorSensitivity.LOW);

        for (Beat b : beats)
            System.out.println(b.toString());
    }

}