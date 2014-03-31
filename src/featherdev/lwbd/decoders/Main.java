package featherdev.lwbd.decoders;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.LinkedList;

import featherdev.lwbd.Beat;
import featherdev.lwbd.BeatDetector;

public class Main {

	public static void main(String[] args) {
		
		File f = new File("/home/snowdrift/music/Shulman/Random Thoughts/01 OMG.mp3");
		JLayerDecoder d;
		try {
			d = new JLayerDecoder(f);
			LinkedList<Beat> b = BeatDetector.detectBeats(d, BeatDetector.SENSITIVITY_AGGRESSIVE);
			
			System.out.println(b.size());
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
	}

}
