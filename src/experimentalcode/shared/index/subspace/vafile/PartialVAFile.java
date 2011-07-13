package experimentalcode.shared.index.subspace.vafile;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;
import java.util.Vector;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

import de.lmu.ifi.dbs.elki.data.DoubleVector;
import de.lmu.ifi.dbs.elki.database.ids.ArrayDBIDs;
import de.lmu.ifi.dbs.elki.database.ids.ArrayModifiableDBIDs;
import de.lmu.ifi.dbs.elki.database.ids.DBID;
import de.lmu.ifi.dbs.elki.database.ids.DBIDUtil;
import de.lmu.ifi.dbs.elki.database.ids.DBIDs;
import de.lmu.ifi.dbs.elki.database.ids.ModifiableDBIDs;
import experimentalcode.franz.utils.SortedDoubleArray;
import experimentalcode.shared.index.subspace.IndexStatistics;
import experimentalcode.shared.index.subspace.SubSpace;
import experimentalcode.shared.index.subspace.SubspaceIndex;
import experimentalcode.shared.index.subspace.SubspaceIndexName;
import experimentalcode.shared.index.subspace.structures.DiskMemory;
import experimentalcode.thomas.structures.IdDistTuple;

/**
 * PartialVAFile
 * 
 * @author Thomas Bernecker
 * @created 15.09.2009
 * @date 15.09.2009
 */
public class PartialVAFile extends AbstractVAFile implements SubspaceIndex {

  Logger log = Logger.getLogger(PartialVAFile.class.getName());
  boolean insertedData = false;
  // Full data representation
  DiskMemory<DoubleVector> data;
  // temporary, full-dimensional VA representation
  // List<VectorApprox> vectorApprox;
  LinkedList<VectorApprox> vectorApprox;
  // VA dimensions
  DAFile[] daFiles;
  private int partitions;
  private static final int p = 2;
  int initialisations = 10, swaps = 10;
  int bufferSize = Integer.MAX_VALUE;
  private long scannedBytes, queryTime;
  private int issuedQueries;
  int pageSize;
  // Has to be reset within all constructors!
  private static int objectIDCounter;
  private int[] prunedVectors;
  
  private int currentSubspaceDims;

  public PartialVAFile(int pageSize, int partitions, int bufferSize) {
    resetObjectIDCounter();

    this.bufferSize = bufferSize;
    this.pageSize = pageSize;
    this.scannedBytes = 0;
    this.issuedQueries = 0;
    this.partitions = partitions;
    currentSubspaceDims = -1;
  }

  public PartialVAFile(int pageSize, List<DoubleVector> fullDimensionalData, int partitions, int bufferSize) {
    this(pageSize, partitions, bufferSize);
    insert(fullDimensionalData);
  }

  public PartialVAFile(int pageSize, List<DoubleVector> fullDimensionalData, int partitions) {
    this(pageSize, fullDimensionalData, partitions, Integer.MAX_VALUE);
  }

  @Override
  public void insert(List<DoubleVector> fullDimensionalData) {
    if (insertedData) {
      throw new IllegalStateException("Data already inserted.");
    }

    int dimensions = fullDimensionalData.iterator().next().getDimensionality();
    daFiles = new DAFile[dimensions];
    for (int d = 0; d < dimensions; d++) {
      daFiles[d] = new DAFile(d);
    }

    setPartitions(fullDimensionalData, this.partitions);

    data = new DiskMemory<DoubleVector>(this.pageSize / (8 * dimensions + 4), this.bufferSize);
    // vectorApprox = new ArrayList<VectorApprox>(fullDimensionalData.size());
    
    //vectorApprox = new DiskMemory<VectorApprox>(this.pageSize / (8 * (this.partitions + 1) + 4), this.bufferSize);
    vectorApprox = new LinkedList<VectorApprox>();
    for (DoubleVector dv : fullDimensionalData) {
      dv.setID(DBIDUtil.importInteger(objectIDCounter++));
      data.add(dv);
      VectorApprox va = new VectorApprox(dv.getID(), dv.getDimensionality());
      va.calculateApproximation(dv, daFiles);
      vectorApprox.add(va);
    }
    insertedData = true;
  }

  private LinkedList<VectorApprox> filter1(int k, int reducedDims, DAFile[] daFiles, VectorApprox queryApprox, int subspaceDims) {
    LinkedList<VectorApprox> candidates1 = new LinkedList<VectorApprox>();
    SortedDoubleArray sda = new SortedDoubleArray(k);

    // for (int i = 0; i < vectorApprox.size(); i++)
    for (VectorApprox va : vectorApprox) {
      va.resetPMaxDist();
      va.resetPMinDist();

      filter1Loop1(reducedDims, daFiles, queryApprox, va);
      filter1Loop2(reducedDims, subspaceDims, va, daFiles, queryApprox);
      distanceCheck(sda, k, va, candidates1);
    }

    
    return candidates1;
  }

