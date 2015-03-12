package v4lk.lwbd.decoders.universal;

import v4lk.lwbd.decoders.Decoder;
import v4lk.lwbd.processing.jflac.FLACDecoder;
import v4lk.lwbd.processing.jflac.frame.Frame;
import v4lk.lwbd.processing.jflac.metadata.Metadata;
import v4lk.lwbd.processing.jflac.metadata.StreamInfo;

import javax.xml.crypto.Data;
import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.util.LinkedList;
import java.util.Queue;
import java.util.zip.DataFormatException;

/**
 * FLAC decoder for lwbd. Backed by jFLAC.
 * Should be initialized with a single FLAC stream, then passed
 * to BeatDetector.detectBeats().
 */
public class JFlacDecoder implements Decoder {

    /**
     * jFlac decoder class. JFlacDecoder is intended
     * to be a wrapper for this class.
     */
    private FLACDecoder decoder;
    /**
     * Information about the FLAC data stream the decoder
     * is working on.
     */
    private StreamInfo info;
    /**
     * Buffer that will be used to store frames returned from
     * FLACDecoder. Needed because while FLAC's frame sizes are
     * variable, they tend to be large multiples of 1024, so we
     * get more data than we need per frame decode op. The extra
     * is stored here. nextMonoFrame() reads its data exclusively
     * from this buffer.
     */
    private Queue<Short> buffer;

    /**
     * Initialize a new jFlacDecoder with the specified input stream.
     * @param stream a raw binary stream of FLAC data
     * @throws IOException on read error
     * @throws java.io.UnsupportedEncodingException if this FLAC file
     *         has an unsupported sample rate or more than two channels.
     */
    public JFlacDecoder(InputStream stream) throws IOException, DataFormatException {
        // setup decoder
        decoder = new FLACDecoder(stream);
        Metadata[] d = decoder.readMetadata();
        info = (StreamInfo) d[0];

        // check support
        if (info.getChannels() > 2)
            throw new DataFormatException("Number of channels > 2; unsupported");
        if (info.getSampleRate() != 44100)
            throw new DataFormatException("Sample rate is not 44.1kHz; unsupported.");

        // initialize buffer
        buffer = new LinkedList<Short>();
    }

    @Override
    public short[] nextMonoFrame() throws EOFException, DataFormatException{

        if (buffer.size() < 1024)
            fillBuffer();

        // grab samples from the buffer and return them
        short[] frame = new short[1024];
        for (int i = 0; i < frame.length; i++)
            frame[i] = buffer.poll();

        return frame;
    }

    /**
     * Merges stereo audio by averaging channels together
     *
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
    /**
     * Fills internal buffer with at least 1024 frames. Deinterlaces samples
     * before writing to the buffer if we're working with stereo audio.
     *
     * @throws java.util.zip.DataFormatException if the decoder is unable to read a frame.
     * @throws java.io.EOFException if the decoder is out of data.
     */
    private void fillBuffer() throws EOFException, DataFormatException {

        // decode frames and store them in the buffer until the buffer has at least 1024 frames
        // or the decoder runs out of data
        if (decoder.isEOF())
            throw new EOFException("Decoder out of data");

        while (buffer.size() < 1024) {
            try {
                // grab a frame
                Frame encodedFrame = null;
                try { encodedFrame = decoder.readNextFrame(); }
                catch (IOException e) { throw new DataFormatException("Unsupported file or corrupt stream"); }

                byte[] byteFrame = decoder.decodeFrame(encodedFrame, null).getData();

                // cast each byte sample to a short
                short[] shortFrame = new short[byteFrame.length];
                for (int i = 0; i < shortFrame.length; i++)
                    shortFrame[i] = (short) byteFrame[i];

                // merge channels to mono if we're working with stereo
                if (info.getChannels() == 2)
                    shortFrame = mergeChannels(shortFrame);

                // add samples to buffer
                for (short s : shortFrame)
                    buffer.add(s);
            } catch (NullPointerException e) {
                throw new EOFException("Decoder out of data");
            }
        }
    }
}
