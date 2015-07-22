package de.lmu.ifi.dbs.elki.algorithm.clustering.kmeans;

import de.lmu.ifi.dbs.elki.algorithm.AbstractDistanceBasedAlgorithm;
import de.lmu.ifi.dbs.elki.algorithm.clustering.ClusteringAlgorithm;
import de.lmu.ifi.dbs.elki.algorithm.clustering.kmeans.initialization.KMedoidsInitialization;
import de.lmu.ifi.dbs.elki.algorithm.clustering.kmeans.initialization.PAMInitialMeans;
import de.lmu.ifi.dbs.elki.data.Cluster;
import de.lmu.ifi.dbs.elki.data.Clustering;
import de.lmu.ifi.dbs.elki.data.model.MedoidModel;
import de.lmu.ifi.dbs.elki.data.type.TypeInformation;
import de.lmu.ifi.dbs.elki.data.type.TypeUtil;
import de.lmu.ifi.dbs.elki.database.Database;
import de.lmu.ifi.dbs.elki.database.datastore.DataStoreFactory;
import de.lmu.ifi.dbs.elki.database.datastore.DataStoreUtil;
import de.lmu.ifi.dbs.elki.database.datastore.WritableDoubleDataStore;
import de.lmu.ifi.dbs.elki.database.datastore.WritableIntegerDataStore;
import de.lmu.ifi.dbs.elki.database.ids.ArrayDBIDs;
import de.lmu.ifi.dbs.elki.database.ids.ArrayModifiableDBIDs;
import de.lmu.ifi.dbs.elki.database.ids.DBIDArrayIter;
import de.lmu.ifi.dbs.elki.database.ids.DBIDIter;
import de.lmu.ifi.dbs.elki.database.ids.DBIDUtil;
import de.lmu.ifi.dbs.elki.database.ids.DBIDVar;
import de.lmu.ifi.dbs.elki.database.ids.DBIDs;
import de.lmu.ifi.dbs.elki.database.query.DatabaseQuery;
import de.lmu.ifi.dbs.elki.database.query.distance.DistanceQuery;
import de.lmu.ifi.dbs.elki.database.relation.Relation;
import de.lmu.ifi.dbs.elki.distance.distancefunction.DistanceFunction;
import de.lmu.ifi.dbs.elki.index.distancematrix.PrecomputedDistanceMatrix;
import de.lmu.ifi.dbs.elki.logging.Logging;
import de.lmu.ifi.dbs.elki.logging.progress.IndefiniteProgress;
import de.lmu.ifi.dbs.elki.logging.statistics.LongStatistic;
import de.lmu.ifi.dbs.elki.logging.statistics.StringStatistic;
import de.lmu.ifi.dbs.elki.utilities.documentation.Reference;
import de.lmu.ifi.dbs.elki.utilities.documentation.Title;
import de.lmu.ifi.dbs.elki.utilities.exceptions.AbortException;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.constraints.CommonConstraints;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.Parameterization;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.IntParameter;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.ObjectParameter;

/**
 * The original PAM algorithm or k-medoids clustering, as proposed by Kaufman
 * and Rousseeuw in "Partitioning Around Medoids".
 * 
 * Reference:
 * <p>
 * Clustering my means of Medoids<br />
 * Kaufman, L. and Rousseeuw, P.J.<br />
 * in: Statistical Data Analysis Based on the L1-Norm and Related Methods
 * </p>
 * 
 * @author Erich Schubert
 * 
 * @apiviz.has MedoidModel
 * @apiviz.composedOf KMedoidsInitialization
 * 
 * @param <V> vector datatype
 */
@Title("Partioning Around Medoids")
@Reference(title = "Clustering by means of Medoids", //
authors = "Kaufman, L. and Rousseeuw, P.J.", //
booktitle = "Statistical Data Analysis Based on the L1-Norm and Related Methods")
public class KMedoidsPAM<V> extends AbstractDistanceBasedAlgorithm<V, Clustering<MedoidModel>>implements ClusteringAlgorithm<Clustering<MedoidModel>> {
  /**
   * The logger for this class.
   */
  private static final Logging LOG = Logging.getLogger(KMedoidsPAM.class);

  /**
   * Key for statistics logging.
   */
  private static final String KEY = KMedoidsPAM.class.getName();

  /**
   * The number of clusters to produce.
   */
  protected int k;

  /**
   * The maximum number of iterations.
   */
  protected int maxiter;

  /**
   * Method to choose initial means.
   */
  protected KMedoidsInitialization<V> initializer;

  /**
   * Constructor.
   * 
   * @param distanceFunction distance function
   * @param k k parameter
   * @param maxiter Maxiter parameter
   * @param initializer Function to generate the initial means
   */
  public KMedoidsPAM(DistanceFunction<? super V> distanceFunction, int k, int maxiter, KMedoidsInitialization<V> initializer) {
    super(distanceFunction);
    this.k = k;
    this.maxiter = maxiter;
    this.initializer = initializer;
  }

