package v4lk.lwbd;

import v4lk.lwbd.BeatDetector.AudioType;
import v4lk.lwbd.BeatDetector.DetectorSensitivity;
import v4lk.lwbd.decoders.Decoder;
import v4lk.lwbd.decoders.JLayerMp3Decoder;
import v4lk.lwbd.util.Beat;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

/**
 * Usage examples
 */
public class Examples {

    public static void main(String[] args) throws IOException {
        // Detect beats from an MP3 file using the platform's default MP3 decoder
        {
            File audioFile = new File("/path/to/audiofile.mp3");
            Beat[] beats = BeatDetector.detectBeats(audioFile, AudioType.MP3);
        }

        // Detect beats from an MP3 file, using a specific decoder
        {
            File audioFile = new File("/path/to/audiofile.mp3");
            FileInputStream stream = new FileInputStream(audioFile);
            Decoder decoder = new JLayerMp3Decoder(stream);
            Beat[] beats = BeatDetector.detectBeats(decoder);
        }

        // Detect beats from an MP3 file, specifying detector sensitivity
        {
            File audioFile = new File("/path/to/audiofile.mp3");
            Beat[] beats = BeatDetector.detectBeats(audioFile, AudioType.MP3, DetectorSensitivity.HIGH);
        }

        // Detect beats from an MP3 file, using a specific decoder and specifying detector sensitivity
        {
            File audioFile = new File("/path/to/audiofile.mp3");
            FileInputStream stream = new FileInputStream(audioFile);
            Decoder decoder = new JLayerMp3Decoder(stream);
            Beat[] beats = BeatDetector.detectBeats(decoder, DetectorSensitivity.LOW);
        }
    }

}