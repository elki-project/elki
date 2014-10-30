package experimentalcode.students.muellerjo.application;

/*
 This file is part of ELKI:
 Environment for Developing KDD-Applications Supported by Index-Structures

 Copyright (C) 2013
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
import java.io.FileNotFoundException;
import java.io.PrintStream;
import java.util.Arrays;

import de.lmu.ifi.dbs.elki.logging.Logging;
import de.lmu.ifi.dbs.elki.logging.Logging.Level;
import de.lmu.ifi.dbs.elki.logging.LoggingConfiguration;
import de.lmu.ifi.dbs.elki.logging.LoggingUtil;
import de.lmu.ifi.dbs.elki.math.Mean;
import de.lmu.ifi.dbs.elki.math.MeanVariance;
import de.lmu.ifi.dbs.elki.math.statistics.ProbabilityWeightedMoments;
import de.lmu.ifi.dbs.elki.utilities.ClassGenericsUtil;
import de.lmu.ifi.dbs.elki.utilities.FormatUtil;
import de.lmu.ifi.dbs.elki.utilities.datastructures.QuickSelect;
import de.lmu.ifi.dbs.elki.utilities.datastructures.arraylike.ArrayLikeUtil;
import de.lmu.ifi.dbs.elki.utilities.exceptions.AbortException;
import de.lmu.ifi.dbs.elki.utilities.exceptions.UnableToComplyException;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.AbstractParameterizer;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.OptionID;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.OptionUtil;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.ParameterException;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.UnspecifiedParameterException;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.constraints.CommonConstraints;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.Parameterization;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.SerializedParameterization;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.TrackParameters;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.ClassParameter;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.DoubleParameter;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.FileParameter;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.Flag;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.IntParameter;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.StringParameter;

/**
 * IDTest Tests different ID Estimations on synthetic distance distributions.
 *
 * @apiviz.uses LoggingConfiguration oneway
 * @apiviz.excludeSubtypes
 */
public class IDTest {
  /**
   * We need a static logger in this class, for code used in "main" methods.
   */
  private static final Logging LOG = Logging.getLogger(IDTest.class);

  /**
   * The newline string according to system.
   */
  private static final String NEWLINE = System.getProperty("line.separator");

  /**
   * Starting value of k.
   */
  final int startk;

  /**
   * Maximum value of k
   */
  final int maxk;

  /**
   * Number of samples
   */
  final int samples;

  /**
   * Target dimension
   */
  final double dim;

  /**
   * Output file
   */
  File outfile;

  /**
   * Constructor.
   */
  public IDTest(int startk, int maxk, int samples, double dim, File outfile) {
    super();
    this.startk = startk;
    this.maxk = maxk;
    this.samples = samples;
    this.dim = dim;
    this.outfile = outfile;
  }

  /**
   * Generic command line invocation.
   *
   * Refactored to have a central place for outermost exception handling.
   *
   * @param cls Application class to run.
   * @param args the arguments to run this application with
   */
  public static void runCLIApplication(Class<?> cls, String[] args) {
    final Flag helpF = new Flag(Parameterizer.HELP_ID);
    final Flag helpLongF = new Flag(Parameterizer.HELP_LONG_ID);
    final ClassParameter<Object> descriptionP = new ClassParameter<>(Parameterizer.DESCRIPTION_ID, Object.class, true);
    final StringParameter debugP = new StringParameter(Parameterizer.DEBUG_ID);
    final Flag verboseF = new Flag(Parameterizer.VERBOSE_ID);
    debugP.setOptional(true);

    SerializedParameterization params = new SerializedParameterization(args);
    try {
      params.grab(helpF);
      params.grab(helpLongF);
      params.grab(descriptionP);
      params.grab(debugP);
      if(descriptionP.isDefined()) {
        params.clearErrors();
        printDescription(descriptionP.getValue());
        return;
      }
      // Fail silently on errors.
      if(params.getErrors().size() > 0) {
        params.logAndClearReportedErrors();
        return;
      }
      if(debugP.isDefined()) {
        LoggingUtil.parseDebugParameter(debugP);
      }
    }
    catch(Exception e) {
      printErrorMessage(e);
      return;
    }
    try {
      TrackParameters config = new TrackParameters(params);
      if(config.grab(verboseF) && verboseF.isTrue()) {
        // Extra verbosity by repeating the flag:
        final Flag verbose2F = new Flag(Parameterizer.VERBOSE_ID);
        if(config.grab(verbose2F) && verbose2F.isTrue()) {
          LoggingConfiguration.setVerbose(Level.VERYVERBOSE);
        }
        else {
          LoggingConfiguration.setVerbose(Level.VERBOSE);
        }
      }
      IDTest task = ClassGenericsUtil.tryInstantiate(IDTest.class, cls, config);

      if((helpF.isDefined() && helpF.getValue()) || (helpLongF.isDefined() && helpLongF.getValue())) {
        LoggingConfiguration.setVerbose(Level.VERBOSE);
        // LOG.verbose(usage(config.getAllParameters()));
      }
      else {
        params.logUnusedParameters();
        if(params.getErrors().size() > 0) {
          LoggingConfiguration.setVerbose(Level.VERBOSE);
          LOG.verbose("The following configuration errors prevented execution:\n");
          for(ParameterException e : params.getErrors()) {
            LOG.verbose(e.getMessage());
          }
          LOG.verbose("\n");
          LOG.verbose("Stopping execution because of configuration errors.");
          System.exit(1);
        }
        else {
          task.run();
        }
      }
    }
    catch(Exception e) {
      printErrorMessage(e);
    }
  }

