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
 * Use the square rooted values of precomputed kNN.
 *
 * @author Erich Schubert
 * @since 0.4.0
 */
public class PreprocessorSqrtKNNQuery extends PreprocessorKNNQuery {
  /**
   * Class logger
   */
  private static final Logging LOG = Logging.getLogger(PreprocessorSqrtKNNQuery.class);

  /**
   * Constructor.
   *
   * @param relation Relation to query
   * @param preprocessor Preprocessor instance to use
   */
  public PreprocessorSqrtKNNQuery(Relation<?> relation, AbstractMaterializeKNNPreprocessor<?> preprocessor) {
    super(relation, preprocessor);
  }

  @Override
  public KNNList getKNN(DBIDRef id, int k) {
    final KNNList knnList = super.getKNN(id, k);
    return knnList != null ? knnList.map(Math::sqrt) : null;
  }

  /**
   * Get the class logger. Override when subclassing!
   * 
   * @return Class logger.
   */
  @Override
  protected Logging getLogger() {
    return LOG;
  }
}
