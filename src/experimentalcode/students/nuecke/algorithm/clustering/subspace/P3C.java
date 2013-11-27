package experimentalcode.students.nuecke.algorithm.clustering.subspace;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.BitSet;
import java.util.Iterator;
import java.util.List;

import de.lmu.ifi.dbs.elki.algorithm.AbstractAlgorithm;
import de.lmu.ifi.dbs.elki.algorithm.clustering.EM;
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
import de.lmu.ifi.dbs.elki.database.ids.ArrayDBIDs;
import de.lmu.ifi.dbs.elki.database.ids.ArrayModifiableDBIDs;
import de.lmu.ifi.dbs.elki.database.ids.DBIDArrayIter;
import de.lmu.ifi.dbs.elki.database.ids.DBIDIter;
import de.lmu.ifi.dbs.elki.database.ids.DBIDMIter;
import de.lmu.ifi.dbs.elki.database.ids.DBIDUtil;
import de.lmu.ifi.dbs.elki.database.ids.DBIDs;
import de.lmu.ifi.dbs.elki.database.ids.HashSetModifiableDBIDs;
import de.lmu.ifi.dbs.elki.database.ids.ModifiableDBIDs;
import de.lmu.ifi.dbs.elki.database.relation.Relation;
import de.lmu.ifi.dbs.elki.database.relation.RelationUtil;
import de.lmu.ifi.dbs.elki.logging.Logging;
import de.lmu.ifi.dbs.elki.logging.progress.FiniteProgress;
import de.lmu.ifi.dbs.elki.logging.progress.StepProgress;
import de.lmu.ifi.dbs.elki.math.MathUtil;
import de.lmu.ifi.dbs.elki.math.MeanVariance;
import de.lmu.ifi.dbs.elki.math.linearalgebra.CovarianceMatrix;
import de.lmu.ifi.dbs.elki.math.linearalgebra.Matrix;
import de.lmu.ifi.dbs.elki.math.linearalgebra.VMath;
import de.lmu.ifi.dbs.elki.math.linearalgebra.Vector;
import de.lmu.ifi.dbs.elki.math.statistics.distribution.ChiSquaredDistribution;
import de.lmu.ifi.dbs.elki.math.statistics.distribution.PoissonDistribution;
import de.lmu.ifi.dbs.elki.utilities.BitsUtil;
import de.lmu.ifi.dbs.elki.utilities.documentation.Reference;
import de.lmu.ifi.dbs.elki.utilities.documentation.Title;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.AbstractParameterizer;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.OptionID;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.constraints.GreaterConstraint;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.Parameterization;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.DoubleParameter;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.IntParameter;

/**
 * P3C: A Robust Projected Clustering Algorithm.
 * 
 * <p>
 * Reference: <br/>
 * Gabriela Moise, Jörg Sander, Martin Ester<br />
 * P3C: A Robust Projected Clustering Algorithm.<br/>
 * In: Proc. Sixth International Conference on Data Mining (ICDM '06)
 * </p>
 * 
 * @author Florian Nuecke
 * @author Erich Schubert
 * 
 * @apiviz.has SubspaceModel
 * 
 * @param <V> the type of NumberVector handled by this Algorithm.
 */
@Title("P3C: A Robust Projected Clustering Algorithm.")
@Reference(authors = "Gabriela Moise, Jörg Sander, Martin Ester", title = "P3C: A Robust Projected Clustering Algorithm", booktitle = "Proc. Sixth International Conference on Data Mining (ICDM '06)", url = "http://dx.doi.org/10.1109/ICDM.2006.123")
public class P3C<V extends NumberVector<?>> extends AbstractAlgorithm<Clustering<SubspaceModel<V>>> {
  /**
   * The logger for this class.
   */
  private static final Logging LOG = Logging.getLogger(P3C.class);

  /**
   * Minimum Log Likelihood.
   */
  private static final double MIN_LOGLIKELIHOOD = -100000;

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
   * Maximum number of EM iterations.
   */
  protected int maxiter = 100;

