package v4lk.lwbd.decoders;

import com.badlogic.gdx.audio.io.VorbisDecoder;
import com.badlogic.gdx.files.FileHandle;
import v4lk.lwbd.decoders.Decoder;

/**
 * Wrapper for Gdx's VorbisDecoder that implements lwbd's decoder interface
 * @author featherdev
 *
 */
public class GdxVorbisDecoder implements Decoder {

    VorbisDecoder decoder;

    public GdxVorbisDecoder(FileHandle f){
        decoder = new VorbisDecoder(f);
    }

    private short[] mergeChannels(short[] samples) {

        int l = (int) Math.floor(samples.length / 2);
        short[] merged = new short[l];

        for (int i = 0; i < l; i++)
            merged[i] = (short) ((samples[i * 2] + samples[i * 2 + 1]) / 2f);

        return merged;
    }

    @Override
    public short[] nextMonoFrame() {
        short[] samples = new short[2048];
        int n = decoder.readSamples(samples, 0, 2048);

        if (n != 2048){
            short[] actual = new short[n];
            System.arraycopy(samples, 0, actual, 0, n);
            return mergeChannels(actual);
        }
        else
            return mergeChannels(samples);
    }

}
