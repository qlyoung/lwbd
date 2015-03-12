package v4lk.lwbd.decoders;

import java.io.EOFException;
import java.util.zip.DataFormatException;

/**
 * Defines the interface of a lwbd-compliant Decoder.
 */
public interface Decoder {
	
	/**
     * Get the next frame of audo data that this decoder has decoded.
	 * @return a short[] with 1024 non-normalized mono PCM samples.
     * @throws java.io.EOFException if there is no more data to return
     * @throws java.io.IOError on read error
	 */
	public short[] nextMonoFrame() throws EOFException, DataFormatException;

}
