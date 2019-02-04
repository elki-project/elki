/*
 * This file is part of ELKI:
 * Environment for Developing KDD-Applications Supported by Index-Structures
 *
 * Copyright (C) 2019
 * ELKI Development Team
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package de.lmu.ifi.dbs.elki.index.vafile;

import java.util.*;

import de.lmu.ifi.dbs.elki.data.DoubleVector;
import de.lmu.ifi.dbs.elki.data.NumberVector;
import de.lmu.ifi.dbs.elki.data.type.TypeInformation;
import de.lmu.ifi.dbs.elki.data.type.TypeUtil;
import de.lmu.ifi.dbs.elki.database.ids.*;
import de.lmu.ifi.dbs.elki.database.query.distance.DistanceQuery;
import de.lmu.ifi.dbs.elki.database.query.knn.KNNQuery;
import de.lmu.ifi.dbs.elki.database.query.range.RangeQuery;
import de.lmu.ifi.dbs.elki.database.relation.Relation;
import de.lmu.ifi.dbs.elki.database.relation.RelationUtil;
import de.lmu.ifi.dbs.elki.distance.distancefunction.DistanceFunction;
import de.lmu.ifi.dbs.elki.distance.distancefunction.minkowski.LPNormDistanceFunction;
import de.lmu.ifi.dbs.elki.distance.distancefunction.subspace.SubspaceLPNormDistanceFunction;
import de.lmu.ifi.dbs.elki.index.AbstractRefiningIndex;
import de.lmu.ifi.dbs.elki.index.IndexFactory;
import de.lmu.ifi.dbs.elki.index.KNNIndex;
import de.lmu.ifi.dbs.elki.index.RangeIndex;
import de.lmu.ifi.dbs.elki.logging.Logging;
import de.lmu.ifi.dbs.elki.logging.statistics.Counter;
import de.lmu.ifi.dbs.elki.math.MathUtil;
import de.lmu.ifi.dbs.elki.persistent.AbstractPageFileFactory;
import de.lmu.ifi.dbs.elki.utilities.datastructures.BitsUtil;
import de.lmu.ifi.dbs.elki.utilities.datastructures.heap.DoubleMaxHeap;
import de.lmu.ifi.dbs.elki.utilities.documentation.Reference;
import de.lmu.ifi.dbs.elki.utilities.io.ByteArrayUtil;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.AbstractParameterizer;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.OptionID;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.constraints.CommonConstraints;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.Parameterization;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.IntParameter;
import de.lmu.ifi.dbs.elki.utilities.pairs.DoubleObjPair;

import net.jafama.FastMath;

/**
 * PartialVAFile. In-memory only implementation.
 * <p>
 * Reference:
 * <p>
 * Hans-Peter Kriegel, Peer Kröger, Matthias Schubert, Ziyue Zhu<br>
 * Efficient Query Processing in Arbitrary Subspaces Using Vector
 * Approximations<br>
 * Proc. 18th Int. Conf. on Scientific and Statistical Database Management
 * (SSDBM 06)
 *
 * @author Thomas Bernecker
 * @author Erich Schubert
 * @since 0.5.0
 *
 * @opt nodefillcolor LemonChiffon
 * @composed - - - DAFile
 * @has - - - PartialVACandidate
 * @has - - - PartialVAFileRangeQuery
 * @has - - - PartialVAFileKNNQuery
 *
 * @param <V> Vector type
 */
@Reference(authors = "Hans-Peter Kriegel, Peer Kröger, Matthias Schubert, Ziyue Zhu", //
    title = "Efficient Query Processing in Arbitrary Subspaces Using Vector Approximations", //
    booktitle = "Proc. 18th Int. Conf. on Scientific and Statistical Database Management (SSDBM 06)", //
    url = "https://doi.org/10.1109/SSDBM.2006.23", //
    bibkey = "DBLP:conf/ssdbm/KriegelKSZ06")
public class PartialVAFile<V extends NumberVector> extends AbstractRefiningIndex<V> implements KNNIndex<V>, RangeIndex<V> {
  /**
   * Class logger.
   */
  private static final Logging LOG = Logging.getLogger(PartialVAFile.class);

  /**
   * Partial VA files.
   */
  List<DAFile> daFiles;

  /**
   * Number of partitions.
   */
  private final int partitions;

