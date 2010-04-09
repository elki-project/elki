package experimentalcode.elke.algorithm.lof;

import java.util.List;

import de.lmu.ifi.dbs.elki.algorithm.outlier.LOF;
import de.lmu.ifi.dbs.elki.data.DatabaseObject;
import de.lmu.ifi.dbs.elki.database.Database;
import de.lmu.ifi.dbs.elki.database.DatabaseEvent;
import de.lmu.ifi.dbs.elki.database.DatabaseListener;
import de.lmu.ifi.dbs.elki.database.DistanceResultPair;
import de.lmu.ifi.dbs.elki.distance.distancefunction.DistanceFunction;
import de.lmu.ifi.dbs.elki.distance.distancevalue.NumberDistance;
import de.lmu.ifi.dbs.elki.result.outlier.OutlierResult;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.Parameterization;

public class OnlineLOF<O extends DatabaseObject, D extends NumberDistance<D, ?>> extends LOF<O, D> implements DatabaseListener<O> {

  /**
   * Constructor, adhering to
   * {@link de.lmu.ifi.dbs.elki.utilities.optionhandling.Parameterizable}
   * 
   * @param config Parameterization
   */
  public OnlineLOF(Parameterization config) {
    super(config);
  }

  @Override
  public void objectsChanged(DatabaseEvent<O> e) {
    throw new UnsupportedOperationException("TODO " + e);

  }

  @Override
  public void objectsInserted(DatabaseEvent<O> e) {
    insert(e.getObjectIDs(), e.getDatabase());
  }

  @Override
  public void objectsRemoved(DatabaseEvent<O> e) {
    throw new UnsupportedOperationException("TODO " + e);
  }
  
  @Override
  protected OutlierResult runInTime(Database<O> database) throws IllegalStateException {
    database.addDatabaseListener(this);
    return super.runInTime(database);
  }

  private void insert(List<Integer> ids, Database<O> database) {
    DistanceFunction<O, D> distanceFunction = getDistanceFunction();
    // get neighbors and reverse nearest neighbors of o
    List<List<DistanceResultPair<D>>> neighborsList = database.bulkKNNQueryForID(ids, k + 1, distanceFunction);
    List<List<DistanceResultPair<D>>> reverseNeighborsList = database.bulkReverseKNNQueryForID(ids, k + 1, distanceFunction);

    for(int i = 0; i < ids.size(); i++) {
      int id = ids.get(i);
      removeID(id, neighborsList.get(i));
      removeID(id, reverseNeighborsList.get(i));

      if(logger.isDebugging()) {
        StringBuffer msg = new StringBuffer();
        msg.append("kNNs[").append(id).append("] ").append(neighborsList.get(i));
        msg.append("\nrNNs[").append(id).append("]").append(reverseNeighborsList.get(i));
        logger.debugFine(msg.toString());
      }
    }

    throw new UnsupportedOperationException("TODO");
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

}
