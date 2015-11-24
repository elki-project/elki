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
 * Check distance to a point using a distance function.
 * 
 * @author Tilmann Zäschke
 *
 */
public class PhFilterDistance implements PhFilter {

  private long[] v;
  private PhDistance dist;
  private double maxDist;

  public void set(long[] v, PhDistance dist, double maxDist) {
    this.v = v;
    this.dist = dist;
    this.maxDist = maxDist;
  }

  @Override
  public boolean isValid(long[] key) {
    return dist.dist(v, key) <= maxDist;
  }

  @Override
  public boolean isValid(int bitsToIgnore, long[] prefix) {
    long maskMin = (-1L) << bitsToIgnore;
    long maskMax = ~maskMin;
    long[] buf = new long[prefix.length];
    for (int i = 0; i < buf.length; i++) {
      //if v is outside the node, return distance to closest edge,
      //otherwise return v itself (assume possible distance=0)
      long min = prefix[i] & maskMin;
      long max = prefix[i] | maskMax;
      buf[i] = min > v[i] ? min : (max < v[i] ? max : v[i]); 
    }
    return dist.dist(v, buf) <= maxDist;
  }

}
