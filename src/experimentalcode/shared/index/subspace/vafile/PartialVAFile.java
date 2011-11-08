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
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;
import java.util.Vector;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;

import de.lmu.ifi.dbs.elki.data.NumberVector;
import de.lmu.ifi.dbs.elki.database.ids.ArrayDBIDs;
import de.lmu.ifi.dbs.elki.database.ids.ArrayModifiableDBIDs;
import de.lmu.ifi.dbs.elki.database.ids.DBID;
import de.lmu.ifi.dbs.elki.database.ids.DBIDUtil;
import de.lmu.ifi.dbs.elki.database.ids.DBIDs;
import de.lmu.ifi.dbs.elki.database.ids.ModifiableDBIDs;
import de.lmu.ifi.dbs.elki.database.query.DistanceResultPair;
import de.lmu.ifi.dbs.elki.database.query.DoubleDistanceResultPair;
import de.lmu.ifi.dbs.elki.database.relation.Relation;
import de.lmu.ifi.dbs.elki.distance.distancevalue.DoubleDistance;
import de.lmu.ifi.dbs.elki.logging.Logging;
import de.lmu.ifi.dbs.elki.utilities.datastructures.heap.Heap;
import de.lmu.ifi.dbs.elki.utilities.datastructures.heap.TopBoundedHeap;
import experimentalcode.shared.index.subspace.IndexStatistics;
import experimentalcode.shared.index.subspace.SubSpace;
import experimentalcode.shared.index.subspace.SubspaceIndex;
import experimentalcode.shared.index.subspace.SubspaceIndexName;

/**
 * PartialVAFile
 * 
 * @author Thomas Bernecker
 * @created 15.09.2009
 * @date 15.09.2009
 */
public class PartialVAFile<V extends NumberVector<V, ?>> implements SubspaceIndex<V> {
  Logging log = Logging.getLogger(PartialVAFile.class);

  boolean insertedData = false;

  // Full data representation
  Relation<V> relation;

  // temporary, full-dimensional VA representation
  private List<PartialVectorApproximation<V>> vectorApprox;

  // VA dimensions
  // FIXME: use ArrayList<DAFile<V>>, better with generics!
  DAFile<V>[] daFiles;

  private int partitions;

  private static final int p = 2;

  int initialisations = 10, swaps = 10;

  int bufferSize = Integer.MAX_VALUE;

  private long scannedBytes, queryTime;

  private int issuedQueries;
  
  int refinements = 0;

  int pageSize;

  private int[] prunedVectors;

  private int currentSubspaceDims;

  public PartialVAFile(int pageSize, Relation<V> fullDimensionalData, int partitions, int bufferSize) {
    this.bufferSize = bufferSize;
    this.pageSize = pageSize;
    this.scannedBytes = 0;
    this.issuedQueries = 0;
    this.refinements = 0;
    this.partitions = partitions;
    currentSubspaceDims = -1;

    insert(fullDimensionalData);
  }

  public PartialVAFile(int pageSize, Relation<V> fullDimensionalData, int partitions) {
    this(pageSize, fullDimensionalData, partitions, Integer.MAX_VALUE);
  }

  @Override
  public void insert(Relation<V> fullDimensionalData) throws IllegalStateException {
    if(insertedData) {
      throw new IllegalStateException("Data already inserted.");
    }

    DBID sampleID = fullDimensionalData.getDBIDs().iterator().next();
    int dimensions = fullDimensionalData.get(sampleID).getDimensionality();
    daFiles = new DAFile[dimensions];
    for(int d = 0; d < dimensions; d++) {
      daFiles[d] = new DAFile<V>(d);
    }

    setPartitions(fullDimensionalData, this.partitions);

    vectorApprox = new ArrayList<PartialVectorApproximation<V>>();
    for(DBID id : fullDimensionalData.getDBIDs()) {
      V dv = fullDimensionalData.get(id);
      PartialVectorApproximation<V> va = new PartialVectorApproximation<V>(id, dv.getDimensionality());
      va.calculateApproximation(dv, daFiles);
      vectorApprox.add(va);
      System.out.println(id + ": " + va.toString());
    }
    insertedData = true;
  }