  /**
   * Sets up a new instance of the algorithm's environment.
   */
  public P3C(double poissonThreshold, int maxEmIterations, double emDelta, int minClusterSize) {
    super();
    this.poissonThreshold = poissonThreshold;
    this.maxEmIterations = maxEmIterations;
    this.emDelta = emDelta;
    this.minClusterSize = minClusterSize;
  }

  /**
   * Performs the P3C algorithm on the given Database.
   */
  public Clustering<SubspaceModel<V>> run(Database database, Relation<V> relation) {
    final int dim = RelationUtil.dimensionality(relation);

    // Overall progress.
    StepProgress stepProgress = LOG.isVerbose() ? new StepProgress(8) : null;

    if (stepProgress != null) {
      stepProgress.beginStep(1, "Grid-partitioning data.", LOG);
    }

    // Desired number of bins, as per Sturge:
    final int binCount = (int) Math.ceil(1 + (Math.log(relation.size()) / MathUtil.LOG2));

    // Perform 1-dimensional projections, and split into bins.
    DBIDs[][] partitions = new DBIDs[dim][binCount];
    ArrayModifiableDBIDs ids = DBIDUtil.newArray(relation.getDBIDs());
    DBIDArrayIter iter = ids.iter();
    SortDBIDsBySingleDimension sorter = new VectorUtil.SortDBIDsBySingleDimension(relation, 0);
    for (int d = 0; d < dim; d++) {
      sorter.setDimension(d);
      ids.sort(sorter);
      // Minimum:
      iter.seek(0);
      double min = relation.get(iter).doubleValue(d);
      // Extend:
      iter.seek(ids.size() - 1);
      double delta = (relation.get(iter).doubleValue(d) - min) / binCount;
      if (delta > 0.) {
        partition(relation, d, min, delta, ids, iter, 0, ids.size(), 0, binCount, partitions[d]);
        if (LOG.isDebugging()) {
          StringBuilder buf = new StringBuilder();
          buf.append("Partition sizes of dim ").append(d).append(": ");
          int sum = 0;
          for (DBIDs p : partitions[d]) {
            buf.append(p.size()).append(' ');
            sum += p.size();
          }
          LOG.debug(buf.toString());
          assert (sum == ids.size());
        }
      } else {
        partitions[d] = null; // Flag whole dimension as bad
      }
    }

    if (stepProgress != null) {
      stepProgress.beginStep(2, "Searching for non-uniform bins in support histograms.", LOG);
    }

    // Set markers for each attribute until they're all deemed uniform.
    final long[][] markers = new long[dim][];
    int numuniform = 0;
    for (int d = 0; d < dim; d++) {
      final DBIDs[] parts = partitions[d];
      if (parts == null) {
        continue; // Never mark any on constant dimensions.
      }
      final long[] marked = markers[d] = BitsUtil.zero(binCount);
      int card = 0;
      while (card < dim - 1) {
        // Find bin with largest support, test only the dimensions that were not
        // previously marked.
        int bestBin = chiSquaredUniformTest(parts, marked, card);
        if (bestBin < 0) {
          numuniform++;
          break; // Uniform
        }
        BitsUtil.setI(marked, bestBin);
        card++;
      }
      if (LOG.isDebugging()) {
        LOG.debug("Marked bins in dim " + d + ": " + BitsUtil.toString(marked, dim));
      }
    }

    if (stepProgress != null) {
      stepProgress.beginStep(3, "Merging marked bins to 1-signatures.", LOG);
    }

    // Generate projected p-signature intervals.
    ArrayList<Signature> signatures = new ArrayList<>();
    for (int d = 0; d < dim; d++) {
      final DBIDs[] parts = partitions[d];
      if (parts == null) {
        continue; // Never mark any on constant dimensions.
      }
      final long[] marked = markers[d];
      // Find sequences of 1s in marked.
      for (int start = BitsUtil.nextSetBit(marked, 0); start >= 0;) {
        int end = BitsUtil.nextClearBit(marked, start + 1) - 1;
        if (end == -1) {
          end = dim;
        }
        int[] signature = new int[dim << 1];
        Arrays.fill(signature, -1);
        signature[d << 1] = start;
        signature[(d << 1) + 1] = end - 1; // inclusive
        HashSetModifiableDBIDs sids = unionDBIDs(parts, start, end /* exclusive */);
        if (LOG.isDebugging()) {
          LOG.debug("1-signature: " + d + " " + start + "-" + end);
        }
        signatures.add(new Signature(signature, sids));
        start = (end < dim) ? BitsUtil.nextSetBit(marked, end + 1) : -1;
      }
    }

    if (stepProgress != null) {
      stepProgress.beginStep(4, "Computing cluster cores from merged p-signatures.", LOG);
    }

    FiniteProgress mergeProgress = LOG.isVerbose() ? new FiniteProgress("1-signatures merges", signatures.size(), LOG) : null;

    // Merge to (p+1)-signatures (cluster cores).
    ArrayList<Signature> clusterCores = new ArrayList<>(signatures);
    // Try adding merge 1-signature with each cluster core.
    for (int i = 0; i < signatures.size(); ++i) {
      final Signature signature = signatures.get(i);
      // Don't merge with future signatures:
      final int end = clusterCores.size();
      // Skip previous 1-signatures: merges are symmetrical. But include newly
      // created cluster cores (i.e. those resulting from previous merges).
      FiniteProgress submergeProgress = LOG.isVerbose() ? new FiniteProgress("p-signatures merges", end - (i + 1), LOG) : null;
      for (int j = i + 1; j < end; ++j) {
        final Signature first = clusterCores.get(j);
        final Signature merge = mergeSignatures(first, signature, binCount);
        if (merge != null) {
          // We add each potential core to the list to allow remaining
          // 1-signatures to try merging with this p-signature as well.
          clusterCores.add(merge);
          // Flag for removal.
          first.prune = true;
          signature.prune = true;
        }
        if (submergeProgress != null) {
          submergeProgress.incrementProcessed(LOG);
        }
      }
      if (submergeProgress != null) {
        submergeProgress.ensureCompleted(LOG);
      }
      if (mergeProgress != null) {
        mergeProgress.incrementProcessed(LOG);
      }
    }
    if (mergeProgress != null) {
      mergeProgress.ensureCompleted(LOG);
    }

    if (stepProgress != null) {
      stepProgress.beginStep(5, "Pruning incomplete cluster cores.", LOG);
    }

    // Prune cluster cores based on Definition 3, Condition 2.
    ArrayList<Signature> retain = new ArrayList<>(clusterCores.size());
    for (Signature clusterCore : clusterCores) {
      if (!clusterCore.prune) {
        retain.add(clusterCore);
      }
    }
    clusterCores = retain;
    if (LOG.isVerbose()) {
      LOG.verbose("Number of cluster cores found: " + clusterCores.size());
    }

    if (clusterCores.size() == 0) {
      stepProgress.setCompleted(LOG);
      // FIXME: return trivial noise clustering.
      return null;
    }

    if (stepProgress != null) {
      stepProgress.beginStep(5, "Refining cluster cores to clusters via EM.", LOG);
    }

    // Track objects not assigned to any cluster:
    ModifiableDBIDs noise = DBIDUtil.newHashSet();
    WritableDataStore<double[]> probClusterIGivenX = DataStoreUtil.makeStorage(relation.getDBIDs(), DataStoreFactory.HINT_HOT | DataStoreFactory.HINT_SORTED, double[].class);
    int k = clusterCores.size();
    double[] clusterWeights = new double[k];
    computeFuzzyMembership(relation, clusterCores, noise, probClusterIGivenX, clusterWeights);

    // Initial estimate of covariances, to assign noise objects
    Vector[] means = new Vector[k];
    Matrix[] covarianceMatrices = new Matrix[k], invCovMatr = new Matrix[k];
    final double norm = MathUtil.powi(MathUtil.TWOPI, dim);
    double[] normDistrFactor = new double[k];
    Arrays.fill(normDistrFactor, 1. / Math.sqrt(norm));
    EM.recomputeCovarianceMatrices(relation, probClusterIGivenX, means, covarianceMatrices, dim);
    EM.computeInverseMatrixes(covarianceMatrices, invCovMatr, normDistrFactor, norm);
    assignUnassigned(relation, probClusterIGivenX, means, invCovMatr, clusterWeights, noise);

    double emNew = EM.assignProbabilitiesToInstances(relation, normDistrFactor, means, invCovMatr, clusterWeights, probClusterIGivenX);
    for (int it = 1; it <= maxiter || maxiter < 0; it++) {
      final double emOld = emNew;
      EM.recomputeCovarianceMatrices(relation, probClusterIGivenX, means, covarianceMatrices, dim);
      EM.computeInverseMatrixes(covarianceMatrices, invCovMatr, normDistrFactor, norm);
      // reassign probabilities
      emNew = EM.assignProbabilitiesToInstances(relation, normDistrFactor, means, invCovMatr, clusterWeights, probClusterIGivenX);

      if (LOG.isVerbose()) {
        LOG.verbose("iteration " + it + " - expectation value: " + emNew);
      }
      if (Math.abs(emOld - emNew) <= emDelta) {
        break;
      }
    }

    // Perform EM clustering.

    if (stepProgress != null) {
      stepProgress.beginStep(6, "Generating hard clustering.", LOG);
    }

    // Create a hard clustering, making sure each data point only is part of one
    // cluster, based on the best match from the membership matrix.
    ArrayList<ClusterCandidate> clusterCandidates = hardClustering(probClusterIGivenX, clusterCores, ids);

    if (stepProgress != null) {
      stepProgress.beginStep(7, "Looking for outliers and moving them to the noise set.", LOG);
    }

    // Outlier detection. Remove points from clusters that have a Mahalanobis
    // distance larger than the critical value of the ChiSquare distribution.
    findOutliers(relation, means, invCovMatr, clusterCandidates, dim - numuniform, noise);

    if (stepProgress != null) {
      stepProgress.beginStep(8, "Removing empty clusters.", LOG);
    }

    // Remove near-empty clusters.
    for (Iterator<ClusterCandidate> it = clusterCandidates.iterator(); it.hasNext();) {
      ClusterCandidate cand = it.next();
      final int size = cand.ids.size();
      if (size < minClusterSize) {
        noise.addDBIDs(cand.ids);
        it.remove();
      }
    }

    // Relevant attribute computation.
    for (ClusterCandidate candidate : clusterCandidates) {
      // TODO Check all attributes previously deemed uniform (section 3.5).
    }

    if (stepProgress != null) {
      stepProgress.beginStep(9, "Generating final result.", LOG);
    }

    // Generate final output.
    Clustering<SubspaceModel<V>> result = new Clustering<>("P3C", "P3C");
    if (noise.size() > 0) {
      result.addToplevelCluster(new Cluster<SubspaceModel<V>>(noise, true));
    }
    for (int cluster = 0; cluster < clusterCandidates.size(); ++cluster) {
      ClusterCandidate candidate = clusterCandidates.get(cluster);
      CovarianceMatrix cvm = CovarianceMatrix.make(relation, candidate.ids);
      result.addToplevelCluster(new Cluster<>(candidate.ids, new SubspaceModel<>(new Subspace(candidate.dimensions), cvm.getMeanVector(relation))));
    }

    if (stepProgress != null) {
      stepProgress.ensureCompleted(LOG);
    }

    return result;
  }

