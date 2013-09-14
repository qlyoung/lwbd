package com.albadev.lwbd.example;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

import javazoom.jl.decoder.JavaLayerException;

import com.albadev.lwbd.Beat;
import com.albadev.lwbd.BeatDetector;

public class MainExample {

	/**
	 * This program demonstrates beat detection with lwbd. It uses Madeon's
	 * remix of Smile Like You Mean It for input.
	 * 
	 * Check the console for output!
	 * 
	 * @param args
	 * @throws IOException
	 *             If you pass the detector a nonexistent file or stream
	 * @throws JavaLayerException
	 *             If the detector can't read the file
	 */
	public static void main(String[] args) throws JavaLayerException,
			IOException {

		// Detect beats
		File mp3file = new File("sample-audio/madeon-smile.mp3");

		BeatDetector detector = new BeatDetector(mp3file);
		detector.setVerbose(System.out);

		ArrayList<Beat> beats = detector.detectBeats(1.5f);

		
		// print some stuff
		System.out.println("\nFirst five beats: ");

		for (int i = 0; i < 5; i++)
			System.out.println(beats.get(i).toString() + "\n");

	}

}
