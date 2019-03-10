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

import elki.algorithm.AbstractAlgorithm;
import elki.outlier.OutlierAlgorithm;
import elki.data.NumberVector;
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
import elki.math.DoubleMinMax;
import elki.math.Mean;
import elki.result.outlier.BasicOutlierScoreMeta;
import elki.result.outlier.OutlierResult;
import elki.result.outlier.OutlierScoreMeta;
import elki.utilities.Priority;

/**
 * Trivial method that takes the average of all dimensions (for one-dimensional
 * data that is just the actual value!) as outlier score. Mostly useful to
 * convert a 1d data set into a score, e.g. in geodata.
 * 
 * @author Erich Schubert
 * @since 0.6.0
 */
@Priority(Priority.SUPPLEMENTARY - 50)
public class TrivialAverageCoordinateOutlier extends AbstractAlgorithm<OutlierResult> implements OutlierAlgorithm {
  /**
   * Our logger.
   */
  private static final Logging logger = Logging.getLogger(TrivialAverageCoordinateOutlier.class);

  /**
   * Constructor.
   */
  public TrivialAverageCoordinateOutlier() {
    super();
  }

  @Override
  public TypeInformation[] getInputTypeRestriction() {
    return TypeUtil.array(TypeUtil.NUMBER_VECTOR_VARIABLE_LENGTH);
  }

  /**
   * Run the actual algorithm.
   * 
   * @param relation Relation
   * @return Result
   */
  public OutlierResult run(Relation<? extends NumberVector> relation) {
    WritableDoubleDataStore scores = DataStoreUtil.makeDoubleStorage(relation.getDBIDs(), DataStoreFactory.HINT_HOT);
    DoubleMinMax minmax = new DoubleMinMax();
    Mean m = new Mean();
    for(DBIDIter iditer = relation.iterDBIDs(); iditer.valid(); iditer.advance()) {
      m.reset();
      NumberVector nv = relation.get(iditer);
      for (int i = 0; i < nv.getDimensionality(); i++) {
        m.put(nv.doubleValue(i));
      }
      final double score = m.getMean();
      scores.putDouble(iditer, score);
      minmax.put(score);
    }
    DoubleRelation scoreres = new MaterializedDoubleRelation("Trivial mean score", relation.getDBIDs(), scores);
    OutlierScoreMeta meta = new BasicOutlierScoreMeta(minmax.getMin(), minmax.getMax());
    return new OutlierResult(meta, scoreres);
  }

  @Override
  protected Logging getLogger() {
    return logger;
  }
}