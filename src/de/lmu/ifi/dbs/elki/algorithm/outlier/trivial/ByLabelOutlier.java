package de.lmu.ifi.dbs.elki.algorithm.outlier.trivial;
/*
This file is part of ELKI:
Environment for Developing KDD-Applications Supported by Index-Structures

Copyright (C) 2011
Ludwig-Maximilians-Universität München
Lehr- und Forschungseinheit für Datenbanksysteme
ELKI Development Team

This program is free software: you can redistribute it and/or modify
it under the terms of the GNU Affero General Public License as published by
the Free Software Foundation, either version 3 of the License, or
(at your option) any later version.

This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU Affero General Public License for more details.

You should have received a copy of the GNU Affero General Public License
along with this program.  If not, see <http://www.gnu.org/licenses/>.
*/

import java.util.regex.Pattern;

import de.lmu.ifi.dbs.elki.algorithm.AbstractAlgorithm;
import de.lmu.ifi.dbs.elki.algorithm.outlier.OutlierAlgorithm;
import de.lmu.ifi.dbs.elki.data.ClassLabel;
import de.lmu.ifi.dbs.elki.data.type.TypeInformation;
import de.lmu.ifi.dbs.elki.data.type.TypeUtil;
import de.lmu.ifi.dbs.elki.database.Database;
import de.lmu.ifi.dbs.elki.database.datastore.DataStoreFactory;
import de.lmu.ifi.dbs.elki.database.datastore.DataStoreUtil;
import de.lmu.ifi.dbs.elki.database.datastore.WritableDataStore;
import de.lmu.ifi.dbs.elki.database.ids.DBID;
import de.lmu.ifi.dbs.elki.database.relation.MaterializedRelation;
import de.lmu.ifi.dbs.elki.database.relation.Relation;
import de.lmu.ifi.dbs.elki.logging.Logging;
import de.lmu.ifi.dbs.elki.result.outlier.OutlierResult;
import de.lmu.ifi.dbs.elki.result.outlier.OutlierScoreMeta;
import de.lmu.ifi.dbs.elki.result.outlier.ProbabilisticOutlierScore;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.AbstractParameterizer;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.OptionID;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.Parameterization;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.PatternParameter;

/**
 * Trivial algorithm that marks outliers by their label. Can be used as
 * reference algorithm in comparisons.
 * 
 * @author Erich Schubert
 */
public class ByLabelOutlier extends AbstractAlgorithm<OutlierResult> implements OutlierAlgorithm {
  /**
   * Our logger.
   */
  private static final Logging logger = Logging.getLogger(ByLabelOutlier.class);

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
  public OutlierResult run(Database database) {
    // Prefer a true class label
    Relation<ClassLabel> relation = database.getRelation(TypeUtil.CLASSLABEL);
    if(relation != null) {
      return run(relation);
    }
    // Otherwise, try any labellike.
    return run(database.getRelation(getInputTypeRestriction()[0]));
  }
  
  /**
   * Run the algorithm
   * 
   * @param relation Relation to process.
   * @return Result
   */
  public OutlierResult run(Relation<?> relation) {
    WritableDataStore<Double> scores = DataStoreUtil.makeStorage(relation.getDBIDs(), DataStoreFactory.HINT_HOT, Double.class);
    for(DBID id : relation.iterDBIDs()) {
      String label = relation.get(id).toString();
      final double score;
      if (pattern.matcher(label).matches()) {
        score = 1.0;
      } else {
        score = 0.0;
      }
      scores.put(id, score);
    }
    Relation<Double> scoreres = new MaterializedRelation<Double>("By label outlier scores", "label-outlier", TypeUtil.DOUBLE, scores, relation.getDBIDs());
    OutlierScoreMeta meta = new ProbabilisticOutlierScore();
    return new OutlierResult(meta, scoreres);
  }

  @Override
  protected Logging getLogger() {
    return logger;
  }

  /**
   * Parameterization class.
   * 
   * @author Erich Schubert
   * 
   * @apiviz.exclude
   */
  public static class Parameterizer extends AbstractParameterizer {
    /**
     * The pattern to match outliers with.
     * 
     * <p>
     * Default value: .*(Outlier|Noise).*
     * </p>
     * <p>
     * Key: {@code -outlier.pattern}
     * </p>
     */
    public static final OptionID OUTLIER_PATTERN_ID = OptionID.getOrCreateOptionID("outlier.pattern", "Label pattern to match outliers.");
    /**
     * Stores the "outlier" class.
     */
    private Pattern pattern;

    @Override
    protected void makeOptions(Parameterization config) {
      super.makeOptions(config);
      PatternParameter patternP = new PatternParameter(OUTLIER_PATTERN_ID, DEFAULT_PATTERN);
      if(config.grab(patternP)) {
        pattern = patternP.getValue();
      }
    }

    @Override
    protected ByLabelOutlier makeInstance() {
      return new ByLabelOutlier(pattern);
    }
  }
}