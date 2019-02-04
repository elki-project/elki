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

import de.lmu.ifi.dbs.elki.database.ids.DBIDVar;

/**
 * Convert a MultipleObjectsBundle to a stream.
 * 
 * To use this, invoke {@link MultipleObjectsBundle#asStream()}.
 * 
 * @author Erich Schubert
 * @since 0.5.0
 */
public class StreamFromBundle implements BundleStreamSource {
  /**
   * Bundle to access
   */
  MultipleObjectsBundle bundle;

  /**
   * Offset in bundle
   */
  int onum = -2;

  /**
   * Constructor.
   * 
   * @param bundle Existing object bundle
   */
  public StreamFromBundle(MultipleObjectsBundle bundle) {
    super();
    this.bundle = bundle;
  }

  @Override
  public BundleMeta getMeta() {
    return bundle.meta();
  }

  @Override
  public Object data(int rnum) {
    return bundle.data(onum, rnum);
  }

  @Override
  public boolean hasDBIDs() {
    return bundle.getDBIDs() != null;
  }

  @Override
  public boolean assignDBID(DBIDVar var) {
    return bundle.assignDBID(onum, var);
  }

  @Override
  public Event nextEvent() {
    onum += 1;
    if(onum < 0) {
      return Event.META_CHANGED;
    }
    if(onum >= bundle.dataLength()) {
      return Event.END_OF_STREAM;
    }
    return Event.NEXT_OBJECT;
  }

  @Override
  public MultipleObjectsBundle asMultipleObjectsBundle() {
    return bundle;
  }
}