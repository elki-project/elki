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
import net.jafama.FastMath;

/**
 * Triangular Distance has relatively tight upper and lower bounds to the
 * (square root of the) Jensen-Shannon divergence, but is much less expensive.
 * <p>
 * \[\text{Triangular-Distance}(\vec{x},\vec{y}):=\sqrt{
 * \sum\nolimits_i \tfrac{|x_i-y_i|^2}{x_i+y_i}}\]
 * <p>
 * This distance function is meant for distribution vectors that sum to 1, and
 * does not work on negative values.
 * <p>
 * This differs from {@link TriangularDistanceFunction} simply by the square
 * root, which makes it a proper metric and a good approximation for the much
 * more expensive {@link SqrtJensenShannonDivergenceDistanceFunction}.
 * <p>
 * Reference:
 * <p>
 * R. Connor, F. A. Cardillo, L. Vadicamo, F. Rabitti<br>
 * Hilbert Exclusion: Improved Metric Search through Finite Isometric
 * Embeddings<br>
 * arXiv preprint arXiv:1604.08640
 * <p>
 * TODO: support sparse vectors, varying length
 *
 * @author Erich Schubert
 * @since 0.7.5
 */
@Reference(authors = "R. Connor, F. A. Cardillo, L. Vadicamo, F. Rabitti", //
    title = "Hilbert Exclusion: Improved Metric Search through Finite Isometric Embeddings", //
    booktitle = "arXiv preprint arXiv:1604.08640", //
    url = "http://arxiv.org/abs/1604.08640", //
    bibkey = "DBLP:journals/corr/ConnorCVR16")
public class TriangularDistanceFunction extends TriangularDiscriminationDistanceFunction {
  /**
   * Static instance. Use this!
   */
  public static final TriangularDistanceFunction STATIC = new TriangularDistanceFunction();

  /**
   * Deprecated constructor: use the static instance {@link #STATIC} instead.
   */
  @Deprecated
  private TriangularDistanceFunction() {
    super();
  }

  @Override
  public double distance(NumberVector v1, NumberVector v2) {
    return FastMath.sqrt(super.distance(v1, v2));
  }

  @Override
  public double minDist(SpatialComparable mbr1, SpatialComparable mbr2) {
    return FastMath.sqrt(super.minDist(mbr1, mbr2));
  }

  @Override
  public boolean isSquared() {
    return false;
  }

  @Override
  public boolean isMetric() {
    return true;
  }

  @Override
  public String toString() {
    return "TriangularDistanceFunction";
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
  public static class Parameterizer extends AbstractParameterizer {
    @Override
    protected TriangularDistanceFunction makeInstance() {
      return STATIC;
    }
  }
}
