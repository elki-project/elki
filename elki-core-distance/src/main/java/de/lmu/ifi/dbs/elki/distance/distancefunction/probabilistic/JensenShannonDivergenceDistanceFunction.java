/*
 * This file is part of ELKI:
 * Environment for Developing KDD-Applications Supported by Index-Structures
 *
 * Copyright (C) 2017
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
import de.lmu.ifi.dbs.elki.utilities.optionhandling.AbstractParameterizer;

/**
 * Jensen-Shannon Divergence is essentially the same as Jeffrey divergence, only
 * scaled by half.
 * 
 * @author Erich Schubert
 * @since 0.6.0
 */
public class JensenShannonDivergenceDistanceFunction extends JeffreyDivergenceDistanceFunction {
  /**
   * Static instance. Use this!
   */
  public static final JensenShannonDivergenceDistanceFunction STATIC = new JensenShannonDivergenceDistanceFunction();

  /**
   * Constructor for the Jensen-Shannon divergence.
   *
   * @deprecated Use static instance!
   */
  @Deprecated
  public JensenShannonDivergenceDistanceFunction() {
    super();
  }

  @Override
  public boolean isSquared() {
    return true;
  }
  
  @Override
  public double distance(NumberVector v1, NumberVector v2) {
    return .5 * super.distance(v1, v2);
  }

  @Override
  public double minDist(SpatialComparable mbr1, SpatialComparable mbr2) {
    return .5 * super.minDist(mbr1, mbr2);
  }

  @Override
  public String toString() {
    return "JensenShannonDivergenceDistance";
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
   * 
   * @apiviz.exclude
   */
  public static class Parameterizer extends AbstractParameterizer {
    @Override
    protected JensenShannonDivergenceDistanceFunction makeInstance() {
      return STATIC;
    }
  }
}
