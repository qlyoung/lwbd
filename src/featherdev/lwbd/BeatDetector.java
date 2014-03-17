package featherdev.lwbd;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.LinkedHashMap;

import com.badlogic.audioanalysis.FFT;

import javazoom.jl.decoder.BitstreamException;
import javazoom.jl.decoder.DecoderException;
import javazoom.jl.decoder.JavaLayerException;

/**
 * lwbd -- portable lightweight beat detector
 * 
 * @author featherdev
 */

public class BeatDetector {

	public static class AudioFunctions {

		/**
		 * Calculates the spectral flux value for each sequential pair of
		 * 1024-sample windows in a given audio file
		 * 
		 * @return A list of all calculated spectral fluxes
		 * @throws IOException
		 * @throws DecoderException
		 * @throws BitstreamException
		 */
		public static ArrayList<Float> calculateSpectralFluxes(InputStream inputStream)
				throws IOException, DecoderException, BitstreamException {

			SimpleDecoder simpleDecoder = new SimpleDecoder(inputStream);

			// some collections and objects we'll need
			float[] currentSpectrum, previousSpectrum;
			FFT transformer = new FFT(1024, 44100);
			transformer.window(FFT.HAMMING);

			// make a list to hold the spectral fluxes
			ArrayList<Float> fluxes = new ArrayList<Float>();

			int spectrumSize = (1024 / 2) + 1;
			currentSpectrum = new float[spectrumSize];
			previousSpectrum = new float[spectrumSize];

			// calculate spectral fluxes
			int[] t = simpleDecoder.nextDeinterlacedFrame();
			
			while (t.length == 1024) {

				// convert to float
				float[] frame = new float[t.length];
				for (int i = 0; i < frame.length; i++)
					frame[i] = (float) t[i] / 32768f;

				// fft
				transformer.forward(frame);

				// array shuffle
				System.arraycopy(currentSpectrum, 0, previousSpectrum, 0, currentSpectrum.length);
				System.arraycopy(transformer.getSpectrum(), 0, currentSpectrum, 0, currentSpectrum.length);

				// calculate the spectral flux between two spectra
				float flux = 0;
				for (int i = 0; i < currentSpectrum.length; i++) {
					float tFlux = (currentSpectrum[i] - previousSpectrum[i]);
					flux += tFlux > 0 ? tFlux : 0;
				}

				fluxes.add(flux);
				
				t = simpleDecoder.nextDeinterlacedFrame();
			}

			return fluxes;
		}

		/**
		 * Performs onset detection on the set of spectral flux values returned
		 * by getSpectralFluxes().
		 * 
		 * @param spectralFluxes
		 *            The ArrayList<Float> containing the spectral flux values.
		 * @param sensitivity
		 *            Sensitivity value for threshold function
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

	public static final float SENSITIVITY_AGGRESSIVE = 1.0f;
	public static final float SENSITIVITY_STANDARD = 1.4f;
	public static final float SENSITIVITY_LOW = 1.7f;

	/**
	 * Detects rhythmic onsets in the given audio stream
	 * 
	 * @param sensitivity
	 *            Detector threshold sensitivity. 1.4 is a good value.
	 * @return A collection containing the detected beats.
	 * @throws JavaLayerException
	 *             If the stream is not an audio stream
	 * @throws IOException
	 *             If the stream is not valid
	 */
	public static ArrayList<Beat> detectBeats(File audioFile, float sensitivity)
			throws JavaLayerException, IOException {
		return detectBeats(new FileInputStream(audioFile), sensitivity);
	}

	public static ArrayList<Beat> detectBeats(InputStream audioStream, float sensitivity)
			throws JavaLayerException, IOException {

		ArrayList<Float> spectralFluxes = AudioFunctions.calculateSpectralFluxes(audioStream);
		ArrayList<Float> peaks = AudioFunctions.getPeaks((ArrayList<Float>) spectralFluxes, sensitivity);
		ArrayList<Beat> beats = new ArrayList<Beat>();
		LinkedHashMap<Long, Float> timeEnergyMap = new LinkedHashMap<Long, Float>( 15);

		// Convert to time - energy map
		long i = 0;
		for (float f : peaks) {
			if (f > 0) {
				long timeInMillis = (long) (((float) i * (1024f / 44100f)) * 1000f);
				timeEnergyMap.put(timeInMillis, f);
			}
			i++;
		}

		// normalize values to range [0, 1]
		float max = 0;

		for (Float f : timeEnergyMap.values())
			if (f > max)
				max = f;

		float value = 0;
		for (Long l : timeEnergyMap.keySet()) {
			value = timeEnergyMap.get(l);
			value /= max;
			timeEnergyMap.put(l, value);
		}

		// store beats in a collection
		for (Long l : timeEnergyMap.keySet())
			beats.add(new Beat(l, timeEnergyMap.get(l)));

		return beats;
	}

}
