/*
 * This file is part of ELKI:
 * Environment for Developing KDD-Applications Supported by Index-Structures
 *
 * Copyright (C) 2022
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
package elki.clustering.correlation;

import static elki.math.linearalgebra.VMath.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import elki.clustering.AbstractProjectedClustering;
import elki.data.Cluster;
import elki.data.Clustering;
import elki.data.NumberVector;
import elki.data.model.ClusterModel;
import elki.data.model.Model;
import elki.data.type.TypeInformation;
import elki.data.type.TypeUtil;
import elki.database.ids.*;
import elki.database.relation.Relation;
import elki.database.relation.RelationUtil;
import elki.distance.minkowski.SquaredEuclideanDistance;
import elki.logging.Logging;
import elki.logging.progress.IndefiniteProgress;
import elki.math.linearalgebra.Centroid;
import elki.math.linearalgebra.pca.PCARunner;
import elki.result.Metadata;
import elki.utilities.documentation.Description;
import elki.utilities.documentation.Reference;
import elki.utilities.documentation.Title;
import elki.utilities.optionhandling.OptionID;
import elki.utilities.optionhandling.constraints.CommonConstraints;
import elki.utilities.optionhandling.parameterization.Parameterization;
import elki.utilities.optionhandling.parameters.DoubleParameter;
import elki.utilities.optionhandling.parameters.IntParameter;
import elki.utilities.optionhandling.parameters.ObjectParameter;
import elki.utilities.optionhandling.parameters.RandomParameter;
import elki.utilities.random.RandomFactory;

import net.jafama.FastMath;

/**
 * ORCLUS: Arbitrarily ORiented projected CLUSter generation.
 * <p>
 * Reference:
 * <p>
 * C. C. Aggarwal, P. S. Yu<br>
 * Finding Generalized Projected Clusters in High Dimensional Spaces<br>
 * Proc. ACM SIGMOD Int. Conf. on Management of Data (SIGMOD '00).
 *
 * @author Elke Achtert
 * @since 0.1
 *
 * @has - - - PCARunner
 * @composed - - - ProjectedEnergy
 */
@Title("ORCLUS: Arbitrarily ORiented projected CLUSter generation")
@Description("Algorithm to find correlation clusters in high dimensional spaces.")
@Reference(authors = "C. C. Aggarwal, P. S. Yu", //
    title = "Finding Generalized Projected Clusters in High Dimensional Spaces", //
    booktitle = "Proc. ACM SIGMOD Int. Conf. on Management of Data (SIGMOD '00)", //
    url = "https://doi.org/10.1145/342009.335383", //
    bibkey = "DBLP:conf/sigmod/AggarwalY00")
public class ORCLUS extends AbstractProjectedClustering<Clustering<Model>> {
  /**
   * The logger for this class.
   */
  private static final Logging LOG = Logging.getLogger(ORCLUS.class);

  /**
   * Holds the value of {@link Par#ALPHA_ID}.
   */
  private double alpha;

  /**
   * Random generator
   */
  private RandomFactory rnd;

  /**
   * The PCA utility object.
   */
  private PCARunner pca;

  /**
   * Java constructor.
   * 
   * @param k k Parameter
   * @param k_i k_i Parameter
   * @param l l Parameter
   * @param alpha Alpha Parameter
   * @param rnd Random generator
   * @param pca PCA runner
   */
  public ORCLUS(int k, int k_i, int l, double alpha, RandomFactory rnd, PCARunner pca) {
    super(k, k_i, l);
    this.alpha = alpha;
    this.rnd = rnd;
    this.pca = pca;
  }

  @Override
  public TypeInformation[] getInputTypeRestriction() {
    return TypeUtil.array(TypeUtil.NUMBER_VECTOR_FIELD);
  }

