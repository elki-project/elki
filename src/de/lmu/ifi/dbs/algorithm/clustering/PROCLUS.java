package de.lmu.ifi.dbs.algorithm.clustering;

import de.lmu.ifi.dbs.algorithm.result.clustering.Clusters;
import de.lmu.ifi.dbs.data.RealVector;
import de.lmu.ifi.dbs.database.Database;
import de.lmu.ifi.dbs.distance.DoubleDistance;
import de.lmu.ifi.dbs.math.linearalgebra.EigenvalueDecomposition;
import de.lmu.ifi.dbs.math.linearalgebra.Matrix;
import de.lmu.ifi.dbs.math.linearalgebra.SortedEigenPairs;
import de.lmu.ifi.dbs.utilities.Description;
import de.lmu.ifi.dbs.utilities.Util;
import de.lmu.ifi.dbs.utilities.optionhandling.*;

import java.util.*;

/**
 * PROCLUS provides the PROCLUS algorithm.
 *
 * @author Elke Achtert (<a
 *         href="mailto:achtert@dbs.ifi.lmu.de">achtert@dbs.ifi.lmu.de</a>)
 */

public class PROCLUS extends ProjectedClustering {

  /**
   * Parameter m_i.
   */
  public static final String M_I_P = "m_i";

  /**
   * Default value for m_i.
   */
  public static final int M_I_DEFAULT = K_I_DEFAULT / 3;

  /**
   * Description for parameter m_i.
   */
  public static final String M_I_D = "positive integer value to specify the multiplier for "
                                     + "the initial number of medoids, default: " + M_I_DEFAULT;

  /**
   * Holds m_i.
   */
  private int m_i;

  /**
   * Sets the parameter k and l the optionhandler additionally to the
   * parameters provided by super-classes.
   */
  public PROCLUS() {
    super();
    // parameter m_i
    IntParameter mi = new IntParameter(M_I_P, M_I_D, new GreaterConstraint(0));
    mi.setDefaultValue(M_I_DEFAULT);
    optionHandler.put(M_I_P, mi);
  }

  /**
   * @see de.lmu.ifi.dbs.algorithm.AbstractAlgorithm#runInTime(de.lmu.ifi.dbs.database.Database)
   */
  protected void runInTime(Database<RealVector> database) throws IllegalStateException {

    try {
      final int dim = getDim();
      final int k = getK();
      final int k_i = getK_i();

      if (database.dimensionality() < dim)
        throw new IllegalStateException("Dimensionality of data < parameter l! " +
                                        "(" + database.dimensionality() + " < " + dim + ")");

      // initialization phase
      int sampleSize = Math.min(database.size(), k_i * k);

      List<Cluster> clusters = initialSeeds(database, sampleSize);

      int medoidSize = Math.min(database.size(), m_i * k);



      // current number of seeds
      int k_c = Math.min(database.size(), k_i * k);

      // current dimensionality associated with each seed
      int dim_c = database.dimensionality();

      // pick k0 > k points from the database
//      List<Cluster> clusters = initialSeeds(database, k_c);


      double beta = Math.exp(-Math.log((double) dim_c / (double) dim)
//                             * Math.log(1 / alpha)
                             / Math.log((double) k_c / (double) k));

      while (k_c > k) {
        if (isVerbose()) {
          verbose("\rCurrent number of clusters: "
                  + clusters.size() + ".                           ");
        }

        // find partitioning induced by the seeds of the clusters
        assign(database, clusters);

        // determine current subspace associated with each cluster
        for (PROCLUS.Cluster cluster : clusters) {
          if (cluster.objectIDs.size() > 0)
            cluster.basis = findBasis(database, cluster, dim_c);
        }

        // reduce number of seeds and dimensionality associated with
        // each seed
//        k_c = (int) Math.max(k, k_c * alpha);
        dim_c = (int) Math.max(dim, dim_c * beta);
        merge(database, clusters, k_c, dim_c);
      }
      assign(database, clusters);

      if (isVerbose()) {
        verbose("\nNumber of clusters: " + clusters.size() + ".                           ");
      }

      // get the result
      Integer[][] ids = new Integer[clusters.size()][];
      int i = 0;
      for (PROCLUS.Cluster c : clusters) {
        ids[i++] = c.objectIDs.toArray(new Integer[c.objectIDs.size()]);
      }
      setResult(new Clusters<RealVector>(ids, database));
    }
    catch (Exception e) {
      e.printStackTrace();
      throw new IllegalStateException(e);
    }

  }

