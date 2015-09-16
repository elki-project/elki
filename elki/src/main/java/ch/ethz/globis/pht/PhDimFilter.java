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