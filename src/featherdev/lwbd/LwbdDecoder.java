package featherdev.lwbd;

public interface LwbdDecoder {
	
	/**
	 * Must return an integer array with 1024 deinterlaced samples
	 * @return
	 */
	public short[] nextMonoFrame();

}
