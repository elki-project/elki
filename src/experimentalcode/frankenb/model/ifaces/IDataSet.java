package experimentalcode.frankenb.model.ifaces;

import de.lmu.ifi.dbs.elki.data.NumberVector;

/**
 * This is a simple intermediate state of Data in a DataBase.
 * Image a projection reduces the dimensions used, then the original ids are preserved but
 * the vectors are different. The new vectors would be stored in a <code>IDataSet</code>
 * 
 * @author Florian Frankenberger
 */
public interface IDataSet {

  /**
   * Returns the object in this data set with the given id
   * 
   * @param id
   * @return
   */
  public NumberVector<?, ?> get(int id);
  
  /**
   * Returns the number of dimensions in this data set
   * 
   * @return
   */
  public int getDimensionality();
  
  /**
   * @return
   */
  public int getSize();
  
  /**
   * Returns the original dataSet or null if not applicable
   * @return
   */
  public IDataSet getOriginal();
  
  /**
   * Returns an iterable for the IDs
   * @return
   */
  public Iterable<Integer> getIDs();

}
