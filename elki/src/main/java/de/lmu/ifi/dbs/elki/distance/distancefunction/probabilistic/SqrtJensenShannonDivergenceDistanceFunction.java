package de.lmu.ifi.dbs.elki.distance.distancefunction.probabilistic;

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
import de.lmu.ifi.dbs.elki.utilities.documentation.Reference;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.AbstractParameterizer;

/**
 * The square root of Jensen-Shannon divergence is metric.
 * 
 * Reference (proof of triangle inequality, distance called D_PQ):
 * <p>
 * D. M. Endres, J. E. Schindelin<br />
 * A new metric for probability distributions<br />
 * IEEE Transactions on Information Theory, 49(7).
 * </p>
 * 
 * @author Erich Schubert
 * @since 0.4.0
 */
@Reference(authors = "D. M. Endres, J. E. Schindelin", title = "A new metric for probability distributions", booktitle = "IEEE Transactions on Information Theory, 49(7)", url = "http://dx.doi.org/10.1109/TIT.2003.813506")
public class SqrtJensenShannonDivergenceDistanceFunction extends AbstractNumberVectorDistanceFunction {
  /**
   * Static instance. Use this!
   */
  public static final SqrtJensenShannonDivergenceDistanceFunction STATIC = new SqrtJensenShannonDivergenceDistanceFunction();

  /**
   * Constructor for sqrt Jensen Shannon divergence.
   * 
   * @deprecated Use static instance!
   */
  @Deprecated
  public SqrtJensenShannonDivergenceDistanceFunction() {
    super();
  }

  @Override
  public double distance(NumberVector v1, NumberVector v2) {
    final int dim = dimensionality(v1, v2);
    double agg = 0.;
    for(int d = 0; d < dim; d++) {
      final double xd = v1.doubleValue(d), yd = v2.doubleValue(d);
      if(xd == yd) {
        continue;
      }
      final double md = .5 * (xd + yd);
      if(!(md > 0. || md < 0.)) {
        continue;
      }
      if(xd > 0.) {
        agg += xd * Math.log(xd / md);
      }
      if(yd > 0.) {
        agg += yd * Math.log(yd / md);
      }
    }
    return Math.sqrt(agg);
  }

  @Override
  public boolean isMetric() {
    return true;
  }

  @Override
  public String toString() {
    return "SqrtJensenShannonDivergenceDistance";
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
    protected SqrtJensenShannonDivergenceDistanceFunction makeInstance() {
      return STATIC;
    }
  }
}
