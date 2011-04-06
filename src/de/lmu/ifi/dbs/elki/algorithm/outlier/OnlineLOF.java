package de.lmu.ifi.dbs.elki.algorithm.outlier;

import java.util.List;

import de.lmu.ifi.dbs.elki.data.DatabaseObject;
import de.lmu.ifi.dbs.elki.database.Database;
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
import de.lmu.ifi.dbs.elki.index.preprocessed.knn.KNNChangeEvent;
import de.lmu.ifi.dbs.elki.index.preprocessed.knn.KNNListener;
import de.lmu.ifi.dbs.elki.index.preprocessed.knn.MaterializeKNNAndRKNNPreprocessor;
import de.lmu.ifi.dbs.elki.index.preprocessed.knn.MaterializeKNNPreprocessor;
import de.lmu.ifi.dbs.elki.logging.Logging;
import de.lmu.ifi.dbs.elki.logging.progress.StepProgress;
import de.lmu.ifi.dbs.elki.math.MinMax;
import de.lmu.ifi.dbs.elki.result.outlier.BasicOutlierScoreMeta;
import de.lmu.ifi.dbs.elki.result.outlier.OutlierResult;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.Parameterizable;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.ListParameterization;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.Parameterization;
import de.lmu.ifi.dbs.elki.utilities.pairs.Pair;

/**
 * Incremental version of the {@link LOF} Algorithm, supports insertions and removals.
 * 
 * @author Elke Achtert
 * 
 * @apiviz.has LOF.LOFResult oneway - - updates
 */
// TODO: related to publication? 
public class OnlineLOF<O extends DatabaseObject, D extends NumberDistance<D, ?>> extends LOF<O, D> {
  /**
   * The logger for this class.
   */
  static final Logging logger = Logging.getLogger(OnlineLOF.class);

  /**
   * Constructor.
   * 
   * @param k the value of k
   * @param neighborhoodDistanceFunction the neighborhood distance function
   * @param reachabilityDistanceFunction the reachability distance function
   */
  public OnlineLOF(int k, DistanceFunction<O, D> neighborhoodDistanceFunction, DistanceFunction<O, D> reachabilityDistanceFunction) {
    super(k, neighborhoodDistanceFunction, reachabilityDistanceFunction);
  }

  /**
   * Performs the Generalized LOF_SCORE algorithm on the given database by
   * calling {@code #doRunInTime(Database)} and adds a {@link LOFKNNListener} to
   * the preprocessors.
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
    KNNListener l = new LOFKNNListener(lofResult);
    ((PreprocessorKNNQuery<O, D>) lofResult.getKNNRefer()).getPreprocessor().addKNNListener(l);
    ((PreprocessorKNNQuery<O, D>) lofResult.getKNNReach()).getPreprocessor().addKNNListener(l);

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

  /**
   * Encapsulates a listener for changes of kNNs used in the online LOF
   * algorithm.
   */
  private class LOFKNNListener implements KNNListener {
    /**
     * Holds the first event of one of the both preprocessors. The online
     * algorithm is waiting until both events have been received, i.e. until
     * both preprocessors are up to date.
     */
    private KNNChangeEvent firstEventReceived;

    /**
     * Holds the result of a former run of the LOF algorithm.
     */
    private LOFResult<O, D> lofResult;

    /**
     * Constructs a listener for the LOF algorithm.
     * 
     * @param lofResult the result of a former run of the LOF algorithm
     */
    public LOFKNNListener(LOFResult<O, D> lofResult) {
      this.lofResult = lofResult;
    }

    @Override
    public void kNNsChanged(KNNChangeEvent e) {
      MaterializeKNNPreprocessor<O, D> p1 = ((PreprocessorKNNQuery<O, D>) lofResult.getKNNRefer()).getPreprocessor();
      MaterializeKNNPreprocessor<O, D> p2 = ((PreprocessorKNNQuery<O, D>) lofResult.getKNNReach()).getPreprocessor();

      if(firstEventReceived == null) {
        if(e.getSource().equals(p1) && e.getSource().equals(p2)) {
          kNNsChanged(e, e);
        }
        else {
          firstEventReceived = e;
        }
      }
      else {
        if(e.getSource().equals(p1) && firstEventReceived.getSource().equals(p2)) {
          kNNsChanged(e, firstEventReceived);
          firstEventReceived = null;
        }
        else if(e.getSource().equals(p2) && firstEventReceived.getSource().equals(p1)) {
          kNNsChanged(firstEventReceived, e);
          firstEventReceived = null;
        }
        else {
          throw new UnsupportedOperationException("Event sources do not fit!");
        }
      }
    }

    /**
     * Invoked after the events of both preprocessors have been received, i.e.
     * until both preprocessors are up to date.
     * 
     * @param e1 the change event of the first preprocessor
     * @param e2 the change event of the second preprocessor
     */
    private void kNNsChanged(KNNChangeEvent e1, KNNChangeEvent e2) {
      if(!e1.getType().equals(e2.getType())) {
        throw new UnsupportedOperationException("Event types do not fit: " + e1.getType() + " != " + e2.getType());
      }
      if(!e1.getObjects().equals(e2.getObjects())) {
        throw new UnsupportedOperationException("Objects do not fit: " + e1.getObjects() + " != " + e2.getObjects());
      }

      if(e1.getType().equals(KNNChangeEvent.Type.DELETE)) {
        kNNsRemoved(e1.getObjects(), e1.getUpdates(), e2.getUpdates(), lofResult);
      }
      else if(e1.getType().equals(KNNChangeEvent.Type.INSERT)) {
        kNNsInserted(e1.getObjects(), e1.getUpdates(), e2.getUpdates(), lofResult);
      }
      else {
        throw new UnsupportedOperationException("Unsupported event type: " + e1.getType());
      }
    }

