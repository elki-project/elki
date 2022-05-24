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
import elki.distance.minkowski.LPNormDistance;

/**
 * Partial distance computations for Euclidean distance.
 * 
 * @author Erich Schubert
 */
public class PartialLPNormDistance implements PartialDistance<NumberVector> {
  /**
   * Inner distance.
   */
  private LPNormDistance dist;

  /**
   * Constructor.
   *
   * @param dist Distance
   */
  public PartialLPNormDistance(LPNormDistance dist) {
    this.dist = dist;
  }

  @Override
  public double combineRaw(double rawdist, double delta, double prevdelta) {
    return rawdist + Math.pow(Math.abs(delta), dist.getP()) - Math.pow(Math.abs(prevdelta), dist.getP());
  }

  @Override
  public boolean compareRawRegular(double raw, double reg) {
    return reg == Double.POSITIVE_INFINITY || raw <= Math.pow(Math.abs(reg), dist.getP());
  }

  @Override
  public double distance(NumberVector a, NumberVector b) {
    return dist.distance(a, b);
  }

  @Override
  public double transformOut(double rawdist) {
    return Math.pow(rawdist, dist.getInvP());
  }
}
