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

/**
 * Preprocessor for integer data.
 * 
 * @author ztilmann
 *
 */
public interface PreProcessorRange {
	
	/**
	 * 
	 * @param raw raw data (input)
	 * @param pre pre-processed data (output, must be non-null and same size as input array)
	 */
	public void pre(long[] raw1, long[] raw2, long[] pre);
	
	
	/**
	 * @param pre pre-processed data (input)
	 * @param post post-processed data (output, must be non-null and same size as input array)
	 */
	public void post(long[] pre, long[] post1, long[] post2);
}
