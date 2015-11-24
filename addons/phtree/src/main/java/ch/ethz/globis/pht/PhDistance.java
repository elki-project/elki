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
   * Returns the distance between v1 and v2.
   * 
   * @param v1
   * @param v2
   * @return The distance.
   */
  double dist(long[] v1, long[] v2);

  /**
   * Calculate the minimum bounding box for all points that are less than 
   * {@code distance} away from {@code center}.
   * @param distance
   * @param center
   * @param outMin
   * @param outMax
   */
  void toMBB(double distance, long[] center, long[] outMin, long[] outMax);
}