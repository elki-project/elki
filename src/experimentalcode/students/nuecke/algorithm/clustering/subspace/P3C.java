package experimentalcode.students.nuecke.algorithm.clustering.subspace;

import java.util.ArrayList;
import java.util.BitSet;

import de.lmu.ifi.dbs.elki.algorithm.AbstractAlgorithm;
import de.lmu.ifi.dbs.elki.data.Cluster;
import de.lmu.ifi.dbs.elki.data.Clustering;
import de.lmu.ifi.dbs.elki.data.Interval;
import de.lmu.ifi.dbs.elki.data.NumberVector;
import de.lmu.ifi.dbs.elki.data.model.SubspaceModel;
import de.lmu.ifi.dbs.elki.data.type.TypeInformation;
import de.lmu.ifi.dbs.elki.data.type.TypeUtil;
import de.lmu.ifi.dbs.elki.database.Database;
import de.lmu.ifi.dbs.elki.database.ids.ArrayModifiableDBIDs;
import de.lmu.ifi.dbs.elki.database.ids.DBIDIter;
import de.lmu.ifi.dbs.elki.database.ids.DBIDRef;
import de.lmu.ifi.dbs.elki.database.ids.DBIDUtil;
import de.lmu.ifi.dbs.elki.database.ids.DBIDs;
import de.lmu.ifi.dbs.elki.database.relation.Relation;
import de.lmu.ifi.dbs.elki.database.relation.RelationUtil;
import de.lmu.ifi.dbs.elki.logging.Logging;
import de.lmu.ifi.dbs.elki.math.MathUtil;
import de.lmu.ifi.dbs.elki.math.linearalgebra.Centroid;
import de.lmu.ifi.dbs.elki.math.linearalgebra.CovarianceMatrix;
import de.lmu.ifi.dbs.elki.math.linearalgebra.Matrix;
import de.lmu.ifi.dbs.elki.math.linearalgebra.Vector;
import de.lmu.ifi.dbs.elki.math.statistics.distribution.ChiSquaredDistribution;
import de.lmu.ifi.dbs.elki.math.statistics.distribution.PoissonDistribution;
import de.lmu.ifi.dbs.elki.utilities.documentation.Reference;
import de.lmu.ifi.dbs.elki.utilities.documentation.Title;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.AbstractParameterizer;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.Parameterization;
import experimentalcode.students.nuecke.utilities.datastructures.histogram.HistogramUtil;
import experimentalcode.students.nuecke.utilities.datastructures.histogram.SupportHistogram;

/**
 * <p>
 * Provides the P3C algorithm, a projected clustering algorithm.
 * </p>
 * 
 * <p>
 * Reference: <br/>
 * Gabriela Moise, J&ouml;rg Sander, Martin Ester: P3C: A Robust Projected
 * Clustering Algorithm. <br/>
 * In: TODO.
 * </p>
 * 
 * @author Florian Nuecke
 * 
 * @apiviz.has SubspaceModel
 * 
 * @param <V> the type of NumberVector handled by this Algorithm.
 */
@Title("HARP: Hierarchical approach with Automatic Relevant dimension selection for Projected clustering")
@Reference(authors = "Gabriela Moise, J&ouml;rg Sander, Martin Ester", title = "P3C: A Robust Projected Clustering Algorithm", booktitle = "TODO", url = "TODO")
public class P3C<V extends NumberVector<?>> extends AbstractAlgorithm<Clustering<SubspaceModel<V>>> {

  /**
   * The logger for this class.
   */
  private static final Logging logger = Logging.getLogger(P3C.class);

  // ---------------------------------------------------------------------- //
  // Configuration
  // ---------------------------------------------------------------------- //

  /**
   * Threshold value for Poisson-test when merging signatures.
   */
  private static final double POISSON_THRESHOLD = 1.0e-20;

  // ---------------------------------------------------------------------- //
  // Relevant for a single run.
  // ---------------------------------------------------------------------- //

