package experimentalcode.elke.algorithm.lof;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;

import de.lmu.ifi.dbs.elki.algorithm.MaterializeDistances;
import de.lmu.ifi.dbs.elki.algorithm.outlier.LOF;
import de.lmu.ifi.dbs.elki.data.DatabaseObject;
import de.lmu.ifi.dbs.elki.database.Database;
import de.lmu.ifi.dbs.elki.database.DistanceResultPair;
import de.lmu.ifi.dbs.elki.database.datastore.DataStoreEvent;
import de.lmu.ifi.dbs.elki.database.datastore.DataStoreListener;
import de.lmu.ifi.dbs.elki.database.datastore.WritableDataStore;
import de.lmu.ifi.dbs.elki.database.ids.ArrayDBIDs;
import de.lmu.ifi.dbs.elki.database.ids.ArrayModifiableDBIDs;
import de.lmu.ifi.dbs.elki.database.ids.DBID;
import de.lmu.ifi.dbs.elki.database.ids.DBIDUtil;
import de.lmu.ifi.dbs.elki.database.ids.DBIDs;
import de.lmu.ifi.dbs.elki.database.ids.ModifiableDBIDs;
import de.lmu.ifi.dbs.elki.database.query.DatabaseQuery;
import de.lmu.ifi.dbs.elki.database.query.distance.DistanceQuery;
import de.lmu.ifi.dbs.elki.database.query.knn.KNNQuery;
import de.lmu.ifi.dbs.elki.database.query.knn.KNNQueryFactory;
import de.lmu.ifi.dbs.elki.database.query.knn.PreprocessorKNNQueryFactory;
import de.lmu.ifi.dbs.elki.database.query.rknn.RKNNQuery;
import de.lmu.ifi.dbs.elki.distance.distancefunction.DistanceFunction;
import de.lmu.ifi.dbs.elki.distance.distancevalue.NumberDistance;
import de.lmu.ifi.dbs.elki.logging.Logging;
import de.lmu.ifi.dbs.elki.logging.progress.StepProgress;
import de.lmu.ifi.dbs.elki.math.MinMax;
import de.lmu.ifi.dbs.elki.preprocessing.MaterializeKNNPreprocessor;
import de.lmu.ifi.dbs.elki.preprocessing.MaterializeKNNPreprocessor.Instance;
import de.lmu.ifi.dbs.elki.result.outlier.OutlierResult;
import de.lmu.ifi.dbs.elki.result.outlier.OutlierScoreMeta;
import de.lmu.ifi.dbs.elki.result.outlier.QuotientOutlierScoreMeta;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.Parameterizable;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.Parameterization;
import de.lmu.ifi.dbs.elki.utilities.pairs.Pair;

// TODO: Elke: comment, add support for deletions
public class OnlineLOF<O extends DatabaseObject, D extends NumberDistance<D, ?>> extends LOF<O, D> {
  /**
   * The logger for this class.
   */
  private static final Logging logger = Logging.getLogger(OnlineLOF.class);

  DistanceQuery<O, D> distQuery;

  DistanceQuery<O, D> reachdistQuery;

  RKNNQuery<O, D> distRQuery;

  RKNNQuery<O, D> reachdistRQuery;

  public OnlineLOF(int k, KNNQueryFactory<O, D> knnQuery1, KNNQueryFactory<O, D> knnQuery2) {
    super(k, knnQuery1, knnQuery2);
  }

  @Override
  protected Class<?> getKNNQueryRestriction() {
    return PreprocessorKNNQueryFactory.class;
  }

