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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Vector;

import de.lmu.ifi.dbs.elki.data.NumberVector;
import de.lmu.ifi.dbs.elki.database.ids.DBID;
import de.lmu.ifi.dbs.elki.database.query.DoubleDistanceResultPair;
import de.lmu.ifi.dbs.elki.database.relation.Relation;
import de.lmu.ifi.dbs.elki.distance.distancevalue.DoubleDistance;
import de.lmu.ifi.dbs.elki.logging.Logging;
import de.lmu.ifi.dbs.elki.utilities.DatabaseUtil;
import de.lmu.ifi.dbs.elki.utilities.datastructures.heap.Heap;
import de.lmu.ifi.dbs.elki.utilities.datastructures.heap.KNNHeap;
import de.lmu.ifi.dbs.elki.utilities.datastructures.heap.KNNList;
import de.lmu.ifi.dbs.elki.utilities.datastructures.heap.TopBoundedHeap;
import experimentalcode.shared.index.subspace.structures.DiskMemory;

/**
 * VAFile
 * 
 * @author Thomas Bernecker
 * @created 15.09.2009
 * @date 15.09.2009
 */
public class VAFile<V extends NumberVector<?, ?>> {
  Logging log = Logging.getLogger(VAFile.class);

  // Full data representation
  DiskMemory<V> data;

  // temporary, full-dimensional VA representation
  private List<VectorApprox<V>> vectorApprox;

  private static final int p = 2;

  int initialisations = 10, swaps = 10;

  int bufferSize = Integer.MAX_VALUE;

  private long scannedBytes;

  int pageSize;

  private double[][] splitPositions;

  private int partitions;

  private int scanPageAccesses;

  public VAFile(int pageSize, Relation<V> relation, int partitions) {

    this.pageSize = pageSize;

    scannedBytes = 0;
    scanPageAccesses = 0;

    setPartitions(relation, partitions);

    int dimensions = DatabaseUtil.dimensionality(relation);
    data = new DiskMemory<V>(pageSize / (8 * dimensions + 4), bufferSize);
    vectorApprox = new ArrayList<VectorApprox<V>>();
    for(DBID id : relation.getDBIDs()) {
      V dv = relation.get(id);
      data.add(id, dv);
      VectorApprox<V> va = new VectorApprox<V>(id, dv.getDimensionality());
      try {
        va.calculateApproximation(dv, splitPositions);
      }
      catch(Exception e) {
        e.printStackTrace();
      }
      vectorApprox.add(va);
    }

  }

  public int getPageAccess() {
    return data.pageAccessesL + scanPageAccesses;
  }

  public void setPartitions(Relation<V> objects, int partitions) throws IllegalArgumentException {
    if((Math.log(partitions) / Math.log(2)) != (int) (Math.log(partitions) / Math.log(2)))
      throw new IllegalArgumentException("Number of partitions must be a power of 2!");

    int dimensions = DatabaseUtil.dimensionality(objects);
    splitPositions = new double[dimensions][partitions + 1];
    int[][] partitionCount = new int[dimensions][partitions];
    this.partitions = partitions;

    for(int d = 0; d < dimensions; d++) {
      int size = objects.size();
      int remaining = size;
      double[] tempdata = new double[size];
      int j = 0;
      for(DBID id : objects.getDBIDs()) {
        tempdata[j++] = objects.get(id).doubleValue(d + 1);
      }
      Arrays.sort(tempdata);

      int bucketSize = (int) (size / (double) partitions);
      int i = 0;
      for(int b = 0; b < partitions; b++) {
        assert i <= tempdata.length : "i out ouf bounds " + i + " <> " + tempdata.length;
        splitPositions[d][b] = tempdata[i];
        remaining -= bucketSize;
        i += bucketSize;

        // test: are there remaining objects that have to be put in the first
        // buckets?
        if(remaining > (bucketSize * (partitionCount.length - b - 1))) {
          i++;
          remaining--;
          partitionCount[d][b]++;
        }

        partitionCount[d][b] += bucketSize;
      }
      splitPositions[d][partitions] = tempdata[size - 1] + 0.000001; // make
                                                                     // sure
                                                                     // that
                                                                     // last
                                                                     // object
                                                                     // will be
                                                                     // included

      log.finest("dim " + (d + 1) + ": ");
      for(int b = 0; b < splitPositions[d].length; b++) {
        log.finest(splitPositions[d][b] + "  ");
        if(b < splitPositions[d].length - 1) {
          log.finest("(bucket " + (b + 1) + "/" + partitions + ", " + partitionCount[d][b] + ")  ");
        }
      }
      log.finest(null);
    }
  }

