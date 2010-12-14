package experimentalcode.elke.database.query.rknn;

import java.util.ArrayList;
import java.util.List;

import de.lmu.ifi.dbs.elki.data.DatabaseObject;
import de.lmu.ifi.dbs.elki.database.Database;
import de.lmu.ifi.dbs.elki.database.ids.ArrayDBIDs;
import de.lmu.ifi.dbs.elki.database.ids.DBID;
import de.lmu.ifi.dbs.elki.database.query.AbstractDatabaseQuery;
import de.lmu.ifi.dbs.elki.database.query.DistanceResultPair;
import de.lmu.ifi.dbs.elki.database.query.rknn.RKNNQuery;
import de.lmu.ifi.dbs.elki.distance.distancevalue.Distance;
import de.lmu.ifi.dbs.elki.index.preprocessed.MaterializeKNNPreprocessor;
import de.lmu.ifi.dbs.elki.logging.LoggingUtil;
import de.lmu.ifi.dbs.elki.utilities.exceptions.AbortException;
import experimentalcode.elke.index.preprocessed.MaterializeKNNAndRKNNPreprocessor;

/**
 * Instance for a particular database, invoking the preprocessor.
 * 
 * @author Elke Achtert
 */
public class PreprocessorRKNNQuery<O extends DatabaseObject, D extends Distance<D>> extends AbstractDatabaseQuery<O> implements RKNNQuery<O, D> {
  /**
   * The last preprocessor result
   */
  final private MaterializeKNNAndRKNNPreprocessor<O, D> preprocessor;

  /**
   * Warn only once.
   */
  private boolean warned = false;

  /**
   * Constructor.
   * 
   * @param database Database to query
   * @param preprocessor Preprocessor instance to use
   */
  public PreprocessorRKNNQuery(Database<O> database, MaterializeKNNAndRKNNPreprocessor<O, D> preprocessor) {
    super(database);
    this.preprocessor = preprocessor;
  }

  /**
   * Constructor.
   * 
   * @param database Database to query
   * @param preprocessor Preprocessor to use
   */
  public PreprocessorRKNNQuery(Database<O> database, MaterializeKNNAndRKNNPreprocessor.Factory<O, D> preprocessor) {
    this(database, preprocessor.instantiate(database));
  }

  @Override
  public List<DistanceResultPair<D>> getRKNNForDBID(DBID id, int k) {
    if(!warned && k > preprocessor.getK()) {
      LoggingUtil.warning("Requested more neighbors than preprocessed!");
    }
    return preprocessor.getRKNN(id);
  }
  
  @Override
  public List<DistanceResultPair<D>> getRKNNForObject(O obj, int k) {
    DBID id = obj.getID();
    if(id != null) {
      return getRKNNForDBID(id, k);
    }
    throw new AbortException("Preprocessor RkNN query used with previously unseen objects.");
  }
  
  @Override
  public List<List<DistanceResultPair<D>>> getRKNNForBulkDBIDs(ArrayDBIDs ids, int k) {
    if(!warned && k > preprocessor.getK()) {
      LoggingUtil.warning("Requested more neighbors than preprocessed!");
    }
    List<List<DistanceResultPair<D>>> result = new ArrayList<List<DistanceResultPair<D>>>(ids.size());
    for(DBID id : ids) {
      result.add(preprocessor.getRKNN(id));
    }
    return result;
  }

  /**
   * Get the preprocessor instance.
   * 
   * @return preprocessor instance
   */
  public MaterializeKNNPreprocessor<O, D> getPreprocessor() {
    return preprocessor;
  }

  @Override
  public D getDistanceFactory() {
    return preprocessor.getDistanceFactory();
  }  
}