  /**
   * Performs the Generalized LOF_SCORE algorithm on the given database by
   * calling {@code #doRunInTime(Database)} and adds a
   * {@link LOFDatabaseListener} to the database.
   */
  @Override
  protected OutlierResult runInTime(Database<O> database) throws IllegalStateException {
    // todo mit hints?
    distQuery = knnQuery1.getDistanceFunction().instantiate(database);
    distRQuery = database.getRKNNQuery(knnQuery1.getDistanceFunction());
    if(knnQuery1.getDistanceFunction() != knnQuery2.getDistanceFunction()) {
      reachdistQuery = knnQuery2.getDistanceFunction().instantiate(database);
      reachdistRQuery = database.getRKNNQuery(knnQuery2.getDistanceFunction());
    }
    else {
      reachdistQuery = distQuery;
      reachdistRQuery = distRQuery;
    }

    LOFResult<O, D> lofResult = super.doRunInTime(database);

    // add listener
    DataStoreListener<DBID> l = new LOFDatabaseListener(lofResult);
    lofResult.getPreproc1().addDataStoreListener(l);
    if(lofResult.getPreproc1() != lofResult.getPreproc2()) {
      lofResult.getPreproc2().addDataStoreListener(l);
    }
    return lofResult.getResult();
  }

  void knnsChanged(DataStoreEvent<DBID> e1, DataStoreEvent<DBID> e2, LOFResult<O, D> lofResult) {
    if(e1.getType() != e2.getType()) {
      throw new UnsupportedOperationException("Event types do not fit: " + e1.getType() + " != " + e2.getType());
    }
    DataStoreEvent.Type eventType = e1.getType();
    if(eventType.equals(DataStoreEvent.Type.DELETE_AND_UPDATE)) {
      Collection<DBID> deletions = e1.getObjects().get(DataStoreEvent.Type.DELETE);
      Collection<DBID> updates1 = e1.getObjects().get(DataStoreEvent.Type.UPDATE);
      Collection<DBID> updates2 = e2.getObjects().get(DataStoreEvent.Type.UPDATE);
      knnsRemoved(deletions, updates1, updates2);
    }
    else if(eventType.equals(DataStoreEvent.Type.INSERT_AND_UPDATE)) {
      Collection<DBID> insertions = e1.getObjects().get(DataStoreEvent.Type.INSERT);
      Collection<DBID> updates1 = e1.getObjects().get(DataStoreEvent.Type.UPDATE);
      Collection<DBID> updates2 = e2.getObjects().get(DataStoreEvent.Type.UPDATE);
      knnsInserted(insertions, updates1, updates2, lofResult);
    }
    else {
      throw new UnsupportedOperationException("Unsupported event types: " + eventType);
    }

    System.out.println("YYY contentChanged  : " + e1.getType());
    System.out.println("YYY contentChanged 1: " + e1.getObjects());
    System.out.println("YYY contentChanged 2: " + e2.getObjects());
  }

