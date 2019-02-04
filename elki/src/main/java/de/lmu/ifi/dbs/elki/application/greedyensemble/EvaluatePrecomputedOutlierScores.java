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
package de.lmu.ifi.dbs.elki.application.greedyensemble;

import java.io.*;
import java.nio.channels.FileChannel;
import java.util.regex.Pattern;

import de.lmu.ifi.dbs.elki.application.AbstractApplication;
import de.lmu.ifi.dbs.elki.data.NumberVector;
import de.lmu.ifi.dbs.elki.data.type.SimpleTypeInformation;
import de.lmu.ifi.dbs.elki.data.type.TypeUtil;
import de.lmu.ifi.dbs.elki.datasource.bundle.BundleMeta;
import de.lmu.ifi.dbs.elki.datasource.bundle.BundleStreamSource;
import de.lmu.ifi.dbs.elki.datasource.parser.NumberVectorLabelParser;
import de.lmu.ifi.dbs.elki.datasource.parser.StreamingParser;
import de.lmu.ifi.dbs.elki.evaluation.scores.*;
import de.lmu.ifi.dbs.elki.evaluation.scores.adapter.AbstractVectorIter;
import de.lmu.ifi.dbs.elki.evaluation.scores.adapter.DecreasingVectorIter;
import de.lmu.ifi.dbs.elki.evaluation.scores.adapter.IncreasingVectorIter;
import de.lmu.ifi.dbs.elki.evaluation.scores.adapter.VectorNonZero;
import de.lmu.ifi.dbs.elki.logging.Logging;
import de.lmu.ifi.dbs.elki.utilities.exceptions.AbortException;
import de.lmu.ifi.dbs.elki.utilities.io.FileUtil;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.OptionID;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.Parameterization;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.ObjectParameter;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.PatternParameter;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.StringParameter;

