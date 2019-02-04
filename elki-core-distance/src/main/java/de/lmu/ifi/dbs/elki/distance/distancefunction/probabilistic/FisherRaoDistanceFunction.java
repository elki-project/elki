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
import de.lmu.ifi.dbs.elki.data.type.SimpleTypeInformation;
import de.lmu.ifi.dbs.elki.distance.distancefunction.AbstractNumberVectorDistanceFunction;
import de.lmu.ifi.dbs.elki.distance.distancefunction.SpatialPrimitiveDistanceFunction;
import de.lmu.ifi.dbs.elki.utilities.Alias;
import de.lmu.ifi.dbs.elki.utilities.documentation.Reference;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.AbstractParameterizer;
import net.jafama.FastMath;

/**
 * Fisher-Rao riemannian metric for (discrete) probability distributions.
 * <p>
 * \[ \text{Fisher-Rao}(\vec{x},\vec{y})
 * := 2 \arccos \sum\nolimits_i \sqrt{p_iq_i} \]
 * <p>
 * References:
 * <p>
 * The metric was theoretically introduced by Rao in 1945:
 * <p>
 * C. R. Rao<br>
 * Information and the Accuracy Attainable in the Estimation of Statistical
 * Parameters<br>
 * Bulletin of the Calcutta Mathematical Society 37(3)
 * <p>
 * here, we use a version for discrete distributions, from:
 * <p>
 * M.-M. Deza, E. Deza<br>
 * Dictionary of distances
 *
 * @author Erich Schubert
 * @since 0.7.5
 */
@Reference(authors = "C. R. Rao", //
    title = "Information and the Accuracy Attainable in the Estimation of Statistical Parameters", //
    booktitle = "Bulletin of the Calcutta Mathematical Society 37(3)", //
    bibkey = "journals/bcalms/Rao45")
@Reference(authors = "M.-M. Deza, E. Deza", //
    title = "Dictionary of distances", //
    booktitle = "Dictionary of distances", //
    url = "https://doi.org/10.1007/978-3-642-00234-2", //
    bibkey = "doi:10.1007/978-3-642-00234-2")
@Alias({ "rao", "fisher-rao", "fisher" })
public class FisherRaoDistanceFunction extends AbstractNumberVectorDistanceFunction implements SpatialPrimitiveDistanceFunction<NumberVector> {
  /**
   * Static instance.
   */
  public static final FisherRaoDistanceFunction STATIC = new FisherRaoDistanceFunction();

  /**
   * Rao distance. Use static instance {@link #STATIC}!
   */
  @Deprecated
  public FisherRaoDistanceFunction() {
    super();
  }

  @Override
  public double distance(final NumberVector fv1, final NumberVector fv2) {
    final int dim1 = fv1.getDimensionality(), dim2 = fv2.getDimensionality();
    final int mindim = dim1 < dim2 ? dim1 : dim2;
    double agg = 0.;
    for(int d = 0; d < mindim; d++) {
      final double v12 = fv1.doubleValue(d) * fv2.doubleValue(d);
      assert (v12 >= 0) : "This distance is not defined on negative values.";
      if(v12 > 0) {
        agg += FastMath.sqrt(v12);
      }
    }
    return agg >= 1 ? 0 : agg <= -1 ? -Math.PI : 2 * FastMath.acos(agg);
  }

  @Override
  public double minDist(SpatialComparable mbr1, SpatialComparable mbr2) {
    final int dim1 = mbr1.getDimensionality(), dim2 = mbr2.getDimensionality();
    final int mindim = (dim1 < dim2) ? dim1 : dim2;
    double agg = 0.;
    for(int d = 0; d < mindim; d++) {
      final double v12 = mbr1.getMax(d) * mbr2.getMax(d);
      assert (v12 >= 0) : "This distance is not defined on negative values.";
      if(v12 > 0) {
        agg += FastMath.sqrt(v12);
      }
    }
    return agg >= 1 ? 0 : agg <= -1 ? -Math.PI : 2 * FastMath.acos(agg);
  }

  @Override
  public boolean isMetric() {
    return true;
  }

  @Override
  public SimpleTypeInformation<? super NumberVector> getInputTypeRestriction() {
    return NumberVector.VARIABLE_LENGTH;
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
   * Parameterization class.
   * 
   * @author Erich Schubert
   */
  public static class Parameterizer extends AbstractParameterizer {
    @Override
    protected FisherRaoDistanceFunction makeInstance() {
      return FisherRaoDistanceFunction.STATIC;
    }
  }
}
