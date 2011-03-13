/**
 * 
 */
package experimentalcode.frankenb.main;

import java.io.File;

import de.lmu.ifi.dbs.elki.algorithm.AbstractAlgorithm;
import de.lmu.ifi.dbs.elki.algorithm.outlier.LOF;
import de.lmu.ifi.dbs.elki.application.StandAloneApplication;
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
import de.lmu.ifi.dbs.elki.utilities.exceptions.UnableToComplyException;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.OptionID;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.ListParameterization;
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
 * No description given.
 * 
 * @author Florian Frankenberger
 */
public class PINN extends StandAloneApplication {

  public static final OptionID K_ID = OptionID.getOrCreateOptionID("k", "k for kNN search");
  private final IntParameter K_PARAM = new IntParameter(K_FACTOR_ID, false);
  
  public static final OptionID K_FACTOR_ID = OptionID.getOrCreateOptionID("kfactor", "factor to multiply with k when searching for the neighborhood in the projected space");
  private final IntParameter K_FACTOR_PARAM = new IntParameter(K_FACTOR_ID, false);    
  
  private final DatabaseConnection<NumberVector<?, ?>> databaseConnection;
  private final RandomProjection randomProjection;
  
  private final ComputeROCCurve<NumberVector<?,?>> rocComputer;
  private int k;
  private int kFactor;
  
  public PINN(Parameterization config) {
    super(config);
    Log.setLogFormatter(new TraceLevelLogFormatter());
    Log.addLogWriter(new StdOutLogWriter());
    Log.setFilter(LogLevel.DEBUG);
   
    if (config.grab(K_PARAM)) {
      this.k = K_PARAM.getValue();
    }
    
    if (config.grab(K_FACTOR_PARAM)) {
      this.kFactor = K_FACTOR_PARAM.getValue();
    }
    
    databaseConnection = FileBasedDatabaseConnection.parameterize(config);
    randomProjection = new RandomProjection(config);
    
    rocComputer = new ComputeROCCurve<NumberVector<?,?>>(config);
  }
  
  /* (non-Javadoc)
   * @see de.lmu.ifi.dbs.elki.application.AbstractApplication#run()
   */
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
    //tree.findNearestNeighbors(7, 10, EuclideanDistanceFunction.STATIC);
    
    this.getOutput().mkdirs();
    ResultWriter<NumberVector<?, ?>> resultWriter = getResultWriter(this.getOutput());

    for (Result aResult : totalResult.getHierarchy().iterDescendants(result)) {
      resultWriter.processResult(dataBase, aResult);
    }
    Log.info("Writing results to dir " + this.getOutput());
    
    Log.info("Done.");
  }
  
  private static ResultWriter<NumberVector<?, ?>> getResultWriter(File targetFile) {
    ListParameterization config = new ListParameterization();
    config.addParameter(OptionID.OUTPUT, targetFile);
    return new ResultWriter<NumberVector<?, ?>>(config);
  }
  
  /* (non-Javadoc)
   * @see de.lmu.ifi.dbs.elki.application.StandAloneApplication#getOutputDescription()
   */
  @Override
  public String getOutputDescription() {
    // TODO Auto-generated method stub
    return null;
  }
  
  public static void main(String[] args) {
    StandAloneApplication.runCLIApplication(PINN.class, args);
  }  

}
