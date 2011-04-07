/**
 * 
 */
package experimentalcode.frankenb.main;

import java.io.File;

import de.lmu.ifi.dbs.elki.algorithm.AbstractAlgorithm;
import de.lmu.ifi.dbs.elki.algorithm.outlier.LOF;
import de.lmu.ifi.dbs.elki.application.AbstractApplication;
import de.lmu.ifi.dbs.elki.data.NumberVector;
import de.lmu.ifi.dbs.elki.database.Database;
import de.lmu.ifi.dbs.elki.database.connection.DatabaseConnection;
import de.lmu.ifi.dbs.elki.database.connection.FileBasedDatabaseConnection;
import de.lmu.ifi.dbs.elki.distance.distancefunction.EuclideanDistanceFunction;
import de.lmu.ifi.dbs.elki.distance.distancevalue.DoubleDistance;
import de.lmu.ifi.dbs.elki.evaluation.roc.ComputeROCCurve;
import de.lmu.ifi.dbs.elki.result.BasicResult;
import de.lmu.ifi.dbs.elki.result.Result;
import de.lmu.ifi.dbs.elki.result.ResultWriter;
import de.lmu.ifi.dbs.elki.result.outlier.OutlierResult;
import de.lmu.ifi.dbs.elki.utilities.ClassGenericsUtil;
import de.lmu.ifi.dbs.elki.utilities.exceptions.UnableToComplyException;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.OptionID;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.Parameterization;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.IntParameter;
import experimentalcode.frankenb.algorithms.projection.RandomProjection;
import experimentalcode.frankenb.log.Log;
import experimentalcode.frankenb.log.LogLevel;
import experimentalcode.frankenb.log.StdOutLogWriter;
import experimentalcode.frankenb.log.TraceLevelLogFormatter;
import experimentalcode.frankenb.model.DataBaseDataSet;
import experimentalcode.frankenb.model.KDTree;
import experimentalcode.frankenb.model.PINNKnnIndex;
import experimentalcode.frankenb.model.ifaces.IDataSet;

/**
 * A simple PINN implementation used as a comparison.
 * 
 * @author Florian Frankenberger
 */
public class PINN extends AbstractApplication {

  public static final OptionID K_ID = OptionID.getOrCreateOptionID("k", "k for kNN search");

  public static final OptionID K_FACTOR_ID = OptionID.getOrCreateOptionID("kfactor", "factor to multiply with k when searching for the neighborhood in the projected space");

  private final DatabaseConnection<NumberVector<?, ?>> databaseConnection;

  private final RandomProjection randomProjection;

  private final ComputeROCCurve rocComputer;

  private final int k;

  private final int kFactor;

  private final File outputFile;

  /**
   * Constructor.
   * 
   * @param verbose
   * @param databaseConnection
   * @param randomProjection
   * @param rocComputer
   * @param k
   * @param kFactor
   * @param outputFile
   */
  public PINN(boolean verbose, DatabaseConnection<NumberVector<?, ?>> databaseConnection, RandomProjection randomProjection, ComputeROCCurve rocComputer, int k, int kFactor, File outputFile) {
    super(verbose);
    this.databaseConnection = databaseConnection;
    this.randomProjection = randomProjection;
    this.rocComputer = rocComputer;
    this.k = k;
    this.kFactor = kFactor;
    this.outputFile = outputFile;

    Log.setLogFormatter(new TraceLevelLogFormatter());
    Log.addLogWriter(new StdOutLogWriter());
    Log.setFilter(LogLevel.DEBUG);
  }

  @Override
  public void run() throws UnableToComplyException {
    Log.info("Reading database ...");
    final Database<NumberVector<?, ?>> dataBase = databaseConnection.getDatabase(null);

    Log.info("Projecting ...");
    IDataSet dataSet = randomProjection.project(new DataBaseDataSet(dataBase));

    Log.info("Creating KD-Tree ...");
    KDTree tree = new KDTree(dataSet);

    PINNKnnIndex index = new PINNKnnIndex(tree, kFactor);
    dataBase.addIndex(index);

    Log.info("Running LOF ...");

    AbstractAlgorithm<NumberVector<?, ?>, OutlierResult> algorithm = new LOF<NumberVector<?, ?>, DoubleDistance>(this.k, EuclideanDistanceFunction.STATIC, EuclideanDistanceFunction.STATIC);
    OutlierResult result = algorithm.run(dataBase);

    Log.info("Calculating ROC ...");

    BasicResult totalResult = new BasicResult("ROC Result", "rocresult");
    rocComputer.processResult(dataBase, result, totalResult.getHierarchy());

    this.outputFile.mkdirs();
    ResultWriter<NumberVector<?, ?>> resultWriter = getResultWriter(this.outputFile);

    for(Result aResult : totalResult.getHierarchy().iterDescendants(result)) {
      resultWriter.processResult(dataBase, aResult);
    }
    Log.info("Writing results to dir " + this.outputFile);

    Log.info("Done.");
  }

  private static ResultWriter<NumberVector<?, ?>> getResultWriter(File targetFile) {
    return new ResultWriter<NumberVector<?, ?>>(targetFile, false, false);
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
  public static class Parameterizer extends AbstractApplication.Parameterizer {
    private FileBasedDatabaseConnection<NumberVector<?, ?>> databaseConnection;

    private RandomProjection randomProjection;

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

      randomProjection = new RandomProjection(config);

      Class<FileBasedDatabaseConnection<NumberVector<?, ?>>> dbcls = ClassGenericsUtil.uglyCastIntoSubclass(FileBasedDatabaseConnection.class);
      databaseConnection = config.tryInstantiate(dbcls);
      rocComputer = config.tryInstantiate(ComputeROCCurve.class);
    }

    @Override
    protected PINN makeInstance() {
      return new PINN(verbose, databaseConnection, randomProjection, rocComputer, k, kFactor, outputFile);
    }
  }
}
