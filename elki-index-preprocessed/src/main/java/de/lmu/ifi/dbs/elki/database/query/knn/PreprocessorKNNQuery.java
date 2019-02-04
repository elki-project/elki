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
package de.lmu.ifi.dbs.elki.database.query.knn;

import java.util.ArrayList;
import java.util.List;

import de.lmu.ifi.dbs.elki.database.ids.*;
import de.lmu.ifi.dbs.elki.database.relation.Relation;
import de.lmu.ifi.dbs.elki.index.preprocessed.knn.AbstractMaterializeKNNPreprocessor;
import de.lmu.ifi.dbs.elki.logging.Logging;
import de.lmu.ifi.dbs.elki.utilities.exceptions.AbortException;

/**
 * Instance for a particular database, invoking the preprocessor.
 *
 * @author Erich Schubert
 * @since 0.4.0
 *
 * @param <O> Data object type
 */
public class PreprocessorKNNQuery<O> implements KNNQuery<O> {
  /**
   * Class logger
   */
  private static final Logging LOG = Logging.getLogger(PreprocessorKNNQuery.class);

  /**
   * The data to use for this query
   */
  final protected Relation<? extends O> relation;

  /**
   * The last preprocessor result
   */
  final private AbstractMaterializeKNNPreprocessor<O> preprocessor;

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
  public PreprocessorKNNQuery(Relation<? extends O> relation, AbstractMaterializeKNNPreprocessor<O> preprocessor) {
    super();
    this.relation = relation;
    this.preprocessor = preprocessor;
  }

  @Override
  public KNNList getKNNForDBID(DBIDRef id, int k) {
    if(!warned && k > preprocessor.getK()) {
      getLogger().warning("Requested more neighbors than preprocessed: requested " + k + " preprocessed " + preprocessor.getK(), new Throwable());
      warned = true;
    }
    return preprocessor.get(id).subList(k);
  }

  @Override
  public List<KNNList> getKNNForBulkDBIDs(ArrayDBIDs ids, int k) {
    if(!warned && k > preprocessor.getK()) {
      getLogger().warning("Requested more neighbors than preprocessed: requested " + k + " preprocessed " + preprocessor.getK(), new Throwable());
      warned = true;
    }
    if(k < preprocessor.getK()) {
      List<KNNList> result = new ArrayList<>(ids.size());
      for(DBIDIter iter = ids.iter(); iter.valid(); iter.advance()) {
        result.add(preprocessor.get(iter).subList(k));
      }
      return result;
    }
    List<KNNList> result = new ArrayList<>(ids.size());
    for(DBIDIter iter = ids.iter(); iter.valid(); iter.advance()) {
      result.add(preprocessor.get(iter));
    }
    return result;
  }

  @Override
  public KNNList getKNNForObject(O obj, int k) {
    throw new AbortException("Preprocessor KNN query only supports ID queries.");
  }

  /**
   * Get the preprocessor instance.
   *
   * @return preprocessor instance
   */
  public AbstractMaterializeKNNPreprocessor<O> getPreprocessor() {
    return preprocessor;
  }

  /**
   * Get the class logger. Override when subclassing!
   * 
   * @return Class logger.
   */
  protected Logging getLogger() {
    return LOG;
  }
}
