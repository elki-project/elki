/*
 * Copyright 2011-2015 ETH Zurich. All Rights Reserved.
 *
 * This software is the proprietary information of ETH Zurich.
 * Use is subject to license terms.
 */
package ch.ethz.globis.pht.pre;

/**
 * Preprocessor for integer data.
 * 
 * @author ztilmann
 *
 */
public interface PreProcessorRange {
	
	/**
	 * 
	 * @param raw raw data (input)
	 * @param pre pre-processed data (output, must be non-null and same size as input array)
	 */
	public void pre(long[] raw1, long[] raw2, long[] pre);
	
	
	/**
	 * @param pre pre-processed data (input)
	 * @param post post-processed data (output, must be non-null and same size as input array)
	 */
	public void post(long[] pre, long[] post1, long[] post2);
}
