package de.lmu.ifi.dbs.elki.algorithm.clustering.correlation;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.logging.LogRecord;

import de.lmu.ifi.dbs.elki.algorithm.clustering.subspace.ProjectedClustering;
import de.lmu.ifi.dbs.elki.data.Clustering;
import de.lmu.ifi.dbs.elki.data.DatabaseObjectGroup;
import de.lmu.ifi.dbs.elki.data.DatabaseObjectGroupCollection;
import de.lmu.ifi.dbs.elki.data.RealVector;
import de.lmu.ifi.dbs.elki.data.cluster.Cluster;
import de.lmu.ifi.dbs.elki.data.model.ClusterModel;
import de.lmu.ifi.dbs.elki.data.model.Model;
import de.lmu.ifi.dbs.elki.database.Database;
import de.lmu.ifi.dbs.elki.database.DistanceResultPair;
import de.lmu.ifi.dbs.elki.distance.DoubleDistance;
import de.lmu.ifi.dbs.elki.logging.LogLevel;
import de.lmu.ifi.dbs.elki.math.linearalgebra.Matrix;
import de.lmu.ifi.dbs.elki.math.linearalgebra.SortedEigenPairs;
import de.lmu.ifi.dbs.elki.math.linearalgebra.pca.PCAResult;
import de.lmu.ifi.dbs.elki.math.linearalgebra.pca.PCARunner;
import de.lmu.ifi.dbs.elki.utilities.DatabaseUtil;
import de.lmu.ifi.dbs.elki.utilities.Description;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.DoubleParameter;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.OptionID;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.ParameterException;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.constraints.IntervalConstraint;

/**
 * ORCLUS provides the ORCLUS algorithm, an algorithm to find clusters in high
 * dimensional spaces.
 * <p>
 * Reference: C. C. Aggrawal, P. S. Yu:
 * Finding Generalized Projected Clusters in High Dimensional Spaces.
 * <br/>In: Proc. ACM SIGMOD Int. Conf. on Management of Data (SIGMOD '00).
 * </p>
 * 
 * @author Elke Achtert
 * @param <V> the type of Realvector handled by this Algorithm
 */
public class ORCLUS<V extends RealVector<V, ?>> extends ProjectedClustering<V> {
  /**
   * OptionID for {@link #ALPHA_PARAM}.
   */
  public static final OptionID ALPHA_ID = OptionID.getOrCreateOptionID("orclus.alpha", "The factor for reducing the number of current clusters in each iteration.");

  /**
   * Parameter to specify the factor for reducing the number of current clusters
   * in each iteration, must be an integer greater than 0 and less than 1.
   * <p>
   * Default value: {@code 0.5}
   * </p>
   * <p>
   * Key: {@code -orclus.alpha}
   * </p>
   */
  private final DoubleParameter ALPHA_PARAM = new DoubleParameter(ALPHA_ID, new IntervalConstraint(0, IntervalConstraint.IntervalBoundary.OPEN, 1, IntervalConstraint.IntervalBoundary.CLOSE), 0.5);

  /**
   * Holds the value of {@link #ALPHA_PARAM}.
   */
  private double alpha;

  /**
   * The PCA utility object.
   */
  private PCARunner<V> pca;

  /**
   * Provides the ORCLUS algorithm, adding parameter {@link #ALPHA_PARAM} to the
   * option handler additionally to parameters of super class.
   */
  public ORCLUS() {
    super();
    // parameter alpha
    addOption(ALPHA_PARAM);
  }

