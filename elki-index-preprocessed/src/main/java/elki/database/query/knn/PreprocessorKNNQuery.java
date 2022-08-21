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
package elki.database.query.knn;

import elki.database.ids.DBIDRef;
import elki.database.ids.KNNList;
import elki.database.relation.Relation;
import elki.index.preprocessed.knn.AbstractMaterializeKNNPreprocessor;
import elki.logging.Logging;

/**
 * Use precomputed kNN.
 *
 * @author Erich Schubert
 * @since 0.4.0
 */
public class PreprocessorKNNQuery implements KNNSearcher<DBIDRef> {
  /**
   * Class logger
   */
  private static final Logging LOG = Logging.getLogger(PreprocessorKNNQuery.class);

  /**
   * The data to use for this query
   */
  protected final Relation<?> relation;

  /**
   * The last preprocessor result
   */
  private final AbstractMaterializeKNNPreprocessor<?> preprocessor;

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
  public PreprocessorKNNQuery(Relation<?> relation, AbstractMaterializeKNNPreprocessor<?> preprocessor) {
    super();
    this.relation = relation;
    this.preprocessor = preprocessor;
  }

  @Override
  public KNNList getKNN(DBIDRef id, int k) {
    if(!warned && k > preprocessor.getK()) {
      getLogger().warning("Requested more neighbors than preprocessed: requested " + k + " preprocessed " + preprocessor.getK(), new Throwable());
      warned = true;
    }
    final KNNList knnList = preprocessor.get(id);
    return knnList != null ? knnList.subList(k) : null;
  }

  /**
   * Get the preprocessor instance.
   *
   * @return preprocessor instance
   */
  public AbstractMaterializeKNNPreprocessor<?> getPreprocessor() {
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
