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
package elki.clustering.kmedoids;

import java.util.ArrayList;
import java.util.List;

import elki.Algorithm;
import elki.clustering.kmeans.KMeans;
import elki.clustering.kmeans.initialization.RandomlyChosen;
import elki.clustering.kmedoids.initialization.AlternateRefinement;
import elki.clustering.kmedoids.initialization.KMedoidsInitialization;
import elki.data.Cluster;
import elki.data.Clustering;
import elki.data.model.MedoidModel;
import elki.data.type.TypeInformation;
import elki.data.type.TypeUtil;
import elki.database.datastore.DataStoreFactory;
import elki.database.datastore.DataStoreUtil;
import elki.database.datastore.WritableIntegerDataStore;
import elki.database.ids.*;
import elki.database.query.QueryBuilder;
import elki.database.query.distance.DistanceQuery;
import elki.database.relation.Relation;
import elki.distance.Distance;
import elki.distance.minkowski.EuclideanDistance;
import elki.logging.Logging;
import elki.logging.progress.IndefiniteProgress;
import elki.logging.statistics.DoubleStatistic;
import elki.logging.statistics.LongStatistic;
import elki.logging.statistics.StringStatistic;
import elki.result.Metadata;
import elki.utilities.Priority;
import elki.utilities.documentation.Reference;
import elki.utilities.optionhandling.Parameterizer;
import elki.utilities.optionhandling.constraints.CommonConstraints;
import elki.utilities.optionhandling.parameterization.Parameterization;
import elki.utilities.optionhandling.parameters.IntParameter;
import elki.utilities.optionhandling.parameters.ObjectParameter;

/**
 * A k-medoids clustering algorithm, implemented as EM-style batch algorithm;
 * known in literature as the "alternate" method.
 * <p>
 * In contrast to PAM, which will in each iteration update one medoid with one
 * (arbitrary) non-medoid, this implementation follows the EM pattern. In the
 * expectation step, the best medoid from the cluster members is chosen; in the
 * M-step, the objects are reassigned to their nearest medoid.
 * <p>
 * This implementation evolved naturally from EM and k-means algorithms.
 * We then came across a similar approach was published by Park and Jun, and
 * made this code match their suggestions (in particular, the default
 * initialization is as proposed by them, despite its shortcomings). But similar
 * ideas were already discussed by Reynolds et al. as a side note, and can be
 * further traced back to Maranzana.
 * <p>
 * In our experiments, it tends to be much faster than PAM, but also find less
 * good solutions, as the medoids are only chosen from the cluster members. This
 * aligns with findings of Reynolds et al. and can be explained with the
 * requirement of the new medoid to cover the entire cluster. Similar
 * observations were also made by Teitz and Bart, 1967, who described its
 * performance as "erratic".
 * <p>
 * Reference:
 * <p>
 * F. E. Maranzana<br>
 * On the location of supply points to minimize transport costs<br>
 * Journal of the Operational Research Society 15.3
 * <p>
 * A. P. Reynolds, G. Richards, B. de la Iglesia, V. J. Rayward-Smith<br>
 * Clustering Rules: A Comparison of Partitioning and Hierarchical Clustering
 * Algorithms<br>
 * J. Math. Model. Algorithms 5(4)
 * <p>
 * H.-S. Park, C.-H. Jun<br>
 * A simple and fast algorithm for K-medoids clustering<br>
 * Expert Systems with Applications 36(2)
 *
 * @author Erich Schubert
 * @since 0.5.0
 *
 * @param <O> vector datatype
 */
@Priority(Priority.SUPPLEMENTARY)
@Reference(authors = "F. E. Maranzana", //
    title = "On the location of supply points to minimize transport costs", //
    booktitle = "Journal of the Operational Research Society 15.3", //
    url = "https://doi.org/10.1057/jors.1964.47", //
    bibkey = "doi:10.1057/jors.1964.47")
@Reference(authors = "A. P. Reynolds, G. Richards, B. de la Iglesia, V. J. Rayward-Smith", //
    title = "Clustering Rules: A Comparison of Partitioning and Hierarchical Clustering Algorithms", //
    booktitle = "J. Math. Model. Algorithms 5(4)", //
    url = "https://doi.org/10.1007/s10852-005-9022-1", //
    bibkey = "DBLP:journals/jmma/ReynoldsRIR06")
@Reference(authors = "H.-S. Park, C.-H. Jun", //
    title = "A simple and fast algorithm for K-medoids clustering", //
    booktitle = "Expert Systems with Applications 36(2)", //
    url = "https://doi.org/10.1016/j.eswa.2008.01.039", //
    bibkey = "DBLP:journals/eswa/ParkJ09")
public class AlternatingKMedoids<O> implements KMedoidsClustering<O> {
  /**
   * The logger for this class.
   */
  private static final Logging LOG = Logging.getLogger(AlternatingKMedoids.class);

  /**
   * Key for statistics logging.
   */
  private static final String KEY = AlternatingKMedoids.class.getName();

  /**
   * Distance function used.
   */
  protected Distance<? super O> distance;

  /**
   * Number of clusters to find.
   */
  protected int k;

  /**
   * Maximum number of iterations.
   */
  protected int maxiter;

  /**
   * Method to choose initial means.
   */
  protected KMedoidsInitialization<O> initializer;

