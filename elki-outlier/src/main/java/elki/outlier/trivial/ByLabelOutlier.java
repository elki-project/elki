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
package elki.outlier.trivial;

import java.util.regex.Pattern;

import elki.data.type.NoSupportedDataTypeException;
import elki.data.type.TypeInformation;
import elki.data.type.TypeUtil;
import elki.database.Database;
import elki.database.datastore.DataStoreFactory;
import elki.database.datastore.DataStoreUtil;
import elki.database.datastore.WritableDoubleDataStore;
import elki.database.ids.DBIDIter;
import elki.database.relation.DoubleRelation;
import elki.database.relation.MaterializedDoubleRelation;
import elki.database.relation.Relation;
import elki.outlier.OutlierAlgorithm;
import elki.result.outlier.OutlierResult;
import elki.result.outlier.OutlierScoreMeta;
import elki.result.outlier.ProbabilisticOutlierScore;
import elki.utilities.Priority;
import elki.utilities.optionhandling.OptionID;
import elki.utilities.optionhandling.Parameterizer;
import elki.utilities.optionhandling.parameterization.Parameterization;
import elki.utilities.optionhandling.parameters.PatternParameter;

/**
 * Trivial algorithm that marks outliers by their label. Can be used as
 * reference algorithm in comparisons.
 * 
 * @author Erich Schubert
 * @since 0.4.0
 */
@Priority(Priority.SUPPLEMENTARY)
public class ByLabelOutlier implements OutlierAlgorithm {
  /**
   * The default pattern to use.
   */
  public static final String DEFAULT_PATTERN = ".*(Outlier|Noise).*";

  /**
   * The pattern we match with.
   */
  final Pattern pattern;

  /**
   * Constructor.
   * 
   * @param pattern Pattern to match with.
   */
  public ByLabelOutlier(Pattern pattern) {
    super();
    this.pattern = pattern;
  }

  /**
   * Constructor.
   */
  public ByLabelOutlier() {
    this(Pattern.compile(DEFAULT_PATTERN));
  }

  @Override
  public TypeInformation[] getInputTypeRestriction() {
    return TypeUtil.array(TypeUtil.GUESSED_LABEL);
  }

  @Override
  public OutlierResult autorun(Database database) {
    // Prefer a true class label
    try {
      return run(database.getRelation(TypeUtil.CLASSLABEL));
    }
    catch(NoSupportedDataTypeException e) {
      // Otherwise, try any labellike.
      return run(database.getRelation(getInputTypeRestriction()[0]));
    }
  }

  /**
   * Run the algorithm
   * 
   * @param relation Relation to process.
   * @return Result
   */
  public OutlierResult run(Relation<?> relation) {
    WritableDoubleDataStore scores = DataStoreUtil.makeDoubleStorage(relation.getDBIDs(), DataStoreFactory.HINT_HOT);
    for(DBIDIter iditer = relation.iterDBIDs(); iditer.valid(); iditer.advance()) {
      String label = relation.get(iditer).toString();
      final double score = (pattern.matcher(label).matches()) ? 1 : 0;
      scores.putDouble(iditer, score);
    }
    DoubleRelation scoreres = new MaterializedDoubleRelation("By label outlier scores", relation.getDBIDs(), scores);
    OutlierScoreMeta meta = new ProbabilisticOutlierScore();
    return new OutlierResult(meta, scoreres);
  }

  /**
   * Parameterization class.
   * 
   * @author Erich Schubert
   */
  public static class Par implements Parameterizer {
    /**
     * The pattern to match outliers with.
     */
    public static final OptionID OUTLIER_PATTERN_ID = new OptionID("outlier.pattern", "Label pattern to match outliers.");

    /**
     * Stores the "outlier" class.
     */
    private Pattern pattern;

    @Override
    public void configure(Parameterization config) {
      new PatternParameter(OUTLIER_PATTERN_ID, DEFAULT_PATTERN) //
          .grab(config, x -> pattern = x);
    }

    @Override
    public ByLabelOutlier make() {
      return new ByLabelOutlier(pattern);
    }
  }
}
