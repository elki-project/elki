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
package de.lmu.ifi.dbs.elki.index.vafile;

import java.util.Arrays;

import de.lmu.ifi.dbs.elki.database.ids.DBID;
import de.lmu.ifi.dbs.elki.utilities.io.ByteArrayUtil;
import net.jafama.FastMath;

/**
 * Object in a VA approximation.
 * 
 * @author Thomas Bernecker
 * @author Erich Schubert
 * @since 0.5.0
 */
public class VectorApproximation {
  /**
   * approximation (va cell ids)
   */
  int[] approximation;

  /**
   * Object represented by this approximation
   */
  protected DBID id;

  /**
   * Constructor.
   * 
   * @param id Object represented (may be <code>null</code> for query objects)
   * @param approximation Approximation
   */
  public VectorApproximation(DBID id, int[] approximation) {
    super();
    this.id = id;
    this.approximation = approximation;
  }

  /**
   * @return the id
   */
  public DBID getId() {
    return id;
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
  // nicht gleich in bytes umwandeln, sonst rundungsfehler erst nachdem *anzahl
  // objekte
  public static int byteOnDisk(int numberOfDimensions, int numberOfPartitions) {
    // (partition*dimension+id) alles in Bit 32bit für 4 byte id
    return (int) (Math.ceil(numberOfDimensions * ((FastMath.log(numberOfPartitions) / FastMath.log(2))) + 32) / ByteArrayUtil.SIZE_DOUBLE);
  }
}