  /**
   * Performs the ORCLUS algorithm on the given database.
   */
  @Override
  protected Clustering<Model> runInTime(Database<V> database) throws IllegalStateException {

    try {
      final int dim = getL();
      final int k = getK();
      final int k_i = getK_i();

      if(database.dimensionality() < dim)
        throw new IllegalStateException("Dimensionality of data < parameter l! " + "(" + database.dimensionality() + " < " + dim + ")");

      // current number of seeds
      int k_c = Math.min(database.size(), k_i * k);

      // current dimensionality associated with each seed
      int dim_c = database.dimensionality();

      // pick k0 > k points from the database
      List<ORCLUSCluster> clusters = initialSeeds(database, k_c);

      double beta = StrictMath.exp(-StrictMath.log((double) dim_c / (double) dim) * StrictMath.log(1 / alpha) / StrictMath.log((double) k_c / (double) k));

      while(k_c > k) {
        if(isVerbose()) {
          progress(new LogRecord(LogLevel.PROGRESS, "\rCurrent number of clusters: " + clusters.size() + ".                           "));
        }

        // find partitioning induced by the seeds of the clusters
        assign(database, clusters);

        // determine current subspace associated with each cluster
        for(ORCLUSCluster cluster : clusters) {
          if(cluster.objectIDs.size() > 0)
            cluster.basis = findBasis(database, cluster, dim_c);
        }

        // reduce number of seeds and dimensionality associated with
        // each seed
        k_c = (int) Math.max(k, k_c * alpha);
        dim_c = (int) Math.max(dim, dim_c * beta);
        merge(database, clusters, k_c, dim_c);
      }
      assign(database, clusters);

      if(isVerbose()) {
        verbose("\nNumber of clusters: " + clusters.size() + ".                           ");
      }

      // get the result
      Clustering<Model> r = new Clustering<Model>();
      for(ORCLUSCluster c : clusters) {
        DatabaseObjectGroup group = new DatabaseObjectGroupCollection<List<Integer>>(c.objectIDs);
        r.addCluster(new Cluster<Model>(group, ClusterModel.CLUSTER));
      }
      setResult(r);
    }
    catch(Exception e) {
      throw new IllegalStateException(e);
    }
    return getResult();
  }

  public Description getDescription() {
    return new Description("ORCLUS", "Arbitrarily ORiented projected CLUSter generation", "Algorithm to find clusters in high dimensional spaces.", "C. C. Aggrawal, P. S. Yu: " + "Finding Generalized Projected Clusters in High Dimensional Spaces. " + "In: Proc. ACM SIGMOD Int. Conf. on Management of Data (SIGMOD '00).");
  }

  /**
   * Calls the super method
   * and sets additionally the value of the parameter {@link #ALPHA_PARAM}.
   */
  @Override
  public String[] setParameters(String[] args) throws ParameterException {
    String[] remainingParameters = super.setParameters(args);

    // alpha
    alpha = ALPHA_PARAM.getValue();

    // pca
    remainingParameters = pca.setParameters(remainingParameters);

    setParameters(args, remainingParameters);
    return remainingParameters;
  }

  /**
   * Initializes the list of seeds wit a random sample of size k.
   * 
   * @param database the database holding the objects
   * @param k the size of the random sample
   * @return the initial seed list
   */
  private List<ORCLUSCluster> initialSeeds(Database<V> database, int k) {
    Set<Integer> randomSample = database.randomSample(k, 1);

    List<ORCLUSCluster> seeds = new ArrayList<ORCLUSCluster>();
    for(Integer id : randomSample) {
      seeds.add(new ORCLUSCluster(database.get(id)));
    }
    return seeds;
  }

  /**
   * Creates a partitioning of the database by assigning each object to its
   * closest seed.
   * 
   * @param database the database holding the objects
   * @param clusters the array of clusters to which the objects should be
   *        assigned to
   */
  private void assign(Database<V> database, List<ORCLUSCluster> clusters) {
    // clear the current clusters
    for(ORCLUSCluster cluster : clusters) {
      cluster.objectIDs.clear();
    }

    // projected centroids of the clusters
    List<V> projectedCentroids = new ArrayList<V>(clusters.size());
    for(ORCLUSCluster c : clusters) {
      projectedCentroids.add(projection(c, c.centroid));
    }

    // for each data point o do
    Iterator<Integer> it = database.iterator();
    while(it.hasNext()) {
      Integer id = it.next();
      V o = database.get(id);

      DoubleDistance minDist = null;
      ORCLUSCluster minCluster = null;

      // determine projected distance between o and cluster
      for(int i = 0; i < clusters.size(); i++) {
        ORCLUSCluster c = clusters.get(i);
        V o_proj = projection(c, o);
        DoubleDistance dist = getDistanceFunction().distance(o_proj, projectedCentroids.get(i));
        if(minDist == null || minDist.compareTo(dist) > 0) {
          minDist = dist;
          minCluster = c;
        }
      }
      // add p to the cluster with the least value of projected distance
      assert minCluster != null;
      minCluster.objectIDs.add(id);
    }

    // recompute the seed in each clusters
    for(ORCLUSCluster cluster : clusters) {
      if(cluster.objectIDs.size() > 0) {
        cluster.centroid = DatabaseUtil.centroid(database, cluster.objectIDs);
      }
    }
  }

