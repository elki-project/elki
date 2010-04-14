package experimentalcode.elke.algorithm.lof;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.logging.Level;

import net.sf.jabref.sql.DbConnectAction;

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

  void insert(List<Integer> ids, Database<O> database, HashMap<Integer, List<DistanceResultPair<D>>> knn1, HashMap<Integer, List<DistanceResultPair<D>>> knn2) {
    // todo weg
    if(isVerbose()) {
      logger.getWrappedLogger().setLevel(Level.ALL);
    }

    StepProgress stepprog = logger.isVerbose() ? new StepProgress(4) : null;

    DistanceFunction<O, D> distanceFunction = getDistanceFunction();

    // get neighbors and reverse nearest neighbors of o
    if(stepprog != null) {
      stepprog.beginStep(1, "New Insertions ocurred, get kNN and RkNN.", logger);
    }
    List<List<DistanceResultPair<D>>> kNNList = database.bulkKNNQueryForID(ids, k + 1, distanceFunction);
    List<List<DistanceResultPair<D>>> rkNNList = database.bulkReverseKNNQueryForID(ids, k + 1, distanceFunction);
    Set<Integer> rkNN_id_set = new TreeSet<Integer>();

    for(int i = 0; i < ids.size(); i++) {
      int id = ids.get(i);
      removeID(id, kNNList.get(i));
      removeID(id, rkNNList.get(i));
      for(DistanceResultPair<D> rknn : rkNNList.get(i)) {
        rkNN_id_set.add(rknn.second);
      }
    }

    List<Integer> rkNN_id_list = new ArrayList<Integer>(rkNN_id_set);
    List<List<DistanceResultPair<D>>> rRkNNList = database.bulkReverseKNNQueryForID(rkNN_id_list, k + 1, distanceFunction);
    Set<Integer> rRkNN_id_set = new TreeSet<Integer>();
    for(int i = 0; i < rkNN_id_list.size(); i++) {
      for(DistanceResultPair<D> rnn : rRkNNList.get(i)) {
        rRkNN_id_set.add(rnn.second);
      }
    }

    Set<Integer> affectedObjects = new TreeSet<Integer>(rkNN_id_set);
    affectedObjects.addAll(rRkNN_id_set);

    if(logger.isDebugging()) {
      StringBuffer msg = new StringBuffer();
      for(int i = 0; i < ids.size(); i++) {
        int id = ids.get(i);
        msg.append("\nkNNs[").append(id).append("] = ").append(kNNList.get(i));
        msg.append("\nrNNs[").append(id).append("] = ").append(rkNNList.get(i));
      }

      msg.append("\n\nRkNN_Ids = ").append(rkNN_id_set);
      msg.append("\nRRkNN_Ids = ").append(rRkNN_id_set);
      msg.append("\naffectedObjects = ").append(affectedObjects);
      msg.append("\n# affectedObjects = ").append(affectedObjects.size());
      // for(int i = 0; i < rkNN_id_list.size(); i++) {
      // int id = rkNN_id_list.get(i);
      // msg.append("\nrNNs[").append(id).append("] = ").append(rRkNNList.get(i));
      // }

      msg.append("\n");
      logger.debug(msg.toString());
    }

    /************************
     * 
     */
    for (Integer id: affectedObjects) {
      List<DistanceResultPair<D>> knn_old = knn1.get(id);
      List<DistanceResultPair<D>> knn_new = database.kNNQueryForID(id, k+1, distanceFunction);
      if (knn_old == null || ! knn_old.equals(knn_new)) {
        System.out.println();
        System.out.println("kNN_old["+id+"] = "+knn_old);
        System.out.println("kNN_new["+id+"] = "+knn_new);
      }
    }
    
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

    public LOFDatabaseListener(OutlierResult result, HashMap<Integer, List<DistanceResultPair<D>>> knn1, HashMap<Integer, List<DistanceResultPair<D>>>knn2) {
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

}
