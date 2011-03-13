/**
 * 
 */
package experimentalcode.frankenb.model;

import java.util.HashSet;
import java.util.Set;

import de.lmu.ifi.dbs.elki.data.NumberVector;
import experimentalcode.frankenb.model.ifaces.IDataSet;

/**
 * This class behaves like the DataSet class but just contains a list of item
 * ids and not the associated vectors with it. This is especially usefull when the
 * data set is e.g. splitted.
 * 
 * @author Florian Frankenberger
 */
public class ReferenceDataSet implements IDataSet {

  private final IDataSet originalDataSet;
  private Set<Integer> items = new HashSet<Integer>();
  
  public ReferenceDataSet(IDataSet originalDataSet) {
    if (originalDataSet == null) throw new RuntimeException("original data set can't be null!");
    this.originalDataSet = originalDataSet;
  }
  
  /* (non-Javadoc)
   * @see experimentalcode.frankenb.model.ifaces.IDataSet#get(int)
   */
  @Override
  public NumberVector<?, ?> get(int id) {
    if (!this.items.contains(id)) return null;
    return this.originalDataSet.get(id);
  }

  /* (non-Javadoc)
   * @see experimentalcode.frankenb.model.ifaces.IDataSet#getDimensionality()
   */
  @Override
  public int getDimensionality() {
    return this.originalDataSet.getDimensionality();
  }

  /* (non-Javadoc)
   * @see experimentalcode.frankenb.model.ifaces.IDataSet#getSize()
   */
  @Override
  public int getSize() {
    return this.items.size();
  }
  
  public void add(int id) {
    this.items.add(id);
  }

  /* (non-Javadoc)
   * @see experimentalcode.frankenb.model.ifaces.IDataSet#getOriginal()
   */
  @Override
  public IDataSet getOriginal() {
    return this.originalDataSet;
  }

  /* (non-Javadoc)
   * @see experimentalcode.frankenb.model.ifaces.IDataSet#getIDs()
   */
  @Override
  public Iterable<Integer> getIDs() {
    return items;
  }
  
  /* (non-Javadoc)
   * @see java.lang.Object#toString()
   */
  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    boolean first = true;
    sb.append("{");
    for (int id : this.getIDs()) {
      if (first) {
        first = false;
      } else
        sb.append(", ");
      sb.append("(").append(this.get(id)).append(")").append(" [").append(id).append("]");
    }
    sb.append("}");
    return sb.toString();
  }

}
