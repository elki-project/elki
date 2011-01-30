/**
 * 
 */
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
  
  /* (non-Javadoc)
   * @see experimentalcode.frankenb.model.ifaces.IDataSet#getOriginalDataSet()
   */
  @Override
  public IDataSet getOriginal() {
    return this.originalDataSet;
  }
  
  public void add(int id, NumberVector<?, ?> vector) {
    this.map.put(id, vector);
  }
  
  /* (non-Javadoc)
   * @see experimentalcode.frankenb.model.ifaces.IDataSet#get(int)
   */
  @Override
  public NumberVector<?, ?> get(int id) {
    return map.get(id);
  }

  /* (non-Javadoc)
   * @see experimentalcode.frankenb.model.ifaces.IDataSet#getDimensionality()
   */
  @Override
  public int getDimensionality() {
    return this.dimensionality;
  }

  /* (non-Javadoc)
   * @see experimentalcode.frankenb.model.ifaces.IDataSet#getSize()
   */
  @Override
  public int getSize() {
    // TODO Auto-generated method stub
    return map.size();
  }

  /* (non-Javadoc)
   * @see experimentalcode.frankenb.model.ifaces.IDataSet#getIDs()
   */
  @Override
  public Iterable<Integer> getIDs() {
    return map.keySet();
  }


}
