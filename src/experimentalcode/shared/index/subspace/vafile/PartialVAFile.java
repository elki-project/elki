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
import java.util.BitSet;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Vector;

import de.lmu.ifi.dbs.elki.data.NumberVector;
import de.lmu.ifi.dbs.elki.data.type.TypeInformation;
import de.lmu.ifi.dbs.elki.data.type.TypeUtil;
import de.lmu.ifi.dbs.elki.database.ids.DBID;
import de.lmu.ifi.dbs.elki.database.ids.DBIDs;
import de.lmu.ifi.dbs.elki.database.query.DatabaseQuery;
import de.lmu.ifi.dbs.elki.database.query.DistanceDBIDResult;
import de.lmu.ifi.dbs.elki.database.query.DoubleDistanceResultPair;
import de.lmu.ifi.dbs.elki.database.query.GenericDistanceDBIDList;
import de.lmu.ifi.dbs.elki.database.query.distance.DistanceQuery;
import de.lmu.ifi.dbs.elki.database.query.knn.KNNQuery;
import de.lmu.ifi.dbs.elki.database.query.knn.KNNResult;
import de.lmu.ifi.dbs.elki.database.query.range.RangeQuery;
import de.lmu.ifi.dbs.elki.database.relation.Relation;
import de.lmu.ifi.dbs.elki.distance.distancefunction.DistanceFunction;
import de.lmu.ifi.dbs.elki.distance.distancefunction.LPNormDistanceFunction;
import de.lmu.ifi.dbs.elki.distance.distancefunction.subspace.SubspaceLPNormDistanceFunction;
import de.lmu.ifi.dbs.elki.distance.distancevalue.Distance;
import de.lmu.ifi.dbs.elki.distance.distancevalue.DoubleDistance;
import de.lmu.ifi.dbs.elki.index.AbstractRefiningIndex;
import de.lmu.ifi.dbs.elki.index.IndexFactory;
import de.lmu.ifi.dbs.elki.index.KNNIndex;
import de.lmu.ifi.dbs.elki.index.RangeIndex;
import de.lmu.ifi.dbs.elki.index.tree.TreeIndexFactory;
import de.lmu.ifi.dbs.elki.index.vafile.VALPNormDistance;
import de.lmu.ifi.dbs.elki.index.vafile.VectorApproximation;
import de.lmu.ifi.dbs.elki.logging.Logging;
import de.lmu.ifi.dbs.elki.utilities.DatabaseUtil;
import de.lmu.ifi.dbs.elki.utilities.datastructures.heap.Heap;
import de.lmu.ifi.dbs.elki.utilities.datastructures.heap.KNNHeap;
import de.lmu.ifi.dbs.elki.utilities.datastructures.heap.KNNList;
import de.lmu.ifi.dbs.elki.utilities.datastructures.heap.TopBoundedHeap;
import de.lmu.ifi.dbs.elki.utilities.documentation.Reference;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.AbstractParameterizer;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.OptionID;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.constraints.GreaterConstraint;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.Parameterization;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.IntParameter;
import experimentalcode.shared.index.subspace.IndexStatistics;
import experimentalcode.shared.index.subspace.SubSpace;

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
 * @author Erich Schubert
 */
@Reference(authors = "Hans-Peter Kriegel, Peer Kröger, Matthias Schubert, Ziyue Zhu", title = "Efficient Query Processing in Arbitrary Subspaces Using Vector Approximations", booktitle = "Proc. 18th Int. Conf. on Scientific and Statistical Database Management (SSDBM 06), Wien, Austria, 2006", url = "http://dx.doi.org/10.1109/SSDBM.2006.23")
public class PartialVAFile<V extends NumberVector<?, ?>> extends AbstractRefiningIndex<V> implements KNNIndex<V>, RangeIndex<V> {
  /**
   * Class logger
   */
  private static final Logging logger = Logging.getLogger(PartialVAFile.class);

  /**
   * Partial VA files
   */
  List<DAFile> daFiles;

