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
package de.lmu.ifi.dbs.elki.algorithm.clustering.subspace;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

import de.lmu.ifi.dbs.elki.algorithm.AbstractAlgorithm;
import de.lmu.ifi.dbs.elki.algorithm.clustering.em.EM;
import de.lmu.ifi.dbs.elki.algorithm.clustering.em.EMClusterModel;
import de.lmu.ifi.dbs.elki.algorithm.clustering.em.MultivariateGaussianModel;
import de.lmu.ifi.dbs.elki.data.Cluster;
import de.lmu.ifi.dbs.elki.data.Clustering;
import de.lmu.ifi.dbs.elki.data.NumberVector;
import de.lmu.ifi.dbs.elki.data.Subspace;
import de.lmu.ifi.dbs.elki.data.VectorUtil;
import de.lmu.ifi.dbs.elki.data.VectorUtil.SortDBIDsBySingleDimension;
import de.lmu.ifi.dbs.elki.data.model.SubspaceModel;
import de.lmu.ifi.dbs.elki.data.type.TypeInformation;
import de.lmu.ifi.dbs.elki.data.type.TypeUtil;
import de.lmu.ifi.dbs.elki.database.Database;
import de.lmu.ifi.dbs.elki.database.datastore.DataStoreFactory;
import de.lmu.ifi.dbs.elki.database.datastore.DataStoreUtil;
import de.lmu.ifi.dbs.elki.database.datastore.WritableDataStore;
import de.lmu.ifi.dbs.elki.database.ids.ArrayModifiableDBIDs;
import de.lmu.ifi.dbs.elki.database.ids.DBIDArrayIter;
import de.lmu.ifi.dbs.elki.database.ids.DBIDIter;
import de.lmu.ifi.dbs.elki.database.ids.DBIDMIter;
import de.lmu.ifi.dbs.elki.database.ids.DBIDUtil;
import de.lmu.ifi.dbs.elki.database.ids.DBIDs;
import de.lmu.ifi.dbs.elki.database.ids.HashSetModifiableDBIDs;
import de.lmu.ifi.dbs.elki.database.ids.ModifiableDBIDs;
import de.lmu.ifi.dbs.elki.database.ids.SetDBIDs;
import de.lmu.ifi.dbs.elki.database.relation.Relation;
import de.lmu.ifi.dbs.elki.database.relation.RelationUtil;
import de.lmu.ifi.dbs.elki.logging.Logging;
import de.lmu.ifi.dbs.elki.logging.progress.MutableProgress;
import de.lmu.ifi.dbs.elki.logging.progress.StepProgress;
import de.lmu.ifi.dbs.elki.math.MathUtil;
import de.lmu.ifi.dbs.elki.math.MeanVariance;
import de.lmu.ifi.dbs.elki.math.linearalgebra.CovarianceMatrix;
import de.lmu.ifi.dbs.elki.math.linearalgebra.VMath;
import de.lmu.ifi.dbs.elki.math.statistics.distribution.ChiSquaredDistribution;
import de.lmu.ifi.dbs.elki.math.statistics.distribution.PoissonDistribution;
import de.lmu.ifi.dbs.elki.utilities.Priority;
import de.lmu.ifi.dbs.elki.utilities.datastructures.BitsUtil;
import de.lmu.ifi.dbs.elki.utilities.documentation.Reference;
import de.lmu.ifi.dbs.elki.utilities.documentation.Title;
import de.lmu.ifi.dbs.elki.utilities.io.FormatUtil;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.AbstractParameterizer;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.OptionID;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.constraints.CommonConstraints;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.Parameterization;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.DoubleParameter;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.IntParameter;

/**
 * P3C: A Robust Projected Clustering Algorithm.
 * <p>
 * Reference:<br>
 * Gabriela Moise, Jörg Sander, Martin Ester<br>
 * P3C: A Robust Projected Clustering Algorithm<br>
 * In: Proc. Sixth International Conference on Data Mining (ICDM '06)
 * <p>
 * This is not a complete implementation of P3C, but good enough for most users.
 * Improvements are welcome. The most obviously missing step is section 3.5 of
 * P3C, where the cluster subspaces are refined.
 * 
 * @author Florian Nuecke
 * @author Erich Schubert
 * @since 0.6.0
 * 
 * @assoc - - - EM
 * @has - - - SubspaceModel
 * @has - - - ClusterCandidate
 * @has - - - Signature
 * 
 * @param <V> the type of NumberVector handled by this Algorithm.
 */
