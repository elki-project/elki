package experimentalcode.shared.algorithm.clustering;

import java.util.AbstractCollection;
import java.util.Collection;
import java.util.Iterator;

import de.lmu.ifi.dbs.elki.data.NumberVector;
import de.lmu.ifi.dbs.elki.database.Database;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.AbstractParameterizable;

public class FullDatabaseReferencePoints<O extends NumberVector<O,?>> extends AbstractParameterizable implements ReferencePointsHeuristic<O> {
  @Override
  public Collection<O> getReferencePoints(Database<O> db) {
    return new DatabaseProxy(db);
  }
  
  class DatabaseProxy extends AbstractCollection<O> implements Collection<O> {
    Database<O> db;
    
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
    
    class ProxyIterator implements Iterator<O> {
      final Iterator<Integer> iter;
      
      public ProxyIterator(Iterator<Integer> iter) {
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
