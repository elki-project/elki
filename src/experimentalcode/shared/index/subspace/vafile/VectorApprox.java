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
import java.util.Collections;
import java.util.Comparator;
import java.util.Vector;

import de.lmu.ifi.dbs.elki.data.NumberVector;
import de.lmu.ifi.dbs.elki.database.ids.DBID;

/**
 * DBObject
 * 
 * @author Thomas Bernecker
 * @created 16.09.2009
 * @date 16.09.2009
 */
public class VectorApprox<V extends NumberVector<V, ?>> {

  /**
   * approximation (va cell ids)
   */
  // Hashtable<Integer, Integer> approximation;
  int[] approximation;

  /**
   * (!) p-dists (default: squared)
   */
  private double minDist;

  private double maxDist;

  private DBID id;

  public VectorApprox(int dimensions) {
    approximation = new int[dimensions];
    Arrays.fill(approximation, -1);

    minDist = 0;
    maxDist = 0;
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

  public void calculateApproximation(V dv, DAFile<V>[] daFiles) {
    for(int i = 0; i < daFiles.length; i++) {
      double val = dv.doubleValue(i + 1);
      double[] borders = daFiles[i].getSplitPositions();
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
          if(val >= borders[s] && val < borders[s + 1] && !approximationIsSet(i)) {
            approximation[i] = s;
          }
        }
      }
    }
  }

  public void calculateApproximation(V dv, double[][] borders) {
    for(int d = 0; d < borders.length; d++) {
      double val = dv.doubleValue(d + 1);
      int lastBorderIndex = borders[d].length - 1;

      // value is lower outlier
      if(val < borders[d][0]) {
        approximation[d] = 0;
        System.err.println("Epsilon value " + val + " is set to first partition.");
      } // value is upper outlier
      else if(val > borders[d][lastBorderIndex]) {
        approximation[d] = lastBorderIndex - 1;
        System.err.println("Epsilon value " + val + " is set to last partition.");
      } // normal case
      else {
        for(int s = 0; s < lastBorderIndex; s++) {
          if(val >= borders[d][s] && val < borders[d][s + 1] && !approximationIsSet(d)) {
            approximation[d] = s;
          }
        }
      }
    }
  }

  public int getApproximationSize() {
    return approximation.length;
  }

  public int getApproximation(int dim) {
    return approximation[dim];
  }

  private boolean approximationIsSet(int dim) {
    return approximation[dim] != -1;
  }
  
  public void resetPDists()
  {
	  minDist = 0;
	  maxDist = 0;
  }

  public void resetPMinDist() {
    minDist = 0;
  }

  public void increasePMinDist(double val) {
    minDist += val;
  }

  public double getPMinDist() {
    return minDist;
  }

  public void resetPMaxDist() {
    maxDist = 0;
  }

  public void increasePMaxDist(double val) {
    maxDist += val;
  }

  public void decreasePMaxDist(double val) {
    maxDist -= val;
  }

  public double getPMaxDist() {
    return maxDist;
  }

  public String toString() {

    return id + " (" + Arrays.toString(approximation) + "), squared bounds: [" + this.getPMinDist() + ", " + this.getPMaxDist() + "]";
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

  public static <V extends NumberVector<V, ?>> Vector<VectorApprox<V>> sortByMinDist(Vector<VectorApprox<V>> vectorApprox) {
    Collections.sort(vectorApprox, new MinDistComparator<V>());
    return vectorApprox;
  }
}

class MinDistComparator<V extends NumberVector<V, ?>> implements Comparator<VectorApprox<V>> {

  /*
   * (non-Javadoc)
   * 
   * @see java.util.Comparator#compare(java.lang.Object, java.lang.Object)
   */
  @Override
  public int compare(VectorApprox<V> a, VectorApprox<V> b) {
    return Double.compare(a.getPMinDist(), b.getPMinDist());
  }
}