  /**
   * Number of partitions
   */
  private final int partitions;

  /**
   * Page size
   */
  private final int pageSize;

  /**
   * Splitting grid
   */
  private double[][] splitPartitions;

  /**
   * Statistics
   */
  protected Statistics stats;

  /**
   * The (full - we are in-memory only right now) vector approximations.
   */
  private ArrayList<VectorApproximation> vectorApprox;

  /**
   * Constructor.
   *
   * @param pageSize Page size
   * @param relation Data relation
   * @param partitions Number of partitions
   */
  public PartialVAFile(int pageSize, Relation<V> relation, int partitions) {
    super(relation);
    this.pageSize = pageSize;
    this.partitions = partitions;
    this.stats = new Statistics();
  }

  @Override
  public void initialize(Relation<V> relation, DBIDs ids) throws IllegalStateException {
    if(splitPartitions != null) {
      throw new IllegalStateException("Data already inserted.");
    }

    if((Math.log(partitions) / Math.log(2)) != (int) (Math.log(partitions) / Math.log(2))) {
      throw new IllegalArgumentException("Number of partitions must be a power of 2!");
    }

    final int dimensions = DatabaseUtil.dimensionality(relation);

    splitPartitions = new double[dimensions][];
    daFiles = new ArrayList<DAFile>(dimensions);
    for(int d = 0; d < dimensions; d++) {
      final DAFile f = new DAFile(relation, d, partitions);
      splitPartitions[d] = f.getSplitPositions();
      daFiles.add(f);
    }

    vectorApprox = new ArrayList<VectorApproximation>();
    for(DBID id : relation.getDBIDs()) {
      V dv = relation.get(id);
      VectorApproximation va = calculateApproximation(id, dv);
      vectorApprox.add(va);
    }
  }

  // TODO: Remove/refactor?
  public IndexStatistics getStatistics() {
    IndexStatistics is = new IndexStatistics(stats.refinements, stats.refinements, stats.queryTime, stats.scannedBytes / pageSize);
    // is.totalPages = /* relation.size() +
    // */((VectorApproximation.byteOnDisk(currentSubspaceDims, partitions) *
    // vectorApprox.size()) / pageSize);
    is.pageSize = pageSize;
    is.numQueries = stats.issuedQueries;
    is.indexName = "PartialVA";
    return is;
  }

  // TODO: remove/refactor.
  public void resetStatistics() {
    stats = new Statistics();
  }

  @Override
  public String getShortName() {
    return "pva-file";
  }

  @Override
  public String getLongName() {
    return "partial va-file";
  }

  /**
   * Fake subspace (full-dimensional).
   * 
   * @param relation Relation with full dimensionality
   * @return Bit set with all bits set.
   */
  protected static BitSet fakeSubspace(Relation<? extends NumberVector<?, ?>> relation) {
    int dim = DatabaseUtil.dimensionality(relation);
    BitSet bits = new BitSet();
    for(int i = 0; i < dim; i++) {
      bits.set(i);
    }
    return bits;
  }

  /**
   * Calculate the VA file position given the existing borders.
   * 
   * @param id Object ID
   * @param dv Data vector
   * @return Vector approximation
   */
  public VectorApproximation calculateApproximation(DBID id, V dv) {
    int approximation[] = new int[dv.getDimensionality()];
    for(int d = 0; d < splitPartitions.length; d++) {
      final double val = dv.doubleValue(d + 1);
      final int lastBorderIndex = splitPartitions[d].length - 1;

      // Value is below data grid
      if(val < splitPartitions[d][0]) {
        approximation[d] = 0;
        if(id != null) {
          logger.warning("Vector outside of VAFile grid!");
        }
      } // Value is above data grid
      else if(val > splitPartitions[d][lastBorderIndex]) {
        approximation[d] = lastBorderIndex - 1;
        if(id != null) {
          logger.warning("Vector outside of VAFile grid!");
        }
      } // normal case
      else {
        // Search grid position
        int pos = Arrays.binarySearch(splitPartitions[d], val);
        pos = (pos >= 0) ? pos : ((-pos) - 2);
        approximation[d] = pos;
      }
    }
    return new VectorApproximation(id, approximation);
  }

