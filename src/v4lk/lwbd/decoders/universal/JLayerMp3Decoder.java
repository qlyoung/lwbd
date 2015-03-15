package v4lk.lwbd.decoders.universal;

import v4lk.lwbd.processing.jlayer.*;
import v4lk.lwbd.processing.jlayer.Decoder;

import java.io.IOException;
import java.io.InputStream;
import java.util.LinkedList;
import java.util.Queue;


/**
 * Decoder implementation backed by JLayer.
 * Supports MP3.
 * 
 * @author Quentin Young
 */
public class JLayerMp3Decoder implements v4lk.lwbd.decoders.Decoder {

	Bitstream bitstream;
	Decoder decoder;
	Queue<Short> buffer;

	public JLayerMp3Decoder(InputStream stream) {
		bitstream = new Bitstream(stream);
		decoder = new Decoder();
		buffer =  new LinkedList<Short>();
	}

    public short[] nextMonoFrame() throws IOException {
        if (buffer.isEmpty())
            fillBuffer();

        if (buffer.size() < 2048)
            return null;

        short[] frame = new short[2048];

        for (int i = 0; i < frame.length; i++)
            frame[i] = buffer.poll();

        return mergeChannels(frame);
    }

	private void fillBuffer() throws IOException {
		SampleBuffer samplebuffer;
		int i = 0;
		Header h;
		try { h = bitstream.readFrame(); }
        catch (BitstreamException e) { throw new IOException("Decoder error"); }
		
		while (h != null && i++ < 8) {
			try { samplebuffer = (SampleBuffer) decoder.decodeFrame(h, bitstream); }
            catch (DecoderException e) { throw new IOException("Decoder error"); }
			finally { bitstream.closeFrame(); }

			short[] samples = samplebuffer.getBuffer();
			for (short s : samples)
				buffer.add(s);
			
			try { h = bitstream.readFrame(); }
            catch (BitstreamException e) { throw new IOException("Decoder error"); }
		}
	}
	private short[] mergeChannels(short[] samples) {
		
		int l = (int) Math.floor(samples.length / 2);
		short[] merged = new short[l];
		
		for (int i = 0; i < l; i++)
			merged[i] = (short) ((samples[i * 2] + samples[i * 2 + 1]) / 2f);
		
		return merged;
	}
}
