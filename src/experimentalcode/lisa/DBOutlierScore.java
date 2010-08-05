package experimentalcode.lisa;

import de.lmu.ifi.dbs.elki.data.DatabaseObject;
import de.lmu.ifi.dbs.elki.database.Database;
import de.lmu.ifi.dbs.elki.database.datastore.DataStoreFactory;
import de.lmu.ifi.dbs.elki.database.datastore.DataStoreUtil;
import de.lmu.ifi.dbs.elki.database.datastore.WritableDataStore;
import de.lmu.ifi.dbs.elki.database.ids.DBID;
import de.lmu.ifi.dbs.elki.database.query.DistanceQuery;
import de.lmu.ifi.dbs.elki.distance.distancevalue.Distance;
import de.lmu.ifi.dbs.elki.logging.Logging;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.Parameterization;

/**
  
      // compute percentage of neighbors in the given neighborhood with size d
 * @author Lisa Reichert
 *
 * @param <O>
 * @param <D>
 */
public  class DBOutlierScore<O extends DatabaseObject, D extends Distance<D>> extends AbstractDBOutlierDetection<O,D> {
  /**
   * The logger for this class.
   */
  private static final Logging logger = Logging.getLogger(DBOutlierScore.class);
  
  public DBOutlierScore(Parameterization config) {
    super(config);
    config = config.descend(this);
  }

  @Override
  protected WritableDataStore<Double> computeOutlierScores(Database<O> database, DistanceQuery<O, D> distFunc, D d) {
    double n;

    WritableDataStore<Double> scores= DataStoreUtil.makeStorage(database.getIDs(), DataStoreFactory.HINT_HOT | DataStoreFactory.HINT_STATIC, Double.class);
    for(DBID id : database) {
      // compute percentage of neighbors in the given neighborhood with size d
      n = (database.rangeQuery(id, d, distFunc).size()) / (double) database.size();
      scores.put(id, 1-n);
    }
    scores.toString();
    return scores;
  }

  @Override
  protected Logging getLogger() {
    return logger;
  }
}
