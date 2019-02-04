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

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintStream;
import java.util.Locale;
import java.util.function.BiConsumer;
import java.util.function.IntFunction;
import java.util.regex.Pattern;

import de.lmu.ifi.dbs.elki.algorithm.DistanceBasedAlgorithm;
import de.lmu.ifi.dbs.elki.algorithm.outlier.DWOF;
import de.lmu.ifi.dbs.elki.algorithm.outlier.anglebased.FastABOD;
import de.lmu.ifi.dbs.elki.algorithm.outlier.distance.*;
import de.lmu.ifi.dbs.elki.algorithm.outlier.intrinsic.IDOS;
import de.lmu.ifi.dbs.elki.algorithm.outlier.intrinsic.ISOS;
import de.lmu.ifi.dbs.elki.algorithm.outlier.intrinsic.IntrinsicDimensionalityOutlier;
import de.lmu.ifi.dbs.elki.algorithm.outlier.lof.*;
import de.lmu.ifi.dbs.elki.algorithm.outlier.trivial.ByLabelOutlier;
import de.lmu.ifi.dbs.elki.application.AbstractApplication;
import de.lmu.ifi.dbs.elki.data.DoubleVector;
import de.lmu.ifi.dbs.elki.data.NumberVector;
import de.lmu.ifi.dbs.elki.data.type.TypeUtil;
import de.lmu.ifi.dbs.elki.database.Database;
import de.lmu.ifi.dbs.elki.database.QueryUtil;
import de.lmu.ifi.dbs.elki.database.ids.DBIDIter;
import de.lmu.ifi.dbs.elki.database.ids.DBIDs;
import de.lmu.ifi.dbs.elki.database.query.knn.KNNQuery;
import de.lmu.ifi.dbs.elki.database.query.knn.PreprocessorKNNQuery;
import de.lmu.ifi.dbs.elki.database.relation.DoubleRelation;
import de.lmu.ifi.dbs.elki.database.relation.Relation;
import de.lmu.ifi.dbs.elki.distance.distancefunction.DistanceFunction;
import de.lmu.ifi.dbs.elki.distance.distancefunction.minkowski.EuclideanDistanceFunction;
import de.lmu.ifi.dbs.elki.distance.similarityfunction.kernel.LinearKernelFunction;
import de.lmu.ifi.dbs.elki.index.preprocessed.knn.MaterializeKNNPreprocessor;
import de.lmu.ifi.dbs.elki.logging.Logging;
import de.lmu.ifi.dbs.elki.logging.statistics.Duration;
import de.lmu.ifi.dbs.elki.math.statistics.intrinsicdimensionality.AggregatedHillEstimator;
import de.lmu.ifi.dbs.elki.math.statistics.kernelfunctions.GaussianKernelDensityFunction;
import de.lmu.ifi.dbs.elki.result.outlier.OutlierResult;
import de.lmu.ifi.dbs.elki.utilities.datastructures.range.IntGenerator;
import de.lmu.ifi.dbs.elki.utilities.documentation.Reference;
import de.lmu.ifi.dbs.elki.utilities.exceptions.AbortException;
import de.lmu.ifi.dbs.elki.utilities.io.FormatUtil;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.OptionID;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.Parameterization;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.IntGeneratorParameter;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.IntParameter;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.ObjectParameter;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.PatternParameter;
import de.lmu.ifi.dbs.elki.utilities.scaling.IdentityScaling;
import de.lmu.ifi.dbs.elki.utilities.scaling.ScalingFunction;
import de.lmu.ifi.dbs.elki.utilities.scaling.outlier.OutlierScaling;
import de.lmu.ifi.dbs.elki.workflow.InputStep;

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
public class ComputeKNNOutlierScores<O extends NumberVector> extends AbstractApplication {
  /**
   * Our logger class.
   */
  private static final Logging LOG = Logging.getLogger(ComputeKNNOutlierScores.class);

  /**
   * Input step
   */
  final InputStep inputstep;

  /**
   * Distance function to use
   */
  final DistanceFunction<? super O> distf;

  /**
   * Range of k.
   */
  final IntGenerator krange;

  /**
   * Output file
   */
  File outfile;

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
   * Constructor.
   *
   * @param inputstep Input step
   * @param distf Distance function
   * @param krange K parameter range
   * @param bylabel By label outlier (reference)
   * @param outfile Output file
   * @param scaling Scaling function
   * @param disable Pattern for disabling methods
   * @param ksquarestop Maximum k for O(k^2) methods
   */
  public ComputeKNNOutlierScores(InputStep inputstep, DistanceFunction<? super O> distf, IntGenerator krange, ByLabelOutlier bylabel, File outfile, ScalingFunction scaling, Pattern disable, int ksquarestop) {
    super();
    this.distf = distf;
    this.krange = krange;
    this.inputstep = inputstep;
    this.bylabel = bylabel;
    this.outfile = outfile;
    this.scaling = scaling;
    this.disable = disable;
    this.ksquarestop = ksquarestop;
  }