@Title("P3C: A Robust Projected Clustering Algorithm.")
@Reference(authors = "Gabriela Moise, Jörg Sander, Martin Ester", //
    title = "P3C: A Robust Projected Clustering Algorithm", //
    booktitle = "Proc. Sixth International Conference on Data Mining (ICDM '06)", //
    url = "https://doi.org/10.1109/ICDM.2006.123", //
    bibkey = "DBLP:conf/icdm/MoiseSE06")
@Priority(Priority.RECOMMENDED - 10) // More specialized
public class P3C<V extends NumberVector> extends AbstractAlgorithm<Clustering<SubspaceModel>> implements SubspaceClusteringAlgorithm<SubspaceModel> {
  /**
   * The logger for this class.
   */
  private static final Logging LOG = Logging.getLogger(P3C.class);

  /**
   * Parameter for the Poisson test threshold.
   */
  protected double poissonThreshold;

  /**
   * Maximum number of iterations for the EM step.
   */
  protected int maxEmIterations;

  /**
   * Threshold when to stop EM iterations.
   */
  protected double emDelta;

  /**
   * Minimum cluster size for noise flagging. (Not existing in the original
   * publication).
   */
  protected int minClusterSize;

  /**
   * Alpha threshold for testing.
   */
  protected double alpha = 0.001;

  /**
   * Constructor.
   * 
   * @param alpha ChiSquared test threshold
   * @param poissonThreshold Poisson test threshold
   * @param maxEmIterations Maximum number of EM iterations
   * @param emDelta EM stopping threshold
   * @param minClusterSize Minimum cluster size
   */
  public P3C(double alpha, double poissonThreshold, int maxEmIterations, double emDelta, int minClusterSize) {
    super();
    this.alpha = alpha;
    this.poissonThreshold = poissonThreshold;
    this.maxEmIterations = maxEmIterations;
    this.emDelta = emDelta;
    this.minClusterSize = minClusterSize;
  }

