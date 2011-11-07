package experimentalcode.shared.index.subspace.vafile;

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

import java.util.Arrays;

import de.lmu.ifi.dbs.elki.database.ids.DBID;

/**
 * DBObject
 * 
 * @author Thomas Bernecker
 * @created 16.09.2009
 * @date 16.09.2009
 */
public class VectorApprox {
  /**
   * approximation (va cell ids)
   */
  int[] approximation;

  protected DBID id;

  public VectorApprox(int dimensions) {
    approximation = new int[dimensions];
    Arrays.fill(approximation, -1);
  }

  public VectorApprox(DBID id, int dimensions) {
    this(dimensions);
    this.id = id;
  }

  /**
   * @return the id
   */
  public DBID getId() {
    return id;
  }

  public int getApproximationSize() {
    return approximation.length;
  }

  public int getApproximation(int dim) {
    return approximation[dim];
  }

  protected boolean approximationIsSet(int dim) {
    return approximation[dim] != -1;
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
    return numberOfDimensions * (int) (Math.ceil((Math.log(numberOfPartitions) / Math.log(2)) / 8));
  }
}