  private LinkedList<PartialVectorApproximation<V>> filter1(int k, int reducedDims, DAFile<V>[] daFiles, PartialVectorApproximation<V> queryApprox, int subspaceDims) {
    LinkedList<PartialVectorApproximation<V>> candidates1 = new LinkedList<PartialVectorApproximation<V>>();
    Heap<Double> sda = new TopBoundedHeap<Double>(k);

    for(int i = 0; i < vectorApprox.size(); i++) {
      PartialVectorApproximation<V> va = vectorApprox.get(i);

      va.resetPMaxDist();
      va.resetPMinDist();

      filter1Loop1(reducedDims, daFiles, queryApprox, va);
      filter1Loop2(reducedDims, subspaceDims, va, daFiles, queryApprox);
      distanceCheck(sda, k, va, candidates1);
    }

    return candidates1;
  }

  private void distanceCheck(Heap<Double> kMinMaxDists, int k, PartialVectorApproximation<V> va, LinkedList<PartialVectorApproximation<V>> candList) {
    if(kMinMaxDists.size() < k || va.getPMinDist() < kMinMaxDists.peek()) {
      candList.add(va);
      kMinMaxDists.add(va.getPMaxDist());
    }
  }

  private void filter1Loop2(int reducedDims, int subspaceDims, PartialVectorApproximation<V> va, DAFile<V>[] daFiles, PartialVectorApproximation<V> queryApprox) {
    for(int d = reducedDims; d < subspaceDims; d++) {
      va.increasePMaxDist(daFiles[d].getMaxMaxDist(queryApprox.getApproximation(daFiles[d].getDimension())));
    }
  }

  private void filter1Loop1(int reducedDims, DAFile<V>[] daFiles, PartialVectorApproximation<V> queryApprox, PartialVectorApproximation<V> va) {
    for(int d = 0; d < reducedDims; d++) {
      int dimension = daFiles[d].getDimension();
      int queryCell = queryApprox.getApproximation(dimension);
      int objectCell = va.getApproximation(dimension);
      va.increasePMinDist(getMinDists(dimension, queryCell)[objectCell]);
      va.increasePMaxDist(getMaxDists(dimension, queryCell)[objectCell]);
    }
  }

  public void setPartitions(Relation<V> objects, int partitions) {
    if((Math.log(partitions) / Math.log(2)) != (int) (Math.log(partitions) / Math.log(2))) {
      throw new IllegalArgumentException("Number of partitions must be a power of 2!");
    }

    log.verbose("PVA: setting partitions (partitionCount=" + (partitions) + ") ...");
    ExecutorService threadExecutor = Executors.newFixedThreadPool(Math.max(1, Runtime.getRuntime().availableProcessors()));
    for(int i = 0; i < daFiles.length; i++) {
      PartitionBuilder<V> builder = new PartitionBuilder<V>(daFiles[i], partitions, objects);
      threadExecutor.execute(builder);
    }
    threadExecutor.shutdown();
    try {
      threadExecutor.awaitTermination(5, TimeUnit.HOURS);
    }
    catch(InterruptedException ex) {
      log.log(Level.SEVERE, null, ex);
      throw new IllegalStateException("interrupted!", ex);
    }
  }

  public double[] getSplitPositions(int dimension) {
    return daFiles[dimension].getSplitPositions();
  }

  public double[] getMinDists(int dimension, int queryCell) {
    return daFiles[dimension].getMinDists(queryCell);
  }

  public double[] getMaxDists(int dimension, int queryCell) {
    return daFiles[dimension].getMaxDists(queryCell);
  }

  public void setLookupTable(V query) {
    for(int i = 0; i < daFiles.length; i++) {
      daFiles[i].setLookupTable(query);
    }
  }

  public DAFile<V> getDAFile(int dimension) {
    return daFiles[dimension];
  }

  public DAFile<V>[] getWorstCaseDistOrder(VectorApproximation query, SubSpace subspace) {
    int subspaceLength = subspace.subspaceDimensions.length;
    @SuppressWarnings("unchecked")
    DAFile<V>[] result = new DAFile[subspaceLength];
    for(int i = 0; i < subspaceLength; i++) {
      result[i] = daFiles[subspace.subspaceDimensions[i]];
    }
    Arrays.sort(result, new WorstCaseDistComparator<V>(query));
    return result;
  }

