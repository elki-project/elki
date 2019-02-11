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
package de.lmu.ifi.dbs.elki.distance.distancefunction.histogram;

import de.lmu.ifi.dbs.elki.data.NumberVector;
import de.lmu.ifi.dbs.elki.data.spatial.SpatialComparable;
import de.lmu.ifi.dbs.elki.distance.distancefunction.AbstractNumberVectorDistanceFunction;
import de.lmu.ifi.dbs.elki.distance.distancefunction.SpatialPrimitiveDistanceFunction;
import de.lmu.ifi.dbs.elki.distance.distancefunction.minkowski.ManhattanDistanceFunction;
import de.lmu.ifi.dbs.elki.utilities.documentation.Reference;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.AbstractParameterizer;

/**
 * Distance function based on histogram matching, i.e., Manhattan distance on
 * the cumulative density function.
 * <p>
 * If your data is normalized to the 1-norm of 1, use
 * {@link ManhattanDistanceFunction} instead, as this will be faster!
 * <p>
 * This distance function assumes there exist a natural order in the vectors,
 * i.e., they should be some 1-dimensional histogram.
 * <p>
 * This is also known as Earth Movers Distance (EMD), 1st Mallows distance or
 * 1st Wasserstein metric (also Vasershtein metric), for the special case of a
 * one-dimensional histogram, where the cost is linear in the number of bins to
 * transport.
 * <p>
 * Reference:
 * <p>
 * L. N. Vaserstein<br>
 * Markov processes over denumerable products of spaces describing large systems
 * of automata<br>
 * Problemy Peredachi Informatsii 5.3 / Problems of Information Transmission 5:3
 *
 * @author Erich Schubert
 * @since 0.6.0
 */
@Reference(authors = "L. N. Vaserstein", //
    title = "Markov processes over denumerable products of spaces describing large systems of automata", //
    booktitle = "Problemy Peredachi Informatsii 5.3 / Problems of Information Transmission, 5:3", //
    url = "http://mi.mathnet.ru/eng/ppi1811", //
    bibkey = "journals/misc/Vaserstein69")
public class HistogramMatchDistanceFunction extends AbstractNumberVectorDistanceFunction implements SpatialPrimitiveDistanceFunction<NumberVector> {
  /**
   * Static instance. Use this!
   */
  public static final HistogramMatchDistanceFunction STATIC = new HistogramMatchDistanceFunction();

  /**
   * Constructor for the Histogram match distance function.
   * 
   * @deprecated Use static instance!
   */
  @Deprecated
  public HistogramMatchDistanceFunction() {
    super();
  }

  @Override
  public double distance(NumberVector v1, NumberVector v2) {
    final int dim = dimensionality(v1, v2);
    double xs = 0., ys = 0., agg = 0.;
    for(int i = 0; i < dim; i++) {
      xs += v1.doubleValue(i);
    }
    for(int i = 0; i < dim; i++) {
      ys += v2.doubleValue(i);
    }
    double fx = xs > 0 ? 1. / xs : 1, fy = ys > 0 ? 1. / ys : 1;
    for(int i = 0; i < dim; i++) {
      agg += Math.abs(v1.doubleValue(i) * fx - v2.doubleValue(i) * fy);
    }
    return agg;
  }

  @Override
  public double minDist(SpatialComparable mbr1, SpatialComparable mbr2) {
    final int dim = dimensionality(mbr1, mbr2);
    double min1 = 0., max1 = 0., min2 = 0., max2 = 0., agg = 0.;
    for(int i = 0; i < dim; i++) {
      min1 += mbr1.getMin(i);
    }
    for(int i = 0; i < dim; i++) {
      max1 += mbr1.getMax(i);
    }
    for(int i = 0; i < dim; i++) {
      min2 += mbr2.getMin(i);
    }
    for(int i = 0; i < dim; i++) {
      max2 += mbr2.getMax(i);
    }
    double fmi1 = min1 > 0 ? 1. / min1 : 1;
    double fma1 = max1 > 0 ? 1. / max1 : 0;
    double fmi2 = min2 > 0 ? 1. / min2 : 1;
    double fma2 = max2 > 0 ? 1. / max2 : 0;
    for(int i = 0; i < dim; i++) {
      double d1 = mbr2.getMin(i) * fma2 - mbr1.getMax(i) * fmi1;
      agg += d1 > 0 ? d1 : Math.max(mbr1.getMin(i) * fma1 - mbr2.getMax(i) * fmi2, 0);
    }
    return agg;
  }

  @Override
  public boolean isMetric() {
    return true;
  }

  @Override
  public String toString() {
    return "HistogramMatchDistanceFunction";
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
    protected HistogramMatchDistanceFunction makeInstance() {
      return STATIC;
    }
  }
}