  /**
   * The relation we're now working on.
   */
  private Relation<V> relation;

  /**
   * Dimensionality of the set we're now working on.
   */
  private int dimensionality;

  // ---------------------------------------------------------------------- //

  // ---------------------------------------------------------------------- //
  // Run methods.
  // ---------------------------------------------------------------------- //

  /**
   * Performs the P3C algorithm on the given Database.
   */
  public Clustering<SubspaceModel<V>> run(Database database, Relation<V> relation) {
    this.dimensionality = RelationUtil.dimensionality(relation);
    this.relation = relation;

    // Allocate histograms and markers for attributes.
    final int binCount = HistogramUtil.getSturgeBinCount(relation.size());
    final SupportHistogram[] histograms = HistogramUtil.newSupportHistograms(relation, binCount);
    final BitSet[] markers = new BitSet[dimensionality];
    for(int dimension = 0; dimension < dimensionality; ++dimension) {
      markers[dimension] = new BitSet(binCount);
    }

    // Set markers for each attribute until they're all deemed uniform.
    for(int dimension = 0; dimension < dimensionality; ++dimension) {
      final SupportHistogram histogram = histograms[dimension];
      final BitSet marked = markers[dimension];
      while(!chiSquaredUniformTest(histogram, marked)) {
        // Find bin with largest support, test only the dimensions that were not
        // previously marked.
        int bestBin = -1;
        int bestSupport = 0;
        for(SupportHistogram.Iter iter = histogram.iter(); iter.valid(); iter.advance()) {
          final int bin = iter.getOffset();
          final int binSupport = iter.getValue();
          // Ignore already marked bins.
          if(marked.get(bin)) {
            continue;
          }
          if(binSupport > bestSupport) {
            bestBin = bin;
            bestSupport = binSupport;
          }
        }
        marked.set(bestBin);
      }
    }

    // Generate projected p-signature intervals.
    ArrayList<Interval> intervals = new ArrayList<Interval>();
    for(int dimension = 0; dimension < dimensionality; ++dimension) {
      final SupportHistogram histogram = histograms[dimension];
      final BitSet marked = markers[dimension];
      boolean inInterval = false;
      double start = Double.NaN, end = Double.NaN;
      for(SupportHistogram.Iter iter = histogram.iter(); iter.valid(); iter.advance()) {
        final int bin = iter.getOffset();
        if(marked.get(bin)) {
          if(inInterval) {
            // Interval continues.
            end = iter.getRight();
          }
          else {
            // Starting new interval at this bin.
            start = iter.getLeft();
            end = iter.getRight();
            inInterval = true;
          }
        }
        else {
          if(inInterval) {
            // Interval ends at previous bin.
            intervals.add(new Interval(dimension, start, end));
            inInterval = false;
          }
          // else: Not in interval, so we skip adjacent unmarked bins.
        }
      }
      if(inInterval) {
        // Finish last interval.
        intervals.add(new Interval(dimension, start, end));
      }
    }

    // Build 1-signatures from intervals.
    ArrayList<Signature> signatures = new ArrayList<Signature>(intervals.size());
    for(Interval i : intervals) {
      signatures.add(new Signature(relation, i));
    }

    // Merge to (p+1)-signatures (cluster cores).
    ArrayList<Signature> clusterCores = new ArrayList<Signature>(signatures);
    // Try adding merge 1-signature with each cluster core.
    for(int i = 0; i < signatures.size(); ++i) {
      final Signature signature = signatures.get(i);
      // Fixed size avoids redundant merges.
      final int k = clusterCores.size();
      // Skip previous 1-signatures: merges are symmetrical. But include newly
      // created cluster cores (i.e. those resulting from previous merges).
      for(int j = i + 1; j < k; ++j) {
        final Signature merge = clusterCores.get(j).tryMerge(signature);
        if(merge != null) {
          // We add each potential core to the list to allow remaining
          // 1-signatures to try merging with this p-signature as well.
          clusterCores.add(merge);
        }
      }
    }

    // Prune cluster cores based on Definition 3, Condition 2.
    for(int i = clusterCores.size() - 1; i >= 0; --i) {
      Signature clusterCore = clusterCores.get(i);
      for(int j = 0; j < signatures.size(); ++j) {
        if(!clusterCore.validate(signatures.get(j))) {
          clusterCores.remove(i);
          break;
        }
      }
    }

    // Refine cluster cores into projected clusters.
    ArrayList<DBIDRef> dbids = new ArrayList<DBIDRef>(); // matrix row->DBID
    Matrix M = computeFuzzyMembership(clusterCores, dbids);
    assignUnassigned(M, clusterCores, dbids);
    M = expectationMaximization(M, dbids);
    ArrayModifiableDBIDs[] clusterCandidates = hardClustering(M, dbids);

    // Outlier detection. Remove points from clusters that have a mahalanobis
    // distance larger than the critical value of the Chi-square distribution.
    DBIDs noise = findOutliers(clusterCandidates, signatures);

    // Relevant attribute computation. This looks for attributes among those
    // formerly deemed uniform which may actually be relevant for some clusters.
    // TODO ...

    Clustering<SubspaceModel<V>> result = new Clustering<SubspaceModel<V>>("P3C", "P3C");
    result.addToplevelCluster(new Cluster<SubspaceModel<V>>(noise, true));
    for(int cluster = 0; cluster < clusterCandidates.length; ++cluster) {
      result.addToplevelCluster(new Cluster<SubspaceModel<V>>(clusterCandidates[cluster],
      // TODO
      new SubspaceModel<V>(null, null)));
    }
    return result;
  }

