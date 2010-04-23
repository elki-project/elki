package de.lmu.ifi.dbs.elki.algorithm.outlier;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.logging.Level;

import de.lmu.ifi.dbs.elki.data.DatabaseObject;
import de.lmu.ifi.dbs.elki.database.Database;
import de.lmu.ifi.dbs.elki.database.DatabaseEvent;
import de.lmu.ifi.dbs.elki.database.DatabaseListener;
import de.lmu.ifi.dbs.elki.database.DistanceResultPair;
import de.lmu.ifi.dbs.elki.distance.distancefunction.DistanceFunction;
import de.lmu.ifi.dbs.elki.distance.distancevalue.NumberDistance;
import de.lmu.ifi.dbs.elki.logging.progress.StepProgress;
import de.lmu.ifi.dbs.elki.math.MinMax;
import de.lmu.ifi.dbs.elki.result.outlier.OutlierResult;
import de.lmu.ifi.dbs.elki.result.outlier.OutlierScoreMeta;
import de.lmu.ifi.dbs.elki.result.outlier.QuotientOutlierScoreMeta;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.Parameterization;
import de.lmu.ifi.dbs.elki.utilities.pairs.Pair;

public class OnlineLOF<O extends DatabaseObject, D extends NumberDistance<D, ?>> extends LOF<O, D> {

  /**
   * Constructor, adhering to
   * {@link de.lmu.ifi.dbs.elki.utilities.optionhandling.Parameterizable}
   * 
   * @param config Parameterization
   */
  public OnlineLOF(Parameterization config) {
    super(config);
  }