  @Override
  public IndexStatistics getStatisitcs() {
    IndexStatistics is = new IndexStatistics(refinements, refinements, queryTime, scannedBytes / pageSize);
    is.totalPages = relation.size() + ((VectorApproximation.byteOnDisk(currentSubspaceDims, partitions) * vectorApprox.size()) / pageSize);
    is.pageSize = pageSize;
    is.numQueries = issuedQueries;
    is.indexName = "PartialVA";
    return is;
  }

  /*
   * (non-Javadoc)
   * 
   * @see experimentalcode.shared.index.subspace.SubspaceIndex#resetStatisitcs()
   */
  @Override
  public void resetStatisitcs() {
    queryTime = 0;
    scannedBytes = 0;
    refinements = 0;
  }

  @Override
  public ArrayDBIDs subSpaceKnnQuery(V query, SubSpace subspace, int k) {
    if(query.getDimensionality() != subspace.fullDimensions) {
      throw new IllegalArgumentException("Query must be given in full dimensions (" + subspace.fullDimensions + ") but was " + query.getDimensionality());
    }

    issuedQueries++;
    long t = System.nanoTime();
    long tmp = System.currentTimeMillis();

    // generate query approximation and lookup table
    PartialVectorApproximation<V> queryApprox = new PartialVectorApproximation<V>(null, query.getDimensionality());
    queryApprox.calculateApproximation(query, daFiles);
    setLookupTable(query);

    // perform multi-step k-NN query
    int numBeforePruning = 0, numAfterPruning = 0;

    // sort DA files by worst case distance
    DAFile<V>[] daFiles = getWorstCaseDistOrder(queryApprox, subspace);
    // for (int i=0; i<daFiles.length; i++)
    // log.info("daFiles[" + i + "]: dim " +
    // daFiles[i].getDimension() + " - worstCaseDist " +
    // daFiles[i].getMaxMaxDist(queryApprox.getApproximation(daFiles[i].getDimension())));
    currentSubspaceDims = subspace.subspaceDimensions.length;
    int reducedDims = 2 * currentSubspaceDims / 3;
    reducedDims = Math.max(1, reducedDims);
    prunedVectors = new int[currentSubspaceDims];
    // log.fine("subspaceDims=" + currentSubspaceDims + ", reducedDims=" +
    // reducedDims);
    System.out.println("subspaceDims=" + currentSubspaceDims + ", reducedDims=" + reducedDims);

    // filter 1
    tmp = System.currentTimeMillis();
    LinkedList<PartialVectorApproximation<V>> candidates1 = filter1(k, reducedDims, daFiles, queryApprox, currentSubspaceDims);
    // scannedBytes += vectorApprox.size() * (2/3 *
    // vectorApprox.get(0).byteOnDisk());
    // scannedBytes += vectorApprox.size() *
    // VectorApprox<V>.byteOnDisk(reducedDims, partitions);
    // log.fine("candidate set after filter 1: " + candidates1.size());
    System.out.println("candidate set after filter 1: " + candidates1.size());
    // log.fine("filter1 took " + (System.currentTimeMillis() - tmp) + " ms");
    System.out.println("filter1 took " + (System.currentTimeMillis() - tmp) + " ms");

    numBeforePruning = vectorApprox.size();
    numAfterPruning = candidates1.size();
    prunedVectors[currentSubspaceDims - 1] = numBeforePruning - numAfterPruning;

    // filters 2+
    LinkedList<PartialVectorApproximation<V>> candidates2 = null;
    int addition = reducedDims;
    int filterStep = 2;

    if(currentSubspaceDims <= reducedDims) {
      candidates2 = candidates1;
    }
    else {
      // continue filtering until I/O costs of refining candidates < I/O
      // costs of loading new DA files
      tmp = System.currentTimeMillis();
      while(candidates2 == null || (getIOCosts(candidates2, currentSubspaceDims) >= getIOCosts(daFiles[0], currentSubspaceDims - addition)) && addition < currentSubspaceDims) {
        // if (candidates2 != null)
        // log.info("filter " + filterStep +": refining costs " +
        // getIOCosts(candidates2, subspaceDims) + " (" + candidates2.size() +
        // "/" + subspaceDims + "), DA file costs " + getIOCosts(daFiles[0],
        // subspaceDims-addition) + " (" +daFiles[0].getBorders().length + "/" +
        // (subspaceDims-addition) + ") (dim " + (addition+1) + " of " +
        // subspaceDims + ")");
        if(candidates2 != null) {
          candidates1 = candidates2;
        }
        candidates2 = new LinkedList<PartialVectorApproximation<V>>();

        Heap<Double> kMinMaxDists = new TopBoundedHeap<Double>(k);
        for(PartialVectorApproximation<V> va : candidates1) {
          int dimension = daFiles[addition].getDimension();
          int queryCell = queryApprox.getApproximation(dimension);
          int objectCell = va.getApproximation(dimension);

          va.increasePMinDist(getMinDists(dimension, queryCell)[objectCell]);
          va.decreasePMaxDist(daFiles[addition].getMaxMaxDist(queryApprox.getApproximation(daFiles[addition].getDimension())));
          va.increasePMaxDist(getMaxDists(dimension, queryCell)[objectCell]);

          distanceCheck(kMinMaxDists, k, va, candidates2);

          // scannedBytes += vectorApprox.get(0).byteOnDisk();
          // read ONE additional dimension per object
          // scannedBytes += VectorApprox<V>.byteOnDisk(1, partitions);
        }

        // log.fine("filter2 took " + (System.currentTimeMillis() - tmp) +
        // " ms");
        System.out.println("filter2 took " + (System.currentTimeMillis() - tmp) + " ms");

        // log.fine("candidate set after filter " + filterStep + ": " +
        // candidates2.size());
        System.out.println("candidate set after filter " + filterStep + ": " + candidates2.size());
        // for (Integer i: candidates2) System.out.print(i + " ");
        // log.info();

        // set pruning power for current dimension number
        numBeforePruning = candidates1.size();
        numAfterPruning = candidates2.size();
        prunedVectors[addition] = prunedVectors[addition - 1] + (numBeforePruning - numAfterPruning);

        addition++;
        filterStep++;
      }
    }

    scannedBytes += vectorApprox.size() * VectorApproximation.byteOnDisk(addition, partitions);

    // refinement step
    Vector<PartialVectorApproximation<V>> sortedCandidates = new Vector<PartialVectorApproximation<V>>(candidates2.size());
    for(PartialVectorApproximation<V> va : candidates2) {
      // sortedCandidates.add(vectorApprox.get(id));
      sortedCandidates.add(va);
    }
    // sort candidates by lower bound (minDist)
    PartialVectorApproximation.sortByMinDist(sortedCandidates);
    List<DoubleDistanceResultPair> result = retrieveAccurateDistances(sortedCandidates, k, subspace, query);

    queryTime += System.nanoTime() - t;

    // log.fine("query = " + query);
    System.out.println("query = (" + query + ")");
    // log.info("database: " + vectorApprox.size() + ", candidates: " +
    // sortedCandidates.size() + ", results: " + (result.size()-1));
    // log.fine("database: " + vectorApprox.size() + ", candidates: " +
    // sortedCandidates.size() + ", results: " + (result.size() - 1));
    System.out.println("database: " + vectorApprox.size() + ", candidates: " + sortedCandidates.size() + ", results: " + (result.size() - 1));
    ArrayModifiableDBIDs resultIDs = DBIDUtil.newArray(result.size());
    for(DistanceResultPair<DoubleDistance> dp : result) {
      resultIDs.add(dp.getDBID());
    }

    return resultIDs;
  }

