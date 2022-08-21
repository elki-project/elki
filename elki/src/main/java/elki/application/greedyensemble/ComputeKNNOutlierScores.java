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
package elki.application.greedyensemble;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;
import java.util.function.BiConsumer;
import java.util.function.IntFunction;
import java.util.regex.Pattern;

import elki.application.AbstractDistanceBasedApplication;
import elki.data.DoubleVector;
import elki.data.NumberVector;
import elki.data.type.TypeUtil;
import elki.database.Database;
import elki.database.ids.DBIDIter;
import elki.database.ids.DBIDRef;
import elki.database.ids.DBIDs;
import elki.database.query.QueryBuilder;
import elki.database.query.knn.KNNSearcher;
import elki.database.query.knn.PreprocessorKNNQuery;
import elki.database.relation.DoubleRelation;
import elki.database.relation.Relation;
import elki.distance.Distance;
import elki.distance.minkowski.EuclideanDistance;
import elki.distance.minkowski.SquaredEuclideanDistance;
import elki.logging.Logging;
import elki.logging.statistics.Duration;
import elki.math.statistics.intrinsicdimensionality.AggregatedHillEstimator;
import elki.math.statistics.kernelfunctions.GaussianKernelDensityFunction;
import elki.outlier.DWOF;
import elki.outlier.anglebased.FastABOD;
import elki.outlier.distance.*;
import elki.outlier.intrinsic.IDOS;
import elki.outlier.intrinsic.ISOS;
import elki.outlier.intrinsic.LID;
import elki.outlier.lof.*;
import elki.outlier.trivial.ByLabelOutlier;
import elki.result.ResultUtil;
import elki.result.outlier.OutlierResult;
import elki.similarity.kernel.LinearKernel;
import elki.utilities.datastructures.range.IntGenerator;
import elki.utilities.documentation.Reference;
import elki.utilities.exceptions.AbortException;
import elki.utilities.io.FormatUtil;
import elki.utilities.optionhandling.OptionID;
import elki.utilities.optionhandling.parameterization.Parameterization;
import elki.utilities.optionhandling.parameters.*;
import elki.utilities.scaling.IdentityScaling;
import elki.utilities.scaling.ScalingFunction;
import elki.utilities.scaling.outlier.OutlierScaling;
import elki.workflow.InputStep;

import net.jafama.FastMath;

/**
 * Application that runs a series of kNN-based algorithms on a data set, for
 * building an ensemble in a second step. The output file consists of a label
 * and one score value for each object.
 * <p>
 * Since some algorithms can be too slow to run on large data sets and for large
 * values of k, they can be disabled. For example
 * <tt>-disable '(LDOF|DWOF|COF|FastABOD)'</tt> disables these two methods
 * completely. Alternatively, you can use the parameter <tt>-ksquaremax</tt>
 * to control the maximum k for these four methods separately.
 * <p>
 * For methods where k=1 does not make sense, this value will be skipped, and
 * the procedure will commence at 1+stepsize.
 * <p>
 * Reference:
 * <p>
 * Erich Schubert, Remigius Wojdanowski, Arthur Zimek, Hans-Peter Kriegel<br>
 * On Evaluation of Outlier Rankings and Outlier Scores<br>
 * Proc. 12th SIAM Int. Conf. on Data Mining (SDM 2012)
 *
 * @author Erich Schubert
 * @since 0.5.0
 *
 * @param <O> Vector type
 */
@Reference(authors = "Erich Schubert, Remigius Wojdanowski, Arthur Zimek, Hans-Peter Kriegel", //
    title = "On Evaluation of Outlier Rankings and Outlier Scores", //
    booktitle = "Proc. 12th SIAM Int. Conf. on Data Mining (SDM 2012)", //
    url = "https://doi.org/10.1137/1.9781611972825.90", //
    bibkey = "DBLP:conf/sdm/SchubertWZK12")
public class ComputeKNNOutlierScores<O extends NumberVector> extends AbstractDistanceBasedApplication<O> {
  /**
   * Our logger class.
   */
  private static final Logging LOG = Logging.getLogger(ComputeKNNOutlierScores.class);

  /**
   * Range of k.
   */
  final IntGenerator krange;

  /**
   * Output file
   */
  Path outfile;

  /**
   * By label outlier detection - reference
   */
  ByLabelOutlier bylabel;

  /**
   * Scaling function.
   */
  ScalingFunction scaling;

  /**
   * Pattern for disabling (skipping) methods.
   */
  Pattern disable = null;

  /**
   * Maximum k for O(k^2) methods.
   */
  int ksquarestop = 1000;

  /**
   * Timelimit for computation (not strictly enforced). In ms.
   */
  long timelimit;

