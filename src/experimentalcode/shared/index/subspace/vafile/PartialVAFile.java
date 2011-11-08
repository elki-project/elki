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
import de.lmu.ifi.dbs.elki.distance.distancefunction.EuclideanDistanceFunction;
import de.lmu.ifi.dbs.elki.distance.distancefunction.LPNormDistanceFunction;
import de.lmu.ifi.dbs.elki.distance.distancefunction.PrimitiveDoubleDistanceFunction;
import de.lmu.ifi.dbs.elki.distance.distancevalue.DoubleDistance;
import de.lmu.ifi.dbs.elki.logging.Logging;
import de.lmu.ifi.dbs.elki.utilities.datastructures.heap.Heap;
import de.lmu.ifi.dbs.elki.utilities.datastructures.heap.TopBoundedHeap;
import de.lmu.ifi.dbs.elki.utilities.documentation.Reference;
import experimentalcode.shared.index.subspace.IndexStatistics;
import experimentalcode.shared.index.subspace.SubSpace;
import experimentalcode.shared.index.subspace.SubspaceIndex;
import experimentalcode.shared.index.subspace.SubspaceIndexName;

/**
 * PartialVAFile
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
 */
@Reference(authors = "Hans-Peter Kriegel, Peer Kröger, Matthias Schubert, Ziyue Zhu", title = "Efficient Query Processing in Arbitrary Subspaces Using Vector Approximations", booktitle = "Proc. 18th Int. Conf. on Scientific and Statistical Database Management (SSDBM 06), Wien, Austria, 2006", url = "http://dx.doi.org/10.1109/SSDBM.2006.23")
public class PartialVAFile<V extends NumberVector<V, ?>> implements SubspaceIndex<V> {
  /**
   * Class logger
   */
  private static final Logging log = Logging.getLogger(PartialVAFile.class);

  /**
   * The relation used for refinement
   */
  Relation<V> relation;

  /**
   * VA approximation working space
   */
  private List<PartialVectorApproximation<V>> vectorApprox;

  /**
   * Partial VA files
   */
  List<DAFile> daFiles;

  private int partitions;

  private long scannedBytes, queryTime;

  private int issuedQueries;

  private int refinements = 0;

  int pageSize;

  private int[] prunedVectors;

  // FIXME: hack.
  private int currentSubspaceDims;

  private double[][] splitPartitions;

  public PartialVAFile(int pageSize, Relation<V> fullDimensionalData, int partitions) {
    this.pageSize = pageSize;
    this.scannedBytes = 0;
    this.issuedQueries = 0;
    this.refinements = 0;
    this.partitions = partitions;
    currentSubspaceDims = -1;

    insert(fullDimensionalData);
  }

