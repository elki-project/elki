package de.lmu.ifi.dbs.elki.algorithm.outlier;

import java.util.List;

import de.lmu.ifi.dbs.elki.algorithm.AbstractDistanceBasedAlgorithm;
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
import de.lmu.ifi.dbs.elki.database.query.distance.DistanceQuery;
import de.lmu.ifi.dbs.elki.database.query.knn.KNNQuery;
import de.lmu.ifi.dbs.elki.database.query.knn.PreprocessorKNNQuery;
import de.lmu.ifi.dbs.elki.database.query.rknn.RKNNQuery;
import de.lmu.ifi.dbs.elki.database.relation.Relation;
import de.lmu.ifi.dbs.elki.distance.distancefunction.DistanceFunction;
import de.lmu.ifi.dbs.elki.distance.distancevalue.NumberDistance;
import de.lmu.ifi.dbs.elki.index.preprocessed.knn.AbstractMaterializeKNNPreprocessor;
import de.lmu.ifi.dbs.elki.index.preprocessed.knn.KNNChangeEvent;
import de.lmu.ifi.dbs.elki.index.preprocessed.knn.KNNListener;
import de.lmu.ifi.dbs.elki.index.preprocessed.knn.MaterializeKNNAndRKNNPreprocessor;
import de.lmu.ifi.dbs.elki.index.preprocessed.knn.MaterializeKNNPreprocessor;
import de.lmu.ifi.dbs.elki.logging.Logging;
import de.lmu.ifi.dbs.elki.logging.progress.StepProgress;
import de.lmu.ifi.dbs.elki.math.DoubleMinMax;
import de.lmu.ifi.dbs.elki.result.outlier.BasicOutlierScoreMeta;
import de.lmu.ifi.dbs.elki.result.outlier.OutlierResult;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.constraints.GreaterConstraint;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.ListParameterization;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.Parameterization;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.IntParameter;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.ObjectParameter;
import de.lmu.ifi.dbs.elki.utilities.pairs.Pair;

/**
 * Incremental version of the {@link LOF} Algorithm, supports insertions and
 * removals.
 * 
 * @author Elke Achtert
 * 
 * @apiviz.has LOF.LOFResult oneway - - updates
 */
// TODO: related to publication? 
public class OnlineLOF<O, D extends NumberDistance<D, ?>> extends LOF<O, D> {
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
  public OnlineLOF(int k, DistanceFunction<? super O, D> neighborhoodDistanceFunction, DistanceFunction<? super O, D> reachabilityDistanceFunction) {
    super(k, neighborhoodDistanceFunction, reachabilityDistanceFunction);
  }

  /**
   * Performs the Generalized LOF_SCORE algorithm on the given database by
   * calling {@code #doRunInTime(Database)} and adds a {@link LOFKNNListener} to
   * the preprocessors.
   */
  @Override
  public OutlierResult run(Database database, Relation<O> relation) {
    StepProgress stepprog = logger.isVerbose() ? new StepProgress("OnlineLOF", 3) : null;

    Pair<Pair<KNNQuery<O, D>, KNNQuery<O, D>>, Pair<RKNNQuery<O, D>, RKNNQuery<O, D>>> queries = getKNNAndRkNNQueries(relation, stepprog);
    KNNQuery<O, D> kNNRefer = queries.getFirst().getFirst();
    KNNQuery<O, D> kNNReach = queries.getFirst().getSecond();
    RKNNQuery<O, D> rkNNRefer = queries.getSecond().getFirst();
    RKNNQuery<O, D> rkNNReach = queries.getSecond().getSecond();

    LOFResult<O, D> lofResult = super.doRunInTime(database, kNNRefer, kNNReach, stepprog);
    lofResult.setRkNNRefer(rkNNRefer);
    lofResult.setRkNNReach(rkNNReach);

    // add listener
    KNNListener l = new LOFKNNListener(lofResult);
    ((MaterializeKNNPreprocessor<O, D>)((PreprocessorKNNQuery<O, D>) lofResult.getKNNRefer()).getPreprocessor()).addKNNListener(l);
    ((MaterializeKNNPreprocessor<O, D>)((PreprocessorKNNQuery<O, D>) lofResult.getKNNReach()).getPreprocessor()).addKNNListener(l);

    return lofResult.getResult();
  }

