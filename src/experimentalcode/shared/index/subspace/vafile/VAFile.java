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
import de.lmu.ifi.dbs.elki.distance.distancefunction.LPNormDistanceFunction;
import de.lmu.ifi.dbs.elki.distance.distancevalue.DoubleDistance;
import de.lmu.ifi.dbs.elki.logging.Logging;
import de.lmu.ifi.dbs.elki.persistent.PageFileStatistics;
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
public class VAFile<V extends NumberVector<?, ?>> implements PageFileStatistics {
  /**
   * Logging class
   */
  Logging log = Logging.getLogger(VAFile.class);

  /**
   * The full object relation we index (and need for refinement
   */
  Relation<V> relation;

  /**
   * Approximation index
   */
  private List<VectorApprox> vectorApprox;

  /**
   * Number of partitions.
   */
  private int partitions;

  /**
   * Quantile grid we use
   */
  private double[][] splitPositions;

  /**
   * Page size, for estimating the VA file size
   */
  int pageSize;

  /**
   * Number of scans we performed.
   */
  int scans;

  /**
   * Number of objects we refined.
   */
  int refinements;

  public VAFile(int pageSize, Relation<V> relation, int partitions) {
    super();
    this.partitions = partitions;
    this.pageSize = pageSize;
    this.relation = relation;
    this.refinements = 0;
    this.scans = 0;

    // Initialize index
    setPartitions();
    vectorApprox = new ArrayList<VectorApprox>();
    for(DBID id : relation.getDBIDs()) {
      vectorApprox.add(calculateApproximation(id, relation.get(id)));
    }
  }

  /**
   * Initialize the data set grid by computing quantiles.
   * 
   * @param objects Data relation
   * @param partitions Number of partitions to generate
   * @throws IllegalArgumentException
   */
  public void setPartitions() throws IllegalArgumentException {
    if((Math.log(partitions) / Math.log(2)) != (int) (Math.log(partitions) / Math.log(2))) {
      throw new IllegalArgumentException("Number of partitions must be a power of 2!");
    }

    final int dimensions = DatabaseUtil.dimensionality(relation);
    final int size = relation.size();
    splitPositions = new double[dimensions][partitions + 1];
    int[][] partitionCount = new int[dimensions][partitions];

    for(int d = 0; d < dimensions; d++) {
      int remaining = size;
      double[] tempdata = new double[size];
      int j = 0;
      for(DBID id : relation.iterDBIDs()) {
        tempdata[j] = relation.get(id).doubleValue(d + 1);
        j += 1;
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

  /**
   * Calculate the VA file position given the existing borders.
   * 
   * @param id Object ID
   * @param dv Data vector
   * @return Vector approximation
   */
  public VectorApprox calculateApproximation(DBID id, V dv) {
    VectorApprox va = new VectorApprox(id, dv.getDimensionality());
    for(int d = 0; d < splitPositions.length; d++) {
      final double val = dv.doubleValue(d + 1);
      final int lastBorderIndex = splitPositions[d].length - 1;

      // value is lower outlier
      if(val < splitPositions[d][0]) {
        va.approximation[d] = 0;
        log.warning("Vector outside of VAFile grid!");
      } // value is upper outlier
      else if(val > splitPositions[d][lastBorderIndex]) {
        va.approximation[d] = lastBorderIndex - 1;
        log.warning("Vector outside of VAFile grid!");
      } // normal case
      else {
        // Search grid position
        int pos = Arrays.binarySearch(splitPositions[d], val);
        pos = (pos >= 0) ? pos : ((-pos) - 2);
        va.approximation[d] = pos;
      }
    }
    return va;
  }

  /**
   * Run a KNN query on this index.
   * 
   * @param query Query vector
   * @param k Number of neighbors
   * @return Neighbor list
   */
  public KNNList<DoubleDistance> knnQuery(V query, int k) {
    // generate query approximation and lookup table
    VectorApprox queryApprox = calculateApproximation(null, query);

    // Exact distance function
    LPNormDistanceFunction exdist = new LPNormDistanceFunction(2.0);
    // Approximative distance function
    VALPNormDistance vadist = new VALPNormDistance(2.0, splitPositions, query, queryApprox);

    // Heap for the kth smallest maximum distance
    Heap<Double> minMaxHeap = new TopBoundedHeap<Double>(k, Collections.reverseOrder());
    double minMaxDist = Double.POSITIVE_INFINITY;
    // Candidates with minDist <= kth maxDist
    ArrayList<DoubleObjPair<DBID>> candidates = new ArrayList<DoubleObjPair<DBID>>(vectorApprox.size());

    // Count a VA file scan
    scans += 1;

    // Approximation step
    for(int i = 0; i < vectorApprox.size(); i++) {
      VectorApprox va = vectorApprox.get(i);
      double minDist = vadist.getMinDist(va);
      double maxDist = vadist.getMaxDist(va);

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
      result.add(new DoubleDistanceResultPair(exdist.doubleDistance(dv, query), va.second));
    }
    if(log.isDebuggingFinest()) {
      log.finest("query = (" + query + ")");
      log.finest("database: " + vectorApprox.size() + ", candidates: " + candidates.size() + ", results: " + result.size());
    }

    return result.toKNNList();
  }

  @Override
  public long getReadOperations() {
    int vasize = vectorApprox.size() * VectorApprox.byteOnDisk(splitPositions.length, partitions);
    vasize = (int) Math.ceil(((double) vasize) / pageSize);
    return refinements + vasize * scans;
  }

  @Override
  public long getWriteOperations() {
    int vasize = vectorApprox.size() * VectorApprox.byteOnDisk(splitPositions.length, partitions);
    vasize = (int) Math.ceil(((double) vasize) / pageSize);
    return vasize;
  }

  @Override
  public void resetPageAccess() {
    refinements = 0;
    scans = 0;
  }

  @Override
  public PageFileStatistics getInnerStatistics() {
    return null;
  }
}