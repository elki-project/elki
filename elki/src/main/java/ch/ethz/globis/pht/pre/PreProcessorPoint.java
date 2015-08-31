package ch.ethz.globis.pht.pre;

public interface PreProcessorPoint {
	
	/**
	 * 
	 * @param raw raw data (input)
	 * @param pre pre-processed data (output, must be non-null and same size as input array)
	 */
	public void pre(double[] raw, long[] pre);
	
	
	/**
	 * @param pre pre-processed data (input)
	 * @param post post-processed data (output, must be non-null and same size as input array)
	 */
	public void post(long[] pre, double[] post);
}