  /**
   * Performs the P3C algorithm on the given Database.
   */
  public Clustering<SubspaceModel> run(Database database, Relation<V> relation) {
    final int dim = RelationUtil.dimensionality(relation);

    // Overall progress.
    StepProgress stepProgress = LOG.isVerbose() ? new StepProgress(8) : null;

    if(stepProgress != null) {
      stepProgress.beginStep(1, "Grid-partitioning data.", LOG);
    }

    // Desired number of bins, as per Sturge:
    final int binCount = (int) Math.ceil(1 + MathUtil.log2(relation.size()));

    // Perform 1-dimensional projections, and split into bins.
    SetDBIDs[][] partitions = partitionData(relation, binCount);

    if(stepProgress != null) {
      stepProgress.beginStep(2, "Searching for non-uniform bins in support histograms.", LOG);
    }

    // Set markers for each attribute until they're all deemed uniform.
    final long[][] markers = new long[dim][];
    for(int d = 0; d < dim; d++) {
      final SetDBIDs[] parts = partitions[d];
      if(parts == null) {
        continue; // Never mark any on constant dimensions.
      }
      final long[] marked = markers[d] = BitsUtil.zero(binCount);
      int card = 0;
      while(card < dim - 1) {
        // Find bin with largest support, test only the dimensions that were not
        // previously marked.
        int bestBin = chiSquaredUniformTest(parts, marked, card);
        if(bestBin < 0) {
          break; // Uniform
        }
        BitsUtil.setI(marked, bestBin);
        card++;
      }
      if(LOG.isDebugging()) {
        LOG.debug("Marked bins in dim " + d + ": " + BitsUtil.toString(marked, binCount));
      }
    }

    if(stepProgress != null) {
      stepProgress.beginStep(3, "Merging marked bins to 1-signatures.", LOG);
    }

    ArrayList<Signature> signatures = constructOneSignatures(partitions, markers);

    if(stepProgress != null) {
      stepProgress.beginStep(4, "Computing cluster cores from merged p-signatures.", LOG);
    }

    ArrayList<Signature> clusterCores = mergeClusterCores(binCount, signatures);

    if(stepProgress != null) {
      stepProgress.beginStep(5, "Pruning redundant cluster cores.", LOG);
    }

    clusterCores = pruneRedundantClusterCores(clusterCores);
    if(LOG.isVerbose()) {
      LOG.verbose("Number of cluster cores found: " + clusterCores.size());
    }

    if(clusterCores.isEmpty()) {
      LOG.setCompleted(stepProgress);
      Clustering<SubspaceModel> c = new Clustering<>("P3C", "P3C");
      c.addToplevelCluster(new Cluster<SubspaceModel>(relation.getDBIDs(), true));
      return c;
    }

    if(stepProgress != null) {
      stepProgress.beginStep(5, "Refining cluster cores to clusters via EM.", LOG);
    }

    // Track objects not assigned to any cluster:
    ModifiableDBIDs noise = DBIDUtil.newHashSet();
    WritableDataStore<double[]> probClusterIGivenX = DataStoreUtil.makeStorage(relation.getDBIDs(), DataStoreFactory.HINT_HOT | DataStoreFactory.HINT_SORTED, double[].class);
    int k = clusterCores.size();
    List<MultivariateGaussianModel> models = new ArrayList<>(k);
    computeFuzzyMembership(relation, clusterCores, noise, probClusterIGivenX, models, dim);

    // Initial estimate of covariances, to assign noise objects
    EM.recomputeCovarianceMatrices(relation, probClusterIGivenX, models, 0.);
    assignUnassigned(relation, probClusterIGivenX, models, noise);

    double emNew = EM.assignProbabilitiesToInstances(relation, models, probClusterIGivenX);
    for(int it = 1; it <= maxEmIterations || maxEmIterations < 0; it++) {
      final double emOld = emNew;
      EM.recomputeCovarianceMatrices(relation, probClusterIGivenX, models, 0.);
      // reassign probabilities
      emNew = EM.assignProbabilitiesToInstances(relation, models, probClusterIGivenX);

      if(LOG.isVerbose()) {
        LOG.verbose("iteration " + it + " - expectation value: " + emNew);
      }
      if((emNew - emOld) <= emDelta) {
        break;
      }
    }

    // Perform EM clustering.

    if(stepProgress != null) {
      stepProgress.beginStep(6, "Generating hard clustering.", LOG);
    }

    // Create a hard clustering, making sure each data point only is part of one
    // cluster, based on the best match from the membership matrix.
    ArrayList<ClusterCandidate> clusterCandidates = hardClustering(probClusterIGivenX, clusterCores, relation.getDBIDs());

    if(stepProgress != null) {
      stepProgress.beginStep(7, "Looking for outliers and moving them to the noise set.", LOG);
    }

    // Outlier detection. Remove points from clusters that have a Mahalanobis
    // distance larger than the critical value of the ChiSquare distribution.
    findOutliers(relation, models, clusterCandidates, noise);

    if(stepProgress != null) {
      stepProgress.beginStep(8, "Removing empty clusters.", LOG);
    }

    // Remove near-empty clusters.
    for(Iterator<ClusterCandidate> it = clusterCandidates.iterator(); it.hasNext();) {
      ClusterCandidate cand = it.next();
      final int size = cand.ids.size();
      if(size < minClusterSize) {
        if(size > 0) {
          noise.addDBIDs(cand.ids);
        }
        it.remove();
      }
    }

    if(LOG.isVerbose()) {
      LOG.verbose("Number of clusters remaining: " + clusterCandidates.size());
    }

    // TODO Check all attributes previously deemed uniform (section 3.5).

    if(stepProgress != null) {
      stepProgress.beginStep(9, "Generating final result.", LOG);
    }

    // Generate final output.
    Clustering<SubspaceModel> result = new Clustering<>("P3C", "P3C");
    for(int cluster = 0; cluster < clusterCandidates.size(); ++cluster) {
      ClusterCandidate candidate = clusterCandidates.get(cluster);
      CovarianceMatrix cvm = CovarianceMatrix.make(relation, candidate.ids);
      result.addToplevelCluster(new Cluster<>(candidate.ids, new SubspaceModel(new Subspace(candidate.dimensions), cvm.getMeanVector())));
    }
    LOG.verbose("Noise size: " + noise.size());
    if(noise.size() > 0) {
      result.addToplevelCluster(new Cluster<SubspaceModel>(noise, true));
    }

    LOG.ensureCompleted(stepProgress);

    return result;
  }

