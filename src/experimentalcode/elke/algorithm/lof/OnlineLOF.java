package experimentalcode.elke.algorithm.lof;

import java.util.Collection;
import java.util.List;
import java.util.Set;

import de.lmu.ifi.dbs.elki.algorithm.outlier.LOF;
import de.lmu.ifi.dbs.elki.data.DatabaseObject;
import de.lmu.ifi.dbs.elki.database.Database;
import de.lmu.ifi.dbs.elki.database.datastore.DataStoreEvent;
import de.lmu.ifi.dbs.elki.database.datastore.DataStoreEvent.Type;
import de.lmu.ifi.dbs.elki.database.datastore.DataStoreListener;
import de.lmu.ifi.dbs.elki.database.datastore.WritableDataStore;
import de.lmu.ifi.dbs.elki.database.ids.ArrayDBIDs;
import de.lmu.ifi.dbs.elki.database.ids.ArrayModifiableDBIDs;
import de.lmu.ifi.dbs.elki.database.ids.DBID;
import de.lmu.ifi.dbs.elki.database.ids.DBIDUtil;
import de.lmu.ifi.dbs.elki.database.ids.DBIDs;
import de.lmu.ifi.dbs.elki.database.ids.ModifiableDBIDs;
import de.lmu.ifi.dbs.elki.database.query.DatabaseQuery;
import de.lmu.ifi.dbs.elki.database.query.DistanceResultPair;
import de.lmu.ifi.dbs.elki.database.query.knn.KNNQuery;
import de.lmu.ifi.dbs.elki.database.query.knn.PreprocessorKNNQuery;
import de.lmu.ifi.dbs.elki.database.query.rknn.RKNNQuery;
import de.lmu.ifi.dbs.elki.distance.distancefunction.DistanceFunction;
import de.lmu.ifi.dbs.elki.distance.distancevalue.NumberDistance;
import de.lmu.ifi.dbs.elki.index.preprocessed.MaterializeKNNPreprocessor;
import de.lmu.ifi.dbs.elki.logging.Logging;
import de.lmu.ifi.dbs.elki.logging.progress.StepProgress;
import de.lmu.ifi.dbs.elki.math.MinMax;
import de.lmu.ifi.dbs.elki.result.outlier.OutlierResult;
import de.lmu.ifi.dbs.elki.result.outlier.OutlierScoreMeta;
import de.lmu.ifi.dbs.elki.result.outlier.QuotientOutlierScoreMeta;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.Parameterizable;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.ListParameterization;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.Parameterization;
import de.lmu.ifi.dbs.elki.utilities.pairs.Pair;
import experimentalcode.elke.index.preprocessed.MaterializeKNNAndRKNNPreprocessor;

// TODO: Elke: comment, add support for deletions
/**
 * @apiviz.has de.lmu.ifi.dbs.elki.algorithm.outlier.LOF.LOFResult oneway - -
 *             updates
 */
public class OnlineLOF<O extends DatabaseObject, D extends NumberDistance<D, ?>> extends LOF<O, D> {
  /**
   * The logger for this class.
   */
  private static final Logging logger = Logging.getLogger(OnlineLOF.class);

  public OnlineLOF(int k, DistanceFunction<O, D> neighborhoodDistanceFunction, DistanceFunction<O, D> reachabilityDistanceFunction) {
    super(k, neighborhoodDistanceFunction, reachabilityDistanceFunction);
  }

  /**
   * Performs the Generalized LOF_SCORE algorithm on the given database by
   * calling {@code #doRunInTime(Database)} and adds a
   * {@link LOFDatabaseListener} to the database.
   */
  @Override
  protected OutlierResult runInTime(Database<O> database) throws IllegalStateException {
    StepProgress stepprog = logger.isVerbose() ? new StepProgress("OnlineLOF", 3) : null;

    Pair<Pair<KNNQuery<O, D>, KNNQuery<O, D>>, Pair<RKNNQuery<O, D>, RKNNQuery<O, D>>> queries = getKNNAndRkNNQueries(database, stepprog);
    KNNQuery<O, D> kNNRefer = queries.getFirst().getFirst();
    KNNQuery<O, D> kNNReach = queries.getFirst().getSecond();
    RKNNQuery<O, D> rkNNRefer = queries.getSecond().getFirst();
    RKNNQuery<O, D> rkNNReach = queries.getSecond().getSecond();

    LOFResult<O, D> lofResult = super.doRunInTime(database, kNNRefer, kNNReach, stepprog);
    lofResult.setRkNNRefer(rkNNRefer);
    lofResult.setRkNNReach(rkNNReach);

    // add listener
    DataStoreListener<DBID> l = new LOFDatabaseListener(lofResult);
    ((PreprocessorKNNQuery<O, D>) lofResult.getKNNRefer()).getPreprocessor().addDataStoreListener(l);
    ((PreprocessorKNNQuery<O, D>) lofResult.getKNNReach()).getPreprocessor().addDataStoreListener(l);

    return lofResult.getResult();
  }

