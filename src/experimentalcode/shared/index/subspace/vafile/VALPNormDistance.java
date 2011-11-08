package experimentalcode.shared.index.subspace.vafile;

import de.lmu.ifi.dbs.elki.data.NumberVector;

/*
 This file is part of ELKI:
 Environment for Developing KDD-Applications Supported by Index-Structures

 Copyright (C) 2011
 Ludwig-Maximilians-Universität München
 Lehr- und Forschungseinheit für Datenbanksysteme
 ELKI Development Team

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

public class VALPNormDistance {
  /**
   * Value of 1/p for lP norm
   */
  private final double onebyp;

  private double[][] lookup;

  private VectorApproximation queryApprox;

  /**
   * Constructor.
   * 
   * @param p Value of p
   * @param splitPositions Split positions
   * @param query Query vector
   * @param queryApprox
   */
  public VALPNormDistance(double p, double[][] splitPositions, NumberVector<?, ?> query, VectorApproximation queryApprox) {
    super();
    this.onebyp = 1.0 / p;
    this.queryApprox = queryApprox;
    initializeLookupTable(splitPositions, query, p);
  }

  /**
   * Get the minimum distance to approximated vector vec
   * 
   * @param vec Vector approximation
   * @return Minimum distance
   */
  public double getMinDist(VectorApproximation vec) {
    final int dim = lookup.length;
    double minDist = 0;
    for(int d = 0; d < dim; d++) {
      final int vp = vec.getApproximation(d);
      final int qp = queryApprox.getApproximation(d);
      if(vp < qp) {
        minDist += lookup[d][vp + 1];
      }
      else if(vp > qp) {
        minDist += lookup[d][vp];
      } // else: 0
    }
    return Math.pow(minDist, onebyp);
  }

  /**
   * Get the maximum distance.
   * 
   * @param vec
   * @return
   */
  public double getMaxDist(VectorApproximation vec) {
    final int dim = lookup.length;
    double maxDist = 0;
    for(int d = 0; d < dim; d++) {
      final int vp = vec.getApproximation(d);
      final int qp = queryApprox.getApproximation(d);
      if(vp < qp) {
        maxDist += lookup[d][vp];
      }
      else if(vp > qp) {
        maxDist += lookup[d][vp + 1];
      }
      else {
        maxDist += Math.max(lookup[d][vp], lookup[d][vp + 1]);
      }
    }
    return Math.pow(maxDist, onebyp);
  }

  /**
   * Initialize the lookup table
   * 
   * @param splitPositions Split positions
   * @param query Query vector
   * @param p p
   */
  private void initializeLookupTable(double[][] splitPositions, NumberVector<?, ?> query, double p) {
    final int dimensions = splitPositions.length;
    final int bordercount = splitPositions[0].length;
    lookup = new double[dimensions][bordercount];
    for(int d = 0; d < dimensions; d++) {
      final double val = query.doubleValue(d + 1);
      for(int i = 0; i < bordercount; i++) {
        lookup[d][i] = Math.pow(splitPositions[d][i] - val, p);
      }
    }
  }
}