  /**
   * Construct the 1-signatures by merging adjacent dense bins.
   * 
   * @param partitions Initial partitions.
   * @param markers Markers for dense partitions.
   * @return 1-signatures
   */
  private ArrayList<Signature> constructOneSignatures(SetDBIDs[][] partitions, final long[][] markers) {
    final int dim = partitions.length;
    // Generate projected p-signature intervals.
    ArrayList<Signature> signatures = new ArrayList<>();
    for(int d = 0; d < dim; d++) {
      final DBIDs[] parts = partitions[d];
      if(parts == null) {
        continue; // Never mark any on constant dimensions.
      }
      final long[] marked = markers[d];
      // Find sequences of 1s in marked.
      for(int start = BitsUtil.nextSetBit(marked, 0); start >= 0;) {
        int end = BitsUtil.nextClearBit(marked, start + 1);
        end = (end == -1) ? dim : end;
        int[] signature = new int[dim << 1];
        Arrays.fill(signature, -1);
        signature[d << 1] = start;
        signature[(d << 1) + 1] = end - 1; // inclusive
        HashSetModifiableDBIDs sids = unionDBIDs(parts, start, end /* exclusive */);
        if(LOG.isDebugging()) {
          LOG.debug("1-signature: " + d + " " + start + "-" + (end - 1));
        }
        signatures.add(new Signature(signature, sids));
        start = (end < dim) ? BitsUtil.nextSetBit(marked, end + 1) : -1;
      }
    }
    return signatures;
  }

  /**
   * Merge 1-signatures into p-signatures.
   * 
   * @param binCount Number of bins in each dimension.
   * @param signatures 1-signatures
   * @return p-signatures
   */
  private ArrayList<Signature> mergeClusterCores(final int binCount, ArrayList<Signature> signatures) {
    MutableProgress mergeProgress = LOG.isVerbose() ? new MutableProgress("Merging signatures", signatures.size(), LOG) : null;

    // Annotate dimensions to 1-signatures for quick stopping.
    int[] firstdim = new int[signatures.size()];
    for(int i = 0; i < signatures.size(); i++) {
      firstdim[i] = signatures.get(i).getFirstDim();
    }
    LOG.debug("First dimensions: " + FormatUtil.format(firstdim));

    // Merge to (p+1)-signatures (cluster cores).
    ArrayList<Signature> clusterCores = new ArrayList<>(signatures);
    // Try adding merge 1-signature with each cluster core.
    for(int i = 0; i < clusterCores.size(); i++) {
      final Signature parent = clusterCores.get(i);
      final int end = parent.getFirstDim();
      for(int j = 0; j < signatures.size() && firstdim[j] < end; j++) {
        final Signature onesig = signatures.get(j);
        final Signature merge = mergeSignatures(parent, onesig, binCount);
        if(merge != null) {
          // We add each potential core to the list to allow remaining
          // 1-signatures to try merging with this p-signature as well.
          clusterCores.add(merge);
          // Flag both "parents" for removal.
          parent.prune = true;
          onesig.prune = true;
        }
      }
      if(mergeProgress != null) {
        mergeProgress.setTotal(clusterCores.size());
        mergeProgress.incrementProcessed(LOG);
      }
    }
    if(mergeProgress != null) {
      mergeProgress.setProcessed(mergeProgress.getTotal(), LOG);
    }
    return clusterCores;
  }

  private ArrayList<Signature> pruneRedundantClusterCores(ArrayList<Signature> clusterCores) {
    // Prune cluster cores based on Definition 3, Condition 2.
    ArrayList<Signature> retain = new ArrayList<>(clusterCores.size());
    outer: for(Signature clusterCore : clusterCores) {
      if(clusterCore.prune) {
        continue;
      }
      for(int k = 0; k < clusterCores.size(); k++) {
        Signature other = clusterCores.get(k);
        if(other != clusterCore && other.isSuperset(clusterCore)) {
          continue outer;
        }
      }
      if(LOG.isDebugging()) {
        LOG.debug("Retained cluster core: " + clusterCore);
      }
      retain.add(clusterCore);
    }
    clusterCores = retain;
    return clusterCores;
  }