  /**
   * Get the kNN and rkNN queries for the algorithm.
   * 
   * @param database Database
   * @param stepprog Progress logger
   * @return the kNN and rkNN queries
   */
  private Pair<Pair<KNNQuery<O, D>, KNNQuery<O, D>>, Pair<RKNNQuery<O, D>, RKNNQuery<O, D>>> getKNNAndRkNNQueries(Database<O> database, StepProgress stepprog) {
    // Use "HEAVY" flag, since this is an online algorithm
    KNNQuery<O, D> kNNRefer = database.getKNNQuery(neighborhoodDistanceFunction, k, DatabaseQuery.HINT_HEAVY_USE, DatabaseQuery.HINT_OPTIMIZED_ONLY, DatabaseQuery.HINT_NO_CACHE);
    KNNQuery<O, D> kNNReach = database.getKNNQuery(reachabilityDistanceFunction, k, DatabaseQuery.HINT_HEAVY_USE, DatabaseQuery.HINT_OPTIMIZED_ONLY, DatabaseQuery.HINT_NO_CACHE);
    RKNNQuery<O, D> rkNNRefer = database.getRKNNQuery(neighborhoodDistanceFunction, DatabaseQuery.HINT_HEAVY_USE, DatabaseQuery.HINT_OPTIMIZED_ONLY, DatabaseQuery.HINT_NO_CACHE);
    RKNNQuery<O, D> rkNNReach = database.getRKNNQuery(reachabilityDistanceFunction, DatabaseQuery.HINT_HEAVY_USE, DatabaseQuery.HINT_OPTIMIZED_ONLY, DatabaseQuery.HINT_NO_CACHE);

    // No optimized kNN query or RkNN query - use a preprocessor!
    if(kNNRefer == null || rkNNRefer == null) {
      if(stepprog != null) {
        stepprog.beginStep(1, "Materializing neighborhood w.r.t. reference neighborhood distance function.", logger);
      }
      MaterializeKNNAndRKNNPreprocessor<O, D> preproc = new MaterializeKNNAndRKNNPreprocessor<O, D>(database, neighborhoodDistanceFunction, k);
      kNNRefer = preproc.getKNNQuery(database, neighborhoodDistanceFunction, k, DatabaseQuery.HINT_HEAVY_USE);
      rkNNRefer = preproc.getRKNNQuery(database, neighborhoodDistanceFunction, DatabaseQuery.HINT_HEAVY_USE);
      // add as index
      database.addIndex(preproc);
    }
    else {
      if(stepprog != null) {
        stepprog.beginStep(1, "Optimized neighborhood w.r.t. reference neighborhood distance function provided by database.", logger);
      }
    }

    if(kNNReach == null || rkNNReach == null) {
      if(stepprog != null) {
        stepprog.beginStep(2, "Materializing neighborhood w.r.t. reachability distance function.", logger);
      }
      ListParameterization config = new ListParameterization();
      config.addParameter(MaterializeKNNPreprocessor.Factory.DISTANCE_FUNCTION_ID, reachabilityDistanceFunction);
      config.addParameter(MaterializeKNNPreprocessor.Factory.K_ID, k);
      MaterializeKNNAndRKNNPreprocessor<O, D> preproc = new MaterializeKNNAndRKNNPreprocessor<O, D>(database, reachabilityDistanceFunction, k);
      kNNReach = preproc.getKNNQuery(database, reachabilityDistanceFunction, k, DatabaseQuery.HINT_HEAVY_USE);
      rkNNReach = preproc.getRKNNQuery(database, reachabilityDistanceFunction, DatabaseQuery.HINT_HEAVY_USE);
      // add as index
      database.addIndex(preproc);
    }

    Pair<KNNQuery<O, D>, KNNQuery<O, D>> kNNPair = new Pair<KNNQuery<O, D>, KNNQuery<O, D>>(kNNRefer, kNNReach);
    Pair<RKNNQuery<O, D>, RKNNQuery<O, D>> rkNNPair = new Pair<RKNNQuery<O, D>, RKNNQuery<O, D>>(rkNNRefer, rkNNReach);

    return new Pair<Pair<KNNQuery<O, D>, KNNQuery<O, D>>, Pair<RKNNQuery<O, D>, RKNNQuery<O, D>>>(kNNPair, rkNNPair);
  }