  private List<DoubleDistanceResultPair> retrieveAccurateDistances(Vector<PartialVectorApproximation<V>> sortedCandidates, int k, SubSpace subspace, V query) {
    List<DoubleDistanceResultPair> result = new ArrayList<DoubleDistanceResultPair>();
    for(PartialVectorApproximation<V> va : sortedCandidates) {
      DoubleDistanceResultPair lastElement = null;
      if(!result.isEmpty()) {
        lastElement = result.get(result.size() - 1);
      }
      DBID currentID = va.getId();
      if(result.size() < k || va.getPMinDist() < lastElement.getDistance().doubleValue()) {
        V dv = relation.get(currentID);
        refinements += 1;
        double dist = 0;
        for(int d = 0; d < subspace.subspaceDimensions.length; d++) {
          int dimension = subspace.fullIndex(d);
          dist += Math.pow(dv.doubleValue(dimension + 1) - query.doubleValue(dimension + 1), p);
        }
        DoubleDistanceResultPair dp = new DoubleDistanceResultPair(dist, currentID);
        if(result.size() >= k) {
          if(dist < lastElement.getDistance().doubleValue()) {
            result.remove(lastElement);
            result.add(dp);
          }
        }
        else {
          result.add(dp);
        }
        // TODO: comparator benötigt?
        Collections.sort(result, new DoubleDistanceResultPairComparator());
      }
    }
    return result;
  }

