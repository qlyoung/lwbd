package v4lk.lwbd;

import v4lk.lwbd.decoders.Decoder;
import v4lk.lwbd.decoders.universal.JFlacDecoder;
import v4lk.lwbd.decoders.universal.JLayerMp3Decoder;
import v4lk.lwbd.processing.fft.FFT;
import v4lk.lwbd.util.Beat;

import java.io.*;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.zip.DataFormatException;

/**
 * lwbd -- a portable lightweight beat detector
 * 
 * @author Quentin Young
 */
public class BeatDetector {

	private static class AudioFunctions {
		/**
		 * Calculates the spectral flux value for each sequential pair of
		 * 1024-sample windows in a given audio file
		 * 
		 * @return A list of all calculated spectral fluxes
		 */
		public static LinkedList<Float> calculateSpectralFluxes(Decoder decoder) throws DataFormatException {

			// some collections and objects we'll need
			FFT transformer = new FFT(1024, 44100);
			transformer.window(FFT.HAMMING);
			float[] currentSpectrum, previousSpectrum;
			LinkedList<Float> fluxes = new LinkedList<Float>();
			int spectrumSize = (1024 / 2) + 1;
			currentSpectrum = new float[spectrumSize];
			previousSpectrum = new float[spectrumSize];

			
			// calculate spectral fluxes
            try {
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
            } catch (EOFException e) { }

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
    private static class ProcessingFunctions {
        /**
         * Calculates a time-energy map from a list of peaks.
         * @param peaks an ordered list of peaks in an audio file.
         * @return A time-energy map.
         */
        public static LinkedHashMap<Long, Float> convertToTimeEnergyMap(LinkedList<Float> peaks) {
            // Convert to time - energy map
            LinkedHashMap<Long, Float> timeEnergyMap = new LinkedHashMap<Long, Float>();
            for (int i = 0; i < peaks.size(); i++){
                if (peaks.get(i) > 0) {
                    long timeInMillis = (long) (((float) i * (1024f / 44100f)) * 1000f);
                    timeEnergyMap.put(timeInMillis, peaks.get(i));
                }
            }
            return timeEnergyMap;
        }
        /**
         * Normalizes all values in this hash map to [0, 1]. Does not normalize
         * keys.
         * @param map the map to normalize. This map is not modified.
         * @return The normalized map.
         */
        public static LinkedHashMap<Long, Float> normalizeValues(final LinkedHashMap<Long, Float> map) {
            // normalize values to range [0, 1]
            LinkedHashMap<Long, Float> newMap = new LinkedHashMap<Long, Float>();

            // find max value
            float max = 0;
            for (Float f : map.values())
                if (f > max)
                    max = f;

            // divide all values by max value
            float value;
            for (Long l : newMap.keySet()) {
                value = newMap.get(l);
                value /= max;
                newMap.put(l, value);
            }

            return newMap;
        }
        /**
         * Converts a time energy map to a LinkedList of Beat objects. Convenience function.
         * @param timeEnergyMap ordered time-energy map
         * @return a LinkedList of Beat objects in the same ordering as the parameter map by key.
         */
        public static Beat[] convertToBeatArray(LinkedHashMap<Long, Float> timeEnergyMap) {
            Iterator<Long> iterator = timeEnergyMap.keySet().iterator();

            Beat[] beats = new Beat[timeEnergyMap.size()];
            for(int i = 0; i < beats.length; i++) {
                long time = iterator.next();
                beats[i] = new Beat(iterator.next(), timeEnergyMap.get(time));
            }

            return beats;
        }
    }

    public static enum AudioType { MP3, FLAC }
    public static enum DetectorSensitivity {
        HIGH (1.0f),
        MIDDLING (1.4f),
        LOW (1.7f);

        float value;
        DetectorSensitivity(float value) { this.value = value; }
    }

    /**
     * Perform beat detection on the provided audio data. This method
     * will block until analysis has completed, which can take a while
     * depending on the amount of audio to be analyzed and the capabilities
     * of the hardware.
     *
     * This overload will use the middling sensitivity.
     *
     * @param audio java.io.File object initialized to a file containing
     *              audio data of one of the supported types (i.e. .flac,
     *              .mp3, etc.) Must b
     * @param type one of the supported audio types in the enum AudioType
     *             declared publicly in this class.
     *             @see v4lk.lwbd.BeatDetector.AudioType
     * @return A time-ordered LinkedList of Beat objects.
     *         @see v4lk.lwbd.util.Beat
     *
     * @throws java.io.EOFException if the end of file has been reached or if the stream has corrupted
     * @throws java.util.zip.DataFormatException if the stream is invalid or does not match the specified
     * format
     */
    public static Beat[] detectBeats(InputStream audio, AudioType type) throws EOFException, DataFormatException {
        return detectBeats(audio, type, DetectorSensitivity.MIDDLING);
    }
    /**
     * Perform beat detection on the provided audio data. This method
     * will block until analysis has completed, which can take a while
     * depending on the amount of audio to be analyzed and the capabilities
     * of the hardware.
     *
     * This overload will use the middling sensitivity.
     *
     * @param audio java.io.File object initialized to a file containing
     *              audio data of one of the supported types (i.e. .flac,
     *              .mp3, etc.) Must b
     * @param type one of the supported audio types in the enum AudioType
     *             declared publicly in this class.
     *             @see v4lk.lwbd.BeatDetector.AudioType
     * @return A time-ordered LinkedList of Beat objects.
     *         @see v4lk.lwbd.util.Beat
     *
     * @throws java.io.FileNotFoundException if the file cannot be found
     * @throws java.io.EOFException if the end of file has been reached or if the stream has corrupted
     * @throws java.util.zip.DataFormatException if the stream is invalid or does not match the specified
     * format
     */
    public static Beat[] detectBeats(File audio, AudioType type) throws FileNotFoundException, EOFException, DataFormatException {
        return detectBeats(new FileInputStream(audio), type, DetectorSensitivity.MIDDLING);
    }
    /**
     * Perform beat detection on the provided audio data. This method
     * will block until analysis has completed, which can take a while
     * depending on the amount of audio to be analyzed and the capabilities
     * of the hardware.
     *
     * @param audio java.io.File object initialized to a file containing
     *              audio data of one of the supported types (i.e. .flac,
     *              .mp3, etc.) Must b
     * @param type one of the supported audio types in the enum AudioType
     *             declared publicly in this class.
     *             @see v4lk.lwbd.BeatDetector.AudioType
     * @param sensitivity One of the preset sensitivity constants declared
     *                    publicly in this lass.
     *                    @see v4lk.lwbd.BeatDetector.DetectorSensitivity
     * @return A time-ordered LinkedList of Beat objects.
     *         @see v4lk.lwbd.util.Beat
     *
     * @throws java.io.EOFException if the end of file has been reached or if the stream has corrupted
     * @throws java.util.zip.DataFormatException if the stream is invalid or does not match the specified
     * format
     */
    public static Beat[] detectBeats(InputStream audio,
                                               AudioType type,
                                               DetectorSensitivity sensitivity) throws EOFException, DataFormatException {

        // initialize the appropriate decoder
        Decoder decoder = null;
        switch (type){
            case MP3:
                decoder = new JLayerMp3Decoder(audio);
                break;
            case FLAC:
                try { decoder = new JFlacDecoder(audio); }
                catch (IOException e) { throw new DataFormatException(); }
                break;
        }

        // do beat detection
        LinkedList<Float> spectralFluxes = AudioFunctions.calculateSpectralFluxes(decoder);
        LinkedList<Float> peaks = AudioFunctions.detectPeaks(spectralFluxes, sensitivity.value);
        // do some data transformation
        LinkedHashMap<Long, Float> timeEnergyMap = ProcessingFunctions.convertToTimeEnergyMap(peaks);
        timeEnergyMap = ProcessingFunctions.normalizeValues(timeEnergyMap);


        return ProcessingFunctions.convertToBeatArray(timeEnergyMap);
    }
    /**
     * Perform beat detection on the provided audio data. This method
     * will block until analysis has completed, which can take a while
     * depending on the amount of audio to be analyzed and the capabilities
     * of the hardware.
     *
     * @param audio java.io.File object initialized to a file containing
     *              audio data of one of the supported types (i.e. .flac,
     *              .mp3, etc.) Must b
     * @param type one of the supported audio types in the enum AudioType
     *             declared publicly in this class.
     *             @see v4lk.lwbd.BeatDetector.AudioType
     * @param sensitivity One of the preset sensitivity constants declared
     *                    publicly in this lass.
     *                    @see v4lk.lwbd.BeatDetector.DetectorSensitivity
     * @return A time-ordered LinkedList of Beat objects.
     *         @see v4lk.lwbd.util.Beat
     *
     * @throws java.io.FileNotFoundException if the file cannot be found
     * @throws java.io.EOFException if the end of file has been reached or if the stream has corrupted
     * @throws java.util.zip.DataFormatException if the stream is invalid or does not match the specified
     * format
     */
    public static Beat[] detectBeats(File audio,
                                               AudioType type,
                                               DetectorSensitivity sensitivity) throws FileNotFoundException, EOFException, DataFormatException {
        return detectBeats(new FileInputStream(audio), type, sensitivity);
    }
}
