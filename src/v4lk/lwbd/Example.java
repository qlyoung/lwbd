package v4lk.lwbd;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.LinkedList;

import v4lk.lwbd.decoders.JLayerMp3Decoder;
import v4lk.lwbd.decoders.LwbdDecoder;

/***
 * Runnable CLI example
 */
public class Example {

	public static void main(String[] args) throws FileNotFoundException {
		// get your audio
        File myAudioFile = new File(args[0]);
        // initialize the appropriate decoder for your platform
		LwbdDecoder decoder = new JLayerMp3Decoder(myAudioFile);
        // perform beat detection
        System.out.println("processing...");
		LinkedList<Beat> beats = BeatDetector.detectBeats(decoder, 1.4f);

        // print results
        for (Beat b : beats){
            System.out.print("Time: " + (b.timeMs / 1000f) + "s");
            System.out.print("\tEnergy: " + b.energy + "\n");
        }

	}

}
