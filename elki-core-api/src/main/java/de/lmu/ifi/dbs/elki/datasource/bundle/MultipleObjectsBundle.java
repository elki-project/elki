/*
 * This file is part of ELKI:
 * Environment for Developing KDD-Applications Supported by Index-Structures
 *
 * Copyright (C) 2019
 * ELKI Development Team
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package de.lmu.ifi.dbs.elki.datasource.bundle;

import java.util.ArrayList;
import java.util.List;

import de.lmu.ifi.dbs.elki.data.type.SimpleTypeInformation;
import de.lmu.ifi.dbs.elki.database.ids.ArrayDBIDs;
import de.lmu.ifi.dbs.elki.database.ids.ArrayModifiableDBIDs;
import de.lmu.ifi.dbs.elki.database.ids.DBIDUtil;
import de.lmu.ifi.dbs.elki.database.ids.DBIDVar;
import de.lmu.ifi.dbs.elki.logging.Logging;
import de.lmu.ifi.dbs.elki.logging.LoggingUtil;
import de.lmu.ifi.dbs.elki.utilities.exceptions.AbortException;

/**
 * This class represents a set of "packaged" objects, which is a transfer
 * container for objects e.g. from parsers to a database. It contains the object
 * with multiple representations outside of any index structure.
 * 
 * @author Erich Schubert
 * @since 0.4.0
 * 
 * @opt nodefillcolor LemonChiffon
 */
public class MultipleObjectsBundle implements ObjectBundle {
  /**
   * Class logger.
   */
  private static final Logging LOG = Logging.getLogger(MultipleObjectsBundle.class);

  /**
   * Storing the meta data.
   */
  private BundleMeta meta;

  /**
   * Storing the real contents.
   */
  private List<List<?>> columns;

  /**
   * DBIDs for these objects, but may be null.
   */
  private ArrayDBIDs ids;

  /**
   * Constructor.
   */
  public MultipleObjectsBundle() {
    this.meta = new BundleMeta();
    this.columns = new ArrayList<>();
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
    return columns.get(rnum).get(onum);
  }

  @Override
  public boolean assignDBID(int onum, DBIDVar var) {
    if(ids == null) {
      var.unset();
      return false;
    }
    ids.assignVar(onum, var);
    return true;
  }

  @Override
  public int dataLength() {
    return (ids != null) ? ids.size() : (columns.isEmpty()) ? 0 : columns.get(0).size();
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
   * @return This object, for chaining.
   */
  public MultipleObjectsBundle appendColumn(SimpleTypeInformation<?> type, List<?> data) {
    meta.add(type);
    columns.add(data);
    return this;
  }

  /**
   * Set the DBID range for this bundle.
   * 
   * @param ids DBIDs
   */
  public void setDBIDs(ArrayDBIDs ids) {
    this.ids = ids;
  }

  /**
   * Get the DBIDs, may be {@code null}.
   * 
   * @return DBIDs
   */
  public ArrayDBIDs getDBIDs() {
    return ids;
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
   * Helper to add a single column to the bundle.
   * 
   * @param <V1> First Object type
   * @param <V2> Second Object type
   * @param <V3> Third Object type
   * @param type1 First type information
   * @param data1 First data column to add
   * @param type2 Second type information
   * @param data2 Second data column to add
   * @param type3 Third type information
   * @param data3 Third data column to add
   */
  public static <V1, V2, V3> MultipleObjectsBundle makeSimple(SimpleTypeInformation<? super V1> type1, List<? extends V1> data1, SimpleTypeInformation<? super V2> type2, List<? extends V2> data2, SimpleTypeInformation<? super V3> type3, List<? extends V3> data3) {
    MultipleObjectsBundle bundle = new MultipleObjectsBundle();
    bundle.appendColumn(type1, data1);
    bundle.appendColumn(type2, data2);
    bundle.appendColumn(type3, data3);
    return bundle;
  }

  /**
   * Process this bundle as stream.
   * 
   * @return Stream
   */
  public BundleStreamSource asStream() {
    return new StreamFromBundle(this);
  }

  /**
   * Convert an object stream to a bundle
   * 
   * @param source Object stream
   * @return Static bundle
   */
  public static MultipleObjectsBundle fromStream(BundleStreamSource source) {
    MultipleObjectsBundle bundle = new MultipleObjectsBundle();
    boolean stop = false;
    DBIDVar var = null;
    ArrayModifiableDBIDs ids = null;
    int size = 0;
    while(!stop) {
      BundleStreamSource.Event ev = source.nextEvent();
      switch(ev){
      case END_OF_STREAM:
        stop = true;
        break;
      case META_CHANGED:
        BundleMeta smeta = source.getMeta();
        // rebuild bundle meta
        bundle.meta = new BundleMeta();
        for(int i = 0; i < bundle.columns.size(); i++) {
          bundle.meta.add(smeta.get(i));
        }
        for(int i = bundle.metaLength(); i < smeta.size(); i++) {
          List<Object> data = new ArrayList<>(bundle.dataLength() + 1);
          bundle.appendColumn(smeta.get(i), data);
        }
        if(var == null && source.hasDBIDs()) {
          var = DBIDUtil.newVar();
          ids = DBIDUtil.newArray();
        }
        continue;
      case NEXT_OBJECT:
        if(var != null && source.assignDBID(var)) {
          ids.add(var);
        }
        for(int i = 0; i < bundle.metaLength(); i++) {
          @SuppressWarnings("unchecked")
          final List<Object> col = (List<Object>) bundle.columns.get(i);
          col.add(source.data(i));
        }
        ++size;
        continue;
      default:
        LoggingUtil.warning("Unknown event: " + ev);
        continue;
      }
    }
    if(ids != null) {
      if(size != ids.size()) {
        LOG.warning("Not every object had an DBID - discarding DBIDs: " + size + " != " + ids.size());
      }
      else {
        bundle.setDBIDs(ids);
      }
    }
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