  private void knnsInserted(Collection<DBID> insertions, Collection<DBID> updates1, Collection<DBID> updates2, LOFResult<O, D> lofResult) {
    StepProgress stepprog = logger.isVerbose() ? new StepProgress(4) : null;

    if(stepprog != null) {
      stepprog.beginStep(3, "Recompute LRDs.", logger);
    }

//    if(source == lofResult.getPreproc2()) {
//      System.out.println("YYY PREPROC2");
//      if(e2.getType().equals(DataStoreEvent.Type.DELETE_AND_UPDATE)) {
//        Collection<DBID> deletions = e2.getObjects().get(DataStoreEvent.Type.DELETE);
//      } // recompute lrds
//      List<List<DistanceResultPair<D>>> reachDistRKNNs = reachdistRQuery.getRKNNForBulkDBIDs(ids, k);
//      ArrayDBIDs affected_lrd_id_candidates = mergeIDs(reachDistRKNNs, ids);
//      ArrayDBIDs affected_lrd_ids = DBIDUtil.newArray(affected_lrd_id_candidates.size());
//      WritableDataStore<Double> new_lrds = computeLRDs(affected_lrd_id_candidates, lofResult.getNeigh2());
//      for(DBID id : affected_lrd_id_candidates) {
//        Double new_lrd = new_lrds.get(id);
//        Double old_lrd = lofResult.getLrds().get(id);
//        if(old_lrd == null || !old_lrd.equals(new_lrd)) {
//          lofResult.getLrds().put(id, new_lrd);
//          affected_lrd_ids.add(id);
//        }
//      } // recompute lofs
//      List<List<DistanceResultPair<D>>> primDistRKNNs = distRQuery.getRKNNForBulkDBIDs(affected_lrd_ids, k);
//      ArrayDBIDs affected_lof_ids = mergeIDs(primDistRKNNs, affected_lrd_ids);
//      recomputeLOFs(affected_lof_ids, lofResult);
//
//      // todo fire result event
//
//      // XXX for(List<DistanceResultPair<D>> drp : reachDistRKNNs) {
//      System.out.println("YYY reachdist rknn(" + drp.get(0).second + ") = " + doExtractIDs(drp));
//    }
//    System.out.println("YYY affected_lrd_ids_candidates = " + affected_lrd_id_candidates);
//    System.out.println("YYY affected_lrd_ids            = " + affected_lrd_ids);
//
//    for(List<DistanceResultPair<D>> drp : primDistRKNNs) {
//      System.out.println("YYY primdist rknn(" + drp.get(0).second + ") = " + doExtractIDs(drp));
//    }
//    System.out.println("YYY affected_lof_ids            = " + affected_lof_ids); // XXX

    // recompute lofs
    System.out.println("YYY PREPROC1");
    ArrayDBIDs ids = DBIDUtil.newArray();
    ids.addAll(insertions);
    ids.addAll(updates1);
    recomputeLOFs(ids, lofResult);

    if(stepprog != null) {
      stepprog.setCompleted(logger);
    }
  }

  private void knnsRemoved(Collection<DBID> deletions, Collection<DBID> updates1, Collection<DBID> updates2) {
    // TODO
  }

