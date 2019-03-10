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
package elki.similarity;

import elki.data.NumberVector;
import elki.distance.AbstractNumberVectorDistance;
import elki.utilities.documentation.Reference;
import elki.utilities.optionhandling.AbstractParameterizer;

/**
 * Kulczynski similarity 2.
 * <p>
 * \[ s_\text{Kulczynski-2}(\vec{x},\vec{y} :=
 * \tfrac{n}{2}\left(\tfrac{1}{\bar{x}}+\tfrac{1}{\bar{y}}\right)
 * \sum\nolimits_i\min\{x_i,y_i\} \]
 * <p>
 * Reference:
 * <p>
 * M.-M. Deza and E. Deza<br>
 * Dictionary of distances
 * <p>
 * TODO: add an optimized version for binary data.
 * 
 * @author Erich Schubert
 * @since 0.6.0
 */
@Reference(authors = "M.-M. Deza, E. Deza", //
    title = "Dictionary of distances", //
    booktitle = "Dictionary of distances", //
    url = "https://doi.org/10.1007/978-3-642-00234-2", //
    bibkey = "doi:10.1007/978-3-642-00234-2")
public class Kulczynski2Similarity extends AbstractVectorSimilarity {
  /**
   * Static instance.
   */
  public static final Kulczynski2Similarity STATIC_CONTINUOUS = new Kulczynski2Similarity();

  /**
   * Constructor.
   * 
   * @deprecated Use {@link #STATIC_CONTINUOUS} instance instead.
   */
  @Deprecated
  public Kulczynski2Similarity() {
    super();
  }

  @Override
  public double similarity(NumberVector v1, NumberVector v2) {
    final int dim = AbstractNumberVectorDistance.dimensionality(v1, v2);
    double sumx = 0., sumy = 0., summin = 0.;
    for(int i = 0; i < dim; i++) {
      double xi = v1.doubleValue(i), yi = v2.doubleValue(i);
      sumx += xi;
      sumy += yi;
      summin += Math.min(xi, yi);
    }
    return dim * .5 * (dim / sumx + dim / sumy) * summin;
  }

  /**
   * Parameterization class.
   * 
   * @author Erich Schubert
   */
  public static class Parameterizer extends AbstractParameterizer {
    @Override
    protected Kulczynski2Similarity makeInstance() {
      return Kulczynski2Similarity.STATIC_CONTINUOUS;
    }
  }
}