  /**
   * Page size.
   */
  private final int pageSize;

  /**
   * Splitting grid.
   */
  private double[][] splitPartitions;

  /**
   * Statistics.
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
    this.stats = new Statistics(this.getClass().getName());
  }

  @Override
  public void initialize() throws IllegalStateException {
    if(splitPartitions != null) {
      throw new IllegalStateException("Data already inserted.");
    }

    if(MathUtil.log2(partitions) != (int) MathUtil.log2(partitions)) {
      throw new IllegalArgumentException("Number of partitions must be a power of 2!");
    }

    final int dimensions = RelationUtil.dimensionality(relation);

    splitPartitions = new double[dimensions][];
    daFiles = new ArrayList<>(dimensions);
    for(int d = 0; d < dimensions; d++) {
      final DAFile f = new DAFile(relation, d, partitions);
      splitPartitions[d] = f.getSplitPositions();
      daFiles.add(f);
    }

    vectorApprox = new ArrayList<>();
    for(DBIDIter iter = relation.iterDBIDs(); iter.valid(); iter.advance()) {
      DBID id = DBIDUtil.deref(iter);
      V dv = relation.get(id);
      VectorApproximation va = calculateFullApproximation(id, dv);
      vectorApprox.add(va);
    }
  }

  @Override
  public String getShortName() {
    return "pva-file";
  }

  @Override
  public String getLongName() {
    return "partial va-file";
  }

  @Override
  public Logging getLogger() {
    return LOG;
  }

  @Override
  public void logStatistics() {
    stats.logStatistics();
  }

  /**
   * Calculate the VA file position given the existing borders.
   * 
   * @param id Object ID
   * @param dv Data vector
   * @return Vector approximation
   */
  protected VectorApproximation calculateFullApproximation(DBID id, V dv) {
    int[] approximation = new int[dv.getDimensionality()];
    for(int d = 0; d < splitPartitions.length; d++) {
      double[] split = daFiles.get(d).getSplitPositions();
      final double val = dv.doubleValue(d);
      final int lastBorderIndex = split.length - 1;

      // Value is below data grid
      if(val < split[0]) {
        approximation[d] = 0;
        if(id != null) {
          LOG.warning("Vector outside of VAFile grid!");
        }
      } // Value is above data grid
      else if(val > split[lastBorderIndex]) {
        approximation[d] = lastBorderIndex - 1;
        if(id != null) {
          LOG.warning("Vector outside of VAFile grid!");
        }
      } // normal case
      else {
        // Search grid position
        int pos = Arrays.binarySearch(split, val);
        pos = (pos >= 0) ? pos : ((-pos) - 2);
        approximation[d] = pos;
      }
    }
    return new VectorApproximation(id, approximation);
  }

  @Override
  public KNNQuery<V> getKNNQuery(DistanceQuery<V> distanceQuery, Object... hints) {
    DistanceFunction<? super V> df = distanceQuery.getDistanceFunction();
    if(df instanceof SubspaceLPNormDistanceFunction) {
      double p = ((SubspaceLPNormDistanceFunction) df).getP();
      long[] bits = ((SubspaceLPNormDistanceFunction) df).getSelectedDimensions();
      return new PartialVAFileKNNQuery(distanceQuery, p, bits);
    }
    if(df instanceof LPNormDistanceFunction) {
      double p = ((LPNormDistanceFunction) df).getP();
      long[] bits = BitsUtil.ones(RelationUtil.dimensionality(distanceQuery.getRelation()));
      return new PartialVAFileKNNQuery(distanceQuery, p, bits);
    }
    // Not supported.
    return null;
  }

  @Override
  public RangeQuery<V> getRangeQuery(DistanceQuery<V> distanceQuery, Object... hints) {
    DistanceFunction<? super V> df = distanceQuery.getDistanceFunction();
    if(df instanceof SubspaceLPNormDistanceFunction) {
      double p = ((SubspaceLPNormDistanceFunction) df).getP();
      long[] bits = ((SubspaceLPNormDistanceFunction) df).getSelectedDimensions();
      return new PartialVAFileRangeQuery(distanceQuery, p, bits);
    }
    if(df instanceof LPNormDistanceFunction) {
      double p = ((LPNormDistanceFunction) df).getP();
      long[] bits = BitsUtil.ones(RelationUtil.dimensionality(distanceQuery.getRelation()));
      return new PartialVAFileRangeQuery(distanceQuery, p, bits);
    }
    // Not supported.
    return null;
  }

