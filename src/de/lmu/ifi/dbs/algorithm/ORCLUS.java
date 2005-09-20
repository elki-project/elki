package de.lmu.ifi.dbs.algorithm;

import de.lmu.ifi.dbs.algorithm.result.Result;
import de.lmu.ifi.dbs.data.DoubleVector;
import de.lmu.ifi.dbs.database.Database;
import de.lmu.ifi.dbs.distance.Distance;
import de.lmu.ifi.dbs.linearalgebra.Matrix;
import de.lmu.ifi.dbs.linearalgebra.EigenvalueDecomposition;
import de.lmu.ifi.dbs.linearalgebra.SortedEigenPairs;
import de.lmu.ifi.dbs.utilities.Description;
import de.lmu.ifi.dbs.utilities.Progress;
import de.lmu.ifi.dbs.utilities.Util;
import de.lmu.ifi.dbs.utilities.optionhandling.OptionHandler;
import de.lmu.ifi.dbs.utilities.optionhandling.UnusedParameterException;

import java.util.*;

/**
 * ORCLUS provides the ORCLUS algorithm.
 *
 * @author Elke Achtert (<a href="mailto:achtert@dbs.ifi.lmu.de">achtert@dbs.ifi.lmu.de</a>)
 */

public class ORCLUS extends DistanceBasedAlgorithm<DoubleVector> {
  /**
   * Parameter k.
   */
  public static final String K_P = "k";

  /**
   * Description for parameter k.
   */
  public static final String K_D = "<integer> value to specify the number of clusters to be found";

  /**
   * Parameter l.
   */
  public static final String DIM_P = "dim";

  /**
   * Description for parameter l.
   */
  public static final String DIM_D = "<integer> value to specify the dimensionality of the clusters to be found";

  /**
   * Parameter name for alpha - factor for reducing the number of current clusters in each iteration.
   */
  public static final String ALPHA_P = "alpha";

  /**
   * Default value for alpha.
   */
  public static final double ALPHA_DEFAULT = 0.50;

  /**
   * Description for parameter alpha - factor for reducing the number of current clusters in each iteration
   */
  public static final String ALPHA_D = "<double>factor for reducing the number of current clusters in each " +
                                       "iteration (0..1) - default: " + ALPHA_DEFAULT;

  /**
   * Number of clusters.
   */
  private int k;

  /**
   * Dimensionality of the clusters.
   */
  private int dim;

  /**
   * Holds alpha.
   */
  protected double alpha;

  /**
   * Sets the parameter k and l the optionhandler additionally to the
   * parameters provided by super-classes. Since ORCLUS is a non-abstract
   * class, finally optionHandler is initialized.
   */
  public ORCLUS() {
    super();
    parameterToDescription.put(K_P + OptionHandler.EXPECTS_VALUE, K_D);
    parameterToDescription.put(DIM_P + OptionHandler.EXPECTS_VALUE, DIM_D);
    parameterToDescription.put(ALPHA_P + OptionHandler.EXPECTS_VALUE, ALPHA_D);
    optionHandler = new OptionHandler(parameterToDescription, getClass().getName());
  }

  /**
   * @see de.lmu.ifi.dbs.algorithm.Algorithm#run(de.lmu.ifi.dbs.database.Database)
   */
  public void run(Database<DoubleVector> database) throws IllegalStateException {
    long start = System.currentTimeMillis();
    try {
      Progress progress = new Progress(database.size());

      if (database.dimensionality() < dim)
        throw new IllegalStateException("Dimensionality of data < parameter l! " +
                                        "(" + database.dimensionality() + " < " + dim + ")");

      // current number of seeds
      // todo
      int k_c = Math.min(database.size(), 15 * k);

      // current dimensionality associated with each seed
      int dim_c = database.dimensionality();

      // pick k0 > k points from the db
      List<Cluster> clusters = initialSeeds(database, k_c);

      double beta = Math.exp(-Math.log((double) dim_c / (double) dim) * Math.log(1 / alpha) /
                             Math.log((double) k_c / (double) k));

      while (k_c > k) {
        // find partitioning induced by the seeds of the clusters
        assign(database, clusters);

        // determine current subspace associated eith each cluster
        for (Cluster cluster : clusters) {
          cluster.basis = findBasis(database, cluster, dim_c);
        }

        // reduce number of seeds and dimensionality associated with each seed
        k_c = (int) Math.max(k, k_c * alpha);
        dim_c = (int) Math.max(dim, dim_c * beta);
//        clusters = merge(clusters, k_c, dim_c);
      }

      assign(database, clusters);
//      print(clusters);
    }
    catch (Exception e) {
      throw new IllegalStateException(e);
    }
    long end = System.currentTimeMillis();

    if (isTime()) {
      long elapsedTime = end - start;
      System.out.println(this.getClass().getName() + " runtime: " + elapsedTime + " milliseconds.");
    }
  }

