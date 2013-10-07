package de.lmu.ifi.dbs.elki.distance.distancefunction.probabilistic;

/*
 This file is part of ELKI:
 Environment for Developing KDD-Applications Supported by Index-Structures

 Copyright (C) 2011
 Ludwig-Maximilians-Universität München
 Lehr- und Forschungseinheit für Datenbanksysteme
 ELKI Development Team

 This program is free software: you can redistribute it and/or modify
 it under the terms of the GNU Affero General Public License as published by
 the Free Software Foundation, either version 3 of the License, or
 (at your option) any later version.

 This program is distributed in the hope that it will be useful,
 but WITHOUT ANY WARRANTY; without even the implied warranty of
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 GNU Affero General Public License for more details.

 You should have received a copy of the GNU Affero General Public License
 along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

import de.lmu.ifi.dbs.elki.data.NumberVector;
import de.lmu.ifi.dbs.elki.distance.distancefunction.AbstractVectorDoubleDistanceFunction;
import de.lmu.ifi.dbs.elki.utilities.Alias;
import de.lmu.ifi.dbs.elki.utilities.documentation.Reference;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.AbstractParameterizer;

/**
 * Kullback-Leibler (asymmetric!) Distance, also known as relative entropy,
 * information deviation or just KL-distance
 * 
 * For a version with the arguments reversed, see {@link KullbackLeiblerDivergenceReverseAsymmetricDistanceFunction}.
 * 
 * For a symmetric version, see {@link JeffreyDivergenceDistanceFunction}.
 * 
 * Reference:
 * <p>
 * S. Kullback<br />
 * Information theory and statistics<br />
 * Courier Dover Publications, 1997.
 * </p>
 * 
 * @author Erich Schubert
 */
@Reference(authors = "S. Kullback", title = "Information theory and statistics", booktitle = "Information theory and statistics, Courier Dover Publications, 1997.")
@Alias("kl")
public class KullbackLeiblerDivergenceAsymmetricDistanceFunction extends AbstractVectorDoubleDistanceFunction {
  /**
   * Static instance. Use this!
   */
  public static final KullbackLeiblerDivergenceAsymmetricDistanceFunction STATIC = new KullbackLeiblerDivergenceAsymmetricDistanceFunction();

  /**
   * Constructor for the Kullback-Leibler divergence.
   * 
   * @deprecated Use static instance!
   */
  @Deprecated
  public KullbackLeiblerDivergenceAsymmetricDistanceFunction() {
    super();
  }

  @Override
  public double doubleDistance(NumberVector<?> v1, NumberVector<?> v2) {
    final int dim1 = v1.getDimensionality();
    if (dim1 != v2.getDimensionality()) {
      throw new IllegalArgumentException("Different dimensionality of FeatureVectors" + "\n  first argument: " + v1.toString() + "\n  second argument: " + v2.toString() + "\n" + v1.getDimensionality() + "!=" + v2.getDimensionality());
    }
    double dist = 0;
    for (int i = 0; i < dim1; i++) {
      final double xi = v1.doubleValue(i);
      final double yi = v2.doubleValue(i);
      if (xi > 0. && yi > 0.) {
        dist += xi * Math.log(xi / yi);
      }
    }
    return dist;
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
    if (obj == null) {
      return false;
    }
    if (obj == this) {
      return true;
    }
    if (this.getClass().equals(obj.getClass())) {
      return true;
    }
    return super.equals(obj);
  }

  /**
   * Parameterization class, using the static instance.
   * 
   * @author Erich Schubert
   * 
   * @apiviz.exclude
   */
  public static class Parameterizer extends AbstractParameterizer {
    @Override
    protected KullbackLeiblerDivergenceAsymmetricDistanceFunction makeInstance() {
      return STATIC;
    }
  }
}
