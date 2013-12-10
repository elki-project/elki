package de.lmu.ifi.dbs.elki.application.greedyensemble;

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
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import de.lmu.ifi.dbs.elki.algorithm.AbstractAlgorithm;
import de.lmu.ifi.dbs.elki.algorithm.outlier.KNNOutlier;
import de.lmu.ifi.dbs.elki.algorithm.outlier.KNNWeightOutlier;
import de.lmu.ifi.dbs.elki.algorithm.outlier.lof.LDF;
import de.lmu.ifi.dbs.elki.algorithm.outlier.lof.LDOF;
import de.lmu.ifi.dbs.elki.algorithm.outlier.lof.LOF;
import de.lmu.ifi.dbs.elki.algorithm.outlier.lof.LoOP;
import de.lmu.ifi.dbs.elki.algorithm.outlier.lof.SimplifiedLOF;
import de.lmu.ifi.dbs.elki.algorithm.outlier.trivial.ByLabelOutlier;
import de.lmu.ifi.dbs.elki.algorithm.outlier.trivial.TrivialAllOutlier;
import de.lmu.ifi.dbs.elki.algorithm.outlier.trivial.TrivialNoOutlier;
import de.lmu.ifi.dbs.elki.application.AbstractApplication;
import de.lmu.ifi.dbs.elki.data.NumberVector;
import de.lmu.ifi.dbs.elki.database.Database;
import de.lmu.ifi.dbs.elki.database.QueryUtil;
import de.lmu.ifi.dbs.elki.database.ids.DBIDIter;
import de.lmu.ifi.dbs.elki.database.ids.DBIDUtil;
import de.lmu.ifi.dbs.elki.database.ids.DBIDs;
import de.lmu.ifi.dbs.elki.database.query.knn.KNNQuery;
import de.lmu.ifi.dbs.elki.database.query.knn.PreprocessorKNNQuery;
import de.lmu.ifi.dbs.elki.database.relation.Relation;
import de.lmu.ifi.dbs.elki.distance.distancefunction.DistanceFunction;
import de.lmu.ifi.dbs.elki.distance.distancefunction.minkowski.EuclideanDistanceFunction;
import de.lmu.ifi.dbs.elki.distance.distancevalue.NumberDistance;
import de.lmu.ifi.dbs.elki.index.preprocessed.knn.MaterializeKNNPreprocessor;
import de.lmu.ifi.dbs.elki.logging.Logging;
import de.lmu.ifi.dbs.elki.math.statistics.kernelfunctions.GaussianKernelDensityFunction;
import de.lmu.ifi.dbs.elki.result.outlier.OutlierResult;
import de.lmu.ifi.dbs.elki.utilities.Base64;
import de.lmu.ifi.dbs.elki.utilities.FormatUtil;
import de.lmu.ifi.dbs.elki.utilities.documentation.Reference;
import de.lmu.ifi.dbs.elki.utilities.exceptions.AbortException;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.OptionID;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.constraints.CommonConstraints;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.Parameterization;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.IntParameter;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.ObjectParameter;
import de.lmu.ifi.dbs.elki.utilities.scaling.IdentityScaling;
import de.lmu.ifi.dbs.elki.utilities.scaling.ScalingFunction;
import de.lmu.ifi.dbs.elki.utilities.scaling.outlier.OutlierScalingFunction;
import de.lmu.ifi.dbs.elki.workflow.InputStep;

/**
 * Application that runs a series of kNN-based algorithms on a data set, for
 * building an ensemble in a second step. The output file consists of a label
 * and one score value for each object.
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
 */
@Reference(authors = "E. Schubert, R. Wojdanowski, A. Zimek, H.-P. Kriegel", title = "On Evaluation of Outlier Rankings and Outlier Scores", booktitle = "Proc. 12th SIAM International Conference on Data Mining (SDM), Anaheim, CA, 2012.")
public class ComputeKNNOutlierScores<O extends NumberVector<?>, D extends NumberDistance<D, ?>> extends AbstractApplication {
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
  final DistanceFunction<? super O, D> distf;

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
   */
  public ComputeKNNOutlierScores(InputStep inputstep, DistanceFunction<? super O, D> distf, int startk, int stepk, int maxk, ByLabelOutlier bylabel, File outfile, ScalingFunction scaling) {
    super();
    this.distf = distf;
    this.startk = startk;
    this.stepk = stepk;
    this.maxk = maxk;
    this.inputstep = inputstep;
    this.bylabel = bylabel;
    this.outfile = outfile;
    this.scaling = scaling;
  }

