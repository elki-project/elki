package de.lmu.ifi.dbs.elki.algorithm.clustering.correlation;

/*
 This file is part of ELKI:
 Environment for Developing KDD-Applications Supported by Index-Structures

 Copyright (C) 2012
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
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import de.lmu.ifi.dbs.elki.algorithm.clustering.AbstractProjectedClustering;
import de.lmu.ifi.dbs.elki.data.Cluster;
import de.lmu.ifi.dbs.elki.data.Clustering;
import de.lmu.ifi.dbs.elki.data.NumberVector;
import de.lmu.ifi.dbs.elki.data.model.ClusterModel;
import de.lmu.ifi.dbs.elki.data.model.Model;
import de.lmu.ifi.dbs.elki.data.type.TypeInformation;
import de.lmu.ifi.dbs.elki.data.type.TypeUtil;
import de.lmu.ifi.dbs.elki.database.Database;
import de.lmu.ifi.dbs.elki.database.ids.DBID;
import de.lmu.ifi.dbs.elki.database.ids.DBIDUtil;
import de.lmu.ifi.dbs.elki.database.ids.DBIDs;
import de.lmu.ifi.dbs.elki.database.ids.ModifiableDBIDs;
import de.lmu.ifi.dbs.elki.database.query.DistanceResultPair;
import de.lmu.ifi.dbs.elki.database.query.GenericDistanceResultPair;
import de.lmu.ifi.dbs.elki.database.query.distance.DistanceQuery;
import de.lmu.ifi.dbs.elki.database.relation.Relation;
import de.lmu.ifi.dbs.elki.distance.distancevalue.DoubleDistance;
import de.lmu.ifi.dbs.elki.logging.Logging;
import de.lmu.ifi.dbs.elki.logging.progress.IndefiniteProgress;
import de.lmu.ifi.dbs.elki.math.linearalgebra.Matrix;
import de.lmu.ifi.dbs.elki.math.linearalgebra.SortedEigenPairs;
import de.lmu.ifi.dbs.elki.math.linearalgebra.pca.PCAResult;
import de.lmu.ifi.dbs.elki.math.linearalgebra.pca.PCARunner;
import de.lmu.ifi.dbs.elki.utilities.ClassGenericsUtil;
import de.lmu.ifi.dbs.elki.utilities.DatabaseUtil;
import de.lmu.ifi.dbs.elki.utilities.documentation.Description;
import de.lmu.ifi.dbs.elki.utilities.documentation.Reference;
import de.lmu.ifi.dbs.elki.utilities.documentation.Title;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.OptionID;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.constraints.IntervalConstraint;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.Parameterization;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.DoubleParameter;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.LongParameter;

/**
 * ORCLUS provides the ORCLUS algorithm, an algorithm to find clusters in high
 * dimensional spaces.
 * <p>
 * Reference: C. C. Aggarwal, P. S. Yu: Finding Generalized Projected Clusters
 * in High Dimensional Spaces. <br/>
 * In: Proc. ACM SIGMOD Int. Conf. on Management of Data (SIGMOD '00).
 * </p>
 * 
 * @author Elke Achtert
 * 
 * @apiviz.uses PCARunner
 * 
 * @param <V> the type of NumberVector handled by this Algorithm
 */
@Title("ORCLUS: Arbitrarily ORiented projected CLUSter generation")
@Description("Algorithm to find correlation clusters in high dimensional spaces.")
@Reference(authors = "C. C. Aggarwal, P. S. Yu", title = "Finding Generalized Projected Clusters in High Dimensional Spaces", booktitle = "Proc. ACM SIGMOD Int. Conf. on Management of Data (SIGMOD '00)", url = "http://dx.doi.org/10.1145/342009.335383")
public class ORCLUS<V extends NumberVector<V, ?>> extends AbstractProjectedClustering<Clustering<Model>, V> {
  /**
   * The logger for this class.
   */
  private static final Logging logger = Logging.getLogger(ORCLUS.class);

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
  public static final OptionID ALPHA_ID = OptionID.getOrCreateOptionID("orclus.alpha", "The factor for reducing the number of current clusters in each iteration.");

  /**
   * Parameter to specify the random generator seed.
   */
  public static final OptionID SEED_ID = OptionID.getOrCreateOptionID("orclus.seed", "The random number generator seed.");

  /**
   * Holds the value of {@link #ALPHA_ID}.
   */
  private double alpha;

