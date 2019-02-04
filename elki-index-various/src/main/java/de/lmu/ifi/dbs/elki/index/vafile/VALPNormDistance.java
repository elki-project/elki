/*
 * This file is part of ELKI:
 * Environment for Developing KDD-Applications Supported by Index-Structures
 *
 * Copyright (C) 2019
 * ELKI Development Team
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package de.lmu.ifi.dbs.elki.index.vafile;

import de.lmu.ifi.dbs.elki.data.NumberVector;
import net.jafama.FastMath;

/**
 * Lp-Norm distance function for partially computed objects.
 * 
 * @author Erich Schubert
 * @since 0.5.0
 */
public class VALPNormDistance {
  /**
   * Value of 1/p for lP norm.
   */
  private final double onebyp;

  /**
   * Lookup table for grid cells.
   */
  private double[][] lookup;

  /**
   * Approximation of the query vector.
   */
  private VectorApproximation queryApprox;

  /**
   * Constructor.
   * 
   * @param p Value of p
   * @param splitPositions Split positions
   * @param query Query vector
   * @param queryApprox Query approximation
   */
  public VALPNormDistance(double p, double[][] splitPositions, NumberVector query, VectorApproximation queryApprox) {
    super();
    this.onebyp = 1.0 / p;
    this.queryApprox = queryApprox;
    initializeLookupTable(splitPositions, query, p);
  }

  /**
   * Get the minimum distance contribution of a single dimension.
   * 
   * @param dimension Dimension
   * @param vp Vector position
   * @return Increment
   */
  public double getPartialMinDist(int dimension, int vp) {
    final int qp = queryApprox.getApproximation(dimension);
    if(vp < qp) {
      return lookup[dimension][vp + 1];
    }
    else if(vp > qp) {
      return lookup[dimension][vp];
    }
    else {
      return 0.0;
    }
  }

  /**
   * Get the minimum distance to approximated vector vec.
   * 
   * @param vec Vector approximation
   * @return Minimum distance
   */
  public double getMinDist(VectorApproximation vec) {
    final int dim = lookup.length;
    double minDist = 0;
    for(int d = 0; d < dim; d++) {
      final int vp = vec.getApproximation(d);
      minDist += getPartialMinDist(d, vp);
    }
    return FastMath.pow(minDist, onebyp);
  }

  /**
   * Get the maximum distance contribution of a single dimension.
   * 
   * @param dimension Dimension
   * @param vp Vector position
   * @return Increment
   */
  public double getPartialMaxDist(int dimension, int vp) {
    final int qp = queryApprox.getApproximation(dimension);
    if(vp < qp) {
      return lookup[dimension][vp];
    }
    else if(vp > qp) {
      return lookup[dimension][vp + 1];
    }
    else {
      return Math.max(lookup[dimension][vp], lookup[dimension][vp + 1]);
    }
  }

  /**
   * Get the maximum distance.
   * 
   * @param vec Approximation vector
   * @return Maximum distance of the vector
   */
  public double getMaxDist(VectorApproximation vec) {
    final int dim = lookup.length;
    double maxDist = 0;
    for(int d = 0; d < dim; d++) {
      final int vp = vec.getApproximation(d);
      maxDist += getPartialMaxDist(d, vp);
    }
    return FastMath.pow(maxDist, onebyp);
  }

  /**
   * Get the maximum distance.
   * 
   * @param dimension Dimension
   * @return Maximum distance in the given dimension
   */
  public double getPartialMaxMaxDist(int dimension) {
    double[] data = lookup[dimension];
    double max = data[0];
    for(int i = 1; i < data.length; i++) {
      max = Math.max(max, data[i]);
    }
    return max;
  }

  /**
   * Initialize the lookup table.
   * 
   * @param splitPositions Split positions
   * @param query Query vector
   * @param p p
   */
  private void initializeLookupTable(double[][] splitPositions, NumberVector query, double p) {
    final int dimensions = splitPositions.length;
    final int bordercount = splitPositions[0].length;
    lookup = new double[dimensions][bordercount];
    for(int d = 0; d < dimensions; d++) {
      final double val = query.doubleValue(d);
      for(int i = 0; i < bordercount; i++) {
        lookup[d][i] = FastMath.pow(splitPositions[d][i] - val, p);
      }
    }
  }
}