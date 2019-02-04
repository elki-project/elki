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
package de.lmu.ifi.dbs.elki.database.datastore;

import java.util.Comparator;

import de.lmu.ifi.dbs.elki.database.ids.DBIDRef;
import de.lmu.ifi.dbs.elki.database.ids.DBIDUtil;
import de.lmu.ifi.dbs.elki.database.ids.DBIDs;

/**
 * Storage utility class. Mostly a shorthand for
 * {@link DataStoreFactory#FACTORY}.
 *
 * @author Erich Schubert
 * @since 0.4.0
 *
 * @opt nodefillcolor LemonChiffon
 * @composed - - - DataStoreFactory
 * @has - - - AscendingByDoubleDataStoreAndId
 * @has - - - DescendingByDoubleDataStoreAndId
 * @has - - - AscendingByDoubleDataStore
 * @has - - - DescendingByDoubleDataStore
 * @has - - - AscendingByIntegerDataStore
 * @has - - - DescendingByIntegerDataStore
 */
public final class DataStoreUtil {
  /**
   * Private constructor. Static methods only.
   */
  private DataStoreUtil() {
    // Do not use.
  }

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
  public static <T> WritableDataStore<T> makeStorage(DBIDs ids, int hints, Class<? super T> dataclass) {
    return DataStoreFactory.FACTORY.makeStorage(ids, hints, dataclass);
  }

  /**
   * Make a new storage, to associate the given ids with an object of class
   * dataclass.
   *
   * @param ids DBIDs to store data for
   * @param hints Hints for the storage manager
   * @return new data store
   */
  public static WritableDBIDDataStore makeDBIDStorage(DBIDs ids, int hints) {
    return DataStoreFactory.FACTORY.makeDBIDStorage(ids, hints);
  }

  /**
   * Make a new storage, to associate the given ids with an object of class
   * dataclass.
   *
   * @param ids DBIDs to store data for
   * @param hints Hints for the storage manager
   * @return new data store
   */
  public static WritableDoubleDataStore makeDoubleStorage(DBIDs ids, int hints) {
    return DataStoreFactory.FACTORY.makeDoubleStorage(ids, hints);
  }

  /**
   * Make a new storage, to associate the given ids with an object of class
   * dataclass.
   *
   * @param ids DBIDs to store data for
   * @param hints Hints for the storage manager
   * @param def Default value
   * @return new data store
   */
  public static WritableDoubleDataStore makeDoubleStorage(DBIDs ids, int hints, double def) {
    return DataStoreFactory.FACTORY.makeDoubleStorage(ids, hints, def);
  }

  /**
   * Make a new storage, to associate the given ids with an object of class
   * dataclass.
   *
   * @param ids DBIDs to store data for
   * @param hints Hints for the storage manager
   * @return new data store
   */
  public static WritableIntegerDataStore makeIntegerStorage(DBIDs ids, int hints) {
    return DataStoreFactory.FACTORY.makeIntegerStorage(ids, hints);
  }

  /**
   * Make a new storage, to associate the given ids with an object of class
   * dataclass.
   *
   * @param ids DBIDs to store data for
   * @param hints Hints for the storage manager
   * @param def Default value
   * @return new data store
   */
  public static WritableIntegerDataStore makeIntegerStorage(DBIDs ids, int hints, int def) {
    return DataStoreFactory.FACTORY.makeIntegerStorage(ids, hints, def);
  }

  /**
   * Make a new record storage, to associate the given ids with an object of
   * class dataclass.
   *
   * @param ids DBIDs to store data for
   * @param hints Hints for the storage manager
   * @param dataclasses classes to store
   * @return new record store
   */
  public static WritableRecordStore makeRecordStorage(DBIDs ids, int hints, Class<?>... dataclasses) {
    return DataStoreFactory.FACTORY.makeRecordStorage(ids, hints, dataclasses);
  }

