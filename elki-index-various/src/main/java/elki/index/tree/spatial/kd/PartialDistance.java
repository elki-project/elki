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

/**
 * Interface to maintain partial distance bounds.
 * 
 * @author Erich Schubert
 * @since 0.8.0
 */
public interface PartialDistance<O> {
  /**
   * Combine the distance information.
   *
   * @param rawdist Raw (often squared) previous distance
   * @param delta New delta
   * @param prevdelta Previous delta
   * @return New raw distance
   */
  double combineRaw(double rawdist, double delta, double prevdelta);

  /**
   * Check if a "raw" distance is less or equal a regular distance.
   * 
   * @param raw Raw distance (usually squared)
   * @param reg Regular distance
   * @return True if less than or equal
   */
  boolean compareRawRegular(double raw, double reg);

  /**
   * Compute the actual distance of two objects.
   * 
   * @param a First
   * @param b Second
   * @return Distance
   */
  double distance(O a, O b);

  /**
   * Transform a raw distance to an output (external) distance.
   * 
   * @param rawdist Raw distance
   * @return External distance
   */
  double transformOut(double rawdist);
}