  /**
   * Print an error message for the given error.
   *
   * @param e Error Exception.
   */
  protected static void printErrorMessage(Exception e) {
    if(e instanceof AbortException) {
      // ensure we actually show the message:
      LoggingConfiguration.setVerbose(Level.VERBOSE);
      LOG.verbose(e.getMessage());
    }
    else if(e instanceof UnspecifiedParameterException) {
      LOG.error(e.getMessage());
    }
    else if(e instanceof ParameterException) {
      LOG.error(e.getMessage());
    }
    else {
      LOG.exception(e);
    }
  }

  /**
   * Print the description for the given parameter
   */
  private static void printDescription(Class<?> descriptionClass) {
    if(descriptionClass != null) {
      LoggingConfiguration.setVerbose(Level.VERBOSE);
      LOG.verbose(OptionUtil.describeParameterizable(new StringBuilder(), descriptionClass, FormatUtil.getConsoleWidth(), "    ").toString());
    }
  }

  /**
   * Estimator according to the Generalized Expansion Dimension method
   * 
   * @param neighbors distances from the query
   * @return intrinsic dimension estimate
   */

  public static double ged(double[] neighbors) {
    double[] meds = new double[(neighbors.length << 1) - 1];
    for(int r = 0; r < neighbors.length; r++) {
      int p = 0;
      for(int r2 = 0; r2 < neighbors.length; r2++) {
        if(r == r2) {
          continue;
        }
        final double dim = Math.log((double) (r + 1) / (double) (r2 + 1)) / Math.log(neighbors[r] / neighbors[r2]);
        meds[r + p] = dim;
        p++;
      }
      meds[r] = QuickSelect.median(meds, r, p + r);
    }
    return QuickSelect.median(meds, 0, neighbors.length);
  }

  /**
   * Estimator according to the Maximum Likelihood method, with additional
   * harmonic mean of all sub-samples
   * 
   * @param neighbors distances from the query
   * @return intrinsic dimension estimate
   */
  public static double avgID(double[] neighbors) {
    double d = 0.0;
    final int n = neighbors.length;
    if(n < 2) {
      return 0.0;
    }
    double id = 0.0;
    double sum = 0.0;
    int p = 0;
    double w = neighbors[1];
    int sumk = 0;
    for(int i = 1; i < n; i++) {
      for(; p < i; p++) {
        sum += Math.log(neighbors[p] / w);
      }
      id += -1.0 * sum;
      sumk += i;
      if(i < n - 1) {
        final double w2 = neighbors[i + 1];
        sum += i * Math.log(w / w2);
        w = w2;
      }
    }
    d = (double) sumk / id;
    return d;
  }