  /**
   * Calculate selectivity coefficients.
   * 
   * @param daFiles List of files to use
   * @param query Query vector
   * @param epsilon Epsilon radius
   */
  protected static void calculateSelectivityCoeffs(List<DoubleObjPair<DAFile>> daFiles, NumberVector query, double epsilon) {
    final int dimensions = query.getDimensionality();
    double[] lowerVals = new double[dimensions];
    double[] upperVals = new double[dimensions];

    VectorApproximation queryApprox = calculatePartialApproximation(null, query, daFiles);

    for(int i = 0; i < dimensions; i++) {
      final double val = query.doubleValue(i);
      lowerVals[i] = val - epsilon;
      upperVals[i] = val + epsilon;
    }

    DoubleVector lowerEpsilon = DoubleVector.wrap(lowerVals);
    VectorApproximation lowerEpsilonPartitions = calculatePartialApproximation(null, lowerEpsilon, daFiles);

    DoubleVector upperEpsilon = DoubleVector.wrap(upperVals);
    VectorApproximation upperEpsilonPartitions = calculatePartialApproximation(null, upperEpsilon, daFiles);

    for(int i = 0; i < daFiles.size(); i++) {
      int coeff = (queryApprox.getApproximation(i) - lowerEpsilonPartitions.getApproximation(i)) + (upperEpsilonPartitions.getApproximation(i) - queryApprox.getApproximation(i)) + 1;
      daFiles.get(i).first = coeff;
    }
  }

