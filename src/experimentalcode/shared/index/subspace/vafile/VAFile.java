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
import de.lmu.ifi.dbs.elki.utilities.pairs.DoubleObjPair;

/**
 * Vector-approximation file (VAFile)
 * 
 * @author Thomas Bernecker
 * @author Erich Schubert
 */
public class VAFile<V extends NumberVector<?, ?>> {
  /**
   * Logging class
   */
  Logging log = Logging.getLogger(VAFile.class);

  // Full data representation
  Relation<V> relation;

  // temporary, full-dimensional VA representation
  private List<VectorApprox> vectorApprox;

  private static final int p = 2;

  int bufferSize = Integer.MAX_VALUE;
  
  int pageSize;

  private double[][] splitPositions;

  private int partitions;

  private int scanPageAccesses;

  private int refinements;

  public VAFile(int pageSize, Relation<V> relation, int partitions) {
    this.pageSize = pageSize;

    refinements = 0;
    scanPageAccesses = 0;

    setPartitions(relation, partitions);

    vectorApprox = new ArrayList<VectorApprox>();
    for(DBID id : relation.getDBIDs()) {
      V dv = relation.get(id);
      VectorApprox va = calculateApproximation(id, dv, splitPositions);
      vectorApprox.add(va);
    }
  }

  public int getPageAccess() {
    return refinements + scanPageAccesses;
  }

  public void setPartitions(Relation<V> objects, int partitions) throws IllegalArgumentException {
    if((Math.log(partitions) / Math.log(2)) != (int) (Math.log(partitions) / Math.log(2))) {
      throw new IllegalArgumentException("Number of partitions must be a power of 2!");
    }

    final int dimensions = DatabaseUtil.dimensionality(objects);
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
      // make sure that last object will be included
      splitPositions[d][partitions] = tempdata[size - 1] + 0.000001;

      if(log.isDebuggingFinest()) {
        log.finest("dim " + (d + 1) + ": ");
        for(int b = 0; b < splitPositions[d].length; b++) {
          log.finest(splitPositions[d][b] + "  ");
          if(b < splitPositions[d].length - 1) {
            log.finest("(bucket " + (b + 1) + "/" + partitions + ", " + partitionCount[d][b] + ")  ");
          }
        }
      }
    }
  }

  public double[] getMinDists(double[][] lookup, int dimension, int queryCell) {
    double[] result = new double[splitPositions[dimension].length - 1];
    for(int i = 0; i < result.length; i++) {
      if(i < queryCell) {
        result[i] = lookup[dimension][i + 1];
      }
      else if(i > queryCell) {
        result[i] = lookup[dimension][i];
      }
      else {
        result[i] = 0;
      }
    }
    return result;
  }

  public double[] getMaxDists(double[][] lookup, int dimension, int queryCell) {
    double[] result = new double[splitPositions[dimension].length - 1];
    for(int i = 0; i < result.length; i++) {
      if(i < queryCell) {
        result[i] = lookup[dimension][i];
      }
      else if(i > queryCell) {
        result[i] = lookup[dimension][i + 1];
      }
      else {
        result[i] = Math.max(lookup[dimension][i], lookup[dimension][i + 1]);
      }
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
    VectorApprox queryApprox = calculateApproximation(null, query, splitPositions);
    double[][] lookup = makeLookupTable(query);

    // Estimate scan costs.
    final int newBytesScanned = vectorApprox.size() * VectorApprox.byteOnDisk(query.getDimensionality(), partitions);
    scanPageAccesses += (int) Math.ceil(((double) newBytesScanned) / pageSize);

    // Heap for the kth smallest maximum distance
    Heap<Double> minMaxHeap = new TopBoundedHeap<Double>(k, Collections.reverseOrder());
    double minMaxDist = Double.POSITIVE_INFINITY;
    // Candidates with minDist <= kth maxDist
    ArrayList<DoubleObjPair<DBID>> candidates = new ArrayList<DoubleObjPair<DBID>>(vectorApprox.size());

    // Approximation step
    for(int i = 0; i < vectorApprox.size(); i++) {
      VectorApprox va = vectorApprox.get(i);
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
      candidates.add(new DoubleObjPair<DBID>(minDist, va.id));

      // Update candidate pruning heap
      minMaxHeap.add(maxDist);
      if(minMaxHeap.size() >= k) {
        minMaxDist = minMaxHeap.peek();
      }
    }
    // sort candidates by lower bound (minDist)
    Collections.sort(candidates);

    // refinement step
    KNNHeap<DoubleDistance> result = new KNNHeap<DoubleDistance>(k);

    // log.fine("candidates size " + candidates.size());
    // retrieve accurate distances
    for(DoubleObjPair<DBID> va : candidates) {
      // Stop when we are sure to have all elements
      if(result.size() >= k) {
        double kDist = result.getKNNDistance().doubleValue();
        if(va.first > kDist) {
          break;
        }
      }

      // refine the next element
      V dv = relation.get(va.second);
      double dist = 0;
      for(int d = 0; d < dv.getDimensionality(); d++) {
        dist += Math.pow(dv.doubleValue(d + 1) - query.doubleValue(d + 1), p);
      }
      dist = Math.pow(dist, 1.0 / p);
      result.add(new DoubleDistanceResultPair(dist, va.second));
    }
    if (log.isDebuggingFinest()) {
      log.finest("query = (" + query + ")");
      log.finest("database: " + vectorApprox.size() + ", candidates: " + candidates.size() + ", results: " + result.size());
    }

    return result.toKNNList();
  }

  public VectorApprox calculateApproximation(DBID id, V dv, double[][] borders) {
    VectorApprox va = new VectorApprox(id, dv.getDimensionality());
    for(int d = 0; d < borders.length; d++) {
      double val = dv.doubleValue(d + 1);
      int lastBorderIndex = borders[d].length - 1;
  
      // value is lower outlier
      if(val < borders[d][0]) {
        va.approximation[d] = 0;
        System.err.println("Epsilon value " + val + " is set to first partition.");
      } // value is upper outlier
      else if(val > borders[d][lastBorderIndex]) {
        va.approximation[d] = lastBorderIndex - 1;
        System.err.println("Epsilon value " + val + " is set to last partition.");
      } // normal case
      else {
        for(int s = 0; s < lastBorderIndex; s++) {
          if(val >= borders[d][s] && val < borders[d][s + 1] && !va.approximationIsSet(d)) {
            va.approximation[d] = s;
          }
        }
      }
    }
    return va;
  }
}