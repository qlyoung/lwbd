package v4lk.lwbd.decoders;

import java.io.EOFException;
import java.io.IOException;
import java.util.zip.DataFormatException;

/**
 * Defines the interface of a lwbd-compatible Decoder.
 */
public interface Decoder {
	
	/**
     * Get the next frame of audio data that this decoder has decoded.
	 * @return a short[] with 1024 non-normalized mono PCM samples, or null
     * if there is no more data available.
     * @throws java.io.IOException on read error.
	 */
	public short[] nextMonoFrame() throws IOException;

}
