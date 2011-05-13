package experimentalcode.frankenb.main;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

import de.lmu.ifi.dbs.elki.algorithm.outlier.KNNOutlier;
import de.lmu.ifi.dbs.elki.algorithm.outlier.KNNWeightOutlier;
import de.lmu.ifi.dbs.elki.algorithm.outlier.LDOF;
import de.lmu.ifi.dbs.elki.algorithm.outlier.LOF;
import de.lmu.ifi.dbs.elki.algorithm.outlier.LoOP;
import de.lmu.ifi.dbs.elki.algorithm.outlier.OutlierAlgorithm;
import de.lmu.ifi.dbs.elki.application.AbstractApplication;
import de.lmu.ifi.dbs.elki.data.NumberVector;
import de.lmu.ifi.dbs.elki.database.Database;
import de.lmu.ifi.dbs.elki.database.relation.Relation;
import de.lmu.ifi.dbs.elki.datasource.DatabaseConnection;
import de.lmu.ifi.dbs.elki.datasource.FileBasedDatabaseConnection;
import de.lmu.ifi.dbs.elki.distance.distancefunction.EuclideanDistanceFunction;
import de.lmu.ifi.dbs.elki.distance.distancevalue.DoubleDistance;
import de.lmu.ifi.dbs.elki.evaluation.roc.ComputeROCCurve;
import de.lmu.ifi.dbs.elki.logging.Logging;
import de.lmu.ifi.dbs.elki.persistent.ByteArrayUtil;
import de.lmu.ifi.dbs.elki.result.BasicResult;
import de.lmu.ifi.dbs.elki.result.Result;
import de.lmu.ifi.dbs.elki.result.ResultWriter;
import de.lmu.ifi.dbs.elki.result.outlier.OutlierResult;
import de.lmu.ifi.dbs.elki.utilities.ClassGenericsUtil;
import de.lmu.ifi.dbs.elki.utilities.exceptions.UnableToComplyException;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.OptionID;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.Parameterization;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.FileParameter;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.Flag;
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
 * <li>LOF</li>
 * <li>...</li>
 * </ul>
 * 
 * @author Florian Frankenberger
 */
public class KnnPerformanceTestSuite extends AbstractApplication {
  /**
   * The logger
   */
  private static final Logging logger = Logging.getLogger(KnnPerformanceTestSuite.class);

  private static final PerformanceTest[] PERFORMANCE_TESTS = new PerformanceTest[] {
      // LOF
  new PerformanceTest(new LOF<NumberVector<?, ?>, DoubleDistance>(10, EuclideanDistanceFunction.STATIC, EuclideanDistanceFunction.STATIC)), new PerformanceTest(new LOF<NumberVector<?, ?>, DoubleDistance>(20, EuclideanDistanceFunction.STATIC, EuclideanDistanceFunction.STATIC)), new PerformanceTest(new LOF<NumberVector<?, ?>, DoubleDistance>(45, EuclideanDistanceFunction.STATIC, EuclideanDistanceFunction.STATIC)),

      // new PerformanceTest(new LOF<NumberVector<?, ?>, DoubleDistance>(5,
      // EuclideanDistanceFunction.STATIC, EuclideanDistanceFunction.STATIC)),
      // new PerformanceTest(new LOF<NumberVector<?, ?>, DoubleDistance>(15,
      // EuclideanDistanceFunction.STATIC, EuclideanDistanceFunction.STATIC)),
      // new PerformanceTest(new LOF<NumberVector<?, ?>, DoubleDistance>(30,
      // EuclideanDistanceFunction.STATIC, EuclideanDistanceFunction.STATIC)),
      // new PerformanceTest(new LOF<NumberVector<?, ?>, DoubleDistance>(60,
      // EuclideanDistanceFunction.STATIC, EuclideanDistanceFunction.STATIC)),

      // LoOP
  new PerformanceTest(new LoOP<NumberVector<?, ?>, DoubleDistance>(10, 10, EuclideanDistanceFunction.STATIC, EuclideanDistanceFunction.STATIC, 3)), new PerformanceTest(new LoOP<NumberVector<?, ?>, DoubleDistance>(20, 20, EuclideanDistanceFunction.STATIC, EuclideanDistanceFunction.STATIC, 3)), new PerformanceTest(new LoOP<NumberVector<?, ?>, DoubleDistance>(45, 45, EuclideanDistanceFunction.STATIC, EuclideanDistanceFunction.STATIC, 3)),

      // new PerformanceTest(new LoOP<NumberVector<?,?>, DoubleDistance>(5, 5,
      // EuclideanDistanceFunction.STATIC, EuclideanDistanceFunction.STATIC,
      // 3)),
      // new PerformanceTest(new LoOP<NumberVector<?,?>, DoubleDistance>(15, 15,
      // EuclideanDistanceFunction.STATIC, EuclideanDistanceFunction.STATIC,
      // 3)),
      // new PerformanceTest(new LoOP<NumberVector<?,?>, DoubleDistance>(30, 30,
      // EuclideanDistanceFunction.STATIC, EuclideanDistanceFunction.STATIC,
      // 3)),
      // new PerformanceTest(new LoOP<NumberVector<?,?>, DoubleDistance>(60, 60,
      // EuclideanDistanceFunction.STATIC, EuclideanDistanceFunction.STATIC,
      // 3)),

      // KNN Outlier
  new PerformanceTest(new KNNOutlier<NumberVector<?, ?>, DoubleDistance>(EuclideanDistanceFunction.STATIC, 10)), new PerformanceTest(new KNNOutlier<NumberVector<?, ?>, DoubleDistance>(EuclideanDistanceFunction.STATIC, 20)), new PerformanceTest(new KNNOutlier<NumberVector<?, ?>, DoubleDistance>(EuclideanDistanceFunction.STATIC, 45)),

      // new PerformanceTest(new KNNOutlier<NumberVector<?,?>,
      // DoubleDistance>(EuclideanDistanceFunction.STATIC, 5)),
      // new PerformanceTest(new KNNOutlier<NumberVector<?,?>,
      // DoubleDistance>(EuclideanDistanceFunction.STATIC, 15)),
      // new PerformanceTest(new KNNOutlier<NumberVector<?,?>,
      // DoubleDistance>(EuclideanDistanceFunction.STATIC, 30)),
      // new PerformanceTest(new KNNOutlier<NumberVector<?,?>,
      // DoubleDistance>(EuclideanDistanceFunction.STATIC, 60)),
      //
  // KNN Weighted
  new PerformanceTest(new KNNWeightOutlier<NumberVector<?, ?>, DoubleDistance>(EuclideanDistanceFunction.STATIC, 10)), new PerformanceTest(new KNNWeightOutlier<NumberVector<?, ?>, DoubleDistance>(EuclideanDistanceFunction.STATIC, 20)), new PerformanceTest(new KNNWeightOutlier<NumberVector<?, ?>, DoubleDistance>(EuclideanDistanceFunction.STATIC, 45)),

      // LDOF
  new PerformanceTest(new LDOF<NumberVector<?, ?>, DoubleDistance>(EuclideanDistanceFunction.STATIC, 10)), new PerformanceTest(new LDOF<NumberVector<?, ?>, DoubleDistance>(EuclideanDistanceFunction.STATIC, 20)), new PerformanceTest(new LDOF<NumberVector<?, ?>, DoubleDistance>(EuclideanDistanceFunction.STATIC, 45)), };