  /**
   * @see Algorithm#getDescription()
   */
  public Description getDescription() {
    return new Description("ORCLUS", "Finding Generalized Projected Clusters in High Dimensional Spaces",
                           "Algorithm to find clusters in high dimensional spaces.",
                           "C. C. Aggrawal, P. S. Yu: " + "Finding Generalized Projected Clusters in High Dimensional Spaces " +
                           "In: Proc. ACM SIGMOD Int. Conf. on Management of Data (SIGMOD '00)");
  }

  /**
   * Sets the parameters k and l additionally to the parameters set
   * by the super-class' method. Both k and l are required
   * parameters.
   *
   * @see de.lmu.ifi.dbs.utilities.optionhandling.Parameterizable#setParameters(String[])
   */
  public String[] setParameters(String[] args) throws IllegalArgumentException {
    String[] remainingParameters = super.setParameters(args);
    try {
      k = Integer.parseInt(optionHandler.getOptionValue(K_P));
      dim = Integer.parseInt(optionHandler.getOptionValue(DIM_P));

      if (optionHandler.isSet(ALPHA_P)) {
        alpha = Double.parseDouble(optionHandler.getOptionValue(ALPHA_P));
      }
      else
        alpha = ALPHA_DEFAULT;
    }
    catch (UnusedParameterException e) {
      throw new IllegalArgumentException(e);
    }
    catch (NumberFormatException e) {
      throw new IllegalArgumentException(e);
    }
    return remainingParameters;
  }

  /**
   * @see Algorithm#getResult()
   *      todo
   */
  public Result getResult() {
    return null;
  }

  /**
   * Initializes the list of seeds wit a random sample of size k.
   *
   * @param database the database holding the objects
   * @param k        the size of the random sample
   * @return the initial seed list
   */
  private List<Cluster> initialSeeds(Database<DoubleVector> database, int k) {
    List<Integer> randomSample = database.randomSample(k, 1);

    List<Cluster> seeds = new ArrayList<Cluster>();
    for (Integer id : randomSample) {
      seeds.add(new Cluster(database.get(id)));
    }
    return seeds;
  }

  /**
   * Creates a partitioning of the database by assigning each object to its closest seed.
   *
   * @param database the database holding the objects
   * @param clusters the array of clusters to which the objects should be assigned to
   */
  private void assign(Database<DoubleVector> database, List<Cluster> clusters) {
    // clear the current clusters
    for (Cluster cluster : clusters) {
      cluster.ids.clear();
    }

    // for each data point o do
    Iterator<Integer> it = database.iterator();
    while (it.hasNext()) {
      Integer id = it.next();
      DoubleVector o = database.get(id);

      Distance minDist = null;
      Cluster minCluster = null;

      // determine projected distance between o and cluster
      for (Cluster cluster : clusters) {
        Distance dist = projectedDistance(cluster, o);
        if (minDist == null || minDist.compareTo(dist) > 0) {
          minDist = dist;
          minCluster = cluster;
        }
      }
      // add p to the cluster with the least value of projected distance
      assert minCluster != null;
      minCluster.ids.add(id);
    }

    // recompute the seed in each clusters
    for (Cluster cluster : clusters) {
      cluster.centroid = Util.centroid(database, cluster.ids);
    }
  }

  /**
   * Finds the basis of the subspace of dimensionality <code> for the specified cluster.
   *
   * @param cluster the cluster
   * @param dim     the dimensionality of the subspace
   * @return matrix defining the basis of the subspace for the specified cluster
   */
  private Matrix findBasis(Database<DoubleVector> database, Cluster cluster, int dim) {
    // covariance matrix of cluster
    Matrix covariance = Util.covarianceMatrix(database, cluster.ids);

    // eigenvectors in ascending order
    EigenvalueDecomposition evd = covariance.eig();
    SortedEigenPairs eigenPairs = new SortedEigenPairs(evd, true);

    // eigenvectors corresponding to the smallest dime eigenvalues
    return eigenPairs.eigenVectors(dim);
  }

  /**
   * Reduces the number of seeds to k_new
   *
   * @param seeds the set of current seeds
   * @param k_new the new number of seeds
   * @param l_new the new dimensionality of the subspaces for each seed
   * @return the new set of current seeds
   */
  private Cluster[] merge(Cluster[] seeds, int k_new, int l_new) {
    /*ArrayList projectedEnergies = new ArrayList();
    for (int i = 0; i < seeds.length; i++) {
      for (int j = 0; j < seeds.length; j++) {
        if (i >= j) continue;
        // projected energy of c_ij in subspace e_ij
        ProjectedEnergy r_ij = projectedEnergy(seeds[i], seeds[j], l_new);
        projectedEnergies.add(r_ij);
      }
    }

    while (seeds.length > k_new) {
      // find the smallest value of r_ij
      ProjectedEnergy r_ij = (ProjectedEnergy) Collections.min(projectedEnergies);
      // merge the corresponding cluster c_i and c_j and renumber the seeds
      seeds = renumber(seeds, r_ij);

      // remove obsolete projected energies ...
      Cluster s_i = r_ij.getSeed_i();
      Cluster s_j = r_ij.getSeed_j();
      Iterator it = projectedEnergies.iterator();
      while (it.hasNext()) {
        ProjectedEnergy r = (ProjectedEnergy) it.next();
        if (r.containsExclusive(s_i, s_j)) {
          it.remove();
        }
      }
      // ... and recompute them
      Cluster s_ij = r_ij.getSeed();
      for (int i = 0; i < seeds.length; i++) {
        if (seeds[i] != s_ij) {
          // projected energy of c_ij in subspace e_ij
          r_ij = projectedEnergy(seeds[i], s_ij, l_new);
          projectedEnergies.add(r_ij);
        }
      }
    }                 */
    return seeds;
  }

