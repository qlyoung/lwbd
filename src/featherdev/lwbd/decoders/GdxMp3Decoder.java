package featherdev.lwbd.decoders;

import com.badlogic.gdx.audio.io.Mpg123Decoder;
import com.badlogic.gdx.files.FileHandle;

import featherdev.lwbd.LwbdDecoder;

/**
 * Wrapper for Gdx's Mpg123Decoder that implements lwbd's decoder interface
 * @author featherdev
 *
 */
public class GdxMp3Decoder implements LwbdDecoder {

	Mpg123Decoder decoder;
	
	public GdxMp3Decoder(FileHandle f){
		decoder = new Mpg123Decoder(f);
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