  private static class PerformanceTest {
    private final OutlierAlgorithm algorithm;

    public PerformanceTest(OutlierAlgorithm algorithm) {
      this.algorithm = algorithm;
    }

    public OutlierAlgorithm getAlgorithm() {
      return this.algorithm;
    }
  }

  public static final OptionID IN_MEMORY_ID = OptionID.getOrCreateOptionID("inmemory", "tells wether the resulting tree data should be buffered in memory or not. This can increase performance but can also lead to OutOfMemoryExceptions!");

  public static final OptionID INPUT_ID = OptionID.getOrCreateOptionID("app.in", "");

  private final ComputeROCCurve rocComputer;

  private final DatabaseConnection databaseConnection;

  private File inputFolder = null;

  private boolean inMemory = false;

  public KnnPerformanceTestSuite(boolean verbose, ComputeROCCurve rocComputer, DatabaseConnection databaseConnection, File inputFolder, boolean inMemory) {
    super(verbose);
    this.rocComputer = rocComputer;
    this.databaseConnection = databaseConnection;
    this.inputFolder = inputFolder;
    this.inMemory = inMemory;
  }

  @Override
  public void run() throws UnableToComplyException {
    try {
      logger.verbose("Starting performance test");
      logger.verbose("using inMemory strategy: " + Boolean.toString(inMemory));

      logger.verbose("Reading database ...");
      Database database = databaseConnection.getDatabase();

      List<File> resultDirectories = findResultDirectories(this.inputFolder);
      for(File resultDirectory : resultDirectories) {
        logger.verbose("Result in " + resultDirectory + " ...");
        runTestSuiteFor(database, resultDirectory);
      }

      logger.verbose("All done.");

    }
    catch(RuntimeException e) {
      throw e;
    }
    catch(Exception e) {
      throw new UnableToComplyException(e);
    }
  }

