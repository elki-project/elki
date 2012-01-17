package de.lmu.ifi.dbs.elki.database.datastore;

/*
 This file is part of ELKI:
 Environment for Developing KDD-Applications Supported by Index-Structures

 Copyright (C) 2012
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

import de.lmu.ifi.dbs.elki.database.datastore.memory.MemoryDataStoreFactory;
import de.lmu.ifi.dbs.elki.database.ids.DBIDs;

/**
 * API for a storage factory used for producing larger storage maps.
 * 
 * Use {@link #FACTORY} for a static instance.
 * 
 * @author Erich Schubert
 * 
 * @apiviz.stereotype factory,interface
 * @apiviz.uses WritableDataStore oneway - - «create»
 * @apiviz.uses WritableRecordStore oneway - - «create»
 */
public interface DataStoreFactory {
  /**
   * Static storage factory
   */
  public static DataStoreFactory FACTORY = new MemoryDataStoreFactory();

  /**
   * Storage will be used only temporary.
   */
  public final static int HINT_TEMP = 0x01;

  /**
   * "Hot" data, that will be used a lot, preferring memory storage.
   */
  public final static int HINT_HOT = 0x02;

  /**
   * "static" data, that will not change often
   */
  public final static int HINT_STATIC = 0x04;

  /**
   * Data that might require sorted access (so hashmaps are suboptimal)
   */
  public final static int HINT_SORTED = 0x08;

  /**
   * Data that is the main database. Includes HOT, STATIC, SORTED
   */
  public final static int HINT_DB = 0x1E;

  /**
   * Make a new storage, to associate the given ids with an object of class
   * dataclass.
   * 
   * @param <T> stored data type
   * @param ids DBIDs to store data for
   * @param hints Hints for the storage manager
   * @param dataclass class to store
   * @return new data store
   */
  public <T> WritableDataStore<T> makeStorage(DBIDs ids, int hints, Class<? super T> dataclass);

  /**
   * Make a new storage, to associate the given ids with an object of class
   * dataclass.
   * 
   * @param ids DBIDs to store data for
   * @param hints Hints for the storage manager
   * @return new data store
   */
  public WritableDoubleDataStore makeDoubleStorage(DBIDs ids, int hints);

  /**
   * Make a new record storage, to associate the given ids with an object of
   * class dataclass.
   * 
   * @param ids DBIDs to store data for
   * @param hints Hints for the storage manager
   * @param dataclasses classes to store
   * @return new record store
   */
  public WritableRecordStore makeRecordStorage(DBIDs ids, int hints, Class<?>... dataclasses);
}