  /**
   * Partition the data set into {@code bins} bins in each dimension
   * <i>independently</i>.
   * 
   * This can be used to construct a grid approximation of the data using O(d n)
   * memory.
   * 
   * When a dimension is found to be constant, it will not be partitioned, but
   * instead the corresponding array will be set to {@code null}.
   * 
   * @param relation Data relation to partition
   * @param bins Number of bins
   * @return Partitions of each dimension.
   */
  private SetDBIDs[][] partitionData(final Relation<V> relation, final int bins) {
    final int dim = RelationUtil.dimensionality(relation);
    SetDBIDs[][] partitions = new SetDBIDs[dim][bins];
    ArrayModifiableDBIDs ids = DBIDUtil.newArray(relation.getDBIDs());
    DBIDArrayIter iter = ids.iter(); // will be reused.
    SortDBIDsBySingleDimension sorter = new VectorUtil.SortDBIDsBySingleDimension(relation, 0);
    for(int d = 0; d < dim; d++) {
      sorter.setDimension(d);
      ids.sort(sorter);
      // Minimum:
      iter.seek(0);
      double min = relation.get(iter).doubleValue(d);
      // Extend:
      iter.seek(ids.size() - 1);
      double delta = (relation.get(iter).doubleValue(d) - min) / bins;
      if(delta > 0.) {
        SetDBIDs[] dimparts = partitions[d];
        double split = min + delta;
        HashSetModifiableDBIDs pids = DBIDUtil.newHashSet();
        dimparts[0] = pids;
        int i = 0;
        for(iter.seek(0); iter.valid(); iter.advance()) {
          final double v = relation.get(iter).doubleValue(d);
          if(v <= split || i == dimparts.length - 1) {
            pids.add(iter);
          }
          else {
            i++;
            split += delta;
            pids = DBIDUtil.newHashSet();
            dimparts[i] = pids;
          }
        }
        for(++i; i < dimparts.length; ++i) {
          dimparts[i] = pids;
        }
      }
      else {
        partitions[d] = null; // Flag whole dimension as bad
      }
    }
    return partitions;
  }

  /**
   * Compute the union of multiple DBID sets.
   * 
   * @param parts Parts array
   * @param start Array start index
   * @param end Array end index (exclusive)
   * @return Union
   */
  protected HashSetModifiableDBIDs unionDBIDs(final DBIDs[] parts, int start, int end) {
    int sum = 0;
    for(int i = start; i < end; i++) {
      sum += parts[i].size();
    }
    HashSetModifiableDBIDs sids = DBIDUtil.newHashSet(sum);
    for(int i = start; i < end; i++) {
      sids.addDBIDs(parts[i]);
    }
    return sids;
  }

  /**
   * Performs a ChiSquared test to determine whether an attribute has a uniform
   * distribution.
   * 
   * @param parts Data partitions.
   * @param marked the marked bins that should be ignored.
   * @param card Cardinality
   * @return Position of maximum, or -1 when uniform.
   */
  private int chiSquaredUniformTest(SetDBIDs[] parts, long[] marked, int card) {
    // Get global mean over all unmarked bins.
    int max = 0, maxpos = -1;
    MeanVariance mv = new MeanVariance();
    for(int i = 0; i < parts.length; i++) {
      // Ignore already marked bins.
      if(BitsUtil.get(marked, i)) {
        continue;
      }
      final int binSupport = parts[i].size();
      mv.put(binSupport);
      if(binSupport > max) {
        max = binSupport;
        maxpos = i;
      }
    }
    if(mv.getCount() < 1. || !(mv.getNaiveVariance() > 0.)) {
      return -1;
    }
    // ChiSquare statistic is the naive variance of the sizes!
    final double chiSquare = mv.getNaiveVariance() / mv.getMean();
    final int binCount = parts.length - card;
    final double test = ChiSquaredDistribution.cdf(chiSquare, Math.max(1, binCount - card - 1));
    return ((1. - alpha) < test) ? maxpos : -1;
  }

  /**
   * Computes a fuzzy membership with the weights based on which cluster cores
   * each data point is part of.
   * 
   * @param relation Data relation
   * @param clusterCores the cluster cores.
   * @param unassigned set to which to add unassigned points.
   * @param probClusterIGivenX Membership probabilities.
   * @param models Cluster models.
   * @param dim Dimensionality
   */
  private void computeFuzzyMembership(Relation<V> relation, ArrayList<Signature> clusterCores, ModifiableDBIDs unassigned, WritableDataStore<double[]> probClusterIGivenX, List<MultivariateGaussianModel> models, int dim) {
    final int n = relation.size();
    final double pweight = 1. / n; // Weight of each point
    final int k = clusterCores.size();

    double[] clusterWeights = new double[k];
    for(DBIDIter iter = relation.iterDBIDs(); iter.valid(); iter.advance()) {
      int count = 0;
      double[] weights = new double[k];
      for(int cluster = 0; cluster < k; ++cluster) {
        if(clusterCores.get(cluster).ids.contains(iter)) {
          weights[cluster] = 1.;
          ++count;
        }
      }

      // Set value(s) in membership matrix.
      if(count > 0) {
        // Rescale.
        VMath.timesEquals(weights, 1. / count);
        VMath.plusTimesEquals(clusterWeights, weights, pweight);
      }
      else {
        // Does not match any cluster, mark it.
        unassigned.add(iter);
      }
      probClusterIGivenX.put(iter, weights);
    }
    for(int i = 0; i < k; i++) {
      models.add(new MultivariateGaussianModel(clusterWeights[i], new double[dim]));
    }
  }

