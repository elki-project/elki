package de.lmu.ifi.dbs.elki.distance.distancefunction.histogram;

/*
 This file is part of ELKI:
 Environment for Developing KDD-Applications Supported by Index-Structures

 Copyright (C) 2015
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
import de.lmu.ifi.dbs.elki.distance.distancefunction.AbstractNumberVectorDistanceFunction;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.AbstractParameterizer;

/**
 * Distance function based on the Kolmogorov-Smirnov goodness of fit test.
 * 
 * This distance function assumes there exist a natural order in the vectors,
 * i.e. they should be some 1-dimensional histogram.
 * 
 * @author Erich Schubert
 * @since 0.4.0
 */
public class KolmogorovSmirnovDistanceFunction extends AbstractNumberVectorDistanceFunction {
  /**
   * Static instance. Use this!
   */
  public static final KolmogorovSmirnovDistanceFunction STATIC = new KolmogorovSmirnovDistanceFunction();

  /**
   * Constructor for the Kolmogorov-Smirnov distance function.
   * 
   * @deprecated Use static instance!
   */
  @Deprecated
  public KolmogorovSmirnovDistanceFunction() {
    super();
  }

  @Override
  public double distance(NumberVector v1, NumberVector v2) {
    final int dim = dimensionality(v1, v2);
    double xs = 0., ys = 0., agg = 0.;
    for (int i = 0; i < dim; i++) {
      xs += v1.doubleValue(i);
      ys += v2.doubleValue(i);
      double diff = Math.abs(xs - ys);
      if (diff > agg) {
        agg = diff;
      }
    }
    return agg;
  }

  @Override
  public String toString() {
    return "KolmogorovSmirnovDistanceFunction";
  }

  @Override
  public boolean equals(Object obj) {
    if (obj == null) {
      return false;
    }
    if (obj == this) {
      return true;
    }
    if (this.getClass().equals(obj.getClass())) {
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
    protected KolmogorovSmirnovDistanceFunction makeInstance() {
      return STATIC;
    }
  }
}
