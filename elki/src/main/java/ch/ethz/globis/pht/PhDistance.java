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