  // ---------------------------------------------------------------------- //
  // Auxiliary algorithm methods.
  // ---------------------------------------------------------------------- //

  /**
   * Performs a ChiSquared test to determine whether an attribute has a uniform
   * distribution.
   * 
   * @param histogram the histogram data for the dimension to check for.
   * @param marked the marked bins that should be ignored.
   * @return true if the dimension is uniformly distributed.
   */
  private boolean chiSquaredUniformTest(SupportHistogram histogram, BitSet marked) {
    final int binCount = histogram.getNumBins() - marked.cardinality();
    // Get global mean over all unmarked bins.
    double mean = 0;
    for(SupportHistogram.Iter iter = histogram.iter(); iter.valid(); iter.advance()) {
      final int bin = iter.getOffset();
      final int binSupport = iter.getValue();
      // Ignore already marked bins.
      if(marked.get(bin)) {
        continue;
      }
      mean += binSupport;
    }
    mean /= binCount;

    // Compute ChiSquare statistic.
    double chiSquare = 0;
    for(SupportHistogram.Iter iter = histogram.iter(); iter.valid(); iter.advance()) {
      final int bin = iter.getOffset();
      final int binSupport = iter.getValue();
      // Ignore already marked bins.
      if(marked.get(bin)) {
        continue;
      }
      final double delta = binSupport - mean;
      chiSquare += delta * delta;
    }
    chiSquare /= mean;

    return chiSquaredQuantiles[binCount - 1] >= chiSquare;
  }