  /**
   * @see de.lmu.ifi.dbs.algorithm.Algorithm#getDescription()
   */
  public Description getDescription() {
    return new Description(
        "PROCLUS",
        "PROjected CLUStering",
        "Algorithm to find subspace clusters in high dimensional spaces.",
        "C. C. Aggrawal, C. Procopiuc, J. L. Wolf, P. S. Yu, J. S. Park: "
        + "Fast Algorithms for Projected Clustering "
        + "In: Proc. ACM SIGMOD Int. Conf. on Management of Data (SIGMOD '99)");
  }

  /**
   * Sets the parameters k and l additionally to the parameters set by the
   * super-class' method. Both k and l are required parameters.
   *
   * @see de.lmu.ifi.dbs.utilities.optionhandling.Parameterizable#setParameters(String[])
   */
  public String[] setParameters(String[] args) throws ParameterException {
    String[] remainingParameters = super.setParameters(args);

    // m_i
    if (optionHandler.isSet(M_I_P)) {
      String m_i_String = optionHandler.getOptionValue(M_I_P);
      try {
        m_i = Integer.parseInt(m_i_String);
        if (m_i <= 0) {
          throw new WrongParameterValueException(M_I_P, m_i_String,M_I_D);
        }
      }
      catch (NumberFormatException e) {
        throw new WrongParameterValueException(M_I_P, m_i_String,M_I_D, e);
      }
    }
    else
      m_i = M_I_DEFAULT;

    return remainingParameters;
  }

  /**
   * Returns the parameter setting of this algorithm.
   *
   * @return the parameter setting of this algorithm
   */
  public List<AttributeSettings> getAttributeSettings() {
    List<AttributeSettings> settings = super.getAttributeSettings();

    AttributeSettings mySettings = settings.get(0);
    mySettings.addSetting(M_I_P, Integer.toString(m_i));

    return settings;
  }

  /**
   * Initializes the list of seeds wit a random sample of size k.
   *
   * @param database the database holding the objects
   * @param k        the size of the random sample
   * @return the initial seed list
   */
  private List<PROCLUS.Cluster> initialSeeds(Database<RealVector> database, int k) {
    Set<Integer> randomSample = database.randomSample(k, 1);

    List<PROCLUS.Cluster> seeds = new ArrayList<PROCLUS.Cluster>();
    for (Integer id : randomSample) {
      seeds.add(new PROCLUS.Cluster(database.get(id)));
    }
    return seeds;
  }

  /**
   * Creates a partitioning of the database by assigning each object to its
   * closest seed.
   *
   * @param database the database holding the objects
   * @param clusters the array of clusters to which the objects should be assigned
   *                 to
   */
  private void assign(Database<RealVector> database, List<PROCLUS.Cluster> clusters) {
    // clear the current clusters
    for (PROCLUS.Cluster cluster : clusters) {
      cluster.objectIDs.clear();
    }

    // projected centroids of the clusters
    RealVector[] projectedCentroids = new RealVector[clusters.size()];
    for (int i = 0; i < projectedCentroids.length; i++) {
      PROCLUS.Cluster c = clusters.get(i);
      projectedCentroids[i] = projection(c, c.centroid);
    }

    // for each data point o do
    Iterator<Integer> it = database.iterator();
    while (it.hasNext()) {
      Integer id = it.next();
      RealVector o = database.get(id);

      DoubleDistance minDist = null;
      PROCLUS.Cluster minCluster = null;

      // determine projected distance between o and cluster
      for (int i = 0; i < projectedCentroids.length; i++) {
        PROCLUS.Cluster c = clusters.get(i);
        RealVector o_proj = projection(c, o);
        DoubleDistance dist = getDistanceFunction().distance(o_proj, projectedCentroids[i]);
        if (minDist == null || minDist.compareTo(dist) > 0) {
          minDist = dist;
          minCluster = c;
        }
      }
      // add p to the cluster with the least value of projected distance
      assert minCluster != null;
      minCluster.objectIDs.add(id);
    }

    // recompute the seed in each clusters
    for (PROCLUS.Cluster cluster : clusters) {
      if (cluster.objectIDs.size() > 0)
        cluster.centroid = Util.centroid(database, cluster.objectIDs);
    }
  }

