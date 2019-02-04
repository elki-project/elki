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
package de.lmu.ifi.dbs.elki.database.query.rknn;

import java.util.ArrayList;
import java.util.List;

import de.lmu.ifi.dbs.elki.database.ids.ArrayDBIDs;
import de.lmu.ifi.dbs.elki.database.ids.DBIDIter;
import de.lmu.ifi.dbs.elki.database.ids.DBIDRef;
import de.lmu.ifi.dbs.elki.database.ids.DoubleDBIDList;
import de.lmu.ifi.dbs.elki.database.relation.Relation;
import de.lmu.ifi.dbs.elki.index.preprocessed.knn.MaterializeKNNAndRKNNPreprocessor;
import de.lmu.ifi.dbs.elki.logging.LoggingUtil;
import de.lmu.ifi.dbs.elki.utilities.exceptions.AbortException;

/**
 * Instance for a particular database, invoking the preprocessor.
 * 
 * @author Elke Achtert
 * @since 0.4.0
 */
public class PreprocessorRKNNQuery<O> implements RKNNQuery<O> {
  /**
   * The data to use for this query
   */
  final protected Relation<? extends O> relation;

  /**
   * The last preprocessor result
   */
  final private MaterializeKNNAndRKNNPreprocessor<O> preprocessor;

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
  public DoubleDBIDList getRKNNForDBID(DBIDRef id, int k) {
    if(!warned && k != preprocessor.getK()) {
      LoggingUtil.warning("Requested more neighbors than preprocessed!");
    }
    return preprocessor.getRKNN(id);
  }

  @Override
  public DoubleDBIDList getRKNNForObject(O obj, int k) {
    throw new AbortException("Preprocessor KNN query only supports ID queries.");
  }

  @Override
  public List<? extends DoubleDBIDList> getRKNNForBulkDBIDs(ArrayDBIDs ids, int k) {
    if(!warned && k != preprocessor.getK()) {
      LoggingUtil.warning("Requested more neighbors than preprocessed!");
    }
    List<DoubleDBIDList> result = new ArrayList<>(ids.size());
    for(DBIDIter iter = ids.iter(); iter.valid(); iter.advance()) {
      result.add(preprocessor.getRKNN(iter));
    }
    return result;
  }
}