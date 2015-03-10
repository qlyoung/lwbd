package v4lk.lwbd;

import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedList;

import v4lk.lwbd.decoders.LwbdDecoder;
import v4lk.lwbd.processing.fft.FFT;

/**
 * lwbd -- portable lightweight beat detector
 * 
 * @author featherdev
 */

public class BeatDetector {

	private static class AudioFunctions {

		/**
		 * Calculates the spectral flux value for each sequential pair of
		 * 1024-sample windows in a given audio file
		 * 
		 * @return A list of all calculated spectral fluxes
		 * @throws IOException
		 * @throws DecoderException
		 * @throws BitstreamException
		 */
		public static LinkedList<Float> calculateSpectralFluxes(LwbdDecoder decoder) {

			// some collections and objects we'll need
			FFT transformer = new FFT(1024, 44100);
			transformer.window(FFT.HAMMING);
			float[] currentSpectrum, previousSpectrum;
			LinkedList<Float> fluxes = new LinkedList<Float>();
			int spectrumSize = (1024 / 2) + 1;
			currentSpectrum = new float[spectrumSize];
			previousSpectrum = new float[spectrumSize];

			
			// calculate spectral fluxes
			short[] protoframe = decoder.nextMonoFrame();
			
			while (protoframe.length == 1024) {

				// convert to float
				float[] frame = new float[protoframe.length];
				for (int i = 0; i < frame.length; i++)
					frame[i] = (float) protoframe[i] / 32768f;

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
				
				protoframe = decoder.nextMonoFrame();
			}

			return fluxes;
		}
		/**
		 * Performs onset detection on a set of spectral fluxes

		 * @param sensitivity
		 *            Sensitivity value for threshold function
		 * @return An ArrayList<Float> containing a representation of the audio
		 *         file. There are approx. 43 values for every 1 second of
		 *         audio. All values are zero except where there are beats;
		 *         those values are the original sample values. The higher the
		 *         value the stronger the beat.
		 */
		public static LinkedList<Float> detectPeaks(LinkedList<Float> spectralFluxes, float sensitivity) {

			ArrayList<Float> threshold = new ArrayList<Float>();

			{
				// This next bit calculates the threshold values for a range of
				// ten
				// spectral fluxes. We'll use this later too find onsets.

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

			LinkedList<Float> peaks = new LinkedList<Float>();

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
	 * Detects rhythmic onsets
	 * 
	 * @param decoder Something that returns the samples you want to analyze
	 * in 1024-sample frames
	 * @param sensitivity
	 * @return
	 */
	public static LinkedList<Beat> detectBeats(LwbdDecoder decoder, float sensitivity) {

		LinkedList<Float> spectralFluxes = AudioFunctions.calculateSpectralFluxes(decoder);
		LinkedList<Float> peaks = AudioFunctions.detectPeaks(spectralFluxes, sensitivity);
		LinkedList<Beat> beats = new LinkedList<Beat>();

		// Convert to time - energy map
		LinkedHashMap<Long, Float> timeEnergyMap = new LinkedHashMap<Long, Float>();
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

		// link beats in a time-ordered list
		// TODO: sort by time, just to be safe
		for (Long l : timeEnergyMap.keySet())
			beats.add(new Beat(l, timeEnergyMap.get(l)));

		return beats;
	}

}
