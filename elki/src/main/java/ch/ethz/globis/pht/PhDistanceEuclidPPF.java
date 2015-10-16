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

import ch.ethz.globis.pht.pre.EmptyPPF;
import ch.ethz.globis.pht.pre.PreProcessorPointF;


/**
 * Calculate the euclidean distance for encoded {@code double} values.
 * 
 * @see PhDistance
 * 
 * @author ztilmann
 */
public class PhDistanceEuclidPPF implements PhDistance {

  /** Euclidean distance with standard `double` encoding. */ 
  public static final PhDistance DOUBLE = 
      new PhDistanceEuclidPPF(new EmptyPPF());

  private final PreProcessorPointF pre;

  public PhDistanceEuclidPPF(PreProcessorPointF pre) {
    this.pre = pre;
  }

  /**
   * Calculate the distance for encoded {@code double} values.
   * 
   * @see PhDistance#dist(long[], long[])
   */
  @Override
  public double dist(long[] v1, long[] v2) {
    double d = distEst(v1, v2);
    return Math.sqrt(d);
  }

  /**
   * Calculate an approximate distance for encoded {@code double} values.
   * 
   * @see PhDistance#distEst(long[], long[])
   */
  @Override
  public double distEst(long[] v1, long[] v2) {
    double d = 0;
    double[] d1 = new double[v1.length];
    double[] d2 = new double[v2.length];
    pre.post(v1, d1);
    pre.post(v2, d2);
    for (int i = 0; i < v1.length; i++) {
      double dl = d1[i] - d2[i];
      d += dl*dl;
    }
    return d;
  }
}