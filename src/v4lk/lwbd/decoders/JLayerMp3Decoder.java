package v4lk.lwbd.decoders;

import v4lk.lwbd.decoders.processing.jlayer.*;

import java.io.IOException;
import java.io.InputStream;
import java.util.LinkedList;
import java.util.Queue;

/**
 * MP3 decoder for lwbd. Backed by JLayer.
 * @author Quentin Young
 */
public class JLayerMp3Decoder implements v4lk.lwbd.decoders.Decoder {

    /**
     * JLayer decoder class
     */
    private v4lk.lwbd.decoders.processing.jlayer.Decoder decoder;
    /**
     * MP3 input stream
     */
    private Bitstream bitstream;
    /**
     * decoded mono sample buffer
     */
    private Queue<Short> buffer;

    /**
     * Initialize this decoder
     * @param stream binary MP3 input stream
     * @throws IOException on decoder error
     */
    public JLayerMp3Decoder(InputStream stream) {
        bitstream = new Bitstream(stream);
        decoder = new v4lk.lwbd.decoders.processing.jlayer.Decoder();
        buffer =  new LinkedList<Short>();
    }

    @Override
    public short[] nextMonoFrame() throws IOException {

        if (buffer.size() < 1024)
            fillBuffer();

        if (buffer.size() < 1024)
            return null;

        short[] frame = new short[1024];

        for (int i = 0; i < frame.length; i++)
            frame[i] = buffer.poll();

        return frame;
    }

    /**
     * Fills buffer with mono PCM samples as much as it can. Best-effort.
     * @throws IOException on decoder error
     */
    private void fillBuffer() throws IOException {

        while (buffer.size() < 1024) {
            try {
                // get & decode a frame
                Header h = bitstream.readFrame();
                if (h == null) // EoF, return
                    return;
                SampleBuffer samplebuffer = (SampleBuffer) decoder.decodeFrame(h, bitstream);
                short[] samples = samplebuffer.getBuffer();

                // merge channels to mono if we're working with stereo
                if (decoder.getOutputChannels() == 2)
                    samples = mergeChannels(samples);

                // add samples to buffer
                for (short s : samples)
                    buffer.add(s);
            } catch (DecoderException e) { throw new IOException("Decoder error", e);
            } catch (BitstreamException e) { throw new IOException("Decoder error", e);
            } finally {
                bitstream.closeFrame();
            }
        }
    }
    /**
     * Merges stereo audio by averaging channels together
     * @param samples interlaced stereo sample buffer
     * @return mono sample buffer
     */
    private short[] mergeChannels(short[] samples) {

        int l = (int) Math.floor(samples.length / 2);
        short[] merged = new short[l];

        for (int i = 0; i < l; i++)
            merged[i] = (short) ((samples[i * 2] + samples[i * 2 + 1]) / 2f);

        return merged;
    }
}