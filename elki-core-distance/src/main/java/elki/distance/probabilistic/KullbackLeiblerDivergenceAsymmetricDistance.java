/*
 * This file is part of ELKI:
 * Environment for Developing KDD-Applications Supported by Index-Structures
 *
 * Copyright (C) 2022
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
package elki.distance.probabilistic;

import elki.data.NumberVector;
import elki.distance.AbstractNumberVectorDistance;
import elki.utilities.Alias;
import elki.utilities.documentation.Reference;
import elki.utilities.optionhandling.Parameterizer;

/**
 * Kullback-Leibler divergence, also known as relative entropy,
 * information deviation, or just KL-distance (albeit asymmetric).
 * <p>
 * \[KL(\vec{x},\vec{y}):=\sum\nolimits_i x_i\log\tfrac{x_i}{y_i}\]
 * <p>
 * For a version with the arguments reversed, see
 * {@link KullbackLeiblerDivergenceReverseAsymmetricDistance}.
 * <p>
 * For a symmetric version, see {@link JeffreyDivergenceDistance}.
 * <p>
 * Reference:
 * <p>
 * S. Kullback<br>
 * Information theory and statistics
 * 
 * @author Erich Schubert
 * @since 0.6.0
 */
@Reference(authors = "S. Kullback", //
    title = "Information theory and statistics", booktitle = "", //
    bibkey = "books/dover/Kullback59")
@Alias("kl")
public class KullbackLeiblerDivergenceAsymmetricDistance extends AbstractNumberVectorDistance {
  /**
   * Static instance. Use this!
   */
  public static final KullbackLeiblerDivergenceAsymmetricDistance STATIC = new KullbackLeiblerDivergenceAsymmetricDistance();

  /**
   * Constructor for the Kullback-Leibler divergence.
   * 
   * @deprecated Use static instance!
   */
  @Deprecated
  public KullbackLeiblerDivergenceAsymmetricDistance() {
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
        agg += xd * Math.log(xd / yd);
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
    return obj == this || (obj != null && this.getClass().equals(obj.getClass()));
  }

  @Override
  public int hashCode() {
    return getClass().hashCode();
  }

  /**
   * Parameterization class, using the static instance.
   * 
   * @author Erich Schubert
   */
  public static class Par implements Parameterizer {
    @Override
    public KullbackLeiblerDivergenceAsymmetricDistance make() {
      return STATIC;
    }
  }
}
