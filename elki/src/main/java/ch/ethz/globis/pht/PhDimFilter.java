/*
 * Copyright 2011-2015 ETH Zurich. All Rights Reserved.
 *
 * This software is the proprietary information of ETH Zurich.
 * Use is subject to license terms.
 */
package ch.ethz.globis.pht;

/**
 * Instances of this class can be used to specify which dimensions should be considered during
 * nearest neighbor queries.
 * 
 * For dimensions that are ignored (not considered), any values are possible for resulting points.
 * Of course the setting in this class should be compatible with the distance calculation provided
 * in an PhDIstance instance that is used alongside this class.
 * 
 * @author ztilmann
 */
public class PhDimFilter {
	
	private long constraints = -1L; //0xFFFF...
	
	/**
	 * Creates a new DIM filter instance. By default all dimensions are constrained.
	 */
	public PhDimFilter() {
		// 
	}
	
	/**
	 * Flag a dimension to be ignored. Dimensions are numbered dim = [0..(k-1)].
	 * @param dim
	 */
	public void ignoreDimension(int dim) {
		constraints &= ~(1L << dim);
	}
	
	protected long getConstraints() {
		return constraints;
	}
}