  /**
   * Determines the projected energy of a cluster by merging seed s_i with seed s_j
   *
   * @param s_i   seed s_i
   * @param s_j   seed s_j
   * @param l_new the dimensionality of the subspace of the cluster
   * @return the projected energy of a cluster by merging seed s_i with seed s_j
   */

 /* private ProjectedEnergy projectedEnergy(Cluster s_i, Cluster s_j, int l_new) {
    // union of cluster c_i and c_j
    Cluster c_ij = s_i.getCluster().union(s_j.getCluster());
    // eigenvectors of c_ij for l_new smallest eigenvalues
    Matrix e_ij = findVectors(c_ij, l_new);
    // projected energy of c_ij in subspace e_ij
    Cluster s_ij = new Cluster(c_ij, e_ij);
    return new ProjectedEnergy(s_ij, s_i, s_j);
  }  */

  /**
   * Renumbers the set of seeds by replacing seed s_i with seed s_ij and
   * discarding seed s_j
   *
   * @param seeds the set of current seeds
   * @param r_ij  the minimum projected energy
   * @return the new current set of seeds
   */

  /*
  private Cluster[] renumber(Cluster[] seeds, ProjectedEnergy r_ij) {
    Cluster s_ij = r_ij.getSeed();
    Cluster s_i = r_ij.getSeed_i();
    Cluster s_j = r_ij.getSeed_j();
    int i = 0;
    boolean switched = false;
    for (i = 0; i < seeds.length; i++) {
      // s[i] = s_ij
      if (seeds[i] == s_i || seeds[i] == s_j) {
        if (switched) break;
        seeds[i] = s_ij;
        switched = true;
      }
    }
    // discard s[j] and renumber the seeds by subtracting 1
    Cluster[] newSeeds = new Cluster[seeds.length - 1];
    // copy from 0..(i-1)
    System.arraycopy(seeds, 0, newSeeds, 0, i);
    // copy from (i+1)..
    if (i + 1 < seeds.length)
      System.arraycopy(seeds, i + 1, newSeeds, i, seeds.length - (i + 1));

    return newSeeds;
  }
  */

  /**
   * Returns the distance of double vector o to cluster c in the subspace of cluster c.
   *
   * @param c the cluster
   * @param o the double vector
   * @return the distance of double vector o to cluster c in the subspace of cluster c
   */
  private Distance projectedDistance(Cluster c, DoubleVector o) {
    DoubleVector o_proj = projection(c, o);
    DoubleVector seed_proj = projection(c, c.centroid);
    return ORCLUS.this.getDistanceFunction().distance(o_proj, seed_proj);
  }

  /**
   * Returns the projection of double vector o in the subspace of cluster c
   *
   * @param c the cluster
   * @param o the double vector
   * @return the projection of double vector o in the subspace of cluster c
   */
  private DoubleVector projection(Cluster c, DoubleVector o) {
    Matrix o_proj = o.getRowVector().times(c.basis);
    double[] values = o_proj.getColumnPackedCopy();
    return new DoubleVector(values);
  }

  class Value implements Comparable {
    double value;
    int dimension;

    Value(double value, int dimension) {
      this.value = value;
      this.dimension = dimension;
    }

    public int compareTo(Object o) {
      Value other = (Value) o;
      if (this.value < other.value) return -1;
      if (this.value > other.value) return 1;
      if (this.dimension < other.dimension) return -1;
      if (this.dimension > other.dimension) return 1;
      return 0;
    }


  }

  private class Cluster {
    /**
     * The dimensionality of this cluster.
     */
    private int dimensionality;

    /**
     * The ids of the objects belonging to this cluster.
     */
    private final List<Integer> ids = new ArrayList<Integer>();

    /**
     * The matrix defining the subspace of this cluster.
     */
    private Matrix basis;

    /**
     * The centroid of this cluster.
     */
    private DoubleVector centroid;

    /**
     * Creates a new cluster containing the specified object o.
     *
     * @param o the object belonging to this cluster.
     */
    private Cluster(DoubleVector o) {
      if (o == null)
        throw new IllegalArgumentException("DoubleVector o == null not allowed");

      this.dimensionality = o.getDimensionality();
      this.ids.add(o.getID());

      // initially e_i ist the original axis-system
      double[][] doubles = new double[dimensionality][dimensionality];
      for (int i = 0; i < dimensionality; i++) {
        doubles[i][i] = 1;
      }
      this.basis = new Matrix(doubles);

      // initially the centroid is the value array of o
      this.centroid = new DoubleVector(o.getValues());
    }
  }
}
