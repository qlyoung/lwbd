package featherdev.lwbd.decoders;

public interface LwbdDecoder {
	
	/**
	 * Must return an integer array with 1024 deinterlaced samples
	 * @return
	 */
	public short[] nextMonoFrame();

}