  private void runTestSuiteFor(Database database, File inputFolder) throws IOException {
    logger.verbose("Opening result tree ...");
    File resultDirectory = new File(inputFolder, "result.dir");
    File resultData = new File(inputFolder, "result.dat");

    DynamicBPlusTree<Integer, DistanceList> resultTree = new DynamicBPlusTree<Integer, DistanceList>(new BufferedDiskBackedDataStorage(resultDirectory), (inMemory ? new BufferedDiskBackedDataStorage(resultData) : new DiskBackedDataStorage(resultData)), ByteArrayUtil.INT_SERIALIZER, new DistanceListSerializer());

    Relation<NumberVector<?, ?>> relation = null;
    PrecalculatedKnnIndex<NumberVector<?, ?>> index = new PrecalculatedKnnIndex<NumberVector<?, ?>>(relation, resultTree);
    database.addIndex(index);

    File outputFolder = new File(inputFolder, "results");
    if(!outputFolder.exists()) {
      outputFolder.mkdirs();
    }

    logger.verbose("Processing results ...");
    for(PerformanceTest performanceTest : PERFORMANCE_TESTS) {
      OutlierAlgorithm algorithm = performanceTest.getAlgorithm();
      String targetFileName = createResultFileName(performanceTest, outputFolder);
      logger.verbose(String.format("%s (%s) ...", algorithm.getClass().getSimpleName(), targetFileName));

      File targetFile = new File(outputFolder, targetFileName);
      if(targetFile.exists()) {
        logger.verbose("\tAlready processed - so skipping.");
        continue;
      }

      File tmpDirectory = new File(outputFolder, "tmp");
      BasicResult totalResult = new BasicResult("ROC Result", "rocresult");

      OutlierResult result = algorithm.run(database);
      rocComputer.processResult(database, result);

      tmpDirectory.mkdirs();
      ResultWriter resultWriter = getResultWriter(tmpDirectory);

      for(Result aResult : totalResult.getHierarchy().iterDescendants(result)) {
        resultWriter.processResult(database, aResult);
      }
      logger.verbose("Writing results to file " + targetFile);

      new File(tmpDirectory, "default.txt").delete();
      File resultFile = new File(tmpDirectory, "roc.txt");
      resultFile.renameTo(targetFile);

      tmpDirectory.deleteOnExit();
    }
  }

  private static String createResultFileName(PerformanceTest performanceTest, File outputDirectory) {
    StringBuilder sb = new StringBuilder();
    sb.append("roc_");

    sb.append(outputDirectory.getParentFile().getName());
    sb.append("_");

    Class<?> algorithmClass = performanceTest.getAlgorithm().getClass();
    sb.append(algorithmClass.getSimpleName().toLowerCase());

    for(Field field : algorithmClass.getDeclaredFields()) {
      if(field.getDeclaringClass().equals(algorithmClass) && (field.getType().equals(String.class) || field.getType().equals(int.class) || Number.class.isAssignableFrom(field.getType()))) {
        try {
          field.setAccessible(true);
          sb.append("_");
          sb.append(field.getName());
          sb.append("-");
          sb.append(field.get(performanceTest.getAlgorithm()));
        }
        catch(IllegalArgumentException e) {
          logger.debug("Can't access field to auto generate folder name", e);
        }
        catch(IllegalAccessException e) {
          logger.debug("Can't access field to auto generate folder name", e);
        }
      }
    }

    sb.append(".txt");
    return sb.toString();
  }

  private static ResultWriter getResultWriter(File targetFile) {
    return new ResultWriter(targetFile, false, false);
  }

  private static List<File> findResultDirectories(File dir) {
    List<File> result = new ArrayList<File>();
    for(File file : dir.listFiles()) {
      if(file.equals(dir) || file.equals(dir.getParentFile())) {
        continue;
      }
      if(file.isDirectory()) {
        result.addAll(findResultDirectories(file));
      }
      else {
        if(file.getName().equalsIgnoreCase("result.dir") && new File(dir, "result.dat").exists()) {
          result.add(dir);
        }
      }
    }
    return result;
  }

  public static void main(String[] args) {
    AbstractApplication.runCLIApplication(KnnPerformanceTestSuite.class, args);
  }

  /**
   * Parameterization class.
   * 
   * @author Erich Schubert
   * 
   * @apiviz.exclude
   */
  public static class Parameterizer extends AbstractApplication.Parameterizer {
    ComputeROCCurve rocComputer;

    FileBasedDatabaseConnection databaseConnection;

    File inputFolder = null;

    boolean inMemory = false;

    @Override
    protected void makeOptions(Parameterization config) {
      super.makeOptions(config);
      FileParameter INPUT_PARAM = new FileParameter(INPUT_ID, FileParameter.FileType.INPUT_FILE);
      if(config.grab(INPUT_PARAM)) {
        inputFolder = INPUT_PARAM.getValue();
      }

      Flag IN_MEMORY_PARAM = new Flag(IN_MEMORY_ID);
      if(config.grab(IN_MEMORY_PARAM)) {
        inMemory = IN_MEMORY_PARAM.getValue();
      }

      Class<FileBasedDatabaseConnection> dbcls = ClassGenericsUtil.uglyCastIntoSubclass(FileBasedDatabaseConnection.class);
      databaseConnection = config.tryInstantiate(dbcls);
      rocComputer = config.tryInstantiate(ComputeROCCurve.class);
    }

    @Override
    protected KnnPerformanceTestSuite makeInstance() {
      return new KnnPerformanceTestSuite(verbose, rocComputer, databaseConnection, inputFolder, inMemory);
    }
  }
}