  /**
   * Constructor.
   *
   * @param inputstep Input step
   * @param distance Distance function
   * @param krange K parameter range
   * @param bylabel By label outlier (reference)
   * @param outfile Output file
   * @param scaling Scaling function
   * @param disable Pattern for disabling methods
   * @param ksquarestop Maximum k for O(k^2) methods
   * @param timelimit Time limit in seconds
   */
  public ComputeKNNOutlierScores(InputStep inputstep, Distance<? super O> distance, IntGenerator krange, ByLabelOutlier bylabel, Path outfile, ScalingFunction scaling, Pattern disable, int ksquarestop, long timelimit) {
    super(inputstep, distance);
    this.krange = krange;
    this.bylabel = bylabel;
    this.outfile = outfile;
    this.scaling = scaling;
    this.disable = disable;
    this.ksquarestop = ksquarestop;
    this.timelimit = timelimit * 1000;
  }

  @Override
  public void run() {
    final Database database = inputstep.getDatabase();
    final Relation<O> relation = database.getRelation(distance.getInputTypeRestriction());
    // Ensure we don't go beyond the relation size:
    final int maxk = Math.min(krange.getMax(), relation.size() - 1);

    // Get a KNN query.
    final int lim = Math.min(maxk + 2, relation.size());
    KNNSearcher<DBIDRef> knnq = new QueryBuilder<>(relation, distance).precomputed().kNNByDBID(lim);
    if(!(knnq instanceof PreprocessorKNNQuery)) {
      throw new AbortException("Not using preprocessor knn query -- KNN queries using class: " + knnq.getClass());
    }

    // Warn for some known slow methods and large k:
    int maxksq = Math.min(maxk, ksquarestop);
    if(!isDisabled("FastABOD") && maxksq > 1000) {
      LOG.warning("Note: FastABOD needs quadratic memory. Use -" + Par.DISABLE_ID.getName() + " FastABOD to disable.");
    }
    if(!isDisabled("LDOF") && maxksq > 1000) {
      LOG.verbose("Note: LODF needs O(k^2) distance computations. Use -" + Par.DISABLE_ID.getName() + " LDOF to disable.");
    }
    if(!isDisabled("DWOF") && maxksq > 1000) {
      LOG.warning("Note: DWOF needs O(k^2) distance computations. Use -" + Par.DISABLE_ID.getName() + " DWOF to disable.");
    }
    if(!isDisabled("COF") && maxksq > 1000) {
      LOG.warning("Note: COF needs O(k^2) distance computations. Use -" + Par.DISABLE_ID.getName() + " COF to disable.");
    }

    final DBIDs ids = relation.getDBIDs();

    try (BufferedWriter fout = Files.newBufferedWriter(outfile)) {
      // Control: print the DBIDs in case we are seeing an odd iteration
      fout.append("# Data set size: " + relation.size()) //
          .append(" data type: " + relation.getDataTypeInformation()).append(FormatUtil.NEWLINE);

      // Label outlier result (reference)
      writeResult(fout, ids, bylabel.autorun(database), new IdentityScaling(), "bylabel");

      // Output function:
      BiConsumer<String, OutlierResult> out = (kstr, result) -> writeResult(fout, ids, result, scaling, kstr);

      // KNN
      runForEachK("KNN", 0, maxk, //
          k -> new KNNOutlier<O>(distance, k) //
              .run(relation), out);
      // KNN Weight
      runForEachK("KNNW", 0, maxk, //
          k -> new KNNWeightOutlier<O>(distance, k) //
              .run(relation), out);
      // Run LOF
      runForEachK("LOF", 0, maxk, //
          k -> new LOF<O>(k, distance) //
              .run(relation), out);
      // Run Simplified-LOF
      runForEachK("SimplifiedLOF", 0, maxk, //
          k -> new SimplifiedLOF<O>(distance, k) //
              .run(relation), out);
      // LoOP
      runForEachK("LoOP", 0, maxk, //
          k -> new LoOP<O>(k, k, distance, distance, 1.0) //
              .run(relation), out);
      // LDOF
      runForEachK("LDOF", 2, maxksq, //
          k -> new LDOF<O>(distance, k) //
              .run(relation), out);
      // Run ODIN
      runForEachK("ODIN", 0, maxk, //
          k -> new ODIN<O>(distance, k) //
              .run(relation), out);
      // Run KDEOS with intrinsic dimensionality 2.
      runForEachK("KDEOS", 2, maxk, //
          k -> new KDEOS<O>(distance, k, k, GaussianKernelDensityFunction.KERNEL, 0., //
              .5 * GaussianKernelDensityFunction.KERNEL.canonicalBandwidth(), 2)//
                  .run(relation), out);
      // Run LDF
      runForEachK("LDF", 0, maxk, //
          k -> new LDF<O>(k, distance, GaussianKernelDensityFunction.KERNEL, 1., .1) //
              .run(relation), out);
      // Run INFLO
      runForEachK("INFLO", 0, maxk, //
          k -> new INFLO<O>(distance, 1.0, k) //
              .run(relation), out);
      // Run COF
      runForEachK("COF", 0, maxksq, //
          k -> new COF<O>(distance, k) //
              .run(relation), out);
      // Run simple Intrinsic dimensionality
      runForEachK("LID", 2, maxk, //
          k -> new LID<O>(distance, k, AggregatedHillEstimator.STATIC) //
              .run(relation), out);
      // Run IDOS
      runForEachK("IDOS", 2, maxk, //
          k -> new IDOS<O>(distance, AggregatedHillEstimator.STATIC, k, k) //
              .run(relation), out);
      // Run simple kernel-density LOF variant
      runForEachK("KDLOF", 2, maxk, //
          k -> new SimpleKernelDensityLOF<O>(k, distance, GaussianKernelDensityFunction.KERNEL) //
              .run(relation), out);
      // Run DWOF (need pairwise distances, too)
      runForEachK("DWOF", 2, maxksq, //
          k -> new DWOF<O>(distance, k, 1.1) //
              .run(relation), out);
      // Run LIC
      runForEachK("LIC", 0, maxk, //
          k -> new LocalIsolationCoefficient<O>(distance, k) //
              .run(relation), out);
      // Run VOV (requires a vector field).
      if(TypeUtil.DOUBLE_VECTOR_FIELD.isAssignableFromType(relation.getDataTypeInformation())) {
        @SuppressWarnings("unchecked")
        final Distance<? super DoubleVector> df = (Distance<? super DoubleVector>) distance;
        @SuppressWarnings("unchecked")
        final Relation<DoubleVector> rel = (Relation<DoubleVector>) (Relation<?>) relation;
        runForEachK("VOV", 0, maxk, //
            k -> new VarianceOfVolume<DoubleVector>(k, df) //
                .run(rel), out);
      }
      // Run KNN DD
      runForEachK("KNNDD", 0, maxk, //
          k -> new KNNDD<O>(distance, k) //
              .run(relation), out);
      // Run KNN SOS
      runForEachK("KNNSOS", 0, maxk, //
          k -> new KNNSOS<O>(distance, k) //
              .run(relation), out);
      // Run ISOS
      runForEachK("ISOS", 2, maxk, //
          k -> new ISOS<O>(distance, k, AggregatedHillEstimator.STATIC) //
              .run(relation), out);
      // Run FastABOD
      if(EuclideanDistance.STATIC.equals(distance) || SquaredEuclideanDistance.STATIC.equals(distance)) {
        runForEachK("FastABOD", 3, maxksq, //
            k -> new FastABOD<O>(LinearKernel.STATIC, k) //
                .run(relation), out);
      }
    }
    catch(IOException e) {
      throw new AbortException("IO error writing output file.", e);
    }
    // Prevent garbage collection
    if(!(knnq instanceof PreprocessorKNNQuery)) {
      LOG.warning("Not using preprocessor knn query. Runtime is suboptimal.");
    }
  }