  /**
   * Sort objects by a double relation
   *
   * @author Erich Schubert
   */
  public static class AscendingByDoubleDataStore implements Comparator<DBIDRef> {
    /**
     * Scores to use for sorting.
     */
    private final DoubleDataStore scores;

    /**
     * Constructor.
     *
     * @param scores Scores for sorting
     */
    public AscendingByDoubleDataStore(DoubleDataStore scores) {
      super();
      this.scores = scores;
    }

    @Override
    public int compare(DBIDRef id1, DBIDRef id2) {
      return Double.compare(scores.doubleValue(id1), scores.doubleValue(id2));
    }
  }

  /**
   * Sort objects by a double relation
   *
   * @author Erich Schubert
   */
  public static class DescendingByDoubleDataStore implements Comparator<DBIDRef> {
    /**
     * Scores to use for sorting.
     */
    private final DoubleDataStore scores;

    /**
     * Constructor.
     *
     * @param scores Scores for sorting
     */
    public DescendingByDoubleDataStore(DoubleDataStore scores) {
      super();
      this.scores = scores;
    }

    @Override
    public int compare(DBIDRef id1, DBIDRef id2) {
      return Double.compare(scores.doubleValue(id2), scores.doubleValue(id1));
    }
  }

  /**
   * Sort objects by a double relation
   *
   * @author Erich Schubert
   */
  public static class AscendingByDoubleDataStoreAndId implements Comparator<DBIDRef> {
    /**
     * Scores to use for sorting.
     */
    private final DoubleDataStore scores;

    /**
     * Constructor.
     *
     * @param scores Scores for sorting
     */
    public AscendingByDoubleDataStoreAndId(DoubleDataStore scores) {
      super();
      this.scores = scores;
    }

    @Override
    public int compare(DBIDRef id1, DBIDRef id2) {
      int c = Double.compare(scores.doubleValue(id1), scores.doubleValue(id2));
      return c != 0 ? c : DBIDUtil.compare(id1, id2);
    }
  }

  /**
   * Sort objects by a double relation
   *
   * @author Erich Schubert
   */
  public static class DescendingByDoubleDataStoreAndId implements Comparator<DBIDRef> {
    /**
     * Scores to use for sorting.
     */
    private final DoubleDataStore scores;

    /**
     * Constructor.
     *
     * @param scores Scores for sorting
     */
    public DescendingByDoubleDataStoreAndId(DoubleDataStore scores) {
      super();
      this.scores = scores;
    }

    @Override
    public int compare(DBIDRef id1, DBIDRef id2) {
      int c = Double.compare(scores.doubleValue(id2), scores.doubleValue(id1));
      return c != 0 ? c : DBIDUtil.compare(id1, id2);
    }
  }
  
  /**
   * Sort objects by a integer relation
   *
   * @author Erich Schubert
   * @author Julian Erhard
   */
  public static class AscendingByIntegerDataStore implements Comparator<DBIDRef> {
    /**
     * Scores to use for sorting.
     */
    private final IntegerDataStore scores;

    /**
     * Constructor.
     *
     * @param scores Scores for sorting
     */
    public AscendingByIntegerDataStore(IntegerDataStore scores) {
      super();
      this.scores = scores;
    }

    @Override
    public int compare(DBIDRef id1, DBIDRef id2) {
      return Integer.compare(scores.intValue(id1), scores.intValue(id2));
    }
  }
  
  /**
   * Sort objects by a integer relation
   *
   * @author Erich Schubert
   * @author Julian Erhard
   */
  public static class DescendingByIntegerDataStore implements Comparator<DBIDRef> {
    /**
     * Scores to use for sorting.
     */
    private final IntegerDataStore scores;

    /**
     * Constructor.
     *
     * @param scores Scores for sorting
     */
    public DescendingByIntegerDataStore(IntegerDataStore scores) {
      super();
      this.scores = scores;
    }

    @Override
    public int compare(DBIDRef id1, DBIDRef id2) {
      return Integer.compare(scores.intValue(id2), scores.intValue(id1));
    }
  }
}
