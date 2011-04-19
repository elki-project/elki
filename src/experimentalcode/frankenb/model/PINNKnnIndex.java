/**
 * 
 */
package experimentalcode.frankenb.model;

import java.util.List;

import de.lmu.ifi.dbs.elki.data.NumberVector;
import de.lmu.ifi.dbs.elki.database.Database;
import de.lmu.ifi.dbs.elki.database.ids.ArrayDBIDs;
import de.lmu.ifi.dbs.elki.database.ids.DBID;
import de.lmu.ifi.dbs.elki.database.ids.DBIDs;
import de.lmu.ifi.dbs.elki.database.query.distance.DistanceQuery;
import de.lmu.ifi.dbs.elki.database.query.knn.KNNQuery;
import de.lmu.ifi.dbs.elki.database.relation.Relation;
import de.lmu.ifi.dbs.elki.distance.distancefunction.DistanceFunction;
import de.lmu.ifi.dbs.elki.distance.distancevalue.Distance;
import de.lmu.ifi.dbs.elki.index.KNNIndex;
import de.lmu.ifi.dbs.elki.persistent.PageFileStatistics;

/**
 * No description given.
 * 
 * @author Florian Frankenberger
 */
public class PINNKnnIndex implements KNNIndex<NumberVector<?, ?>> {

  private final KDTree tree;
  private final int kFactor;
  
  /**
   * Constructor for kdtree and kFactor
   * @param tree
   * @param kFactor factor for k to multiply with when searching within the projected space
   */
  public PINNKnnIndex(KDTree tree, int kFactor) {
    this.tree = tree;
    this.kFactor = kFactor;
  }
  
  @Override
  public PageFileStatistics getPageFileStatistics() {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public void insert(DBID id, NumberVector<?, ?> object) {
    // TODO Auto-generated method stub
    
  }

  @Override
  public boolean delete(DBID id) {
    // TODO Auto-generated method stub
    return false;
  }
  
  

  @Override
  public void insertAll(ArrayDBIDs ids, List<NumberVector<?, ?>> objects) {
    // TODO Auto-generated method stub
    
  }

  @Override
  public void deleteAll(DBIDs ids) {
    // TODO Auto-generated method stub
    
  }

  @Override
  public String getLongName() {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public String getShortName() {
    // TODO Auto-generated method stub
    return null;
  }

  @SuppressWarnings("unchecked")
  @Override
  public <D extends Distance<D>> KNNQuery<NumberVector<?, ?>, D> getKNNQuery(Relation<NumberVector<?, ?>> database, DistanceFunction<? super NumberVector<?, ?>, D> distanceFunction, Object... hints) {
    return (KNNQuery<NumberVector<?, ?>, D>) new PINNKnnQuery(database, this.tree, this.kFactor);
  }

  @SuppressWarnings("unchecked")
  @Override
  public <D extends Distance<D>> KNNQuery<NumberVector<?, ?>, D> getKNNQuery(Relation<NumberVector<?, ?>> database, DistanceQuery<NumberVector<?, ?>, D> distanceQuery, Object... hints) {
    return (KNNQuery<NumberVector<?, ?>, D>) new PINNKnnQuery(database, this.tree, this.kFactor);
  }
}