/*
 * Copyright 2011-2015 ETH Zurich. All Rights Reserved.
 *
 * This software is the proprietary information of ETH Zurich.
 * Use is subject to license terms.
 */
package ch.ethz.globis.pht.pre;

public abstract class ColumnType {
	private int depth;
	
	public ColumnType(int depth) {
		if (depth < 1 || depth > 64)
			throw new IllegalArgumentException("Parameter 'depth': value must lie within [1, 64]");
		
		this.depth = depth;
	}
	
	public int getDepth() {
		return depth;
	}
		
	
	// ===== Integral Types =====
	
	public static class IntColumn extends ColumnType {
		
		public IntColumn() {
			super(Integer.SIZE);
		}
		
	}
	
	public static class LongColumn extends ColumnType {
		
		public LongColumn() {
			super(Long.SIZE);
		}
		
	}
	
	// ===== Floating Point Types =====
	
	public static class FloatColumn extends ColumnType {
		private int digits;
		
		public FloatColumn(int digits) {
			super(Float.SIZE);
			this.digits = digits;
		}
				
		public int getDigits() {
			return digits;
		}
	}
	
	public static class DoubleColumn extends ColumnType {
		private int digits;
		
		public DoubleColumn(int digits) {
			super(Double.SIZE);
			this.digits = digits;
		}
		
		public int getDigits() {
			return digits;
		}
	}
}