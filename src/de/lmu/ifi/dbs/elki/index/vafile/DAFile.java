package de.lmu.ifi.dbs.elki.index.vafile;

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

import de.lmu.ifi.dbs.elki.data.NumberVector;
import de.lmu.ifi.dbs.elki.database.ids.DBID;
import de.lmu.ifi.dbs.elki.database.ids.DBIDIter;
import de.lmu.ifi.dbs.elki.database.relation.Relation;
import de.lmu.ifi.dbs.elki.utilities.documentation.Reference;

/**
 * Dimension approximation file, a one-dimensional part of the
 * {@link PartialVAFile}.
 * 
 * Reference:
 * <p>
 * Hans-Peter Kriegel, Peer Kröger, Matthias Schubert, Ziyue Zhu:<br />
 * Efficient Query Processing in Arbitrary Subspaces Using Vector Approximations
 * <br />
 * in Proc. 18th Int. Conf. on Scientific and Statistical Database Management
 * (SSDBM 06), Wien, Austria, 2006.
 * </p>
 * 
 * @author Thomas Bernecker
 * @author Erich Schubert
 */
@Reference(authors = "Hans-Peter Kriegel, Peer Kröger, Matthias Schubert, Ziyue Zhu", title = "Efficient Query Processing in Arbitrary Subspaces Using Vector Approximations", booktitle = "Proc. 18th Int. Conf. on Scientific and Statistical Database Management (SSDBM 06), Wien, Austria, 2006", url = "http://dx.doi.org/10.1109/SSDBM.2006.23")
public class DAFile {
  /**
   * Dimension of this approximation file
   */
  final private int dimension;

  /**
   * Splitting grid
   */
  final private double[] splitPositions;

  /**
   * Constructor.
   * 
   * @param dimension Dimension of this file
   */
  public DAFile(Relation<? extends NumberVector<?, ?>> relation, int dimension, int partitions) {
    final int size = relation.size();
    this.dimension = dimension;
    this.splitPositions = new double[partitions + 1];

    double[] tempdata = new double[size];
    int j = 0;
    for(DBIDIter iditer = relation.iterDBIDs(); iditer.valid(); iditer.advance()) {
      DBID id  = iditer.getDBID();
      tempdata[j] = relation.get(id).doubleValue(dimension + 1);
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
   * @return the split positions
   */
  public double[] getSplitPositions() {
    return splitPositions;
  }

  /**
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
    return splitPositions.length * 8 + 4;
  }
}