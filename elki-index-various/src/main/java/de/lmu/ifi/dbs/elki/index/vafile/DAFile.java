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

import de.lmu.ifi.dbs.elki.data.NumberVector;
import de.lmu.ifi.dbs.elki.database.ids.DBIDIter;
import de.lmu.ifi.dbs.elki.database.relation.Relation;
import de.lmu.ifi.dbs.elki.utilities.documentation.Reference;
import de.lmu.ifi.dbs.elki.utilities.io.ByteArrayUtil;

/**
 * Dimension approximation file, a one-dimensional part of the
 * {@link PartialVAFile}.
 * <p>
 * Reference:
 * <p>
 * Hans-Peter Kriegel, Peer Kröger, Matthias Schubert, Ziyue Zhu<br>
 * Efficient Query Processing in Arbitrary Subspaces Using Vector
 * Approximations<br>
 * Proc. 18th Int. Conf. on Scientific and Statistical Database Management
 * (SSDBM 06)
 *
 * @author Thomas Bernecker
 * @author Erich Schubert
 * @since 0.5.0
 */
@Reference(authors = "Hans-Peter Kriegel, Peer Kröger, Matthias Schubert, Ziyue Zhu", //
    title = "Efficient Query Processing in Arbitrary Subspaces Using Vector Approximations", //
    booktitle = "Proc. 18th Int. Conf. on Scientific and Statistical Database Management (SSDBM 06)", //
    url = "https://doi.org/10.1109/SSDBM.2006.23", //
    bibkey = "DBLP:conf/ssdbm/KriegelKSZ06")
public class DAFile {
  /**
   * Dimension of this approximation file.
   */
  private final int dimension;

  /**
   * Splitting grid.
   */
  private final double[] splitPositions;

  /**
   * Constructor.
   * 
   * @param relation Relation to index
   * @param dimension Dimension of this file
   * @param partitions Number of partitions
   */
  public DAFile(Relation<? extends NumberVector> relation, int dimension, int partitions) {
    final int size = relation.size();
    this.dimension = dimension;
    this.splitPositions = new double[partitions + 1];

    double[] tempdata = new double[size];
    int j = 0;
    for(DBIDIter iditer = relation.iterDBIDs(); iditer.valid(); iditer.advance()) {
      tempdata[j] = relation.get(iditer).doubleValue(dimension);
      j += 1;
    }
    Arrays.sort(tempdata);

    for(int b = 0; b < partitions; b++) {
      int start = (int) (b * size / (double) partitions);
      splitPositions[b] = tempdata[start];
    }
    // make sure that last object will be included
    splitPositions[partitions] = tempdata[size - 1] + 0.000001;
  }

  /**
   * Return the split positions.
   * 
   * @return the split positions
   */
  public double[] getSplitPositions() {
    return splitPositions;
  }

  /**
   * Return the dimension we indexed.
   * 
   * @return the dimension
   */
  public int getDimension() {
    return dimension;
  }

  /**
   * Estimate the IO costs for this index.
   * 
   * @return IO costs
   */
  public int getIOCosts() {
    return splitPositions.length * ByteArrayUtil.SIZE_DOUBLE + 4;
  }
}