  void knnsChanged(DataStoreEvent<DBID> e1, DataStoreEvent<DBID> e2, LOFResult<O, D> lofResult) {
    if(! e1.getTypes().equals(e2.getTypes())) {
      throw new UnsupportedOperationException("Event types do not fit: " + e1.getTypes() + " != " + e2.getTypes());
    }

    System.out.println("YYY contentChanged  : " + e1.getTypes());
    System.out.println("YYY contentChanged 1: " + e1.getObjects());
    System.out.println("YYY contentChanged 2: " + e2.getObjects());

    Set<Type> eventTypes = e1.getTypes();
    if(eventTypes.size() != 2) {
      throw new UnsupportedOperationException("Unsupported event types: " + eventTypes);
    }
    if(eventTypes.contains(Type.DELETE) && eventTypes.contains(Type.UPDATE)) {
      Collection<DBID> deletions = e1.getObjects().get(DataStoreEvent.Type.DELETE);
      Collection<DBID> updates1 = e1.getObjects().get(DataStoreEvent.Type.UPDATE);
      Collection<DBID> updates2 = e2.getObjects().get(DataStoreEvent.Type.UPDATE);

      ModifiableDBIDs deletion_ids = DBIDUtil.newArray(deletions.size());
      ModifiableDBIDs updates1_ids = DBIDUtil.newArray(updates1.size());
      ModifiableDBIDs updates2_ids = DBIDUtil.newArray(updates2.size());
      deletion_ids.addAll(deletions);
      updates1_ids.addAll(updates1);
      updates2_ids.addAll(updates2);

      knnsRemoved(deletion_ids, updates1_ids, updates2_ids);
    }
    else if(eventTypes.contains(Type.INSERT) && eventTypes.contains(Type.UPDATE)) {
      Collection<DBID> insertions = e1.getObjects().get(DataStoreEvent.Type.INSERT);
      Collection<DBID> updates1 = e1.getObjects().get(DataStoreEvent.Type.UPDATE);
      Collection<DBID> updates2 = e2.getObjects().get(DataStoreEvent.Type.UPDATE);

      ModifiableDBIDs insertion_ids = DBIDUtil.newArray(insertions.size());
      ModifiableDBIDs updates1_ids = DBIDUtil.newArray(updates1.size());
      ModifiableDBIDs updates2_ids = DBIDUtil.newArray(updates2.size());
      insertion_ids.addAll(insertions);
      updates1_ids.addAll(updates1);
      updates2_ids.addAll(updates2);

      knnsInserted(insertion_ids, updates1_ids, updates2_ids, lofResult);
    }
    else {
      throw new UnsupportedOperationException("Unsupported event types: " + eventTypes);
    }
  }

  private void knnsInserted(DBIDs insertions, DBIDs updates1, DBIDs updates2, LOFResult<O, D> lofResult) {
    StepProgress stepprog = logger.isVerbose() ? new StepProgress(3) : null;

    // recompute lrds
    if(stepprog != null) {
      stepprog.beginStep(1, "Recompute LRDs.", logger);
    }
    ArrayDBIDs lrd_ids = DBIDUtil.ensureArray(DBIDUtil.union(insertions, updates2));
    List<List<DistanceResultPair<D>>> reachDistRKNNs = lofResult.getRkNNReach().getRKNNForBulkDBIDs(lrd_ids, k);
    ArrayDBIDs affected_lrd_id_candidates = mergeIDs(reachDistRKNNs, lrd_ids);
    ArrayDBIDs affected_lrd_ids = DBIDUtil.newArray(affected_lrd_id_candidates.size());
    WritableDataStore<Double> new_lrds = computeLRDs(affected_lrd_id_candidates, lofResult.getKNNReach());
    for(DBID id : affected_lrd_id_candidates) {
      Double new_lrd = new_lrds.get(id);
      Double old_lrd = lofResult.getLrds().get(id);
      if(old_lrd == null || !old_lrd.equals(new_lrd)) {
        lofResult.getLrds().put(id, new_lrd);
        affected_lrd_ids.add(id);
      }
    }

    // recompute lofs
    if(stepprog != null) {
      stepprog.beginStep(2, "Recompute LOFS.", logger);
    }
    List<List<DistanceResultPair<D>>> primDistRKNNs = lofResult.getRkNNRefer().getRKNNForBulkDBIDs(affected_lrd_ids, k);
    ArrayDBIDs affected_lof_ids = mergeIDs(primDistRKNNs, affected_lrd_ids, insertions, updates1);
    recomputeLOFs(affected_lof_ids, lofResult);

    // todo fire result event
    if(stepprog != null) {
      stepprog.beginStep(3, "Inform listeners.", logger);
    }

    if(stepprog != null) {
      stepprog.setCompleted(logger);
    }

    System.out.println("YYY affected_lrd_ids_candidates = " + affected_lrd_id_candidates);
    System.out.println("YYY affected_lrd_ids            = " + affected_lrd_ids);
    System.out.println("YYY affected_lof_ids            = " + affected_lof_ids);

  }

  private void knnsRemoved(DBIDs deletions, DBIDs updates1, DBIDs updates2) {
    System.out.println("deletions " + deletions);
    System.out.println("updates1  " + updates1);
    System.out.println("updates2  " + updates2);
  }

