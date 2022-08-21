/*
 * This file is part of ELKI:
 * Environment for Developing KDD-Applications Supported by Index-Structures
 * 
 * Copyright (C) 2022
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
package elki.index.tree.spatial.kd;

import elki.data.NumberVector;
import elki.distance.minkowski.EuclideanDistance;

/**
 * Partial distance computations for Euclidean distance.
 * 
 * @author Erich Schubert
 * @since 0.8.0
 */
public class PartialEuclideanDistance implements PartialDistance<NumberVector> {
  /**
   * Static instance
   */
  public final static PartialEuclideanDistance STATIC = new PartialEuclideanDistance();

  /**
   * Constructor. Use {@link #STATIC} instead.
   */
  @Deprecated
  private PartialEuclideanDistance() {
    super();
  }

  @Override
  public double combineRaw(double rawdist, double delta, double prevdelta) {
    return rawdist + delta * delta - prevdelta * prevdelta;
  }

  @Override
  public boolean compareRawRegular(double raw, double reg) {
    return reg == Double.POSITIVE_INFINITY || raw <= reg * reg;
  }

  @Override
  public double distance(NumberVector a, NumberVector b) {
    return EuclideanDistance.STATIC.distance(a, b);
  }

  @Override
  public double transformOut(double rawdist) {
    return Math.sqrt(rawdist);
  }
}
