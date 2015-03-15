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
		public static LinkedList<Float> calculateSpectralFluxes(Decoder decoder) throws IOException {

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

            while (protoframe != null && protoframe.length == 1024) {
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

			ArrayList<Float> thresholds = new ArrayList<Float>();

			// calculate an energy threshold for each flux using a moving window of size 10
			for (int i = 0; i < spectralFluxes.size(); i++) {
				int start = Math.max(0, i - 10);
				int end = Math.min(spectralFluxes.size() - 1, i + 10);
				float mean = 0;
				for (int j = start; j <= end; j++)
				mean += spectralFluxes.get(j);
				mean /= (end - start);
				thresholds.add(mean * sensitivity);
			}

            // zero out non-beats and keep the beats
			ArrayList<Float> prunedSpectralFluxes = new ArrayList<Float>();
			for (int i = 0; i < thresholds.size(); i++) {
                float flux = spectralFluxes.get(i);
                float threshold = thresholds.get(i);
                float value = flux >= threshold ? flux - threshold : 0;
                prunedSpectralFluxes.add(value);
			}

            // condense millisecond-consecutive beats to a single beat
			LinkedList<Float> peaks = new LinkedList<Float>();
			for (int i = 0; i < prunedSpectralFluxes.size() - 1; i++) {
                float flux = prunedSpectralFluxes.get(i);
                float nextflux = prunedSpectralFluxes.get(i + 1);
                float value = flux > nextflux ? flux : 0;
                peaks.add(value);
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
            for (Long l : map.keySet()) {
                value = map.get(l);
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

            System.out.println(timeEnergyMap.size() + " " + timeEnergyMap.keySet().size());

            Beat[] beats = new Beat[timeEnergyMap.size()];
            for(int i = 0; i < beats.length; i++) {
                long time = iterator.next();
                beats[i] = new Beat(time, timeEnergyMap.get(time));
            }

            return beats;
        }
    }
    public static class UnsupportedPlatformException extends Exception {}

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
     * This overload will use the default sensitivity (DetectorSensitivity.MIDDLING).
     *
     * @param audio InputStream of encoded audio data corresponding to one of
     *              the supported types (i.e. .flac, .mp3, etc.)
     * @param type an AudioType indicating the format of the audio
     *             @see v4lk.lwbd.BeatDetector.AudioType
     *
     * @return A time-ordered LinkedList of Beat objects.
     *         @see v4lk.lwbd.util.Beat
     *
     * @throws java.io.IOException on read error. Possibilities include a corrupted
     * or closed stream, invalid data, or a mismatch between the audio format
     * indicated by type parameter and the actual data.
     * @throws v4lk.lwbd.BeatDetector.UnsupportedPlatformException if the indicated
     * format is not supported on this platform.
     */
    public static Beat[] detectBeats(InputStream audio, AudioType type) throws IOException, UnsupportedPlatformException {
        return detectBeats(audio, type, DetectorSensitivity.MIDDLING);
    }
    /**
     * Perform beat detection on the provided audio data. This method
     * will block until analysis has completed, which can take a while
     * depending on the amount of audio to be analyzed and the capabilities
     * of the hardware.
     *
     * This overload will use the default sensitivity (DetectorSensitivity.MIDDLING).
     *
     * @param audio File of encoded audio data corresponding to one of
     *              the supported types (i.e. .flac, .mp3, etc.)
     * @param type an AudioType indicating the format of the audio
     *             @see v4lk.lwbd.BeatDetector.AudioType
=
     * @return A time-ordered LinkedList of Beat objects.
     *         @see v4lk.lwbd.util.Beat
     *
     * @throws java.io.IOException on read error. Possibilities include a corrupted
     * or closed stream, invalid data, a mismatch between the audio format
     * indicated by type parameter and the actual data, or an invalid File object.
     * @throws v4lk.lwbd.BeatDetector.UnsupportedPlatformException if the indicated
     * format is not supported on this platform.
     */
    public static Beat[] detectBeats(File audio, AudioType type) throws IOException, UnsupportedPlatformException {
        return detectBeats(new FileInputStream(audio), type, DetectorSensitivity.MIDDLING);
    }
    /**
     * Perform beat detection on the provided audio data. This method
     * will block until analysis has completed, which can take a while
     * depending on the amount of audio to be analyzed and the capabilities
     * of the hardware.
     *
     * @param audio InputStream of encoded audio data corresponding to one of
     *              the supported types (i.e. .flac, .mp3, etc.)
     * @param type an AudioType indicating the format of the audio
     *             @see v4lk.lwbd.BeatDetector.AudioType
     * @param sensitivity a DetectorSensitivity indicating the detector's
     *                    propensity to see beats in data
     *                    @see v4lk.lwbd.BeatDetector.DetectorSensitivity
     *
     * @return A time-ordered LinkedList of Beat objects.
     *         @see v4lk.lwbd.util.Beat
     *
     * @throws java.io.IOException on read error. Possibilities include a corrupted
     * or closed stream, invalid data, or a mismatch between the audio format
     * indicated by type parameter and the actual data.
     * @throws v4lk.lwbd.BeatDetector.UnsupportedPlatformException if the indicated
     * format is not supported on this platform.
     */
    public static Beat[] detectBeats(InputStream audio,
                                               AudioType type,
                                               DetectorSensitivity sensitivity) throws IOException, UnsupportedPlatformException {

        // initialize the appropriate decoder
        Decoder decoder;
        switch (type){
            case MP3:
                decoder = new JLayerMp3Decoder(audio);
                break;
            case FLAC:
                decoder = new JFlacDecoder(audio);
                break;
            default:
                throw new UnsupportedPlatformException();
        }

        // do beat detection
        LinkedList<Float> spectralFluxes = AudioFunctions.calculateSpectralFluxes(decoder);
        System.out.println(spectralFluxes.size());
        LinkedList<Float> peaks = AudioFunctions.detectPeaks(spectralFluxes, sensitivity.value);
        System.out.println(peaks.size());
        // do some data transformation
        LinkedHashMap<Long, Float> timeEnergyMap = ProcessingFunctions.convertToTimeEnergyMap(peaks);
        System.out.println(timeEnergyMap.size());
        timeEnergyMap = ProcessingFunctions.normalizeValues(timeEnergyMap);
        System.out.println(timeEnergyMap.size());
        return ProcessingFunctions.convertToBeatArray(timeEnergyMap);
    }
    /**
     * Perform beat detection on the provided audio data. This method
     * will block until analysis has completed, which can take a while
     * depending on the amount of audio to be analyzed and the capabilities
     * of the hardware.
     *
     * @param audio File of encoded audio data corresponding to one of
     *              the supported types (i.e. .flac, .mp3, etc.)
     * @param type an AudioType indicating the format of the audio
     *             @see v4lk.lwbd.BeatDetector.AudioType
     * @param sensitivity a DetectorSensitivity indicating the detector's
     *                    propensity to see beats in data
     *                    @see v4lk.lwbd.BeatDetector.DetectorSensitivity
     *
     * @return A time-ordered LinkedList of Beat objects.
     *         @see v4lk.lwbd.util.Beat
     *
     * @throws java.io.IOException on read error. Possibilities include a corrupted
     * or closed stream, invalid data, a mismatch between the audio format
     * indicated by type parameter and the actual data, or an invalid File object.
     * @throws v4lk.lwbd.BeatDetector.UnsupportedPlatformException if the indicated
     * format is not supported on this platform.
     */
    public static Beat[] detectBeats(File audio,
                                               AudioType type,
                                               DetectorSensitivity sensitivity) throws IOException, UnsupportedPlatformException {
        return detectBeats(new FileInputStream(audio), type, sensitivity);
    }
}