  /**
   * Holds the value of {@link #SEED_ID}.
   */
  private Long seed;

  /**
   * The PCA utility object.
   */
  private PCARunner<V> pca;

  /**
   * Java constructor.
   * 
   * @param k k Parameter
   * @param k_i k_i Parameter
   * @param l l Parameter
   * @param alpha Alpha Parameter
   * @param seed Seed parameter
   * @param pca PCA runner
   */
  public ORCLUS(int k, int k_i, int l, double alpha, long seed, PCARunner<V> pca) {
    super(k, k_i, l);
    this.alpha = alpha;
    this.seed = seed;
    this.pca = pca;
  }

  /**
   * Performs the ORCLUS algorithm on the given database.
   */
  public Clustering<Model> run(Database database, Relation<V> relation) throws IllegalStateException {
    try {
      DistanceQuery<V, DoubleDistance> distFunc = this.getDistanceQuery(database);
      // current dimensionality associated with each seed
      int dim_c = DatabaseUtil.dimensionality(relation);

      if(dim_c < l) {
        throw new IllegalStateException("Dimensionality of data < parameter l! " + "(" + dim_c + " < " + l + ")");
      }

      // current number of seeds
      int k_c = Math.min(relation.size(), k_i * k);

      // pick k0 > k points from the database
      List<ORCLUSCluster> clusters = initialSeeds(relation, k_c);

      double beta = StrictMath.exp(-StrictMath.log((double) dim_c / (double) l) * StrictMath.log(1 / alpha) / StrictMath.log((double) k_c / (double) k));

      IndefiniteProgress cprogress = logger.isVerbose() ? new IndefiniteProgress("Current number of clusters:", logger) : null;

      while(k_c > k) {
        if(cprogress != null) {
          cprogress.setProcessed(clusters.size(), logger);
        }

        // find partitioning induced by the seeds of the clusters
        assign(relation, distFunc, clusters);

        // determine current subspace associated with each cluster
        for(ORCLUSCluster cluster : clusters) {
          if(cluster.objectIDs.size() > 0) {
            cluster.basis = findBasis(relation, distFunc, cluster, dim_c);
          }
        }

        // reduce number of seeds and dimensionality associated with
        // each seed
        k_c = (int) Math.max(k, k_c * alpha);
        dim_c = (int) Math.max(l, dim_c * beta);
        merge(relation, distFunc, clusters, k_c, dim_c, cprogress);
      }
      assign(relation, distFunc, clusters);

      if(cprogress != null) {
        cprogress.setProcessed(clusters.size());
        cprogress.setCompleted(logger);
      }

      // get the result
      Clustering<Model> r = new Clustering<Model>("ORCLUS clustering", "orclus-clustering");
      for(ORCLUSCluster c : clusters) {
        r.addCluster(new Cluster<Model>(c.objectIDs, ClusterModel.CLUSTER));
      }
      return r;
    }
    catch(Exception e) {
      throw new IllegalStateException(e);
    }
  }

  /**
   * Initializes the list of seeds wit a random sample of size k.
   * 
   * @param database the database holding the objects
   * @param k the size of the random sample
   * @return the initial seed list
   */
  private List<ORCLUSCluster> initialSeeds(Relation<V> database, int k) {
    DBIDs randomSample = DBIDUtil.randomSample(database.getDBIDs(), k, seed);
    V factory = DatabaseUtil.assumeVectorField(database).getFactory();
    List<ORCLUSCluster> seeds = new ArrayList<ORCLUSCluster>();
    for(DBID id : randomSample) {
      seeds.add(new ORCLUSCluster(database.get(id), id, factory));
    }
    return seeds;
  }

