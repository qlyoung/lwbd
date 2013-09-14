package com.albadev.lwbd;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.LinkedHashMap;

import javazoom.jl.decoder.BitstreamException;
import javazoom.jl.decoder.DecoderException;
import javazoom.jl.decoder.JavaLayerException;

import com.badlogic.audio.analysis.FFT;

/**
 * A lightweight beat detector in pure Java. Completely portable, doesn't rely
 * on JavaSound or the JMF. Uses JLayer for decoding and pieces of Minim for
 * Fourier transformations.
 * 
 * Works, albeit somewhat slowly, on Android.
 * 
 * @author albatross
 */

public class BeatDetector {

	public static class AudioFunctions {

		/**
		 * Calculates the spectral fluxes of a given audio file.
		 * 
		 * @param simpleDecoder A SimpleDecoder initialized with the audio file.
		 * @return An ArrayList<Float> containing the spectral flux value for
		 *         each 1024 sample window in the audio file.
		 * @throws IOException
		 * @throws DecoderException
		 * @throws BitstreamException
		 */
		public static ArrayList<Float> getSpectralFluxes(InputStream inputStream)
				throws IOException, DecoderException, BitstreamException {

			SimpleDecoder simpleDecoder = new SimpleDecoder(inputStream);

			// some collections and objects we'll need
			float[] currentSpectrum, previousSpectrum;
			FFT transformer = new FFT(1024, 44100);
			transformer.window(FFT.HAMMING);

			// make a list to hold the spectral fluxes
			ArrayList<Float> spectralFluxes = new ArrayList<Float>();

			currentSpectrum = new float[(1024 / 2) + 1];
			previousSpectrum = new float[(1024 / 2) + 1];

			// sequentially retrieve frames and calculate spectral flux between
			// them

			while (!simpleDecoder.hitEnd) {

				int[] mergedFrames = new int[1024];
				mergedFrames = simpleDecoder.getNextDeinterlacedFrame();

				// convert those integer samples to float samples
				float[] frame = new float[mergedFrames.length];

				for (int i = 0; i < frame.length; i++) {
					frame[i] = (float) mergedFrames[i] / 32768f;
				}

				// do some Fourier magic
				transformer.forward(frame);

				// put the old spectrum into previousSpectrum and put the new
				// one into currentSpectrum
				System.arraycopy(currentSpectrum, 0, previousSpectrum, 0,
						currentSpectrum.length);
				System.arraycopy(transformer.getSpectrum(), 0, currentSpectrum,
						0, currentSpectrum.length);

				// calculate the spectral flux using the previous and current
				// frame
				float flux = 0;
				for (int i = 0; i < currentSpectrum.length; i++) {
					float tFlux = (currentSpectrum[i] - previousSpectrum[i]);
					flux += tFlux > 0 ? tFlux : 0;
				}

				spectralFluxes.add(flux);
			}

			return spectralFluxes;
		}

		/**
		 * Performs onset detection on the set of spectral flux values returned
		 * by getSpectralFluxes().
		 * 
		 * @param spectralFluxes The ArrayList<Float> containing the spectral flux values.
		 * @param sensitivity Sensitivity value for threshold function
		 * @return An ArrayList<Float> containing a representation of the audio
		 *         file. There are approx. 43 values for every 1 second of
		 *         audio. All values are zero except where there are beats;
		 *         those values are the original sample values. The higher the
		 *         value the stronger the beat.
		 */
		public static ArrayList<Float> getPeaks(
				ArrayList<Float> spectralFluxes, float sensitivity) {

			ArrayList<Float> threshold = new ArrayList<Float>();

			{
				// This next bit calculates the threshold values for a range of ten
				// spectral fluxes. We'll use this later to find onsets.

				for (int i = 0; i < spectralFluxes.size(); i++) {
					int start = Math.max(0, i - 10);
					int end = Math.min(spectralFluxes.size() - 1, i + 10);
					float mean = 0;
					for (int j = start; j <= end; j++)
						mean += spectralFluxes.get(j);
					mean /= (end - start);
					threshold.add((float) mean * sensitivity);
				}
			}

			ArrayList<Float> prunedSpectralFluxes = new ArrayList<Float>();

			for (int i = 0; i < threshold.size(); i++) {
				if (threshold.get(i) <= spectralFluxes.get(i))
					prunedSpectralFluxes.add(spectralFluxes.get(i)
							- threshold.get(i));
				else
					prunedSpectralFluxes.add((float) 0);
			}

			ArrayList<Float> peaks = new ArrayList<Float>();

			for (int i = 0; i < prunedSpectralFluxes.size() - 1; i++) {
				if (prunedSpectralFluxes.get(i) > prunedSpectralFluxes
						.get(i + 1))
					peaks.add(prunedSpectralFluxes.get(i));
				else
					peaks.add((float) 0);
			}

			return peaks;
		}
	
	}

