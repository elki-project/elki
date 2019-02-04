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
package de.lmu.ifi.dbs.elki.distance.distancefunction.correlation;

import de.lmu.ifi.dbs.elki.data.NumberVector;
import de.lmu.ifi.dbs.elki.distance.distancefunction.AbstractNumberVectorDistanceFunction;
import de.lmu.ifi.dbs.elki.math.PearsonCorrelation;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.AbstractParameterizer;

/**
 * Absolute Pearson correlation distance function for feature vectors.
 * 
 * The absolute Pearson correlation distance is computed from the Pearson
 * correlation coefficient <code>r</code> as: <code>1-abs(r)</code>.
 * 
 * The distance between two vectors will be low (near 0), if their attribute
 * values are dimension-wise strictly positively or negatively correlated, it
 * will be high (near 1), if their attribute values are dimension-wise
 * uncorrelated.
 * 
 * @author Erich Schubert
 * @since 0.7.0
 */
public class AbsolutePearsonCorrelationDistanceFunction extends AbstractNumberVectorDistanceFunction {
  /**
   * Static instance.
   */
  public static final AbsolutePearsonCorrelationDistanceFunction STATIC = new AbsolutePearsonCorrelationDistanceFunction();

  /**
   * Constructor - use {@link #STATIC} instead.
   * 
   * @deprecated Use static instance!
   */
  @Deprecated
  public AbsolutePearsonCorrelationDistanceFunction() {
    super();
  }

  /**
   * Computes the absolute Pearson correlation distance for two given feature
   * vectors.
   * 
   * The absolute Pearson correlation distance is computed from the Pearson
   * correlation coefficient <code>r</code> as: <code>1-abs(r)</code>. Hence,
   * possible values of this distance are between 0 and 1.
   * 
   * @param v1 first feature vector
   * @param v2 second feature vector
   * @return the absolute Pearson correlation distance for two given feature
   *         vectors v1 and v2
   */
  @Override
  public double distance(NumberVector v1, NumberVector v2) {
    return 1 - Math.abs(PearsonCorrelation.coefficient(v1, v2));
  }

  @Override
  public String toString() {
    return "AbsolutePearsonCorrelationDistance";
  }

  @Override
  public boolean equals(Object obj) {
    return obj != null && this.getClass().equals(obj.getClass());
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
    protected AbsolutePearsonCorrelationDistanceFunction makeInstance() {
      return AbsolutePearsonCorrelationDistanceFunction.STATIC;
    }
  }
}