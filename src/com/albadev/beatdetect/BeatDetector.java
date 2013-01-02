package com.albadev.beatdetect;

import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Set;

import javazoom.jl.decoder.BitstreamException;
import javazoom.jl.decoder.DecoderException;
import javazoom.jl.decoder.JavaLayerException;

import com.badlogic.audio.analysis.FFT;

/**
 * A lightweight beat detector in pure Java. Completely portable, doesn't rely
 * on JavaSound or the JMF. Uses JLayer for decoding and pieces of Minim for
 * Fourier transformations.
 * 
 * LWBD can handle mp3 files sampled at 44.1khz, making it compatible with the
 * vast majority of mp3 files. Depending on the
 * 
 * Works on Android!
 * 
 * @author albatross
 * 
 */

public class BeatDetector {

	public static class AudioFunctions {

		/**
		 * Calculates the spectral fluxes of a given audio file.
		 * 
		 * @param simpleDecoder
		 *            A SimpleDecoder initialized with the audio file.
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
		 * @param spectralFluxes
		 *            The ArrayList<Float> containing the spectral flux values.
		 * @param sensitivity
		 *            How sensitive detection should be. A good value is 1.6
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
				// This next bit calculates the threshold values for a range of
				// ten
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

			/*
			 * Right, now we have the threshold function, so let's pull out the
			 * values from the spectral flux function that are bigger than the
			 * threshold. What this does is it goes through each spectral flux
			 * value and checks to see if it radically exceeds those around it,
			 * using the threshold function we calculated in the previous block.
			 * For each spectral flux value it inspects, it adds a value to a
			 * new array called prunedSpectralFluxes. If it finds that the
			 * spectral flux value does indeed exceed those around it (ergo it
			 * exceeds its respective threshold value), it adds the difference
			 * between the threshold and the flux value to the list; if not, it
			 * adds a zero. Thus we get an ArrayList that parallels the entire
			 * audio track, and contains positive values where there are beats
			 * and zeros where there are none. The returned list holds approx.
			 * 43 values per second of audio.
			 */

			ArrayList<Float> prunedSpectralFluxes = new ArrayList<Float>();

			for (int i = 0; i < threshold.size(); i++) {
				if (threshold.get(i) <= spectralFluxes.get(i))
					prunedSpectralFluxes.add(spectralFluxes.get(i)
							- threshold.get(i));
				else
					prunedSpectralFluxes.add((float) 0);
			}

			/*
			 * So now, in prunedSpectralFluxes, we have values that are greater
			 * than their threshold. These will by necessity form up in a spike
			 * due to the nature of waveforms. --diagram omitted-- Now, we just
			 * want the peak value of the spike. So all we do is go through each
			 * value and see if it is bigger than the next value. If so, it's a
			 * peak.
			 */

			ArrayList<Float> peaks = new ArrayList<Float>();

			for (int i = 0; i < prunedSpectralFluxes.size() - 1; i++) {
				if (prunedSpectralFluxes.get(i) > prunedSpectralFluxes
						.get(i + 1))
					peaks.add(prunedSpectralFluxes.get(i));
				else
					peaks.add((float) 0);
			}

			/*
			 * And voila. ArrayList peaks now contains all of our beats. To
			 * calculate the time these peaks occur, simply multiply the index
			 * of the peak by (frame size / sampling rate) So here, to calculate
			 * the time, we would use (index * (1024 / 44100)) to get the time.
			 */