  /**
   * Assign unassigned objects to best candidate based on shortest Mahalanobis
   * distance.
   * 
   * @param relation Data relation
   * @param probClusterIGivenX fuzzy membership matrix.
   * @param models Cluster models.
   * @param unassigned the list of points not yet assigned.
   */
  private void assignUnassigned(Relation<V> relation, WritableDataStore<double[]> probClusterIGivenX, List<MultivariateGaussianModel> models, ModifiableDBIDs unassigned) {
    if(unassigned.size() == 0) {
      return;
    }
    final int k = models.size();
    double pweight = 1. / relation.size();

    // Rescale weights, to take unassigned points into account:
    for(EMClusterModel<?> m : models) {
      m.setWeight(m.getWeight() * (relation.size() - unassigned.size()) * pweight);
    }

    // Assign noise objects, increase weights accordingly.
    for(DBIDIter iter = unassigned.iter(); iter.valid(); iter.advance()) {
      // Find the best matching known cluster core using the Mahalanobis
      // distance.
      V v = relation.get(iter);
      int bestCluster = -1;
      MultivariateGaussianModel bestModel = null;
      double minDistance = Double.POSITIVE_INFINITY;
      int c = 0;
      for(MultivariateGaussianModel model : models) {
        final double distance = model.mahalanobisDistance(v);
        if(distance < minDistance) {
          minDistance = distance;
          bestCluster = c;
          bestModel = model;
        }
        c++;
      }
      // Assign to best core.
      double[] weights = new double[k];
      weights[bestCluster] = 1.;

      if(bestModel == null) {
        throw new IllegalStateException("No models?");
      }
      bestModel.setWeight(bestModel.getWeight() + pweight);
      probClusterIGivenX.put(iter, weights);
    }

    // Clear the list of unassigned objects.
    unassigned.clear();
  }

  /**
   * Creates a hard clustering from the specified soft membership matrix.
   * 
   * @param probClusterIGivenX the membership matrix.
   * @param dbids mapping matrix row to DBID.
   * @return a hard clustering based on the matrix.
   */
  private ArrayList<ClusterCandidate> hardClustering(WritableDataStore<double[]> probClusterIGivenX, List<Signature> clusterCores, DBIDs dbids) {
    final int k = clusterCores.size();

    // Initialize cluster sets.
    ArrayList<ClusterCandidate> candidates = new ArrayList<>();
    for(Signature sig : clusterCores) {
      candidates.add(new ClusterCandidate(sig));
    }

    // Perform hard partitioning, assigning each data point only to one cluster,
    // namely that one it is most likely to belong to.
    for(DBIDIter iter = dbids.iter(); iter.valid(); iter.advance()) {
      final double[] probs = probClusterIGivenX.get(iter);
      int bestCluster = 0;
      double bestProbability = probs[0];
      for(int c = 1; c < k; ++c) {
        if(probs[c] > bestProbability) {
          bestCluster = c;
          bestProbability = probs[c];
        }
      }
      candidates.get(bestCluster).ids.add(iter);
    }

    return candidates;
  }

  /**
   * Performs outlier detection by testing the Mahalanobis distance of each
   * point in a cluster against the critical value of the ChiSquared
   * distribution with as many degrees of freedom as the cluster has relevant
   * attributes.
   * 
   * @param relation Data relation
   * @param models Cluster models
   * @param clusterCandidates the list of clusters to check.
   * @param noise the set to which to add points deemed outliers.
   */
  private void findOutliers(Relation<V> relation, List<MultivariateGaussianModel> models, ArrayList<ClusterCandidate> clusterCandidates, ModifiableDBIDs noise) {
    Iterator<MultivariateGaussianModel> it = models.iterator();
    for(int c = 0; it.hasNext(); c++) {
      MultivariateGaussianModel model = it.next();
      final ClusterCandidate candidate = clusterCandidates.get(c);
      final int dof = BitsUtil.cardinality(candidate.dimensions);
      final double threshold = ChiSquaredDistribution.quantile(1 - alpha, dof);
      for(DBIDMIter iter = candidate.ids.iter(); iter.valid(); iter.advance()) {
        final double distance = model.mahalanobisDistance(relation.get(iter));
        if(distance >= threshold) {
          // Outlier, remove it and add it to the outlier set.
          noise.add(iter);
          iter.remove();
        }
      }
    }
  }

