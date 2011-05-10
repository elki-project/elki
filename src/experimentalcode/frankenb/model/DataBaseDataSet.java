package experimentalcode.frankenb.model;

import de.lmu.ifi.dbs.elki.data.NumberVector;
import de.lmu.ifi.dbs.elki.database.ids.DBID;
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
  public NumberVector<?, ?> get(DBID id) {
    return dataBase.get(id);
  }

  @Override
  public int getDimensionality() {
    return DatabaseUtil.dimensionality(dataBase);
  }

  @Override
  public Iterable<DBID> getIDs() {
    return dataBase.iterDBIDs();
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