  /**
   * Estimator according to the Maximum Likelihood method
   * 
   * @param neighbors distances from the query
   * @return intrinsic dimension estimate
   */
  public static double hill(double[] neighbors) {
    double d = 0.0;
    final int n = neighbors.length;
    if(n < 2) {
      return 0.0;
    }
    final double w = neighbors[n - 1];
    double sum = 0.0;
    for(int i = 0; i < n - 1; ++i) {
      sum += Math.log(neighbors[i] / w);
    }
    d = -1.0 * (double) (n - 1) / sum;
    return d;
  }

  /**
   * Estimator according to the Probability weighted moments method
   * 
   * @param neighbors distances from the query
   * @return intrinsic dimension estimate
   */
  public static double pwm1(double[] neighbors) {
    Mean m = new Mean();
    final int n = neighbors.length;
    for(int i = 0; i < n; i++) {
      m.put(neighbors[i] * ((i + 1) - 0.35) / ((double) n));
    }
    final double mean = m.getMean() / neighbors[n - 1];
    return mean / (1.0 - 2.0 * mean);
  }

  /**
   * Estimator according to the Probability weighted moments method, using
   * L-moments
   * 
   * @param neighbors distances from the query
   * @return intrinsic dimension estimate
   */
  public static double pwm2(double[] neighbors) {
    final int n = neighbors.length;
    double w = neighbors[n - 1];
    double[] excess = new double[n];
    for(int i = 0; i < n; ++i) {
      excess[i] = w - neighbors[n - i - 1];
    }
    double[] lmom = ProbabilityWeightedMoments.samLMR(excess, ArrayLikeUtil.doubleArrayAdapter(), 2);
    double dim = w / ((lmom[0] * lmom[0] / lmom[1]) - lmom[0]);
    return dim;
  }

  /**
   * Runs the application.
   *
   * @throws UnableToComplyException if an error occurs during running the
   *         application
   */
  public void run() throws UnableToComplyException {
    final PrintStream fout;
    try {
      fout = new PrintStream(outfile);
    }
    catch(FileNotFoundException e) {
      throw new AbortException("Cannot create output file.", e);
    }
    final int digits = (int) Math.ceil(Math.log10(maxk));
    fout.append(String.format("%" + digits + "s GED-Mean     GED-StdDev   AVG-Mean     AVG-StdDev   HILL-Mean    HILL-StdDev  PWM1-Mean    PWM1-StdDev  PWM2-Mean    PWM2-StdDev", "K")).append(FormatUtil.NEWLINE);
    double[][] dists = new double[samples][];
    for(int i = 0; i < samples; i++) {
      dists[i] = new double[maxk];
    }
    final double e = 1. / dim;
    for(int p = 0; p < samples; p++) {
      for(int i = 1; i < startk; i++) {
        double r = Math.random();
        dists[p][i - 1] = Math.pow(r, e);
      }
    }
    for(int l = startk; l <= maxk; l++) {
      MeanVariance[] mvs = new MeanVariance[5];
      for(int i = 0; i < 5; i++) {
        mvs[i] = new MeanVariance();
      }
      String kstr = String.format("%0" + digits + "d", l);
      for(int p = 0; p < samples; p++) {
        double r = Math.random();
        dists[p][l - 1] = Math.pow(r, e);
        Arrays.sort(dists[p], 0, l);
        final double[] sdists = Arrays.copyOf(dists[p], l);
        mvs[0].put(ged(sdists));
        mvs[1].put(avgID(sdists));
        mvs[2].put(hill(sdists));
        mvs[3].put(pwm1(sdists));
        mvs[4].put(pwm2(sdists));
      }
      writeResult(fout, mvs, kstr);
    }
  }

