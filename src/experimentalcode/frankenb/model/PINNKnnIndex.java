/**
 * 
 */
package experimentalcode.frankenb.model;

import java.util.List;

import de.lmu.ifi.dbs.elki.data.NumberVector;
import de.lmu.ifi.dbs.elki.database.Database;
import de.lmu.ifi.dbs.elki.database.query.distance.DistanceQuery;
import de.lmu.ifi.dbs.elki.database.query.knn.KNNQuery;
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
  
  /* (non-Javadoc)
   * @see de.lmu.ifi.dbs.elki.index.Index#getPageFileStatistics()
   */
  @Override
  public PageFileStatistics getPageFileStatistics() {
    // TODO Auto-generated method stub
    return null;
  }

  /* (non-Javadoc)
   * @see de.lmu.ifi.dbs.elki.index.Index#insert(de.lmu.ifi.dbs.elki.data.DatabaseObject)
   */
  @Override
  public void insert(NumberVector<?, ?> object) {
    // TODO Auto-generated method stub
    
  }

  /* (non-Javadoc)
   * @see de.lmu.ifi.dbs.elki.index.Index#insert(java.util.List)
   */
  @Override
  public void insert(List<NumberVector<?, ?>> objects) {
    // TODO Auto-generated method stub
    
  }

  /* (non-Javadoc)
   * @see de.lmu.ifi.dbs.elki.index.Index#delete(de.lmu.ifi.dbs.elki.data.DatabaseObject)
   */
  @Override
  public boolean delete(NumberVector<?, ?> object) {
    // TODO Auto-generated method stub
    return false;
  }

  /* (non-Javadoc)
   * @see de.lmu.ifi.dbs.elki.index.Index#delete(java.util.List)
   */
  @Override
  public void delete(List<NumberVector<?, ?>> objects) {
    // TODO Auto-generated method stub
    
  }

  /* (non-Javadoc)
   * @see de.lmu.ifi.dbs.elki.result.Result#getLongName()
   */
  @Override
  public String getLongName() {
    // TODO Auto-generated method stub
    return null;
  }

  /* (non-Javadoc)
   * @see de.lmu.ifi.dbs.elki.result.Result#getShortName()
   */
  @Override
  public String getShortName() {
    // TODO Auto-generated method stub
    return null;
  }

  /* (non-Javadoc)
   * @see de.lmu.ifi.dbs.elki.index.KNNIndex#getKNNQuery(de.lmu.ifi.dbs.elki.database.Database, de.lmu.ifi.dbs.elki.distance.distancefunction.DistanceFunction, java.lang.Object[])
   */
  @SuppressWarnings("unchecked")
  @Override
  public <D extends Distance<D>> KNNQuery<NumberVector<?, ?>, D> getKNNQuery(Database<NumberVector<?, ?>> database, DistanceFunction<? super NumberVector<?, ?>, D> distanceFunction, Object... hints) {
    return (KNNQuery<NumberVector<?, ?>, D>) new PINNKnnQuery(database, this.tree, this.kFactor);
  }

  /* (non-Javadoc)
   * @see de.lmu.ifi.dbs.elki.index.KNNIndex#getKNNQuery(de.lmu.ifi.dbs.elki.database.Database, de.lmu.ifi.dbs.elki.database.query.distance.DistanceQuery, java.lang.Object[])
   */
  @SuppressWarnings("unchecked")
  @Override
  public <D extends Distance<D>> KNNQuery<NumberVector<?, ?>, D> getKNNQuery(Database<NumberVector<?, ?>> database, DistanceQuery<NumberVector<?, ?>, D> distanceQuery, Object... hints) {
    return (KNNQuery<NumberVector<?, ?>, D>) new PINNKnnQuery(database, this.tree, this.kFactor);
  }

}
