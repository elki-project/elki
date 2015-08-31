/*
 * Copyright 2011-2015 ETH Zurich. All Rights Reserved.
 *
 * This software is the proprietary information of ETH Zurich.
 * Use is subject to license terms.
 */
package ch.ethz.globis.pht;

/**
 * Distance method for the PhTree, for example used in nearest neighbor queries.
 * 
 * @author ztilmann
 */
public interface PhDistance {
	
	/**
	 * Returns a measurement for the distance. The returned distance does not need to have
	 * euclidean properties. For example, for 2D coordinate distance, it is sufficient to return
	 * d = x1*x1 + x2*x2, without applying square-root function.
	 * 
	 * The only requirement is that if (and only if) d1 > d2 then d1 should always indicate a bigger
	 * distance, while d1=d2 should always indicate equal distance.
	 * 
	 * Depending on the dataset it may help if (d1=2*d2) really indicates approximately
	 * double distance in real terms.
	 * 
	 * @param v1
	 * @param v2
	 * @return A measurement for the distance.
	 */
	double dist(long[] v1, long[] v2);
}