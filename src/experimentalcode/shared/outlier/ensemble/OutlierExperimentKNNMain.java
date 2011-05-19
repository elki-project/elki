package experimentalcode.shared.outlier.ensemble;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintStream;

import de.lmu.ifi.dbs.elki.algorithm.AbstractAlgorithm;
import de.lmu.ifi.dbs.elki.algorithm.outlier.ABOD;
import de.lmu.ifi.dbs.elki.algorithm.outlier.ByLabelOutlier;
import de.lmu.ifi.dbs.elki.algorithm.outlier.KNNOutlier;
import de.lmu.ifi.dbs.elki.algorithm.outlier.KNNWeightOutlier;
import de.lmu.ifi.dbs.elki.algorithm.outlier.LDOF;
import de.lmu.ifi.dbs.elki.algorithm.outlier.LOF;
import de.lmu.ifi.dbs.elki.algorithm.outlier.LoOP;
import de.lmu.ifi.dbs.elki.algorithm.outlier.TrivialAllOutlier;
import de.lmu.ifi.dbs.elki.algorithm.outlier.TrivialNoOutlier;
import de.lmu.ifi.dbs.elki.application.AbstractApplication;
import de.lmu.ifi.dbs.elki.data.DoubleVector;
import de.lmu.ifi.dbs.elki.database.Database;
import de.lmu.ifi.dbs.elki.database.ids.DBID;
import de.lmu.ifi.dbs.elki.database.ids.DBIDs;
import de.lmu.ifi.dbs.elki.database.query.knn.KNNQuery;
import de.lmu.ifi.dbs.elki.database.query.knn.PreprocessorKNNQuery;
import de.lmu.ifi.dbs.elki.database.relation.Relation;
import de.lmu.ifi.dbs.elki.distance.distancefunction.DistanceFunction;
import de.lmu.ifi.dbs.elki.distance.distancefunction.EuclideanDistanceFunction;
import de.lmu.ifi.dbs.elki.distance.distancevalue.DoubleDistance;
import de.lmu.ifi.dbs.elki.distance.distancevalue.NumberDistance;
import de.lmu.ifi.dbs.elki.distance.similarityfunction.kernel.PolynomialKernelFunction;
import de.lmu.ifi.dbs.elki.index.preprocessed.knn.MaterializeKNNPreprocessor;
import de.lmu.ifi.dbs.elki.logging.Logging;
import de.lmu.ifi.dbs.elki.result.AnnotationResult;
import de.lmu.ifi.dbs.elki.result.Result;
import de.lmu.ifi.dbs.elki.result.ResultHierarchy;
import de.lmu.ifi.dbs.elki.result.outlier.OutlierResult;
import de.lmu.ifi.dbs.elki.utilities.FormatUtil;
import de.lmu.ifi.dbs.elki.utilities.exceptions.AbortException;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.OptionID;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.constraints.GreaterConstraint;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.Parameterization;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.IntParameter;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.ObjectParameter;
import de.lmu.ifi.dbs.elki.utilities.scaling.IdentityScaling;
import de.lmu.ifi.dbs.elki.utilities.scaling.ScalingFunction;
import de.lmu.ifi.dbs.elki.utilities.scaling.outlier.MinusLogStandardDeviationScaling;
import de.lmu.ifi.dbs.elki.utilities.scaling.outlier.StandardDeviationScaling;
import de.lmu.ifi.dbs.elki.workflow.InputStep;

/**
 * Application that runs a series of algorithms on a data set.
 * 
 * @author Erich Schubert
 */
public class OutlierExperimentKNNMain<O, D extends NumberDistance<D, ?>> extends AbstractApplication {
  /**
   * Our logger class.
   */
  private static final Logging logger = Logging.getLogger(OutlierExperimentKNNMain.class);

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
   * Constructor.
   * 
   * @param verbose Verbose flag
   * @param inputstep Input step
   * @param distf Distance function
   * @param startk Starting value of k
   * @param stepk K step size
   * @param maxk Maximum k value
   * @param bylabel By label outlier (reference)
   * @param outfile Output file
   */
  public OutlierExperimentKNNMain(boolean verbose, InputStep inputstep, DistanceFunction<? super O, D> distf, int startk, int stepk, int maxk, ByLabelOutlier bylabel, File outfile) {
    super(verbose);
    this.distf = distf;
    this.startk = startk;
    this.stepk = stepk;
    this.maxk = maxk;
    this.inputstep = inputstep;
    this.bylabel = bylabel;
    this.outfile = outfile;
  }