  /**
   * Performs the ORCLUS algorithm on the given database.
   *
   * @param relation Relation
   */
  public Clustering<Model> run(Relation<? extends NumberVector> relation) {
    // current dimensionality associated with each seed
    int dim_c = RelationUtil.dimensionality(relation);

    if(dim_c < l) {
      throw new IllegalStateException("Dimensionality of data < parameter l! " + "(" + dim_c + " < " + l + ")");
    }

    // current number of seeds
    int k_c = Math.min(relation.size(), k_i * k);

    // pick k0 > k points from the database
    List<ORCLUSCluster> clusters = initialSeeds(relation, k_c);

    double beta = FastMath.exp(-FastMath.log(dim_c / (double) l) * FastMath.log(1 / alpha) / FastMath.log(k_c / (double) k));

    IndefiniteProgress cprogress = LOG.isVerbose() ? new IndefiniteProgress("Current number of clusters:", LOG) : null;

    while(k_c > k) {
      // find partitioning induced by the seeds of the clusters
      assign(relation, clusters);

      // determine current subspace associated with each cluster
      for(ORCLUSCluster cluster : clusters) {
        if(cluster.objectIDs.size() > 0) {
          cluster.basis = findBasis(relation, cluster, dim_c);
        }
      }

      // reduce number of seeds and dimensionality associated with
      // each seed
      k_c = (int) Math.max(k, k_c * alpha);
      dim_c = (int) Math.max(l, dim_c * beta);
      merge(relation, clusters, k_c, dim_c, cprogress);
      if(cprogress != null) {
        cprogress.setProcessed(clusters.size(), LOG);
      }
    }
    assign(relation, clusters);

    LOG.setCompleted(cprogress);

    // get the result
    Clustering<Model> result = new Clustering<>();
    Metadata.of(result).setLongName("ORCLUS Clustering");
    for(ORCLUSCluster c : clusters) {
      result.addToplevelCluster(new Cluster<Model>(c.objectIDs, ClusterModel.CLUSTER));
    }
    return result;
  }

