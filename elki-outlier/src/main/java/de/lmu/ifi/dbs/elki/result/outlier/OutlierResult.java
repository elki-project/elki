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
package de.lmu.ifi.dbs.elki.result.outlier;

import java.util.ArrayList;
import java.util.List;

import de.lmu.ifi.dbs.elki.database.ids.DBIDUtil;
import de.lmu.ifi.dbs.elki.database.relation.DoubleRelation;
import de.lmu.ifi.dbs.elki.evaluation.scores.ScoreEvaluation;
import de.lmu.ifi.dbs.elki.evaluation.scores.adapter.DBIDsTest;
import de.lmu.ifi.dbs.elki.evaluation.scores.adapter.OutlierScoreAdapter;
import de.lmu.ifi.dbs.elki.result.Metadata;
import de.lmu.ifi.dbs.elki.result.Metadata.Hierarchy;
import de.lmu.ifi.dbs.elki.result.OrderingResult;

/**
 * Wrap a typical Outlier result, keeping direct references to the main result
 * parts.
 * 
 * @author Erich Schubert
 * @since 0.3
 * 
 * @opt nodefillcolor LemonChiffon
 * @composed - - - OutlierScoreMeta
 * @navcomposed - - - DoubleRelation
 * @composed - - - OrderingFromRelation
 */
public class OutlierResult {
  /**
   * Outlier score meta information
   */
  private OutlierScoreMeta meta;

  /**
   * Outlier scores.
   */
  private DoubleRelation scores;

  /**
   * Outlier ordering.
   */
  private OrderingResult ordering;

  /**
   * Constructor.
   * 
   * @param meta Outlier score metadata.
   * @param scores Scores result.
   */
  public OutlierResult(OutlierScoreMeta meta, DoubleRelation scores) {
    super();
    this.meta = meta;
    this.scores = scores;
    this.ordering = new OrderingFromRelation(scores, meta instanceof InvertedOutlierScoreMeta);
    Metadata.of(this).setLongName(scores.getLongName());
    Hierarchy hier = Metadata.hierarchyOf(this);
    hier.addChild(scores);
    hier.addChild(ordering);
    hier.addChild(meta);
  }

  /**
   * Get the outlier score meta data
   * 
   * @return the outlier meta information
   */
  public OutlierScoreMeta getOutlierMeta() {
    return meta;
  }

  /**
   * Get the outlier scores association.
   * 
   * @return the scores
   */
  public DoubleRelation getScores() {
    return scores;
  }

  /**
   * Get the outlier ordering
   * 
   * @return the ordering
   */
  public OrderingResult getOrdering() {
    return ordering;
  }

  /**
   * Collect all outlier results from a Result
   *
   * @param r Result
   * @return List of outlier results
   */
  public static List<OutlierResult> getOutlierResults(Object r) {
    return Metadata.hierarchyOf(r).iterDescendantsSelf()//
        .filter(OutlierResult.class).collect(new ArrayList<>());
  }

  /**
   * Evaluate given a set of positives and a scoring.
   *
   * @param eval Evaluation measure
   * @return Score
   */
  double evaluateBy(ScoreEvaluation eval) {
    return eval.evaluate(new DBIDsTest(DBIDUtil.ensureSet(scores.getDBIDs())), new OutlierScoreAdapter(this));
  }
}