/**
 * Class to load an outlier detection summary file, as produced by
 * {@link ComputeKNNOutlierScores}, and compute popular evaluation metrics for
 * it.
 * <p>
 * File format description:
 * <ul>
 * <li>Each column is one object in the data set</li>
 * <li>Each line is a different algorithm</li>
 * <li>There is a mandatory label column, containing the method name</li>
 * <li>The first line <i>must</i> contain the ground-truth, titled
 * <tt>bylabel</tt>, where <tt>0</tt> indicates an inlier and <tt>1</tt>
 * indicates an outlier</li>
 * </ul>
 * The evaluation assumes that high scores correspond to outliers, unless the
 * method name matches the pattern given using {@code -reversed}.
 * The default value matches several scores known to use reversed values.
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
   * Pattern to match a set of known reversed scores.
   */
  public static final String KNOWN_REVERSED = "(ODIN|DWOF|gaussian-model|silhouette|OutRank|OUTRES|aggarwal.?yu|ABOD)";

  /**
   * The data input file.
   */
  File infile;

  /**
   * Parser to read input data.
   */
  StreamingParser parser;

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
   * Vector of positive values.
   */
  VectorNonZero positive;

  /**
   * Normalization term E[NDCG].
   */
  double endcg;

  /**
   * Constructor.
   *
   * @param infile Input file
   * @param parser Streaming input parser
   * @param reverse Pattern for reversed outlier scores.
   * @param outfile Output file name
   * @param name Constant column to prepend
   */
  public EvaluatePrecomputedOutlierScores(File infile, StreamingParser parser, Pattern reverse, File outfile, String name) {
    super();
    this.infile = infile;
    this.parser = parser;
    this.reverse = reverse;
    this.outfile = outfile;
    this.name = name;
  }

  @Override
  public void run() {
    try (FileInputStream fis = new FileInputStream(infile); //
        InputStream is = new BufferedInputStream(FileUtil.tryGzipInput(fis)); //
        FileOutputStream fosResult = new FileOutputStream(outfile, true);
        PrintStream fout = new PrintStream(fosResult);
        FileChannel chan = fosResult.getChannel()) {
      // Setup the input stream.
      parser.initStream(is);
      // Lock the output file:
      chan.lock();
      if(chan.position() == 0L) {
        writeHeader(fout);
      }
      else {
        LOG.info("Appending to existing output " + outfile);
      }
      int lcol = -1, dcol = -1;
      loop: while(true) {
        BundleStreamSource.Event ev = parser.nextEvent();
        switch(ev){
        case END_OF_STREAM:
          break loop;
        case META_CHANGED:
          BundleMeta meta = parser.getMeta();
          lcol = -1;
          dcol = -1;
          for(int i = 0; i < meta.size(); i++) {
            SimpleTypeInformation<?> m = meta.get(i);
            if(TypeUtil.NUMBER_VECTOR_VARIABLE_LENGTH.isAssignableFromType(m)) {
              if(dcol >= 0) {
                throw new AbortException("More than one vector column.");
              }
              dcol = i;
            }
            else if(TypeUtil.GUESSED_LABEL.isAssignableFromType(m)) {
              if(lcol >= 0) {
                throw new AbortException("More than one label column.");
              }
              lcol = i;
            }
            else {
              throw new AbortException("Unexpected data column type: " + m);
            }
          }
          break;
        case NEXT_OBJECT:
          if(lcol < 0) {
            throw new AbortException("No label column available.");
          }
          if(dcol < 0) {
            throw new AbortException("No vector column available.");
          }
          processRow(fout, (NumberVector) parser.data(dcol), parser.data(lcol).toString());
          break;
        }
      }
    }
    catch(IOException e) {
      throw new AbortException("IO error.", e);
    }
  }

  private void writeHeader(PrintStream fout) {
    // Write CSV header:
    if(name != null) {
      fout.append("\"Name\",");
    }
    fout.append("\"Algorithm\",\"k\"") //
        .append(",\"ROC AUC\"") //
        .append(",\"Average Precision\"") //
        .append(",\"R-Precision\"") //
        .append(",\"Maximum F1\"") //
        .append(",\"DCG\"") //
        .append(",\"NDCG\"") //
        .append(",\"Adjusted ROC AUC\"") //
        .append(",\"Adjusted Average Precision\"") //
        .append(",\"Adjusted R-Precision\"") //
        .append(",\"Adjusted Maximum F1\"") //
        .append(",\"Adjusted DCG\"") //
        .append('\n');
  }

  private void processRow(PrintStream fout, NumberVector vec, String label) {
    if(checkForNaNs(vec)) {
      LOG.warning("NaN value encountered in vector " + label);
      return;
    }
    if(positive == null) {
      if(!label.matches("bylabel")) {
        throw new AbortException("No 'by label' reference outlier found, which is needed for evaluation!");
      }
      positive = new VectorNonZero(vec);
      endcg = NDCGEvaluation.STATIC.expected(positive.numPositive(), positive.getDimensionality());
      return;
    }
    AbstractVectorIter iter = reverse.matcher(label).find() ? new IncreasingVectorIter(vec) : new DecreasingVectorIter(vec);
    double expected = positive.numPositive() / (double) positive.getDimensionality();
    double auc = ROCEvaluation.STATIC.evaluate(positive, iter.seek(0));
    double avep = AveragePrecisionEvaluation.STATIC.evaluate(positive, iter.seek(0));
    double rprecision = PrecisionAtKEvaluation.RPRECISION.evaluate(positive, iter.seek(0));
    double maxf1 = MaximumF1Evaluation.STATIC.evaluate(positive, iter.seek(0));
    double dcg = DCGEvaluation.STATIC.evaluate(positive, iter.seek(0));
    double ndcg = NDCGEvaluation.STATIC.evaluate(positive, iter.seek(0));
    double adjauc = 2 * auc - 1;
    double adjrprecision = (rprecision - expected) / (1 - expected);
    double adjavep = (avep - expected) / (1 - expected);
    double adjmaxf1 = (maxf1 - expected) / (1 - expected);
    double adjdcg = (ndcg - endcg) / (1 - endcg);
    final int p = label.lastIndexOf('-');
    String prefix = label.substring(0, p);
    int k = Integer.valueOf(label.substring(p + 1));
    // Write CSV
    if(name != null) {
      fout.append('"').append(name).append("\",");
    }
    fout.append('"').append(prefix).append('"') //
        .append(',').append(Integer.toString(k)) //
        .append(',').append(Double.toString(auc)) //
        .append(',').append(Double.toString(avep)) //
        .append(',').append(Double.toString(rprecision)) //
        .append(',').append(Double.toString(maxf1)) //
        .append(',').append(Double.toString(dcg)) //
        .append(',').append(Double.toString(ndcg)) //
        .append(',').append(Double.toString(adjauc)) //
        .append(',').append(Double.toString(adjavep)) //
        .append(',').append(Double.toString(adjrprecision)) //
        .append(',').append(Double.toString(adjmaxf1)) //
        .append(',').append(Double.toString(adjdcg)) //
        .append('\n');
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
   */
  public static class Parameterizer extends AbstractApplication.Parameterizer {
    /**
     * Row name.
     */
    public static final OptionID NAME_ID = new OptionID("name", "Data set name to use in a 'Name' CSV column.");

    /**
     * Input parser.
     */
    public static final OptionID PARSER_ID = new OptionID("parser", "Input parser.");

    /**
     * Pattern for reversed methods.
     */
    public static final OptionID REVERSED_ID = new OptionID("reversed", "Pattern to recognize reversed methods.");

    /**
     * Data source.
     */
    File infile;

    /**
     * Parser to read input data.
     */
    StreamingParser parser;

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
      infile = super.getParameterInputFile(config, "Input file containing the outlier score vectors.");
      ObjectParameter<StreamingParser> parserP = new ObjectParameter<>(PARSER_ID, StreamingParser.class, NumberVectorLabelParser.class);
      if(config.grab(parserP)) {
        parser = parserP.instantiateClass(config);
      }
      outfile = super.getParameterOutputFile(config, "File to output the resulting evaluation vectors to.");
      // Row name prefix
      StringParameter nameP = new StringParameter(NAME_ID) //
          .setOptional(true);
      if(config.grab(nameP)) {
        name = nameP.getValue();
      }
      // Pattern for reversed methods:
      PatternParameter reverseP = new PatternParameter(REVERSED_ID, KNOWN_REVERSED);
      if(config.grab(reverseP)) {
        reverse = reverseP.getValue();
      }
    }

    @Override
    protected EvaluatePrecomputedOutlierScores makeInstance() {
      return new EvaluatePrecomputedOutlierScores(infile, parser, reverse, outfile, name);
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