  @SuppressWarnings("unchecked")
  @Override
  public <D extends Distance<D>> KNNQuery<V, D> getKNNQuery(DistanceQuery<V, D> distanceQuery, Object... hints) {
    for(Object hint : hints) {
      if(hint == DatabaseQuery.HINT_BULK) {
        // FIXME: support bulk?
        return null;
      }
    }
    DistanceFunction<? super V, ?> df = distanceQuery.getDistanceFunction();
    if(df instanceof SubspaceLPNormDistanceFunction) {
      double p = ((SubspaceLPNormDistanceFunction) df).getP();
      BitSet bits = ((SubspaceLPNormDistanceFunction) df).getSelectedDimensions();
      DistanceQuery<V, ?> ddq = (DistanceQuery<V, ?>) distanceQuery;
      KNNQuery<V, ?> dq = new PartialVAFileKNNQuery((DistanceQuery<V, DoubleDistance>) ddq, p, new SubSpace(bits));
      return (KNNQuery<V, D>) dq;
    }
    if(df instanceof LPNormDistanceFunction) {
      double p = ((LPNormDistanceFunction) df).getP();
      BitSet bits = fakeSubspace(distanceQuery.getRelation());
      DistanceQuery<V, ?> ddq = (DistanceQuery<V, ?>) distanceQuery;
      KNNQuery<V, ?> dq = new PartialVAFileKNNQuery((DistanceQuery<V, DoubleDistance>) ddq, p, new SubSpace(bits));
      return (KNNQuery<V, D>) dq;
    }
    // Not supported.
    return null;
  }

  @SuppressWarnings("unchecked")
  @Override
  public <D extends Distance<D>> RangeQuery<V, D> getRangeQuery(DistanceQuery<V, D> distanceQuery, Object... hints) {
    DistanceFunction<? super V, ?> df = distanceQuery.getDistanceFunction();
    if(df instanceof SubspaceLPNormDistanceFunction) {
      double p = ((SubspaceLPNormDistanceFunction) df).getP();
      BitSet bits = ((SubspaceLPNormDistanceFunction) df).getSelectedDimensions();
      DistanceQuery<V, ?> ddq = (DistanceQuery<V, ?>) distanceQuery;
      RangeQuery<V, ?> dq = new PartialVAFileRangeQuery((DistanceQuery<V, DoubleDistance>) ddq, p, bits);
      return (RangeQuery<V, D>) dq;
    }
    if(df instanceof LPNormDistanceFunction) {
      double p = ((LPNormDistanceFunction) df).getP();
      BitSet bits = fakeSubspace(distanceQuery.getRelation());
      DistanceQuery<V, ?> ddq = (DistanceQuery<V, ?>) distanceQuery;
      RangeQuery<V, ?> dq = new PartialVAFileRangeQuery((DistanceQuery<V, DoubleDistance>) ddq, p, bits);
      return (RangeQuery<V, D>) dq;
    }
    // Not supported.
    return null;
  }

  public static class Statistics {
    private long scannedBytes = 0;

    private long queryTime = 0;

    private int issuedQueries = 0;

    private int refinements = 0;
  }

  /**
   * Range query for this index.
   * 
   * @author Erich Schubert
   * @author Thomas Bernecker
   */
  class PartialVAFileRangeQuery extends AbstractRefiningIndex<V>.AbstractRangeQuery<DoubleDistance> {
    /**
     * Lp-Norm p
     */
    private double p;

    /**
     * Subspace
     */
    private BitSet subspace;

    /**
     * Constructor.
     * 
     * @param ddq Distance query
     * @param p LP Norm p
     * @param subspace Subspace
     */
    public PartialVAFileRangeQuery(DistanceQuery<V, DoubleDistance> ddq, double p, BitSet subspace) {
      super(ddq);
      this.p = p;
      this.subspace = subspace;
    }

