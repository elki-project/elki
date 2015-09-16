package ch.ethz.globis.pht.pre;

/*
This file is part of ELKI:
Environment for Developing KDD-Applications Supported by Index-Structures

Copyright (C) 2011-2015
Eidgenössische Technische Hochschule Zürich (ETH Zurich)
Institute for Information Systems
GlobIS Group

This program is free software: you can redistribute it and/or modify
it under the terms of the GNU Affero General Public License as published by
the Free Software Foundation, either version 3 of the License, or
(at your option) any later version.

This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU Affero General Public License for more details.

You should have received a copy of the GNU Affero General Public License
along with this program.  If not, see <http://www.gnu.org/licenses/>.
*/

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