/*
 * Copyright 2011-2015 ETH Zurich. All Rights Reserved.
 *
 * This software is the proprietary information of ETH Zurich.
 * Use is subject to license terms.
 */
package ch.ethz.globis.pht;

public class PhTreeConfig {

	private int dimUser;
	private int dimActual;
	private int depth;
	private boolean[] unique; 
	
	public PhTreeConfig(int dim, int depth) {
		this.dimUser = dim;
		this.dimActual = dim;
		this.depth = depth;
		this.unique = new boolean[dimUser];
	}
	
	/**
	 * Mark a dimension as unique
	 * @param dim
	 */
	public void setUnique(int dim) {
		unique[dim] = true;
		dimActual++;
	}
	
	public int getDimActual() {
		return dimActual;
	}
	
	/**
	 * 
	 * @return Dimensionality as defined by user.
	 */
	public int getDim() {
		return dimUser;
	}
	
	/**
	 * 
	 * @return Depth in bits.
	 */
	public int getDepth() {
		return depth;
	}

	public int[] getDimsToSplit() {
		int[] ret = new int[dimActual-dimUser];
		int n = 0;
		for (int i = 0; i < unique.length; i++) {
			if (unique[i]) {
				ret[n] = i;
				n++;
			}
		}
		return ret;
	}
}
