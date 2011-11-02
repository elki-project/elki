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
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import de.lmu.ifi.dbs.elki.data.NumberVector;
import experimentalcode.franz.utils.ArrayUtils;

/**
 * DAFile
 * 
 * @author Thomas Bernecker
 * @created 22.09.2009
 * @date 22.09.2009
 */
public class DAFile<V extends NumberVector<V, ?>> {
  private int dimension;

  private double[] splitPositions;

  private double[] lookup;

  private int selectivityCoeff;

  private static int p = 2;

  double[] maxDists, minDists;

  public DAFile(int dimension) {
    this.dimension = dimension;
    selectivityCoeff = -1;
  }

  public void setPartitions(Collection<V> objects, int partitions) {
    long start = System.currentTimeMillis();

    splitPositions = new double[partitions + 1];
    int[] partitionCount = new int[partitions];

    int size = objects.size();
    int remaining = size;
    double[] tempdata = new double[size];
    int j = 0;
    for(V dv : objects)
      tempdata[j++] = dv.doubleValue(dimension + 1);
    Arrays.sort(tempdata);
    tempdata = ArrayUtils.unique(tempdata, 1 / (100 * partitions));

    int bucketSize = (int) (size / (double) partitions);
    int i = 0;
    for(int b = 0; b < partitionCount.length; b++) {
      splitPositions[b] = tempdata[i];
      remaining -= bucketSize;
      i += bucketSize;

      // test: are there remaining objects that have to be put in the
      // first buckets?
      if(remaining > (bucketSize * (partitionCount.length - b - 1))) {
        i++;
        remaining--;
        partitionCount[b]++;
      }

      partitionCount[b] += bucketSize;
    }
    splitPositions[partitions] = tempdata[size - 1] + 0.000001; // make sure
                                                                // that
    // last object will
    // be included

    System.out.println("dimension " + dimension + " finished! (time: " + (System.currentTimeMillis() - start) + " ms)");

    assert splitPositions != null : "borders are null";
  }

  public void setPartitions(double[] borders) {
    this.splitPositions = borders;
  }

  /**
   * @return the split positions
   */
  public double[] getSplitPositions() {
    return splitPositions;
  }

  public double[] getMinDists(int queryCell) {
    return minDists;
  }

  public double[] getMaxDists(int queryCell) {
    return maxDists;
  }

  public double getMaxMaxDist(int queryCell) {
    return ArrayUtils.maxValue(getMaxDists(queryCell));
    // double[] maxDists = getMaxDists(queryCell);
    // double result = Double.NEGATIVE_INFINITY;
    // for (int i = 0; i < maxDists.length; i++)
    // {
    // result = Math.max(result, maxDists[i]);
    // }
    // return result;
  }

  public void setLookupTable(V query) {
    int bordercount = splitPositions.length;
    lookup = new double[bordercount];
    for(int i = 0; i < bordercount; i++) {
      lookup[i] = Math.pow(splitPositions[i] - query.doubleValue(dimension + 1), p);
    }

    int queryCellGlobal = -1;
    for(int i = 0; i < splitPositions.length; i++) {
      if(query.doubleValue(dimension + 1) < splitPositions[i])
        break;
      else
        queryCellGlobal++;
    }
    // maxdists
    maxDists = new double[splitPositions.length - 1];
    for(int i = 0; i < maxDists.length; i++) {
      if(i < queryCellGlobal)
        maxDists[i] = lookup[i];
      else if(i > queryCellGlobal)
        maxDists[i] = lookup[i + 1];
      else
        maxDists[i] = Math.max(lookup[i], lookup[i + 1]);
    }

    // mindists
    minDists = new double[splitPositions.length - 1];
    for(int i = 0; i < minDists.length; i++) {
      if(i < queryCellGlobal)
        minDists[i] = lookup[i + 1];
      else if(i > queryCellGlobal)
        minDists[i] = lookup[i];
      else
        minDists[i] = 0;
    }

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
   * @return the selectivityCoeff
   */
  public int getSelectivityCoeff() {
    return selectivityCoeff;
  }

  public void setSelectivityCoeff(int val) {
    selectivityCoeff = val;
  }

  public int getIOCosts() {
    return splitPositions.length * 8 + 4;
  }

  /**
   * 
   * @param daFileList
   * @param query
   * @param epsilon
   */
  public static <V extends NumberVector<V, ?>> void calculateSelectivityCoeffs(List<DAFile<V>> daFileList, V query, double epsilon) {
    @SuppressWarnings("unchecked")
    DAFile<V>[] daFiles = new DAFile[daFileList.size()];
    for(DAFile<V> da : daFileList)
      daFiles[da.getDimension()] = da;

    int dimensions = query.getDimensionality();
    double[] lowerVals = new double[dimensions];
    double[] upperVals = new double[dimensions];

    VectorApprox<V> queryApprox = new VectorApprox<V>(dimensions);
    queryApprox.calculateApproximation(query, daFiles);

    for(int i = 0; i < dimensions; i++) {
      lowerVals[i] = query.doubleValue(i + 1) - epsilon;
      upperVals[i] = query.doubleValue(i + 1) + epsilon;
    }

    V lowerEpsilon = query.newNumberVector(lowerVals);
    VectorApprox<V> lowerEpsilonPartitions = new VectorApprox<V>(dimensions);
    lowerEpsilonPartitions.calculateApproximation(lowerEpsilon, daFiles);

    V upperEpsilon = query.newNumberVector(upperVals);
    VectorApprox<V> upperEpsilonPartitions = new VectorApprox<V>(dimensions);
    upperEpsilonPartitions.calculateApproximation(upperEpsilon, daFiles);

    for(int i = 0; i < daFiles.length; i++) {
      int coeff = (queryApprox.getApproximation(i) - lowerEpsilonPartitions.getApproximation(i)) + (upperEpsilonPartitions.getApproximation(i) - queryApprox.getApproximation(i)) + 1;
      daFiles[i].setSelectivityCoeff(coeff);
    }
  }

  public static <V extends NumberVector<V, ?>> List<DAFile<V>> sortBySelectivity(List<DAFile<V>> daFiles) {
    Collections.sort(daFiles, new DAFileSelectivityComparator<V>());
    return daFiles;
  }

  static class DAFileSelectivityComparator<V extends NumberVector<V, ?>> implements Comparator<DAFile<V>> {
    @Override
    public int compare(DAFile<V> a, DAFile<V> b) {
      return Double.compare(a.getSelectivityCoeff(), b.getSelectivityCoeff());
    }
  }
}