    /**
     * Invoked after kNNs have been inserted and updated, updates the result.
     * 
     * @param insertions the ids of the newly inserted neighborhoods
     * @param updates1 the ids of the updated neighborhood w.r.t. the
     *        neighborhood distance function
     * @param updates2 the ids of the updated neighborhood w.r.t. the
     *        reachability distance function
     * @param lofResult the result of the former LOF run
     */
    private void kNNsInserted(DBIDs insertions, DBIDs updates1, DBIDs updates2, LOFResult<O, D> lofResult) {
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

      // fire result changed
      if(stepprog != null) {
        stepprog.beginStep(3, "Inform listeners.", logger);
      }
      lofResult.getResult().getHierarchy().resultChanged(lofResult.getResult());

      if(stepprog != null) {
        stepprog.setCompleted(logger);
      }
    }

    /**
     * Invoked after kNNs have been removed and updated, updates the result.
     * 
     * @param deletions the ids of the removed neighborhoods
     * @param updates1 the ids of the updated neighborhood w.r.t. the
     *        neighborhood distance function
     * @param updates2 the ids of the updated neighborhood w.r.t. the
     *        reachability distance function
     * @param lofResult the result of the former LOF run
     */
    private void kNNsRemoved(DBIDs deletions, DBIDs updates1, DBIDs updates2, LOFResult<O, D> lofResult) {
      StepProgress stepprog = logger.isVerbose() ? new StepProgress(4) : null;

      // delete lrds and lofs
      if(stepprog != null) {
        stepprog.beginStep(1, "Delete old LRDs and LOFs.", logger);
      }
      for(DBID id : deletions) {
        lofResult.getLrds().delete(id);
        lofResult.getLofs().delete(id);
      }

      // recompute lrds
      if(stepprog != null) {
        stepprog.beginStep(2, "Recompute LRDs.", logger);
      }
      ArrayDBIDs lrd_ids = DBIDUtil.ensureArray(updates2);
      List<List<DistanceResultPair<D>>> reachDistRKNNs = lofResult.getRkNNReach().getRKNNForBulkDBIDs(lrd_ids, k);
      ArrayDBIDs affected_lrd_id_candidates = mergeIDs(reachDistRKNNs, lrd_ids);
      ArrayDBIDs affected_lrd_ids = DBIDUtil.newArray(affected_lrd_id_candidates.size());
      WritableDataStore<Double> new_lrds = computeLRDs(affected_lrd_id_candidates, lofResult.getKNNReach());
      for(DBID id : affected_lrd_id_candidates) {
        Double new_lrd = new_lrds.get(id);
        Double old_lrd = lofResult.getLrds().get(id);
        if(!old_lrd.equals(new_lrd)) {
          lofResult.getLrds().put(id, new_lrd);
          affected_lrd_ids.add(id);
        }
      }

      // recompute lofs
      if(stepprog != null) {
        stepprog.beginStep(3, "Recompute LOFS.", logger);
      }
      List<List<DistanceResultPair<D>>> primDistRKNNs = lofResult.getRkNNRefer().getRKNNForBulkDBIDs(affected_lrd_ids, k);
      ArrayDBIDs affected_lof_ids = mergeIDs(primDistRKNNs, affected_lrd_ids, updates1);
      recomputeLOFs(affected_lof_ids, lofResult);

      // fire result changed
      if(stepprog != null) {
        stepprog.beginStep(4, "Inform listeners.", logger);
      }
      lofResult.getResult().getHierarchy().resultChanged(lofResult.getResult());

      if(stepprog != null) {
        stepprog.setCompleted(logger);
      }
    }

    /**
     * Merges the ids of the query result with the specified ids.
     * 
     * @param queryResults the list of query results
     * @param ids the list of ids
     * @return a set containing the ids of the query result and the specified
     *         ids
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
     * Recomputes the lofs of the specified ids.
     * 
     * @param ids the ids of the lofs to be recomputed
     * @param lofResult the result of the former LOF run
     */
    private void recomputeLOFs(DBIDs ids, LOFResult<O, D> lofResult) {
      Pair<WritableDataStore<Double>, MinMax<Double>> lofsAndMax = computeLOFs(ids, lofResult.getLrds(), lofResult.getKNNRefer());
      WritableDataStore<Double> new_lofs = lofsAndMax.getFirst();
      for(DBID id : ids) {
        lofResult.getLofs().put(id, new_lofs.get(id));
      }
      // track the maximum value for normalization.
      MinMax<Double> new_lofminmax = lofsAndMax.getSecond();

      // Actualize meta info
      if(new_lofminmax.getMax() != null && lofResult.getResult().getOutlierMeta().getActualMaximum() < new_lofminmax.getMax()) {
        BasicOutlierScoreMeta scoreMeta = (BasicOutlierScoreMeta) lofResult.getResult().getOutlierMeta();
        scoreMeta.setActualMaximum(new_lofminmax.getMax());
      }

      if(new_lofminmax.getMin() != null && lofResult.getResult().getOutlierMeta().getActualMinimum() > new_lofminmax.getMin()) {
        BasicOutlierScoreMeta scoreMeta = (BasicOutlierScoreMeta) lofResult.getResult().getOutlierMeta();
        scoreMeta.setActualMinimum(new_lofminmax.getMin());
      }
    }
  }

  /**
   * Factory method for {@link Parameterizable}.
   * 
   * @param config Parameterization
   * @return OnlineLOF algorithm
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