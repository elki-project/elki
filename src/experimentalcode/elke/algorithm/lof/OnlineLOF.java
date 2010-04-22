package experimentalcode.elke.algorithm.lof;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.logging.Level;

import de.lmu.ifi.dbs.elki.algorithm.outlier.LOF;
import de.lmu.ifi.dbs.elki.data.DatabaseObject;
import de.lmu.ifi.dbs.elki.database.Database;
import de.lmu.ifi.dbs.elki.database.DatabaseEvent;
import de.lmu.ifi.dbs.elki.database.DatabaseListener;
import de.lmu.ifi.dbs.elki.database.DistanceResultPair;
import de.lmu.ifi.dbs.elki.distance.distancefunction.DistanceFunction;
import de.lmu.ifi.dbs.elki.distance.distancevalue.NumberDistance;
import de.lmu.ifi.dbs.elki.logging.progress.StepProgress;
import de.lmu.ifi.dbs.elki.result.outlier.OutlierResult;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.Parameterization;

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
   * Runs the super method and adds a listener to the database.
   */
  @Override
  protected OutlierResult runInTime(Database<O> database) throws IllegalStateException {
    // todo weg
    logger.getWrappedLogger().setLevel(Level.OFF);

    OutlierResult result = super.runInTime(database);
    HashMap<Integer, List<DistanceResultPair<D>>> knn1 = preprocessor1.getMaterialized();
    HashMap<Integer, List<DistanceResultPair<D>>> knn2 = getDistanceFunction() != reachabilityDistanceFunction ? preprocessor2.getMaterialized() : knn1;
    database.addDatabaseListener(new LOFDatabaseListener(result, knn1, knn2));
    return result;
  }

  void insert(List<Integer> ids, Database<O> database, HashMap<Integer, List<DistanceResultPair<D>>> kNN1, HashMap<Integer, List<DistanceResultPair<D>>> kNN2) {
    // todo weg
    logger.getWrappedLogger().setLevel(Level.ALL);

    StepProgress stepprog = logger.isVerbose() ? new StepProgress(4) : null;

    DistanceFunction<O, D> distanceFunction = getDistanceFunction();

    // get neighbors and reverse nearest neighbors of each new object w.r.t.
    // primary distance
    if(stepprog != null) {
      stepprog.beginStep(1, "New Insertions ocurred, update kNN w.r.t. primary distance.", logger);
    }
    List<Integer> rkNN1_ids = update_kNNs(ids, database, kNN1, getDistanceFunction());

    List<Integer> rkNN2_ids = null;
    if(getDistanceFunction() != reachabilityDistanceFunction) {
      if(stepprog != null) {
        stepprog.beginStep(2, "Update kNN w.r.t. reachability distance.", logger);
        rkNN2_ids = update_kNNs(ids, database, kNN2, reachabilityDistanceFunction);
      }
    }
    else {
      if(stepprog != null) {
        stepprog.beginStep(2, "Reusing kNN of primary distance.", logger);
        rkNN2_ids = rkNN1_ids;
        // kNN2 = kNN1;
      }
    }

    {
      if(stepprog != null) {
        stepprog.beginStep(3, "Recompute LRDs.", logger);
      }
      List<List<DistanceResultPair<D>>> rRkNNs = database.bulkReverseKNNQueryForID(rkNN2_ids, k + 1, reachabilityDistanceFunction);
      Set<Integer> affectedObjects = new TreeSet<Integer>(rkNN2_ids);
      affectedObjects.addAll(getIDs(rRkNNs));
      if(logger.isDebugging()) {
        StringBuffer msg = new StringBuffer();
        msg.append("\naffected Objects " + affectedObjects);
        logger.debug(msg.toString());
      }
      HashMap<Integer, Double> lrds = computeLRDs(new ArrayList<Integer>(affectedObjects), kNN2);
    }

    if(stepprog != null) {
      stepprog.beginStep(4, "Recompute LOFs.", logger);
    }

    /************************
     * 
     */
    // for(Integer id : affectedObjects) {
    //
    // List<DistanceResultPair<D>> knn_old = knn1.get(id);
    // List<DistanceResultPair<D>> knn_new = database.kNNQueryForID(id, k + 1,
    // distanceFunction);
    // if(knn_old == null || !knn_old.equals(knn_new)) {
    // System.out.println();
    // System.out.println("kNN_old[" + id + "] = " + knn_old);
    // System.out.println("kNN_new[" + id + "] = " + knn_new);
    // }
    // }

    if(stepprog != null) {
      stepprog.setCompleted(logger);
    }
  }

  /**
   * Removes the first occurrence of the element with the specified id from the
   * list.
   * 
   * @param id the id to be removed
   * @param list the list
   */
  private void removeID(Integer id, List<DistanceResultPair<D>> list) {
    int index = -1;
    for(int j = 0; j < list.size(); j++) {
      DistanceResultPair<D> pair = list.get(j);
      if(pair.second == id) {
        index = j;
        break;
      }
    }
    list.remove(index);
  }

  private class LOFDatabaseListener implements DatabaseListener<O> {
    OutlierResult result;

    HashMap<Integer, List<DistanceResultPair<D>>> knn1;

    HashMap<Integer, List<DistanceResultPair<D>>> knn2;

    public LOFDatabaseListener(OutlierResult result, HashMap<Integer, List<DistanceResultPair<D>>> knn1, HashMap<Integer, List<DistanceResultPair<D>>> knn2) {
      this.result = result;
      this.knn1 = knn1;
      this.knn2 = knn2;
    }

    @Override
    public void objectsChanged(DatabaseEvent<O> e) {
      throw new UnsupportedOperationException("TODO " + e);
    }

    @Override
    public void objectsInserted(DatabaseEvent<O> e) {
      insert(e.getObjectIDs(), e.getDatabase(), knn1, knn2);
    }

    @Override
    public void objectsRemoved(DatabaseEvent<O> e) {
      throw new UnsupportedOperationException("TODO " + e);
    }
  }

  private List<Integer> update_kNNs(List<Integer> ids, Database<O> db, HashMap<Integer, List<DistanceResultPair<D>>> kNNMap, DistanceFunction<O, D> distanceFunction) {
    List<List<DistanceResultPair<D>>> rkNNs = db.bulkReverseKNNQueryForID(ids, k + 1, distanceFunction);
    List<Integer> rkNN_ids = getIDs(rkNNs);

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

    // List<List<DistanceResultPair<D>>> rRkNNs =
    // db.bulkReverseKNNQueryForID(new ArrayList<Integer>(rkNN_ids), k + 1,
    // distanceFunction);
    // Set<Integer> rRkNN_ids = new TreeSet<Integer>();
    // for(int i = 0; i < rRkNNs.size(); i++) {
    // for(DistanceResultPair<D> rRkNN : rRkNNs.get(i)) {
    // rRkNN_ids.add(rRkNN.second);
    // }
    // }
    //
    // Set<Integer> affectedObjects = new TreeSet<Integer>(rkNN_ids);
    // affectedObjects.addAll(rRkNN_ids);

    // if(logger.isDebugging()) {
    // StringBuffer msg = new StringBuffer();
    // for(int i = 0; i < ids.size(); i++) {
    // int id = ids.get(i);
    // msg.append("\nkNNs[").append(id).append("] = ").append(kNNs.get(i));
    // msg.append("\nrNNs[").append(id).append("] = ").append(rkNNs.get(i));
    // }

    // msg.append("\n\nRkNN_Ids = ").append(rkNN1_id_set);
    // msg.append("\nRRkNN_Ids = ").append(rRkNN_id_set);
    // msg.append("\naffectedObjects = ").append(affectedObjects);
    // msg.append("\n# affectedObjects = ").append(affectedObjects.size());
    // for(int i = 0; i < rkNN_id_list.size(); i++) {
    // int id = rkNN_id_list.get(i);
    // msg.append("\nrNNs[").append(id).append("] = ").append(rRkNNList.get(i));
    // }

    // msg.append("\n");
    // logger.debug(msg.toString());
  }

  private List<Integer> getIDs(List<List<DistanceResultPair<D>>> queryResults) {
    Set<Integer> ids = new TreeSet<Integer>();
    for(List<DistanceResultPair<D>> queryResult : queryResults) {
      for(DistanceResultPair<D> qr : queryResult) {
        ids.add(qr.second);
      }
    }

    return new ArrayList<Integer>(ids);
  }
  
  

}
