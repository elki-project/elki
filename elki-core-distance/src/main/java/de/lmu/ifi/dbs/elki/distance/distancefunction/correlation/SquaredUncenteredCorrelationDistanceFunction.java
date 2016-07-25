package de.lmu.ifi.dbs.elki.distance.distancefunction.correlation;

/*
 This file is part of ELKI:
 Environment for Developing KDD-Applications Supported by Index-Structures

 Copyright (C) 2015
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
import de.lmu.ifi.dbs.elki.distance.distancefunction.AbstractNumberVectorDistanceFunction;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.AbstractParameterizer;

/**
 * Squared uncentered correlation distance function for feature vectors.
 * 
 * This is highly similar to {@link SquaredPearsonCorrelationDistanceFunction},
 * but uses a fixed mean of 0 instead of the sample mean.
 * 
 * @author Erich Schubert
 * @since 0.4.0
 */
public class SquaredUncenteredCorrelationDistanceFunction extends AbstractNumberVectorDistanceFunction {
  /**
   * Static instance.
   */
  public static final SquaredUncenteredCorrelationDistanceFunction STATIC = new SquaredUncenteredCorrelationDistanceFunction();

  /**
   * Constructor - use {@link #STATIC} instead.
   * 
   * @deprecated Use static instance!
   */
  @Deprecated
  public SquaredUncenteredCorrelationDistanceFunction() {
    super();
  }

  @Override
  public double distance(NumberVector v1, NumberVector v2) {
    final double pcc = UncenteredCorrelationDistanceFunction.uncenteredCorrelation(v1, v2);
    return 1. - pcc * pcc;
  }

  @Override
  public String toString() {
    return "SquaredUncenteredCorrelationDistanceFunction";
  }

  @Override
  public boolean equals(Object obj) {
    if(obj == null) {
      return false;
    }
    return this.getClass().equals(obj.getClass());
  }

  /**
   * Parameterization class.
   * 
   * @author Erich Schubert
   * 
   * @apiviz.exclude
   */
  public static class Parameterizer extends AbstractParameterizer {
    @Override
    protected SquaredUncenteredCorrelationDistanceFunction makeInstance() {
      return SquaredUncenteredCorrelationDistanceFunction.STATIC;
    }
  }
}