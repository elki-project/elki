package de.lmu.ifi.dbs.elki.datasource.bundle;

import java.util.ArrayList;
import java.util.List;

import de.lmu.ifi.dbs.elki.data.type.SimpleTypeInformation;

/**
 * This class represents a "packaged" object, which is a transfer container for
 * objects e.g. from parsers to a database. It contains the object with multiple
 * representations outside of any index structure.
 * 
 * @author Erich Schubert
 */
public class SingleObjectBundle implements ObjectBundle {
  /**
   * Store the meta data.
   */
  private BundleMeta meta;

  /**
   * Storing the real contents.
   */
  private List<Object> contents;

  /**
   * Constructor.
   */
  public SingleObjectBundle() {
    this(new BundleMeta(), new ArrayList<Object>(5));
  }

  /**
   * Constructor.
   * 
   * @param meta Metadata
   * @param contents Object values
   */
  public SingleObjectBundle(BundleMeta meta, List<Object> contents) {
    super();
    this.meta = meta;
    this.contents = contents;
    assert (meta.size() == contents.size());
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

  /**
   * Get the value of the ith component.
   * 
   * @param rnum representation number
   * @return value
   */
  public Object data(int rnum) {
    return contents.get(rnum);
  }

  @Override
  public int dataLength() {
    return 1;
  }

  @Override
  public Object data(int onum, int rnum) {
    if(onum != 0) {
      throw new ArrayIndexOutOfBoundsException();
    }
    return contents.get(rnum);
  }

  /**
   * Append a single representation to the object.
   * 
   * @param meta Meta for the representation
   * @param data Data to append
   */
  public void append(SimpleTypeInformation<?> meta, Object data) {
    this.meta.add(meta);
    this.contents.add(data);
  }
}