  /**
   * Creates a partitioning of the database by assigning each object to its
   * closest seed.
   * 
   * @param database the database holding the objects
   * @param distFunc distance function
   * @param clusters the array of clusters to which the objects should be
   *        assigned to
   */
  private void assign(Relation<V> database, DistanceQuery<V, DoubleDistance> distFunc, List<ORCLUSCluster> clusters) {
    V factory = DatabaseUtil.assumeVectorField(database).getFactory();
    // clear the current clusters
    for(ORCLUSCluster cluster : clusters) {
      cluster.objectIDs.clear();
    }

    // projected centroids of the clusters
    List<V> projectedCentroids = new ArrayList<V>(clusters.size());
    for(ORCLUSCluster c : clusters) {
      projectedCentroids.add(projection(c, c.centroid, factory));
    }

    // for each data point o do
    Iterator<DBID> it = database.iterDBIDs();
    while(it.hasNext()) {
      DBID id = it.next();
      V o = database.get(id);

      DoubleDistance minDist = null;
      ORCLUSCluster minCluster = null;

      // determine projected distance between o and cluster
      for(int i = 0; i < clusters.size(); i++) {
        ORCLUSCluster c = clusters.get(i);
        V o_proj = projection(c, o, factory);
        DoubleDistance dist = distFunc.distance(o_proj, projectedCentroids.get(i));
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
   * Finds the basis of the subspace of dimensionality <code>dim</code> for the
   * specified cluster.
   * 
   * @param database the database to run the algorithm on
   * @param distFunc the distance function
   * @param cluster the cluster
   * @param dim the dimensionality of the subspace
   * @return matrix defining the basis of the subspace for the specified cluster
   */
  private Matrix findBasis(Relation<V> database, DistanceQuery<V, DoubleDistance> distFunc, ORCLUSCluster cluster, int dim) {
    // covariance matrix of cluster
    // Matrix covariance = Util.covarianceMatrix(database, cluster.objectIDs);
    List<DistanceResultPair<DoubleDistance>> results = new ArrayList<DistanceResultPair<DoubleDistance>>(cluster.objectIDs.size());
    for(Iterator<DBID> it = cluster.objectIDs.iterator(); it.hasNext();) {
      DBID id = it.next();
      DoubleDistance distance = distFunc.distance(cluster.centroid, database.get(id));
      DistanceResultPair<DoubleDistance> qr = new GenericDistanceResultPair<DoubleDistance>(distance, id);
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
   * @param distFunc the distance function
   * @param clusters the set of current seeds
   * @param k_new the new number of seeds
   * @param d_new the new dimensionality of the subspaces for each seed
   */
  private void merge(Relation<V> database, DistanceQuery<V, DoubleDistance> distFunc, List<ORCLUSCluster> clusters, int k_new, int d_new, IndefiniteProgress cprogress) {
    ArrayList<ProjectedEnergy> projectedEnergies = new ArrayList<ProjectedEnergy>();
    for(int i = 0; i < clusters.size(); i++) {
      for(int j = 0; j < clusters.size(); j++) {
        if(i >= j) {
          continue;
        }
        // projected energy of c_ij in subspace e_ij
        ORCLUSCluster c_i = clusters.get(i);
        ORCLUSCluster c_j = clusters.get(j);

        ProjectedEnergy pe = projectedEnergy(database, distFunc, c_i, c_j, i, j, d_new);
        projectedEnergies.add(pe);
      }
    }

    while(clusters.size() > k_new) {
      if(cprogress != null) {
        cprogress.setProcessed(clusters.size(), logger);
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
          projectedEnergies.add(projectedEnergy(database, distFunc, clusters.get(c), c_ij, c, i, d_new));
        }
        else if(c > i) {
          projectedEnergies.add(projectedEnergy(database, distFunc, clusters.get(c), c_ij, i, c, d_new));
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
   * @param distFunc the distance function
   * @param c_i the first cluster
   * @param c_j the second cluster
   * @param i the index of cluster c_i in the cluster list
   * @param j the index of cluster c_j in the cluster list
   * @param dim the dimensionality of the clusters
   * @return the projected energy of the specified cluster
   */
  private ProjectedEnergy projectedEnergy(Relation<V> database, DistanceQuery<V, DoubleDistance> distFunc, ORCLUSCluster c_i, ORCLUSCluster c_j, int i, int j, int dim) {
    // union of cluster c_i and c_j
    ORCLUSCluster c_ij = union(database, distFunc, c_i, c_j, dim);
    V factory = DatabaseUtil.assumeVectorField(database).getFactory();

    DoubleDistance sum = getDistanceFunction().getDistanceFactory().nullDistance();
    V c_proj = projection(c_ij, c_ij.centroid, factory);
    for(DBID id : c_ij.objectIDs) {
      V o = database.get(id);
      V o_proj = projection(c_ij, o, factory);
      DoubleDistance dist = distFunc.distance(o_proj, c_proj);
      sum = sum.plus(dist.times(dist));
    }
    DoubleDistance projectedEnergy = sum.times(1.0 / c_ij.objectIDs.size());

    return new ProjectedEnergy(i, j, c_ij, projectedEnergy);
  }

  /**
   * Returns the union of the two specified clusters.
   * 
   * @param database the database holding the objects
   * @param distFunc the distance function
   * @param c1 the first cluster
   * @param c2 the second cluster
   * @param dim the dimensionality of the union cluster
   * @return the union of the two specified clusters
   */
  private ORCLUSCluster union(Relation<V> database, DistanceQuery<V, DoubleDistance> distFunc, ORCLUSCluster c1, ORCLUSCluster c2, int dim) {
    ORCLUSCluster c = new ORCLUSCluster();

    c.objectIDs = DBIDUtil.newHashSet(c1.objectIDs);
    c.objectIDs.addDBIDs(c2.objectIDs);
    // convert into array.
    c.objectIDs = DBIDUtil.newArray(c.objectIDs);

    if(c.objectIDs.size() > 0) {
      c.centroid = DatabaseUtil.centroid(database, c.objectIDs);
      c.basis = findBasis(database, distFunc, c, dim);
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
   * @param factory Factory object / prototype
   * @return the projection of double vector o in the subspace of cluster c
   */
  private V projection(ORCLUSCluster c, V o, V factory) {
    Matrix o_proj = o.getRowVector().times(c.basis);
    double[] values = o_proj.getColumnPackedCopy();
    return factory.newNumberVector(values);
  }

  @Override
  public TypeInformation[] getInputTypeRestriction() {
    return TypeUtil.array(TypeUtil.NUMBER_VECTOR_FIELD);
  }

  @Override
  protected Logging getLogger() {
    return logger;
  }

  /**
   * Encapsulates the attributes of a cluster.
   * 
   * @apiviz.exclude
   */
  private final class ORCLUSCluster {
    // TODO: reuse/derive from existing cluster classes?
    /**
     * The ids of the objects belonging to this cluster.
     */
    ModifiableDBIDs objectIDs = DBIDUtil.newArray();

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
     * @param factory Factory object / prototype
     */
    ORCLUSCluster(V o, DBID id, V factory) {
      this.objectIDs.add(id);

      // initially the basis ist the original axis-system
      int dim = o.getDimensionality();
      this.basis = Matrix.unitMatrix(dim);

      // TODO: can we replace this with some kind of clone() statement?
      // initially the centroid is the value array of o
      double[] values = new double[o.getDimensionality()];
      for(int d = 1; d <= o.getDimensionality(); d++) {
        values[d - 1] = o.doubleValue(d);
      }
      this.centroid = factory.newNumberVector(values);
    }
  }

  /**
   * Encapsulates the projected energy for a cluster.
   * 
   * @apiviz.exclude
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
    @Override
    public int compareTo(ProjectedEnergy o) {
      return this.projectedEnergy.compareTo(o.projectedEnergy);
    }
  }

  /**
   * Parameterization class.
   * 
   * @author Erich Schubert
   * 
   * @apiviz.exclude
   */
  public static class Parameterizer<V extends NumberVector<V, ?>> extends AbstractProjectedClustering.Parameterizer {
    protected double alpha = -1;

    protected Long seed = null;

    protected PCARunner<V> pca = null;

    @Override
    protected void makeOptions(Parameterization config) {
      super.makeOptions(config);
      configK(config);
      configKI(config);
      configL(config);
      configAlpha(config);
      configSeed(config);

      // TODO: make configurable, to allow using stabilized PCA
      Class<PCARunner<V>> cls = ClassGenericsUtil.uglyCastIntoSubclass(PCARunner.class);
      pca = config.tryInstantiate(cls);
    }

    protected void configAlpha(Parameterization config) {
      DoubleParameter alphaP = new DoubleParameter(ALPHA_ID, new IntervalConstraint(0, IntervalConstraint.IntervalBoundary.OPEN, 1, IntervalConstraint.IntervalBoundary.CLOSE), 0.5);
      if(config.grab(alphaP)) {
        alpha = alphaP.getValue();
      }
    }

    protected void configSeed(Parameterization config) {
      LongParameter seedP = new LongParameter(SEED_ID, true);
      if(config.grab(seedP)) {
        seed = seedP.getValue();
      }
    }

    @Override
    protected ORCLUS<V> makeInstance() {
      return new ORCLUS<V>(k, k_i, l, alpha, seed, pca);
    }
  }
}