package v4lk.lwbd.decoders.universal;

import v4lk.lwbd.decoders.Decoder;
import v4lk.lwbd.processing.jlayer.*;

import java.io.EOFException;
import java.io.InputStream;
import java.util.LinkedList;
import java.util.Queue;
import java.util.zip.DataFormatException;


/**
 * MP3 decoder for lwbd. Backed by JLayer.
 */

public class JLayerMp3Decoder implements Decoder {

    /**
     * MP3 bitstream.
     */
	private Bitstream bitstream;
    /**
     * JLayer decoder.
     */
	private v4lk.lwbd.processing.jlayer.Decoder decoder;
    /**
     * Buffer that will be used to store frames returned from
     * FLACDecoder. Needed because while FLAC's frame sizes are
     * variable, they tend to be large multiples of 1024, so we
     * get more data than we need per frame decode op. The extra
     * is stored here. nextMonoFrame() reads its data exclusively
     * from this buffer.
     */
    private Queue<Short> buffer;

	public JLayerMp3Decoder(InputStream stream) {
		bitstream = new Bitstream(stream);
		decoder = new v4lk.lwbd.processing.jlayer.Decoder();
		buffer =  new LinkedList<Short>();
	}

    @Override
    public short[] nextMonoFrame() throws EOFException {
        if (buffer.isEmpty())
            fillBuffer();

        short[] frame = new short[1024];
        for (int i = 0; i < frame.length; i++)
            frame[i] = buffer.poll();

        return frame;
    }

	private void fillBuffer() throws EOFException {
		while (buffer.size() < 1024){
            SampleBuffer sampleBuffer = null;
            try {
                // get the header of the next frame in the bitstream
                Header header = bitstream.readFrame();
                // decode the frame in the bitstream specified by the header
                sampleBuffer = (SampleBuffer) decoder.decodeFrame(header, bitstream);
            }
            catch (BitstreamException e) { throw new EOFException("unable to read header; eof"); }
            catch (DecoderException e) { throw new EOFException("unable to decode frame; eof"); }
            finally { bitstream.closeFrame(); }

			short[] samples = sampleBuffer.getBuffer();

            // convert to mono if necessary
            if (decoder.getOutputChannels() == 2)
                samples = mergeChannels(samples);

            // add decoded samples to buffer
			for (short s : samples)
				buffer.add(s);
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
