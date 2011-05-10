package experimentalcode.frankenb.model;

import java.util.Iterator;

import de.lmu.ifi.dbs.elki.data.NumberVector;
import de.lmu.ifi.dbs.elki.database.ids.DBID;
import de.lmu.ifi.dbs.elki.database.ids.DBIDUtil;
import de.lmu.ifi.dbs.elki.database.relation.Relation;
import de.lmu.ifi.dbs.elki.utilities.DatabaseUtil;
import experimentalcode.frankenb.model.ifaces.IDataSet;

/**
 * Simple proxy class that marshals a dataBase
 * 
 * @author Florian Frankenberger
 */
public class DataBaseDataSet implements IDataSet {

  private final Relation<? extends NumberVector<?, ?>> dataBase;
  
  public DataBaseDataSet(Relation<? extends NumberVector<?, ?>> dataBase) {
    this.dataBase = dataBase;
  }
  
  @Override
  public NumberVector<?, ?> get(int id) {
    return dataBase.get(DBIDUtil.importInteger(id));
  }

  @Override
  public int getDimensionality() {
    return DatabaseUtil.dimensionality(dataBase);
  }

  @Override
  public Iterable<Integer> getIDs() {
    return new Iterable<Integer>() {

      @Override
      public Iterator<Integer> iterator() {
        return new Iterator<Integer>() {

          Iterator<DBID> iterator = dataBase.iterDBIDs();
          
          @Override
          public boolean hasNext() {
            return iterator.hasNext();
          }

          @Override
          public Integer next() {
            return iterator.next().getIntegerID();
          }

          @Override
          public void remove() {
            throw new UnsupportedOperationException();
          }
          
        };
      }
      
    };
  }

  @Override
  public int getSize() {
    return dataBase.size();
  }

  @Override
  public IDataSet getOriginal() {
    return this;
  }
}