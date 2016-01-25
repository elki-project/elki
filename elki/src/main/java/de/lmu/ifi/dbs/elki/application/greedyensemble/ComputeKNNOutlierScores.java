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
import java.io.FileNotFoundException;
import java.io.PrintStream;
import java.util.Locale;
import java.util.regex.Pattern;

import de.lmu.ifi.dbs.elki.algorithm.AbstractAlgorithm;
import de.lmu.ifi.dbs.elki.algorithm.outlier.DWOF;
import de.lmu.ifi.dbs.elki.algorithm.outlier.anglebased.FastABOD;
import de.lmu.ifi.dbs.elki.algorithm.outlier.distance.KNNOutlier;
import de.lmu.ifi.dbs.elki.algorithm.outlier.distance.KNNWeightOutlier;
import de.lmu.ifi.dbs.elki.algorithm.outlier.distance.LocalIsolationCoefficient;
import de.lmu.ifi.dbs.elki.algorithm.outlier.distance.ODIN;
import de.lmu.ifi.dbs.elki.algorithm.outlier.intrinsic.IDOS;
import de.lmu.ifi.dbs.elki.algorithm.outlier.intrinsic.IntrinsicDimensionalityOutlier;
import de.lmu.ifi.dbs.elki.algorithm.outlier.lof.COF;
import de.lmu.ifi.dbs.elki.algorithm.outlier.lof.INFLO;
import de.lmu.ifi.dbs.elki.algorithm.outlier.lof.KDEOS;
import de.lmu.ifi.dbs.elki.algorithm.outlier.lof.LDF;
import de.lmu.ifi.dbs.elki.algorithm.outlier.lof.LDOF;
import de.lmu.ifi.dbs.elki.algorithm.outlier.lof.LOF;
import de.lmu.ifi.dbs.elki.algorithm.outlier.lof.LoOP;
import de.lmu.ifi.dbs.elki.algorithm.outlier.lof.SimpleKernelDensityLOF;
import de.lmu.ifi.dbs.elki.algorithm.outlier.lof.SimplifiedLOF;
import de.lmu.ifi.dbs.elki.algorithm.outlier.lof.VarianceOfVolume;
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
import de.lmu.ifi.dbs.elki.distance.similarityfunction.kernel.PolynomialKernelFunction;
import de.lmu.ifi.dbs.elki.index.preprocessed.knn.MaterializeKNNPreprocessor;
import de.lmu.ifi.dbs.elki.logging.Logging;
import de.lmu.ifi.dbs.elki.logging.statistics.Duration;
import de.lmu.ifi.dbs.elki.math.statistics.intrinsicdimensionality.HillEstimator;
import de.lmu.ifi.dbs.elki.math.statistics.kernelfunctions.GaussianKernelDensityFunction;
import de.lmu.ifi.dbs.elki.result.outlier.OutlierResult;
import de.lmu.ifi.dbs.elki.utilities.FormatUtil;
import de.lmu.ifi.dbs.elki.utilities.documentation.Reference;
import de.lmu.ifi.dbs.elki.utilities.exceptions.AbortException;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.OptionID;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.constraints.CommonConstraints;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.Parameterization;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.IntParameter;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.ObjectParameter;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.PatternParameter;
import de.lmu.ifi.dbs.elki.utilities.scaling.IdentityScaling;
import de.lmu.ifi.dbs.elki.utilities.scaling.ScalingFunction;
import de.lmu.ifi.dbs.elki.utilities.scaling.outlier.OutlierScalingFunction;
import de.lmu.ifi.dbs.elki.workflow.InputStep;

/**
 * Application that runs a series of kNN-based algorithms on a data set, for
 * building an ensemble in a second step. The output file consists of a label
 * and one score value for each object.
 *
 * Since some algorithms can be too slow to run on large data sets and for large
 * values of k, they can be disabled. For example
 * <tt>-disable '(LDOF|FastABOD)'</tt> disables these two methods.
 *
 * For methods where k=1 does not make sense, this value will be skipped, and
 * the procedure will commence at 1+stepsize.
 *
 * Reference:
 * <p>
 * E. Schubert, R. Wojdanowski, A. Zimek, H.-P. Kriegel<br />
 * On Evaluation of Outlier Rankings and Outlier Scores<br/>
 * In Proceedings of the 12th SIAM International Conference on Data Mining
 * (SDM), Anaheim, CA, 2012.
 * </p>
 *
 * @author Erich Schubert
 * @since 0.5.0
 *
 * @param <O> Vector type
 */
