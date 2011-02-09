/**
 * 
 */
package experimentalcode.frankenb.main;

import java.io.File;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import de.lmu.ifi.dbs.elki.algorithm.AbstractAlgorithm;
import de.lmu.ifi.dbs.elki.algorithm.outlier.LOF;
import de.lmu.ifi.dbs.elki.algorithm.outlier.LoOP;
import de.lmu.ifi.dbs.elki.application.AbstractApplication;
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
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.FileParameter;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.Flag;
import experimentalcode.frankenb.log.Log;
import experimentalcode.frankenb.log.LogLevel;
import experimentalcode.frankenb.log.StdOutLogWriter;
import experimentalcode.frankenb.log.TraceLevelLogFormatter;
import experimentalcode.frankenb.model.ConstantSizeIntegerSerializer;
import experimentalcode.frankenb.model.DistanceList;
import experimentalcode.frankenb.model.DistanceListSerializer;
import experimentalcode.frankenb.model.DynamicBPlusTree;
import experimentalcode.frankenb.model.PrecalculatedKnnIndex;
import experimentalcode.frankenb.model.datastorage.BufferedDiskBackedDataStorage;
import experimentalcode.frankenb.model.datastorage.DiskBackedDataStorage;

/**
 * This class will do all performance tests with the following algorithms and
 * write the ROC Curves in a standardized form in the ./result subdirectory 
 * <ul>
 *  <li>LOF</li>
 *  <li>...</li>
 * </ul>
 * 
 * @author Florian Frankenberger
 */
public class KnnPerformanceTestSuite extends AbstractApplication {

  private static final PerformanceTest[] PERFORMANCE_TESTS = new PerformanceTest[] {
//    new PerformanceTest(new LOF<NumberVector<?, ?>, DoubleDistance>(10, EuclideanDistanceFunction.STATIC, EuclideanDistanceFunction.STATIC)),
//    new PerformanceTest(new LOF<NumberVector<?, ?>, DoubleDistance>(20, EuclideanDistanceFunction.STATIC, EuclideanDistanceFunction.STATIC)),
//    new PerformanceTest(new LOF<NumberVector<?, ?>, DoubleDistance>(45, EuclideanDistanceFunction.STATIC, EuclideanDistanceFunction.STATIC)),
    new PerformanceTest(new LoOP<NumberVector<?, ?>, DoubleDistance>(10, 10, EuclideanDistanceFunction.STATIC, EuclideanDistanceFunction.STATIC, 2)),
  };
  
  private static class PerformanceTest {
    private final AbstractAlgorithm<NumberVector<?, ?>, OutlierResult> algorithm;
    
    public PerformanceTest(AbstractAlgorithm<NumberVector<?, ?>, OutlierResult> algorithm) {
      this.algorithm = algorithm;
    }
    
    public AbstractAlgorithm<NumberVector<?, ?>, OutlierResult> getAlgorithm() {
      return this.algorithm;
    }
  }
  
  public static final OptionID IN_MEMORY_ID = OptionID.getOrCreateOptionID("inmemory", "tells wether the resulting tree data should be buffered in memory or not. This can increase performance but can also lead to OutOfMemoryExceptions!");
  private final Flag IN_MEMORY_PARAM = new Flag(IN_MEMORY_ID);
  
  public static final OptionID INPUT_ID = OptionID.getOrCreateOptionID("app.in", "");
  private final FileParameter INPUT_PARAM = new FileParameter(INPUT_ID, FileParameter.FileType.INPUT_FILE);
  
  private final ComputeROCCurve<NumberVector<?, ?>> rocComputer;
  private final DatabaseConnection<NumberVector<?, ?>> databaseConnection;
  private File inputFolder = null;
  private boolean inMemory = false;
  
  /**
   * @param config
   */
  public KnnPerformanceTestSuite(Parameterization config) {
    super(config);

    Log.setLogFormatter(new TraceLevelLogFormatter());
    Log.addLogWriter(new StdOutLogWriter());
    Log.setFilter(LogLevel.DEBUG);
    
    if (config.grab(INPUT_PARAM)) {
      inputFolder = INPUT_PARAM.getValue();
    }
    
    if (config.grab(IN_MEMORY_PARAM)) {
      inMemory = IN_MEMORY_PARAM.getValue();
    }
    
    databaseConnection = FileBasedDatabaseConnection.parameterize(config);
    rocComputer = new ComputeROCCurve<NumberVector<?,?>>(config);
  }

