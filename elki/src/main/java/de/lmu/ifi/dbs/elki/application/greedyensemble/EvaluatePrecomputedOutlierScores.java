package de.lmu.ifi.dbs.elki.application.greedyensemble;

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

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.channels.FileChannel;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import de.lmu.ifi.dbs.elki.application.AbstractApplication;
import de.lmu.ifi.dbs.elki.data.NumberVector;
import de.lmu.ifi.dbs.elki.data.type.TypeUtil;
import de.lmu.ifi.dbs.elki.database.Database;
import de.lmu.ifi.dbs.elki.database.ids.DBID;
import de.lmu.ifi.dbs.elki.database.ids.DBIDIter;
import de.lmu.ifi.dbs.elki.database.ids.DBIDUtil;
import de.lmu.ifi.dbs.elki.database.relation.Relation;
import de.lmu.ifi.dbs.elki.evaluation.scores.AveragePrecisionEvaluation;
import de.lmu.ifi.dbs.elki.evaluation.scores.MaximumF1Evaluation;
import de.lmu.ifi.dbs.elki.evaluation.scores.PrecisionAtKEvaluation;
import de.lmu.ifi.dbs.elki.evaluation.scores.ROCEvaluation;
import de.lmu.ifi.dbs.elki.evaluation.scores.adapter.AbstractVectorIter;
import de.lmu.ifi.dbs.elki.evaluation.scores.adapter.DecreasingVectorIter;
import de.lmu.ifi.dbs.elki.evaluation.scores.adapter.IncreasingVectorIter;
import de.lmu.ifi.dbs.elki.evaluation.scores.adapter.VectorNonZero;
import de.lmu.ifi.dbs.elki.logging.Logging;
import de.lmu.ifi.dbs.elki.utilities.DatabaseUtil;
import de.lmu.ifi.dbs.elki.utilities.exceptions.AbortException;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.OptionID;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.Parameterization;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.PatternParameter;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.StringParameter;
import de.lmu.ifi.dbs.elki.workflow.InputStep;

/**
 * Class to load an outlier detection summary file, as produced by
 * {@link ComputeKNNOutlierScores}, and compute popular evaluation metrics for
 * it.
 *
 * File format description:
 * <ul>
 * <li>Each column is one object in the data set</li>
 * <li>Each line is a different algorithm</li>
 * <li>There is a mandatory label column, containing the method name</li>
 * <li>The first line <i>must</i> contain the ground-truth, titled
 * <tt>bylabel</tt>, where <tt>0</tt> indicates an inlier and <tt>1</tt>
 * indicates an outlier</li>
 * </ul>
 *
 * The evaluation assumes that high scores correspond to outliers, unless the
 * method name matches the pattern given using {@link Parameterizer#REVERSED_ID}
 * (Default: <tt>(ODIN|ABOD)</tt>).
 *
 * @author Erich Schubert
 * @author Guilherme Oliveira Campos
 * @since 0.7.0
 */
public class EvaluatePrecomputedOutlierScores extends AbstractApplication {
  /**
   * Get static logger.
   */
  private static final Logging LOG = Logging.getLogger(EvaluatePrecomputedOutlierScores.class);

  /**
   * The data input part.
   */
  InputStep inputstep;

  /**
   * Pattern to recognize reversed methods.
   */
  Pattern reverse;

  /**
   * Output file name
   */
  File outfile;

  /**
   * Constant column to prepend (may be null)
   */
  String name;

  /**
   * Constructor.
   *
   * @param inputstep Input step
   * @param reverse Pattern for reversed outlier scores.
   * @param outfile Output file name
   * @param name Constant column to prepend
   */
  public EvaluatePrecomputedOutlierScores(InputStep inputstep, Pattern reverse, File outfile, String name) {
    super();
    this.inputstep = inputstep;
    this.reverse = reverse;
    this.outfile = outfile;
    this.name = name;
  }