  /**
   * Calculate partial vector approximation.
   * 
   * @param id Object ID
   * @param dv Object vector
   * @param daFiles List of approximations to use
   * @return Vector approximation
   */
  protected static VectorApproximation calculatePartialApproximation(DBID id, NumberVector dv, List<DoubleObjPair<DAFile>> daFiles) {
    int[] approximation = new int[dv.getDimensionality()];
    for(int i = 0; i < daFiles.size(); i++) {
      double val = dv.doubleValue(i);
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

  /**
   * Class for tracking Partial VA file statistics.
   * 
   * TODO: refactor into a common statistics API
   */
  public static class Statistics {
    private Counter scannedBytes;

    private Counter queryTime;

    private Counter issuedQueries;

    private Counter refinements;

    protected Statistics(String parent) {
      scannedBytes = LOG.isStatistics() ? LOG.newCounter(parent + ".scannedBytes") : null;
      queryTime = LOG.isStatistics() ? LOG.newCounter(parent + ".queryTime") : null;
      issuedQueries = LOG.isStatistics() ? LOG.newCounter(parent + ".issuedQueries") : null;
      refinements = LOG.isStatistics() ? LOG.newCounter(parent + ".refinements") : null;
    }

    public void logStatistics() {
      if(scannedBytes != null) {
        LOG.statistics(scannedBytes);
      }
      if(queryTime != null) {
        LOG.statistics(queryTime);
      }
      if(issuedQueries != null) {
        LOG.statistics(issuedQueries);
      }
      if(refinements != null) {
        LOG.statistics(refinements);
      }
    }

    protected void incrementScannedBytes(long bytes) {
      if(scannedBytes != null) {
        scannedBytes.increment(bytes);
      }
    }

    protected void incrementQueryTime(long time) {
      if(queryTime != null) {
        queryTime.increment(time);
      }
    }

    protected void incrementIssuedQueries() {
      if(issuedQueries != null) {
        issuedQueries.increment();
      }
    }

    protected void incrementRefinements() {
      if(refinements != null) {
        refinements.increment();
      }
    }
  }

  /**
   * Object in a VA approximation.
   * 
   * @author Thomas Bernecker
   * @author Erich Schubert
   */
  protected static class PartialVACandidate implements Comparable<PartialVACandidate> {
    /**
     * (Current) maximum distance of this candidate.
     */
    protected double maxDistP = 0.0;

    /**
     * (Current) minimum distance of this candidate.
     */
    protected double minDistP = 0.0;

    /**
     * The actual approximation.
     */
    final private VectorApproximation approx;

    /**
     * 
     * Constructor.
     * 
     * @param approx The actual approximation
     */
    public PartialVACandidate(VectorApproximation approx) {
      super();
      this.approx = approx;
    }

    public int getApproximation(int dimension) {
      return approx.getApproximation(dimension);
    }

    public DBID getId() {
      return approx.getId();
    }

    @Override
    public String toString() {
      return approx.toString() + ", bounds^p: [" + minDistP + ", " + maxDistP + "]";
    }

    @Override
    public int compareTo(PartialVACandidate o) {
      return Double.compare(this.minDistP, o.minDistP);
    }
  }

  /**
   * Range query for this index.
   * 
   * @author Erich Schubert
   * @author Thomas Bernecker
   */
  public class PartialVAFileRangeQuery extends AbstractRefiningIndex<V>.AbstractRangeQuery {
    /**
     * Lp-Norm p.
     */
    private double p;

    /**
     * Subspace.
     */
    private long[] subspace;

    /**
     * Constructor.
     * 
     * @param ddq Distance query
     * @param p LP Norm p
     * @param subspace Subspace
     */
    public PartialVAFileRangeQuery(DistanceQuery<V> ddq, double p, long[] subspace) {
      super(ddq);
      this.p = p;
      this.subspace = subspace;
    }

    @Override
    public void getRangeForObject(V query, double range, ModifiableDoubleDBIDList result) {
      stats.incrementIssuedQueries();
      long t = System.nanoTime();

      final double epsilonP = FastMath.pow(range, p);

      // generate query approximation and lookup table
      final VectorApproximation queryApprox = calculateFullApproximation(null, query);
      final VALPNormDistance dist = new VALPNormDistance(p, splitPartitions, query, queryApprox);

      // perform multi-step range query

      // filter step

      // calculate selectivity coefficients
      List<DoubleObjPair<DAFile>> subspaceDAFiles = new ArrayList<>(BitsUtil.cardinality(subspace));
      for(int d = BitsUtil.nextSetBit(subspace, 0); d >= 0; d = BitsUtil.nextSetBit(subspace, d + 1)) {
        DAFile daFile = daFiles.get(d);
        subspaceDAFiles.add(new DoubleObjPair<>(-1, daFile));
      }
      calculateSelectivityCoeffs(subspaceDAFiles, query, range);
      // sort DA files by selectivity
      // TODO: validate that this is the correct order
      Collections.sort(subspaceDAFiles, Collections.reverseOrder());

      // create candidate list (all objects) and prune candidates w.r.t.
      // mindist (i.e. remove them from the list)
      // important: this structure contains the maxDist values for refinement!
      int candidates = 0;
      for(VectorApproximation va : vectorApprox) {
        DBID id = va.getId();
        PartialVACandidate pva = new PartialVACandidate(va);

        boolean pruned = false;
        for(DoubleObjPair<DAFile> da : subspaceDAFiles) {
          int dimension = da.second.getDimension();
          int objectCell = va.getApproximation(dimension);
          pva.minDistP += dist.getPartialMinDist(dimension, objectCell);
          pva.maxDistP += dist.getPartialMaxDist(dimension, objectCell);
          if(pva.minDistP > epsilonP) {
            pruned = true;
            break;
          }
        }
        if(!pruned) {
          candidates++;
          if(pva.maxDistP <= epsilonP) {
            // candidate cannot be dropped
            // TODO: actually: no refinement needed - need API that allows
            // reporting maxdists only.
            result.add(refine(id, query), id);
          }
          else { // refine candidate - true refinement
            double dis = refine(id, query);
            stats.incrementRefinements();
            if(dis <= range) {
              result.add(dis, id);
            }
          }
        }
      }
      result.sort();

      stats.incrementScannedBytes(relation.size() * VectorApproximation.byteOnDisk(BitsUtil.cardinality(subspace), partitions));

      stats.incrementQueryTime(System.nanoTime() - t);

      if(LOG.isDebuggingFine()) {
        LOG.fine("query = " + query);
        LOG.fine("database: " + relation.size() + ", candidates: " + candidates + ", results: " + result.size());
      }
    }
  }

  /**
   * KNN query for this index.
   * 
   * @author Erich Schubert
   * @author Thomas Bernecker
   */
  public class PartialVAFileKNNQuery extends AbstractRefiningIndex<V>.AbstractKNNQuery {
    /**
     * Lp-Norm p.
     */
    private double p;

    /**
     * Subspace.
     */
    private long[] subspace;

    /**
     * Constructor.
     * 
     * @param ddq Distance query
     * @param p LP-norm p
     * @param subspace Subspace to query
     */
    public PartialVAFileKNNQuery(DistanceQuery<V> ddq, double p, long[] subspace) {
      super(ddq);
      this.p = p;
      this.subspace = subspace;
    }

    @Override
    public KNNList getKNNForObject(V query, int k) {
      stats.incrementIssuedQueries();
      long t = System.nanoTime();

      // generate query approximation and lookup table
      VectorApproximation queryApprox = calculateFullApproximation(null, query);
      final VALPNormDistance dist = new VALPNormDistance(p, splitPartitions, query, queryApprox);

      // sort DA files by worst case distance
      List<DAFile> daFiles = getWorstCaseDistOrder(dist, subspace);

      final int currentSubspaceDims = BitsUtil.cardinality(subspace);
      int reducedDims = (2 * currentSubspaceDims) / 3;
      reducedDims = Math.max(1, reducedDims);
      if(LOG.isDebuggingFine()) {
        LOG.fine("subspaceDims=" + currentSubspaceDims + ", reducedDims=" + reducedDims);
      }

      // filter 1
      LinkedList<PartialVACandidate> candidates1 = filter1(k, reducedDims, daFiles, queryApprox, currentSubspaceDims, dist);
      if(LOG.isDebuggingFine()) {
        LOG.fine("candidate set after filter 1: " + candidates1.size());
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
          if(candidates2 != null && LOG.isDebuggingFine()) {
            LOG.fine("filter " + filterStep + ": refining costs " + getIOCosts(candidates2.size(), currentSubspaceDims) + " (" + candidates2.size() + "/" + currentSubspaceDims + "), DA file costs " + getIOCosts(daFiles.get(0), currentSubspaceDims - addition) + " (dim " + (addition + 1) + " of " + currentSubspaceDims + ")");
          }
          if(candidates2 != null) {
            candidates1 = candidates2;
          }
          candidates2 = new LinkedList<>();

          DoubleMaxHeap kMinMaxDists = new DoubleMaxHeap(k + 1);
          for(PartialVACandidate va : candidates1) {
            int dimension = daFiles.get(addition).getDimension();
            int objectCell = va.getApproximation(dimension);

            va.minDistP += dist.getPartialMinDist(dimension, objectCell);
            va.maxDistP += dist.getPartialMaxDist(dimension, objectCell) - dist.getPartialMaxMaxDist(dimension);

            if(kMinMaxDists.size() < k || va.minDistP <= kMinMaxDists.peek()) {
              candidates2.add(va);
              kMinMaxDists.add(va.maxDistP, k);
            }
          }

          if(LOG.isDebuggingFine()) {
            LOG.fine("candidate set after filter " + filterStep + ": " + candidates2.size());
          }

          addition++;
          filterStep++;
        }
      }

      stats.incrementScannedBytes(relation.size() * VectorApproximation.byteOnDisk(addition, partitions));

      // refinement step
      ArrayList<PartialVACandidate> sortedCandidates = new ArrayList<>(candidates2);
      // sort candidates by lower bound (minDist)
      Collections.sort(sortedCandidates);
      KNNList result = retrieveAccurateDistances(sortedCandidates, k, subspace, query);

      stats.incrementQueryTime(System.nanoTime() - t);
      return result;
    }

    private LinkedList<PartialVACandidate> filter1(int k, int reducedDims, List<DAFile> daFiles, VectorApproximation queryApprox, int subspaceDims, VALPNormDistance dist) {
      LinkedList<PartialVACandidate> candidates1 = new LinkedList<>();
      DoubleMaxHeap minmaxdist = new DoubleMaxHeap(k + 1);

      for(VectorApproximation va : vectorApprox) {
        PartialVACandidate pva = new PartialVACandidate(va);
        for(int d = 0; d < reducedDims; d++) {
          int dimension = daFiles.get(d).getDimension();
          int objectCell = pva.getApproximation(dimension);
          pva.minDistP += dist.getPartialMinDist(dimension, objectCell);
          pva.maxDistP += dist.getPartialMaxDist(dimension, objectCell);
        }
        for(int d = reducedDims; d < subspaceDims; d++) {
          pva.maxDistP += dist.getPartialMaxMaxDist(daFiles.get(d).getDimension());
        }
        if(minmaxdist.size() < k || pva.minDistP <= minmaxdist.peek()) {
          candidates1.add(pva);
          minmaxdist.add(pva.maxDistP, k);
        }
      }
      // Drop candidates that don't satisfy the latest minmaxdist
      final double minmax = minmaxdist.peek();
      Iterator<PartialVACandidate> it = candidates1.iterator();
      while(it.hasNext()) {
        PartialVACandidate pva = it.next();
        if(pva.minDistP > minmax) {
          it.remove();
        }
      }

      return candidates1;
    }

    /**
     * Computes IO costs (in bytes) needed for refining the candidates.
     * 
     * @param size The nuber of candidates
     * @param subspaceDims the required subspace dimensions
     * @return the cost value (in bytes)
     */
    private int getIOCosts(int size, int subspaceDims) {
      return size * (subspaceDims * ByteArrayUtil.SIZE_DOUBLE + 4);
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

    /**
     * Order subspaces by their worst case distance.
     * 
     * @param dist Distance function
     * @param subspace Subspace
     * @return Ordered list of dimension files
     */
    public List<DAFile> getWorstCaseDistOrder(VALPNormDistance dist, long[] subspace) {
      int subspaceLength = BitsUtil.cardinality(subspace);
      List<DAFile> result = new ArrayList<>(subspaceLength);
      for(int i = BitsUtil.nextSetBit(subspace, 0); i >= 0; i = BitsUtil.nextSetBit(subspace, i + 1)) {
        result.add(daFiles.get(i));
      }
      Collections.sort(result, new WorstCaseDistComparator(dist));
      return result;
    }

    protected KNNList retrieveAccurateDistances(List<PartialVACandidate> sortedCandidates, int k, long[] subspace, V query) {
      KNNHeap result = DBIDUtil.newHeap(k);
      for(PartialVACandidate va : sortedCandidates) {
        double stopdist = result.getKNNDistance();
        DBID currentID = va.getId();
        if(result.size() < k || va.minDistP < stopdist) {
          double dist = refine(currentID, query);
          stats.incrementRefinements();
          if(dist < stopdist) {
            result.insert(dist, currentID);
          }
        }
      }
      return result.toKNNList();
    }
  }

  /**
   * Compare DAfiles by their worst case distance.
   */
  protected static class WorstCaseDistComparator implements Comparator<DAFile> {
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
   * Index factory class.
   * 
   * @author Erich Schubert
   * 
   * @stereotype factory
   * @has - - - PartialVAFile
   * 
   * @param <V> Vector type
   */
  public static class Factory<V extends NumberVector> implements IndexFactory<V> {
    /**
     * Number of partitions to use in each dimension.
     * 
     * <pre>
     * -vafile.partitions 8
     * </pre>
     */
    public static final OptionID PARTITIONS_ID = new OptionID("vafile.partitions", "Number of partitions to use in each dimension.");

    /**
     * Page size.
     */
    int pagesize = 1;

    /**
     * Number of partitions.
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
      return new PartialVAFile<>(pagesize, relation, numpart);
    }

    @Override
    public TypeInformation getInputTypeRestriction() {
      return TypeUtil.NUMBER_VECTOR_FIELD;
    }

    /**
     * Parameterization class.
     * 
     * @author Erich Schubert
     */
    public static class Parameterizer extends AbstractParameterizer {
      /**
       * Page size.
       */
      int pagesize = 1;

      /**
       * Number of partitions.
       */
      int numpart = 2;

      @Override
      protected void makeOptions(Parameterization config) {
        super.makeOptions(config);
        IntParameter pagesizeP = new IntParameter(AbstractPageFileFactory.Parameterizer.PAGE_SIZE_ID, 1024) //
            .addConstraint(CommonConstraints.GREATER_EQUAL_ONE_INT);
        if(config.grab(pagesizeP)) {
          pagesize = pagesizeP.getValue();
        }
        IntParameter partitionsP = new IntParameter(Factory.PARTITIONS_ID) //
            .addConstraint(CommonConstraints.GREATER_THAN_ONE_INT);
        if(config.grab(partitionsP)) {
          numpart = partitionsP.getValue();
        }
      }

      @Override
      protected Factory<?> makeInstance() {
        return new Factory<>(pagesize, numpart);
      }
    }
  }
}
