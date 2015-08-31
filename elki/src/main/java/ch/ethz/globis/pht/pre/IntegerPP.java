/*
 * Copyright 2011-2015 ETH Zurich. All Rights Reserved.
 *
 * This software is the proprietary information of ETH Zurich.
 * Use is subject to license terms.
 */
package ch.ethz.globis.pht.pre;


public class IntegerPP implements PreProcessorPoint {

	private final double preMult;
	private final double postMult;
	
	public IntegerPP(double multiplyer) {
		preMult = multiplyer;
		postMult = 1./multiplyer;
	}
	
	@Override
	public void pre(double[] raw, long[] pre) {
		for (int d=0; d<raw.length; d++) {
			pre[d] = (long) (raw[d] * preMult);
		}
	}

	@Override
	public void post(long[] pre, double[] post) {
		for (int d=0; d<pre.length; d++) {
			post[d] = pre[d] * postMult;
		}
	}

}
