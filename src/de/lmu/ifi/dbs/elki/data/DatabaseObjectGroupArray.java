package de.lmu.ifi.dbs.elki.data;

import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;

/**
 * Array backed DatabaseObjectGroup.
 * Stores a list of DatabaseObjectIDs using an array of Integer[].
 * 
 * @author Erich Schubert
 *
 */
public final class DatabaseObjectGroupArray  implements DatabaseObjectGroup {
  /**
   * ID storage.
   */
  public Integer[] ids;
  
  /**
   * Constructor.
   * 
   * @param ids Array of IDs
   */  
  public DatabaseObjectGroupArray(Integer[] ids) {
    super();
    this.ids = ids;
  }

  /**
   * Retrieve the IDs as collection.
   * Note that for array backed groups this will generate a copy.
   */
  @Override
  public Collection<Integer> getIDs() {
    return Arrays.asList(ids);
  }

  /**
   * Return an iterator over the array.
   */
  @Override
  public Iterator<Integer> iterator() {
    return Arrays.asList(ids).iterator();
  }

  /**
   * Return the backing array length.
   */
  @Override
  public int size() {
    return ids.length;
  }
}