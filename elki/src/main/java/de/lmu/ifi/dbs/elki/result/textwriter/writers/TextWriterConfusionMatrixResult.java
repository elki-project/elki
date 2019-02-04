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
package de.lmu.ifi.dbs.elki.result.textwriter.writers;

import java.io.IOException;

import de.lmu.ifi.dbs.elki.evaluation.classification.ConfusionMatrixEvaluationResult;
import de.lmu.ifi.dbs.elki.result.textwriter.TextWriterStream;
import de.lmu.ifi.dbs.elki.result.textwriter.TextWriterWriterInterface;

/**
 * Write a classification evaluation to a text file.
 * 
 * @author Erich Schubert
 * @since 0.7.0
 */
public class TextWriterConfusionMatrixResult extends TextWriterWriterInterface<ConfusionMatrixEvaluationResult> {
  @Override
  public void write(TextWriterStream out, String label, ConfusionMatrixEvaluationResult eval) throws IOException {
    out.commentPrintLn("Evaluation:");
    out.commentPrintLn(eval.evaluationName);
    // out.println(evaluationProcedure.setting());
    out.commentPrintLn("Accuracy: \n  correctly classified instances: ");
    out.commentPrintLn(eval.confusionmatrix.truePositives());
    out.commentPrintLn("true positive rate:         ");
    double tpr = eval.confusionmatrix.truePositiveRate();
    out.commentPrintLn(tpr);
    out.commentPrintLn("false positive rate:        ");
    out.commentPrintLn(eval.confusionmatrix.falsePositiveRate());
    out.commentPrintLn("positive predicted value:   ");
    double ppv = eval.confusionmatrix.positivePredictedValue();
    out.commentPrintLn(ppv);
    out.commentPrintLn("F1-measure:                 ");
    out.commentPrintLn((2 * ppv * tpr) / (ppv + tpr));
    out.commentPrintLn("\nconfusion matrix:\n");
    out.commentPrintLn(eval.confusionmatrix.toString());
  }
}
