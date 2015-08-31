/*
 * Copyright 2011-2015 ETH Zurich. All Rights Reserved.
 *
 * This software is the proprietary information of ETH Zurich.
 * Use is subject to license terms.
 */
package ch.ethz.globis.pht;

import ch.ethz.globis.pht.util.BitTools;


/**
 * Calculate the distance for encoded {@code double} values.
 * 
 * @see PhDistance
 * 
 * @author ztilmann
 */
public class PhDistanceD implements PhDistance {

	/**
	 * Calculate the distance for encoded {@code double} values.
	 * 
	 * @see PhDistance#dist(long[], long[])
	 */
	@Override
	public double dist(long[] v1, long[] v2) {
		double d = 0;
		for (int i = 0; i < v1.length; i++) {
			double dl = BitTools.toDouble(v1[i]) - BitTools.toDouble(v2[i]);
			d += dl*dl;
		}
		return Math.sqrt(d);
	}
}