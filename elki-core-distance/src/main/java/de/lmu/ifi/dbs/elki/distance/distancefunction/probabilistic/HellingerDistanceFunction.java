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
import de.lmu.ifi.dbs.elki.database.query.distance.SpatialPrimitiveDistanceSimilarityQuery;
import de.lmu.ifi.dbs.elki.database.relation.Relation;
import de.lmu.ifi.dbs.elki.distance.distancefunction.AbstractNumberVectorDistanceFunction;
import de.lmu.ifi.dbs.elki.distance.distancefunction.SpatialPrimitiveDistanceFunction;
import de.lmu.ifi.dbs.elki.distance.similarityfunction.NormalizedPrimitiveSimilarityFunction;
import de.lmu.ifi.dbs.elki.math.MathUtil;
import de.lmu.ifi.dbs.elki.utilities.Alias;
import de.lmu.ifi.dbs.elki.utilities.documentation.Reference;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.AbstractParameterizer;
import net.jafama.FastMath;

/**
 * Hellinger metric / affinity / kernel, Bhattacharyya coefficient, fidelity
 * similarity, Matusita distance, Hellinger-Kakutani metric on a probability
 * distribution.
 * <p>
 * We assume vectors represent normalized probability distributions. Then
 * \[\text{Hellinger}(\vec{x},\vec{y}):=
 * \sqrt{\tfrac12\sum\nolimits_i \left(\sqrt{x_i}-\sqrt{y_i}\right)^2 } \]
 * <p>
 * The corresponding kernel / similarity is
 * \[ K_{\text{Hellinger}}(\vec{x},\vec{y}) := \sum\nolimits_i \sqrt{x_i y_i} \]
 * <p>
 * If we have normalized probability distributions, we have the nice
 * property that
 * \( K_{\text{Hellinger}}(\vec{x},\vec{x}) = \sum\nolimits_i x_i = 1\).
 * and therefore \( K_{\text{Hellinger}}(\vec{x},\vec{y}) \in [0:1] \).
 * <p>
 * Furthermore, we have the following relationship between this variant of the
 * distance and this kernel:
 * \[ \text{Hellinger}^2(\vec{x},\vec{y})
 * = \tfrac12\sum\nolimits_i \left(\sqrt{x_i}-\sqrt{y_i}\right)^2
 * = \tfrac12\sum\nolimits_i x_i + y_i - 2 \sqrt{x_i y_i} \]
 * \[ \text{Hellinger}^2(\vec{x},\vec{y})
 * = \tfrac12K_{\text{Hellinger}}(\vec{x},\vec{x})
 * + \tfrac12K_{\text{Hellinger}}(\vec{y},\vec{y})
 * - K_{\text{Hellinger}}(\vec{x},\vec{y})
 * = 1 - K_{\text{Hellinger}}(\vec{x},\vec{y}) \]
 * which implies \(\text{Hellinger}(\vec{x},\vec{y}) \in [0;1]\),
 * and is very similar to the Euclidean distance and the linear kernel.
 * <p>
 * From this, it follows trivially that Hellinger distance corresponds
 * to the kernel transformation
 * \(\phi:\vec{x}\mapsto(\tfrac12\sqrt{x_1},\ldots,\tfrac12\sqrt{x_d})\).
 * <p>
 * Deza and Deza unfortunately also give a second definition, as:
 * \[\text{Hellinger-Deza}(\vec{x},\vec{y}):=\sqrt{2\sum\nolimits_i
 * \left(\sqrt{\tfrac{x_i}{\bar{x}}}-\sqrt{\tfrac{y_i}{\bar{y}}}\right)^2}\]
 * which has a built-in normalization, and a different scaling that is no longer
 * bound to $[0;1]$. The 2 in this definition likely should be a \(\frac12\).
 * <p>
 * This distance is well suited for histograms, but it is then more efficient to
 * once normalize the histograms, apply the square roots, and then use Euclidean
 * distance (i.e., use the "kernel trick" in reverse, materializing the
 * transformation \(\phi\) given above).
 * <p>
 * Reference:
 * <p>
 * E. Hellinger<br>
 * Neue Begründung der Theorie quadratischer Formen von unendlichvielen
 * Veränderlichen<br>
 * Journal für die reine und angewandte Mathematik
 * <p>
 * M.-M. Deza, E. Deza<br>
 * Dictionary of distances
 * <p>
 * TODO: support acceleration for sparse vectors.
 * <p>
 * TODO: add a second variant, with built-in normalization.
 *
 * @author Erich Schubert
 * @since 0.7.0
 */
@Reference(authors = "E. Hellinger", //
    title = "Neue Begründung der Theorie quadratischer Formen von unendlichvielen Veränderlichen", //
    booktitle = "Journal für die reine und angewandte Mathematik", //
    url = "http://resolver.sub.uni-goettingen.de/purl?GDZPPN002166941", //
    bibkey = "journals/mathematik/Hellinger1909")