  private void recomputeLOFs(DBIDs ids, LOFResult<O, D> lofResult) {
    Pair<WritableDataStore<Double>, MinMax<Double>> lofsAndMax = computeLOFs(ids, lofResult.getLrds(), lofResult.getKNNRefer());
    WritableDataStore<Double> new_lofs = lofsAndMax.getFirst();
    for(DBID id : ids) {
      lofResult.getLofs().put(id, new_lofs.get(id));
    }
    // track the maximum value for normalization.
    MinMax<Double> new_lofminmax = lofsAndMax.getSecond();

    // Actualize result
    if((new_lofminmax.getMax() != null && lofResult.getResult().getOutlierMeta().getActualMaximum() < new_lofminmax.getMax()) || (new_lofminmax.getMin() != null && lofResult.getResult().getOutlierMeta().getActualMinimum() > new_lofminmax.getMin())) {
      OutlierScoreMeta scoreMeta = new QuotientOutlierScoreMeta(new_lofminmax.getMin(), new_lofminmax.getMax(), 0.0, Double.POSITIVE_INFINITY, 1.0);
      lofResult.getResult().setOutlierMeta(scoreMeta);
    }
  }

  /**
   * Merges the ids of the query result with the specified ids.
   * 
   * @param queryResults the list of query results
   * @param ids the list of ids
   * @return a set containing the ids of the query result and the specified ids
   */
  private ArrayModifiableDBIDs mergeIDs(List<List<DistanceResultPair<D>>> queryResults, DBIDs... ids) {
    ModifiableDBIDs result = DBIDUtil.newTreeSet();
    for(DBIDs dbids : ids) {
      result.addDBIDs(dbids);
    }
    for(List<DistanceResultPair<D>> queryResult : queryResults) {
      for(DistanceResultPair<D> qr : queryResult) {
        result.add(qr.getID());
      }
    }
    return DBIDUtil.newArray(result);
  }

  /**
   * Encapsulates a database listener for the LOF algorithm.
   */
  private class LOFDatabaseListener implements DataStoreListener<DBID> {
    /**
     * Holds the first event of one of the both preprocessors. The online
     * algorithm is waiting until both events have been received, i.e. until
     * both preprocessors are up to date.
     */
    private DataStoreEvent<DBID> firstEventReceived;

    /**
     * Holds the result of a former run of the LOF algorithm.
     */
    private LOFResult<O, D> lofResult;

    /**
     * Constructs a database listener for the LOF algorithm.
     * 
     * @param lofResult the result of a former run of the LOF algorithm
     */
    public LOFDatabaseListener(LOFResult<O, D> lofResult) {
      this.lofResult = lofResult;
    }

    @Override
    public void contentChanged(DataStoreEvent<DBID> e) {
      MaterializeKNNPreprocessor<O, D> p1 = ((PreprocessorKNNQuery<O, D>) lofResult.getKNNRefer()).getPreprocessor();
      MaterializeKNNPreprocessor<O, D> p2 = ((PreprocessorKNNQuery<O, D>) lofResult.getKNNReach()).getPreprocessor();

      if(firstEventReceived == null) {
        if(e.getSource().equals(p1) && e.getSource().equals(p2)) {
          knnsChanged(e, e, lofResult);
        }
        else {
          firstEventReceived = e;
        }
      }
      else {
        if(e.getSource().equals(p1) && firstEventReceived.getSource().equals(p2)) {
          knnsChanged(e, firstEventReceived, lofResult);
          firstEventReceived = null;
        }
        else if(e.getSource().equals(p2) && firstEventReceived.getSource().equals(p1)) {
          knnsChanged(firstEventReceived, e, lofResult);
          firstEventReceived = null;
        }
        else {
          throw new UnsupportedOperationException("Event sources do not fit!");
        }
      }
    }
  }

  /**
   * Factory method for {@link Parameterizable}
   * 
   * @param config Parameterization
   * @return KNN outlier detection algorithm
   */
  public static <O extends DatabaseObject, D extends NumberDistance<D, ?>> OnlineLOF<O, D> parameterize(Parameterization config) {
    int k = getParameterK(config);
    DistanceFunction<O, D> distanceFunction = getParameterDistanceFunction(config);
    DistanceFunction<O, D> reachabilityDistanceFunction = getParameterReachabilityDistanceFunction(config);
    if(reachabilityDistanceFunction == null) {
      reachabilityDistanceFunction = distanceFunction;
    }
    if(config.hasErrors()) {
      return null;
    }
    return new OnlineLOF<O, D>(k + (objectIsInKNN ? 0 : 1), distanceFunction, reachabilityDistanceFunction);
  }

  @Override
  protected Logging getLogger() {
    return logger;
  }
}