  private void distanceCheck(SortedDoubleArray kMinMaxDists, int k, VectorApprox va, LinkedList<VectorApprox> candList) {
    if (kMinMaxDists.size() < k || va.getPMinDist() < kMinMaxDists.get(kMinMaxDists.size() - 1)) {
      candList.add(va);
      kMinMaxDists.add(va.getPMaxDist());
    }
  }

  private void filter1Loop2(int reducedDims, int subspaceDims, VectorApprox va, DAFile[] daFiles, VectorApprox queryApprox) {
    for (int d = reducedDims; d < subspaceDims; d++) {
      va.increasePMaxDist(daFiles[d].getMaxMaxDist(queryApprox.getApproximation(daFiles[d].getDimension())));
    }
  }

  private void filter1Loop1(int reducedDims, DAFile[] daFiles, VectorApprox queryApprox, VectorApprox va) {
    for (int d = 0; d < reducedDims; d++) {
      int dimension = daFiles[d].getDimension();
      int queryCell = queryApprox.getApproximation(dimension);
      int objectCell = va.getApproximation(dimension);
      va.increasePMinDist(getMinDists(dimension, queryCell)[objectCell]);
      va.increasePMaxDist(getMaxDists(dimension, queryCell)[objectCell]);
    }
  }

  // has to be called first within the constructors!!
  private void resetObjectIDCounter() {
    objectIDCounter = 0;
  }

  public void setPartitions(Collection<DoubleVector> objects, int partitions) {
    if ((Math.log(partitions) / Math.log(2)) != (int) (Math.log(partitions) / Math.log(2))) {
      throw new IllegalArgumentException("Number of partitions must be a power of 2!");
    }

    log.info("PVA: setting partitions (partitionCount=" + (partitions) + ") ...");
    ExecutorService threadExecutor = Executors.newFixedThreadPool(Math.max(1, Runtime.getRuntime().availableProcessors()));
    for (int i = 0; i< daFiles.length;i++) {
      PartitionBuilder builder = new PartitionBuilder(daFiles[i], partitions, objects);
      threadExecutor.execute(builder);
    }
    threadExecutor.shutdown();
    try {
      threadExecutor.awaitTermination(5, TimeUnit.HOURS);
    } catch (InterruptedException ex) {
      log.log(Level.SEVERE, null, ex);
      throw new IllegalStateException("interrupted!", ex);
    }
  }

  public double[] getBorders(int dimension) {
    return daFiles[dimension].getBorders();
  }

  public double[] getMinDists(int dimension, int queryCell) {

    return daFiles[dimension].getMinDists(queryCell);
  }

  public double[] getMaxDists(int dimension, int queryCell) {

    return daFiles[dimension].getMaxDists(queryCell);
  }

  public void setLookupTable(DoubleVector query) {
    for (int i = 0; i< daFiles.length;i++) {
      daFiles[i].setLookupTable(query);
    }
  }

  public DAFile getDAFile(int dimension) {
    return daFiles[dimension];
  }

  public DAFile[] getWorstCaseDistOrder(VectorApprox query, SubSpace subspace) {
    int subspaceLength = subspace.subspaceDimensions.length;
    DAFile[] result = new DAFile[subspaceLength];
    for (int i = 0; i < subspaceLength; i++) {
      result[i] = daFiles[subspace.subspaceDimensions[i]];
    }
    Arrays.sort(result, new WorstCaseDistComparator(query));
    return result;
  }


  /*
   * (non-Javadoc)
   * @see experimentalcode.tobias.subspace.SubspaceIndex#getStatisitcs()
   */
  @Override
  public IndexStatistics getStatisitcs() {
    IndexStatistics is = new IndexStatistics(data.pageAccessesL, data.pageAccessesP, queryTime, scannedBytes / pageSize);
    is.totalPages = data.getPageCount() + ((VectorApprox.byteOnDisk(currentSubspaceDims, partitions)*vectorApprox.size())/pageSize);
    is.pageSize = pageSize;
    is.numQueries = issuedQueries;
    is.indexName = "PartialVA";
    return is;
  }


  /*
   * (non-Javadoc)
   * @see experimentalcode.tobias.subspace.SubspaceIndex#resetStatisitcs()
   */
  @Override
  public void resetStatisitcs() {
    data.resetCounters();
    data.resetBuffer();
    queryTime = 0;
    scannedBytes = 0;
  }

