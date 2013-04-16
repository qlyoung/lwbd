package com.albadev.beatdetect;

import java.io.InputStream;
import java.util.ArrayList;

import javazoom.jl.decoder.Bitstream;
import javazoom.jl.decoder.BitstreamException;
import javazoom.jl.decoder.Decoder;
import javazoom.jl.decoder.DecoderException;
import javazoom.jl.decoder.SampleBuffer;


/**
 * Wrapper for JLayer that returns 1024 sample frames instead of JLayer's 2304,
 * because it's easier to write this class than change all my magic numbers.
 * 
 * @author albatross
 */

public class SimpleDecoder {

	private Decoder decoder;
	private ArrayList<Short> nineInterlacedFrames;
	private int index;
	public boolean hitEnd = false;

	Bitstream bitstream;

	public SimpleDecoder(InputStream stream) throws DecoderException,
			BitstreamException {

		bitstream = new Bitstream(stream);

		decoder = new Decoder();
		nineInterlacedFrames = new ArrayList<Short>();
		index = 0;

		// PRIME THE BUFFER, LIEUTENANT
		fillNextNineFrames();
	}

	public int[] getNextInterlacedFrame() throws DecoderException, BitstreamException {
		
		if (hitEnd)
			return null;

		int[] samples = new int[2048];

		for (int i = 0; i < 2048; i++) {
			samples[i] = nineInterlacedFrames.get(index);
			index++;
		}

		if (index == nineInterlacedFrames.size())
			fillNextNineFrames();

		return samples;
	}

	public int[] getNextDeinterlacedFrame() throws DecoderException,
			BitstreamException {
		return (mergeChannels(getNextInterlacedFrame()));
	}

	private void fillNextNineFrames() throws DecoderException,
			BitstreamException {
		SampleBuffer b;
		nineInterlacedFrames.clear();

		for (int i = 0; i < 8; i++) {
			try {
				b = (SampleBuffer) decoder.decodeFrame(bitstream.readFrame(),
						bitstream);
				bitstream.closeFrame();

				short[] samples = b.getBuffer();

				for (short s : samples)
					nineInterlacedFrames.add(s);

			} catch (Exception e) {
				hitEnd = true;
			}
		}
		// check to see if we got 9 full frames
		if (nineInterlacedFrames.size() != 2304 * 8)
			hitEnd = true;

		// reset the index
		index = 0;
	}

	/**
	 * Merges double channel PCM data into single-channel by averaging.
	 * 
	 * @param samples
	 * An integer array containing interlaced samples. E.G. first
	 * element is from left channel, second element is from right
	 * channel, third is from left, fourth from right, etc. Don't pass
	 * an odd-sized array to it unless. That would be stupid.
	 * 
	 * @return
	 * An integer array containing the merged data. It is exactly 
	 * half the size of the array given it.
	 */
	private int[] mergeChannels(int[] samples) {

		int[] merged = new int[(int) (samples.length / 2)];

		int unmergedIndex = 0, mergedIndex = 0;

		for (; unmergedIndex < samples.length; mergedIndex++) {
			merged[mergedIndex] = (samples[unmergedIndex] + samples[unmergedIndex + 1]) / 2;
			unmergedIndex += 2;
		}

		return merged;
	}

}
