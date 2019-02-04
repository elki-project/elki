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
package de.lmu.ifi.dbs.elki.distance.distancefunction.probabilistic;

import de.lmu.ifi.dbs.elki.data.NumberVector;
import de.lmu.ifi.dbs.elki.data.spatial.SpatialComparable;
import de.lmu.ifi.dbs.elki.utilities.documentation.Reference;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.AbstractParameterizer;

/**
 * Jensen-Shannon Divergence for {@link NumberVector}s is a symmetric,
 * smoothened version of the
 * {@link KullbackLeiblerDivergenceAsymmetricDistanceFunction}.
 * <p>
 * It essentially is the same as {@link JeffreyDivergenceDistanceFunction}, only
 * scaled by half. For completeness, we include both.
 * <p>
 * \[JS(\vec{x},\vec{y}):=\tfrac12\sum\nolimits_i
 * x_i\log\tfrac{2x_i}{x_i+y_i}+y_i\log\tfrac{2y_i}{x_i+y_i}
 * = \tfrac12 KL(\vec{x},\tfrac12(\vec{x}+\vec{y}))
 * + \tfrac12 KL(\vec{y},\tfrac12(\vec{x}+\vec{y}))\]
 * <p>
 * There exists a variable definition where the two vectors are weighted with
 * \(\beta\) and \(1-\beta\), which for the common choice of \(\beta=\tfrac12\)
 * yields this version.
 * <p>
 * Reference:
 * <p>
 * J. Lin<br>
 * Divergence measures based on the Shannon entropy<br>
 * IEEE Transactions on Information Theory 37(1)
 * <p>
 * D. M. Endres, J. E. Schindelin<br>
 * A new metric for probability distributions<br>
 * IEEE Transactions on Information Theory 49(7)
 * <p>
 * M.-M. Deza, E. Deza<br>
 * Dictionary of distances
 *
 * @author Erich Schubert
 * @since 0.6.0
 */
@Reference(authors = "J. Lin", //
    title = "Divergence measures based on the Shannon entropy", //
    booktitle = "IEEE Transactions on Information Theory 37(1)", //
    url = "https://doi.org/10.1109/18.61115", //
    bibkey = "DBLP:journals/tit/Lin91")
@Reference(authors = "D. M. Endres, J. E. Schindelin", //
    title = "A new metric for probability distributions", //
    booktitle = "IEEE Transactions on Information Theory 49(7)", //
    url = "https://doi.org/10.1109/TIT.2003.813506", //
    bibkey = "DBLP:journals/tit/EndresS03")
@Reference(authors = "M.-M. Deza, E. Deza", //
    title = "Dictionary of distances", //
    booktitle = "Dictionary of distances", //
    url = "https://doi.org/10.1007/978-3-642-00234-2", //
    bibkey = "doi:10.1007/978-3-642-00234-2")
public class JensenShannonDivergenceDistanceFunction extends JeffreyDivergenceDistanceFunction {
  /**
   * Static instance. Use this!
   */
  public static final JensenShannonDivergenceDistanceFunction STATIC = new JensenShannonDivergenceDistanceFunction();

  /**
   * Constructor for the Jensen-Shannon divergence.
   *
   * @deprecated Use static instance!
   */
  @Deprecated
  public JensenShannonDivergenceDistanceFunction() {
    super();
  }

  @Override
  public double distance(NumberVector v1, NumberVector v2) {
    return .5 * super.distance(v1, v2);
  }

  @Override
  public double minDist(SpatialComparable mbr1, SpatialComparable mbr2) {
    return .5 * super.minDist(mbr1, mbr2);
  }

  @Override
  public String toString() {
    return "JensenShannonDivergenceDistance";
  }

  /**
   * Parameterization class, using the static instance.
   * 
   * @author Erich Schubert
   */
  public static class Parameterizer extends AbstractParameterizer {
    @Override
    protected JensenShannonDivergenceDistanceFunction makeInstance() {
      return STATIC;
    }
  }
}
