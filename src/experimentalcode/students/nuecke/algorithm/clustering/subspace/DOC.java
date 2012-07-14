package experimentalcode.students.nuecke.algorithm.clustering.subspace;

import java.util.BitSet;
import java.util.Random;
import java.util.Vector;

import de.lmu.ifi.dbs.elki.algorithm.AbstractAlgorithm;
import de.lmu.ifi.dbs.elki.algorithm.clustering.subspace.SubspaceClusteringAlgorithm;
import de.lmu.ifi.dbs.elki.data.Cluster;
import de.lmu.ifi.dbs.elki.data.Clustering;
import de.lmu.ifi.dbs.elki.data.HyperBoundingBox;
import de.lmu.ifi.dbs.elki.data.NumberVector;
import de.lmu.ifi.dbs.elki.data.Subspace;
import de.lmu.ifi.dbs.elki.data.model.SubspaceModel;
import de.lmu.ifi.dbs.elki.data.type.TypeInformation;
import de.lmu.ifi.dbs.elki.data.type.TypeUtil;
import de.lmu.ifi.dbs.elki.database.Database;
import de.lmu.ifi.dbs.elki.database.ids.ArrayModifiableDBIDs;
import de.lmu.ifi.dbs.elki.database.ids.DBID;
import de.lmu.ifi.dbs.elki.database.ids.DBIDIter;
import de.lmu.ifi.dbs.elki.database.ids.DBIDUtil;
import de.lmu.ifi.dbs.elki.database.ids.DBIDs;
import de.lmu.ifi.dbs.elki.database.relation.Relation;
import de.lmu.ifi.dbs.elki.logging.Logging;
import de.lmu.ifi.dbs.elki.logging.progress.FiniteProgress;
import de.lmu.ifi.dbs.elki.logging.progress.IndefiniteProgress;
import de.lmu.ifi.dbs.elki.math.linearalgebra.Centroid;
import de.lmu.ifi.dbs.elki.utilities.DatabaseUtil;
import de.lmu.ifi.dbs.elki.utilities.documentation.Reference;
import de.lmu.ifi.dbs.elki.utilities.documentation.Title;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.AbstractParameterizer;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.OptionID;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.constraints.GreaterConstraint;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.constraints.GreaterEqualConstraint;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.constraints.LessConstraint;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.constraints.LessEqualConstraint;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.constraints.ParameterConstraint;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.Parameterization;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.DoubleParameter;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.Flag;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.IntParameter;

/**
 * <p>
 * Provides the DOC algorithm, and it's heuristic variant, FastDOC. DOC is a
 * sampling based subspace clustering algorithm.
 * </p>
 * 
 * <p>
 * Reference: <br/>
 * Cecilia M. Procopiuc, Michael Jones, Pankaj K. Agarwal, T. M. Murali: A Monte
 * Carlo algorithm for fast projective clustering. <br/>
 * In: Proc. ACM SIGMOD Int. Conf. on Management of Data (SIGMOD '02).
 * </p>
 * 
 * @author Florian Nuecke
 * 
 * @apiviz.has SubspaceModel
 * 
 * @param <V> the type of NumberVector handled by this Algorithm.
 */
@Title("DOC: Density-based Optimal projective Clustering")
@Reference(authors = "Cecilia M. Procopiuc, Michael Jones, Pankaj K. Agarwal, T. M. Murali", title = "A Monte Carlo algorithm for fast projective clustering", booktitle = "Proc. ACM SIGMOD Int. Conf. on Management of Data (SIGMOD '02)", url = "http://dx.doi.org/10.1145/564691.564739")
public class DOC<V extends NumberVector<V, ?>> extends AbstractAlgorithm<Clustering<SubspaceModel<V>>> implements SubspaceClusteringAlgorithm<SubspaceModel<V>> {
  /**
   * The logger for this class.
   */
  private static final Logging logger = Logging.getLogger(DOC.class);

  // ---------------------------------------------------------------------- //
  // Configuration
  // ---------------------------------------------------------------------- //