  /**
   * Write a single output line.
   *
   * @param out Output stream
   * @param mvs MeanVariances
   * @param label Identification label
   */
  void writeResult(PrintStream out, MeanVariance[] mvs, String label) {
    out.append(label);
    for(int i = 0; i < mvs.length; i++) {
      String s = "" + mvs[i].getMean() + "             ";
      out.append(' ').append(s.substring(0, 12));
      s = "" + mvs[i].getSampleStddev() + "             ";
      out.append(' ').append(s.substring(0, 12));
    }
    out.append(FormatUtil.NEWLINE);
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
     * Parameter that specifies the name of the output file.
     * <p>
     * Key: {@code -app.out}
     * </p>
     */
    public static final OptionID OUTPUT_ID = new OptionID("app.out", "");

    /**
     * Flag to obtain help-message.
     * <p>
     * Key: {@code -h}
     * </p>
     */
    public static final OptionID HELP_ID = new OptionID("h", "Request a help-message, either for the main-routine or for any specified algorithm. " + "Causes immediate stop of the program.");

    /**
     * Flag to obtain help-message.
     * <p>
     * Key: {@code -help}
     * </p>
     */
    public static final OptionID HELP_LONG_ID = new OptionID("help", "Request a help-message, either for the main-routine or for any specified algorithm. " + "Causes immediate stop of the program.");

    /**
     * Optional Parameter to specify a class to obtain a description for.
     * <p>
     * Key: {@code -description}
     * </p>
     */
    public static final OptionID DESCRIPTION_ID = new OptionID("description", "Class to obtain a description of. " + "Causes immediate stop of the program.");

    /**
     * Optional Parameter to specify a class to enable debugging for.
     * <p>
     * Key: {@code -enableDebug}
     * </p>
     */
    public static final OptionID DEBUG_ID = new OptionID("enableDebug", "Parameter to enable debugging for particular packages.");

    /**
     * Flag to allow verbose messages while running the application.
     * <p>
     * Key: {@code -verbose}
     * </p>
     */
    public static final OptionID VERBOSE_ID = new OptionID("verbose", "Enable verbose messages.");

    /**
     * Option ID for k start size.
     */
    public static final OptionID STARTK_ID = new OptionID("startk", "Minimum value for k.");

    /**
     * Option ID for k max size.
     */
    public static final OptionID MAXK_ID = new OptionID("maxk", "Maximum value for k.");

    /**
     * Option ID for the samples.
     */
    public static final OptionID SAMPLES_ID = new OptionID("samples", "Number of samples to draw.");

    /**
     * Option ID for the Target dimension.
     */
    public static final OptionID DIM_ID = new OptionID("dim", "Target dimension for of the distribution.");

    /**
     * starting value of k
     */
    int startk;

    /**
     * Maximum value of k
     */
    int maxk;

    /**
     * Number of samples
     */
    int samples;

    /**
     * Target dimension
     */
    double dim;

    /**
     * Output destination file
     */
    File outfile;

    /**
     * Get the output file parameter.
     *
     * @param config Options
     * @return Output file
     */

    protected File getParameterOutputFile(Parameterization config) {
      return getParameterOutputFile(config, "Output filename.");
    }

    /**
     * Get the output file parameter.
     *
     * @param config Options
     * @param description Short description
     * @return Output file
     */
    protected File getParameterOutputFile(Parameterization config, String description) {
      final FileParameter outputP = new FileParameter(OUTPUT_ID, FileParameter.FileType.OUTPUT_FILE);
      outputP.setShortDescription(description);
      if(config.grab(outputP)) {
        return outputP.getValue();
      }
      return null;
    }

    @Override
    protected void makeOptions(Parameterization config) {
      super.makeOptions(config);

      IntParameter startkP = new IntParameter(STARTK_ID);
      startkP.addConstraint(CommonConstraints.GREATER_THAN_ONE_INT);
      startkP.setOptional(true);
      if(config.grab(startkP)) {
        startk = startkP.getValue();
      }

      IntParameter maxkP = new IntParameter(MAXK_ID);
      maxkP.addConstraint(CommonConstraints.GREATER_THAN_ONE_INT);
      if(config.grab(maxkP)) {
        maxk = maxkP.getValue();
      }

      IntParameter sampleP = new IntParameter(SAMPLES_ID);
      sampleP.addConstraint(CommonConstraints.GREATER_EQUAL_ONE_INT);
      if(config.grab(sampleP)) {
        samples = sampleP.getValue();
      }

      DoubleParameter dimP = new DoubleParameter(DIM_ID);
      dimP.addConstraint(CommonConstraints.GREATER_THAN_ZERO_DOUBLE);
      if(config.grab(dimP)) {
        dim = dimP.getValue();
      }
      // Output
      outfile = getParameterOutputFile(config);
    }

    @Override
    protected IDTest makeInstance() {
      return new IDTest(startk, maxk, samples, dim, outfile);
    }
  }

  /**
   * Main method.
   *
   * @param args Command line parameters.
   */
  public static void main(String[] args) {
    runCLIApplication(IDTest.class, args);
  }
}