  /**
   * Initializes the list of seeds wit a random sample of size k.
   * 
   * @param database the database holding the objects
   * @param k the size of the random sample
   * @return the initial seed list
   */
  private List<ORCLUSCluster> initialSeeds(Relation<? extends NumberVector> database, int k) {
    DBIDs randomSample = DBIDUtil.randomSample(database.getDBIDs(), k, rnd);
    List<ORCLUSCluster> seeds = new ArrayList<>(k);
    for(DBIDIter iter = randomSample.iter(); iter.valid(); iter.advance()) {
      seeds.add(new ORCLUSCluster(database.get(iter).toArray(), iter));
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
  private void assign(Relation<? extends NumberVector> database, List<ORCLUSCluster> clusters) {
    SquaredEuclideanDistance distFunc = SquaredEuclideanDistance.STATIC;
    // clear the current clusters
    for(ORCLUSCluster cluster : clusters) {
      cluster.objectIDs.clear();
    }

    // projected centroids of the clusters
    List<double[]> projectedCentroids = new ArrayList<>(clusters.size());
    for(ORCLUSCluster c : clusters) {
      projectedCentroids.add(times(c.basis, c.centroid));
    }

    // for each data point o do
    for(DBIDIter it = database.iterDBIDs(); it.valid(); it.advance()) {
      double[] o = database.get(it).toArray();

      // determine projected distance between o and clusters
      ORCLUSCluster minCluster = clusters.get(0);
      double minDist = distFunc.distance(times(minCluster.basis, o), projectedCentroids.get(0));
      for(int i = 1; i < clusters.size(); i++) {
        ORCLUSCluster c = clusters.get(i);
        double dist = distFunc.distance(times(c.basis, o), projectedCentroids.get(i));
        if(dist < minDist) {
          minDist = dist;
          minCluster = c;
        }
      }
      // add p to the cluster with the least value of projected distance
      minCluster.objectIDs.add(it);
    }

    // recompute the seed in each clusters
    for(ORCLUSCluster cluster : clusters) {
      if(cluster.objectIDs.size() > 0) {
        cluster.centroid = Centroid.make(database, cluster.objectIDs).toArray();
      }
    }
  }

  /**
   * Finds the basis of the subspace of dimensionality <code>dim</code> for the
   * specified cluster.
   * 
   * @param database the database to run the algorithm on
   * @param cluster the cluster
   * @param dim the dimensionality of the subspace
   * @return matrix defining the basis of the subspace for the specified cluster
   */
  private double[][] findBasis(Relation<? extends NumberVector> database, ORCLUSCluster cluster, int dim) {
    double[][] evs = pca.processIds(cluster.objectIDs, database).getEigenvectors();
    return Arrays.copyOfRange(evs, evs.length - dim, evs.length);
  }

  /**
   * Reduces the number of seeds to k_new
   * 
   * @param relation the database holding the objects
   * @param clusters the set of current seeds
   * @param k_new the new number of seeds
   * @param d_new the new dimensionality of the subspaces for each seed
   */
  private void merge(Relation<? extends NumberVector> relation, List<ORCLUSCluster> clusters, int k_new, int d_new, IndefiniteProgress cprogress) {
    ArrayList<ProjectedEnergy> projectedEnergies = new ArrayList<>((clusters.size() * (clusters.size() - 1)) >>> 1);
    for(int i = 0; i < clusters.size(); i++) {
      ORCLUSCluster c_i = clusters.get(i);
      for(int j = i + 1; j < clusters.size(); j++) {
        // projected energy of c_ij in subspace e_ij
        projectedEnergies.add(projectedEnergy(relation, c_i, clusters.get(j), i, j, d_new));
      }
    }

    while(clusters.size() > k_new) {
      if(cprogress != null) {
        cprogress.setProcessed(clusters.size(), LOG);
      }
      // find the smallest value of r_ij
      ProjectedEnergy minPE = Collections.min(projectedEnergies);
      ORCLUSCluster c_ij = minPE.cluster;
      final int i = minPE.i, j = minPE.j;

      // renumber the clusters by replacing cluster c_i with cluster c_ij
      // and discarding cluster c_j
      clusters.set(i, c_ij);
      clusters.remove(j);

      // remove obsolete projected energies and renumber the others ...
      projectedEnergies.removeIf(pe -> pe.i == i || pe.i == j || pe.j == i || pe.j == j);
      for(ProjectedEnergy pe : projectedEnergies) {
        if(pe.i > j) {
          pe.i--;
        }
        if(pe.j > j) {
          pe.j--;
        }
      }

      // ... and recompute the PEs with the new cluster
      for(int c = 0; c < clusters.size(); c++) {
        if(c < i) {
          projectedEnergies.add(projectedEnergy(relation, clusters.get(c), c_ij, c, i, d_new));
        }
        else if(c > i) {
          projectedEnergies.add(projectedEnergy(relation, clusters.get(c), c_ij, i, c, d_new));
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
   * @param relation the relation holding the objects
   * @param c_i the first cluster
   * @param c_j the second cluster
   * @param i the index of cluster c_i in the cluster list
   * @param j the index of cluster c_j in the cluster list
   * @param dim the dimensionality of the clusters
   * @return the projected energy of the specified cluster
   */
  private ProjectedEnergy projectedEnergy(Relation<? extends NumberVector> relation, ORCLUSCluster c_i, ORCLUSCluster c_j, int i, int j, int dim) {
    SquaredEuclideanDistance distFunc = SquaredEuclideanDistance.STATIC;
    // union of cluster c_i and c_j
    ORCLUSCluster c_ij = union(relation, c_i, c_j, dim);

    double sum = 0.;
    double[] c_proj = times(c_ij.basis, c_ij.centroid);
    for(DBIDIter iter = c_ij.objectIDs.iter(); iter.valid(); iter.advance()) {
      sum += distFunc.distance(c_proj, times(c_ij.basis, relation.get(iter).toArray()));
    }
    sum /= c_ij.objectIDs.size();

    return new ProjectedEnergy(i, j, c_ij, sum);
  }

  /**
   * Returns the union of the two specified clusters.
   * 
   * @param relation the database holding the objects
   * @param c1 the first cluster
   * @param c2 the second cluster
   * @param dim the dimensionality of the union cluster
   * @return the union of the two specified clusters
   */
  private ORCLUSCluster union(Relation<? extends NumberVector> relation, ORCLUSCluster c1, ORCLUSCluster c2, int dim) {
    ORCLUSCluster c = new ORCLUSCluster();
    c.objectIDs = DBIDUtil.union(c1.objectIDs, c2.objectIDs);

    if(!c.objectIDs.isEmpty()) {
      c.centroid = Centroid.make(relation, c.objectIDs).getArrayRef();
      c.basis = findBasis(relation, c, dim);
    }
    else {
      c.centroid = timesEquals(plus(c1.centroid, c2.centroid), .5);
      c.basis = identity(dim, c.centroid.length);
    }
    return c;
  }

  /**
   * Encapsulates the attributes of a cluster.
   */
  private static final class ORCLUSCluster {
    // TODO: reuse/derive from existing cluster classes?
    /**
     * The ids of the objects belonging to this cluster.
     */
    ModifiableDBIDs objectIDs = DBIDUtil.newArray();

    /**
     * The matrix defining the subspace of this cluster.
     * 
     * Note: we store the vectors in rows here, think of an array of
     * Eigenvectors.
     */
    double[][] basis;

    /**
     * The centroid of this cluster.
     */
    double[] centroid;

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
     * @param id Object id
     */
    ORCLUSCluster(double[] o, DBIDRef id) {
      this.centroid = o;
      this.basis = unitMatrix(o.length);
      this.objectIDs.add(id);
    }
  }

  /**
   * Encapsulates the projected energy for a cluster.
   */
  private static final class ProjectedEnergy implements Comparable<ProjectedEnergy> {
    /**
     * Origin cluster indexes
     */
    int i, j;

    /**
     * Resulting merged cluster
     */
    ORCLUSCluster cluster;

    /**
     * Projected energy
     */
    double projectedEnergy;

    /**
     * Constructor.
     *
     * @param i First cluster id
     * @param j Second cluster id
     * @param cluster Resulting merged cluster
     * @param projectedEnergy Projected energy
     */
    ProjectedEnergy(int i, int j, ORCLUSCluster cluster, double projectedEnergy) {
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
      return Double.compare(projectedEnergy, o.projectedEnergy);
    }
  }

  /**
   * Parameterization class.
   * 
   * @author Erich Schubert
   */
  public static class Par extends AbstractProjectedClustering.Par {
    /**
     * Parameter to specify the factor for reducing the number of current
     * clusters in each iteration, must be an integer greater than 0 and less
     * than 1.
     */
    public static final OptionID ALPHA_ID = new OptionID("orclus.alpha", "The factor for reducing the number of current clusters in each iteration.");

    /**
     * Parameter to specify the random generator seed.
     */
    public static final OptionID SEED_ID = new OptionID("orclus.seed", "The random number generator seed.");

    /**
     * Cluster reduction factor
     */
    protected double alpha;

    /**
     * Random number generation.
     */
    protected RandomFactory rnd;

    /**
     * PCA procedure
     */
    protected PCARunner pca;

    @Override
    public void configure(Parameterization config) {
      super.configure(config);
      new IntParameter(K_ID) //
          .addConstraint(CommonConstraints.GREATER_EQUAL_ONE_INT) //
          .grab(config, x -> k = x);
      new IntParameter(K_I_ID, 30) //
          .addConstraint(CommonConstraints.GREATER_EQUAL_ONE_INT) //
          .grab(config, x -> k_i = x);
      new IntParameter(L_ID) //
          .addConstraint(CommonConstraints.GREATER_EQUAL_ONE_INT) //
          .grab(config, x -> l = x);
      new DoubleParameter(ALPHA_ID, 0.5) //
          .addConstraint(CommonConstraints.GREATER_THAN_ZERO_DOUBLE) //
          .addConstraint(CommonConstraints.LESS_EQUAL_ONE_DOUBLE) //
          .grab(config, x -> alpha = x);
      new RandomParameter(SEED_ID).grab(config, x -> rnd = x);
      new ObjectParameter<PCARunner>(PCARunner.Par.PCARUNNER_ID, PCARunner.class, PCARunner.class) //
          .grab(config, x -> pca = x);
    }

    @Override
    public ORCLUS make() {
      return new ORCLUS(k, k_i, l, alpha, rnd, pca);
    }
  }
}
