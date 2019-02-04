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
 * The square root of Jensen-Shannon divergence is a metric.
 * <p>
 * \[\sqrt{JS}(\vec{x},\vec{y}):=\sqrt{\tfrac12\sum\nolimits_i
 * x_i\log\tfrac{2x_i}{x_i+y_i}+y_i\log\tfrac{2y_i}{x_i+y_i}}
 * = \sqrt{JS(\vec{x},\vec{y})}\]
 * <p>
 * A proof of triangle inequality (for "\(D_{PQ}\)") can be found in Endres and
 * Schindelin.
 * <p>
 * References:
 * <p>
 * D. M. Endres, J. E. Schindelin<br>
 * A new metric for probability distributions<br>
 * IEEE Transactions on Information Theory, 49(7)
 *
 * @author Erich Schubert
 * @since 0.6.0
 */
@Reference(authors = "D. M. Endres, J. E. Schindelin", //
    title = "A new metric for probability distributions", //
    booktitle = "IEEE Transactions on Information Theory, 49(7)", //
    url = "https://doi.org/10.1109/TIT.2003.813506", //
    bibkey = "DBLP:journals/tit/EndresS03")
public class SqrtJensenShannonDivergenceDistanceFunction extends JensenShannonDivergenceDistanceFunction {
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
    return FastMath.sqrt(super.distance(v1, v2));
  }

  @Override
  public double minDist(SpatialComparable mbr1, SpatialComparable mbr2) {
    return FastMath.sqrt(super.minDist(mbr1, mbr2));
  }

  @Override
  public boolean isMetric() {
    return true;
  }
  
  @Override
  public boolean isSquared() {
    return false;
  }

  @Override
  public String toString() {
    return "SqrtJensenShannonDivergenceDistance";
  }

  /**
   * Parameterization class, using the static instance.
   * 
   * @author Erich Schubert
   */
  public static class Parameterizer extends AbstractParameterizer {
    @Override
    protected SqrtJensenShannonDivergenceDistanceFunction makeInstance() {
      return STATIC;
    }
  }
}