  public static final OptionID ALPHA_ID = OptionID.getOrCreateOptionID("doc.alpha", "Minimum relative density for a set of points to be considered a cluster (|C|>=doc.alpha*|S|).");

  public static final OptionID BETA_ID = OptionID.getOrCreateOptionID("doc.beta", "Preference of cluster size versus number of relevant dimensions (higher value means higher priority on larger clusters).");

  public static final OptionID W_ID = OptionID.getOrCreateOptionID("doc.w", "Maximum extent of scattering of points along a single attribute for the attribute to be considered relevant.");

  public static final OptionID HEURISTICS_ID = OptionID.getOrCreateOptionID("doc.fastdoc", "Use heuristics as described, thus using the FastDOC algorithm (not yet implemented).");

  public static final OptionID D_ZERO_ID = OptionID.getOrCreateOptionID("doc.d0", "Parameter for FastDOC, setting the number of relevant attributes which, when found for a cluster, are deemed enough to stop iterating.");

  /**
   * Holds the value of {@link #ALPHA_ID}.
   */
  private double alpha;

  /**
   * Holds the value of {@link #BETA_ID}.
   */
  private double beta;

  /**
   * Holds the value of {@link #W_ID}.
   */
  private double w;

  /**
   * Holds the value of {@link #HEURISTICS_ID}.
   */
  private boolean heuristics;

  /**
   * Holds the value of {@link #D_ZERO_ID}.
   */
  private int d_zero;

  // ---------------------------------------------------------------------- //
  // Relevant for a single run.
  // Kind of ugly to keep those as instance variables, but reduces redundancy
  // for DOC vs. FastDOC a lot, so it's justified.
  // ---------------------------------------------------------------------- //

  /**
   * Randomizer used internally for sampling points.
   */
  private Random random = new Random();

  /**
   * The set of points we're working on.
   */
  private ArrayModifiableDBIDs S;

  /**
   * Dimensionality of the data set we're currently working on.
   */
  private int d;

  /**
   * Size of random samples.
   */
  private double r;

  /**
   * Number of inner iterations (per seed point).
   */
  private int m;

  /**
   * Number of outer iterations (seed points).
   */
  private int n;

  /**
   * Minimum size a cluster must have to be accepted.
   */
  private int minClusterSize;

  // ---------------------------------------------------------------------- //

  /**
   * Triggers execution of SkyNet and will eat your kittens.
   * 
   * @param alpha &alpha; input parameter.
   * @param beta &beta; input parameter.
   * @param w <em>w</em> input parameter.
   * @param heuristics whether to use heuristics (FastDOC) or not.
   */
  public DOC(double alpha, double beta, double w, boolean heuristics, int d_zero) {
    this.alpha = alpha;
    this.beta = beta;
    this.w = w;
    this.heuristics = heuristics;
    this.d_zero = d_zero;
  }

  // ---------------------------------------------------------------------- //
  // Run methods.
  // ---------------------------------------------------------------------- //

