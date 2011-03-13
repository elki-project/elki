/**
 * 
 */
package experimentalcode.frankenb.model;

import java.util.Iterator;
import java.util.List;

import de.lmu.ifi.dbs.elki.data.NumberVector;
import experimentalcode.frankenb.model.ifaces.IDataSet;

/**
 * No description given.
 * 
 * @author Florian Frankenberger
 */
public class ListDataSet implements IDataSet {

  private final int dimensionality;
  private final List<NumberVector<?, ?>> items;
  
  public ListDataSet(int dimensionality, List<NumberVector<?, ?>> list) {
    this.dimensionality = dimensionality;
    this.items = list;
  }
  
  /* (non-Javadoc)
   * @see experimentalcode.frankenb.model.ifaces.IDataSet#get(int)
   */
  @Override
  public NumberVector<?, ?> get(int id) {
    return items.get(id);
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
    return items.size();
  }

  /* (non-Javadoc)
   * @see experimentalcode.frankenb.model.ifaces.IDataSet#getOriginal()
   */
  @Override
  public IDataSet getOriginal() {
    return this;
  }

  /* (non-Javadoc)
   * @see experimentalcode.frankenb.model.ifaces.IDataSet#getIDs()
   */
  @Override
  public Iterable<Integer> getIDs() {
    return new Iterable<Integer>() {

      @Override
      public Iterator<Integer> iterator() {
        return new Iterator<Integer>() {

          private int counter = 0;
          
          @Override
          public boolean hasNext() {
            return counter < items.size();
          }

          @Override
          public Integer next() {
            if (counter < items.size()) {
              return counter++;
            } else
              return null;
          }

          @Override
          public void remove() {
            throw new UnsupportedOperationException();
          }
          
        };
      }
      
    };
  }
  
  /* (non-Javadoc)
   * @see java.lang.Object#toString()
   */
  @Override
  public String toString() {
    return items.toString();
  }

}
