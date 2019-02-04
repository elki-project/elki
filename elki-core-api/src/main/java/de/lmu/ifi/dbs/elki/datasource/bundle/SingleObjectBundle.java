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
import de.lmu.ifi.dbs.elki.database.ids.DBID;
import de.lmu.ifi.dbs.elki.database.ids.DBIDVar;

/**
 * This class represents a "packaged" object, which is a transfer container for
 * objects e.g. from parsers to a database. It contains the object with multiple
 * representations outside of any index structure.
 * 
 * @author Erich Schubert
 * @since 0.4.0
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
   * Object ID
   */
  private DBID id;

  /**
   * Constructor.
   */
  public SingleObjectBundle() {
    this(new BundleMeta(), new ArrayList<>(5));
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

  /**
   * Constructor.
   * 
   * @param meta Metadata
   * @param id ID of object
   * @param contents Object values
   */
  public SingleObjectBundle(BundleMeta meta, DBID id, List<Object> contents) {
    super();
    this.meta = meta;
    this.id = id;
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

  @Override
  public boolean assignDBID(int onum, DBIDVar var) {
    if(id == null) {
      var.unset();
      return false;
    }
    var.set(id);
    return true;
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