  @Override
  public void run() {
    // Note: the database contains the *result vectors*, not the original data.
    final Database database = inputstep.getDatabase();
    final Relation<NumberVector> relation = database.getRelation(TypeUtil.NUMBER_VECTOR_FIELD);
    final Relation<String> labels = DatabaseUtil.guessLabelRepresentation(database);
    final DBID firstid = DBIDUtil.deref(labels.iterDBIDs());
    final String firstlabel = labels.get(firstid);
    if(!firstlabel.matches("bylabel")) {
      throw new AbortException("No 'by label' reference outlier found, which is needed for evaluation!");
    }

    // Reference vector
    final NumberVector refvec = relation.get(firstid);
    // Build the positive index set for ROC AUC.
    VectorNonZero positive = new VectorNonZero(refvec);
    double rate = positive.numPositive() / (double) refvec.getDimensionality();

    try (FileOutputStream fosResult = new FileOutputStream(outfile, true);
        PrintStream fout = new PrintStream(fosResult);
        FileChannel chan = fosResult.getChannel()) {
      chan.lock();
      if(chan.position() == 0L) {
        // Write CSV header:
        if(name != null) {
          fout.append("\"Name\",");
        }
        fout.append("\"Algorithm\",\"k\"");
        fout.append(",\"ROC AUC\"");
        fout.append(",\"Average Precision\"");
        fout.append(",\"R-Precision\"");
        fout.append(",\"Maximum F1\"");
        fout.append(",\"Adjusted ROC AUC\"");
        fout.append(",\"Adjusted Average Precision\"");
        fout.append(",\"Adjusted R-Precision\"");
        fout.append(",\"Adjusted Maximum F1\"");
        fout.append('\n');
      }
      Matcher m = reverse.matcher("");
      // Compute individual scores
      for(DBIDIter iditer = relation.iterDBIDs(); iditer.valid(); iditer.advance()) {
        if(DBIDUtil.equal(firstid, iditer)) {
          continue;
        }
        String label = labels.get(iditer);
        final NumberVector vec = relation.get(iditer);
        if(checkForNaNs(vec)) {
          LOG.warning("NaN value encountered in vector " + label);
          continue;
        }
        AbstractVectorIter iter = m.reset(label).find() ? new IncreasingVectorIter(vec) : new DecreasingVectorIter(vec);
        double auc = ROCEvaluation.STATIC.evaluate(positive, iter.seek(0));
        double avep = AveragePrecisionEvaluation.STATIC.evaluate(positive, iter.seek(0));
        double rprecision = PrecisionAtKEvaluation.RPRECISION.evaluate(positive, iter.seek(0));
        double maxf1 = MaximumF1Evaluation.STATIC.evaluate(positive, iter.seek(0));
        double adjauc = 2 * auc - 1;
        double adjrprecision = (rprecision - rate) / (1 - rate);
        double adjavep = (avep - rate) / (1 - rate);
        double adjmaxf1 = (maxf1 - rate) / (1 - rate);
        String prefix = label.substring(0, label.lastIndexOf('-'));
        int k = Integer.valueOf(label.substring(label.lastIndexOf('-') + 1));
        // Write CSV
        if(name != null) {
          fout.append("\"" + name + "\",");
        }
        fout.append("\"" + prefix + "\"," + k);
        fout.append(',').append(Double.toString(auc));
        fout.append(',').append(Double.toString(avep));
        fout.append(',').append(Double.toString(rprecision));
        fout.append(',').append(Double.toString(maxf1));
        fout.append(',').append(Double.toString(adjauc));
        fout.append(',').append(Double.toString(adjavep));
        fout.append(',').append(Double.toString(adjrprecision));
        fout.append(',').append(Double.toString(adjmaxf1));
        fout.append('\n');
      }
    }
    catch(IOException e) {
      LOG.exception(e);
    }
  }

  /**
   * Check for NaN values.
   *
   * @param vec Vector
   * @return {@code true} if NaN values are present.
   */
  private boolean checkForNaNs(NumberVector vec) {
    for(int i = 0, d = vec.getDimensionality(); i < d; i++) {
      double v = vec.doubleValue(i);
      if(v != v) { // NaN!
        return true;
      }
    }
    return false;
  }

  /**
   * Parameterization class.
   *
   * @author Erich Schubert
   *
   * @apiviz.exclude
   */
  public static class Parameterizer extends AbstractApplication.Parameterizer {
    /**
     * Row name.
     */
    public static final OptionID NAME_ID = new OptionID("name", "Data set name to use in a 'Name' CSV column.");

    /**
     * Pattern for reversed methods.
     */
    public static final OptionID REVERSED_ID = new OptionID("reversed", "Pattern to recognize reversed methods.");

    /**
     * Data source.
     */
    InputStep inputstep;

    /**
     * Pattern to recognize reversed methods.
     */
    Pattern reverse;

    /**
     * Output destination file
     */
    File outfile;

    /**
     * Name column to prepend.
     */
    String name;

    @Override
    protected void makeOptions(Parameterization config) {
      super.makeOptions(config);
      // Data input
      inputstep = config.tryInstantiate(InputStep.class);
      outfile = super.getParameterOutputFile(config, "File to output the resulting score vectors to.");
      // Row name prefix
      StringParameter nameP = new StringParameter(NAME_ID);
      nameP.setOptional(true);
      if(config.grab(nameP)) {
        name = nameP.getValue();
      }
      // Pattern for reversed methods:
      PatternParameter reverseP = new PatternParameter(REVERSED_ID, "(ODIN|ABOD)");
      if(config.grab(reverseP)) {
        reverse = reverseP.getValue();
      }
    }

    @Override
    protected EvaluatePrecomputedOutlierScores makeInstance() {
      return new EvaluatePrecomputedOutlierScores(inputstep, reverse, outfile, name);
    }
  }

  /**
   * Main method.
   *
   * @param args Command line parameters.
   */
  public static void main(String[] args) {
    runCLIApplication(EvaluatePrecomputedOutlierScores.class, args);
  }
}