  /**
   * Generates a merged signature of this and another one, where the other
   * signature must be a 1-signature.
   * 
   * @param first First signature.
   * @param second Second signature, must be a 1-signature.
   * @param numBins Number of bins per dimension.
   * @return the merged signature, or null if the merge failed.
   */
  protected Signature mergeSignatures(Signature first, Signature second, int numBins) {
    int d2 = -1;
    for(int i = 0; i < second.spec.length; i += 2) {
      if(second.spec[i] >= 0) {
        assert (d2 == -1) : "Merging with non-1-signature?!?";
        d2 = i;
      }
    }
    assert (d2 >= 0) : "Merging with empty signature?";

    // Avoid generating redundant signatures.
    if(first.spec[d2] >= 0) {
      return null;
    }

    // Definition 3, Condition 1:
    // True support:
    final ModifiableDBIDs intersection = DBIDUtil.intersection(first.ids, second.ids);
    final int support = intersection.size();
    // Interval width, computed using selected number of bins / total bins
    double width = (second.spec[d2 + 1] - second.spec[d2] + 1.) / (double) numBins;
    // Expected size thus:
    double expect = first.ids.size() * width;
    if(support <= expect || support < minClusterSize) {
      return null;
    }
    final double test = PoissonDistribution.rawProbability(support, expect);
    if(poissonThreshold <= test) {
      return null;
    }
    // Create merged signature.
    int[] spec = first.spec.clone();
    spec[d2] = second.spec[d2];
    spec[d2 + 1] = second.spec[d2];

    final Signature newsig = new Signature(spec, intersection);
    if(LOG.isDebugging()) {
      LOG.debug(newsig.toString());
    }
    return newsig;
  }

  /**
   * P3C Cluster signature.
   * 
   * @author Erich Schubert
   */
  private static class Signature {
    /**
     * Subspace specification
     */
    int[] spec;

    /**
     * Object ids.
     */
    DBIDs ids;

    /**
     * Pruning flag.
     */
    boolean prune = false;

    /**
     * Constructor.
     * 
     * @param spec Subspace specification
     * @param ids IDs.
     */
    private Signature(int[] spec, DBIDs ids) {
      super();
      this.spec = spec;
      this.ids = ids;
    }

    /**
     * Test whether this is a superset of the other signature.
     * 
     * @param other Other signature.
     * @return {@code true} when this is a superset.
     */
    public boolean isSuperset(Signature other) {
      for(int i = 1; i < spec.length; i += 2) {
        if((spec[i - 1] != other.spec[i - 1] || spec[i] != other.spec[i - 1]) && other.spec[i - 1] != -1) {
          return false;
        }
      }
      return true;
    }

    /**
     * Find the first dimension set in this signature.
     * 
     * @return Dimension
     */
    public int getFirstDim() {
      for(int i = 0; i < spec.length; i += 2) {
        if(spec[i] >= 0) {
          return (i >>> 1);
        }
      }
      return -1;
    }

    @Override
    public String toString() {
      int p = 0;
      for(int i = 0; i < spec.length; i += 2) {
        if(spec[i] >= 0) {
          p++;
        }
      }
      StringBuilder buf = new StringBuilder(1000) //
          .append(p).append("-signature: ");
      for(int i = 1; i < spec.length; i += 2) {
        if(spec[i - 1] >= 0) {
          buf.append(i >>> 1).append(':').append(spec[i - 1]).append('-').append(spec[i]).append(' ');
        }
      }
      return buf.append(" size: ").append(ids.size()).toString();
    }
  }

  /**
   * This class is used to represent potential clusters.
   * 
   * @author Erich Schubert
   */
  private static class ClusterCandidate {
    /**
     * Selected dimensions
     */
    public final long[] dimensions;

    /**
     * Objects contained in cluster.
     */
    public final ModifiableDBIDs ids;

