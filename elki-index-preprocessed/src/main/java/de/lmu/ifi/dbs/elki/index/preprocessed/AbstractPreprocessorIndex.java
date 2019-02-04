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
package de.lmu.ifi.dbs.elki.index.preprocessed;

import de.lmu.ifi.dbs.elki.database.datastore.WritableDataStore;
import de.lmu.ifi.dbs.elki.database.relation.Relation;
import de.lmu.ifi.dbs.elki.index.Index;
import de.lmu.ifi.dbs.elki.logging.Logging;

/**
 * Abstract base class for simple preprocessor based indexes, requiring a simple
 * object storage for preprocessing results.
 * 
 * @author Erich Schubert
 * @since 0.4.0
 * 
 * @composed - - - WritableDataStore
 * 
 * @param <O> Object type
 * @param <R> Stored data type
 */
public abstract class AbstractPreprocessorIndex<O, R> implements Index {
  /**
   * The representation we are bound to.
   */
  protected final Relation<O> relation;

  /**
   * The data store.
   */
  protected WritableDataStore<R> storage = null;

  /**
   * Constructor.
   * 
   * @param relation Relation to index
   */
  public AbstractPreprocessorIndex(Relation<O> relation) {
    super();
    this.relation = relation;
  }

  /**
   * Get the classes static logger.
   * 
   * @return Logger
   */
  protected abstract Logging getLogger();
}