  /**
   * Write a single output line.
   *
   * @param out Output stream
   * @param ids DBIDs
   * @param result Outlier result
   * @param scaling Scaling function
   * @param label Identification label
   */
  void writeResult(Appendable out, DBIDs ids, OutlierResult result, ScalingFunction scaling, String label) {
    try {
      if(scaling instanceof OutlierScaling) {
        ((OutlierScaling) scaling).prepare(result);
      }
      out.append(label);
      DoubleRelation scores = result.getScores();
      for(DBIDIter iter = ids.iter(); iter.valid(); iter.advance()) {
        double value = scores.doubleValue(iter);
        value = scaling != null ? scaling.getScaled(value) : value;
        out.append(' ').append(Double.toString(value));
      }
      out.append(FormatUtil.NEWLINE);
    }
    catch(IOException e) {
      // Unfortunately we need to rewrap this in an unchecked exception to use
      // this in a lambda.
      throw new AbortException("IO Error writing to file", e);
    }
  }

  /**
   * Iterate over the k range.
   *
   * @param prefix Prefix string
   * @param mink Minimum value of k for this method
   * @param maxk Maximum value of k for this method
   * @param runner Runner to run
   * @param out Output function
   */
  private void runForEachK(String prefix, int mink, int maxk, IntFunction<OutlierResult> runner, BiConsumer<String, OutlierResult> out) {
    if(isDisabled(prefix)) {
      LOG.verbose("Skipping (disabled): " + prefix);
      return; // Disabled
    }
    LOG.verbose("Running " + prefix);
    final int digits = (int) FastMath.ceil(FastMath.log10(krange.getMax() + 1));
    final String format = "%s-%0" + digits + "d";
    try {
      krange.forEach(k -> {
        if(k >= mink && k <= maxk) {
          Duration time = LOG.newDuration(this.getClass().getCanonicalName() + "." + prefix + ".k" + k + ".runtime").begin();
          OutlierResult result = runner.apply(k);
          LOG.statistics(time.end());
          if(result != null) {
            out.accept(String.format(Locale.ROOT, format, prefix, k), result);
            ResultUtil.removeRecursive(result);
          }
          if(timelimit > 0 && time.getDuration() > timelimit) {
            throw new TimeoutException("Timeout in " + prefix + " at k=" + k + ": " + time.getDuration());
          }
        }
      });
    }
    catch(TimeoutException e) {
      LOG.error(e.getMessage()); // The stack trace is not helpful.
    }
  }