  @Override
  public void run() {
    final Database database = inputstep.getDatabase();
    final Relation<O> relation = database.getRelation(distf.getInputTypeRestriction());

    // If there is no kNN preprocessor already, then precompute.
    KNNQuery<O, D> knnq = QueryUtil.getKNNQuery(relation, distf, maxk + 2);
    if(!(knnq instanceof PreprocessorKNNQuery)) {
      LOG.verbose("Running preprocessor ...");
      MaterializeKNNPreprocessor<O, D> preproc = new MaterializeKNNPreprocessor<>(relation, distf, maxk + 2);
      database.addIndex(preproc);
    }

    // Test that we now get a proper index query
    knnq = QueryUtil.getKNNQuery(relation, distf, maxk + 2);
    if(!(knnq instanceof PreprocessorKNNQuery)) {
      LOG.warning("Not using preprocessor knn query -- KNN queries using class: " + knnq.getClass());
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
    {
      try {
        MessageDigest md = MessageDigest.getInstance("MD5");
        for(DBIDIter iter = ids.iter(); iter.valid(); iter.advance()) {
          md.update((byte) ' ');
          md.update(DBIDUtil.toString(iter).getBytes());
        }
        fout.append("# DBID-series MD5:");
        fout.append(Base64.encodeBase64(md.digest()));
        fout.append(FormatUtil.NEWLINE);
      }
      catch(NoSuchAlgorithmException e) {
        throw new AbortException("MD5 not found.");
      }
    }

    // Label outlier result (reference)
    {
      OutlierResult bylabelresult = bylabel.run(database);
      writeResult(fout, ids, bylabelresult, new IdentityScaling(), "bylabel");
    }
    // No/all outliers "results"
    boolean withdummy = false;
    if(withdummy) {
      OutlierResult noresult = (new TrivialNoOutlier()).run(database);
      writeResult(fout, ids, noresult, new IdentityScaling(), "no-outliers");
      OutlierResult allresult = (new TrivialAllOutlier()).run(database);
      writeResult(fout, ids, allresult, new IdentityScaling(), "all-outliers");
    }

    // KNN
    LOG.verbose("Running KNN");
    runForEachK(new AlgRunner() {
      @Override
      public void run(int k, String kstr) {
        KNNOutlier<O, D> knn = new KNNOutlier<>(distf, k);
        OutlierResult knnresult = knn.run(database, relation);
        writeResult(fout, ids, knnresult, scaling, "KNN-" + kstr);
        database.getHierarchy().removeSubtree(knnresult);
      }
    });
    // KNN Weight
    LOG.verbose("Running KNNweight");
    runForEachK(new AlgRunner() {
      @Override
      public void run(int k, String kstr) {
        KNNWeightOutlier<O, D> knnw = new KNNWeightOutlier<>(distf, k);
        OutlierResult knnresult = knnw.run(database, relation);
        writeResult(fout, ids, knnresult, scaling, "KNNW-" + kstr);
        database.getHierarchy().removeSubtree(knnresult);
      }
    });
    // Run LOF
    LOG.verbose("Running LOF");
    runForEachK(new AlgRunner() {
      @Override
      public void run(int k, String kstr) {
        LOF<O, D> lof = new LOF<>(k, distf);
        OutlierResult lofresult = lof.run(database, relation);
        writeResult(fout, ids, lofresult, scaling, "LOF-" + kstr);
        database.getHierarchy().removeSubtree(lofresult);
      }
    });
    // Run Simplified-LOF
    LOG.verbose("Running Simplified-LOF");
    runForEachK(new AlgRunner() {
      @Override
      public void run(int k, String kstr) {
        SimplifiedLOF<O, D> lof = new SimplifiedLOF<>(k, distf);
        OutlierResult lofresult = lof.run(database, relation);
        writeResult(fout, ids, lofresult, scaling, "Simplified-LOF-" + kstr);
        database.getHierarchy().removeSubtree(lofresult);
      }
    });
    // LoOP
    LOG.verbose("Running LoOP");
    runForEachK(new AlgRunner() {
      @Override
      public void run(int k, String kstr) {
        LoOP<O, D> loop = new LoOP<>(k, k, distf, distf, 1.0);
        OutlierResult loopresult = loop.run(database, relation);
        writeResult(fout, ids, loopresult, scaling, "LOOP-" + kstr);
        database.getHierarchy().removeSubtree(loopresult);
      }
    });
    // LDOF
    boolean runldof = false;
    if(runldof) {
      LOG.verbose("Running LDOF");
      runForEachK(new AlgRunner() {
        @Override
        public void run(int k, String kstr) {
          LDOF<O, D> ldof = new LDOF<>(distf, k + 1);
          OutlierResult ldofresult = ldof.run(database, relation);
          writeResult(fout, ids, ldofresult, scaling, "LDOF-" + kstr);
          database.getHierarchy().removeSubtree(ldofresult);
        }
      });
    }
    // Run LDF
    LOG.verbose("Running LDF");
    runForEachK(new AlgRunner() {
      @Override
      public void run(int k, String kstr) {
        LDF<O, D> ldf = new LDF<>(k, distf, GaussianKernelDensityFunction.KERNEL, 2, .1);
        OutlierResult ldfresult = ldf.run(database, relation);
        writeResult(fout, ids, ldfresult, scaling, "LDF-" + kstr);
        database.getHierarchy().removeSubtree(ldfresult);
      }
    });
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
    Relation<Double> scores = result.getScores();
    for(DBIDIter iter = ids.iter(); iter.valid(); iter.advance()) {
      double value = scores.get(iter);
      if(scaling != null) {
        value = scaling.getScaled(value);
      }
      out.append(' ').append(FormatUtil.NF.format(value));
    }
    out.append(FormatUtil.NEWLINE);
  }

  /**
   * Iterate over the k range.
   * 
   * @param runner Runner to run
   */
  private void runForEachK(AlgRunner runner) {
    final int digits = (int) Math.ceil(Math.log10(maxk));
    final int startk = (this.startk > 0) ? this.startk : this.stepk;
    for(int k = startk; k <= maxk; k += stepk) {
      String kstr = String.format("%0" + digits + "d", k);
      runner.run(k, kstr);
    }
  }

  /**
   * Run an algorithm for a given k.
   * 
   * @author Erich Schubert
   * 
   * @apiviz.exclude
   */
  private interface AlgRunner {
    public void run(int k, String kstr);
  }

  /**
   * Parameterization class.
   * 
   * @author Erich Schubert
   * 
   * @apiviz.exclude
   */
  public static class Parameterizer<O extends NumberVector<?>, D extends NumberDistance<D, ?>> extends AbstractApplication.Parameterizer {
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
     * Option ID for scaling class
     */
    public static final OptionID SCALING_ID = new OptionID("scaling", "Scaling function.");

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
    DistanceFunction<? super O, D> distf;

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

    @Override
    protected void makeOptions(Parameterization config) {
      super.makeOptions(config);
      // Data input
      inputstep = config.tryInstantiate(InputStep.class);
      // Distance function
      ObjectParameter<DistanceFunction<? super O, D>> distP = AbstractAlgorithm.makeParameterDistanceFunction(EuclideanDistanceFunction.class, DistanceFunction.class);
      if(config.grab(distP)) {
        distf = distP.instantiateClass(config);
      }
      // k parameters
      IntParameter stepkP = new IntParameter(STEPK_ID);
      stepkP.addConstraint(CommonConstraints.GREATER_EQUAL_ONE_INT);
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
      IntParameter maxkP = new IntParameter(MAXK_ID);
      maxkP.addConstraint(CommonConstraints.GREATER_EQUAL_ONE_INT);
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
    }

    @Override
    protected ComputeKNNOutlierScores<O, D> makeInstance() {
      return new ComputeKNNOutlierScores<>(inputstep, distf, startk, stepk, maxk, bylabel, outfile, scaling);
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