			return peaks;
		}
	}

	/**
	 * Post processor for use with arraylists returned by
	 * AudioFunctions.detectPeaks(). Formats data into convenient forms.
	 * 
	 * @author albatross
	 * 
	 */
	public static class BeatsProcessor {

		/**
		 * Trims all zero values, calculates the sample index of each remaining
		 * value, and pairs them in a linked hash map with beat sample index as
		 * the key and beat intensity as the value.
		 * 
		 * @param beats
		 * @return
		 */
		public static LinkedHashMap<Long, Float> getIndexIntensityMap(
				ArrayList<Float> beats) {

			LinkedHashMap<Long, Float> timeIntensities = new LinkedHashMap<Long, Float>(
					15);

			long i = 0;
			for (float f : beats) {
				if (f > 0) {
					timeIntensities.put(i, f);
				}
				i++;
			}

			return timeIntensities;
		}

		/**
		 * Trims all zero values, calculates the time in milliseconds of each
		 * remaining value, and pairs them in a linked hash map with beat time
		 * as the key and beat intensity as the value.
		 * 
		 * @param beats
		 * @return
		 */
		public static LinkedHashMap<Long, Float> getTimeIntensityMap(
				ArrayList<Float> beats) {

			LinkedHashMap<Long, Float> timeIntensities = new LinkedHashMap<Long, Float>(
					15);

			long i = 0;
			for (float f : beats) {
				if (f > 0) {

					long timeInMillis = (long) ( i * (1024f / 44100f) * 1000f);
					
					System.out.println("Original Calculation: " + timeInMillis);
					

					timeIntensities.put(timeInMillis, f);
				}
				i++;
			}

			return timeIntensities;

		}

		/**
		 * Trims beats to guarantee that the specified number of milliseconds
		 * are present between each beat. Compares consecutive beats and checks
		 * time in between; if it is less than the specified amount of time then
		 * it removes the smaller beat and checks against the next consecutive
		 * values.
		 * 
		 * @param minTimeBetween
		 * @param timeIntensityBeats
		 */
		public static void removeCloseBeats(long minTimeBetween,
				LinkedHashMap<Long, Float> timeIntensityBeats) {

			Iterator<Long> iterator = timeIntensityBeats.keySet().iterator();

			long prevTime = iterator.next();
			long currTime = 0l;

			while (iterator.hasNext()) {
				currTime = iterator.next();

				if (currTime - prevTime < minTimeBetween) {

					// keep the stronger beat

					if (timeIntensityBeats.get(prevTime) < timeIntensityBeats
							.get(currTime)) {
						
						// remove the first beat
						timeIntensityBeats.remove(prevTime);
						
						/*
						 * Reset the iterator and continue counting. NOTE:
						 * Extremely expensive operation. This does the same
						 * thing as removing one beat and passing the resulting
						 * array to the method again, effectively restarting the
						 * trimming process from the beginning of the song. Must
						 * be fixed for production builds.
						 */
						iterator = timeIntensityBeats.keySet().iterator();
						prevTime = iterator.next();
						
					} else {
						// remove the second beat
						iterator.remove();
					}
				} else {
					prevTime = currTime;
				}
			}

		}
	
		/**
		 * Not implemented yet
		 * @param percentage Percentage of low energy beats to keep
		 * @param beats Map containing beats
		 */
		public static void thinSmallBeats(int percentage, LinkedHashMap<Long, Float> beats){
			
			int count = 0;
			float average, acc = 0;
			
			for (float f : beats.values()){
				acc += f;
				count++;
			}
			
			average = acc / count;
			
			
			Set<Long> set;
			
		}
	}

	private InputStream audioStream;
	private PrintStream debugStream;

	public enum BeatsFormat {
		TIME_INTENSITY, INDEX_INTENSITY
	};

	public BeatDetector(InputStream stream) {
		audioStream = stream;
	}

	/**
	 * Performs beat detection with the provided sensitivity
	 * 
	 * @param sensitivity
	 *            How sensitive detection should be. A good value is 1.6.
	 * @param format
	 *            The format the returned LinkedHashMap<Long, Float> should be
	 *            in
	 * @return A LinkedHashMap<Long, Float> containing the beats. The format of
	 *         the map is either time (milliseconds) : beat intensity or sample
	 *         index : beat intensity depending on the value of format.
	 * @throws JavaLayerException
	 * @throws IOException
	 */
	public LinkedHashMap<Long, Float> detectBeats(float sensitivity,
			BeatsFormat format) throws JavaLayerException, IOException {

		if (format == null)
			return null;

		long start_time = System.currentTimeMillis();

		writeDebug("Calculating spectral flux values...");
		ArrayList<Float> spectralFluxes = AudioFunctions
				.getSpectralFluxes(audioStream);

		writeDebug("Detecting rhythmic onsets...");
		ArrayList<Float> peaks = AudioFunctions.getPeaks(
				(ArrayList<Float>) spectralFluxes, sensitivity);

		writeDebug("Formatting...");
		LinkedHashMap<Long, Float> beats = null;

		if (format == BeatsFormat.INDEX_INTENSITY)
			beats = BeatsProcessor.getIndexIntensityMap(peaks);
		else if (format == BeatsFormat.TIME_INTENSITY)
			beats = BeatsProcessor.getTimeIntensityMap(peaks);

		printResults(System.currentTimeMillis() - start_time, spectralFluxes
				.size(), beats.size());

		return beats;
	}

	/**
	 * Set the PrintStream to print debug information to.
	 * 
	 * @param stream
	 *            The PrintStream. Can be null.
	 */
	public void setDebugStream(PrintStream stream) {
		this.debugStream = stream;
	}

	/**
	 * Write a debug message to the PrintStream provided by the client.
	 * 
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