  @Override
  public void insert(Relation<V> fullDimensionalData) throws IllegalStateException {
    if(vectorApprox.size() > 0) {
      throw new IllegalStateException("Data already inserted.");
    }

    DBID sampleID = fullDimensionalData.getDBIDs().iterator().next();
    int dimensions = fullDimensionalData.get(sampleID).getDimensionality();
    daFiles = new ArrayList<DAFile>(dimensions);
    for(int d = 0; d < dimensions; d++) {
      daFiles.add(new DAFile(d));
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
  }

  private LinkedList<PartialVectorApproximation<V>> filter1(int k, int reducedDims, List<DAFile> daFiles, PartialVectorApproximation<V> queryApprox, int subspaceDims, VALPNormDistance dist) {
    LinkedList<PartialVectorApproximation<V>> candidates1 = new LinkedList<PartialVectorApproximation<V>>();
    Heap<Double> sda = new TopBoundedHeap<Double>(k);

    for(int i = 0; i < vectorApprox.size(); i++) {
      PartialVectorApproximation<V> va = vectorApprox.get(i);

      va.resetPMaxDist();
      va.resetPMinDist();

      filter1Loop1(reducedDims, daFiles, va, dist);
      filter1Loop2(reducedDims, subspaceDims, va, daFiles, queryApprox);
      distanceCheck(sda, k, va, candidates1);
    }

    return candidates1;
  }

  private void distanceCheck(Heap<Double> kMinMaxDists, int k, PartialVectorApproximation<V> va, LinkedList<PartialVectorApproximation<V>> candList) {
    if(kMinMaxDists.size() < k || va.getMinDistP() < kMinMaxDists.peek()) {
      candList.add(va);
      kMinMaxDists.add(va.getMaxDistP());
    }
  }

  private void filter1Loop2(int reducedDims, int subspaceDims, PartialVectorApproximation<V> va, List<DAFile> daFiles, PartialVectorApproximation<V> queryApprox) {
    for(int d = reducedDims; d < subspaceDims; d++) {
      va.increaseMaxDistP(daFiles.get(d).getMaxMaxDist(queryApprox.getApproximation(daFiles.get(d).getDimension())));
    }
  }

  private void filter1Loop1(int reducedDims, List<DAFile> daFiles, PartialVectorApproximation<V> va, VALPNormDistance dist) {
    for(int d = 0; d < reducedDims; d++) {
      int dimension = daFiles.get(d).getDimension();
      int objectCell = va.getApproximation(dimension);
      va.increaseMinDistP(dist.getPartialMinDist(dimension, objectCell));
      va.increaseMaxDistP(dist.getPartialMaxDist(dimension, objectCell));
    }
  }

  public void setPartitions(Relation<V> objects, int partitions) {
    if((Math.log(partitions) / Math.log(2)) != (int) (Math.log(partitions) / Math.log(2))) {
      throw new IllegalArgumentException("Number of partitions must be a power of 2!");
    }
    splitPartitions = new double[daFiles.size()][];

    if(log.isVerbose()) {
      log.verbose("PVA: setting partitions (partitionCount=" + (partitions) + ") ...");
    }
    for(int i = 0; i < daFiles.size(); i++) {
      PartitionBuilder<V> builder = new PartitionBuilder<V>(daFiles.get(i), partitions, objects);
      builder.run();
      splitPartitions[i] = builder.getSplitPositions();
    }
  }

  public double[] getSplitPositions(int dimension) {
    return daFiles.get(dimension).getSplitPositions();
  }

  public DAFile getDAFile(int dimension) {
    return daFiles.get(dimension);
  }

  public List<DAFile> getWorstCaseDistOrder(VectorApproximation query, SubSpace subspace) {
    int subspaceLength = subspace.subspaceDimensions.length;
    List<DAFile> result = new ArrayList<DAFile>(subspaceLength);
    for(int i = 0; i < subspaceLength; i++) {
      result.add(daFiles.get(subspace.subspaceDimensions[i]));
    }
    Collections.sort(result, new WorstCaseDistComparator<V>(query));
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

    LPNormDistanceFunction fulldist = EuclideanDistanceFunction.STATIC;
    VALPNormDistance dist = new VALPNormDistance(2, splitPartitions, query, queryApprox);

    // perform multi-step k-NN query
    int numBeforePruning = 0, numAfterPruning = 0;

    // sort DA files by worst case distance
    List<DAFile> daFiles = getWorstCaseDistOrder(queryApprox, subspace);
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
    LinkedList<PartialVectorApproximation<V>> candidates1 = filter1(k, reducedDims, daFiles, queryApprox, currentSubspaceDims, dist);
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
      while(candidates2 == null || (getIOCosts(candidates2, currentSubspaceDims) >= getIOCosts(daFiles.get(0), currentSubspaceDims - addition)) && addition < currentSubspaceDims) {
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
          int dimension = daFiles.get(addition).getDimension();
          int objectCell = va.getApproximation(dimension);

          va.increaseMinDistP(dist.getPartialMinDist(dimension, objectCell));
          va.decreaseMaxDistP(daFiles.get(addition).getMaxMaxDist(queryApprox.getApproximation(daFiles.get(addition).getDimension())));
          va.increaseMaxDistP(dist.getPartialMaxDist(dimension, objectCell));

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
    List<DoubleDistanceResultPair> result = retrieveAccurateDistances(fulldist, sortedCandidates, k, subspace, query);

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

  private List<DoubleDistanceResultPair> retrieveAccurateDistances(PrimitiveDoubleDistanceFunction<? super V> distf, Vector<PartialVectorApproximation<V>> sortedCandidates, int k, SubSpace subspace, V query) {
    List<DoubleDistanceResultPair> result = new ArrayList<DoubleDistanceResultPair>();
    for(PartialVectorApproximation<V> va : sortedCandidates) {
      DoubleDistanceResultPair lastElement = null;
      if(!result.isEmpty()) {
        lastElement = result.get(result.size() - 1);
      }
      DBID currentID = va.getId();
      if(result.size() < k || va.getMinDistP() < lastElement.getDistance().doubleValue()) {
        V dv = relation.get(currentID);
        refinements += 1;
        double dist = distf.doubleDistance(dv, query);
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

    final LPNormDistanceFunction fulldist = EuclideanDistanceFunction.STATIC;
    final VALPNormDistance dist = new VALPNormDistance(2, splitPartitions, query, queryApprox);
    final double p = 2.0;

    // perform multi-step range query

    // filter step

    // calculate selectivity coefficients

    int[] subspaceDims = subspace.subspaceDimensions;
    List<DAFile> subspaceDAFiles = new ArrayList<DAFile>(subspaceDims.length);
    for(Integer key : subspaceDims) {
      DAFile daFile = daFiles.get(key);
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
      for(DAFile da : subspaceDAFiles) {
        int dimension = da.getDimension();
        int objectCell = va.getApproximation(dimension);
        va.increaseMinDistP(dist.getPartialMinDist(dimension, objectCell));
        va.increaseMaxDistP(dist.getPartialMaxDist(dimension, objectCell));
        if(Math.pow(va.getMinDistP(), 1.0 / p) > epsilon) {
          pruned = true;
          break;
        }
      }
      if(!pruned) {
        candidates.add(new DoubleDistanceResultPair(va.getMaxDistP(), va.getId()));
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
        double distance = fulldist.doubleDistance(dv, query);
        if(distance <= epsilon) {
          resultIDs.add(id);
        }
      }
    }

    queryTime += System.nanoTime() - t;

    if(log.isDebuggingFine()) {
      log.fine("\nquery = " + query);
      // log.info("database: " + vectorApprox.size() + ", candidates: " +
      // candidates.size() + ", results: " + resultIDs.size());
      log.fine("database: " + vectorApprox.size() + ", candidates: " + candidates.size() + ", results: " + resultIDs.size());
    }

    return resultIDs;
  }

  /**
   * 
   * @return number of pruned objects in each subspace dimension
   */
  public int[] getPrunedVectors() {
    return prunedVectors;
  }

  @Override
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
  private static int getIOCosts(DAFile sample, int numberOfDAFiles) {
    return sample.getIOCosts() * numberOfDAFiles;
  }
}

class WorstCaseDistComparator<V extends NumberVector<?, ?>> implements Comparator<DAFile> {
  private VectorApproximation query;

  public WorstCaseDistComparator(VectorApproximation query) {
    this.query = query;
  }

  @Override
  public int compare(DAFile a, DAFile b) {
    return Double.compare(a.getMaxMaxDist(query.getApproximation(a.getDimension())), b.getMaxMaxDist(query.getApproximation(b.getDimension())));
  }
}

class PartitionBuilder<V extends NumberVector<?, ?>> implements Runnable {
  private final static Logging log = Logging.getLogger(PartitionBuilder.class);

  private final DAFile daFile;

  private final int partitions;

  private final double[] splitPositions;

  private final Relation<V> objects;

  public PartitionBuilder(DAFile da, int partitions, Relation<V> objects) {
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

      if(log.isDebuggingFine()) {
        int d = daFile.getDimension();
        StringBuffer buf = new StringBuffer();
        buf.append("dim " + (d + 1) + ": ");
        for(int b = 0; b < splitPositions.length; b++) {
          buf.append(splitPositions[b] + "  ");
          if(b < splitPositions.length - 1) {
            buf.append("(bucket " + (b + 1) + "/" + partitions + ", " + partitionCount[b] + ")  ");
          }
        }
        log.fine(buf);
      }
    }
    catch(Throwable t) {
      log.log(Level.SEVERE, "Exception occured in Partition Builder!", t);
    }
    log.fine("Dimension " + daFile.getDimension() + " finished!");

  }

  public double[] getSplitPositions() {
    return splitPositions;
  }
}