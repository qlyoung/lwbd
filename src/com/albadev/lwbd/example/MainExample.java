package com.albadev.lwbd.example;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

import javazoom.jl.decoder.JavaLayerException;

import com.albadev.lwbd.Beat;
import com.albadev.lwbd.BeatDetector;

public class MainExample {

	/**
	 * This program demonstrates the use of lwbd
	 * 
	 * @param args
	 * @throws IOException
	 *             If you pass the detector a nonexistent file or stream
	 * @throws JavaLayerException
	 *             If the detector can't read the file
	 */
	
	@SuppressWarnings("unused")
	public static void main(String[] args) throws JavaLayerException, IOException {

		// step 1 - get your audio file
		File mp3file = new File("sample-audio/madeon-smile.mp3");

		// step 2 - get your beats
		ArrayList<Beat> beats = BeatDetector.detectBeats(mp3file, BeatDetector.SENSITIVITY_AGGRESSIVE);

	}
}
