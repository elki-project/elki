package experimentalcode.shared.outlier.ensemble;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintStream;

import de.lmu.ifi.dbs.elki.algorithm.AbstractAlgorithm;
import de.lmu.ifi.dbs.elki.algorithm.outlier.ByLabelOutlier;
import de.lmu.ifi.dbs.elki.algorithm.outlier.KNNOutlier;
import de.lmu.ifi.dbs.elki.algorithm.outlier.KNNWeightOutlier;
import de.lmu.ifi.dbs.elki.algorithm.outlier.LDOF;
import de.lmu.ifi.dbs.elki.algorithm.outlier.LOF;
import de.lmu.ifi.dbs.elki.algorithm.outlier.LoOP;
import de.lmu.ifi.dbs.elki.application.AbstractApplication;
import de.lmu.ifi.dbs.elki.database.Database;
import de.lmu.ifi.dbs.elki.database.ids.DBID;
import de.lmu.ifi.dbs.elki.database.ids.DBIDs;
import de.lmu.ifi.dbs.elki.database.query.knn.KNNQuery;
import de.lmu.ifi.dbs.elki.database.query.knn.PreprocessorKNNQuery;
import de.lmu.ifi.dbs.elki.database.relation.Relation;
import de.lmu.ifi.dbs.elki.distance.distancefunction.DistanceFunction;
import de.lmu.ifi.dbs.elki.distance.distancefunction.EuclideanDistanceFunction;
import de.lmu.ifi.dbs.elki.distance.distancevalue.NumberDistance;
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
  private final InputStep inputstep;

  /**
   * Distance function to use
   */
  private DistanceFunction<? super O, D> distf;

  /**
   * k step size
   */
  private final int stepk;

  /**
   * Maximum value of k
   */
  private final int maxk;

  /**
   * Output file
   */
  private File outfile;

  /**
   * By label outlier detection - reference
   */
  private ByLabelOutlier bylabel;

  /**
   * Constructor.
   * 
   * @param verbose Verbose flag
   * @param inputstep Input step
   * @param distf Distance function
   * @param stepk K step size
   * @param maxk Maximum k value
   * @param bylabel By label outlier (reference) 
   * @param outfile Output file
   */
  public OutlierExperimentKNNMain(boolean verbose, InputStep inputstep, DistanceFunction<? super O, D> distf, int stepk, int maxk, ByLabelOutlier bylabel, File outfile) {
    super(verbose);
    this.distf = distf;
    this.stepk = stepk;
    this.maxk = maxk;
    this.inputstep = inputstep;
    this.bylabel = bylabel;
    this.outfile = outfile;
  }

  @Override
  public void run() {
    Database database = inputstep.getDatabase();
    Relation<O> relation = database.getRelation(distf.getInputTypeRestriction());
    logger.verbose("Running preprocessor ...");
    MaterializeKNNPreprocessor<O, D> preproc = new MaterializeKNNPreprocessor<O, D>(relation, distf, maxk);
    database.addIndex(preproc);

    // Test that we did get a proper index query
    KNNQuery<O, D> knnq = database.getKNNQuery(relation, distf);
    if(!(knnq instanceof PreprocessorKNNQuery)) {
      logger.warning("Not using preprocessor knn query -- KNN queries using class: " + knnq.getClass());
    }

    DBIDs ids = relation.getDBIDs();

    PrintStream fout;
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

    // Iterate over ks.
    for(int k = stepk; k <= maxk; k += stepk) {
      logger.verbose("Running for k=" + k);
      // LOF
      {
        LOF<O, D> lof = new LOF<O, D>(k, distf, distf);
        OutlierResult lofresult = lof.run(database, relation);
        // Setup scaling
        StandardDeviationScaling scaling = new StandardDeviationScaling(1.0, 1.0);
        scaling.prepare(ids, lofresult);
        writeResult(fout, ids, lofresult, scaling, "LOF-" + k);
        detachResult(database, lofresult);
      }
      // LoOP
      {
        LoOP<O, D> loop = new LoOP<O, D>(k, k, distf, distf, 1.0);
        OutlierResult loopresult = loop.run(database, relation);
        writeResult(fout, ids, loopresult, new IdentityScaling(), "LOOP-" + k);
        detachResult(database, loopresult);
      }
      // KNN
      {
        KNNOutlier<O, D> knn = new KNNOutlier<O, D>(distf, k);
        OutlierResult knnresult = knn.run(database, relation);
        // Setup scaling
        StandardDeviationScaling scaling = new StandardDeviationScaling();
        scaling.prepare(ids, knnresult);
        writeResult(fout, ids, knnresult, scaling, "KNN-" + k);
        detachResult(database, knnresult);
      }
      // KNN Weight
      {
        KNNWeightOutlier<O, D> knnw = new KNNWeightOutlier<O, D>(distf, k);
        OutlierResult knnresult = knnw.run(database, relation);
        // Setup scaling
        StandardDeviationScaling scaling = new StandardDeviationScaling();
        scaling.prepare(ids, knnresult);
        writeResult(fout, ids, knnresult, scaling, "KNNW-" + k);
        detachResult(database, knnresult);
      }
      // LDOF
      {
        LDOF<O, D> ldof = new LDOF<O, D>(distf, k);
        OutlierResult lofresult = ldof.run(database, relation);
        // Setup scaling
        StandardDeviationScaling scaling = new StandardDeviationScaling(1.0, 1.0);
        scaling.prepare(ids, lofresult);
        writeResult(fout, ids, lofresult, scaling, "LDOF-" + k);
        detachResult(database, lofresult);
      }
    }
  }

  /**
   * Avoid that (future changes?) keep a reference to the result.
   * 
   * @param database Database
   * @param discardresult Result to discard.
   */
  private void detachResult(Database database, OutlierResult discardresult) {
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
  private void writeResult(PrintStream out, DBIDs ids, OutlierResult result, ScalingFunction scaling, String label) {
    out.append(label);
    AnnotationResult<Double> scores = result.getScores();
    for(DBID id : ids) {
      final double value = scaling.getScaled(scores.getValueFor(id));
      out.append(" ").append(FormatUtil.format(value));
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
  public static class Parameterizer<O, D extends NumberDistance<D, ?>> extends AbstractApplication.Parameterizer {
    /**
     * Option ID for k step size.
     */
    public static final OptionID STEPK_ID = OptionID.getOrCreateOptionID("stepk", "Step size for k.");

    /**
     * Option ID for k step size.
     */
    public static final OptionID MAXK_ID = OptionID.getOrCreateOptionID("maxk", "Maximum value for k.");

    /**
     * k step size
     */
    int stepk;

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
      return new OutlierExperimentKNNMain<O, D>(verbose, inputstep, distf, stepk, maxk, bylabel, outfile);
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