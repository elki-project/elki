package experimentalcode.students.frankenb;

import java.io.File;
import java.util.Iterator;

import de.lmu.ifi.dbs.elki.algorithm.outlier.OutlierAlgorithm;
import de.lmu.ifi.dbs.elki.algorithm.outlier.lof.LOF;
import de.lmu.ifi.dbs.elki.application.AbstractApplication;
import de.lmu.ifi.dbs.elki.data.NumberVector;
import de.lmu.ifi.dbs.elki.data.type.TypeUtil;
import de.lmu.ifi.dbs.elki.database.Database;
import de.lmu.ifi.dbs.elki.database.HashmapDatabase;
import de.lmu.ifi.dbs.elki.database.relation.Relation;
import de.lmu.ifi.dbs.elki.distance.distancefunction.minkowski.EuclideanDistanceFunction;
import de.lmu.ifi.dbs.elki.evaluation.outlier.OutlierROCCurve;
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
public class PINN<V extends NumberVector<?>> extends AbstractApplication {
  /**
   * The logger
   */
  private static final Logging LOG = Logging.getLogger(PINN.class);

  private final Database database;

  private final RandomProjection<V> randomProjection;

  private final OutlierROCCurve rocComputer;

  private final int k;

  private final int kFactor;

  private final File outputFile;

  /**
   * Constructor.
   * 
   * @param database
   * @param randomProjection
   * @param rocComputer
   * @param k
   * @param kFactor
   * @param outputFile
   */
  public PINN(Database database, RandomProjection<V> randomProjection, OutlierROCCurve rocComputer, int k, int kFactor, File outputFile) {
    super();
    this.database = database;
    this.randomProjection = randomProjection;
    this.rocComputer = rocComputer;
    this.k = k;
    this.kFactor = kFactor;
    this.outputFile = outputFile;
  }

  @Override
  public void run() throws UnableToComplyException {
    LOG.verbose("Reading database ...");
    database.initialize();
    Relation<V> dataset = database.getRelation(TypeUtil.NUMBER_VECTOR_FIELD);

    LOG.verbose("Projecting ...");
    Relation<V> dataSet = randomProjection.project(dataset);

    LOG.verbose("Creating KD-Tree ...");
    KDTree<V> tree = new KDTree<>(dataSet);

    PINNKnnIndex<V> index = new PINNKnnIndex<>(dataset, tree, kFactor);
    database.addIndex(index);

    LOG.verbose("Running LOF ...");

    OutlierAlgorithm algorithm = new LOF<>(this.k, EuclideanDistanceFunction.STATIC, EuclideanDistanceFunction.STATIC);
    OutlierResult result = algorithm.run(database);

    LOG.verbose("Calculating ROC ...");

    BasicResult totalResult = new BasicResult("ROC Result", "rocresult");
    rocComputer.processNewResult(database, result);

    this.outputFile.mkdirs();
    ResultWriter resultWriter = getResultWriter(this.outputFile);

    Iterator<Result> iter = totalResult.getHierarchy().iterDescendants(result);
    while(iter.hasNext()) {
      Result aResult = iter.next();
      resultWriter.processNewResult(database, aResult);
    }
    LOG.verbose("Writing results to dir " + this.outputFile);

    LOG.verbose("Done.");
  }

  private static ResultWriter getResultWriter(File targetFile) {
    return new ResultWriter(targetFile, false, false);
  }

  public static void main(String[] args) {
    runCLIApplication(PINN.class, args);
  }

  /**
   * Parameterization class.
   * 
   * @author Erich Schubert
   * 
   * @apiviz.exclude
   */
  public static class Parameterizer<V extends NumberVector<?>> extends AbstractApplication.Parameterizer {
    public static final OptionID K_ID = new OptionID("k", "k for kNN search");

    public static final OptionID K_FACTOR_ID = new OptionID("kfactor", "factor to multiply with k when searching for the neighborhood in the projected space");

    private Database database;

    private RandomProjection<V> randomProjection;

    private OutlierROCCurve rocComputer;

    private int k;

    private int kFactor;

    private File outputFile;

    @Override
    protected void makeOptions(Parameterization config) {
      super.makeOptions(config);
      outputFile = getParameterOutputFile(config);

      IntParameter kP = new IntParameter(K_ID);
      if(config.grab(kP)) {
        this.k = kP.getValue();
      }

      IntParameter kFactorP = new IntParameter(K_FACTOR_ID);
      if(config.grab(kFactorP)) {
        this.kFactor = kFactorP.getValue();
      }

      randomProjection = new RandomProjection<>(config);

      database = config.tryInstantiate(HashmapDatabase.class);
      rocComputer = config.tryInstantiate(OutlierROCCurve.class);
    }

    @Override
    protected PINN<V> makeInstance() {
      return new PINN<>(database, randomProjection, rocComputer, k, kFactor, outputFile);
    }
  }
}