  /**
   * Get the kNN and rkNN queries for the algorithm.
   * 
   * @param relaton Data
   * @param stepprog Progress logger
   * @return the kNN and rkNN queries
   */
  private Pair<Pair<KNNQuery<O, D>, KNNQuery<O, D>>, Pair<RKNNQuery<O, D>, RKNNQuery<O, D>>> getKNNAndRkNNQueries(Relation<O> relation, StepProgress stepprog) {
    // Use "HEAVY" flag, since this is an online algorithm
    KNNQuery<O, D> kNNRefer = relation.getDatabase().getKNNQuery(relation, neighborhoodDistanceFunction, k, DatabaseQuery.HINT_HEAVY_USE, DatabaseQuery.HINT_OPTIMIZED_ONLY, DatabaseQuery.HINT_NO_CACHE);
    RKNNQuery<O, D> rkNNRefer = relation.getDatabase().getRKNNQuery(relation, neighborhoodDistanceFunction, DatabaseQuery.HINT_HEAVY_USE, DatabaseQuery.HINT_OPTIMIZED_ONLY, DatabaseQuery.HINT_NO_CACHE);

    // No optimized kNN query or RkNN query - use a preprocessor!
    if(kNNRefer == null || rkNNRefer == null) {
      if(stepprog != null) {
        stepprog.beginStep(1, "Materializing neighborhood w.r.t. reference neighborhood distance function.", logger);
      }
      MaterializeKNNAndRKNNPreprocessor<O, D> preproc = new MaterializeKNNAndRKNNPreprocessor<O, D>(relation, neighborhoodDistanceFunction, k);
      DistanceQuery<O, D> ndq = relation.getDatabase().getDistanceQuery(relation, neighborhoodDistanceFunction);
      kNNRefer = preproc.getKNNQuery(ndq, k, DatabaseQuery.HINT_HEAVY_USE);
      rkNNRefer = preproc.getRKNNQuery(ndq, k, DatabaseQuery.HINT_HEAVY_USE);
      // add as index
      relation.getDatabase().addIndex(preproc);
    }
    else {
      if(stepprog != null) {
        stepprog.beginStep(1, "Optimized neighborhood w.r.t. reference neighborhood distance function provided by database.", logger);
      }
    }

    KNNQuery<O, D> kNNReach = relation.getDatabase().getKNNQuery(relation, reachabilityDistanceFunction, k, DatabaseQuery.HINT_HEAVY_USE, DatabaseQuery.HINT_OPTIMIZED_ONLY, DatabaseQuery.HINT_NO_CACHE);
    RKNNQuery<O, D> rkNNReach = relation.getDatabase().getRKNNQuery(relation, reachabilityDistanceFunction, DatabaseQuery.HINT_HEAVY_USE, DatabaseQuery.HINT_OPTIMIZED_ONLY, DatabaseQuery.HINT_NO_CACHE);
    if(kNNReach == null || rkNNReach == null) {
      if(stepprog != null) {
        stepprog.beginStep(2, "Materializing neighborhood w.r.t. reachability distance function.", logger);
      }
      ListParameterization config = new ListParameterization();
      config.addParameter(AbstractMaterializeKNNPreprocessor.Factory.DISTANCE_FUNCTION_ID, reachabilityDistanceFunction);
      config.addParameter(AbstractMaterializeKNNPreprocessor.Factory.K_ID, k);
      MaterializeKNNAndRKNNPreprocessor<O, D> preproc = new MaterializeKNNAndRKNNPreprocessor<O, D>(relation, reachabilityDistanceFunction, k);
      DistanceQuery<O, D> rdq = relation.getDatabase().getDistanceQuery(relation, reachabilityDistanceFunction);
      kNNReach = preproc.getKNNQuery(rdq, k, DatabaseQuery.HINT_HEAVY_USE);
      rkNNReach = preproc.getRKNNQuery(rdq, k, DatabaseQuery.HINT_HEAVY_USE);
      // add as index
      relation.getDatabase().addIndex(preproc);
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
      AbstractMaterializeKNNPreprocessor<O, D> p1 = ((PreprocessorKNNQuery<O, D>) lofResult.getKNNRefer()).getPreprocessor();
      AbstractMaterializeKNNPreprocessor<O, D> p2 = ((PreprocessorKNNQuery<O, D>) lofResult.getKNNReach()).getPreprocessor();

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
          result.add(qr.getDBID());
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
      Pair<WritableDataStore<Double>, DoubleMinMax> lofsAndMax = computeLOFs(ids, lofResult.getLrds(), lofResult.getKNNRefer());
      WritableDataStore<Double> new_lofs = lofsAndMax.getFirst();
      for(DBID id : ids) {
        lofResult.getLofs().put(id, new_lofs.get(id));
      }
      // track the maximum value for normalization.
      DoubleMinMax new_lofminmax = lofsAndMax.getSecond();

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

  @Override
  protected Logging getLogger() {
    return logger;
  }

  /**
   * Parameterization class.
   *
   * @author Erich Schubert
   *
   * @apiviz.exclude
   */
  public static class Parameterizer<O, D extends NumberDistance<D, ?>> extends AbstractDistanceBasedAlgorithm.Parameterizer<O, D> {
    /**
     * The neighborhood size to use
     */
    protected int k = 2;

    /**
     * Neighborhood distance function.
     */
    protected DistanceFunction<O, D> neighborhoodDistanceFunction = null;

    /**
     * Reachability distance function.
     */
    protected DistanceFunction<O, D> reachabilityDistanceFunction = null;

    @Override
    protected void makeOptions(Parameterization config) {
      super.makeOptions(config);

      final IntParameter pK = new IntParameter(K_ID, new GreaterConstraint(1));
      if(config.grab(pK)) {
        k = pK.getValue();
      }

      final ObjectParameter<DistanceFunction<O, D>> reachDistP = new ObjectParameter<DistanceFunction<O, D>>(REACHABILITY_DISTANCE_FUNCTION_ID, DistanceFunction.class, true);
      if(config.grab(reachDistP)) {
        reachabilityDistanceFunction = reachDistP.instantiateClass(config);
      }
    }

    @Override
    protected OnlineLOF<O, D> makeInstance() {
      // Default is to re-use the same distance
      DistanceFunction<O, D> rdist = (reachabilityDistanceFunction != null) ? reachabilityDistanceFunction : distanceFunction;
      return new OnlineLOF<O, D>(k + (objectIsInKNN ? 0 : 1), distanceFunction, rdist);
    }
  }
}