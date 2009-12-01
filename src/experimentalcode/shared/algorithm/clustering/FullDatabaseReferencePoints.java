package experimentalcode.shared.algorithm.clustering;

import java.util.AbstractCollection;
import java.util.Collection;
import java.util.Iterator;

import de.lmu.ifi.dbs.elki.data.NumberVector;
import de.lmu.ifi.dbs.elki.database.Database;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.AbstractParameterizable;

/**
 * Strategy to use the complete database as reference points.
 * 
 * @author Erich Schubert
 *
 * @param <O> Object type.
 */
public class FullDatabaseReferencePoints<O extends NumberVector<O,?>> extends AbstractParameterizable implements ReferencePointsHeuristic<O> {  
  /**
   * Constructor, Parameterizable style.
   */
  public FullDatabaseReferencePoints() {
    super();
  }

  @Override
  public Collection<O> getReferencePoints(Database<O> db) {
    return new DatabaseProxy(db);
  }
  
  /**
   * Proxy class to map a database ID collection to a database Object collection.
   * 
   * @author Erich Schubert
   */
  // TODO: refactor into DatabaseUtil oder so?
  class DatabaseProxy extends AbstractCollection<O> implements Collection<O> {
    /**
     * The database we query
     */
    Database<O> db;
    
    /**
     * Constructor.
     * 
     * @param db Database
     */
    public DatabaseProxy(Database<O> db) {
      super();
      this.db = db;
    }

    @Override
    public Iterator<O> iterator() {
      return new ProxyIterator(db.iterator());
    }

    @Override
    public int size() {
      return db.size();
    }
    
    /**
     * Iterator class
     * 
     * @author Erich Schubert
     */
    class ProxyIterator implements Iterator<O> {
      /**
       * The real iterator.
       */
      final Iterator<Integer> iter;
      
      /**
       * Constructor
       * 
       * @param iter Original iterator.
       */
      ProxyIterator(Iterator<Integer> iter) {
        super();
        this.iter = iter;
      }

      @Override
      public boolean hasNext() {
        return iter.hasNext();
      }

      @Override
      public O next() {
        Integer id = iter.next();
        return db.get(id);
      }

      @Override
      public void remove() {
        iter.remove();
      }
    }
  }
}