  @Override
  public DBIDs subSpaceRangeQuery(V query, SubSpace subspace, double epsilon) {
    issuedQueries++;
    long t = System.nanoTime();

    // generate query approximation and lookup table

    PartialVectorApproximation<V> queryApprox = new PartialVectorApproximation<V>(null, query.getDimensionality());
    queryApprox.calculateApproximation(query, daFiles);
    setLookupTable(query);

    // perform multi-step range query

    // filter step

    // calculate selectivity coefficients

    int[] subspaceDims = subspace.subspaceDimensions;
    List<DAFile<V>> subspaceDAFiles = new ArrayList<DAFile<V>>(subspaceDims.length);
    for(Integer key : subspaceDims) {
      DAFile<V> daFile = daFiles[key];
      subspaceDAFiles.add(daFile);
    }
    DAFile.calculateSelectivityCoeffs(subspaceDAFiles, query, epsilon);
    // sort DA files by selectivity
    subspaceDAFiles = DAFile.sortBySelectivity(subspaceDAFiles);

    // for (Integer key: subspaceDims)
    // {
    // log.info("DAFile " + key + " - selectivity " +
    // daFiles.get(key).getSelectivityCoeff());
    // }

    // create candidate list (all objects) and prune candidates w.r.t.
    // mindist (i.e. remove them from the list)
    // important: this structure contains the maxDist values for refinement!
    List<DistanceResultPair<DoubleDistance>> candidates = new ArrayList<DistanceResultPair<DoubleDistance>>();
    for(int i = 0; i < vectorApprox.size(); i++) {
      PartialVectorApproximation<V> va = vectorApprox.get(i);

      boolean pruned = false;
      for(DAFile<V> da : subspaceDAFiles) {
        int dimension = da.getDimension();
        int queryCell = queryApprox.getApproximation(dimension);
        int objectCell = va.getApproximation(dimension);
        va.increasePMinDist(getMinDists(dimension, queryCell)[objectCell]);
        va.increasePMaxDist(getMaxDists(dimension, queryCell)[objectCell]);
        if(Math.pow(va.getPMinDist(), 1.0 / p) > epsilon) {
          pruned = true;
          break;
        }
      }
      if(!pruned) {
        candidates.add(new DoubleDistanceResultPair(va.getPMaxDist(), va.getId()));
      }
    }

    // scannedBytes += vectorApprox.size() * vectorApprox.get(0).byteOnDisk();
    scannedBytes += vectorApprox.size() * VectorApproximation.byteOnDisk(subspaceDims.length, partitions);

    // refinement step
    ModifiableDBIDs resultIDs = DBIDUtil.newArray();
    for(DistanceResultPair<DoubleDistance> dp : candidates) {
      DBID id = dp.getDBID();
      if(Math.pow(dp.getDistance().doubleValue(), (1.0 / p)) <= epsilon) {
        // candidate cannot be dropped
        resultIDs.add(id);
      }
      else { // refine candidate
        V dv = relation.get(id);
        refinements += 1;
        double dist = 0;
        for(int d = 0; d < subspace.subspaceDimensions.length; d++) {
          int dimension = subspace.fullIndex(d);
          dist += Math.pow(dv.doubleValue(dimension + 1) - query.doubleValue(dimension + 1), p);
        }
        if(Math.pow(dist, (1.0 / p)) <= epsilon) {
          resultIDs.add(id);
        }
      }
    }

    queryTime += System.nanoTime() - t;

    log.fine("\nquery = " + query);
    // log.info("database: " + vectorApprox.size() + ", candidates: " +
    // candidates.size() + ", results: " + resultIDs.size());
    log.fine("database: " + vectorApprox.size() + ", candidates: " + candidates.size() + ", results: " + resultIDs.size());

    return resultIDs;
  }