  private void recomputeLOFs(DBIDs ids, LOFResult<O, D> lofResult) {
    Pair<WritableDataStore<Double>, MinMax<Double>> lofsAndMax = computeLOFs(ids, lofResult.getLrds(), lofResult.getNeigh1());
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
   * Is called after the specified objects are inserted into the given database.
   * Determines the affected objects and recalculates the result.
   * 
   * @param ids the ids of the objects inserted
   * @param database the database storing the objects
   * @param lofResult the result of the former run of this algorithm (before the
   *        insertions have occurred)
   */
  void insert(DBIDs ids, Database<O> database, LOFResult<O, D> lofResult) {
    StepProgress stepprog = logger.isVerbose() ? new StepProgress(4) : null;

    ArrayDBIDs idsarray = DBIDUtil.ensureArray(ids);

    // get neighbors and reverse nearest neighbors of each new object w.r.t.
    // primary distance
    if(stepprog != null) {
      stepprog.beginStep(1, "New Insertions ocurred, update kNN w.r.t. primary distance.", logger);
    }
    // FIXME: Get rid of this cast - make an OnlineKNNPreprocessor?
    WritableDataStore<List<DistanceResultPair<D>>> knnstore1 = null;
    // (WritableDataStore<List<DistanceResultPair<D>>>)
    // lofResult.getPreproc1().getMaterialized();
    ArrayModifiableDBIDs rkNN1_ids = update_kNNs(idsarray, database, knnstore1, distQuery, distRQuery);

    ArrayModifiableDBIDs rkNN2_ids = null;
    if(distQuery != reachdistQuery) {
      if(stepprog != null) {
        stepprog.beginStep(2, "Update kNN w.r.t. reachability distance.", logger);
      }
      // FIXME: Get rid of this cast - make an OnlineKNNPreprocessor?
      WritableDataStore<List<DistanceResultPair<D>>> knnstore2 = null;
      // (WritableDataStore<List<DistanceResultPair<D>>>)
      // lofResult.getPreproc2().getMaterialized();
      rkNN2_ids = update_kNNs(idsarray, database, knnstore2, reachdistQuery, reachdistRQuery);
    }
    else {
      if(stepprog != null) {
        stepprog.beginStep(2, "Reusing kNN of primary distance.", logger);
      }
      rkNN2_ids = rkNN1_ids;
    }

    if(stepprog != null) {
      stepprog.beginStep(3, "Recompute LRDs.", logger);
    }
    List<List<DistanceResultPair<D>>> rRkNNs = reachdistRQuery.getRKNNForBulkDBIDs(rkNN2_ids, k + 1);
    DBIDs affectedObjects = mergeIDs(rRkNNs, rkNN2_ids);
    if(logger.isDebugging()) {
      StringBuffer msg = new StringBuffer();
      msg.append("\n" + affectedObjects.size() + " affected Objects for LRDs " + affectedObjects);
      logger.debug(msg.toString());
    }
    WritableDataStore<Double> new_lrds = computeLRDs(affectedObjects, lofResult.getNeigh2());
    for(DBID id : affectedObjects) {
      lofResult.getLrds().put(id, new_lrds.get(id));
    }

    if(stepprog != null) {
      stepprog.beginStep(4, "Recompute LOFs.", logger);
    }
    ArrayModifiableDBIDs rRkNN_ids = mergeIDs(rRkNNs, DBIDUtil.newArray());
    List<List<DistanceResultPair<D>>> rrRkNNs = distRQuery.getRKNNForBulkDBIDs(rRkNN_ids, k + 1);
    affectedObjects = mergeIDs(rrRkNNs, affectedObjects);
    if(logger.isDebugging()) {
      StringBuffer msg = new StringBuffer();
      msg.append("\n" + affectedObjects.size() + " affected Objects for LOFs " + affectedObjects);
      logger.debug(msg.toString());
    }
    Pair<WritableDataStore<Double>, MinMax<Double>> lofsAndMax = computeLOFs(affectedObjects, lofResult.getLrds(), lofResult.getNeigh1());
    WritableDataStore<Double> new_lofs = lofsAndMax.getFirst();
    for(DBID id : affectedObjects) {
      lofResult.getLofs().put(id, new_lofs.get(id));
    }
    // track the maximum value for normalization.
    MinMax<Double> new_lofminmax = lofsAndMax.getSecond();

    // Actualize result
    if((new_lofminmax.getMax() != null && lofResult.getResult().getOutlierMeta().getActualMaximum() < new_lofminmax.getMax()) || (new_lofminmax.getMin() != null && lofResult.getResult().getOutlierMeta().getActualMinimum() > new_lofminmax.getMin())) {
      OutlierScoreMeta scoreMeta = new QuotientOutlierScoreMeta(new_lofminmax.getMin(), new_lofminmax.getMax(), 0.0, Double.POSITIVE_INFINITY, 1.0);
      lofResult.getResult().setOutlierMeta(scoreMeta);
    }

    if(stepprog != null) {
      stepprog.setCompleted(logger);
    }
  }

  private ArrayModifiableDBIDs update_kNNs(ArrayDBIDs ids, Database<O> db, WritableDataStore<List<DistanceResultPair<D>>> kNNMap, DistanceQuery<O, D> distanceFunction, RKNNQuery<O, D> reverseQuery) {
    List<List<DistanceResultPair<D>>> rkNNs = reverseQuery.getRKNNForBulkDBIDs(ids, k + 1);
    ArrayModifiableDBIDs rkNN_ids = mergeIDs(rkNNs, DBIDUtil.EMPTYDBIDS);

    KNNQuery<O, D> knnQuery = db.getKNNQuery(distanceFunction, k + 1, DatabaseQuery.HINT_BULK);
    List<List<DistanceResultPair<D>>> kNNs = knnQuery.getKNNForBulkDBIDs(rkNN_ids, k + 1);

    StringBuffer msg = new StringBuffer();
    for(int i = 0; i < rkNN_ids.size(); i++) {
      DBID id = rkNN_ids.get(i);
      List<DistanceResultPair<D>> old = kNNMap.put(id, kNNs.get(i));

      if(logger.isDebugging()) {
        msg.append("\n");
        if(old != null) {
          msg.append("\nknn_old[" + id + "]" + old);
        }
        msg.append("\nknn_new[" + id + "]" + kNNs.get(i));
      }
    }

    if(logger.isDebugging()) {
      logger.debug(msg.toString());
    }
    return rkNN_ids;
  }

  /**
   * Merges the ids of the query result with the specified ids.
   * 
   * @param queryResults the list of query results
   * @param ids the list of ids
   * @return a set containing the ids of the query result and the specified ids
   */
  private ArrayModifiableDBIDs mergeIDs(List<List<DistanceResultPair<D>>> queryResults, DBIDs ids) {
    ModifiableDBIDs result = DBIDUtil.newTreeSet();
    result.addDBIDs(ids);
    for(List<DistanceResultPair<D>> queryResult : queryResults) {
      for(DistanceResultPair<D> qr : queryResult) {
        result.add(qr.getID());
      }
    }
    return DBIDUtil.newArray(result);
  }

  /**
   * Extracts the ids of the specified query results.
   * 
   * @param queryResults the list of query results
   * @return an array containing the ids of the query result
   */
  private ArrayModifiableDBIDs extractIDs(List<List<DistanceResultPair<D>>> queryResults) {
    ModifiableDBIDs result = DBIDUtil.newTreeSet();
    for(List<DistanceResultPair<D>> queryResult : queryResults) {
      for(DistanceResultPair<D> qr : queryResult) {
        result.add(qr.getID());
      }
    }
    return DBIDUtil.newArray(result);
  }

  /**
   * Extracts the ids of the specified query result.
   * 
   * @param queryResults the list of query results
   * @return an array containing the ids of the query result
   */
  private ArrayModifiableDBIDs doExtractIDs(List<DistanceResultPair<D>> queryResult) {
    ModifiableDBIDs result = DBIDUtil.newTreeSet();
    for(DistanceResultPair<D> drp : queryResult) {
      result.add(drp.getID());
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

    @SuppressWarnings("unchecked")
    @Override
    public void contentChanged(DataStoreEvent<DBID> e) {
      if(firstEventReceived == null) {
        if(e.getSource().equals(lofResult.getPreproc1()) && e.getSource().equals(lofResult.getPreproc2())) {
          knnsChanged(e, e, lofResult);
        }
        else {
          firstEventReceived = e;
        }
      }
      else {
        if(e.getSource().equals(lofResult.getPreproc1()) && firstEventReceived.getSource().equals(lofResult.getPreproc2())) {
          knnsChanged(e, firstEventReceived, lofResult);
          firstEventReceived = null;
        }
        else if(e.getSource().equals(lofResult.getPreproc2()) && firstEventReceived.getSource().equals(lofResult.getPreproc1())) {
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
    KNNQueryFactory<O, D> knnQuery1 = getParameterKNNQuery(config, k + (objectIsInKNN ? 0 : 1), distanceFunction, PreprocessorKNNQueryFactory.class);
    KNNQueryFactory<O, D> knnQuery2 = null;
    if(reachabilityDistanceFunction != null) {
      knnQuery2 = getParameterKNNQuery(config, k + (objectIsInKNN ? 0 : 1), reachabilityDistanceFunction, PreprocessorKNNQueryFactory.class);
    }
    else {
      reachabilityDistanceFunction = distanceFunction;
      knnQuery2 = knnQuery1;
    }
    if(config.hasErrors()) {
      return null;
    }
    return new OnlineLOF<O, D>(k + (objectIsInKNN ? 0 : 1), knnQuery1, knnQuery2);
  }

  @Override
  protected Logging getLogger() {
    return logger;
  }
}