  /**
   * Run k-medoids
   * 
   * @param database Database
   * @param relation relation to use
   * @return result
   */
  public Clustering<MedoidModel> run(Database database, Relation<V> relation) {
    if(relation.size() <= 0) {
      return new Clustering<>("PAM Clustering", "pam-clustering");
    }
    DistanceQuery<V> distQ = database.getDistanceQuery(relation, getDistanceFunction(), DatabaseQuery.HINT_OPTIMIZED_ONLY);
    if(distQ == null) {
      LOG.verbose("Adding a distance matrix index to accelerate PAM.");
      PrecomputedDistanceMatrix<V> idx = new PrecomputedDistanceMatrix<V>(relation, getDistanceFunction());
      idx.initialize();
      distQ = idx.getDistanceQuery(getDistanceFunction());
    }
    if(distQ == null) {
      distQ = database.getDistanceQuery(relation, getDistanceFunction());
      LOG.warning("PAM may be slow, because we do not have a precomputed distance matrix available.");
    }
    DBIDs ids = relation.getDBIDs();
    // Choose initial medoids
    if(LOG.isStatistics()) {
      LOG.statistics(new StringStatistic(KEY + ".initialization", initializer.toString()));
    }
    ArrayModifiableDBIDs medoids = DBIDUtil.newArray(initializer.chooseInitialMedoids(k, ids, distQ));
    if(medoids.size() != k) {
      throw new AbortException("Initializer " + initializer.toString() + " did not return " + k + " means, but " + medoids.size());
    }

    // Setup cluster assignment store
    WritableIntegerDataStore assignment = DataStoreUtil.makeIntegerStorage(ids, DataStoreFactory.HINT_HOT | DataStoreFactory.HINT_TEMP, -1);
    runPAMOptimization(distQ, ids, medoids, assignment);

    // Rewrap result
    int[] sizes = new int[k];
    for(DBIDIter iter = ids.iter(); iter.valid(); iter.advance()) {
      sizes[assignment.intValue(iter)] += 1;
    }
    ArrayModifiableDBIDs[] clusters = new ArrayModifiableDBIDs[k];
    for(int i = 0; i < k; i++) {
      clusters[i] = DBIDUtil.newArray(sizes[i]);
    }
    for(DBIDIter iter = ids.iter(); iter.valid(); iter.advance()) {
      clusters[assignment.intValue(iter)].add(iter);
    }

    // Wrap result
    Clustering<MedoidModel> result = new Clustering<>("PAM Clustering", "pam-clustering");
    for(DBIDArrayIter it = medoids.iter(); it.valid(); it.advance()) {
      MedoidModel model = new MedoidModel(DBIDUtil.deref(it));
      result.addToplevelCluster(new Cluster<>(clusters[it.getOffset()], model));
    }
    return result;
  }

  /**
   * Run the PAM optimization phase.
   * 
   * @param distQ Distance query
   * @param ids IDs to process
   * @param medoids Medoids list
   * @param assignment Cluster assignment
   */
  protected void runPAMOptimization(DistanceQuery<V> distQ, DBIDs ids, ArrayModifiableDBIDs medoids, WritableIntegerDataStore assignment) {
    WritableDoubleDataStore nearest = DataStoreUtil.makeDoubleStorage(ids, DataStoreFactory.HINT_HOT | DataStoreFactory.HINT_TEMP);
    WritableDoubleDataStore second = DataStoreUtil.makeDoubleStorage(ids, DataStoreFactory.HINT_HOT | DataStoreFactory.HINT_TEMP);
    // Initial assignment to nearest medoids
    // TODO: reuse this information, from the build phase, when possible?
    assignToNearestCluster(medoids, ids, nearest, second, assignment, distQ);

    IndefiniteProgress prog = LOG.isVerbose() ? new IndefiniteProgress("PAM iteration", LOG) : null;
    // Swap phase
    DBIDVar bestid = DBIDUtil.newVar();
    DBIDArrayIter m = medoids.iter(), i = medoids.iter();
    int iteration = 0;
    for(; maxiter <= 0 || iteration < maxiter; iteration++) {
      LOG.incrementProcessed(prog);
      // Try to swap a medoid with a better non-medoid member:
      double best = Double.POSITIVE_INFINITY;
      int bestcluster = -1;
      // Iterate over all objects, per cluster.
      for(DBIDIter h = ids.iter(); h.valid(); h.advance()) {
        final int pm = assignment.intValue(h);
        m.seek(pm);
        if(DBIDUtil.equal(m, h)) {
          continue; // Only not-selected items
        }
        // h is a non-medoid currently in cluster of medoid m.
        double cost = 0;
        for(DBIDIter j = ids.iter(); j.valid(); j.advance()) {
          final int pi = assignment.intValue(j);
          i.seek(pi); // Medoid j is assigned to.
          if(DBIDUtil.equal(i, j)) {
            continue; // Only not-selected items
          }
          // This is an improvement over the original PAM. We do not consider
          // arbitrary pairs (i,h) and points j, but only those where i
          // is the current nearest medoid of j. Since we remembered the
          // distance to the second nearest medoid, we do not need to consider
          // any other option.
          double distcur = nearest.doubleValue(j); // Current assignment.
          // = distance (j, i), because j is assigned to i
          double dist_h = distQ.distance(j, h); // Possible reassignment.
          if(pi == pm) { // Within the cluster of h and j
            // Distance to second best medoid for j
            double distsec = second.doubleValue(j);
            cost += (dist_h < distsec) ? //
            // Case 1b1) j is closer to h
            dist_h - distcur
            // Case 1b2) j would switch to a third medoid
            : distsec - distcur;
          }
          else {
            // Case 1c) j is closer to h than its current medoid
            if(dist_h < distcur) {
              cost += dist_h - distcur;
            }
            // else Case 1a): j is closer to i than h and m, so no change.
          }
        }
        if(cost < best) {
          best = cost;
          bestid.set(h);
          bestcluster = pm;
        }
      }
      if(LOG.isDebugging()) {
        LOG.debug("Best cost: " + best);
      }
      if(best >= 0.) {
        break;
      }
      medoids.set(bestcluster, bestid);
      // Reassign
      // TODO: can we save some of these computations?
      assignToNearestCluster(medoids, ids, nearest, second, assignment, distQ);
    }
    LOG.setCompleted(prog);
    if(LOG.isStatistics()) {
      LOG.statistics(new LongStatistic(KEY + ".iterations", iteration));
    }
  }