  /**
   * 
   * @return number of pruned objects in each subspace dimension
   */
  public int[] getPrunedVectors() {
    return prunedVectors;
  }

  public String getShortName() {
    return SubspaceIndexName.PVA.name();
  }

  /**
   * Computes IO costs (in bytes) needed for refining the candidates.
   * 
   * @param candidates the candidate IDs
   * @param subspaceDims the required subspace dimensions
   * @return the cost value (in bytes)
   */
  private static int getIOCosts(LinkedList<? extends VectorApproximation> candidates, int subspaceDims) {
    return candidates.size() * (subspaceDims * 8 + 4);
  }

  /**
   * Computes IO costs (in bytes) needed for reading several DA-files.
   * 
   * @param sample the DA-file specific costs
   * @param numberOfDAFiles the number of DA-files that have to be read
   * @return the cost value (in bytes)
   */
  private static int getIOCosts(DAFile<?> sample, int numberOfDAFiles) {
    return sample.getIOCosts() * numberOfDAFiles;
  }
}

class WorstCaseDistComparator<V extends NumberVector<?, ?>> implements Comparator<DAFile<V>> {
  private VectorApproximation query;

  public WorstCaseDistComparator(VectorApproximation query) {
    this.query = query;
  }

  @Override
  public int compare(DAFile<V> a, DAFile<V> b) {
    return Double.compare(a.getMaxMaxDist(query.getApproximation(a.getDimension())), b.getMaxMaxDist(query.getApproximation(b.getDimension())));
  }
}

class PartitionBuilder<V extends NumberVector<?, ?>> implements Runnable {
  private final Logging log = Logging.getLogger(PartitionBuilder.class);

  private final DAFile<V> daFile;

  private final int partitions;

  private final double[] splitPositions;

  private final Relation<V> objects;

  public PartitionBuilder(DAFile<V> da, int partitions, Relation<V> objects) {
    this.daFile = da;
    this.partitions = partitions;
    this.objects = objects;
    this.splitPositions = new double[partitions + 1];
  }

  public void run() {
    try {
      log.fine("Dimension " + daFile.getDimension() + " started");
      int[] partitionCount = new int[partitions];

      int size = objects.size();
      int remaining = size;
      double[] tempdata = new double[size];
      int j = 0;
      for(DBID id : objects.getDBIDs()) {
        tempdata[j++] = objects.get(id).doubleValue(daFile.getDimension() + 1);
      }
      Arrays.sort(tempdata);
      // tempdata = unique(tempdata, 1 / (100 * partitions));

      int bucketSize = (int) (size / (double) partitions);
      int i = 0;
      for(int b = 0; b < partitions; b++) {
        assert i <= tempdata.length : "i out ouf bounds " + i + " <> " + tempdata.length;
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
      // make sure that lastobject will be included
      splitPositions[partitions] = tempdata[size - 1] + 0.000001;
      daFile.setPartitions(splitPositions);

      int d = daFile.getDimension();
      System.out.print("dim " + (d + 1) + ": ");
      for(int b = 0; b < splitPositions.length; b++) {
        System.out.print(splitPositions[b] + "  ");
        if(b < splitPositions.length - 1) {
          System.out.print("(bucket " + (b + 1) + "/" + partitions + ", " + partitionCount[b] + ")  ");
        }
      }
      System.out.println();

    }
    catch(Throwable t) {
      log.log(Level.SEVERE, "Exception occured in Partition Builder!", t);
    }
    log.fine("Dimension " + daFile.getDimension() + " finished!");

  }

  public double[] unique(double[] sortedArr, double accuracy) {
    double[] res = new double[sortedArr.length];
    int lastIndex = 1;
    res[0] = sortedArr[0];
    double lastValue = sortedArr[0];
    for(int i = 1; i < sortedArr.length; i++) {
      if(lastValue + accuracy < sortedArr[i]) {
        res[lastIndex] = sortedArr[i];
        lastValue = sortedArr[i];
        lastIndex++;
      }
    }
    return Arrays.copyOf(res, lastIndex - 1);
  }

  public double[] getSplitPositions() {
    return splitPositions;
  }
}