    /**
     * Constructor.
     * 
     * @param clusterCore Signature
     */
    public ClusterCandidate(Signature clusterCore) {
      this.dimensions = BitsUtil.zero(clusterCore.spec.length >> 1);
      for(int i = 0; i < clusterCore.spec.length; i += 2) {
        BitsUtil.setI(this.dimensions, i >> 1);
      }
      this.ids = DBIDUtil.newArray(clusterCore.ids.size());
    }
  }

  @Override
  public TypeInformation[] getInputTypeRestriction() {
    return TypeUtil.array(TypeUtil.NUMBER_VECTOR_FIELD);
  }

  @Override
  protected Logging getLogger() {
    return LOG;
  }

  /**
   * Parameterization class.
   * 
   * @author Florian Nuecke
   */
  public static class Parameterizer<V extends NumberVector> extends AbstractParameterizer {
    /**
     * Parameter for the chi squared test threshold.
     */
    public static final OptionID ALPHA_THRESHOLD_ID = new OptionID("p3c.alpha", "The significance level for uniform testing in the initial binning step.");

    /**
     * Parameter for the poisson test threshold.
     */
    public static final OptionID POISSON_THRESHOLD_ID = new OptionID("p3c.threshold", "The threshold value for the poisson test used when merging signatures.");

    /**
     * Maximum number of iterations for the EM step.
     */
    public static final OptionID MAX_EM_ITERATIONS_ID = new OptionID("p3c.em.maxiter", "The maximum number of iterations for the EM step. Use -1 to run until delta convergence.");

    /**
     * Threshold when to stop EM iterations.
     */
    public static final OptionID EM_DELTA_ID = new OptionID("p3c.em.delta", "The change delta for the EM step below which to stop.");

    /**
     * Minimum cluster size for noise flagging. (Not existant in the original
     * publication).
     */
    public static final OptionID MIN_CLUSTER_SIZE_ID = new OptionID("p3c.minsize", "The minimum size of a cluster, otherwise it is seen as noise (this is a cheat, it is not mentioned in the paper).");

    /**
     * Parameter for the chi squared test threshold.
     * 
     * While statistical values such as 0.01 are a good choice, we found the
     * need to modify this parameter in our experiments.
     */
    protected double alpha;

    /**
     * Parameter for the poisson test threshold.
     */
    protected double poissonThreshold;

    /**
     * Maximum number of iterations for the EM step.
     */
    protected int maxEmIterations;

    /**
     * Threshold when to stop EM iterations.
     */
    protected double emDelta;

    /**
     * Minimum cluster size for noise flagging. (Not existant in the original
     * publication).
     */
    protected int minClusterSize;

    @Override
    protected void makeOptions(Parameterization config) {
      super.makeOptions(config);

      {
        DoubleParameter param = new DoubleParameter(ALPHA_THRESHOLD_ID, .001) //
            .addConstraint(CommonConstraints.GREATER_THAN_ZERO_DOUBLE) //
            .addConstraint(CommonConstraints.LESS_THAN_HALF_DOUBLE);
        if(config.grab(param)) {
          alpha = param.getValue();
        }
      }

      {
        DoubleParameter param = new DoubleParameter(POISSON_THRESHOLD_ID, 1.e-4) //
            .addConstraint(CommonConstraints.GREATER_THAN_ZERO_DOUBLE) //
            .addConstraint(CommonConstraints.LESS_THAN_HALF_DOUBLE);
        if(config.grab(param)) {
          poissonThreshold = param.getValue();
        }
      }

      {
        IntParameter param = new IntParameter(MAX_EM_ITERATIONS_ID, 20) //
            .addConstraint(CommonConstraints.GREATER_EQUAL_MINUSONE_INT);
        if(config.grab(param)) {
          maxEmIterations = param.getValue();
        }
      }

      {
        DoubleParameter param = new DoubleParameter(EM_DELTA_ID, 1.e-5) //
            .addConstraint(CommonConstraints.GREATER_THAN_ZERO_DOUBLE);
        if(config.grab(param)) {
          emDelta = param.getValue();
        }
      }

      {
        IntParameter param = new IntParameter(MIN_CLUSTER_SIZE_ID, 1) //
            .addConstraint(CommonConstraints.GREATER_EQUAL_ONE_INT);
        if(config.grab(param)) {
          minClusterSize = param.getValue();
        }
      }
    }

    @Override
    protected P3C<V> makeInstance() {
      return new P3C<>(alpha, poissonThreshold, maxEmIterations, emDelta, minClusterSize);
    }
  }
}
