/*
 * This file is part of ELKI:
 * Environment for Developing KDD-Applications Supported by Index-Structures
 *
 * Copyright (C) 2022
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
package elki.database.query.rknn;

import elki.database.ids.DBIDRef;
import elki.database.ids.DoubleDBIDList;
import elki.database.relation.Relation;
import elki.index.preprocessed.knn.MaterializeKNNAndRKNNPreprocessor;
import elki.logging.LoggingUtil;

/**
 * Instance for a particular database, invoking the preprocessor.
 * 
 * @author Elke Achtert
 * @since 0.4.0
 */
public class PreprocessorRKNNQuery<O> implements RKNNSearcher<DBIDRef> {
  /**
   * The data to use for this query
   */
  protected final Relation<? extends O> relation;

  /**
   * The last preprocessor result
   */
  private final MaterializeKNNAndRKNNPreprocessor<O> preprocessor;

  /**
   * Warn only once.
   */
  private boolean warned = false;

  /**
   * Constructor.
   * 
   * @param relation Relation to query
   * @param preprocessor Preprocessor instance to use
   */
  public PreprocessorRKNNQuery(Relation<O> relation, MaterializeKNNAndRKNNPreprocessor<O> preprocessor) {
    super();
    this.relation = relation;
    this.preprocessor = preprocessor;
  }

  /**
   * Constructor.
   * 
   * @param database Database to query
   * @param preprocessor Preprocessor to use
   */
  public PreprocessorRKNNQuery(Relation<O> database, MaterializeKNNAndRKNNPreprocessor.Factory<O> preprocessor) {
    this(database, preprocessor.instantiate(database));
  }

  @Override
  public DoubleDBIDList getRKNN(DBIDRef id, int k) {
    if(!warned && k != preprocessor.getK()) {
      LoggingUtil.warning("Requested more neighbors than preprocessed!");
    }
    return preprocessor.getRKNN(id);
  }
}
