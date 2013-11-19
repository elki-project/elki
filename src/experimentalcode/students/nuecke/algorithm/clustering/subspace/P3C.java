package experimentalcode.students.nuecke.algorithm.clustering.subspace;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.BitSet;
import java.util.List;

import de.lmu.ifi.dbs.elki.algorithm.AbstractAlgorithm;
import de.lmu.ifi.dbs.elki.data.Cluster;
import de.lmu.ifi.dbs.elki.data.Clustering;
import de.lmu.ifi.dbs.elki.data.Interval;
import de.lmu.ifi.dbs.elki.data.NumberVector;
import de.lmu.ifi.dbs.elki.data.Subspace;
import de.lmu.ifi.dbs.elki.data.VectorUtil;
import de.lmu.ifi.dbs.elki.data.VectorUtil.SortDBIDsBySingleDimension;
import de.lmu.ifi.dbs.elki.data.model.SubspaceModel;
import de.lmu.ifi.dbs.elki.data.type.TypeInformation;
import de.lmu.ifi.dbs.elki.data.type.TypeUtil;
import de.lmu.ifi.dbs.elki.database.Database;
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
import de.lmu.ifi.dbs.elki.math.linearalgebra.Centroid;
import de.lmu.ifi.dbs.elki.math.linearalgebra.CovarianceMatrix;
import de.lmu.ifi.dbs.elki.math.linearalgebra.Matrix;
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
        StringBuilder buf = new StringBuilder();
        buf.append("Partition sizes of dim ").append(d).append(": ");
        int sum = 0;
        for (DBIDs p : partitions[d]) {
          buf.append(p.size()).append(' ');
          sum += p.size();
        }
        LOG.warning(buf.toString());
        assert (sum == ids.size());
      } else {
        partitions[d] = null; // Flag whole dimension as bad
      }
    }

    if (stepProgress != null) {
      stepProgress.beginStep(2, "Searching for non-uniform bins in support histograms.", LOG);
    }

    // Set markers for each attribute until they're all deemed uniform.
    final long[][] markers = new long[dim][];
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
          break; // Uniform
        }
        BitsUtil.setI(marked, bestBin);
        card++;
      }
      LOG.verbose("Marked bins in dim " + d + ": " + BitsUtil.toString(marked, dim));
    }

    if (stepProgress != null) {
      stepProgress.beginStep(3, "Merging marked bins to 1-signatures.", LOG);
    }

    // Generate projected p-signature intervals.
    ArrayList<Sig> signatures = new ArrayList<>();
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
        LOG.verbose("1-signature: " + d + " " + start + "-" + end);
        signatures.add(new Sig(signature, sids));
        start = (end < dim) ? BitsUtil.nextSetBit(marked, end + 1) : -1;
      }
    }

    if (stepProgress != null) {
      stepProgress.beginStep(4, "Computing cluster cores from merged p-signatures.", LOG);
    }

    FiniteProgress mergeProgress = LOG.isVerbose() ? new FiniteProgress("1-signatures merges", signatures.size(), LOG) : null;

    // Merge to (p+1)-signatures (cluster cores).
    ArrayList<Sig> clusterCores = new ArrayList<>(signatures);
    // Try adding merge 1-signature with each cluster core.
    for (int i = 0; i < signatures.size(); ++i) {
      final Sig signature = signatures.get(i);
      // Don't merge with future signatures:
      final int end = clusterCores.size();
      // Skip previous 1-signatures: merges are symmetrical. But include newly
      // created cluster cores (i.e. those resulting from previous merges).
      FiniteProgress submergeProgress = LOG.isVerbose() ? new FiniteProgress("p-signatures merges", end - (i + 1), LOG) : null;
      for (int j = i + 1; j < end; ++j) {
        final Sig first = clusterCores.get(j);
        final Sig merge = mergeSignatures(first, signature, binCount);
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
    ArrayList<Sig> retain = new ArrayList<>(clusterCores.size());
    for (Sig clusterCore : clusterCores) {
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

    // Refine cluster cores into projected clusters (matrix row->DBID).
    ArrayModifiableDBIDs assigned = DBIDUtil.newArray(relation.size());
    ModifiableDBIDs noise = DBIDUtil.newHashSet();
    Matrix M = computeFuzzyMembership(relation, clusterCores, assigned, noise);
    assignUnassigned(relation, M, clusterCores, assigned, noise);
    M = expectationMaximization(relation, dim, M, assigned);

    if (stepProgress != null) {
      stepProgress.beginStep(6, "Generating hard clustering.", LOG);
    }

    // Create a hard clustering, making sure each data point only is part of one
    // cluster, based on the best match from the membership matrix.
    ArrayList<ClusterCandidate> clusterCandidates = hardClustering(M, clusterCores, assigned);

    if (stepProgress != null) {
      stepProgress.beginStep(7, "Looking for outliers and moving them to the noise set.", LOG);
    }

    // Outlier detection. Remove points from clusters that have a Mahalanobis
    // distance larger than the critical value of the ChiSquare distribution.
    findOutliers(relation, clusterCandidates, dim - countUniformAttributes(markers), noise);

    if (stepProgress != null) {
      stepProgress.beginStep(8, "Removing empty clusters.", LOG);
    }

    // Remove empty clusters.
    for (int cluster = clusterCandidates.size() - 1; cluster >= 0; --cluster) {
      final int size = clusterCandidates.get(cluster).ids.size();
      if (size < minClusterSize) {
        noise.addDBIDs(clusterCandidates.remove(cluster).ids);
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
    result.addToplevelCluster(new Cluster<SubspaceModel<V>>(noise, true));
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
      // LOG.warning("sh: " + sh);
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
    // LOG.warning("ss: " + ss + " se: " + se + " start: " + start + " end: " +
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
   * Counts how many attributes are globally uniform, meaning they do not are
   * not relevant to any cluster.
   * 
   * @param markers the markers used to tag intervals as non-uniform per
   *        dimension.
   * @return Number of dimensions that are irrelevant to the clustering.
   */
  private int countUniformAttributes(long[][] markers) {
    int sum = 0;
    for (int dimension = 0; dimension < markers.length; ++dimension) {
      if (markers[dimension] == null || BitsUtil.cardinality(markers[dimension]) == 0) {
        ++sum;
      }
    }
    return sum;
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

    LOG.warning(mv.toString());
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
   * @param assigned mapping of matrix row to DBID.
   * @param unassigned set to which to add unassigned points.
   * @return the fuzzy membership matrix.
   */
  private Matrix computeFuzzyMembership(Relation<V> relation, ArrayList<Sig> clusterCores, ModifiableDBIDs assigned, ModifiableDBIDs unassigned) {
    final int n = relation.size();
    final int k = clusterCores.size();

    Matrix membership = new Matrix(n, k);

    for (DBIDIter iter = relation.iterDBIDs(); iter.valid(); iter.advance()) {
      // Count in how many cores the object is present.
      int count = 0;
      for (Sig core : clusterCores) {
        if (core.ids.contains(iter)) {
          ++count;
        }
      }

      // Set value(s) in membership matrix.
      if (count > 0) {
        final double fraction = 1.0 / count;
        for (int cluster = 0; cluster < k; ++cluster) {
          if (clusterCores.get(cluster).ids.contains(iter)) {
            membership.set(assigned.size(), cluster, fraction);
          }
        }
        assigned.add(iter);
      } else {
        // Does not match any cluster, mark it.
        unassigned.add(iter);
      }
    }

    return membership;
  }

  /**
   * Assign unassigned objects to best candidate based on shortest Mahalanobis
   * distance.
   * 
   * @param relation Data relation
   * @param M fuzzy membership matrix.
   * @param clusterCores the cluster cores.
   * @param assigned mapping of matrix row to DBID.
   * @param unassigned the list of points not yet assigned.
   */
  private void assignUnassigned(Relation<V> relation, Matrix M, ArrayList<Sig> clusterCores, ArrayModifiableDBIDs assigned, ModifiableDBIDs unassigned) {
    if (unassigned.size() == 0) {
      return;
    }
    final int k = clusterCores.size();

    // Compute centroids and covariance matrixes.
    for (Sig sig : clusterCores) {
      sig.centroid = Centroid.make(relation, sig.ids);
      sig.inverseCovarianceMatrix = CovarianceMatrix.make(relation, sig.ids).destroyToNaiveMatrix().inverse();
    }

    for (DBIDIter iter = unassigned.iter(); iter.valid(); iter.advance()) {
      // Find the best matching known cluster core using the Mahalanobis
      // distance.
      Vector v = relation.get(iter).getColumnVector();
      int bestCluster = -1;
      double minDistance = Double.POSITIVE_INFINITY;
      for (int cluster = 0; cluster < k; ++cluster) {
        Sig c = clusterCores.get(cluster);
        final double distance = MathUtil.mahalanobisDistance(c.inverseCovarianceMatrix, v.minus(c.centroid));
        if (distance < minDistance) {
          minDistance = distance;
          bestCluster = cluster;
        }
      }
      // Assign to best core.
      M.set(assigned.size(), bestCluster, 1);
      assigned.add(iter);
    }

    // Clear the list of unassigned objects.
    unassigned.clear();
  }

  /**
   * Performs an expectation maximization for data points and cluster cores, to
   * determine the probabilities of a data point belonging to a specific
   * cluster.
   * 
   * @see de.lmu.ifi.dbs.elki.algorithm.clustering.EM
   * 
   * @param relation Data relation
   * @param dim Dimensionality
   * @param M the initial membership matrix.
   * @param dbids mapping of matrix row to DBID.
   */
  private Matrix expectationMaximization(Relation<V> relation, int dim, Matrix M, ArrayModifiableDBIDs dbids) {
    final int n = dbids.size();
    final int k = M.getColumnDimensionality();

    Matrix probabilities = new Matrix(M);
    double[] clusterWeights = new double[k];
    double[] normalDistributionFactor = new double[k];

    CovarianceMatrix[] cvms = new CovarianceMatrix[k];
    Vector[] means = new Vector[k];
    Matrix[] covariances = new Matrix[k];
    Matrix[] inverseCovariances = new Matrix[k];

    // Initialize cluster weights to be evenly distributed.
    for (int cluster = 0; cluster < k; ++cluster) {
      clusterWeights[cluster] = 1.0 / k;
    }

    FiniteProgress emIterProgress = LOG.isVerbose() ? new FiniteProgress("EM iteration", maxEmIterations, LOG) : null;

    // Iterate until maximum number of iteration hits or computation converges.
    double em = Double.NEGATIVE_INFINITY;
    for (int iteration = 0; iteration < maxEmIterations; ++iteration) {
      // Reset weights and covariance matrices.
      for (int cluster = 0; cluster < k; ++cluster) {
        clusterWeights[cluster] = 0.0;
        cvms[cluster] = new CovarianceMatrix(dim);
      }

      // Compute covariance matrices.
      for (int point = 0; point < n; ++point) {
        final Vector value = relation.get(dbids.get(point)).getColumnVector();
        for (int cluster = 0; cluster < k; ++cluster) {
          final double pointInClusterProbability = probabilities.get(point, cluster);
          clusterWeights[cluster] += pointInClusterProbability;
          if (pointInClusterProbability > 0) {
            cvms[cluster].put(value, pointInClusterProbability);
          }
        }
      }
      for (int cluster = 0; cluster < k; ++cluster) {
        means[cluster] = cvms[cluster].getMeanVector();
        covariances[cluster] = cvms[cluster].destroyToNaiveMatrix().cheatToAvoidSingularity(1e-9);
        inverseCovariances[cluster] = covariances[cluster].inverse();
      }

      // Normalize weights.
      for (int cluster = 0; cluster < k; ++cluster) {
        clusterWeights[cluster] /= n;
      }
      // Compute normal distribution factor used for computing probabilities.
      final double twopidim = Math.pow(MathUtil.TWOPI, dim);
      for (int cluster = 0; cluster < k; ++cluster) {
        normalDistributionFactor[cluster] = 1.0 / Math.sqrt(twopidim * covariances[cluster].det());
      }

      FiniteProgress emProbProgress = LOG.isVerbose() ? new FiniteProgress("Computing probabilities for points", n, LOG) : null;

      // Recompute probabilities.
      double emNew = 0.0;
      for (int point = 0; point < n; ++point) {
        final Vector value = relation.get(dbids.get(point)).getColumnVector();
        final double[] newProbabilities = new double[k];
        double priorProbability = 0.0;

        for (int cluster = 0; cluster < k; ++cluster) {
          final Vector delta = value.minus(means[cluster]);
          final double squaredMahalanobisDistance = delta.transposeTimesTimes(inverseCovariances[cluster], delta);
          final double probability = normalDistributionFactor[cluster] * Math.exp(-0.5 * squaredMahalanobisDistance);
          newProbabilities[cluster] = probability;
          priorProbability += probability * clusterWeights[cluster];
        }

        final double logProbability = Math.max(Math.log(priorProbability), MIN_LOGLIKELIHOOD);
        assert (!Double.isNaN(logProbability));
        emNew += logProbability;

        for (int cluster = 0; cluster < k; ++cluster) {
          if (priorProbability == 0) {
            probabilities.set(point, cluster, 0);
          } else {
            probabilities.set(point, cluster, newProbabilities[cluster] / priorProbability * clusterWeights[cluster]);
          }
        }

        if (emProbProgress != null) {
          emProbProgress.incrementProcessed(LOG);
        }
      }

      if (emProbProgress != null) {
        emProbProgress.ensureCompleted(LOG);
      }

      // See if the delta is very small, if so we can stop.
      if (Math.abs(em - emNew) < emDelta) {
        if (emIterProgress != null) {
          emIterProgress.setProcessed(maxEmIterations, LOG);
        }
        break;
      }
      em = emNew;

      if (emIterProgress != null) {
        emIterProgress.incrementProcessed(LOG);
      }
    }

    if (emIterProgress != null) {
      emIterProgress.ensureCompleted(LOG);
    }

    return probabilities;
  }

  /**
   * Creates a hard clustering from the specified soft membership matrix.
   * 
   * @param M the membership matrix.
   * @param dbids mapping matrix row to DBID.
   * @return a hard clustering based on the matrix.
   */
  private ArrayList<ClusterCandidate> hardClustering(Matrix M, List<Sig> clusterCores, ArrayModifiableDBIDs dbids) {
    final int n = dbids.size();
    final int k = M.getColumnDimensionality();

    // Initialize cluster sets.
    ArrayList<ClusterCandidate> candidates = new ArrayList<>();
    for (Sig sig : clusterCores) {
      candidates.add(new ClusterCandidate(sig));
    }

    DBIDArrayIter iter = dbids.iter();
    // Perform hard partitioning, assigning each data point only to one cluster,
    // namely that one it is most likely to belong to.
    for (int point = 0; point < n; ++point) {
      int bestCluster = 0;
      double bestProbability = M.get(point, 0);
      for (int cluster = 1; cluster < k; ++cluster) {
        double probability = M.get(point, cluster);
        if (probability > bestProbability) {
          bestCluster = cluster;
          bestProbability = probability;
        }
      }
      iter.seek(point);
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
   * @param clusterCandidates the list of clusters to check.
   * @param nonUniformDimensionCount the number of dimensions to consider when
   *        testing.
   * @param noise the set to which to add points deemed outliers.
   */
  private void findOutliers(Relation<V> relation, ArrayList<ClusterCandidate> clusterCandidates, int nonUniformDimensionCount, ModifiableDBIDs noise) {
    final int k = clusterCandidates.size();

    for (int cluster = 0; cluster < k; ++cluster) {
      final ClusterCandidate candidate = clusterCandidates.get(cluster);
      if (candidate.ids.size() < 2) {
        continue;
      }
      final CovarianceMatrix cvm = CovarianceMatrix.make(relation, candidate.ids);
      final Vector mean = cvm.getMeanVector();
      final Matrix inverseCovariance = cvm.destroyToNaiveMatrix().cheatToAvoidSingularity(10e-9).inverse();
      // FIXME: use an array iterator, instead of "get" - more efficient!
      for (DBIDMIter iter = candidate.ids.iter(); iter.valid(); iter.advance()) {
        final Vector value = relation.get(iter).getColumnVector();
        final Vector delta = mean.minus(value);
        final double distance = MathUtil.mahalanobisDistance(inverseCovariance, delta);
        final int dof = candidate.dimensions.cardinality() - 1;
        // final double distance = candidate.clusterCore.computeDistance(value);
        // final int dof = nonUniformDimensionCount - 1;
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
  protected Sig mergeSignatures(Sig first, Sig second, int numBins) {
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
    return new Sig(spec, intersection);
  }

  private static class Sig implements Cloneable {
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
     * Cached centroid, initialized as necessary.
     */
    Centroid centroid = null;

    /**
     * Cached covariance matrix, initialized as necessary.
     */
    Matrix inverseCovarianceMatrix = null;

    /**
     * Constructor.
     * 
     * @param spec Subspace specification
     * @param ids IDs.
     */
    private Sig(int[] spec, DBIDs ids) {
      super();
      this.spec = spec;
      this.ids = ids;
    }

    @Override
    protected Sig clone() throws CloneNotSupportedException {
      Sig c = (Sig) super.clone();
      c.spec = this.spec.clone();
      c.ids = null;
      return c;
    }
  }

  /**
   * Class representing a p-signature (where p is the size of the vector).
   * 
   * <p>
   * Important: this only overrides the methods used in this algorithm!
   * </p>
   */
  private static class Signature {
    /**
     * The intervals contributing to this signature.
     */
    private final ArrayList<Interval> intervals = new ArrayList<>();

    /**
     * The dimensions this signature spans.
     */
    private final BitSet dimensions = new BitSet();

    /**
     * The data points contained in the hyper-cube spanned by this signature.
     */
    private final DBIDs supportSet;

    /**
     * Cached centroid, initialized as necessary.
     */
    private Centroid centroid;

    /**
     * Cached covariance matrix, initialized as necessary.
     */
    private Matrix inverseCovarianceMatrix;

    /**
     * Creates a new 1-signature for the specified interval.
     * 
     * @param relation the relation to get the support set.
     * @param interval the interval.
     */
    public Signature(Relation<? extends NumberVector<?>> relation, Interval interval) {
      intervals.add(interval);
      dimensions.set(interval.getDimension());
      // TODO Replace with DB query? Only runs once, so probably not so
      // important.
      ModifiableDBIDs support = DBIDUtil.newHashSet();
      for (DBIDIter iter = relation.iterDBIDs(); iter.valid(); iter.advance()) {
        double value = relation.get(iter).doubleValue(interval.getDimension());
        if (interval.getMin() <= value && interval.getMax() >= value) {
          support.add(iter);
        }
      }
      this.supportSet = support;
    }

    /**
     * Creates a merged signature of two other signatures.
     * 
     * @param a the one signature.
     * @param b the other signature.
     */
    private Signature(Signature a, Signature b) {
      intervals.addAll(a.intervals);
      intervals.addAll(b.intervals);
      dimensions.or(a.dimensions);
      dimensions.or(b.dimensions);
      supportSet = DBIDUtil.intersection(a.supportSet, b.supportSet);
    }

    /**
     * The support for this signature (<code>Supp(S)</code>).
     */
    public int getSupport() {
      return supportSet.size();
    }

    /**
     * Generates a merged signature of this and another one, where the other
     * signature must be a 1-signature.
     * 
     * @param other the other signature to create a merge with.
     * @return the merged signature, or null if the merge failed.
     */
    public Signature tryMerge(Signature other, double poissonThreshold) {
      // Validate input.
      if (other.intervals.size() != 1) {
        throw new IllegalArgumentException("Other signature must be 1-signature.");
      }

      // Skip the merge if the interval is already part of the signature.
      if (intervals.contains(other.intervals.get(0))) {
        return null;
      }

      // Create merged signature.
      Signature merged = new Signature(this, other);

      // Definition 3, Condition 1:
      int v = merged.getSupport();
      double E = expectedSupport(other.intervals.get(0));
      if (v > E && poisson(v, E) < poissonThreshold) {
        // Condition is fulfilled, allow the merge.
        return merged;
      } else {
        // Does not qualify for a merge.
        return null;
      }
    }

    /**
     * Validates a cluster core against a 1-signature based on Definition 3,
     * Condition 2, for pruning of cluster cores after the merge step.
     * 
     * @param other the interval to validate against.
     * @return true if the cluster core holds, else false.
     */
    public boolean validate(Signature other, double poissonThreshold) {
      if (intervals.contains(other.intervals.get(0))) {
        // Interval is contained, don't check.
        return true;
      }

      // Interval is not in cluster core, validate.
      Signature merge = new Signature(this, other);

      // Definition 3, Condition 2:
      int v = merge.getSupport();
      double E = expectedSupport(other.intervals.get(0));
      return v <= E || poisson(v, E) >= poissonThreshold;
    }

    /**
     * Computes the Mahalanobis distance for the specified vector to the
     * centroid of the cluster core represented by this signature.
     * 
     * @param relation Data relation
     * @param v the vector to compute the distance for.
     * @return Mahalanobis distance.
     */
    public double computeDistance(Relation<? extends NumberVector<?>> relation, Vector v) {
      // Lazy initialization.
      if (centroid == null) {
        centroid = Centroid.make(relation, supportSet);
        inverseCovarianceMatrix = CovarianceMatrix.make(relation, supportSet).destroyToNaiveMatrix().inverse();
      }
      return MathUtil.mahalanobisDistance(inverseCovarianceMatrix, centroid.minus(v));
    }

    /**
     * Computes the expected support of the (p+1)-signature resulting from
     * adding the specified interval to this signature.
     * 
     * @param interval the interval to compute for.
     * @return the expected support.
     */
    private double expectedSupport(Interval interval) {
      return getSupport() * (interval.getMax() - interval.getMin());
    }

    /**
     * Computes the value of the Poisson probability density function as defined
     * in the paper (page 4).
     * 
     * @param v the number of observed occurrences.
     * @param E the number of expected occurrences.
     * @return the probability of observing the observation.
     */
    private double poisson(int v, double E) {
      /*
       * // This is precise but - as you would expect - very very slow. final
       * BigDecimal exp = BigDecimal.valueOf(Math.exp(-E)); final BigDecimal pow
       * = BigDecimal.valueOf(E).pow(v); final BigDecimal dividend =
       * exp.multiply(pow); final BigDecimal divisor = new
       * BigDecimal(MathUtil.factorial(BigInteger.valueOf(v))); final double
       * result = dividend.divide(divisor, 40,
       * RoundingMode.HALF_DOWN).doubleValue(); return result; /
       */
      // This is fast but tends to become inaccurate.
      // return Math.exp(-E) * Math.pow(E, v) /
      // MathUtil.approximateFactorial(v);
      // So we move to log space which allows a fast approximation of the
      // factorial (using the natural logarithm) via `v! = sum[i=1-v](log(i))`.
      // e^-E * e^v / v! = e^(log(e^-E * E^v / v!)) = e^(log(e^-E) + log(E^v) -
      // log(v!)) = e^(E + log(E^v) - sum[i=1-v](log(i)))
      double sum = 0;
      for (int i = 1; i <= v; ++i) {
        sum += Math.log(sum);
      }
      return Math.exp(E + Math.log(Math.pow(E, v)) - sum);
      // */
    }
  }

  /**
   * This class is used to represent potential clusters.
   * 
   * TODO: Documentation.
   */
  private static class ClusterCandidate {
    public final BitSet dimensions;

    public final Sig clusterCore;

    public final ModifiableDBIDs ids;

    public ClusterCandidate(Sig clusterCore) {
      this.clusterCore = clusterCore;
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