  /* (non-Javadoc)
   * @see de.lmu.ifi.dbs.elki.application.AbstractApplication#run()
   */
  @Override
  public void run() throws UnableToComplyException {
    try {
      Log.info("Starting performance test");
      Log.info();
      Log.info("using inMemory strategy: " + Boolean.toString(inMemory));
      
      Log.info("Reading database ...");
      Database<NumberVector<?, ?>> database = databaseConnection.getDatabase(null);
      File outputFolder = new File(this.inputFolder, "results");
      if (!outputFolder.exists()) {
        outputFolder.mkdirs();
      }
      
      Log.info("Opening result tree ...");
      File resultDirectory = new File(this.inputFolder, "result.dir");
      File resultData = new File(this.inputFolder, "result.dat");
      
      DynamicBPlusTree<Integer, DistanceList> resultTree = new DynamicBPlusTree<Integer, DistanceList>(
          new BufferedDiskBackedDataStorage(resultDirectory),
          (inMemory ? new BufferedDiskBackedDataStorage(resultData) : new DiskBackedDataStorage(resultData)),
          new ConstantSizeIntegerSerializer(),
          new DistanceListSerializer()
      );
      PrecalculatedKnnIndex<NumberVector<?, ?>> index = new PrecalculatedKnnIndex<NumberVector<?, ?>>(resultTree);
      database.addIndex(index);
      
      Log.info("Processing results ...");
      for (PerformanceTest performanceTest : PERFORMANCE_TESTS) {
        AbstractAlgorithm<NumberVector<?, ?>, OutlierResult> algorithm = performanceTest.getAlgorithm();
        String resultDirectoryName = createResultDirectoryName(performanceTest);
        Log.info(String.format("%s (%s) ...", algorithm.getClass().getSimpleName(), resultDirectoryName));
        BasicResult totalResult = new BasicResult("ROC Result", "rocresult");
        
        OutlierResult result = algorithm.run(database);
        rocComputer.processResult(database, result, totalResult.getHierarchy());
        
        File targetDirectory = new File(outputFolder, resultDirectoryName);
        targetDirectory.mkdirs();
        ResultWriter<NumberVector<?, ?>> resultWriter = getResultWriter(targetDirectory);

        for (Result aResult : totalResult.getHierarchy().iterDescendants(result.getOrdering())) {
          resultWriter.processResult(database, aResult);
        }
        
        new File(targetDirectory, "default.txt").delete();
      }
      
      Log.info("done");
      
    } catch (RuntimeException e) {
      throw e;
    } catch (Exception e) {
      throw new UnableToComplyException(e);
    }
  }
  
  private static String createResultDirectoryName(PerformanceTest performanceTest) {
    StringBuilder sb = new StringBuilder();
    
    Class<?> algorithmClass = performanceTest.getAlgorithm().getClass();
    sb.append(algorithmClass.getSimpleName().toLowerCase());
    
    for (Field field : algorithmClass.getDeclaredFields()) {
      if ((Modifier.isProtected(field.getModifiers()) || Modifier.isPublic(field.getModifiers()) || Modifier.isPrivate(field.getModifiers())) &&
          field.getDeclaringClass().equals(algorithmClass) &&
          (field.getType().equals(String.class)
              || field.getType().equals(int.class)
              || Number.class.isAssignableFrom(field.getType()))) {
        try {
          field.setAccessible(true);
          sb.append("_");
          sb.append(field.getName());
          sb.append("-");
          sb.append(field.get(performanceTest.getAlgorithm()));
        }
        catch(IllegalArgumentException e) {
          Log.debug("Can't access field to auto generate folder name", e);
        }
        catch(IllegalAccessException e) {
          Log.debug("Can't access field to auto generate folder name", e);
        }
      }
    }
    
    return sb.toString();
  }
  
  private static ResultWriter<NumberVector<?, ?>> getResultWriter(File targetFile) {
    ListParameterization config = new ListParameterization();
    config.addParameter(OptionID.OUTPUT, targetFile);
    return new ResultWriter<NumberVector<?, ?>>(config);
  }
  
  public static void main(String[] args) {
    StandAloneApplication.runCLIApplication(KnnPerformanceTestSuite.class, args);
  }  

}
