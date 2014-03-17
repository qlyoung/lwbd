package featherdev.lwbd;

import java.io.InputStream;
import java.util.PriorityQueue;

import javazoom.jl.decoder.Bitstream;
import javazoom.jl.decoder.BitstreamException;
import javazoom.jl.decoder.Decoder;
import javazoom.jl.decoder.DecoderException;
import javazoom.jl.decoder.Header;
import javazoom.jl.decoder.SampleBuffer;


/**
 * Wrapper for JLayer that returns 1024 sample frames instead of JLayer's 2304,
 * because it's easier to write this class than change all my magic numbers.
 * 
 * @author featherdev
 */

public class SimpleDecoder {

	Bitstream bitstream;
	Decoder decoder;
	PriorityQueue<Short> buffer;

	public SimpleDecoder(InputStream stream) throws DecoderException,
			BitstreamException {

		bitstream = new Bitstream(stream);
		decoder = new Decoder();
		buffer =  new PriorityQueue<Short>();
		
		// buffer prime
		fillBuffer();
	}

	/**
	 * Merges dual-channel PCM data into single-channel by averaging.
	 * 
	 * @param samples
	 * An integer array containing interlaced samples. E.G. first
	 * element is from left channel, second element is from right
	 * channel, third is from left, fourth from right, etc.
	 * Array size must be even, or you get null.
	 * 
	 * @return
	 * An integer array containing the merged data. It is exactly 
	 * half the size of the array given it.
	 */
	private int[] mergeChannels(int[] samples) {
		
		int numMergedSamples = (int) (samples.length / 2f);
		int[] merged = new int[numMergedSamples];

		int unmergedIndex = 0, mergedIndex = 0;

		for (; unmergedIndex < samples.length; mergedIndex++) {
			float average = (samples[unmergedIndex] + samples[unmergedIndex + 1]) / 2f;
			merged[mergedIndex] = Math.round(average);
			
			unmergedIndex += 2;
		}

		return merged;
	}
	/**
	 * fills the sample buffer with up to 9 frames
	 * @throws DecoderException
	 * @throws BitstreamException
	 */
	private int fillBuffer() throws DecoderException, BitstreamException {
		buffer.clear();
		
		SampleBuffer samplebuffer;
		int i = 0;
		Header h = bitstream.readFrame();
		
		while (h != null && i++ < 8){
			samplebuffer = (SampleBuffer) decoder.decodeFrame(h, bitstream);
			bitstream.closeFrame();

			short[] samples = samplebuffer.getBuffer();
			for (short s : samples)
				buffer.add(s);
			
			h = bitstream.readFrame();
		}
		
		return buffer.size();
	}
	public int[] nextInterlacedFrame() throws DecoderException, BitstreamException {

		if (buffer.isEmpty())
			fillBuffer();
		
		int framesize = buffer.size() >= 2048 ? 2048 : buffer.size();
		int[] frame = new int[framesize];
		
		for (int i = 0; i < frame.length; i++)
			frame[i] = buffer.poll();
		
		return frame;
	}
	public int[] nextDeinterlacedFrame() throws DecoderException, BitstreamException {
		return (mergeChannels(nextInterlacedFrame()));
	}

}
