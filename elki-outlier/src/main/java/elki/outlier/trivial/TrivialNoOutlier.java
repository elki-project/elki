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
package elki.outlier.trivial;

import elki.outlier.OutlierAlgorithm;
import elki.AbstractAlgorithm;
import elki.data.type.TypeInformation;
import elki.data.type.TypeUtil;
import elki.database.datastore.DataStoreFactory;
import elki.database.datastore.DataStoreUtil;
import elki.database.datastore.WritableDoubleDataStore;
import elki.database.ids.DBIDIter;
import elki.database.relation.DoubleRelation;
import elki.database.relation.MaterializedDoubleRelation;
import elki.database.relation.Relation;
import elki.logging.Logging;
import elki.result.outlier.OutlierResult;
import elki.result.outlier.OutlierScoreMeta;
import elki.result.outlier.ProbabilisticOutlierScore;
import elki.utilities.Priority;

/**
 * Trivial method that claims to find no outliers. Can be used as reference
 * algorithm in comparisons.
 * 
 * @author Erich Schubert
 * @since 0.4.0
 */
@Priority(Priority.SUPPLEMENTARY - 50)
public class TrivialNoOutlier extends AbstractAlgorithm<OutlierResult> implements OutlierAlgorithm {
  /**
   * Our logger.
   */
  private static final Logging LOG = Logging.getLogger(TrivialNoOutlier.class);

  /**
   * Constructor.
   */
  public TrivialNoOutlier() {
    super();
  }

  @Override
  public TypeInformation[] getInputTypeRestriction() {
    return TypeUtil.array(TypeUtil.ANY);
  }

  /**
   * Run the actual algorithm.
   * 
   * @param relation Relation
   * @return Result
   */
  public OutlierResult run(Relation<?> relation) {
    WritableDoubleDataStore scores = DataStoreUtil.makeDoubleStorage(relation.getDBIDs(), DataStoreFactory.HINT_HOT);
    for(DBIDIter iditer = relation.iterDBIDs(); iditer.valid(); iditer.advance()) {
      scores.putDouble(iditer, 0.0);
    }
    DoubleRelation scoreres = new MaterializedDoubleRelation("Trivial no-outlier score", relation.getDBIDs(), scores);
    OutlierScoreMeta meta = new ProbabilisticOutlierScore();
    return new OutlierResult(meta, scoreres);
  }

  @Override
  protected Logging getLogger() {
    return LOG;
  }
}