  /**
   * Finds the basis of the subspace of dimensionality
   * <code> for the specified cluster.
   *
   * @param cluster the cluster
   * @param dim     the dimensionality of the subspace
   * @return matrix defining the basis of the subspace for the specified cluster
   */
  private Matrix findBasis(Database<RealVector> database, PROCLUS.Cluster cluster,
                           int dim) {
    // covariance matrix of cluster
    Matrix covariance = Util.covarianceMatrix(database, cluster.objectIDs);

    // eigenvectors in ascending order
    EigenvalueDecomposition evd = covariance.eig();
    SortedEigenPairs eigenPairs = new SortedEigenPairs(evd, true);

    // eigenvectors corresponding to the smallest dim eigenvalues
    return eigenPairs.eigenVectors(dim);
  }

  /**
   * Reduces the number of seeds to k_new
   *
   * @param database the database holding the objects
   * @param clusters the set of current seeds
   * @param k_new    the new number of seeds
   * @param d_new    the new dimensionality of the subspaces for each seed
   */
  private void merge(Database<RealVector> database, List<PROCLUS.Cluster> clusters,
                     int k_new, int d_new) {
    ArrayList<PROCLUS.ProjectedEnergy> projectedEnergies = new ArrayList<PROCLUS.ProjectedEnergy>();
    for (int i = 0; i < clusters.size(); i++) {
      for (int j = 0; j < clusters.size(); j++) {
        if (i >= j)
          continue;
        // projected energy of c_ij in subspace e_ij
        PROCLUS.Cluster c_i = clusters.get(i);
        PROCLUS.Cluster c_j = clusters.get(j);

        PROCLUS.ProjectedEnergy pe = projectedEnergy(database, c_i, c_j, i, j,
                                                     d_new);
        projectedEnergies.add(pe);
      }
    }

    while (clusters.size() > k_new) {
      if (isVerbose()) {
        verbose("\rCurrent number of clusters: "
                + clusters.size() + ".                           ");
      }
      // find the smallest value of r_ij
      PROCLUS.ProjectedEnergy minPE = Collections.min(projectedEnergies);

      // renumber the clusters by replacing cluster c_i with cluster c_ij
      // and discarding cluster c_j
      for (int c = 0; c < clusters.size(); c++) {
        if (c == minPE.i) {
          clusters.remove(c);
          clusters.add(c, minPE.cluster);
        }
        if (c == minPE.j) {
          clusters.remove(c);
        }
      }

      // remove obsolete projected energies and renumber the others ...
      int i = minPE.i;
      int j = minPE.j;
      Iterator<PROCLUS.ProjectedEnergy> it = projectedEnergies.iterator();
      while (it.hasNext()) {
        PROCLUS.ProjectedEnergy pe = it.next();
        if (pe.i == i || pe.i == j || pe.j == i || pe.j == j) {
          it.remove();
        }
        else {
          if (pe.i > j) {
            pe.i -= 1;
          }
          if (pe.j > j) {
            pe.j -= 1;
          }
        }
      }

      // ... and recompute them
      PROCLUS.Cluster c_ij = minPE.cluster;
      for (int c = 0; c < clusters.size(); c++) {
        if (c < i) {
          projectedEnergies.add(projectedEnergy(database, clusters.get(c), c_ij, c, i, d_new));
        }
        else if (c > i) {
          projectedEnergies.add(projectedEnergy(database, clusters.get(c), c_ij, i, c, d_new));
        }
      }
    }
  }

