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
package de.lmu.ifi.dbs.elki.evaluation.classification;

import de.lmu.ifi.dbs.elki.result.Result;

/**
 * Provides the prediction performance measures for a classifier based on the
 * confusion matrix.
 *
 * Note: this API is non-final, and will be refactored soon.
 *
 * @author Arthur Zimek
 * @since 0.7.0
 *
 * @composed - - - ConfusionMatrix
 */
public class ConfusionMatrixEvaluationResult implements Result {
  /**
   * Holds the confusion matrix.
   */
  public final ConfusionMatrix confusionmatrix;

  /**
   * Holds the used EvaluationProcedure.
   */
  public final String evaluationName;

  /**
   * Provides an evaluation based on the given confusion matrix.
   *
   * @param confusionmatrix the confusion matrix to provide the prediction
   *        performance measures for
   * @param evaluationName name of the evaluation procedure used
   */
  public ConfusionMatrixEvaluationResult(ConfusionMatrix confusionmatrix, String evaluationName) {
    super();
    this.confusionmatrix = confusionmatrix;
    this.evaluationName = evaluationName;
  }

  @Override
  public String getLongName() {
    return "confusionmatrixresult";
  }

  @Override
  public String getShortName() {
    return "confusionmatrixresult";
  }
}
