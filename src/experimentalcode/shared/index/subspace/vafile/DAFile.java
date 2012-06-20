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
import java.util.List;

import de.lmu.ifi.dbs.elki.data.NumberVector;
import de.lmu.ifi.dbs.elki.database.ids.DBID;
import de.lmu.ifi.dbs.elki.database.relation.Relation;
import de.lmu.ifi.dbs.elki.index.vafile.VectorApproximation;
import de.lmu.ifi.dbs.elki.math.linearalgebra.Vector;
import de.lmu.ifi.dbs.elki.utilities.documentation.Reference;
import de.lmu.ifi.dbs.elki.utilities.pairs.DoubleObjPair;

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
// FIXME: does not allow multiple queries in parallel, because of the selectivity coefficient handling!
@Reference(authors = "Hans-Peter Kriegel, Peer Kröger, Matthias Schubert, Ziyue Zhu", title = "Efficient Query Processing in Arbitrary Subspaces Using Vector Approximations", booktitle = "Proc. 18th Int. Conf. on Scientific and Statistical Database Management (SSDBM 06), Wien, Austria, 2006", url = "http://dx.doi.org/10.1109/SSDBM.2006.23")
public class DAFile {
  /**
   * Dimension of this approximation file
   */
  private int dimension;

  /**
   * Splitting grid
   */
  private double[] splitPositions;

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
    for(DBID id : relation.iterDBIDs()) {
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
   * @param dimension the dimension to set
   */
  public void setDimension(int dimension) {
    this.dimension = dimension;
  }

  /**
   * Estimate the IO costs for this index.
   * 
   * @return IO costs
   */
  public int getIOCosts() {
    return splitPositions.length * 8 + 4;
  }

  /**
   * 
   * @param daFileList
   * @param query
   * @param epsilon
   */
  public static void calculateSelectivityCoeffs(List<DoubleObjPair<DAFile>> daFiles, NumberVector<?, ?> query, double epsilon) {
    final int dimensions = query.getDimensionality();
    double[] lowerVals = new double[dimensions];
    double[] upperVals = new double[dimensions];

    VectorApproximation queryApprox = calculateApproximation(null, query, daFiles);

    for(int i = 0; i < dimensions; i++) {
      lowerVals[i] = query.doubleValue(i + 1) - epsilon;
      upperVals[i] = query.doubleValue(i + 1) + epsilon;
    }

    Vector lowerEpsilon = new Vector(lowerVals);
    VectorApproximation lowerEpsilonPartitions = calculateApproximation(null, lowerEpsilon, daFiles);

    Vector upperEpsilon = new Vector(upperVals);
    VectorApproximation upperEpsilonPartitions = calculateApproximation(null, upperEpsilon, daFiles);

    for(int i = 0; i < daFiles.size(); i++) {
      int coeff = (queryApprox.getApproximation(i) - lowerEpsilonPartitions.getApproximation(i)) + (upperEpsilonPartitions.getApproximation(i) - queryApprox.getApproximation(i)) + 1;
      daFiles.get(i).first = coeff;
    }
  }

  public static VectorApproximation calculateApproximation(DBID id, NumberVector<?, ?> dv, List<DoubleObjPair<DAFile>> daFiles) {
    int[] approximation = new int[dv.getDimensionality()];
    for(int i = 0; i < daFiles.size(); i++) {
      double val = dv.doubleValue(i + 1);
      double[] borders = daFiles.get(i).second.getSplitPositions();
      assert borders != null : "borders are null";
      int lastBorderIndex = borders.length - 1;

      // value is lower outlier
      if(val < borders[0]) {
        approximation[i] = 0;
      } // value is upper outlier
      else if(val > borders[lastBorderIndex]) {
        approximation[i] = lastBorderIndex - 1;
      } // normal case
      else {
        for(int s = 0; s < lastBorderIndex; s++) {
          if(val >= borders[s] && val < borders[s + 1] && approximation[i] != -1) {
            approximation[i] = s;
          }
        }
      }
    }
    return new VectorApproximation(id, approximation);
  }
}