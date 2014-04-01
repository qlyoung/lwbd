package featherdev.lwbd.decoders;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.util.PriorityQueue;

import featherdev.lwbd.processing.jlayer.Bitstream;
import featherdev.lwbd.processing.jlayer.BitstreamException;
import featherdev.lwbd.processing.jlayer.Decoder;
import featherdev.lwbd.processing.jlayer.DecoderException;
import featherdev.lwbd.processing.jlayer.Header;
import featherdev.lwbd.processing.jlayer.SampleBuffer;


/**
 * LwbdDecoder implementation backed by JLayer.
 * Supports MP3.
 * 
 * @author featherdev
 */

public class JLayerMp3Decoder implements LwbdDecoder {

	Bitstream bitstream;
	Decoder decoder;
	PriorityQueue<Short> buffer;

	public JLayerMp3Decoder(File audiofile) throws FileNotFoundException {
		this(new FileInputStream(audiofile));
	}
	public JLayerMp3Decoder(InputStream stream) {

		bitstream = new Bitstream(stream);
		decoder = new Decoder();
		buffer =  new PriorityQueue<Short>();
		
		// buffer prime
		fillBuffer();
	}

	private int fillBuffer() {
		buffer.clear();
		
		SampleBuffer samplebuffer;
		int i = 0;
		Header h;
		try {
			h = bitstream.readFrame();
		} catch (BitstreamException e) {
			e.printStackTrace();
			return 0;
		}
		
		while (h != null && i++ < 8){
			try {
				samplebuffer = (SampleBuffer) decoder.decodeFrame(h, bitstream);
			} catch (DecoderException e) {
				e.printStackTrace();
				return buffer.size();
			}
			finally {
				bitstream.closeFrame();
			}

			short[] samples = samplebuffer.getBuffer();
			for (short s : samples)
				buffer.add(s);
			
			try {
				h = bitstream.readFrame();
			} catch (BitstreamException e) {
				e.printStackTrace();
				return buffer.size();
			}
		}
		
		return buffer.size();
	}
	private short[] mergeChannels(short[] samples) {
		
		int l = (int) Math.floor(samples.length / 2);
		short[] merged = new short[l];
		
		for (int i = 0; i < l; i++)
			merged[i] = (short) ((samples[i * 2] + samples[i * 2 + 1]) / 2f);
		
		return merged;
	}

	public short[] nextMonoFrame() {
		if (buffer.isEmpty())
			fillBuffer();
		
		int framesize = buffer.size() >= 2048 ? 2048 : buffer.size();
		short[] frame = new short[framesize];
		
		for (int i = 0; i < frame.length; i++)
			frame[i] = buffer.poll();
		
		return mergeChannels(frame);
	}

}