  /**
   * Finds the basis of the subspace of dimensionality <code>dim</code> for
   * the specified cluster.
   * 
   * @param database the database to run the algorithm on
   * @param cluster the cluster
   * @param dim the dimensionality of the subspace
   * @return matrix defining the basis of the subspace for the specified cluster
   */
  private Matrix findBasis(Database<V> database, ORCLUSCluster cluster, int dim) {
    // covariance matrix of cluster
    // Matrix covariance = Util.covarianceMatrix(database, cluster.objectIDs);
    List<DistanceResultPair<DoubleDistance>> results = new ArrayList<DistanceResultPair<DoubleDistance>>(cluster.objectIDs.size());
    for(Iterator<Integer> it = cluster.objectIDs.iterator(); it.hasNext();) {
      Integer id = it.next();
      DoubleDistance distance = getDistanceFunction().distance(cluster.centroid, database.get(id));
      DistanceResultPair<DoubleDistance> qr = new DistanceResultPair<DoubleDistance>(distance, id);
      results.add(qr);
    }
    Collections.sort(results);
    PCAResult pcares = pca.processQueryResult(results, database);
    SortedEigenPairs eigenPairs = pcares.getEigenPairs();
    return eigenPairs.reverseEigenVectors(dim);

    // Used to be just this:
    
    // Matrix pcaMatrix = pca.pcaMatrixResults(database, results);
    // pca.determineEigenPairs(pcaMatrix);

    // eigenvectors in ascending order
    // EigenvalueDecomposition evd = covariance.eig();
    // SortedEigenPairs eigenPairs = new SortedEigenPairs(evd, true);

    // eigenvectors corresponding to the smallest dim eigenvalues
    // return eigenPairs.eigenVectors(dim);
  }

  /**
   * Reduces the number of seeds to k_new
   * 
   * @param database the database holding the objects
   * @param clusters the set of current seeds
   * @param k_new the new number of seeds
   * @param d_new the new dimensionality of the subspaces for each seed
   */
  private void merge(Database<V> database, List<ORCLUSCluster> clusters, int k_new, int d_new) {
    ArrayList<ProjectedEnergy> projectedEnergies = new ArrayList<ProjectedEnergy>();
    for(int i = 0; i < clusters.size(); i++) {
      for(int j = 0; j < clusters.size(); j++) {
        if(i >= j) {
          continue;
        }
        // projected energy of c_ij in subspace e_ij
        ORCLUSCluster c_i = clusters.get(i);
        ORCLUSCluster c_j = clusters.get(j);

        ProjectedEnergy pe = projectedEnergy(database, c_i, c_j, i, j, d_new);
        projectedEnergies.add(pe);
      }
    }

    while(clusters.size() > k_new) {
      if(isVerbose()) {
        progress(new LogRecord(LogLevel.PROGRESS, "\rCurrent number of clusters: " + clusters.size() + ".                           "));
      }
      // find the smallest value of r_ij
      ProjectedEnergy minPE = Collections.min(projectedEnergies);

      // renumber the clusters by replacing cluster c_i with cluster c_ij
      // and discarding cluster c_j
      for(int c = 0; c < clusters.size(); c++) {
        if(c == minPE.i) {
          clusters.remove(c);
          clusters.add(c, minPE.cluster);
        }
        if(c == minPE.j) {
          clusters.remove(c);
        }
      }

      // remove obsolete projected energies and renumber the others ...
      int i = minPE.i;
      int j = minPE.j;
      Iterator<ProjectedEnergy> it = projectedEnergies.iterator();
      while(it.hasNext()) {
        ProjectedEnergy pe = it.next();
        if(pe.i == i || pe.i == j || pe.j == i || pe.j == j) {
          it.remove();
        }
        else {
          if(pe.i > j) {
            pe.i -= 1;
          }
          if(pe.j > j) {
            pe.j -= 1;
          }
        }
      }

      // ... and recompute them
      ORCLUSCluster c_ij = minPE.cluster;
      for(int c = 0; c < clusters.size(); c++) {
        if(c < i) {
          projectedEnergies.add(projectedEnergy(database, clusters.get(c), c_ij, c, i, d_new));
        }
        else if(c > i) {
          projectedEnergies.add(projectedEnergy(database, clusters.get(c), c_ij, i, c, d_new));
        }
      }
    }
  }

