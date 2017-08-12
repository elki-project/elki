/*
 * This file is part of ELKI:
 * Environment for Developing KDD-Applications Supported by Index-Structures
 *
 * Copyright (C) 2017
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
package de.lmu.ifi.dbs.elki.distance.distancefunction.probabilistic;

import de.lmu.ifi.dbs.elki.data.NumberVector;
import de.lmu.ifi.dbs.elki.distance.distancefunction.AbstractNumberVectorDistanceFunction;
import de.lmu.ifi.dbs.elki.utilities.Alias;
import de.lmu.ifi.dbs.elki.utilities.documentation.Reference;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.AbstractParameterizer;
import net.jafama.FastMath;

/**
 * Kullback-Leibler (asymmetric!) Distance, also known as relative entropy,
 * information deviation or just KL-distance
 * 
 * For a version with the arguments reversed, see
 * {@link KullbackLeiblerDivergenceReverseAsymmetricDistanceFunction}.
 * 
 * For a symmetric version, see {@link JeffreyDivergenceDistanceFunction}.
 * 
 * Reference:
 * <p>
 * S. Kullback<br />
 * Information theory and statistics<br />
 * Courier Dover Publications, 1997.
 * </p>
 * 
 * @author Erich Schubert
 * @since 0.4.0
 */
@Reference(authors = "S. Kullback", //
    title = "Information theory and statistics", //
    booktitle = "Information theory and statistics, Courier Dover Publications, 1997.")
@Alias("kl")
public class KullbackLeiblerDivergenceAsymmetricDistanceFunction extends AbstractNumberVectorDistanceFunction {
  /**
   * Static instance. Use this!
   */
  public static final KullbackLeiblerDivergenceAsymmetricDistanceFunction STATIC = new KullbackLeiblerDivergenceAsymmetricDistanceFunction();

  /**
   * Constructor for the Kullback-Leibler divergence.
   * 
   * @deprecated Use static instance!
   */
  @Deprecated
  public KullbackLeiblerDivergenceAsymmetricDistanceFunction() {
    super();
  }

  @Override
  public double distance(NumberVector v1, NumberVector v2) {
    final int dim = dimensionality(v1, v2);
    double agg = 0.;
    for(int d = 0; d < dim; d++) {
      final double xd = v1.doubleValue(d), yd = v2.doubleValue(d);
      if(yd <= 0.) {
        return Double.POSITIVE_INFINITY;
      }
      if(xd > 0.) {
        agg += xd * FastMath.log(xd / yd);
      }
    }
    return agg;
  }

  @Override
  public boolean isSymmetric() {
    return false;
  }

  @Override
  public String toString() {
    return "KullbackLeiblerDivergenceDistance";
  }

  @Override
  public boolean equals(Object obj) {
    return obj instanceof KullbackLeiblerDivergenceAsymmetricDistanceFunction;
  }

  @Override
  public int hashCode() {
    return getClass().hashCode();
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
    protected KullbackLeiblerDivergenceAsymmetricDistanceFunction makeInstance() {
      return STATIC;
    }
  }
}