  /**
   * ChiSquare quantiles, indexed by degrees of freedom.
   * http://www.itl.nist.gov/div898/handbook/eda/section3/eda3674.htm
   */
  private static double[] chiSquaredQuantiles = new double[] { 10.828, 13.816, 16.266, 18.467, 20.515, 22.458, 24.322, 26.125, 27.877, 29.588, 31.264, 32.910, 34.528, 36.123, 37.697, 39.252, 40.790, 42.312, 43.820, 45.315, 46.797, 48.268, 49.728, 51.179, 52.620, 54.052, 55.476, 56.892, 58.301, 59.703, 61.098, 62.487, 63.870, 65.247, 66.619, 67.985, 69.347, 70.703, 72.055, 73.402, 74.745, 76.084, 77.419, 78.750, 80.077, 81.400, 82.720, 84.037, 85.351, 86.661, 87.968, 89.272, 90.573, 91.872, 93.168, 94.461, 95.751, 97.039, 98.324, 99.607, 100.888, 102.166, 103.442, 104.716, 105.988, 107.258, 108.526, 109.791, 111.055, 112.317, 113.577, 114.835, 116.092, 117.346, 118.599, 119.850, 121.100, 122.348, 123.594, 124.839, 126.083, 127.324, 128.565, 129.804, 131.041, 132.277, 133.512, 134.746, 135.978, 137.208, 138.438, 139.666, 140.893, 142.119, 143.344, 144.567, 145.789, 147.010, 148.230, 149.449 };

  /**
   * Computes a fuzzy membership with the weights based on which cluster cores
   * each data point is part of.
   * 
   * @param clusterCores the cluster cores.
   * @param dbids mapping of matrix row to DBID.
   * @return the fuzzy membership matrix.
   */
  private Matrix computeFuzzyMembership(ArrayList<Signature> clusterCores, ArrayList<DBIDRef> dbids) {
    final int n = relation.size();
    final int k = clusterCores.size();

    Matrix membership = new Matrix(n, k);

    for(DBIDIter iter = relation.iterDBIDs(); iter.valid(); iter.advance()) {
      dbids.add(iter);

      // Count in how many cores the object is present.
      int count = 0;
      for(int l = 0; l < k; ++l) {
        if(clusterCores.get(l).supportSet.contains(iter)) {
          ++count;
        }
      }

      // Set value in membership matrix.
      if(count > 0) {
        final double fraction = 1.0 / count;
        final int i = dbids.size() - 1;
        for(int l = 0; l < k; ++l) {
          if(clusterCores.get(l).supportSet.contains(iter)) {
            membership.set(i, l, fraction);
          }
        }
      }
    }

    return membership;
  }

  /**
   * Assign unassigned objects to best candidate based on shortest Mahalanobis
   * distance.
   * 
   * @param M fuzzy membership matrix.
   * @param clusterCores the cluster cores.
   * @param dbids mapping of matrix row to DBID.
   */
  private void assignUnassigned(Matrix M, ArrayList<Signature> clusterCores, ArrayList<DBIDRef> dbids) {
    final int n = relation.size();
    final int k = clusterCores.size();

    outer: for(int point = 0; point < n; ++point) {
      for(int cluster = 0; cluster < k; ++cluster) {
        if(M.get(point, cluster) > 0) {
          // Part of at least one core, meaning we don't have to find a core
          // to add it to. Continue with the next object.
          continue outer;
        }
      }
      // If we come here the object is not part of any cluster core. Find the
      // best matching known cluster core using the Mahalanobis distance.
      Vector v = relation.get(dbids.get(point)).getColumnVector();
      int bestCluster = 0;
      double minDistance = Double.POSITIVE_INFINITY;
      for(int cluster = 1; cluster < k; ++cluster) {
        double distance = clusterCores.get(cluster).computeDistance(v);
        if(distance < minDistance) {
          minDistance = distance;
          bestCluster = cluster;
        }
      }
      // Assign to best core.
      M.set(point, bestCluster, 1);
    }
  }

