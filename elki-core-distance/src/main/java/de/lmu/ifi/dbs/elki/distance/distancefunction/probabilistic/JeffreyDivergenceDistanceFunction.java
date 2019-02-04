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
import de.lmu.ifi.dbs.elki.distance.distancefunction.AbstractNumberVectorDistanceFunction;
import de.lmu.ifi.dbs.elki.distance.distancefunction.SpatialPrimitiveDistanceFunction;
import de.lmu.ifi.dbs.elki.utilities.Alias;
import de.lmu.ifi.dbs.elki.utilities.documentation.Reference;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.AbstractParameterizer;
import net.jafama.FastMath;

/**
 * Jeffrey Divergence for {@link NumberVector}s is a symmetric, smoothened
 * version of the {@link KullbackLeiblerDivergenceAsymmetricDistanceFunction}.
 * Topsøe called this "capacitory discrimination".
 * <p>
 * \[JD(\vec{x},\vec{y}):= \sum\nolimits_i
 * x_i\log\tfrac{2x_i}{x_i+y_i}+y_i\log\tfrac{2y_i}{x_i+y_i}
 * = KL(\vec{x},\tfrac12(\vec{x}+\vec{y}))
 * + KL(\vec{y},\tfrac12(\vec{x}+\vec{y}))\]
 * <p>
 * Reference:
 * <p>
 * H. Jeffreys<br>
 * An invariant form for the prior probability in estimation problems<br>
 * Proc. Royal Society A: Mathematical, Physical and Engineering Sciences
 * 186(1007)
 * <p>
 * J. Puzicha, J. M. Buhmann, Y. Rubner, C. Tomasi<br>
 * Empirical evaluation of dissimilarity measures for color and texture<br>
 * Proc. 7th IEEE International Conference on Computer Vision
 * <p>
 * F. Topsøe<br>
 * Some inequalities for information divergence and related measures of
 * discrimination<br>
 * IEEE Transactions on information theory, 46(4)
 * <p>
 * D. M. Endres, J. E. Schindelin<br>
 * A new metric for probability distributions<br>
 * IEEE Transactions on Information Theory 49(7)
 * <p>
 * TODO: add support for sparse vectors + varying length
 *
 * @author Erich Schubert
 * @since 0.5.0
 */
@Reference(authors = "H. Jeffreys", //
    title = "An invariant form for the prior probability in estimation problems", //
    booktitle = "Proc. Royal Society A: Mathematical, Physical and Engineering Sciences 186(1007)", //
    url = "https://doi.org/10.1098/rspa.1946.0056", //
    bibkey = "doi:10.1098/rspa.1946.0056")
@Reference(authors = "J. Puzicha, J. M. Buhmann, Y. Rubner, C. Tomasi", //
    title = "Empirical evaluation of dissimilarity measures for color and texture", //
    booktitle = "Proc. 7th IEEE International Conference on Computer Vision", //
    url = "https://doi.org/10.1109/ICCV.1999.790412", //
    bibkey = "DBLP:conf/iccv/PuzichaRTB99")
@Reference(authors = "F. Topsøe", //
    title = "Some inequalities for information divergence and related measures of discrimination", //
    booktitle = "IEEE Transactions on information theory, 46(4)", //
    url = "https://doi.org/10.1109/18.850703", //
    bibkey = "DBLP:journals/tit/Topsoe00")
@Reference(authors = "D. M. Endres, J. E. Schindelin", //
    title = "A new metric for probability distributions", //
    booktitle = "IEEE Transactions on Information Theory 49(7)", //
    url = "https://doi.org/10.1109/TIT.2003.813506", //
    bibkey = "DBLP:journals/tit/EndresS03")
@Alias({ "j-divergence" })
public class JeffreyDivergenceDistanceFunction extends AbstractNumberVectorDistanceFunction implements SpatialPrimitiveDistanceFunction<NumberVector> {
  /**
   * Static instance. Use this!
   */
  public static final JeffreyDivergenceDistanceFunction STATIC = new JeffreyDivergenceDistanceFunction();

  /**
   * Constructor for the Jeffrey divergence - use {@link #STATIC} instead.
   * 
   * @deprecated Use static instance!
   */
  @Deprecated
  public JeffreyDivergenceDistanceFunction() {
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
      if(!(md > 0.)) {
        continue;
      }
      agg += (xd > 0 ? xd * FastMath.log(xd / md) : 0) //
          + (yd > 0 ? yd * FastMath.log(yd / md) : 0);
    }
    return agg;
  }

  @Override
  public double minDist(SpatialComparable mbr1, SpatialComparable mbr2) {
    final int dim = dimensionality(mbr1, mbr2);
    double agg = 0;
    for(int d = 0; d < dim; d++) {
      final double min1 = mbr1.getMin(d), min2 = mbr2.getMin(d);
      final double md = .5 * (mbr1.getMax(d) + mbr2.getMax(d));
      if(!(md > 0.)) {
        continue;
      }
      agg += (min1 > 0 ? min1 * FastMath.log(min1 / md) : 0) //
          + (min2 > 0 ? min2 * FastMath.log(min2 / md) : 0);
    }
    return agg > 0 ? agg : 0;
  }

  @Override
  public boolean isSquared() {
    return true;
  }

  @Override
  public String toString() {
    return "JeffreyDivergenceDistance";
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
    protected JeffreyDivergenceDistanceFunction makeInstance() {
      return STATIC;
    }
  }
}
