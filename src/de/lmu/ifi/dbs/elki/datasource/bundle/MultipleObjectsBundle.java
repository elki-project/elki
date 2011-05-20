package de.lmu.ifi.dbs.elki.datasource.bundle;

import java.util.ArrayList;
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
  private List<List<?>> columns;

  /**
   * Constructor.
   */
  public MultipleObjectsBundle() {
    this.meta = new BundleMeta();
    this.columns = new ArrayList<List<?>>();
  }

  /**
   * Constructor.
   * 
   * @param meta Meta data contained.
   * @param columns Content in columns
   */
  @Deprecated
  public MultipleObjectsBundle(BundleMeta meta, List<List<?>> columns) {
    super();
    this.meta = meta;
    this.columns = columns;
    if(this.columns.size() != this.meta.size()) {
      throw new AbortException("Meta size and columns do not agree!");
    }
    int len = -1;
    for(List<?> col : columns) {
      if(len < 0) {
        len = col.size();
      }
      else {
        if(col.size() != len) {
          throw new AbortException("Column lengths do not agree.");
        }
      }
    }
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
    return columns.get(rnum).get(onum);
  }

  @Override
  public int dataLength() {
    try {
      return columns.get(0).size();
    }
    catch(IndexOutOfBoundsException e) {
      return 0;
    }
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
      @SuppressWarnings("unchecked")
      final List<Object> col = (List<Object>) columns.get(i);
      col.add(data[i]);
    }
  }

  /**
   * Helper to add a single column to the bundle.
   * 
   * @param type Type information
   * @param data Data to add
   */
  public void appendColumn(SimpleTypeInformation<?> type, List<?> data) {
    meta.add(type);
    columns.add(data);
  }

  /**
   * Get the raw objects columns. Use with caution!
   * 
   * @param i column number
   * @return the ith column
   */
  public List<?> getColumn(int i) {
    return columns.get(i);
  }

  /**
   * Helper to add a single column to the bundle.
   * 
   * @param <V> Object type
   * @param type Type information
   * @param data Data to add
   */
  public static <V> MultipleObjectsBundle makeSimple(SimpleTypeInformation<? super V> type, List<? extends V> data) {
    MultipleObjectsBundle bundle = new MultipleObjectsBundle();
    bundle.appendColumn(type, data);
    return bundle;
  }

  /**
   * Helper to add a single column to the bundle.
   * 
   * @param <V1> First Object type
   * @param <V2> Second Object type
   * @param type1 Type information
   * @param data1 Data column to add
   * @param type2 Second Type information
   * @param data2 Second data column to add
   */
  public static <V1, V2> MultipleObjectsBundle makeSimple(SimpleTypeInformation<? super V1> type1, List<? extends V1> data1, SimpleTypeInformation<? super V2> type2, List<? extends V2> data2) {
    MultipleObjectsBundle bundle = new MultipleObjectsBundle();
    bundle.appendColumn(type1, data1);
    bundle.appendColumn(type2, data2);
    return bundle;
  }

  /**
   * Get an object row.
   * 
   * @param row Row number
   * @return Array of values
   */
  public Object[] getRow(int row) {
    Object[] ret = new Object[columns.size()];
    for(int c = 0; c < columns.size(); c++) {
      ret[c] = data(row, c);
    }
    return ret;
  }
}