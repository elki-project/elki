package ch.ethz.globis.pht;

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