  /**
   * Computes the projected energy of the specified clusters. The projected
   * energy is given by the mean square distance of the points to the centroid
   * of the union cluster c, when all points in c are projected to the
   * subspace of c.
   *
   * @param database the database holding the objects
   * @param c_i      the first cluster
   * @param c_j      the second cluster
   * @param i        the index of cluster c_i in the cluster list
   * @param j        the index of cluster c_j in the cluster list
   * @return the projected energy of the specified cluster
   */
  private PROCLUS.ProjectedEnergy projectedEnergy(Database<RealVector> database,
                                                  PROCLUS.Cluster c_i, PROCLUS.Cluster c_j, int i, int j, int dim) {
    // union of cluster c_i and c_j
    PROCLUS.Cluster c_ij = union(database, c_i, c_j, dim);

    DoubleDistance sum = getDistanceFunction().nullDistance();
    RealVector c_proj = projection(c_ij, c_ij.centroid);
    for (Integer id : c_ij.objectIDs) {
      RealVector o = database.get(id);
      RealVector o_proj = projection(c_ij, o);
      DoubleDistance dist = getDistanceFunction().distance(o_proj, c_proj);
      sum = sum.plus(dist.times(dist));
    }
    DoubleDistance projectedEnergy = sum.times(1.0 / c_ij.objectIDs.size());

    return new PROCLUS.ProjectedEnergy(i, j, c_ij, projectedEnergy);
  }

  /**
   * Returns the union of the two specified clusters.
   *
   * @param database the database holding the objects
   * @param c1       the first cluster
   * @param c2       the second cluster
   * @param dim      the dimensionality of the union cluster
   * @return the union of the two specified clusters
   */
  private PROCLUS.Cluster union(Database<RealVector> database, PROCLUS.Cluster c1,
                                PROCLUS.Cluster c2, int dim) {
    PROCLUS.Cluster c = new PROCLUS.Cluster();

    HashSet<Integer> ids = new HashSet<Integer>(c1.objectIDs);
    ids.addAll(c2.objectIDs);

    c.objectIDs = new ArrayList<Integer>(ids);

    if (c.objectIDs.size() > 0) {
      c.centroid = Util.centroid(database, c.objectIDs);
      c.basis = findBasis(database, c, dim);
    }
    else {
      // noinspection unchecked
      c.centroid = (RealVector) c1.centroid.plus(c2.centroid)
          .multiplicate(0.5);

      double[][] doubles = new double[c1.basis.getRowDimensionality()][dim];
      for (int i = 0; i < dim; i++) {
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
  private RealVector projection(PROCLUS.Cluster c, RealVector o) {
    Matrix o_proj = o.getRowVector().times(c.basis);
    double[] values = o_proj.getColumnPackedCopy();
    return o.newInstance(values);
  }

  private Set<Integer> greedy() {
    return null;
  }

  /**
   * Encapsulates the attributes of a cluster.
   */
  private class Cluster {
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
    RealVector centroid;

    /**
     * Creates a new empty cluster.
     */
    Cluster() {
    }

    /**
     * Creates a new cluster containing the specified object o.
     *
     * @param o the object belonging to this cluster.
     */
    Cluster(RealVector o) {
      this.objectIDs.add(o.getID());

      // initially the basis ist the original axis-system
      int dim = o.getDimensionality();
      double[][] doubles = new double[dim][dim];
      for (int i = 0; i < dim; i++) {
        doubles[i][i] = 1;
      }
      this.basis = new Matrix(doubles);

      // initially the centroid is the value array of o
      double[] values = new double[o.getDimensionality()];
      for (int d = 1; d <= o.getDimensionality(); d++)
        values[d - 1] = o.getValue(d).doubleValue();
      this.centroid = o.newInstance(values);
    }
  }

  private class ProjectedEnergy implements Comparable<PROCLUS.ProjectedEnergy> {
    int i;

    int j;

    PROCLUS.Cluster cluster;

    DoubleDistance projectedEnergy;

    ProjectedEnergy(int i, int j, PROCLUS.Cluster cluster,
                    DoubleDistance projectedEnergy) {
      this.i = i;
      this.j = j;
      this.cluster = cluster;
      this.projectedEnergy = projectedEnergy;
    }

    /**
     * Compares this object with the specified object for order.
     *
     * @param o the Object to be compared.
     * @return a negative integer, zero, or a positive integer as this
     *         object is less than, equal to, or greater than the specified
     *         object.
     */
    public int compareTo(PROCLUS.ProjectedEnergy o) {
      return this.projectedEnergy.compareTo(o.projectedEnergy);
    }
  }
}