  /**
   * Performs the DOC or FastDOC (as configured) algorithm on the given
   * Database.
   * 
   * <p>
   * This will run exhaustively, i.e. run DOC until no clusters are found
   * anymore / the database size has shrunk below the threshold for minimum
   * cluster size.
   * </p>
   */
  public Clustering<SubspaceModel<V>> run(Database database, Relation<V> relation) {

    // Dimensionality of our set.
    d = DatabaseUtil.dimensionality(relation);

    // Get available DBIDs as a set we can remove items from.
    S = DBIDUtil.newArray(relation.getDBIDs());

    // Precompute values as described in Figure 2.
    r = Math.abs(Math.log10(d + d) / Math.log10(beta / 2));
    // Outer loop count.
    n = (int) (2 / alpha);
    // Inner loop count.
    m = (int) (Math.pow(2 / alpha, r) * Math.log(4));
    if(heuristics) {
      m = Math.min(m, Math.min(1000000, d * d));
    }

    // Minimum size for a cluster for it to be accepted.
    minClusterSize = (int) (alpha * S.size());

    // List of all clusters we found.
    Clustering<SubspaceModel<V>> result = new Clustering<SubspaceModel<V>>("DOC Clusters", "DOC");

    // Inform the user about the number of actual clusters found so far.
    IndefiniteProgress cprogress = logger.isVerbose() ? new IndefiniteProgress("Number of clusters found so far", logger) : null;

    // To not only find a single cluster, we continue running until our set
    // of points is empty.
    while(S.size() > minClusterSize) {

      Cluster<SubspaceModel<V>> C;
      if(heuristics) {
        C = runFastDOC(relation);
      }
      else {
        C = runDOC(relation);
      }

      if(C == null) {
        // Stop trying if we couldn't find a cluster.
        // TODO not explicitly mentioned in the paper!
        break;
      }
      else {
        // Found a cluster, remember it, remove its points from the set.
        result.addCluster(C);

        if(cprogress != null) {
          cprogress.setProcessed(result.getAllClusters().size(), logger);
        }

        // Remove all points of the cluster from the set and continue.
        S.removeDBIDs(C.getIDs());
      }
    }

    if(cprogress != null) {
      cprogress.setCompleted(logger);
    }

    return result;
  }

  /**
   * Performs a single run of DOC, finding a single cluster.
   * 
   * @param relation used to get actual values for DBIDs.
   * @return a cluster, if one is found, else <code>null</code>.
   */
  private Cluster<SubspaceModel<V>> runDOC(Relation<V> relation) {

    // Best cluster for the current run.
    ArrayModifiableDBIDs C = null;
    // Relevant attributes for the best cluster.
    BitSet D = null;
    // Quality of the best cluster.
    double quality = Double.NEGATIVE_INFINITY;

    // Inform the user about the progress in the current iteration.
    FiniteProgress iprogress = logger.isVerbose() ? new FiniteProgress("Iteration progress for current cluster", m * n, logger) : null;

    for(int i = 0; i < n; ++i) {
      // Pick a random seed point.
      DBID p = S.get(random.nextInt(S.size()));
      V pV = relation.get(p);

      for(int j = 0; j < m; ++j) {
        // Choose a set of random points.
        DBIDs randomSet = DBIDUtil.randomSample(S, Math.min(S.size(), (int) r), random.nextLong());

        // Initialize cluster info.
        ArrayModifiableDBIDs nC = DBIDUtil.newArray();
        BitSet nD = new BitSet(d);

        // Test each dimension and build bounding box while we're at
        // it.
        double[] min = new double[d];
        double[] max = new double[d];
        for(int k = 0; k < d; ++k) {
          if(dimensionIsRelevant(k, relation, randomSet)) {
            nD.set(k);
            min[k] = pV.doubleValue(k + 1) - w;
            max[k] = pV.doubleValue(k + 1) + w;
          }
          else {
            min[k] = Double.NEGATIVE_INFINITY;
            max[k] = Double.POSITIVE_INFINITY;
          }
        }

        // Bounds for our cluster.
        HyperBoundingBox bounds = new HyperBoundingBox(min, max);

        // Get all points in the box.
        // TODO nicer way to do this?
        for(DBIDIter iter = S.iter(); iter.valid(); iter.advance()) {
          if(isPointInBounds(relation.get(iter), bounds)) {
            nC.add(iter);
          }
        }

        if(logger.isDebuggingFiner()) {
          logger.finer("Found a cluster, |C| = " + nC.size() + ", |D| = " + nD.cardinality());
        }

        // Is the cluster large enough?
        if(nC.size() < minClusterSize) {
          // Too small.
          if(logger.isDebuggingFiner()) {
            logger.finer("... but it's too small.");
          }
        }
        else {
          // TODO not explicitly mentioned in the paper!
          if(nD.cardinality() == 0) {
            if(logger.isDebuggingFiner()) {
              logger.finer("... but it has no relevant attributes.");
            }
          }
          else {
            // Better cluster than before?
            double nQuality = computeClusterQuality(nC.size(), nD.cardinality());
            if(nQuality > quality) {
              if(logger.isDebuggingFiner()) {
                logger.finer("... and it's the best so far: " + nQuality + " vs. " + quality);
              }

              C = nC;
              D = nD;
              quality = nQuality;
            }
            else {
              if(logger.isDebuggingFiner()) {
                logger.finer("... but we already have a better one.");
              }
            }
          }
        }

        if(iprogress != null) {
          iprogress.incrementProcessed(logger);
        }
      }
    }

    if(iprogress != null) {
      iprogress.ensureCompleted(logger);
    }

    if(C != null) {
      return makeCluster(relation, C, D);
    }
    else {
      return null;
    }
  }