  @Override
  public void run() {
    final Database database = inputstep.getDatabase();
    final Relation<O> relation = database.getRelation(distf.getInputTypeRestriction());
    // Ensure we don't go beyond the relation size:
    final int maxk = Math.min(krange.getMax(), relation.size() - 1);

    // Get a KNN query.
    final int lim = Math.min(maxk + 2, relation.size());
    KNNQuery<O> knnq = QueryUtil.getKNNQuery(relation, distf, lim);

    // Precompute kNN:
    if(!(knnq instanceof PreprocessorKNNQuery)) {
      MaterializeKNNPreprocessor<O> preproc = new MaterializeKNNPreprocessor<>(relation, distf, lim);
      preproc.initialize();
      relation.getHierarchy().add(relation, preproc);
    }

    // Test that we now get a proper index query
    knnq = QueryUtil.getKNNQuery(relation, distf, lim);
    if(!(knnq instanceof PreprocessorKNNQuery)) {
      throw new AbortException("Not using preprocessor knn query -- KNN queries using class: " + knnq.getClass());
    }

    // Warn for some known slow methods and large k:
    int maxksq = Math.min(maxk, ksquarestop);
    if(!isDisabled("FastABOD") && maxksq > 1000) {
      LOG.warning("Note: FastABOD needs quadratic memory. Use -" + Parameterizer.DISABLE_ID.getName() + " FastABOD to disable.");
    }
    if(!isDisabled("LDOF") && maxksq > 1000) {
      LOG.verbose("Note: LODF needs O(k^2) distance computations. Use -" + Parameterizer.DISABLE_ID.getName() + " LDOF to disable.");
    }
    if(!isDisabled("DWOF") && maxksq > 1000) {
      LOG.warning("Note: DWOF needs O(k^2) distance computations. Use -" + Parameterizer.DISABLE_ID.getName() + " DWOF to disable.");
    }
    if(!isDisabled("COF") && maxksq > 1000) {
      LOG.warning("Note: COF needs O(k^2) distance computations. Use -" + Parameterizer.DISABLE_ID.getName() + " COF to disable.");
    }

    final DBIDs ids = relation.getDBIDs();

    try (PrintStream fout = new PrintStream(outfile)) {
      // Control: print the DBIDs in case we are seeing an odd iteration
      fout.append("# Data set size: " + relation.size()) //
          .append(" data type: " + relation.getDataTypeInformation()).append(FormatUtil.NEWLINE);

      // Label outlier result (reference)
      writeResult(fout, ids, bylabel.run(database), new IdentityScaling(), "bylabel");

      // Output function:
      BiConsumer<String, OutlierResult> out = (kstr, result) -> writeResult(fout, ids, result, scaling, kstr);

      // KNN
      runForEachK("KNN", 0, maxk, //
          k -> new KNNOutlier<O>(distf, k) //
              .run(database, relation), out);
      // KNN Weight
      runForEachK("KNNW", 0, maxk, //
          k -> new KNNWeightOutlier<O>(distf, k) //
              .run(database, relation), out);
      // Run LOF
      runForEachK("LOF", 0, maxk, //
          k -> new LOF<O>(k, distf) //
              .run(database, relation), out);
      // Run Simplified-LOF
      runForEachK("SimplifiedLOF", 0, maxk, //
          k -> new SimplifiedLOF<O>(k, distf) //
              .run(database, relation), out);
      // LoOP
      runForEachK("LoOP", 0, maxk, //
          k -> new LoOP<O>(k, k, distf, distf, 1.0) //
              .run(database, relation), out);
      // LDOF
      runForEachK("LDOF", 2, maxksq, //
          k -> new LDOF<O>(distf, k) //
              .run(database, relation), out);
      // Run ODIN
      runForEachK("ODIN", 0, maxk, //
          k -> new ODIN<O>(distf, k) //
              .run(database, relation), out);
      // Run FastABOD
      runForEachK("FastABOD", 3, maxksq, //
          k -> new FastABOD<O>(LinearKernelFunction.STATIC, k) //
              .run(database, relation), out);
      // Run KDEOS with intrinsic dimensionality 2.
      runForEachK("KDEOS", 2, maxk, //
          k -> new KDEOS<O>(distf, k, k, GaussianKernelDensityFunction.KERNEL, 0., //
              .5 * GaussianKernelDensityFunction.KERNEL.canonicalBandwidth(), 2)//
                  .run(database, relation), out);
      // Run LDF
      runForEachK("LDF", 0, maxk, //
          k -> new LDF<O>(k, distf, GaussianKernelDensityFunction.KERNEL, 1., .1) //
              .run(database, relation), out);
      // Run INFLO
      runForEachK("INFLO", 0, maxk, //
          k -> new INFLO<O>(distf, 1.0, k) //
              .run(database, relation), out);
      // Run COF
      runForEachK("COF", 0, maxksq, //
          k -> new COF<O>(k, distf) //
              .run(database, relation), out);
      // Run simple Intrinsic dimensionality
      runForEachK("Intrinsic", 2, maxk, //
          k -> new IntrinsicDimensionalityOutlier<O>(distf, k, AggregatedHillEstimator.STATIC) //
              .run(database, relation), out);
      // Run IDOS
      runForEachK("IDOS", 2, maxk, //
          k -> new IDOS<O>(distf, AggregatedHillEstimator.STATIC, k, k) //
              .run(database, relation), out);
      // Run simple kernel-density LOF variant
      runForEachK("KDLOF", 2, maxk, //
          k -> new SimpleKernelDensityLOF<O>(k, distf, GaussianKernelDensityFunction.KERNEL) //
              .run(database, relation), out);
      // Run DWOF (need pairwise distances, too)
      runForEachK("DWOF", 2, maxksq, //
          k -> new DWOF<O>(distf, k, 1.1) //
              .run(database, relation), out);
      // Run LIC
      runForEachK("LIC", 0, maxk, //
          k -> new LocalIsolationCoefficient<O>(distf, k) //
              .run(database, relation), out);
      // Run VOV (requires a vector field).
      if(TypeUtil.DOUBLE_VECTOR_FIELD.isAssignableFromType(relation.getDataTypeInformation())) {
        @SuppressWarnings("unchecked")
        final DistanceFunction<? super DoubleVector> df = (DistanceFunction<? super DoubleVector>) distf;
        @SuppressWarnings("unchecked")
        final Relation<DoubleVector> rel = (Relation<DoubleVector>) (Relation<?>) relation;
        runForEachK("VOV", 0, maxk, //
            k -> new VarianceOfVolume<DoubleVector>(k, df) //
                .run(database, rel), out);
      }
      // Run KNN DD
      runForEachK("KNNDD", 0, maxk, //
          k -> new KNNDD<O>(distf, k) //
              .run(database, relation), out);
      // Run KNN SOS
      runForEachK("KNNSOS", 0, maxk, //
          k -> new KNNSOS<O>(distf, k) //
              .run(relation), out);
      // Run ISOS
      runForEachK("ISOS", 2, maxk, //
          k -> new ISOS<O>(distf, k, AggregatedHillEstimator.STATIC) //
              .run(relation), out);
    }
    catch(FileNotFoundException e) {
      throw new AbortException("Cannot create output file.", e);
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
  void writeResult(PrintStream out, DBIDs ids, OutlierResult result, ScalingFunction scaling, String label) {
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
    krange.forEach(k -> {
      if(k >= mink && k <= maxk) {
        Duration time = LOG.newDuration(this.getClass().getCanonicalName() + "." + prefix + ".k" + k + ".runtime").begin();
        OutlierResult result = runner.apply(k);
        LOG.statistics(time.end());
        if(result != null) {
          out.accept(String.format(Locale.ROOT, format, prefix, k), result);
          result.getHierarchy().removeSubtree(result);
        }
      }
    });
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
  public static class Parameterizer<O extends NumberVector> extends AbstractApplication.Parameterizer {
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
     * k step size
     */
    IntGenerator krange;

    /**
     * Data source
     */
    InputStep inputstep;

    /**
     * Distance function to use
     */
    DistanceFunction<? super O> distf;

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
    File outfile;

    /**
     * Pattern for disabling (skipping) methods.
     */
    Pattern disable = null;

    /**
     * Maximum k for O(k^2) methods.
     */
    int ksquarestop = 100;

    @Override
    protected void makeOptions(Parameterization config) {
      super.makeOptions(config);
      // Data input
      inputstep = config.tryInstantiate(InputStep.class);
      // Distance function
      ObjectParameter<DistanceFunction<? super O>> distP = new ObjectParameter<>(DistanceBasedAlgorithm.DISTANCE_FUNCTION_ID, DistanceFunction.class, EuclideanDistanceFunction.class);
      if(config.grab(distP)) {
        distf = distP.instantiateClass(config);
      }
      IntGeneratorParameter kP = new IntGeneratorParameter(KRANGE_ID);
      if(config.grab(kP)) {
        krange = kP.getValue();
      }
      bylabel = config.tryInstantiate(ByLabelOutlier.class);
      // Output
      outfile = super.getParameterOutputFile(config, "File to output the resulting score vectors to.");

      ObjectParameter<ScalingFunction> scalingP = new ObjectParameter<>(SCALING_ID, ScalingFunction.class);
      scalingP.setOptional(true);
      if(config.grab(scalingP)) {
        scaling = scalingP.instantiateClass(config);
      }

      PatternParameter disableP = new PatternParameter(DISABLE_ID) //
          .setOptional(true);
      if(config.grab(disableP)) {
        disable = disableP.getValue();
      }
      IntParameter ksqP = new IntParameter(KSQUARE_ID, 100);
      if(config.grab(ksqP)) {
        ksquarestop = ksqP.intValue();
      }
    }

    @Override
    protected ComputeKNNOutlierScores<O> makeInstance() {
      return new ComputeKNNOutlierScores<>(inputstep, distf, krange, bylabel, outfile, scaling, disable, ksquarestop);
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
