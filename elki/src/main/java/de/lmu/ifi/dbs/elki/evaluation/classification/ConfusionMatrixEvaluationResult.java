package de.lmu.ifi.dbs.elki.evaluation.classification;

/*
 This file is part of ELKI:
 Environment for Developing KDD-Applications Supported by Index-Structures

 Copyright (C) 2015
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

import de.lmu.ifi.dbs.elki.result.Result;
import de.lmu.ifi.dbs.elki.result.textwriter.TextWriteable;
import de.lmu.ifi.dbs.elki.result.textwriter.TextWriterStream;

/**
 * Provides the prediction performance measures for a classifier based on the
 * confusion matrix.
 *
 * Note: this API is non-final, and will be refactored soon.
 *
 * @author Arthur Zimek
 * @since 0.7.0
 *
 * @apiviz.composedOf ConfusionMatrix
 */
public class ConfusionMatrixEvaluationResult implements Result, TextWriteable {
  /**
   * Holds the confusion matrix.
   */
  private ConfusionMatrix confusionmatrix;

  /**
   * Holds the used EvaluationProcedure.
   */
  private String evaluationName;

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
  public void writeToText(TextWriterStream out, String label) {
    out.commentPrintLn("Evaluation:");
    out.commentPrintLn(evaluationName);
    // out.println(evaluationProcedure.setting());
    out.commentPrintLn("Accuracy: \n  correctly classified instances: ");
    out.commentPrintLn(confusionmatrix.truePositives());
    out.commentPrintLn("true positive rate:         ");
    double tpr = confusionmatrix.truePositiveRate();
    out.commentPrintLn(tpr);
    out.commentPrintLn("false positive rate:        ");
    out.commentPrintLn(confusionmatrix.falsePositiveRate());
    out.commentPrintLn("positive predicted value:   ");
    double ppv = confusionmatrix.positivePredictedValue();
    out.commentPrintLn(ppv);
    out.commentPrintLn("F1-measure:                 ");
    out.commentPrintLn((2 * ppv * tpr) / (ppv + tpr));
    out.commentPrintLn("\nconfusion matrix:\n");
    out.commentPrintLn(confusionmatrix.toString());
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
