package experimentalcode.elke.algorithm.lof;

import java.util.List;

import de.lmu.ifi.dbs.elki.algorithm.outlier.LOF;
import de.lmu.ifi.dbs.elki.data.DatabaseObject;
import de.lmu.ifi.dbs.elki.database.Database;
import de.lmu.ifi.dbs.elki.database.DatabaseEvent;
import de.lmu.ifi.dbs.elki.database.DatabaseListener;
import de.lmu.ifi.dbs.elki.database.DistanceResultPair;
import de.lmu.ifi.dbs.elki.distance.distancevalue.NumberDistance;
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

  private void insert(List<Integer> ids, Database<O> database) {
    // get neighbors and reverse nearest neighbors of o
    List<List<DistanceResultPair<D>>> neighbors = database.bulkKNNQueryForID(ids, k + 1, getDistanceFunction());
    //List<List<DistanceResultPair<D>>> reverseNeighbors = database.reverseKNNQueryForID(o, k + 1, getDistanceFunction());
    neighbors.remove(0);
    reverseNeighbors.remove(0);

    if(logger.isDebugging()) {
      StringBuffer msg = new StringBuffer();
      msg.append("kNNs[").append(o).append("] ").append(neighbors);
      msg.append("\nrNNs[").append(o).append("]").append(reverseNeighbors);
      logger.debugFine(msg.toString());
    }

    throw new UnsupportedOperationException("TODO");
  }

}
