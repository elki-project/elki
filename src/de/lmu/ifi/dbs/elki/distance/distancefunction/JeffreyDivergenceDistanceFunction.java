package de.lmu.ifi.dbs.elki.distance.distancefunction;

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
import de.lmu.ifi.dbs.elki.utilities.optionhandling.AbstractParameterizer;

/**
 * Provides the Jeffrey Divergence Distance for FeatureVectors.
 * 
 * @author Erich Schubert
 */
public class JeffreyDivergenceDistanceFunction extends AbstractVectorDoubleDistanceFunction {
  /**
   * Static instance. Use this!
   */
  public static final JeffreyDivergenceDistanceFunction STATIC = new JeffreyDivergenceDistanceFunction();

  /**
   * Constructor for the Jeffrey divergence.
   * 
   * @deprecated Use static instance!
   */
  @Deprecated
  public JeffreyDivergenceDistanceFunction() {
  }

  @Override
  public double doubleDistance(NumberVector<?, ?> v1, NumberVector<?, ?> v2) {
    final int dim1 = v1.getDimensionality();
    if(dim1 != v2.getDimensionality()) {
      throw new IllegalArgumentException("Different dimensionality of FeatureVectors" + "\n  first argument: " + v1.toString() + "\n  second argument: " + v2.toString() + "\n" + v1.getDimensionality() + "!=" + v2.getDimensionality());
    }
    double dist = 0;
    for(int i = 1; i <= dim1; i++) {
      final double xi = v1.doubleValue(i);
      final double yi = v2.doubleValue(i);
      final double mi = (xi + yi) / 2;
      dist += xi * Math.log(xi / mi);
      dist += yi * Math.log(yi / mi);
    }
    return dist;
  }

  @Override
  public String toString() {
    return "JeffreyDivergenceDistance";
  }

  @Override
  public boolean equals(Object obj) {
    if(obj == null) {
      return false;
    }
    if(obj == this) {
      return true;
    }
    if(this.getClass().equals(obj.getClass())) {
      return true;
    }
    return super.equals(obj);
  }

  /**
   * Parameterization class, using the static instance.
   * 
   * @author Erich Schubert
   * 
   * @apiviz.exclude
   */
  public static class Parameterizer extends AbstractParameterizer {
    @Override
    protected JeffreyDivergenceDistanceFunction makeInstance() {
      return JeffreyDivergenceDistanceFunction.STATIC;
    }
  }
}