@Reference(authors = "E. Schubert, R. Wojdanowski, A. Zimek, H.-P. Kriegel", //
title = "On Evaluation of Outlier Rankings and Outlier Scores", //
booktitle = "Proc. 12th SIAM International Conference on Data Mining (SDM), Anaheim, CA, 2012.")
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
   * Starting value of k.
   */
  final int startk;

  /**
   * k step size
   */
  final int stepk;

  /**
   * Maximum value of k
   */
  final int maxk;

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
   * Constructor.
   *
   * @param inputstep Input step
   * @param distf Distance function
   * @param startk Starting value of k
   * @param stepk K step size
   * @param maxk Maximum k value
   * @param bylabel By label outlier (reference)
   * @param outfile Output file
   * @param scaling Scaling function
   * @param disable Pattern for disabling methods
   */
  public ComputeKNNOutlierScores(InputStep inputstep, DistanceFunction<? super O> distf, int startk, int stepk, int maxk, ByLabelOutlier bylabel, File outfile, ScalingFunction scaling, Pattern disable) {
    super();
    this.distf = distf;
    this.startk = startk;
    this.stepk = stepk;
    this.maxk = maxk;
    this.inputstep = inputstep;
    this.bylabel = bylabel;
    this.outfile = outfile;
    this.scaling = scaling;
    this.disable = disable;
  }

  @Override
  public void run() {
    final Database database = inputstep.getDatabase();
    final Relation<O> relation = database.getRelation(distf.getInputTypeRestriction());
    // Ensure we don't go beyond the relation size:
    final int maxk = Math.min(this.maxk, relation.size() - 1);

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

    final DBIDs ids = relation.getDBIDs();

    final PrintStream fout;
    try {
      fout = new PrintStream(outfile);
    }
    catch(FileNotFoundException e) {
      throw new AbortException("Cannot create output file.", e);
    }
    // Control: print the DBIDs in case we are seeing an odd iteration
    fout.append("# Data set size: " + relation.size());
    fout.append(" data type: " + relation.getDataTypeInformation());
    fout.append(FormatUtil.NEWLINE);

    // Label outlier result (reference)
    {
      OutlierResult bylabelresult = bylabel.run(database);
      writeResult(fout, ids, bylabelresult, new IdentityScaling(), "bylabel");
    }

    final int startk = (this.startk > 0) ? this.startk : this.stepk;
    final int startkmin2 = (startk >= 2) ? startk : (startk + stepk);
    final int startkmin3 = (startk >= 3) ? startk : (startkmin2 >= 3) ? startkmin2 : (startkmin2 + stepk);

    // KNN
    runForEachK("KNN", startk, stepk, maxk, new AlgRunner() {
      @Override
      public void run(int k, String kstr) {
        KNNOutlier<O> knn = new KNNOutlier<>(distf, k);
        OutlierResult result = knn.run(database, relation);
        writeResult(fout, ids, result, scaling, kstr);
        database.getHierarchy().removeSubtree(result);
      }
    });
    // KNN Weight
    runForEachK("KNNW", startk, stepk, maxk, new AlgRunner() {
      @Override
      public void run(int k, String kstr) {
        KNNWeightOutlier<O> knnw = new KNNWeightOutlier<>(distf, k);
        OutlierResult result = knnw.run(database, relation);
        writeResult(fout, ids, result, scaling, kstr);
        database.getHierarchy().removeSubtree(result);
      }
    });
    // Run LOF
    runForEachK("LOF", startk, stepk, maxk, new AlgRunner() {
      @Override
      public void run(int k, String kstr) {
        LOF<O> lof = new LOF<>(k, distf);
        OutlierResult result = lof.run(database, relation);
        writeResult(fout, ids, result, scaling, kstr);
        database.getHierarchy().removeSubtree(result);
      }
    });
    // Run Simplified-LOF
    runForEachK("SimplifiedLOF", startk, stepk, maxk, new AlgRunner() {
      @Override
      public void run(int k, String kstr) {
        SimplifiedLOF<O> lof = new SimplifiedLOF<>(k, distf);
        OutlierResult result = lof.run(database, relation);
        writeResult(fout, ids, result, scaling, kstr);
        database.getHierarchy().removeSubtree(result);
      }
    });
    // LoOP
    runForEachK("LoOP", startk, stepk, maxk, new AlgRunner() {
      @Override
      public void run(int k, String kstr) {
        LoOP<O> loop = new LoOP<>(k, k, distf, distf, 1.0);
        OutlierResult result = loop.run(database, relation);
        writeResult(fout, ids, result, scaling, kstr);
        database.getHierarchy().removeSubtree(result);
      }
    });
    // LDOF
    runForEachK("LDOF", startkmin2, stepk, maxk, new AlgRunner() {
      @Override
      public void run(int k, String kstr) {
        if(k == startkmin2 && maxk > 100) {
          LOG.verbose("Note: LODF needs O(k^2) distance computations. Use -" + Parameterizer.DISABLE_ID.getName() + " LDOF to disable.");
        }
        LDOF<O> ldof = new LDOF<>(distf, k);
        OutlierResult result = ldof.run(database, relation);
        writeResult(fout, ids, result, scaling, kstr);
        database.getHierarchy().removeSubtree(result);
      }
    });
    // Run ODIN
    runForEachK("ODIN", startk, stepk, maxk, new AlgRunner() {
      @Override
      public void run(int k, String kstr) {
        ODIN<O> odin = new ODIN<>(distf, k);
        OutlierResult result = odin.run(database, relation);
        writeResult(fout, ids, result, scaling, kstr);
        database.getHierarchy().removeSubtree(result);
      }
    });
    // Run FastABOD
    runForEachK("FastABOD", startkmin3, stepk, maxk, new AlgRunner() {
      @Override
      public void run(int k, String kstr) {
        if(k == startkmin3 && maxk > 100) {
          LOG.verbose("Note: FastABOD needs quadratic memory. Use -" + Parameterizer.DISABLE_ID.getName() + " FastABOD to disable.");
        }
        FastABOD<O> fabod = new FastABOD<>(new PolynomialKernelFunction(2), k);
        OutlierResult result = fabod.run(database, relation);
        writeResult(fout, ids, result, scaling, kstr);
        database.getHierarchy().removeSubtree(result);
      }
    });
    // Run KDEOS with intrinsic dimensionality 2.
    runForEachK("KDEOS", startkmin2, stepk, maxk, new AlgRunner() {
      @Override
      public void run(int k, String kstr) {
        KDEOS<O> kdeos = new KDEOS<>(distf, k, k, //
        GaussianKernelDensityFunction.KERNEL, 0., //
        .5 * GaussianKernelDensityFunction.KERNEL.canonicalBandwidth(), 2);
        OutlierResult result = kdeos.run(database, relation);
        writeResult(fout, ids, result, scaling, kstr);
        database.getHierarchy().removeSubtree(result);
      }
    });
    // Run LDF
    runForEachK("LDF", startk, stepk, maxk, new AlgRunner() {
      @Override
      public void run(int k, String kstr) {
        LDF<O> ldf = new LDF<>(k, distf, GaussianKernelDensityFunction.KERNEL, 1., .1);
        OutlierResult result = ldf.run(database, relation);
        writeResult(fout, ids, result, scaling, kstr);
        database.getHierarchy().removeSubtree(result);
      }
    });
    // Run INFLO
    runForEachK("INFLO", startk, stepk, maxk, new AlgRunner() {
      @Override
      public void run(int k, String kstr) {
        INFLO<O> inflo = new INFLO<>(distf, 1.0, k);
        OutlierResult result = inflo.run(database, relation);
        writeResult(fout, ids, result, scaling, kstr);
        database.getHierarchy().removeSubtree(result);
      }
    });
    // Run COF
    runForEachK("COF", startk, stepk, maxk, new AlgRunner() {
      @Override
      public void run(int k, String kstr) {
        COF<O> cof = new COF<>(k, distf);
        OutlierResult result = cof.run(database, relation);
        writeResult(fout, ids, result, scaling, kstr);
        database.getHierarchy().removeSubtree(result);
      }
    });
    // Run simple Intrinsic dimensionality
    runForEachK("Intrinsic", startkmin2, stepk, maxk, new AlgRunner() {
      @Override
      public void run(int k, String kstr) {
        IntrinsicDimensionalityOutlier<O> sid = new IntrinsicDimensionalityOutlier<>(distf, k, HillEstimator.STATIC);
        OutlierResult result = sid.run(database, relation);
        writeResult(fout, ids, result, scaling, kstr);
        database.getHierarchy().removeSubtree(result);
      }
    });
    // Run IDOS
    runForEachK("IDOS", startkmin2, stepk, maxk, new AlgRunner() {
      @Override
      public void run(int k, String kstr) {
        IDOS<O> idos = new IDOS<>(distf, HillEstimator.STATIC, k, k);
        OutlierResult result = idos.run(database, relation);
        writeResult(fout, ids, result, scaling, kstr);
        database.getHierarchy().removeSubtree(result);
      }
    });
    // Run simple kernel-density LOF variant
    runForEachK("KDLOF", startkmin2, stepk, maxk, new AlgRunner() {
      @Override
      public void run(int k, String kstr) {
        SimpleKernelDensityLOF<O> kdlof = new SimpleKernelDensityLOF<>(k, distf, //
        GaussianKernelDensityFunction.KERNEL);
        OutlierResult result = kdlof.run(database, relation);
        writeResult(fout, ids, result, scaling, kstr);
        database.getHierarchy().removeSubtree(result);
      }
    });
    // Run DWOF (need pairwise distances, too)
    runForEachK("DWOF", startkmin2, stepk, maxk, new AlgRunner() {
      @Override
      public void run(int k, String kstr) {
        if(k == startkmin2 && maxk > 100) {
          LOG.verbose("Note: DWOF needs O(k^2) distance computations. Use -" + Parameterizer.DISABLE_ID.getName() + " DWOF to disable.");
        }
        DWOF<O> dwof = new DWOF<>(distf, k, 1.1);
        OutlierResult result = dwof.run(database, relation);
        writeResult(fout, ids, result, scaling, kstr);
        database.getHierarchy().removeSubtree(result);
      }
    });
    // Run LIC
    runForEachK("LIC", startk, stepk, maxk, new AlgRunner() {
      @Override
      public void run(int k, String kstr) {
        LocalIsolationCoefficient<O> lic = new LocalIsolationCoefficient<>(distf, k);
        OutlierResult result = lic.run(database, relation);
        writeResult(fout, ids, result, scaling, kstr);
        database.getHierarchy().removeSubtree(result);
      }
    });
    // Run VOV (requires a vector field).
    if(TypeUtil.DOUBLE_VECTOR_FIELD.isAssignableFromType(relation.getDataTypeInformation())) {
      @SuppressWarnings("unchecked")
      final DistanceFunction<? super DoubleVector> df = (DistanceFunction<? super DoubleVector>) distf;
      @SuppressWarnings("unchecked")
      final Relation<DoubleVector> rel = (Relation<DoubleVector>) (Relation<?>) relation;
      runForEachK("VOV", startk, stepk, maxk, new AlgRunner() {
        @Override
        public void run(int k, String kstr) {
          VarianceOfVolume<DoubleVector> vov = new VarianceOfVolume<>(k, df);
          OutlierResult result = vov.run(database, rel);
          writeResult(fout, ids, result, scaling, kstr);
          database.getHierarchy().removeSubtree(result);
        }
      });
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
    if(scaling instanceof OutlierScalingFunction) {
      ((OutlierScalingFunction) scaling).prepare(result);
    }
    out.append(label);
    DoubleRelation scores = result.getScores();
    for(DBIDIter iter = ids.iter(); iter.valid(); iter.advance()) {
      double value = scores.doubleValue(iter);
      if(scaling != null) {
        value = scaling.getScaled(value);
      }
      out.append(' ').append(Double.toString(value));
    }
    out.append(FormatUtil.NEWLINE);
  }

  /**
   * Iterate over the k range.
   *
   * @param prefix Prefix string
   * @param startk Start k
   * @param stepk Step k
   * @param maxk Max k
   * @param runner Runner to run
   */
  private void runForEachK(String prefix, int startk, int stepk, int maxk, AlgRunner runner) {
    if(isDisabled(prefix)) {
      LOG.verbose("Skipping (disabled): " + prefix);
      return; // Disabled
    }
    LOG.verbose("Running " + prefix);
    final int digits = (int) Math.ceil(Math.log10(maxk + 1));
    final String format = "%s-%0" + digits + "d";
    for(int k = startk; k <= maxk; k += stepk) {
      Duration time = LOG.newDuration(this.getClass().getCanonicalName() + "." + prefix + ".k" + k + ".runtime").begin();
      runner.run(k, String.format(Locale.ROOT, format, prefix, k));
      LOG.statistics(time.end());
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
   * Run an algorithm for a given k.
   *
   * @author Erich Schubert
   *
   * @apiviz.exclude
   */
  private interface AlgRunner {
    /**
     * Run a single algorithm instance.
     *
     * @param k K parameter
     * @param kstr String identifier.
     */
    public void run(int k, String kstr);
  }

  /**
   * Parameterization class.
   *
   * @author Erich Schubert
   *
   * @apiviz.exclude
   */
  public static class Parameterizer<O extends NumberVector> extends AbstractApplication.Parameterizer {
    /**
     * Option ID for k step size.
     */
    public static final OptionID STEPK_ID = new OptionID("stepk", "Step size for k.");

    /**
     * Option ID for k start size.
     */
    public static final OptionID STARTK_ID = new OptionID("startk", "Minimum value for k.");

    /**
     * Option ID for k step size.
     */
    public static final OptionID MAXK_ID = new OptionID("maxk", "Maximum value for k.");

    /**
     * Option ID for scaling class.
     */
    public static final OptionID SCALING_ID = new OptionID("scaling", "Scaling function.");

    /**
     * Option ID for disabling methods.
     */
    public static final OptionID DISABLE_ID = new OptionID("disable", "Disable methods (regular expression, case insensitive, anchored).");

    /**
     * k step size
     */
    int stepk;

    /**
     * starting value of k
     */
    int startk;

    /**
     * Maximum value of k
     */
    int maxk;

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

    @Override
    protected void makeOptions(Parameterization config) {
      super.makeOptions(config);
      // Data input
      inputstep = config.tryInstantiate(InputStep.class);
      // Distance function
      ObjectParameter<DistanceFunction<? super O>> distP = AbstractAlgorithm.makeParameterDistanceFunction(EuclideanDistanceFunction.class, DistanceFunction.class);
      if(config.grab(distP)) {
        distf = distP.instantiateClass(config);
      }
      // k parameters
      IntParameter stepkP = new IntParameter(STEPK_ID) //
      .addConstraint(CommonConstraints.GREATER_EQUAL_ONE_INT);
      if(config.grab(stepkP)) {
        stepk = stepkP.getValue();
      }
      IntParameter startkP = new IntParameter(STARTK_ID);
      startkP.setOptional(true);
      if(config.grab(startkP)) {
        startk = startkP.getValue();
      }
      else {
        startk = stepk;
      }
      IntParameter maxkP = new IntParameter(MAXK_ID)//
      .addConstraint(CommonConstraints.GREATER_EQUAL_ONE_INT);
      if(config.grab(maxkP)) {
        maxk = maxkP.getValue();
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
    }

    @Override
    protected ComputeKNNOutlierScores<O> makeInstance() {
      return new ComputeKNNOutlierScores<>(inputstep, distf, startk, stepk, maxk, bylabel, outfile, scaling, disable);
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