  /**
   * Performs a single run of FastDOC, finding a single cluster.
   * 
   * @param relation used to get actual values for DBIDs.
   * @return a cluster, if one is found, else <code>null</code>.
   */
  private Cluster<SubspaceModel<V>> runFastDOC(Relation<V> relation) {
    // Relevant attributes of highest cardinality.
    BitSet D = null;
    // The seed point for the best dimensions.
    V dV = null;

    // Inform the user about the progress in the current iteration.
    FiniteProgress iprogress = logger.isVerbose() ? new FiniteProgress("Iteration progress for current cluster", m * n, logger) : null;

    outer: for(int i = 0; i < n; ++i) {
      // Pick a random seed point.
      DBID p = S.get(random.nextInt(S.size()));
      V pV = relation.get(p);

      for(int j = 0; j < m; ++j) {
        // Choose a set of random points.
        DBIDs randomSet = DBIDUtil.randomSample(S, Math.min(S.size(), (int) r), random.nextLong());

        // Initialize cluster info.
        BitSet nD = new BitSet(d);

        // Test each dimension and build bounding box while we're at it.
        for(int k = 0; k < d; ++k) {
          if(dimensionIsRelevant(k, relation, randomSet)) {
            nD.set(k);
          }
        }

        if(D == null || nD.cardinality() > D.cardinality()) {
          D = nD;
          dV = pV;

          if(D.cardinality() >= d_zero) {
            iprogress.setProcessed(iprogress.getTotal(), logger);
            break outer;
          }
        }

        if(iprogress != null) {
          iprogress.incrementProcessed(logger);
        }
      }
    }

    if(iprogress != null) {
      iprogress.ensureCompleted(logger);
    }

    // If no relevant dimensions were found, skip it.
    if(D == null || D.cardinality() == 0) {
      return null;
    }

    // Bounds for our cluster.
    double[] min = new double[d];
    double[] max = new double[d];
    for(int k = 0; k < d; ++k) {
      if(D.get(k)) {
        min[k] = dV.doubleValue(k + 1) - w;
        max[k] = dV.doubleValue(k + 1) + w;
      }
      else {
        min[k] = Double.NEGATIVE_INFINITY;
        max[k] = Double.POSITIVE_INFINITY;
      }
    }
    HyperBoundingBox bounds = new HyperBoundingBox(min, max);

    // Accumulate points inside the bounds.
    ArrayModifiableDBIDs C = DBIDUtil.newArray();

    // Get all points in the box.
    // TODO nicer way to do this?
    for(DBIDIter iter = S.iter(); iter.valid(); iter.advance()) {
      if(isPointInBounds(relation.get(iter), bounds)) {
        C.add(iter);
      }
    }

    // If we have a non-empty cluster, return it.
    if(C.size() > 0) {
      return makeCluster(relation, C, D);
    }
    else {
      return null;
    }
  }

  // ---------------------------------------------------------------------- //
  // Utility methods
  // ---------------------------------------------------------------------- //

  /**
   * Utility method to test if a given dimension is relevant as determined via a
   * set of reference points (i.e. if the variance along the attribute is lower
   * than the threshold).
   * 
   * @param dimension the dimension to test.
   * @param relation used to get actual values for DBIDs.
   * @param points the points to test.
   * @return <code>true</code> if the dimension is relevant.
   */
  private boolean dimensionIsRelevant(int dimension, Relation<V> relation, DBIDs points) {
    double min = Double.POSITIVE_INFINITY;
    double max = Double.NEGATIVE_INFINITY;
    for(DBIDIter iter = points.iter(); iter.valid(); iter.advance()) {
      V xV = relation.get(iter);
      min = Math.min(min, xV.doubleValue(dimension + 1));
      max = Math.max(max, xV.doubleValue(dimension + 1));
      if(max - min > w) {
        return false;
      }
    }
    return true;
  }

