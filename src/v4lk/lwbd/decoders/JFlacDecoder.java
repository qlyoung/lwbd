package v4lk.lwbd.decoders;

import v4lk.lwbd.decoders.Decoder;
import v4lk.lwbd.decoders.processing.jflac.FLACDecoder;
import v4lk.lwbd.decoders.processing.jflac.frame.Frame;
import v4lk.lwbd.decoders.processing.jflac.metadata.Metadata;
import v4lk.lwbd.decoders.processing.jflac.metadata.StreamInfo;
import v4lk.lwbd.decoders.processing.jflac.util.ByteData;

import java.io.IOException;
import java.io.InputStream;
import java.util.LinkedList;
import java.util.Queue;

/**
 * FLAC decoder for lwbd. Backed by jFLAC.
 * @author Quentin Young
 */
public class JFlacDecoder implements Decoder {

    /**
     * jFlac decoder class
     */
    private FLACDecoder decoder;
    /**
     * Information about the FLAC data stream the decoder
     * is working on.
     */
    private StreamInfo info;
    /**
     * decoded mono sample buffer
     */
    private Queue<Short> buffer;

    /**
     * Initialize this decoder
     * @param stream binary FLAC input stream
     * @throws IOException on decoder error
     */
    public JFlacDecoder(InputStream stream) throws IOException {
        // setup decoder
        decoder = new FLACDecoder(stream);
        Metadata[] d = decoder.readMetadata();
        info = (StreamInfo) d[0];

        // check support
        if (info.getChannels() > 2)
            throw new IOException("Number of channels > 2; unsupported");
        if (info.getSampleRate() != 44100)
            throw new IOException("Sample rate is not 44.1kHz; unsupported.");

        // initialize buffer
        buffer = new LinkedList<Short>();
    }

    @Override
    public short[] nextMonoFrame() throws IOException {

        if (buffer.size() < 1024)
            fillBuffer();

        if (buffer.size() < 1024)
            return null;

        // grab samples from the buffer and return them
        short[] frame = new short[1024];
        for (int i = 0; i < frame.length; i++)
            frame[i] = buffer.poll();

        System.out.println(frame.length);

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
                Frame encodedFrame = decoder.readNextFrame();
                ByteData d = decoder.decodeFrame(encodedFrame, null);

                /* ByteData has a larger capacity than the data it contains. getLen()
                * doesn't return the capacity, it returns the number of valid elements
                * in the collection. The rest of the values are initialized to 0, so
                * you can't do a foreach because you'll read out all those as well. */
                byte[] untrimmedByteFrame = d.getData();
                byte[] byteFrame = new byte[d.getLen()];
                for (int i = 0; i < d.getLen(); i++)
                    byteFrame[i] = untrimmedByteFrame[i];

                // convert byte[] to short[]
                short[] shortFrame = new short[byteFrame.length];
                for (int i = 0; i < shortFrame.length; i++)
                    shortFrame[i] = (short) byteFrame[i];

                // merge channels to mono if we're working with stereo
                if (info.getChannels() == 2)
                    shortFrame = mergeChannels(shortFrame);

                // add samples to buffer
                for (short s : shortFrame)
                    buffer.add(s);

            } catch (NullPointerException e) { return; }
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