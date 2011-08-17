package de.lmu.ifi.dbs.elki.distance.distancefunction.timeseries;
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

import de.lmu.ifi.dbs.elki.data.NumberVector;
import de.lmu.ifi.dbs.elki.distance.distancevalue.DoubleDistance;
import de.lmu.ifi.dbs.elki.utilities.documentation.Reference;
import de.lmu.ifi.dbs.elki.utilities.documentation.Title;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.OptionID;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.constraints.GreaterEqualConstraint;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.Parameterization;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.DoubleParameter;

/**
 * Provides the Edit Distance With Real Penalty distance for FeatureVectors.
 * 
 * @author Thomas Bernecker
 */
@Title("Edit Distance with Real Penalty")
@Reference(authors = "L. Chen and R. Ng", title = "On the marriage of Lp-norms and edit distance", booktitle = "VLDB '04: Proceedings of the Thirtieth international conference on Very large data bases", url = "http://www.vldb.org/conf/2004/RS21P2.PDF")
public class ERPDistanceFunction extends AbstractEditDistanceFunction {
  /**
   * G parameter
   */
  public static final OptionID G_ID = OptionID.getOrCreateOptionID("erp.g", "the g parameter ERP (positive number)");

  /**
   * Keeps the currently set g.
   */
  private double g;

  /**
   * Constructor.
   * 
   * @param bandSize Band size
   * @param g G parameter
   */
  public ERPDistanceFunction(double bandSize, double g) {
    super(bandSize);
    this.g = g;
  }

  /**
   * Provides the Edit Distance With Real Penalty distance between the given two
   * vectors.
   * 
   * @return the Edit Distance With Real Penalty distance between the given two
   *         vectors as an instance of {@link DoubleDistance DoubleDistance}.
   */
  @Override
  public double doubleDistance(NumberVector<?, ?> v1, NumberVector<?, ?> v2) {
    // Current and previous columns of the matrix
    double[] curr = new double[v2.getDimensionality()];
    double[] prev = new double[v2.getDimensionality()];

    // size of edit distance band
    // bandsize is the maximum allowed distance to the diagonal
    int band = (int) Math.ceil(v2.getDimensionality() * bandSize);

    // g parameter for local usage
    double gValue = g;

    for(int i = 0; i < v1.getDimensionality(); i++) {
      // Swap current and prev arrays. We'll just overwrite the new curr.
      {
        double[] temp = prev;
        prev = curr;
        curr = temp;
      }
      int l = i - (band + 1);
      if(l < 0) {
        l = 0;
      }
      int r = i + (band + 1);
      if(r > (v2.getDimensionality() - 1)) {
        r = (v2.getDimensionality() - 1);
      }

      for(int j = l; j <= r; j++) {
        if(Math.abs(i - j) <= band) {
          // compute squared distance of feature vectors
          double val1 = v1.doubleValue(i + 1);
          double val2 = gValue;
          double diff = (val1 - val2);
          final double d1 = Math.sqrt(diff * diff);

          val1 = gValue;
          val2 = v2.doubleValue(j + 1);
          diff = (val1 - val2);
          final double d2 = Math.sqrt(diff * diff);

          val1 = v1.doubleValue(i + 1);
          val2 = v2.doubleValue(j + 1);
          diff = (val1 - val2);
          final double d12 = Math.sqrt(diff * diff);

          final double dist1 = d1 * d1;
          final double dist2 = d2 * d2;
          final double dist12 = d12 * d12;

          final double cost;

          if((i + j) != 0) {
            if((i == 0) || ((j != 0) && (((prev[j - 1] + dist12) > (curr[j - 1] + dist2)) && ((curr[j - 1] + dist2) < (prev[j] + dist1))))) {
              // del
              cost = curr[j - 1] + dist2;
            }
            else if((j == 0) || ((i != 0) && (((prev[j - 1] + dist12) > (prev[j] + dist1)) && ((prev[j] + dist1) < (curr[j - 1] + dist2))))) {
              // ins
              cost = prev[j] + dist1;
            }
            else {
              // match
              cost = prev[j - 1] + dist12;
            }
          }
          else {
            cost = 0;
          }

          curr[j] = cost;
          // steps[i][j] = step;
        }
        else {
          curr[j] = Double.POSITIVE_INFINITY; // outside band
        }
      }
    }

    return Math.sqrt(curr[v2.getDimensionality() - 1]);
  }

  @Override
  public boolean equals(Object obj) {
    if(!super.equals(obj)) {
      return false;
    }
    return this.g == ((ERPDistanceFunction) obj).g;
  }
  
  /**
   * Parameterization class.
   * 
   * @author Erich Schubert
   * 
   * @apiviz.exclude
   */
  public static class Parameterizer extends AbstractEditDistanceFunction.Parameterizer {
    protected double g = 0.0;

    @Override
    protected void makeOptions(Parameterization config) {
      super.makeOptions(config);
      final DoubleParameter gP = new DoubleParameter(G_ID, new GreaterEqualConstraint(0), 0.0);
      if(config.grab(gP)) {
        g = gP.getValue();
      }
    }

    @Override
    protected ERPDistanceFunction makeInstance() {
      return new ERPDistanceFunction(bandSize, g);
    }
  }
}