  /**
   * Utility method to test if a point is in a given hypercube.
   * 
   * @param v the point to test for.
   * @param bounds the hypercube to use as the bounds.
   * 
   * @return <code>true</code> if the point is inside the cube.
   */
  private boolean isPointInBounds(V v, HyperBoundingBox bounds) {
    for(int i = 1; i <= v.getDimensionality(); ++i) {
      if(v.doubleValue(i) < bounds.getMin(i) || v.doubleValue(i) > bounds.getMax(i)) {
        return false;
      }
    }
    return true;
  }

  /**
   * Utility method to create a subspace cluster from a list of DBIDs and the
   * relevant attributes.
   * 
   * @param relation to compute a centroid.
   * @param C the cluster points.
   * @param D the relevant dimensions.
   * @return an object representing the subspace cluster.
   */
  private Cluster<SubspaceModel<V>> makeCluster(Relation<V> relation, DBIDs C, BitSet D) {
    ArrayModifiableDBIDs ids = DBIDUtil.newArray(C.size());
    ids.addDBIDs(C);
    Cluster<SubspaceModel<V>> cluster = new Cluster<SubspaceModel<V>>(ids);
    cluster.setModel(new SubspaceModel<V>(new Subspace<V>(D), Centroid.make(relation, ids).toVector(relation)));
    return cluster;
  }

  /**
   * Computes the quality of a cluster based on its size and number of relevant
   * attributes, as described via the &mu;-function from the paper.
   * 
   * @param clusterSize the size of the cluster.
   * @param numRelevantDimensions the number of dimensions relevant to the
   *        cluster.
   * @return a quality measure (only use this to compare the quality to that
   *         other clusters).
   */
  private double computeClusterQuality(int clusterSize, int numRelevantDimensions) {
    return clusterSize * Math.pow(1 / beta, numRelevantDimensions);
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
  public static class Parameterizer<V extends NumberVector<V, ?>> extends AbstractParameterizer {

    protected double alpha;

    protected double beta;

    protected double w;

    protected boolean heuristics;

    protected int d_zero;

    @Override
    protected void makeOptions(Parameterization config) {
      super.makeOptions(config);

      {
        Vector<ParameterConstraint<Number>> constraints = new Vector<ParameterConstraint<Number>>();
        constraints.add(new GreaterEqualConstraint(0));
        constraints.add(new LessEqualConstraint(1));
        DoubleParameter param = new DoubleParameter(ALPHA_ID, constraints, 0.2);
        if(config.grab(param)) {
          alpha = param.getValue();
        }
      }

      {
        Vector<ParameterConstraint<Number>> constraints = new Vector<ParameterConstraint<Number>>();
        constraints.add(new GreaterConstraint(0));
        constraints.add(new LessConstraint(1));
        DoubleParameter param = new DoubleParameter(BETA_ID, constraints, 0.8);
        if(config.grab(param)) {
          beta = param.getValue();
        }
      }

      {
        DoubleParameter param = new DoubleParameter(W_ID, new GreaterEqualConstraint(0), 0.05);
        if(config.grab(param)) {
          w = param.getValue();
        }
      }

      {
        Flag param = new Flag(HEURISTICS_ID);
        if(config.grab(param)) {
          heuristics = param.getValue();
        }
      }

      if(heuristics) {
        IntParameter param = new IntParameter(D_ZERO_ID, new GreaterConstraint(0), 5);
        if(config.grab(param)) {
          d_zero = param.getValue();
        }
      }

    }

    @Override
    protected DOC<V> makeInstance() {
      return new DOC<V>(alpha, beta, w, heuristics, d_zero);
    }
  }
}