    @Override
    public DistanceDBIDResult<DoubleDistance> getRangeForObject(V query, DoubleDistance range) {
      stats.issuedQueries++;
      long t = System.nanoTime();

      final double epsilon = ((DoubleDistance) range).doubleValue();

      // generate query approximation and lookup table
      VectorApproximation queryApprox = DAFile.calculateApproximation(null, query, daFiles);
      final VALPNormDistance dist = new VALPNormDistance(p, splitPartitions, query, queryApprox);

      // perform multi-step range query

      // filter step

      // calculate selectivity coefficients

      List<DAFile> subspaceDAFiles = new ArrayList<DAFile>(subspace.cardinality());
      for(int d = subspace.nextSetBit(0); d >= 0; d = subspace.nextSetBit(d + 1)) {
        DAFile daFile = daFiles.get(d);
        subspaceDAFiles.add(daFile);
      }
      DAFile.calculateSelectivityCoeffs(subspaceDAFiles, query, epsilon);
      // sort DA files by selectivity
      DAFile.sortBySelectivity(subspaceDAFiles);

      // for (Integer key: subspaceDims)
      // {
      // log.info("DAFile " + key + " - selectivity " +
      // daFiles.get(key).getSelectivityCoeff());
      // }

      // create candidate list (all objects) and prune candidates w.r.t.
      // mindist (i.e. remove them from the list)
      // important: this structure contains the maxDist values for refinement!
      DistanceDBIDResult<DoubleDistance> result = new GenericDistanceDBIDList<DoubleDistance>();
      final double onebyp = 1.0 / p;
      int candidates = 0;
      for(VectorApproximation va : vectorApprox) {
        DBID id = va.getId();
        PartialVACandidate pva = new PartialVACandidate(va);

        boolean pruned = false;
        for(DAFile da : subspaceDAFiles) {
          int dimension = da.getDimension();
          int objectCell = va.getApproximation(dimension);
          pva.increaseMinDistP(dist.getPartialMinDist(dimension, objectCell));
          pva.increaseMaxDistP(dist.getPartialMaxDist(dimension, objectCell));
          if(Math.pow(pva.getMinDistP(), onebyp) > epsilon) {
            pruned = true;
            break;
          }
        }
        if(!pruned) {
          candidates++;
          if(Math.pow(pva.getMaxDistP(), onebyp) <= epsilon) {
            // candidate cannot be dropped
            // TODO: actually: no refinement needed - need API that allows
            // reporting maxdists only.
            result.add(new DoubleDistanceResultPair(refine(id, query).doubleValue(), id));
          }
          else { // refine candidate - true refinement
            DoubleDistance dis = refine(id, query);
            stats.refinements += 1;
            if(dis.doubleValue() <= epsilon) {
              result.add(new DoubleDistanceResultPair(dis.doubleValue(), id));
            }
          }
        }
      }
      Collections.sort(result);

      // scannedBytes += vectorApprox.size() * vectorApprox.get(0).byteOnDisk();
      stats.scannedBytes += relation.size() * VectorApproximation.byteOnDisk(subspace.cardinality(), partitions);

      stats.queryTime += System.nanoTime() - t;

      if(logger.isDebuggingFine()) {
        logger.fine("\nquery = " + query);
        // log.info("database: " + vectorApprox.size() + ", candidates: " +
        // candidates.size() + ", results: " + resultIDs.size());
        logger.fine("database: " + relation.size() + ", candidates: " + candidates + ", results: " + result.size());
      }

      return result;
    }
  }

  /**
   * KNN query for this index.
   * 
   * @author Erich Schubert
   * @author Thomas Bernecker
   */
  class PartialVAFileKNNQuery extends AbstractRefiningIndex<V>.AbstractKNNQuery<DoubleDistance> {
    /**
     * Lp-Norm p
     */
    private double p;

    /**
     * Subspace
     */
    private SubSpace subspace;

    // TODO: make non-class variable
    private int currentSubspaceDims;