  /**
   * Exception used in timeout logic.
   *
   * @author Erich Schubert
   */
  private static class TimeoutException extends RuntimeException {
    /**
     * Serialization version
     */
    private static final long serialVersionUID = 1L;

    /**
     * Constructor.
     *
     * @param msg Message
     */
    public TimeoutException(String msg) {
      super(msg);
    }

  }

  /**
   * Test if a given algorithm is disabled.
   *
   * @param name Algorithm name
   * @return {@code true} if disabled
   */
  protected boolean isDisabled(String name) {
    return disable != null && disable.matcher(name).matches();
  }

  /**
   * Parameterization class.
   *
   * @author Erich Schubert
   */
  public static class Par<O extends NumberVector> extends AbstractDistanceBasedApplication.Par<O> {
    /**
     * Option ID for k parameter range
     */
    public static final OptionID KRANGE_ID = new OptionID("krange", "Range of k. This accepts multiple ranges, such as 1,2,..,10,20,..,100");

    /**
     * Option ID for scaling class.
     */
    public static final OptionID SCALING_ID = new OptionID("scaling", "Scaling function.");

    /**
     * Option ID for disabling methods.
     */
    public static final OptionID DISABLE_ID = new OptionID("disable", "Disable methods (regular expression, case insensitive, anchored).");

    /**
     * Option ID with an additional bound on k.
     */
    public static final OptionID KSQUARE_ID = new OptionID("ksquaremax", "Maximum k for methods with O(k^2) cost.");

    /**
     * Option ID to limit the maximum time for an iteration
     */
    public static final OptionID TIMELIMIT_ID = new OptionID("timelimit", "Maximum run time per iteration in seconds (NOT strictly enforced).");

    /**
     * k step size
     */
    IntGenerator krange;

    /**
     * By label outlier -- reference
     */
    ByLabelOutlier bylabel;

    /**
     * Scaling function.
     */
    ScalingFunction scaling = null;

    /**
     * Output destination file
     */
    Path outfile;

    /**
     * Pattern for disabling (skipping) methods.
     */
    Pattern disable = null;

    /**
     * Maximum k for O(k^2) methods.
     */
    int ksquarestop = 100;

    /**
     * Timelimit
     */
    long timelimit = -1;

    @Override
    public void configure(Parameterization config) {
      super.configure(config);
      new IntGeneratorParameter(KRANGE_ID) //
          .grab(config, x -> krange = x);
      bylabel = config.tryInstantiate(ByLabelOutlier.class);
      // Output
      outfile = super.getParameterOutputFile(config, "File to output the resulting score vectors to.");
      new ObjectParameter<ScalingFunction>(SCALING_ID, ScalingFunction.class) //
          .setOptional(true) //
          .grab(config, x -> scaling = x);
      new PatternParameter(DISABLE_ID) //
          .setOptional(true) //
          .grab(config, x -> disable = x);
      new IntParameter(KSQUARE_ID, 100) //
          .grab(config, x -> ksquarestop = x);
      new LongParameter(TIMELIMIT_ID, 12 * 60 * 60) //
          .grab(config, x -> timelimit = x);
    }

    @Override
    public ComputeKNNOutlierScores<O> make() {
      return new ComputeKNNOutlierScores<>(inputstep, distance, krange, bylabel, outfile, scaling, disable, ksquarestop, timelimit);
    }
  }

  /**
   * Main method.
   *
   * @param args Command line parameters.
   */
  public static void main(String[] args) {
    runCLIApplication(ComputeKNNOutlierScores.class, args);
  }
}