@Reference(authors = "M.-M. Deza, E. Deza", //
    title = "Dictionary of distances", //
    booktitle = "Dictionary of distances", //
    url = "https://doi.org/10.1007/978-3-642-00234-2", //
    bibkey = "doi:10.1007/978-3-642-00234-2")
@Alias({ "hellinger", "bhattacharyya" })
public class HellingerDistanceFunction extends AbstractNumberVectorDistanceFunction implements SpatialPrimitiveDistanceFunction<NumberVector>, NormalizedPrimitiveSimilarityFunction<NumberVector> {
  /**
   * Static instance.
   */
  public static final HellingerDistanceFunction STATIC = new HellingerDistanceFunction();

  /**
   * Assertion error message.
   */
  private static final String NON_NEGATIVE = "Hellinger distance requires non-negative values.";

  /**
   * Hellinger kernel. Use static instance {@link #STATIC}!
   */
  @Deprecated
  public HellingerDistanceFunction() {
    super();
  }

  @Override
  public double distance(final NumberVector fv1, final NumberVector fv2) {
    final int dim1 = fv1.getDimensionality(), dim2 = fv2.getDimensionality();
    final int mindim = dim1 < dim2 ? dim1 : dim2;
    double agg = 0.;
    for(int d = 0; d < mindim; d++) {
      final double v1 = fv1.doubleValue(d), v2 = fv2.doubleValue(d);
      assert (v1 >= 0 && v2 >= 0) : NON_NEGATIVE;
      if(v1 != v2) {
        final double v = FastMath.sqrt(v1) - FastMath.sqrt(v2);
        agg += v * v;
      }
    }
    for(int d = mindim; d < dim1; d++) {
      final double v1 = fv1.doubleValue(d);
      assert (v1 >= 0) : NON_NEGATIVE;
      agg += v1;
    }
    for(int d = mindim; d < dim2; d++) {
      final double v2 = fv2.doubleValue(d);
      assert (v2 >= 0) : NON_NEGATIVE;
      agg += v2;
    }
    return MathUtil.SQRTHALF * FastMath.sqrt(agg);
  }

  @Override
  public double minDist(SpatialComparable mbr1, SpatialComparable mbr2) {
    final int dim1 = mbr1.getDimensionality(), dim2 = mbr2.getDimensionality();
    final int mindim = (dim1 < dim2) ? dim1 : dim2;
    double agg = 0.;
    for(int d = 0; d < mindim; d++) {
      final double min1 = mbr1.getMin(d), max1 = mbr1.getMax(d);
      final double min2 = mbr2.getMin(d), max2 = mbr2.getMax(d);
      assert (min1 >= 0 && min2 >= 0) : NON_NEGATIVE;
      if(max1 < min2) {
        final double v = FastMath.sqrt(max1) - FastMath.sqrt(min2);
        agg += v * v;
      }
      else if(max2 < min1) {
        final double v = FastMath.sqrt(max2) - FastMath.sqrt(min1);
        agg += v * v;
      }
    }
    for(int d = mindim; d < dim1; d++) {
      final double min1 = mbr1.getMin(d);
      assert (min1 >= 0) : NON_NEGATIVE;
      agg += min1;
    }
    for(int d = mindim; d < dim2; d++) {
      final double min2 = mbr2.getMin(d);
      assert (min2 >= 0) : NON_NEGATIVE;
      agg += min2;
    }
    return MathUtil.SQRTHALF * FastMath.sqrt(agg);
  }

  @Override
  public double similarity(final NumberVector o1, final NumberVector o2) {
    // TODO: accelerate sparse!
    final int dim1 = o1.getDimensionality(), dim2 = o2.getDimensionality();
    final int mindim = dim1 < dim2 ? dim1 : dim2;
    double agg = 0.;
    for(int d = 0; d < mindim; d++) {
      final double v1 = o1.doubleValue(d), v2 = o2.doubleValue(d);
      agg += v1 == v2 ? (v1 > 0 ? v1 : -v1) : (v1 == 0 || v2 == 0) ? 0. : FastMath.sqrt(v1 * v2);
    }
    return agg;
  }

  @Override
  public boolean isSymmetric() {
    return true;
  }

  @Override
  public boolean isMetric() {
    return true; // as this equals Euclidean in sqrt space
  }

  @Override
  public <T extends NumberVector> SpatialPrimitiveDistanceSimilarityQuery<T> instantiate(Relation<T> database) {
    return new SpatialPrimitiveDistanceSimilarityQuery<>(database, this, this);
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
    protected HellingerDistanceFunction makeInstance() {
      return HellingerDistanceFunction.STATIC;
    }
  }
}