  /**
   * Performs the Generalized LOF_SCORE algorithm on the given database by
   * calling {@code #doRunInTime(Database)} and adds a
   * {@link LOFDatabaseListener} to the database.
   */
  @Override
  protected OutlierResult runInTime(Database<O> database) throws IllegalStateException {
    LOFResult lofResult = super.doRunInTime(database);

    // add db listener
    database.addDatabaseListener(new LOFDatabaseListener(lofResult));

    return lofResult.getResult();
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
  void insert(List<Integer> ids, Database<O> database, LOFResult lofResult) {
    StepProgress stepprog = logger.isVerbose() ? new StepProgress(4) : null;

    // get neighbors and reverse nearest neighbors of each new object w.r.t.
    // primary distance
    if(stepprog != null) {
      stepprog.beginStep(1, "New Insertions ocurred, update kNN w.r.t. primary distance.", logger);
    }
    List<Integer> rkNN1_ids = update_kNNs(ids, database, lofResult.getNeigh1(), getDistanceFunction());

    List<Integer> rkNN2_ids = null;
    if(getDistanceFunction() != reachabilityDistanceFunction) {
      if(stepprog != null) {
        stepprog.beginStep(2, "Update kNN w.r.t. reachability distance.", logger);
      }
      rkNN2_ids = update_kNNs(ids, database, lofResult.getNeigh2(), reachabilityDistanceFunction);
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
    List<List<DistanceResultPair<D>>> rRkNNs = database.bulkReverseKNNQueryForID(rkNN2_ids, k + 1, reachabilityDistanceFunction);
    List<Integer> affectedObjects = mergeIDs(rRkNNs, rkNN2_ids);
    if(logger.isDebugging()) {
      StringBuffer msg = new StringBuffer();
      msg.append("\n" + affectedObjects.size() + " affected Objects for LRDs " + affectedObjects);
      logger.debug(msg.toString());
    }
    HashMap<Integer, Double> new_lrds = computeLRDs(affectedObjects, lofResult.getNeigh2());
    for(Integer id : new_lrds.keySet()) {
      lofResult.getLrds().put(id, new_lrds.get(id));
    }

    if(stepprog != null) {
      stepprog.beginStep(4, "Recompute LOFs.", logger);
    }
    List<Integer> rRkNN_ids = mergeIDs(rRkNNs, new ArrayList<Integer>());
    List<List<DistanceResultPair<D>>> rrRkNNs = database.bulkReverseKNNQueryForID(rRkNN_ids, k + 1, getDistanceFunction());
    affectedObjects = mergeIDs(rrRkNNs, affectedObjects);
    if(logger.isDebugging()) {
      StringBuffer msg = new StringBuffer();
      msg.append("\n" + affectedObjects.size() + " affected Objects for LOFs " + affectedObjects);
      logger.debug(msg.toString());
    }
    Pair<HashMap<Integer, Double>, MinMax<Double>> lofsAndMax = computeLOFs(affectedObjects, lofResult.getLrds(), lofResult.getNeigh1());
    HashMap<Integer, Double> new_lofs = lofsAndMax.getFirst();
    for(Integer id : new_lofs.keySet()) {
      lofResult.getLofs().put(id, new_lofs.get(id));
    }
    // track the maximum value for normalization.
    MinMax<Double> new_lofminmax = lofsAndMax.getSecond();

    // Actualize result
    if(lofResult.getResult().getOutlierMeta().getActualMaximum() < new_lofminmax.getMax() || lofResult.getResult().getOutlierMeta().getActualMinimum() > new_lofminmax.getMin()) {
      OutlierScoreMeta scoreMeta = new QuotientOutlierScoreMeta(new_lofminmax.getMin(), new_lofminmax.getMax(), 0.0, Double.POSITIVE_INFINITY, 1.0);
      lofResult.getResult().setOutlierMeta(scoreMeta);
    }

    if(stepprog != null) {
      stepprog.setCompleted(logger);
    }
  }

  private List<Integer> update_kNNs(List<Integer> ids, Database<O> db, HashMap<Integer, List<DistanceResultPair<D>>> kNNMap, DistanceFunction<O, D> distanceFunction) {
    List<List<DistanceResultPair<D>>> rkNNs = db.bulkReverseKNNQueryForID(ids, k + 1, distanceFunction);
    List<Integer> rkNN_ids = mergeIDs(rkNNs, new ArrayList<Integer>());

    List<List<DistanceResultPair<D>>> kNNs = db.bulkKNNQueryForID(rkNN_ids, k + 1, distanceFunction);

    StringBuffer msg = new StringBuffer();
    for(int i = 0; i < rkNN_ids.size(); i++) {
      int id = rkNN_ids.get(i);
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
  private List<Integer> mergeIDs(List<List<DistanceResultPair<D>>> queryResults, List<Integer> ids) {
    Set<Integer> result = new TreeSet<Integer>();
    result.addAll(ids);
    for(List<DistanceResultPair<D>> queryResult : queryResults) {
      for(DistanceResultPair<D> qr : queryResult) {
        result.add(qr.second);
      }
    }
    return new ArrayList<Integer>(result);
  }

  /**
   * Encapsulates a database listener for the LOF algorithm.
   */
  private class LOFDatabaseListener implements DatabaseListener<O> {
    /**
     * Holds the result of a former run of the LOF algorithm.
     */
    private LOFResult lofResult;

    /**
     * Constructs a database listener for the LOF algorithm.
     * 
     * @param lofResult the result of a former run of the LOF algorithm
     */
    public LOFDatabaseListener(LOFResult lofResult) {
      this.lofResult = lofResult;
    }

    @Override
    public void objectsChanged(DatabaseEvent<O> e) {
      throw new UnsupportedOperationException("TODO " + e);
    }

    /**
     * Invoked after an object has been inserted into the database. Calls
     * {@link OnlineLOF#insert(List, Database, de.lmu.ifi.dbs.elki.algorithm.outlier.LOF.LOFResult)
     * .
     */
    @Override
    public void objectsInserted(DatabaseEvent<O> e) {
      insert(e.getObjectIDs(), e.getDatabase(), lofResult);
    }

    @Override
    public void objectsRemoved(DatabaseEvent<O> e) {
      throw new UnsupportedOperationException("TODO " + e);
    }
  }

}