    /**
     * Constructor.
     * 
     * @param ddq Distance query
     * @param df Distance function
     */
    public PartialVAFileKNNQuery(DistanceQuery<V, DoubleDistance> ddq, double p, SubSpace subspace) {
      super(ddq);
      this.p = p;
      this.subspace = subspace;
    }

    @Override
    public KNNResult<DoubleDistance> getKNNForObject(V query, int k) {
      stats.issuedQueries++;
      long t = System.nanoTime();

      // generate query approximation and lookup table
      VectorApproximation queryApprox = DAFile.calculateApproximation(null, query, daFiles);
      final VALPNormDistance dist = new VALPNormDistance(p, splitPartitions, query, queryApprox);

      // sort DA files by worst case distance
      List<DAFile> daFiles = getWorstCaseDistOrder(dist, subspace);

      currentSubspaceDims = subspace.dimensions();
      int reducedDims = (2 * currentSubspaceDims) / 3;
      reducedDims = Math.max(1, reducedDims);
      if(logger.isDebuggingFine()) {
        logger.fine("subspaceDims=" + currentSubspaceDims + ", reducedDims=" + reducedDims);
      }

      // filter 1
      LinkedList<PartialVACandidate> candidates1 = filter1(k, reducedDims, daFiles, queryApprox, currentSubspaceDims, dist);
      // scannedBytes += vectorApprox.size() * (2/3 *
      // vectorApprox.get(0).byteOnDisk());
      // scannedBytes += vectorApprox.size() *
      // VectorApprox<V>.byteOnDisk(reducedDims, partitions);
      if(logger.isDebuggingFine()) {
        logger.fine("candidate set after filter 1: " + candidates1.size());
      }

      // filters 2+
      LinkedList<PartialVACandidate> candidates2 = null;
      int addition = reducedDims;
      int filterStep = 2;

      if(currentSubspaceDims <= reducedDims) {
        candidates2 = candidates1;
      }
      else {
        // continue filtering until I/O costs of refining candidates < I/O
        // costs of loading new DA files
        while(candidates2 == null || (getIOCosts(candidates2.size(), currentSubspaceDims) >= getIOCosts(daFiles.get(0), currentSubspaceDims - addition)) && addition < currentSubspaceDims) {
          if(candidates2 != null && logger.isDebuggingFine()) {
            logger.fine("filter " + filterStep + ": refining costs " + getIOCosts(candidates2.size(), currentSubspaceDims) + " (" + candidates2.size() + "/" + currentSubspaceDims + "), DA file costs " + getIOCosts(daFiles.get(0), currentSubspaceDims - addition) + " (dim " + (addition + 1) + " of " + currentSubspaceDims + ")");
            // + " (" + daFiles.get(0).getBorders().length + "/" +
            // (currentSubspaceDims - addition) + ")" +
          }
          if(candidates2 != null) {
            candidates1 = candidates2;
          }
          candidates2 = new LinkedList<PartialVACandidate>();

          Heap<Double> kMinMaxDists = new TopBoundedHeap<Double>(k, Collections.reverseOrder());
          for(PartialVACandidate va : candidates1) {
            int dimension = daFiles.get(addition).getDimension();
            int objectCell = va.getApproximation(dimension);

            va.increaseMinDistP(dist.getPartialMinDist(dimension, objectCell));
            va.decreaseMaxDistP(dist.getPartialMaxMaxDist(dimension));
            va.increaseMaxDistP(dist.getPartialMaxDist(dimension, objectCell));

            distanceCheck(kMinMaxDists, k, va, candidates2);

            // scannedBytes += vectorApprox.get(0).byteOnDisk();
            // read ONE additional dimension per object
            // scannedBytes += VectorApprox<V>.byteOnDisk(1, partitions);
          }

          // log.fine("filter2 took " + (System.currentTimeMillis() - tmp) +
          // " ms");
          // System.out.println("filter2 took " + (System.currentTimeMillis() -
          // tmp) + " ms");

          if(logger.isDebuggingFine()) {
            logger.fine("candidate set after filter " + filterStep + ": " + candidates2.size());
          }
          // System.out.println("candidate set after filter " + filterStep +
          // ": " + candidates2.size());
          // for (Integer i: candidates2) System.out.print(i + " ");
          // log.info();

          addition++;
          filterStep++;
        }
      }

      stats.scannedBytes += relation.size() * VectorApproximation.byteOnDisk(addition, partitions);

      // refinement step
      Vector<PartialVACandidate> sortedCandidates = new Vector<PartialVACandidate>(candidates2.size());
      for(PartialVACandidate va : candidates2) {
        // sortedCandidates.add(vectorApprox.get(id));
        sortedCandidates.add(va);
      }
      // sort candidates by lower bound (minDist)
      PartialVACandidate.sortByMinDist(sortedCandidates);
      KNNList<DoubleDistance> result = retrieveAccurateDistances(sortedCandidates, k, subspace, query);

      stats.queryTime += System.nanoTime() - t;
      return result;
    }