	private InputStream audioStream;
	private PrintStream debugStream;

	public BeatDetector(InputStream stream) {
		audioStream = stream;
	}
	public BeatDetector(File mp3file) throws FileNotFoundException{
		this(new FileInputStream(mp3file));
	}

	/**
	 * Detects rhythmic onsets in the given audio stream
	 * @param sensitivity Detector threshold sensitivity. 1.4 is a good value.
	 * @return A collection containing the detected beats.
	 * @throws JavaLayerException If the stream is not an audio stream
	 * @throws IOException If the stream is not valid
	 */
	public ArrayList<Beat> detectBeats(float sensitivity) throws JavaLayerException, IOException {

		long start_time = System.currentTimeMillis();
		

		writeDebug("Calculating spectral flux values...");
		ArrayList<Float> spectralFluxes = AudioFunctions.getSpectralFluxes(audioStream);


		writeDebug("Detecting rhythmic onsets...");
		ArrayList<Float> peaks = AudioFunctions.getPeaks((ArrayList<Float>) spectralFluxes, sensitivity);


		writeDebug("Post processing...");

		ArrayList<Beat> beats = new ArrayList<Beat>();
		{
			
			// Convert to time - energy map
			LinkedHashMap<Long, Float> timeEnergyMap = new LinkedHashMap<Long, Float>(15);
			{
				long i = 0;
				for (float f : peaks) {

					if (f > 0) {

						long timeInMillis = (long) (((float) i * (1024f / 44100f)) * 1000f);
						timeEnergyMap.put(timeInMillis, f);

					}

					i++;
				}
			}


			{// normalize values to range [0, 1]
				float max = 0;


				for (Float f : timeEnergyMap.values()){
					if (f > max)
						max = f;
				}


				float value = 0;
				for (Long l : timeEnergyMap.keySet()){
					value = timeEnergyMap.get(l);
					value /= max;
					timeEnergyMap.put(l, value);
				}
			}
			
			
			// store beats in a collection
			for (Long l : timeEnergyMap.keySet()){
				beats.add(new Beat(l, timeEnergyMap.get(l)));
			}
		
		}


		printResults(System.currentTimeMillis() - start_time, spectralFluxes.size(), beats.size());

		return beats;
	}

	
	/**
	 * Set the PrintStream to print debug information to.
	 * @param stream
	 *            The PrintStream. Can be null.
	 */
	public void setVerbose(PrintStream stream) {
		this.debugStream = stream;
	}

	/**
	 * Write a debug message to the PrintStream provided by the client.
	 * @param message
	 *            The message to print.
	 */
	private void writeDebug(String message) {
		if (this.debugStream != null) {
			this.debugStream.println(message);
		}
	}

	/**
	 * Print the results of a complete detection operation.
	 * 
	 * @param peakCount
	 *            Amount of actual beats
	 * @param fluxCount
	 *            Amount of spectral flux values calculated
	 * @param time
	 *            How long analysis took
	 */
	private void printResults(long time, int fluxCount, int beatCount) {
		writeDebug("---Audio Analysis Complete---");
		writeDebug("Time taken: " + String.valueOf(time / 1000l) + " seconds");
		writeDebug("Flux values: " + fluxCount);
		writeDebug("Beat values: " + beatCount);
		writeDebug("Song Percentage Beats: "
				+ String.valueOf(((float) beatCount / fluxCount) * 100) + "%");
		writeDebug("-------Analysis Complete-------");
	}
}
