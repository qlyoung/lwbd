package featherdev.lwbd;

import java.io.File;
import java.io.FileNotFoundException;

import featherdev.lwbd.decoders.JLayerMp3Decoder;
import featherdev.lwbd.decoders.LwbdDecoder;

public class Example {

	public static void main(String[] args) throws FileNotFoundException {
		File myAudioFile = new File("audio.mp3");
		LwbdDecoder decoder = new JLayerMp3Decoder(myAudioFile);
		BeatDetector.detectBeats(decoder, 1.4f);
	}

}
