package experimentalcode.frankenb.model;

import java.util.LinkedHashMap;
import java.util.Map;

import de.lmu.ifi.dbs.elki.data.NumberVector;
import experimentalcode.frankenb.model.ifaces.IDataSet;

/**
 * No description given.
 * 
 * @author Florian Frankenberger
 */
public class DataSet implements IDataSet {
  
  private final IDataSet originalDataSet;
  private final int dimensionality;
  private Map<Integer, NumberVector<?, ?>> map = new LinkedHashMap<Integer, NumberVector<?, ?>>();
  
  public DataSet(IDataSet originalDataSet, int newDimensionality) {
    if (originalDataSet == null) throw new RuntimeException("original data set can't be null!");
    this.originalDataSet = originalDataSet;
    this.dimensionality = newDimensionality;
  }
  
  @Override
  public IDataSet getOriginal() {
    return this.originalDataSet;
  }
  
  public void add(int id, NumberVector<?, ?> vector) {
    this.map.put(id, vector);
  }
  
  @Override
  public NumberVector<?, ?> get(int id) {
    return map.get(id);
  }

  @Override
  public int getDimensionality() {
    return this.dimensionality;
  }

  @Override
  public int getSize() {
    // TODO Auto-generated method stub
    return map.size();
  }

  @Override
  public Iterable<Integer> getIDs() {
    return map.keySet();
  }


}