  /**
   * @return the split positions
   */
  public double[][] getSplitPositions() {
    return splitPositions;
  }

  public double[] getMinDists(double[][] lookup, int dimension, int queryCell) {
    double[] result = new double[splitPositions[dimension].length - 1];
    for(int i = 0; i < result.length; i++) {
      if(i < queryCell){
        result[i] = lookup[dimension][i + 1];}
      else if(i > queryCell){
        result[i] = lookup[dimension][i];}
      else{
        result[i] = 0;}
    }
    return result;
  }

  public double[] getMaxDists(double[][] lookup, int dimension, int queryCell) {
    double[] result = new double[splitPositions[dimension].length - 1];
    for(int i = 0; i < result.length; i++) {
      if(i < queryCell){
        result[i] = lookup[dimension][i];}
      else if(i > queryCell){
        result[i] = lookup[dimension][i + 1];}
      else{
        result[i] = Math.max(lookup[dimension][i], lookup[dimension][i + 1]);}
    }
    return result;
  }

  public double[][] makeLookupTable(V query) {
    int dimensions = splitPositions.length;
    int bordercount = splitPositions[0].length;
    double[][] lookup = new double[dimensions][bordercount];
    for(int d = 0; d < dimensions; d++) {
      for(int i = 0; i < bordercount; i++) {
        lookup[d][i] = Math.pow(splitPositions[d][i] - query.doubleValue(d + 1), p);
      }
    }
    return lookup;
  }

  public KNNList<DoubleDistance> knnQuery(V query, int k) {
    // generate query approximation and lookup table
    VectorApprox<V> queryApprox = new VectorApprox<V>(query.getDimensionality());
    queryApprox.calculateApproximation(query, splitPositions);
    double[][] lookup = makeLookupTable(query);

    // Estimate scan costs.
    final int newBytesScanned = vectorApprox.size() * VectorApprox.byteOnDisk(query.getDimensionality(), partitions);
    scanPageAccesses += (int) Math.ceil(((double) newBytesScanned) / pageSize);
    scannedBytes += newBytesScanned;

    // Heap for the kth smallest maximum distance
    Heap<Double> minMaxHeap = new TopBoundedHeap<Double>(k, Collections.reverseOrder());
    double minMaxDist = Double.POSITIVE_INFINITY;
    // Candidates with minDist <= kth maxDist
    Vector<VectorApprox<V>> candidates = new Vector<VectorApprox<V>>();

    // Approximation step
    for(int i = 0; i < vectorApprox.size(); i++) {
      VectorApprox<V> va = vectorApprox.get(i);
      va.resetPDists();
      double minDist = 0;
      double maxDist = 0;
      for(int d = 0; d < va.getApproximationSize(); d++) {
        int queryApproxDim = queryApprox.getApproximation(d);
        int vectorApproxDim = va.getApproximation(d);
        minDist += getMinDists(lookup, d, queryApproxDim)[vectorApproxDim];
        maxDist += getMaxDists(lookup, d, queryApproxDim)[vectorApproxDim];
      }
      minDist = Math.pow(minDist, 1.0 / p);
      maxDist = Math.pow(maxDist, 1.0 / p);

      // Skip excess candidate generation:
      if(minDist > minMaxDist) {
        continue;
      }
      va.increasePMinDist(minDist);
      va.increasePMaxDist(maxDist);
      candidates.add(va);

      // Update candidate pruning heap
      minMaxHeap.add(maxDist);
      if(minMaxHeap.size() >= k) {
        minMaxDist = minMaxHeap.peek();
      }
    }
    // sort candidates by lower bound (minDist)
    VectorApprox.sortByMinDist(candidates);

    // refinement step
    KNNHeap<DoubleDistance> result = new KNNHeap<DoubleDistance>(k);

    // log.fine("candidates size " + candidates.size());
    // retrieve accurate distances
    for(VectorApprox<V> va : candidates) {
      // Stop when we are sure to have all elements
      if(result.size() >= k) {
        double kDist = result.getKNNDistance().doubleValue();
        if(va.getPMinDist() > kDist) {
          break;
        }
      }

      // refine the next element
      V dv = data.getObject(va.getId());
      double dist = 0;
      for(int d = 0; d < dv.getDimensionality(); d++) {
        dist += Math.pow(dv.doubleValue(d + 1) - query.doubleValue(d + 1), p);
      }
      dist = Math.pow(dist, 1.0 / p);
      result.add(new DoubleDistanceResultPair(dist, va.getId()));
    }
    log.finest("\nquery = (" + query + ")");
    log.finest("database: " + vectorApprox.size() + ", candidates: " + candidates.size() + ", results: " + result.size());

    return result.toKNNList();
  }
}
