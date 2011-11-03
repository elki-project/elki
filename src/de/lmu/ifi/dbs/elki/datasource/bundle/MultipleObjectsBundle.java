package de.lmu.ifi.dbs.elki.datasource.bundle;

/*
 This file is part of ELKI:
 Environment for Developing KDD-Applications Supported by Index-Structures

 Copyright (C) 2011
 Ludwig-Maximilians-Universität München
 Lehr- und Forschungseinheit für Datenbanksysteme
 ELKI Development Team

 This program is free software: you can redistribute it and/or modify
 it under the terms of the GNU Affero General Public License as published by
 the Free Software Foundation, either version 3 of the License, or
 (at your option) any later version.

 This program is distributed in the hope that it will be useful,
 but WITHOUT ANY WARRANTY; without even the implied warranty of
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 GNU Affero General Public License for more details.

 You should have received a copy of the GNU Affero General Public License
 along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

import java.util.ArrayList;
import java.util.List;

import de.lmu.ifi.dbs.elki.data.type.SimpleTypeInformation;
import de.lmu.ifi.dbs.elki.datasource.bundle.BundleStreamSource.Event;
import de.lmu.ifi.dbs.elki.logging.LoggingUtil;
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
   * @return This object, for chaining.
   */
  public MultipleObjectsBundle appendColumn(SimpleTypeInformation<?> type, List<?> data) {
    meta.add(type);
    columns.add(data);
    return this;
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
   * Convert an object stream to a bundle
   * 
   * @param source Object stream
   * @return Static bundle
   */
  public static MultipleObjectsBundle fromStream(BundleStreamSource source) {
    MultipleObjectsBundle bundle = new MultipleObjectsBundle();
    boolean stop = false;
    while(!stop) {
      Event ev = source.nextEvent();
      switch(ev) {
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
          List<Object> data = new ArrayList<Object>(bundle.dataLength() + 1);
          bundle.appendColumn(smeta.get(i), data);
        }
        continue;
      case NEXT_OBJECT:
        for (int i = 0; i < bundle.metaLength(); i++) {
          @SuppressWarnings("unchecked")
          final List<Object> col = (List<Object>) bundle.columns.get(i);
          col.add(source.data(i));
        }
        continue;
      default:
        LoggingUtil.warning("Unknown event: " + ev);
        continue;
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