    private LinkedList<PartialVACandidate> filter1(int k, int reducedDims, List<DAFile> daFiles, VectorApproximation queryApprox, int subspaceDims, VALPNormDistance dist) {
      LinkedList<PartialVACandidate> candidates1 = new LinkedList<PartialVACandidate>();
      Heap<Double> minmaxdist = new TopBoundedHeap<Double>(k, Collections.reverseOrder());

      for(VectorApproximation va : vectorApprox) {
        PartialVACandidate pva = new PartialVACandidate(va);

        filter1Loop1(reducedDims, daFiles, pva, dist);
        filter1Loop2(reducedDims, subspaceDims, pva, daFiles, dist);
        distanceCheck(minmaxdist, k, pva, candidates1);
      }
      // Drop candidates that don't satisfy the latest minmaxdist
      final double minmax = minmaxdist.peek();
      Iterator<PartialVACandidate> it = candidates1.iterator();
      while(it.hasNext()) {
        PartialVACandidate pva = it.next();
        if(pva.getMinDistP() > minmax) {
          it.remove();
        }
      }

      return candidates1;
    }

    private void distanceCheck(Heap<Double> kMinMaxDists, int k, PartialVACandidate va, LinkedList<PartialVACandidate> candList) {
      if(kMinMaxDists.size() < k || va.getMinDistP() <= kMinMaxDists.peek()) {
        candList.add(va);
        kMinMaxDists.add(va.getMaxDistP());
      }
    }

    private void filter1Loop1(int reducedDims, List<DAFile> daFiles, PartialVACandidate va, VALPNormDistance dist) {
      for(int d = 0; d < reducedDims; d++) {
        int dimension = daFiles.get(d).getDimension();
        int objectCell = va.getApproximation(dimension);
        va.increaseMinDistP(dist.getPartialMinDist(dimension, objectCell));
        va.increaseMaxDistP(dist.getPartialMaxDist(dimension, objectCell));
      }
    }

    private void filter1Loop2(int reducedDims, int subspaceDims, PartialVACandidate va, List<DAFile> daFiles, VALPNormDistance dist) {
      for(int d = reducedDims; d < subspaceDims; d++) {
        va.increaseMaxDistP(dist.getPartialMaxMaxDist(daFiles.get(d).getDimension()));
      }
    }

    /**
     * Computes IO costs (in bytes) needed for refining the candidates.
     * 
     * @param size The nuber of candidates
     * @param subspaceDims the required subspace dimensions
     * @return the cost value (in bytes)
     */
    private int getIOCosts(int size, int subspaceDims) {
      return size * (subspaceDims * 8 + 4);
    }

    /**
     * Computes IO costs (in bytes) needed for reading several DA-files.
     * 
     * @param sample the DA-file specific costs
     * @param numberOfDAFiles the number of DA-files that have to be read
     * @return the cost value (in bytes)
     */
    private int getIOCosts(DAFile sample, int numberOfDAFiles) {
      return sample.getIOCosts() * numberOfDAFiles;
    }