  @Override
  public void run() {
    final Database database = inputstep.getDatabase();
    final Relation<O> relation = database.getRelation(distf.getInputTypeRestriction());
    logger.verbose("Running preprocessor ...");
    MaterializeKNNPreprocessor<O, D> preproc = new MaterializeKNNPreprocessor<O, D>(relation, distf, maxk);
    database.addIndex(preproc);

    // Test that we did get a proper index query
    KNNQuery<O, D> knnq = database.getKNNQuery(relation, distf);
    if(!(knnq instanceof PreprocessorKNNQuery)) {
      logger.warning("Not using preprocessor knn query -- KNN queries using class: " + knnq.getClass());
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
      fout.append("# DBIDs:");
      for(DBID id : ids) {
        fout.append(" ").append(id.toString());
      }
      fout.append(FormatUtil.NEWLINE);
    }

    // Label outlier result (reference)
    {
      OutlierResult bylabelresult = bylabel.run(database);
      writeResult(fout, ids, bylabelresult, new IdentityScaling(), "bylabel");
    }
    // No/all outliers "results"
    {
      OutlierResult noresult = (new TrivialNoOutlier()).run(database);
      writeResult(fout, ids, noresult, new IdentityScaling(), "no-outliers");
      OutlierResult allresult = (new TrivialAllOutlier()).run(database);
      writeResult(fout, ids, allresult, new IdentityScaling(), "all-outliers");
    }

    // Run LOF
    logger.verbose("Running LOF");
    runForEachK(new AlgRunner() {
      @Override
      public void run(int k, String kstr) {
        LOF<O, D> lof = new LOF<O, D>(k, distf, distf);
        OutlierResult lofresult = lof.run(database, relation);
        // Setup scaling
        StandardDeviationScaling scaling = new StandardDeviationScaling(1.0, 1.0);
        scaling.prepare(ids, lofresult);
        writeResult(fout, ids, lofresult, scaling, "LOF-" + kstr);
        detachResult(database, lofresult);
      }
    });
    // LoOP
    logger.verbose("Running LoOP");
    runForEachK(new AlgRunner() {
      @Override
      public void run(int k, String kstr) {
        LoOP<O, D> loop = new LoOP<O, D>(k, k, distf, distf, 1.0);
        OutlierResult loopresult = loop.run(database, relation);
        writeResult(fout, ids, loopresult, new IdentityScaling(), "LOOP-" + kstr);
        detachResult(database, loopresult);
      }
    });
    // LDOF
    logger.verbose("Running LDOF");
    runForEachK(new AlgRunner() {
      @Override
      public void run(int k, String kstr) {
        LDOF<O, D> ldof = new LDOF<O, D>(distf, k);
        OutlierResult lofresult = ldof.run(database, relation);
        // Setup scaling
        StandardDeviationScaling scaling = new StandardDeviationScaling(1.0, 1.0);
        scaling.prepare(ids, lofresult);
        writeResult(fout, ids, lofresult, scaling, "LDOF-" + kstr);
        detachResult(database, lofresult);
      }
    });
    // KNN
    logger.verbose("Running KNN");
    runForEachK(new AlgRunner() {
      @Override
      public void run(int k, String kstr) {
        KNNOutlier<O, D> knn = new KNNOutlier<O, D>(distf, k);
        OutlierResult knnresult = knn.run(database, relation);
        // Setup scaling
        StandardDeviationScaling scaling = new StandardDeviationScaling();
        scaling.prepare(ids, knnresult);
        writeResult(fout, ids, knnresult, scaling, "KNN-" + kstr);
        detachResult(database, knnresult);
      }
    });
    // KNN Weight
    logger.verbose("Running KNNweight");
    runForEachK(new AlgRunner() {
      @Override
      public void run(int k, String kstr) {
        KNNWeightOutlier<O, D> knnw = new KNNWeightOutlier<O, D>(distf, k);
        OutlierResult knnresult = knnw.run(database, relation);
        // Setup scaling
        StandardDeviationScaling scaling = new StandardDeviationScaling();
        scaling.prepare(ids, knnresult);
        writeResult(fout, ids, knnresult, scaling, "KNNW-" + kstr);
        detachResult(database, knnresult);
      }
    });
    // ABOD
    try {
      final PolynomialKernelFunction poly = new PolynomialKernelFunction(PolynomialKernelFunction.DEFAULT_DEGREE);
      @SuppressWarnings("unchecked")
      final DistanceFunction<DoubleVector, DoubleDistance> df = DistanceFunction.class.cast(distf);
      logger.verbose("Running ABOD");
      runForEachK(new AlgRunner() {
        @Override
        public void run(int k, String kstr) {
          ABOD<DoubleVector> abod = new ABOD<DoubleVector>(k, poly, df);
          OutlierResult abodresult = abod.run(database);
          // Setup scaling
          StandardDeviationScaling scaling = new MinusLogStandardDeviationScaling(null, 1.0);
          scaling.prepare(ids, abodresult);
          writeResult(fout, ids, abodresult, scaling, "ABOD-" + kstr);
          detachResult(database, abodresult);
        }
      });
    }
    catch(ClassCastException e) {
      // ABOD might just be not appropriate.
      logger.warning("Running ABOD failed - probably not appropriate to this data type / distance?", e);
    }
  }

