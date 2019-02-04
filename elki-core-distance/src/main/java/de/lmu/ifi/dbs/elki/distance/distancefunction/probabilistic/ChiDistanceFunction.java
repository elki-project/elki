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
import de.lmu.ifi.dbs.elki.utilities.Alias;
import de.lmu.ifi.dbs.elki.utilities.Priority;
import de.lmu.ifi.dbs.elki.utilities.documentation.Reference;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.AbstractParameterizer;
import net.jafama.FastMath;

/**
 * χ distance function, symmetric version.
 * This is the square root of the {@link ChiSquaredDistanceFunction}, and can
 * serve as a fast approximation to
 * {@link SqrtJensenShannonDivergenceDistanceFunction}.
 * <p>
 * This implementation assumes \(\sum_i x_i=\sum_i y_i\), and is defined as:
 * \[ \chi(\vec{x},\vec{y}):= \sqrt{2 \sum\nolimits_i
 * \tfrac{(x_i-x_i)^2}{x_i+y_i}} \]
 * <p>
 * Reference:
 * <p>
 * J. Puzicha, J. M. Buhmann, Y. Rubner, C. Tomasi<br>
 * Empirical evaluation of dissimilarity measures for color and texture<br>
 * Proc. 7th IEEE International Conference on Computer Vision
 * <p>
 * D. M. Endres, J. E. Schindelin<br>
 * A new metric for probability distributions<br>
 * IEEE Transactions on Information Theory, 49(7)
 *
 * @author Erich Schubert
 * @since 0.7.5
 */
@Alias("chi")
@Priority(Priority.IMPORTANT)
@Reference(authors = "J. Puzicha, J. M. Buhmann, Y. Rubner, C. Tomasi", //
    title = "Empirical evaluation of dissimilarity measures for color and texture", //
    booktitle = "Proc. 7th IEEE International Conference on Computer Vision", //
    url = "https://doi.org/10.1109/ICCV.1999.790412", //
    bibkey = "DBLP:conf/iccv/PuzichaRTB99")
@Reference(authors = "D. M. Endres, J. E. Schindelin", //
    title = "A new metric for probability distributions", //
    booktitle = "IEEE Transactions on Information Theory, 49(7)", //
    url = "https://doi.org/10.1109/TIT.2003.813506", //
    bibkey = "DBLP:journals/tit/EndresS03")
public class ChiDistanceFunction extends ChiSquaredDistanceFunction {
  /**
   * Static instance. Use this!
   */
  public static final ChiDistanceFunction STATIC = new ChiDistanceFunction();

  /**
   * Constructor for the Chi-Squared distance function.
   * 
   * @deprecated Use static instance!
   */
  @Deprecated
  public ChiDistanceFunction() {
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
    return "ChiDistance";
  }

  /**
   * Parameterization class, using the static instance.
   * 
   * @author Erich Schubert
   */
  public static class Parameterizer extends AbstractParameterizer {
    @Override
    protected ChiDistanceFunction makeInstance() {
      return STATIC;
    }
  }
}