  /**
   * Performs an expectation maximization for data points and cluster cores, to
   * determine the probabilities of a data point belonging to a specific
   * cluster.
   * 
   * @param M the initial membership matrix.
   * @param dbids mapping of matrix row to DBID.
   */
  private Matrix expectationMaximization(Matrix M, ArrayList<DBIDRef> dbids) {
    final int n = M.getRowDimensionality();
    final int k = M.getColumnDimensionality();

    Vector[] means = new Vector[k];
    Matrix[] covariances = new Matrix[k];
    Matrix probabilities = new Matrix(n, k);

    // Initialize variables based on original membership guess.
    for(int cluster = 0; cluster < k; ++cluster) {
      CovarianceMatrix cvm = new CovarianceMatrix(dimensionality);
      for(int point = 0; point < n; ++cluster) {
        cvm.put(relation.get(dbids.get(point)), M.get(point, cluster));
      }
      means[cluster] = cvm.getMeanVector();
      covariances[cluster] = cvm.destroyToNaiveMatrix();
    }

    // Do EM as paper says...
    final int maxEmIterations = 5;
    for(int iteration = 0; iteration < maxEmIterations; ++iteration) {
      // ... using mahalanobis to compute probabilities.
      for(int point = 0; point < n; ++point) {
        for(int cluster = 0; cluster < k; ++cluster) {
          // TODO does this need normalization?
          probabilities.set(point, cluster, MathUtil.mahalanobisDistance(covariances[cluster], means[cluster].minus(relation.get(dbids.get(point)).getColumnVector())));
        }
      }

      // Iterate means and covariance matrices. Stop if means don't change.
      boolean allMeansEqual = true;
      for(int cluster = 0; cluster < k; ++cluster) {
        CovarianceMatrix cvm = new CovarianceMatrix(dimensionality);
        for(int point = 0; point < n; ++cluster) {
          cvm.put(relation.get(dbids.get(point)), M.get(point, cluster));
        }
        Vector mean = cvm.getMeanVector();
        for(int dim = 0; dim < dimensionality; ++dim) {
          if(Math.abs(mean.get(dim) - means[cluster].get(dim)) > 0.001) {
            allMeansEqual = false;
            break;
          }
        }
        means[cluster] = mean;
        covariances[cluster] = cvm.destroyToNaiveMatrix();
      }
      if(allMeansEqual) {
        break;
      }
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
  private ArrayModifiableDBIDs[] hardClustering(Matrix M, ArrayList<DBIDRef> dbids) {
    final int n = M.getRowDimensionality();
    final int k = M.getColumnDimensionality();

    // Initialize cluster sets.
    ArrayModifiableDBIDs[] clusters = new ArrayModifiableDBIDs[k];
    for(int cluster = 0; cluster < k; ++cluster) {
      clusters[cluster] = DBIDUtil.newArray();
    }

    // Perform hard partitioning, assigning each data point only to one cluster,
    // namely that one it is most likely to belong to.
    for(int point = 0; point < n; ++point) {
      int bestCluster = 0;
      double bestProbability = M.get(point, 0);
      for(int cluster = 1; cluster < k; ++cluster) {
        double probability = M.get(point, cluster);
        if(probability > bestProbability) {
          bestCluster = cluster;
          bestProbability = probability;
        }
      }
      clusters[bestCluster].add(dbids.get(point));
    }

    return clusters;
  }

  /**
   * Performs outlier detection by testing the mahalanobis distance of each
   * point in a cluster against the critical value of the Chi-squared
   * distribution with as many degrees of freedom as the cluster has relevant
   * attributes.
   * 
   * @param clusters the list of clusters to check.
   * @param signatures the signatures for the clusters, for relevant attributes.
   * @return the set of points deemed outliers.
   */
  private DBIDs findOutliers(ArrayModifiableDBIDs[] clusters, ArrayList<Signature> signatures) {
    final int k = clusters.length;

    ArrayModifiableDBIDs outliers = DBIDUtil.newArray();

    for(int cluster = 0; cluster < k; ++cluster) {
      CovarianceMatrix cvm = CovarianceMatrix.make(relation, clusters[cluster]);
      Vector mean = cvm.getMeanVector();
      Matrix covariance = cvm.destroyToNaiveMatrix();
      double cv = ChiSquaredDistribution.cdf(0.001f, signatures.get(cluster).dimensions.cardinality());
      for(int point = clusters[cluster].size() - 1; point >= 0; ++point) {
        if(MathUtil.mahalanobisDistance(covariance, mean.minus(relation.get(clusters[cluster].get(point)).getColumnVector())) > cv) {
          // Outlier, remove it and add it to the outlier set.
          outliers.add(clusters[cluster].remove(point));
        }
      }
    }

    return outliers;
  }

  // ---------------------------------------------------------------------- //
  // Helper class representing p-signatures.
  // ---------------------------------------------------------------------- //

  /**
   * Class representing a p-signature (where p is the size of the vector).
   * 
   * <p>
   * Important: this only overrides the methods used in this algorithm!
   * </p>
   */
  private class Signature {
    /**
     * The intervals contributing to this signature.
     */
    private final ArrayList<Interval> intervals = new ArrayList<Interval>();

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
    private Matrix covarianceMatrix;

    /**
     * Creates a new 1-signature for the specified interval.
     * 
     * @param relation the relation to get the support set.
     * @param interval the interval.
     */
    public Signature(Relation<V> relation, Interval interval) {
      intervals.add(interval);
      dimensions.set(interval.getDimension());
      // TODO replace with db query?
      ArrayModifiableDBIDs support = DBIDUtil.newArray();
      for(DBIDIter iter = relation.iterDBIDs(); iter.valid(); iter.advance()) {
        double value = relation.get(iter).doubleValue(interval.getDimension());
        if(interval.getMin() <= value && interval.getMax() >= value) {
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
    public Signature tryMerge(Signature other) {
      // Validate input.
      if(other.intervals.size() != 1) {
        throw new IllegalArgumentException("Other signature must be 1-signature.");
      }

      // Skip the merge if the interval is already part of the signature.
      if(intervals.contains(other.intervals.get(0))) {
        return null;
      }

      // Create merged signature.
      Signature merged = new Signature(this, other);

      // Definition 3, Condition 1:
      double v = merged.getSupport();
      double E = expectedSupport(other.intervals.get(0));
      if(v > E && PoissonDistribution.rawProbability(v, E) < POISSON_THRESHOLD) {
        // Condition is fulfilled, allow the merge.
        return merged;
      }
      else {
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
    public boolean validate(Signature other) {
      if(intervals.contains(other.intervals.get(0))) {
        // Interval is contained, don't check.
        return true;
      }

      // Interval is not in cluster core, validate.
      Signature merge = new Signature(this, other);

      // Definition 3, Condition 2:
      double v = merge.getSupport();
      double E = expectedSupport(other.intervals.get(0));
      return v <= E || PoissonDistribution.rawProbability(v, E) >= POISSON_THRESHOLD;
    }

    /**
     * Computes the Mahalanobis distance for the specified vector to the
     * centroid of the cluster core represented by this signature.
     * 
     * @param v the vector to compute the distance for.
     * @return
     */
    public double computeDistance(de.lmu.ifi.dbs.elki.math.linearalgebra.Vector v) {
      // Lazy initialization.
      if(centroid == null) {
        centroid = Centroid.make(relation, supportSet);
        covarianceMatrix = CovarianceMatrix.make(relation, supportSet).destroyToNaiveMatrix();
      }
      return MathUtil.mahalanobisDistance(covarianceMatrix, centroid.minus(v));
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
  }

  // ---------------------------------------------------------------------- //

  @Override
  public TypeInformation[] getInputTypeRestriction() {
    return TypeUtil.array(TypeUtil.NUMBER_VECTOR_FIELD);
  }

  @Override
  protected Logging getLogger() {
    return logger;
  }

  // ---------------------------------------------------------------------- //
  // Parameterization.
  // ---------------------------------------------------------------------- //

  /**
   * Parameterization class.
   * 
   * @apiviz.exclude
   */
  public static class Parameterizer<V extends NumberVector<?>> extends AbstractParameterizer {

    @Override
    protected void makeOptions(Parameterization config) {
      super.makeOptions(config);
    }

    @Override
    protected P3C<V> makeInstance() {
      return new P3C<V>();
    }
  }
}