  /**
   * Avoid that (future changes?) keep a reference to the result.
   * 
   * @param database Database
   * @param discardresult Result to discard.
   */
  void detachResult(Database database, OutlierResult discardresult) {
    final ResultHierarchy hier = database.getHierarchy();
    for(Result parent : hier.getParents(discardresult)) {
      hier.remove(parent, discardresult);
    }
  }

  /**
   * Write a single output line.
   * 
   * @param out Output stream
   * @param ids DBIDs
   * @param result Outlier result
   * @param scaling Scaling function.
   * @param label Identification label
   */
  void writeResult(PrintStream out, DBIDs ids, OutlierResult result, ScalingFunction scaling, String label) {
    out.append(label);
    AnnotationResult<Double> scores = result.getScores();
    for(DBID id : ids) {
      final double value = scaling.getScaled(scores.getValueFor(id));
      out.append(" ").append(FormatUtil.format(value, FormatUtil.NF8));
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
  public static class Parameterizer<O, D extends NumberDistance<D, ?>> extends AbstractApplication.Parameterizer {
    /**
     * Option ID for k step size.
     */
    public static final OptionID STEPK_ID = OptionID.getOrCreateOptionID("stepk", "Step size for k.");

    /**
     * Option ID for k start size.
     */
    public static final OptionID STARTK_ID = OptionID.getOrCreateOptionID("startk", "Minimum value for k.");

    /**
     * Option ID for k step size.
     */
    public static final OptionID MAXK_ID = OptionID.getOrCreateOptionID("maxk", "Maximum value for k.");

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
      IntParameter stepkP = new IntParameter(STEPK_ID, new GreaterConstraint(0));
      if(config.grab(stepkP)) {
        stepk = stepkP.getValue();
      }
      IntParameter startkP = new IntParameter(STARTK_ID, true);
      if(config.grab(startkP)) {
        startk = startkP.getValue();
      }
      else {
        startk = stepk;
      }
      IntParameter maxkP = new IntParameter(MAXK_ID, new GreaterConstraint(0));
      if(config.grab(maxkP)) {
        maxk = maxkP.getValue();
      }
      bylabel = config.tryInstantiate(ByLabelOutlier.class);
      // Output
      outfile = super.getParameterOutputFile(config, "File to output the resulting score vectors to.");
    }

    @Override
    protected AbstractApplication makeInstance() {
      return new OutlierExperimentKNNMain<O, D>(verbose, inputstep, distf, startk, stepk, maxk, bylabel, outfile);
    }
  }

  /**
   * Main method.
   * 
   * @param args Command line parameters.
   */
  public static void main(String[] args) {
    OutlierExperimentKNNMain.runCLIApplication(OutlierExperimentKNNMain.class, args);
  }
}