  public void resetBuffer() {
    data.resetBuffer();
  }


  /*
   * (non-Javadoc)
   * @see
   * experimentalcode.tobias.subspace.SubspaceIndex#subSpaceKnnQuery(de.lmu
   * .ifi.dbs.elki.data.DoubleVector,
   * experimentalcode.tobias.subspace.SubSpace, int)
   */
  @Override
  public ArrayDBIDs subSpaceKnnQuery(DoubleVector query, SubSpace subspace, int k) {
    if (query.getDimensionality() != subspace.fullDimensions) {
      throw new IllegalArgumentException("Query must be given in full dimensions (" + subspace.fullDimensions + ") but was " + query.getDimensionality());
    }

    issuedQueries++;
    long t = System.nanoTime();
    long tmp = System.currentTimeMillis();

    // generate query approximation and lookup table
    VectorApprox queryApprox = new VectorApprox(query.getID(), query.getDimensionality());
    queryApprox.calculateApproximation(query, daFiles);
    setLookupTable(query);

    // perform multi-step NN-query
    int numBeforePruning = 0, numAfterPruning = 0;

    // sort DA files by worst case distance
    DAFile[] daFiles = getWorstCaseDistOrder(queryApprox, subspace);
    // for (int i=0; i<daFiles.length; i++)
    // log.info("daFiles[" + i + "]: dim " +
    // daFiles[i].getDimension() + " - worstCaseDist " +
    // daFiles[i].getMaxMaxDist(queryApprox.getApproximation(daFiles[i].getDimension())));
    currentSubspaceDims = subspace.subspaceDimensions.length;
    int reducedDims = 2 * currentSubspaceDims / 3;
    reducedDims = Math.max(1, reducedDims);
    prunedVectors = new int[currentSubspaceDims];
    log.fine("subspaceDims=" + currentSubspaceDims + ", reducedDims=" + reducedDims);

    // filter 1
    tmp = System.currentTimeMillis();
    LinkedList<VectorApprox> candidates1 = filter1(k, reducedDims, daFiles, queryApprox, currentSubspaceDims);
//  scannedBytes += vectorApprox.size() * (2/3 * vectorApprox.get(0).byteOnDisk());
   // scannedBytes += vectorApprox.size() * VectorApprox.byteOnDisk(reducedDims, partitions);
    log.fine("candidate set after filter 1: " + candidates1.size());
    log.fine("filter1 took " + (System.currentTimeMillis() - tmp) + " ms");

    numBeforePruning = vectorApprox.size();
    numAfterPruning = candidates1.size();
    prunedVectors[currentSubspaceDims - 1] = numBeforePruning - numAfterPruning;

    // filters 2+
    LinkedList<VectorApprox> candidates2 = null;
    int addition = reducedDims;
    int filterStep = 2;

    if (currentSubspaceDims <= reducedDims) {
      candidates2 = candidates1;
    } else {
      // continue filtering until I/O costs of refining candidates < I/O
      // costs of loading new DA files
      tmp = System.currentTimeMillis();
      while (candidates2 == null || (getIOCosts(candidates2, currentSubspaceDims) >= getIOCosts(daFiles[0], currentSubspaceDims - addition)) && addition < currentSubspaceDims) {
        // if (candidates2 != null)
        // log.info("filter " + filterStep +": refining costs " + getIOCosts(candidates2, subspaceDims) + " (" + candidates2.size() + "/" + subspaceDims + "), DA file costs " + getIOCosts(daFiles[0], subspaceDims-addition) + " (" +daFiles[0].getBorders().length + "/" + (subspaceDims-addition) + ") (dim " + (addition+1) + " of " + subspaceDims + ")");
        if (candidates2 != null) {
          candidates1 = candidates2;
        }
        candidates2 = new LinkedList<VectorApprox>();

        SortedDoubleArray kMinMaxDists = new SortedDoubleArray(k);
        for (VectorApprox va: candidates1) {
          int dimension = daFiles[addition].getDimension();
          int queryCell = queryApprox.getApproximation(dimension);
          int objectCell = va.getApproximation(dimension);

          va.increasePMinDist(getMinDists(dimension, queryCell)[objectCell]);
          va.decreasePMaxDist(daFiles[addition].getMaxMaxDist(queryApprox.getApproximation(daFiles[addition].getDimension())));
          va.increasePMaxDist(getMaxDists(dimension, queryCell)[objectCell]);

          distanceCheck(kMinMaxDists, k, va, candidates2);

          // scannedBytes += vectorApprox.get(0).byteOnDisk();
          // read ONE additional dimension per object
       //   scannedBytes += VectorApprox.byteOnDisk(1, partitions);
        }
        
        log.fine("filter2 took " + (System.currentTimeMillis() - tmp) + " ms");

        if (log.isLoggable(Level.FINE)) {
          log.fine("candidate set after filter " + filterStep + ": " + candidates2.size());
        }
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

    scannedBytes += vectorApprox.size() * VectorApprox.byteOnDisk(addition, partitions);
    
    // refinement step
    Vector<VectorApprox> sortedCandidates = new Vector<VectorApprox>(candidates2.size());
    for (VectorApprox va : candidates2) // sortedCandidates.add(vectorApprox.get(id));
    {
      sortedCandidates.add(va);
    }
    // sort candidates by lower bound (minDist)
    sortedCandidates = VectorApprox.sortByMinDist(sortedCandidates);
    Vector<IdDistTuple> result = retrieveAccurateDistances(sortedCandidates, k, subspace, query);

    queryTime += System.nanoTime() - t;
    resetBuffer();

    log.fine("query = " + query);
    // log.info("database: " + vectorApprox.size() + ", candidates: " + sortedCandidates.size() + ", results: " + (result.size()-1));
    log.fine("database: " + vectorApprox.size() + ", candidates: " + sortedCandidates.size() + ", results: " + (result.size() - 1));
    ArrayModifiableDBIDs resultIDs = DBIDUtil.newArray(result.size());
    for (IdDistTuple o : result) {
      resultIDs.add(o.getID());
    }

    return resultIDs;
  }

  private Vector<IdDistTuple> retrieveAccurateDistances(Vector<VectorApprox> sortedCandidates, int k, SubSpace subspace, DoubleVector query) {
    Vector<IdDistTuple> result = new Vector<IdDistTuple>();
    for (VectorApprox va : sortedCandidates) {
      if (result.size() < k || va.getPMinDist() < result.lastElement().getPDist()) {
        DoubleVector dv = data.getObject(va.getID());
        double dist = 0;
        for (int d = 0; d < subspace.subspaceDimensions.length; d++) {
          int dimension = subspace.fullIndex(d);
          dist += Math.pow(dv.doubleValue(dimension + 1) - query.doubleValue(dimension + 1), p);
        }
        IdDistTuple tup = new IdDistTuple(va.getID());
        tup.setPDist(dist);
        if (result.size() >= k) {
          if (dist < result.lastElement().getPDist()) {
            result.remove(result.lastElement());
            result.add(tup);
          }
        } else {
          result.add(tup);
        }
        IdDistTuple.sortByDist(result);
      }
    }
    return result;
  }


  /*
   * (non-Javadoc)
   * @see
   * experimentalcode.tobias.subspace.SubspaceIndex#subSpaceRangeQuery(de.
   * lmu.ifi.dbs.elki.data.DoubleVector,
   * experimentalcode.tobias.subspace.SubSpace, double)
   */
  @Override
  public DBIDs subSpaceRangeQuery(DoubleVector query, SubSpace subspace, double epsilon) {
    issuedQueries++;
    long t = System.nanoTime();

    // generate query approximation and lookup table

    VectorApprox queryApprox = new VectorApprox(query.getID(), query.getDimensionality());
    try {
      queryApprox.calculateApproximation(query, daFiles);
    } catch (Exception e) {
      e.printStackTrace();
    }
    setLookupTable(query);

    // perform multi-step NN-query

    // filter step

    // calculate selectivity coefficients

    int[] subspaceDims = subspace.subspaceDimensions;
    List<DAFile> subspaceDAFiles = new ArrayList<DAFile>(subspaceDims.length);
    for (Integer key : subspaceDims) {
      DAFile daFile = daFiles[key];
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
    Vector<IdDistTuple> candidates = new Vector<IdDistTuple>();
    // for (VectorApprox va: vectorApprox)
    for (VectorApprox va : vectorApprox) {

      boolean pruned = false;
      IdDistTuple tup = new IdDistTuple(va.getID());
      for (DAFile da : subspaceDAFiles) {
        int dimension = da.getDimension();
        int queryCell = queryApprox.getApproximation(dimension);
        int objectCell = va.getApproximation(dimension);
        va.increasePMinDist(getMinDists(dimension, queryCell)[objectCell]);
        va.increasePMaxDist(getMaxDists(dimension, queryCell)[objectCell]);
        if (Math.pow(va.getPMinDist(), 1.0 / p) > epsilon) {
          pruned = true;
          break;
        }
      }
      if (!pruned) {
        tup.setPDist(va.getPMaxDist());
        candidates.add(tup);
      }
    }

    // scannedBytes += vectorApprox.size() * vectorApprox.get(0).byteOnDisk();
    scannedBytes += vectorApprox.size() * VectorApprox.byteOnDisk(subspaceDims.length, partitions);

    // refinement step
    ModifiableDBIDs resultIDs = DBIDUtil.newArray();
    for (IdDistTuple tup : candidates) {
      DBID id = tup.getID();
      if (Math.pow(tup.getPDist(), (1.0 / p)) <= epsilon) // candidate cannot bedropped
      {
        resultIDs.add(id);
      } else // refine candidate
      {
        DoubleVector dv = data.getObject(id);
        double dist = 0;
        for (int d = 0; d < subspace.subspaceDimensions.length; d++) {
          int dimension = subspace.fullIndex(d);
          dist += Math.pow(dv.doubleValue(dimension + 1) - query.doubleValue(dimension + 1), p);
        }
        if (Math.pow(dist, (1.0 / p)) <= epsilon) {
          resultIDs.add(id);
        }
      }
    }

    queryTime += System.nanoTime() - t;
    resetBuffer();

    log.fine("\nquery = " + query);
    // log.info("database: " + vectorApprox.size() + ", candidates: " + candidates.size() + ", results: " + resultIDs.size());
    log.fine("database: " + vectorApprox.size() + ", candidates: " + candidates.size() + ", results: " + resultIDs.size());

    return resultIDs;
  }

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
  private static int getIOCosts(LinkedList<VectorApprox> candidates, int subspaceDims) {
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

class WorstCaseDistComparator implements Comparator<DAFile> {

  private VectorApprox query;

  public WorstCaseDistComparator(VectorApprox query) {
    this.query = query;
  }

  /*
   * (non-Javadoc)
   * @see java.util.Comparator#compare(java.lang.Object, java.lang.Object)
   */
  @Override
  public int compare(DAFile a, DAFile b) {
    return Double.compare(a.getMaxMaxDist(query.getApproximation(a.getDimension())), b.getMaxMaxDist(query.getApproximation(b.getDimension())));
  }
}

class PartitionBuilder implements Runnable {

  private final Logger log = Logger.getLogger(PartitionBuilder.class.getName());
  private final DAFile daFile;
  private final int partitions;
  private final double[] borders;
  private final Collection<DoubleVector> objects;

  public PartitionBuilder(DAFile da, int partitions, Collection<DoubleVector> objects) {
    this.daFile = da;
    this.partitions = partitions;
    this.objects = objects;
    this.borders = new double[partitions + 1];
  }

  public void run() {
    try {
      log.fine("Dimension " + daFile.getDimension() + " started");
      int[] partitionCount = new int[partitions];

      int size = objects.size();
      int remaining = size;
      double[] tempdata = new double[size];
      int j = 0;
      for (DoubleVector dv : objects) {
        tempdata[j++] = dv.doubleValue(daFile.getDimension() + 1);
      }
      Arrays.sort(tempdata);
      // tempdata = unique(tempdata, 1 / (100 * partitions));

      int bucketSize = (int) (size / (double) partitions);
      int i = 0;
      for (int b = 0; b < partitionCount.length; b++) {
        assert i <= tempdata.length : "i out ouf bounds "+i+" <> "+tempdata.length;
        borders[b] = tempdata[i];
        remaining -= bucketSize;
        i += bucketSize;

        // test: are there remaining objects that have to be put in the
        // first buckets?
        if (remaining > (bucketSize * (partitionCount.length - b - 1))) {
          i++;
          remaining--;
          partitionCount[b]++;
        }

        partitionCount[b] += bucketSize;
      }
      borders[partitions] = tempdata[size - 1] + 0.000001; // make sure that last object will be included
      daFile.setPartitions(borders);
    } catch (Throwable t) {
      log.log(Level.SEVERE, "Exception occured in Partition Builder!", t);
    }
    log.fine("Dimension " + daFile.getDimension() + " finished!");
  }

  public double[] unique (double[] sortedArr, double accuracy){
    double[] res = new double[sortedArr.length];
    int lastIndex = 1;
    res[0] = sortedArr[0];
    double lastValue = sortedArr[0];
    for(int i = 1; i< sortedArr.length;i++){
      if(lastValue+accuracy <sortedArr[i]){
        res[lastIndex] = sortedArr[i];
        lastValue = sortedArr[i];
        lastIndex++;
      }
    }
    return Arrays.copyOf(res, lastIndex-1);
  }
  public double[] getBorders() {
    return borders;
  }
}