    public List<DAFile> getWorstCaseDistOrder(VALPNormDistance dist, SubSpace subspace) {
      int subspaceLength = subspace.subspaceDimensions.length;
      List<DAFile> result = new ArrayList<DAFile>(subspaceLength);
      for(int i = 0; i < subspaceLength; i++) {
        result.add(daFiles.get(subspace.subspaceDimensions[i]));
      }
      Collections.sort(result, new WorstCaseDistComparator<V>(dist));
      return result;
    }

    protected KNNList<DoubleDistance> retrieveAccurateDistances(Vector<PartialVACandidate> sortedCandidates, int k, SubSpace subspace, V query) {
      KNNHeap<DoubleDistance> result = new KNNHeap<DoubleDistance>(k, DoubleDistance.FACTORY.infiniteDistance());
      for(PartialVACandidate va : sortedCandidates) {
        double stopdist = result.getKNNDistance().doubleValue();
        DBID currentID = va.getId();
        if(result.size() < k || va.getMinDistP() < stopdist) {
          DoubleDistance dist = refine(currentID, query);
          stats.refinements += 1;
          if(dist.doubleValue() < stopdist) {
            result.add(new DoubleDistanceResultPair(dist.doubleValue(), currentID));
          }
        }
      }
      return result.toKNNList();
    }
  }

  static class WorstCaseDistComparator<V extends NumberVector<?, ?>> implements Comparator<DAFile> {
    private VALPNormDistance dist;

    public WorstCaseDistComparator(VALPNormDistance dist) {
      this.dist = dist;
    }

    @Override
    public int compare(DAFile a, DAFile b) {
      return Double.compare(dist.getPartialMaxMaxDist(a.getDimension()), dist.getPartialMaxMaxDist(b.getDimension()));
    }
  }

  /**
   * Index factory class
   * 
   * @author Erich Schubert
   * 
   * @param <V> Vector type
   */
  public static class Factory<V extends NumberVector<?, ?>> implements IndexFactory<V, PartialVAFile<V>> {
    /**
     * Number of partitions to use in each dimension.
     * 
     * <pre>
     * -vafile.partitions 8
     * </pre>
     */
    public static final OptionID PARTITIONS_ID = OptionID.getOrCreateOptionID("vafile.partitions", "Number of partitions to use in each dimension.");

    /**
     * Page size
     */
    int pagesize = 1;

    /**
     * Number of partitions
     */
    int numpart = 2;

    /**
     * Constructor.
     * 
     * @param pagesize Page size
     * @param numpart Number of partitions
     */
    public Factory(int pagesize, int numpart) {
      super();
      this.pagesize = pagesize;
      this.numpart = numpart;
    }

    @Override
    public PartialVAFile<V> instantiate(Relation<V> relation) {
      return new PartialVAFile<V>(pagesize, relation, numpart);
    }

    @Override
    public TypeInformation getInputTypeRestriction() {
      return TypeUtil.NUMBER_VECTOR_FIELD;
    }

    /**
     * Parameterization class
     * 
     * @author Erich Schubert
     * 
     * @apiviz.exclude
     */
    public static class Parameterizer extends AbstractParameterizer {
      /**
       * Page size
       */
      int pagesize = 1;

      /**
       * Number of partitions
       */
      int numpart = 2;

      @Override
      protected void makeOptions(Parameterization config) {
        super.makeOptions(config);
        IntParameter pagesizeP = new IntParameter(TreeIndexFactory.PAGE_SIZE_ID, new GreaterConstraint(0), 1024);
        if(config.grab(pagesizeP)) {
          pagesize = pagesizeP.getValue();
        }
        IntParameter partitionsP = new IntParameter(Factory.PARTITIONS_ID, new GreaterConstraint(2));
        if(config.grab(partitionsP)) {
          numpart = partitionsP.getValue();
        }
      }

      @Override
      protected Factory<?> makeInstance() {
        return new Factory<NumberVector<?, ?>>(pagesize, numpart);
      }
    }
  }
}