  /**
   * Compute the union of multiple DBID sets.
   * 
   * @param parts Parts array
   * @param start Array start index
   * @param end Array end index (exclusive)
   * @return
   */
  protected HashSetModifiableDBIDs unionDBIDs(final DBIDs[] parts, int start, int end) {
    int sum = 0;
    for (int i = start; i < end; i++) {
      sum += parts[i].size();
    }
    HashSetModifiableDBIDs sids = DBIDUtil.newHashSet(sum);
    for (int i = start; i < end; i++) {
      sids.addDBIDs(parts[i]);
    }
    return sids;
  }

  /**
   * Partition the data set by repeated binary splitting.
   * 
   * @param relation Relation
   * @param d Dimension
   * @param min Minimum
   * @param delta Step size
   * @param ids ID array
   * @param iter ID iterator
   * @param start ID interval start
   * @param end ID interval end
   * @param ps Partition interval start
   * @param pe Partition interval end
   * @param partitions
   */
  private void partition(Relation<V> relation, int d, double min, double delta, ArrayDBIDs ids, DBIDArrayIter iter, int start, int end, int ps, int pe, DBIDs[] partitions) {
    final int ph = (ps + pe) >>> 1;
    final double split = min + ph * delta;
    // Perform binary search
    int ss = start, se = end - 1;
    while (ss < se) {
      final int sh = (ss + se) >>> 1;
      iter.seek(sh);
      // LOG.debugFinest("sh: " + sh);
      final double v = relation.get(iter).doubleValue(d);
      if (split < v) {
        if (ss < sh - 1) {
          se = sh - 1;
        } else {
          se = sh;
          break;
        }
      } else {
        if (sh < se) {
          ss = sh + 1;
        } else {
          ss = sh;
          break;
        }
      }
    }
    // LOG.debugFinest("ss: " + ss + " se: " + se + " start: " + start +
    // " end: " +
    // end);
    // start to ss (inclusive) are left,
    // ss + 1 to end (exclusive) are right.
    if (ps == ph - 1) {
      assert (partitions[ph] == null);
      ModifiableDBIDs pids = DBIDUtil.newHashSet(ss + 1 - start);
      iter.seek(start);
      for (int i = start; i <= ss; i++, iter.advance()) {
        pids.add(iter);
      }
      partitions[ps] = pids;
    } else {
      partition(relation, d, min, delta, ids, iter, start, ss + 1, ps, ph, partitions);
    }
    if (ph == pe - 1) {
      assert (partitions[ph] == null);
      ModifiableDBIDs pids = DBIDUtil.newHashSet(end - (ss + 1));
      iter.seek(start);
      for (int i = ss + 1; i < end; i++, iter.advance()) {
        pids.add(iter);
      }
      partitions[ph] = pids;
    } else {
      partition(relation, d, min, delta, ids, iter, ss + 1, end, ph, pe, partitions);
    }
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
  private int chiSquaredUniformTest(DBIDs[] parts, long[] marked, int card) {
    // Remaining number of bins.
    final int binCount = parts.length - card;
    // Get global mean over all unmarked bins.
    int max = 0, maxpos = -1;
    MeanVariance mv = new MeanVariance();
    for (int i = 0; i < parts.length; i++) {
      // Ignore already marked bins.
      if (BitsUtil.get(marked, i)) {
        continue;
      }
      final int binSupport = parts[i].size();
      mv.put(binSupport);
      if (binSupport > max) {
        max = binSupport;
        maxpos = i;
      }
    }
    if (mv.getCount() < 1.) {
      return -1;
    }
    // ChiSquare statistic is the naive variance of the sizes!
    double chiSquare = mv.getNaiveVariance();

    if ((1 - 0.001) < ChiSquaredDistribution.cdf(chiSquare, Math.max(1, binCount - card - 1))) {
      return maxpos;
    }
    return -1;
  }

  /**
   * Computes a fuzzy membership with the weights based on which cluster cores
   * each data point is part of.
   * 
   * @param relation Data relation
   * @param clusterCores the cluster cores.
   * @param unassigned set to which to add unassigned points.
   * @param probClusterIGivenX Membership probabilities.
   * @param clusterWeights Cluster weights
   */
  private void computeFuzzyMembership(Relation<V> relation, ArrayList<Signature> clusterCores, ModifiableDBIDs unassigned, WritableDataStore<double[]> probClusterIGivenX, double[] clusterWeights) {
    final int n = relation.size();
    final int k = clusterCores.size();

    for (DBIDIter iter = relation.iterDBIDs(); iter.valid(); iter.advance()) {
      int count = 0;
      double[] weights = new double[k];
      for (int cluster = 0; cluster < k; ++cluster) {
        if (clusterCores.get(cluster).ids.contains(iter)) {
          weights[cluster] = 1.;
          ++count;
        }
      }

      // Set value(s) in membership matrix.
      if (count > 0) {
        // Rescale.
        VMath.timesEquals(weights, 1. / count);
        VMath.plusTimesEquals(clusterWeights, weights, 1. / n);
      } else {
        // Does not match any cluster, mark it.
        unassigned.add(iter);
      }
      probClusterIGivenX.put(iter, weights);
    }
  }

  /**
   * Assign unassigned objects to best candidate based on shortest Mahalanobis
   * distance.
   * 
   * @param relation Data relation
   * @param probClusterIGivenX fuzzy membership matrix.
   * @param means Cluster means.
   * @param invCovMatr Cluster covariance matrices.
   * @param clusterWeights
   * @param assigned mapping of matrix row to DBID.
   * @param unassigned the list of points not yet assigned.
   */
  private void assignUnassigned(Relation<V> relation, WritableDataStore<double[]> probClusterIGivenX, Vector[] means, Matrix[] invCovMatr, double[] clusterWeights, ModifiableDBIDs unassigned) {
    if (unassigned.size() == 0) {
      return;
    }
    final int k = means.length;
    double pweight = 1. / relation.size();

    for (DBIDIter iter = unassigned.iter(); iter.valid(); iter.advance()) {
      // Find the best matching known cluster core using the Mahalanobis
      // distance.
      Vector v = relation.get(iter).getColumnVector();
      int bestCluster = -1;
      double minDistance = Double.POSITIVE_INFINITY;
      for (int c = 0; c < k; ++c) {
        final double distance = MathUtil.mahalanobisDistance(invCovMatr[c], v.minus(means[c]));
        if (distance < minDistance) {
          minDistance = distance;
          bestCluster = c;
        }
      }
      // Assign to best core.
      double[] weights = new double[k];
      weights[bestCluster] = 1.0;
      clusterWeights[bestCluster] += pweight;
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
  private ArrayList<ClusterCandidate> hardClustering(WritableDataStore<double[]> probClusterIGivenX, List<Signature> clusterCores, ArrayModifiableDBIDs dbids) {
    final int k = clusterCores.size();

    // Initialize cluster sets.
    ArrayList<ClusterCandidate> candidates = new ArrayList<>();
    for (Signature sig : clusterCores) {
      candidates.add(new ClusterCandidate(sig));
    }

    // Perform hard partitioning, assigning each data point only to one cluster,
    // namely that one it is most likely to belong to.
    for (DBIDIter iter = dbids.iter(); iter.valid(); iter.advance()) {
      final double[] probs = probClusterIGivenX.get(iter);
      int bestCluster = 0;
      double bestProbability = probs[0];
      for (int c = 1; c < k; ++c) {
        if (probs[c] > bestProbability) {
          bestCluster = c;
          bestProbability = probs[c];
        }
      }
      candidates.get(bestCluster).ids.add(iter);
    }

    return candidates;
  }

  /**
   * Performs outlier detection by testing the mahalanobis distance of each
   * point in a cluster against the critical value of the ChiSquared
   * distribution with as many degrees of freedom as the cluster has relevant
   * attributes.
   * 
   * @param relation Data relation
   * @param means Cluster means
   * @param invCovMatr Inverse covariance matrixes
   * @param clusterCandidates the list of clusters to check.
   * @param nonUniformDimensionCount the number of dimensions to consider when
   *        testing.
   * @param noise the set to which to add points deemed outliers.
   */
  private void findOutliers(Relation<V> relation, Vector[] means, Matrix[] invCovMatr, ArrayList<ClusterCandidate> clusterCandidates, int nonUniformDimensionCount, ModifiableDBIDs noise) {
    final int k = clusterCandidates.size();

    for (int c = 0; c < k; ++c) {
      final ClusterCandidate candidate = clusterCandidates.get(c);
      if (candidate.ids.size() < 2) {
        continue;
      }
      for (DBIDMIter iter = candidate.ids.iter(); iter.valid(); iter.advance()) {
        final Vector mean = means[c];
        final Vector delta = relation.get(iter).getColumnVector().minusEquals(mean);
        final Matrix invCov = invCovMatr[c];
        final double distance = MathUtil.mahalanobisDistance(invCov, delta);
        final int dof = candidate.dimensions.cardinality() - 1;
        if ((1 - 0.001) <= ChiSquaredDistribution.cdf(distance, dof)) {
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
    for (int i = 0; i < second.spec.length; i += 2) {
      if (second.spec[i] >= 0) {
        assert (d2 == -1) : "Merging with non-1-signature?!?";
        d2 = i;
      }
    }
    assert (d2 >= 0) : "Merging with empty signature?";

    // Skip the merge if the interval is already part of the signature.
    if (first.spec[d2] >= 0) {
      return null;
    }

    // Definition 3, Condition 1:
    // True support:
    final ModifiableDBIDs intersection = DBIDUtil.intersection(first.ids, second.ids);
    final int support = intersection.size();
    // Interval width, computed using selected number of bins / total bins
    double width = (second.spec[d2 + 1] + 1 - second.spec[d2]) / (double) numBins;
    // Expected size thus:
    double expect = support * width;
    if (support <= expect) {
      return null;
    }
    if (PoissonDistribution.rawProbability(support, expect) >= poissonThreshold) {
      return null;
    }
    // Create merged signature.
    int[] spec = first.spec.clone();
    spec[d2] = second.spec[d2];
    spec[d2 + 1] = second.spec[d2];
    return new Signature(spec, intersection);
  }

  private static class Signature implements Cloneable {
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

    @Override
    protected Signature clone() throws CloneNotSupportedException {
      Signature c = (Signature) super.clone();
      c.spec = this.spec.clone();
      c.ids = null;
      return c;
    }
  }

  /**
   * This class is used to represent potential clusters.
   * 
   * TODO: Documentation.
   */
  private static class ClusterCandidate {
    public final BitSet dimensions;

    public final ModifiableDBIDs ids;

    public ClusterCandidate(Signature clusterCore) {
      this.dimensions = new BitSet(clusterCore.spec.length >> 1);
      for (int i = 0; i < clusterCore.spec.length; i += 2) {
        this.dimensions.set(i >> 1);
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
   * 
   * @apiviz.exclude
   */
  public static class Parameterizer<V extends NumberVector<?>> extends AbstractParameterizer {
    /**
     * Parameter for the poisson test threshold.
     */
    public static final OptionID POISSON_THRESHOLD_ID = new OptionID("p3c.threshold", "The threshold value for the poisson test used when merging signatures.");

    /**
     * Maximum number of iterations for the EM step.
     */
    public static final OptionID MAX_EM_ITERATIONS_ID = new OptionID("p3c.em.maxiter", "The maximum number of iterations for the EM step.");

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
        DoubleParameter param = new DoubleParameter(POISSON_THRESHOLD_ID, 1.e-20);
        param.addConstraint(new GreaterConstraint(0.));
        if (config.grab(param)) {
          poissonThreshold = param.getValue();
        }
      }

      {
        IntParameter param = new IntParameter(MAX_EM_ITERATIONS_ID, 10);
        param.addConstraint(new GreaterConstraint(0));
        if (config.grab(param)) {
          maxEmIterations = param.getValue();
        }
      }

      {
        DoubleParameter param = new DoubleParameter(EM_DELTA_ID, 1.e-9);
        param.addConstraint(new GreaterConstraint(0.));
        if (config.grab(param)) {
          emDelta = param.getValue();
        }
      }

      {
        IntParameter param = new IntParameter(MIN_CLUSTER_SIZE_ID, 1);
        param.addConstraint(new GreaterConstraint(0));
        if (config.grab(param)) {
          minClusterSize = param.getValue();
        }
      }
    }

    @Override
    protected P3C<V> makeInstance() {
      return new P3C<>(poissonThreshold, maxEmIterations, emDelta, minClusterSize);
    }
  }
}
