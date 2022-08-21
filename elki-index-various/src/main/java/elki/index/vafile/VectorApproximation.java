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
package elki.index.vafile;

import java.util.Arrays;

import elki.database.ids.DBIDRef;
import elki.math.MathUtil;

import net.jafama.FastMath;

/**
 * Object in a VA approximation.
 * 
 * @author Thomas Bernecker
 * @author Erich Schubert
 * @since 0.5.0
 */
public class VectorApproximation implements DBIDRef {
  /**
   * approximation (va cell ids)
   */
  int[] approximation;

  /**
   * Object id.
   */
  int id;

  /**
   * Constructor.
   * 
   * @param id Object represented (may be <code>null</code> for query objects)
   * @param approximation Approximation
   */
  public VectorApproximation(DBIDRef id, int[] approximation) {
    super();
    this.id = id != null ? id.internalGetIndex() : Integer.MIN_VALUE;
    this.approximation = approximation;
  }

  /**
   * Get the dimensionality
   * 
   * @return Dimensionality
   */
  public int getDimensionality() {
    return approximation.length;
  }

  /**
   * Get the VA approximation
   * 
   * @param dim Dimension
   * @return Bin number
   */
  public int getApproximation(int dim) {
    return approximation[dim];
  }
  
  @Override
  public int internalGetIndex() {
    return id;
  }

  @Override
  public String toString() {
    return id + " (" + Arrays.toString(approximation) + ")";
  }

  /**
   * Computes IO costs (in bytes) needed for reading the candidates. For one
   * object, log2(numberOfPartitions) bits have to be read per dimension.
   * 
   * @param numberOfDimensions the number of relevant dimensions
   * @param numberOfPartitions the number of relevant partitions
   * @return the cost values (in bytes)
   */
  public static int byteOnDisk(int numberOfDimensions, int numberOfPartitions) {
    // (partition*dimension+id) alles in Bit 32bit für 4 byte id
    return numberOfDimensions * ((int) Math.ceil(FastMath.log(numberOfPartitions) * MathUtil.ONE_BY_LOG2) + 4);
  }
}
