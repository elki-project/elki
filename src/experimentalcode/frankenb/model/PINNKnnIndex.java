package experimentalcode.frankenb.model;

import de.lmu.ifi.dbs.elki.data.NumberVector;
import de.lmu.ifi.dbs.elki.database.ids.DBID;
import de.lmu.ifi.dbs.elki.database.ids.DBIDs;
import de.lmu.ifi.dbs.elki.database.query.distance.DistanceQuery;
import de.lmu.ifi.dbs.elki.database.query.knn.KNNQuery;
import de.lmu.ifi.dbs.elki.database.relation.Relation;
import de.lmu.ifi.dbs.elki.distance.distancevalue.Distance;
import de.lmu.ifi.dbs.elki.index.KNNIndex;
import de.lmu.ifi.dbs.elki.persistent.PageFileStatistics;

/**
 * No description given.
 * 
 * @author Florian Frankenberger
 */
public class PINNKnnIndex<V extends NumberVector<?, ?>> implements KNNIndex<V> {

  private final KDTree<V> tree;
  private final int kFactor;
  private Relation<V> relation;
  
  /**
   * Constructor for kdtree and kFactor
   * @param tree
   * @param kFactor factor for k to multiply with when searching within the projected space
   */
  public PINNKnnIndex(Relation<V> relation, KDTree<V> tree, int kFactor) {
    this.relation = relation;
    this.tree = tree;
    this.kFactor = kFactor;
  }
  
  @Override
  public PageFileStatistics getPageFileStatistics() {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public void insert(DBID id) {
    // TODO Auto-generated method stub
    
  }

  @Override
  public boolean delete(DBID id) {
    // TODO Auto-generated method stub
    return false;
  }
  
  

  @Override
  public void insertAll(DBIDs ids) {
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

  @Override
  public <D extends Distance<D>> KNNQuery<V, D> getKNNQuery(DistanceQuery<V, D> distanceQuery, Object... hints) {
    // Query on the relation we index
    if (distanceQuery.getRelation() != relation) {
      return null;
    }
    return (KNNQuery<V, D>) new PINNKnnQuery(this.relation, this.tree, this.kFactor);
  }
}