  /**
   * Computes the projected energy of the specified clusters. The projected
   * energy is given by the mean square distance of the points to the centroid
   * of the union cluster c, when all points in c are projected to the subspace
   * of c.
   * 
   * @param database the database holding the objects
   * @param c_i the first cluster
   * @param c_j the second cluster
   * @param i the index of cluster c_i in the cluster list
   * @param j the index of cluster c_j in the cluster list
   * @param dim the dimensionality of the clusters
   * @return the projected energy of the specified cluster
   */
  private ProjectedEnergy projectedEnergy(Database<V> database, ORCLUSCluster c_i, ORCLUSCluster c_j, int i, int j, int dim) {
    // union of cluster c_i and c_j
    ORCLUSCluster c_ij = union(database, c_i, c_j, dim);

    DoubleDistance sum = getDistanceFunction().nullDistance();
    V c_proj = projection(c_ij, c_ij.centroid);
    for(Integer id : c_ij.objectIDs) {
      V o = database.get(id);
      V o_proj = projection(c_ij, o);
      DoubleDistance dist = getDistanceFunction().distance(o_proj, c_proj);
      sum = sum.plus(dist.times(dist));
    }
    DoubleDistance projectedEnergy = sum.times(1.0 / c_ij.objectIDs.size());

    return new ProjectedEnergy(i, j, c_ij, projectedEnergy);
  }

  /**
   * Returns the union of the two specified clusters.
   * 
   * @param database the database holding the objects
   * @param c1 the first cluster
   * @param c2 the second cluster
   * @param dim the dimensionality of the union cluster
   * @return the union of the two specified clusters
   */
  private ORCLUSCluster union(Database<V> database, ORCLUSCluster c1, ORCLUSCluster c2, int dim) {
    ORCLUSCluster c = new ORCLUSCluster();

    HashSet<Integer> ids = new HashSet<Integer>(c1.objectIDs);
    ids.addAll(c2.objectIDs);

    c.objectIDs = new ArrayList<Integer>(ids);

    if(c.objectIDs.size() > 0) {
      c.centroid = DatabaseUtil.centroid(database, c.objectIDs);
      c.basis = findBasis(database, c, dim);
    }
    else {
      c.centroid = c1.centroid.plus(c2.centroid).multiplicate(0.5);
      double[][] doubles = new double[c1.basis.getRowDimensionality()][dim];
      for(int i = 0; i < dim; i++) {
        doubles[i][i] = 1;
      }
      c.basis = new Matrix(doubles);
    }

    return c;
  }

  /**
   * Returns the projection of real vector o in the subspace of cluster c.
   * 
   * @param c the cluster
   * @param o the double vector
   * @return the projection of double vector o in the subspace of cluster c
   */
  private V projection(ORCLUSCluster c, V o) {
    Matrix o_proj = o.getRowVector().times(c.basis);
    double[] values = o_proj.getColumnPackedCopy();
    return o.newInstance(values);
  }

  /**
   * Encapsulates the attributes of a cluster.
   */
  private final class ORCLUSCluster {
    // TODO: reuse/derive from existing cluster classes?
    /**
     * The ids of the objects belonging to this cluster.
     */
    List<Integer> objectIDs = new ArrayList<Integer>();

    /**
     * The matrix defining the subspace of this cluster.
     */
    Matrix basis;

    /**
     * The centroid of this cluster.
     */
    V centroid;

    /**
     * Creates a new empty cluster.
     */
    ORCLUSCluster() {
      // creates a new empty cluster
    }

    /**
     * Creates a new cluster containing the specified object o.
     * 
     * @param o the object belonging to this cluster.
     */
    ORCLUSCluster(V o) {
      this.objectIDs.add(o.getID());

      // initially the basis ist the original axis-system
      int dim = o.getDimensionality();
      double[][] doubles = new double[dim][dim];
      for(int i = 0; i < dim; i++) {
        doubles[i][i] = 1;
      }
      this.basis = new Matrix(doubles);

      // initially the centroid is the value array of o
      double[] values = new double[o.getDimensionality()];
      for(int d = 1; d <= o.getDimensionality(); d++)
        values[d - 1] = o.getValue(d).doubleValue();
      this.centroid = o.newInstance(values);
    }
  }

  /**
   * Encapsulates the projected energy for a cluster.
   */
  private final class ProjectedEnergy implements Comparable<ProjectedEnergy> {
    int i;

    int j;

    ORCLUSCluster cluster;

    DoubleDistance projectedEnergy;

    ProjectedEnergy(int i, int j, ORCLUSCluster cluster, DoubleDistance projectedEnergy) {
      this.i = i;
      this.j = j;
      this.cluster = cluster;
      this.projectedEnergy = projectedEnergy;
    }

    /**
     * Compares this object with the specified object for order.
     * 
     * @param o the Object to be compared.
     * @return a negative integer, zero, or a positive integer as this object is
     *         less than, equal to, or greater than the specified object.
     */
    public int compareTo(ProjectedEnergy o) {
      return this.projectedEnergy.compareTo(o.projectedEnergy);
    }
  }
}