  /**
   * Returns a list of clusters. The k<sup>th</sup> cluster contains the ids of
   * those FeatureVectors, that are nearest to the k<sup>th</sup> mean.
   * 
   * @param means Object centroids
   * @param ids Object ids
   * @param nearest Distance to nearest medoid
   * @param second Distance to second nearest medoid
   * @param assignment Cluster assignment
   * @param distQ distance query
   * @return true when any object was reassigned
   */
  protected boolean assignToNearestCluster(ArrayDBIDs means, DBIDs ids, WritableDoubleDataStore nearest, WritableDoubleDataStore second, WritableIntegerDataStore assignment, DistanceQuery<V> distQ) {
    boolean changed = false;
    assert(means.size() == k);
    DBIDArrayIter miter = means.iter();
    for(DBIDIter iditer = distQ.getRelation().iterDBIDs(); iditer.valid(); iditer.advance()) {
      double mindist = Double.POSITIVE_INFINITY,
          mindist2 = Double.POSITIVE_INFINITY;
      int minIndex = -1;
      for(int i = 0; i < k; i++) {
        double dist = distQ.distance(iditer, miter.seek(i));
        if(dist < mindist) {
          minIndex = i;
          mindist2 = mindist;
          mindist = dist;
        }
        else if(dist < mindist2) {
          mindist2 = dist;
        }
      }
      changed |= (assignment.put(iditer, minIndex) != minIndex);
      nearest.put(iditer, mindist);
      second.put(iditer, mindist2);
    }
    return changed;
  }

  @Override
  public TypeInformation[] getInputTypeRestriction() {
    return TypeUtil.array(getDistanceFunction().getInputTypeRestriction());
  }

  @Override
  protected Logging getLogger() {
    return LOG;
  }

  /**
   * Parameterization class.
   * 
   * @author Erich Schubert
   * 
   * @apiviz.exclude
   */
  public static class Parameterizer<V> extends AbstractDistanceBasedAlgorithm.Parameterizer<V> {
    /**
     * The number of clusters to produce.
     */
    protected int k;

    /**
     * The maximum number of iterations.
     */
    protected int maxiter;

    /**
     * Method to choose initial means.
     */
    protected KMedoidsInitialization<V> initializer;

    @Override
    protected void makeOptions(Parameterization config) {
      super.makeOptions(config);
      IntParameter kP = new IntParameter(KMeans.K_ID) //
      .addConstraint(CommonConstraints.GREATER_EQUAL_ONE_INT);
      if(config.grab(kP)) {
        k = kP.intValue();
      }

      ObjectParameter<KMedoidsInitialization<V>> initialP = new ObjectParameter<>(KMeans.INIT_ID, KMedoidsInitialization.class, PAMInitialMeans.class);
      if(config.grab(initialP)) {
        initializer = initialP.instantiateClass(config);
      }

      IntParameter maxiterP = new IntParameter(KMeans.MAXITER_ID, 0) //
      .addConstraint(CommonConstraints.GREATER_EQUAL_ZERO_INT);
      if(config.grab(maxiterP)) {
        maxiter = maxiterP.intValue();
      }
    }

    @Override
    protected KMedoidsPAM<V> makeInstance() {
      return new KMedoidsPAM<>(distanceFunction, k, maxiter, initializer);
    }
  }
}