  /**
   * Constructor.
   * 
   * @param distance distance function
   * @param k k parameter
   * @param maxiter Maxiter parameter
   * @param initializer Function to generate the initial means
   */
  public AlternatingKMedoids(Distance<? super O> distance, int k, int maxiter, KMedoidsInitialization<O> initializer) {
    super();
    this.distance = distance;
    this.k = k;
    this.maxiter = maxiter;
    this.initializer = initializer;
  }

  @Override
  public TypeInformation[] getInputTypeRestriction() {
    return TypeUtil.array(distance.getInputTypeRestriction());
  }

  @Override
  public Clustering<MedoidModel> run(Relation<O> relation) {
    return run(relation, k, new QueryBuilder<>(relation, distance).precomputed().distanceQuery());
  }

  @Override
  public Clustering<MedoidModel> run(Relation<O> relation, int k, DistanceQuery<? super O> distQ) {
    // Choose initial medoids
    if(LOG.isStatistics()) {
      LOG.statistics(new StringStatistic(KEY + ".initialization", initializer.toString()));
    }
    final DBIDs ids = relation.getDBIDs();
    ArrayModifiableDBIDs medoids = DBIDUtil.newArray(initializer.chooseInitialMedoids(k, ids, distQ));
    DBIDArrayMIter miter = medoids.iter();
    double[] cost = new double[k];
    WritableIntegerDataStore assignment = DataStoreUtil.makeIntegerStorage(ids, DataStoreFactory.HINT_HOT | DataStoreFactory.HINT_TEMP, 0);

    // Initial assignment to nearest medoids
    // TODO: reuse this information, from the build phase, when possible?
    double tc = AlternateRefinement.assignToNearestCluster(miter, ids, distQ, assignment, cost);
    if(LOG.isStatistics()) {
      LOG.statistics(new DoubleStatistic(KEY + ".iteration-" + 0 + ".cost", tc));
    }

    IndefiniteProgress prog = LOG.isVerbose() ? new IndefiniteProgress("K-Medoids EM iteration", LOG) : null;
    // Refinement phase
    int iteration = 0;
    while(iteration < maxiter || maxiter <= 0) {
      ++iteration;
      boolean changed = false;
      // Try to swap the medoid with a better cluster member:
      for(miter.seek(0); miter.valid(); miter.advance()) {
        changed |= AlternateRefinement.findMedoid(ids, distQ, assignment, miter.getOffset(), miter, cost);
      }
      if(!changed) {
        break; // Converged
      }
      // Reassign
      double nc = AlternateRefinement.assignToNearestCluster(miter, ids, distQ, assignment, cost);
      if(LOG.isStatistics()) {
        LOG.statistics(new DoubleStatistic(KEY + ".iteration-" + iteration + ".cost", nc));
      }
      if(nc > tc) {
        LOG.warning(getClass().getName() + " failed to converge - numerical instability?");
        break;
      }
      tc = nc;
      LOG.incrementProcessed(prog);
    }
    LOG.setCompleted(prog);
    if(LOG.isStatistics()) {
      LOG.statistics(new LongStatistic(KEY + ".iterations", iteration));
      LOG.statistics(new DoubleStatistic(KEY + ".final-cost", tc));
    }

    List<ModifiableDBIDs> clusters = new ArrayList<>();
    for(int i = 0; i < k; i++) {
      clusters.add(DBIDUtil.newArray(relation.size() / k / 2));
    }
    for(DBIDIter iter = ids.iter(); iter.valid(); iter.advance()) {
      clusters.get(assignment.intValue(iter)).add(iter);
    }
    // Wrap result
    Clustering<MedoidModel> result = new Clustering<>();
    Metadata.of(result).setLongName("k-Medoids Clustering");
    for(DBIDArrayIter it = medoids.iter(); it.valid(); it.advance()) {
      result.addToplevelCluster(new Cluster<>(clusters.get(it.getOffset()), new MedoidModel(DBIDUtil.deref(it))));
    }
    return result;
  }

  /**
   * Parameterization class.
   * 
   * @author Erich Schubert
   */
  public static class Par<V> implements Parameterizer {
    /**
     * The number of clusters to find
     */
    protected int k;

    /**
     * The maximum number of iterations
     */
    protected int maxiter;

    /**
     * Initialization method.
     */
    protected KMedoidsInitialization<V> initializer;

    /**
     * The distance function to use.
     */
    protected Distance<? super V> distance;

    @Override
    public void configure(Parameterization config) {
      new ObjectParameter<Distance<? super V>>(Algorithm.Utils.DISTANCE_FUNCTION_ID, Distance.class, EuclideanDistance.class) //
          .grab(config, x -> distance = x);
      new IntParameter(KMeans.K_ID) //
          .addConstraint(CommonConstraints.GREATER_EQUAL_ONE_INT) //
          .grab(config, x -> k = x);
      new ObjectParameter<KMedoidsInitialization<V>>(KMeans.INIT_ID, KMedoidsInitialization.class, RandomlyChosen.class) //
          .grab(config, x -> initializer = x);
      new IntParameter(KMeans.MAXITER_ID, 0) //
          .addConstraint(CommonConstraints.GREATER_EQUAL_ZERO_INT) //
          .grab(config, x -> maxiter = x);
    }

    @Override
    public AlternatingKMedoids<V> make() {
      return new AlternatingKMedoids<>(distance, k, maxiter, initializer);
    }
  }
}
