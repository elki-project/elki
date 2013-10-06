package de.lmu.ifi.dbs.elki.distance.distancefunction.histogram;

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
import de.lmu.ifi.dbs.elki.distance.distancefunction.AbstractVectorDoubleDistanceFunction;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.AbstractParameterizer;

/**
 * Distance function based on histogram matching, i.e. Manhattan distance on the
 * cumulative density function.
 * 
 * This distance function assumes there exist a natural order in the vectors,
 * i.e. they should be some 1-dimensional histogram.
 * 
 * @author Erich Schubert
 */
public class HistogramMatchDistanceFunction extends AbstractVectorDoubleDistanceFunction {
  /**
   * Static instance. Use this!
   */
  public static final HistogramMatchDistanceFunction STATIC = new HistogramMatchDistanceFunction();

  /**
   * Constructor for the Histogram match distance function.
   * 
   * @deprecated Use static instance!
   */
  @Deprecated
  public HistogramMatchDistanceFunction() {
    super();
  }

  @Override
  public double doubleDistance(NumberVector<?> v1, NumberVector<?> v2) {
    final int dim1 = v1.getDimensionality();
    if(dim1 != v2.getDimensionality()) {
      throw new IllegalArgumentException("Different dimensionality of FeatureVectors" + "\n  first argument: " + v1.toString() + "\n  second argument: " + v2.toString() + "\n" + v1.getDimensionality() + "!=" + v2.getDimensionality());
    }
    double xs = 0., ys = 0., dist = 0.;
    for(int i = 0; i < dim1; i++) {
      xs += v1.doubleValue(i);
      ys += v2.doubleValue(i);
      dist += Math.abs(xs - ys);
    }
    return dist;
  }

  @Override
  public String toString() {
    return "HistogramMatchDistanceFunction";
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
    protected HistogramMatchDistanceFunction makeInstance() {
      return STATIC;
    }
  }
}
