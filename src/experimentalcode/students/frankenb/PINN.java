package experimentalcode.students.frankenb;

import java.io.File;

import de.lmu.ifi.dbs.elki.algorithm.outlier.LOF;
import de.lmu.ifi.dbs.elki.algorithm.outlier.OutlierAlgorithm;
import de.lmu.ifi.dbs.elki.application.AbstractApplication;
import de.lmu.ifi.dbs.elki.data.NumberVector;
import de.lmu.ifi.dbs.elki.data.type.TypeUtil;
import de.lmu.ifi.dbs.elki.database.Database;
import de.lmu.ifi.dbs.elki.database.HashmapDatabase;
import de.lmu.ifi.dbs.elki.database.relation.Relation;
import de.lmu.ifi.dbs.elki.distance.distancefunction.EuclideanDistanceFunction;
import de.lmu.ifi.dbs.elki.distance.distancevalue.DoubleDistance;
import de.lmu.ifi.dbs.elki.evaluation.roc.ComputeROCCurve;
import de.lmu.ifi.dbs.elki.logging.Logging;
import de.lmu.ifi.dbs.elki.result.BasicResult;
import de.lmu.ifi.dbs.elki.result.Result;
import de.lmu.ifi.dbs.elki.result.ResultWriter;
import de.lmu.ifi.dbs.elki.result.outlier.OutlierResult;
import de.lmu.ifi.dbs.elki.utilities.exceptions.UnableToComplyException;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.OptionID;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.Parameterization;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.IntParameter;

/**
 * A simple PINN implementation used as a comparison.
 * 
 * @author Florian Frankenberger
 */
public class PINN<V extends NumberVector<V, ?>> extends AbstractApplication {
  /**
   * The logger
   */
  private static final Logging logger = Logging.getLogger(PINN.class);

  public static final OptionID K_ID = OptionID.getOrCreateOptionID("k", "k for kNN search");

  public static final OptionID K_FACTOR_ID = OptionID.getOrCreateOptionID("kfactor", "factor to multiply with k when searching for the neighborhood in the projected space");

  private final Database database;

  private final RandomProjection<V> randomProjection;

  private final ComputeROCCurve rocComputer;

  private final int k;

  private final int kFactor;

  private final File outputFile;

  /**
   * Constructor.
   * 
   * @param verbose
   * @param database
   * @param randomProjection
   * @param rocComputer
   * @param k
   * @param kFactor
   * @param outputFile
   */
  public PINN(boolean verbose, Database database, RandomProjection<V> randomProjection, ComputeROCCurve rocComputer, int k, int kFactor, File outputFile) {
    super(verbose);
    this.database = database;
    this.randomProjection = randomProjection;
    this.rocComputer = rocComputer;
    this.k = k;
    this.kFactor = kFactor;
    this.outputFile = outputFile;
  }

  @Override
  public void run() throws UnableToComplyException {
    logger.verbose("Reading database ...");
    database.initialize();
    Relation<V> dataset = database.getRelation(TypeUtil.NUMBER_VECTOR_FIELD);

    logger.verbose("Projecting ...");
    Relation<V> dataSet = randomProjection.project(dataset);

    logger.verbose("Creating KD-Tree ...");
    KDTree<V> tree = new KDTree<V>(dataSet);

    PINNKnnIndex<V> index = new PINNKnnIndex<V>(dataset, tree, kFactor);
    database.addIndex(index);

    logger.verbose("Running LOF ...");

    OutlierAlgorithm algorithm = new LOF<NumberVector<?, ?>, DoubleDistance>(this.k, EuclideanDistanceFunction.STATIC, EuclideanDistanceFunction.STATIC);
    OutlierResult result = algorithm.run(database);

    logger.verbose("Calculating ROC ...");

    BasicResult totalResult = new BasicResult("ROC Result", "rocresult");
    rocComputer.processNewResult(database, result);

    this.outputFile.mkdirs();
    ResultWriter resultWriter = getResultWriter(this.outputFile);

    for(Result aResult : totalResult.getHierarchy().iterDescendants(result)) {
      resultWriter.processNewResult(database, aResult);
    }
    logger.verbose("Writing results to dir " + this.outputFile);

    logger.verbose("Done.");
  }

  private static ResultWriter getResultWriter(File targetFile) {
    return new ResultWriter(targetFile, false, false);
  }

  public static void main(String[] args) {
    AbstractApplication.runCLIApplication(PINN.class, args);
  }

  /**
   * Parameterization class.
   * 
   * @author Erich Schubert
   * 
   * @apiviz.exclude
   */
  public static class Parameterizer<V extends NumberVector<V, ?>> extends AbstractApplication.Parameterizer {
    private Database database;

    private RandomProjection<V> randomProjection;

    private ComputeROCCurve rocComputer;

    private int k;

    private int kFactor;

    private File outputFile;

    @Override
    protected void makeOptions(Parameterization config) {
      super.makeOptions(config);
      outputFile = getParameterOutputFile(config);

      IntParameter kP = new IntParameter(K_ID, false);
      if(config.grab(kP)) {
        this.k = kP.getValue();
      }

      IntParameter kFactorP = new IntParameter(K_FACTOR_ID, false);
      if(config.grab(kFactorP)) {
        this.kFactor = kFactorP.getValue();
      }

      randomProjection = new RandomProjection<V>(config);

      database = config.tryInstantiate(HashmapDatabase.class);
      rocComputer = config.tryInstantiate(ComputeROCCurve.class);
    }

    @Override
    protected PINN<V> makeInstance() {
      return new PINN<V>(verbose, database, randomProjection, rocComputer, k, kFactor, outputFile);
    }
  }
}
