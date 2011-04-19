package de.lmu.ifi.dbs.elki.datasource.bundle;

import java.util.List;

import de.lmu.ifi.dbs.elki.data.type.SimpleTypeInformation;
import de.lmu.ifi.dbs.elki.utilities.exceptions.AbortException;

/**
 * This class represents a set of "packaged" objects, which is a transfer
 * container for objects e.g. from parsers to a database. It contains the object
 * with multiple representations outside of any index structure.
 * 
 * @author Erich Schubert
 */
public class MultipleObjectsBundle implements ObjectBundle {
  /**
   * Storing the meta data.
   */
  private BundleMeta meta;

  /**
   * Storing the real contents.
   */
  private List<Object> folded;

  /**
   * Constructor.
   * 
   * @param meta Meta data contained.
   * @param folded Contents to wrap. Should be n times the object data.
   */
  public MultipleObjectsBundle(BundleMeta meta, List<Object> folded) {
    super();
    this.meta = meta;
    this.folded = folded;
    assert (this.folded.size() % this.meta.size() == 0);
  }

  @Override
  public BundleMeta meta() {
    return meta;
  }

  @Override
  public SimpleTypeInformation<?> meta(int i) {
    return meta.get(i);
  }

  @Override
  public int metaLength() {
    return meta.size();
  }

  @Override
  public Object data(int onum, int rnum) {
    if(rnum < 0 || rnum >= meta.size()) {
      throw new ArrayIndexOutOfBoundsException();
    }
    return folded.get(onum * meta.size() + rnum);
  }

  @Override
  public int dataLength() {
    return folded.size() / meta.size();
  }

  /**
   * Append a new record to the data set. Pay attention to having the right
   * number of values!
   * 
   * @param data Data to append
   */
  public void appendSimple(Object... data) {
    if(data.length != meta.size()) {
      throw new AbortException("Invalid number of attributes in 'append'.");
    }
    for(int i = 0; i < data.length; i++) {
      folded.add(data[i]);
    }
  }

  /**
   * Get the folded objects list. Use with caution!
   * 
   * @return the folded objects list
   */
  public List<Object> getFolded() {
    return folded;
  }
}