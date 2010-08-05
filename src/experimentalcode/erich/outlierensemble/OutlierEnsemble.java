package experimentalcode.erich.outlierensemble;

import java.util.ArrayList;
import java.util.List;

import de.lmu.ifi.dbs.elki.algorithm.AbstractAlgorithm;
import de.lmu.ifi.dbs.elki.algorithm.Algorithm;
import de.lmu.ifi.dbs.elki.data.DatabaseObject;
import de.lmu.ifi.dbs.elki.database.AssociationID;
import de.lmu.ifi.dbs.elki.database.Database;
import de.lmu.ifi.dbs.elki.database.datastore.DataStoreFactory;
import de.lmu.ifi.dbs.elki.database.datastore.DataStoreUtil;
import de.lmu.ifi.dbs.elki.database.datastore.WritableDataStore;
import de.lmu.ifi.dbs.elki.database.ids.DBID;
import de.lmu.ifi.dbs.elki.logging.Logging;
import de.lmu.ifi.dbs.elki.logging.progress.FiniteProgress;
import de.lmu.ifi.dbs.elki.math.MinMax;
import de.lmu.ifi.dbs.elki.result.AnnotationFromDataStore;
import de.lmu.ifi.dbs.elki.result.AnnotationResult;
import de.lmu.ifi.dbs.elki.result.OrderingFromDataStore;
import de.lmu.ifi.dbs.elki.result.OrderingResult;
import de.lmu.ifi.dbs.elki.result.Result;
import de.lmu.ifi.dbs.elki.result.ResultUtil;
import de.lmu.ifi.dbs.elki.result.outlier.BasicOutlierScoreMeta;
import de.lmu.ifi.dbs.elki.result.outlier.OutlierResult;
import de.lmu.ifi.dbs.elki.result.outlier.OutlierScoreMeta;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.OptionID;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.ChainedParameterization;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.ListParameterization;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.Parameterization;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.ObjectListParameter;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.ObjectParameter;

/**
 * Simple outlier ensemble method.
 * 
 * @author Erich Schubert
 * 
 * @param <O> object type
 */
public class OutlierEnsemble<O extends DatabaseObject> extends AbstractAlgorithm<O, OutlierResult> {
  /**
   * The logger for this class.
   */
  private static final Logging logger = Logging.getLogger(OutlierEnsemble.class);
  
  /**
   * Parameter for the individual algorithms
   */
  private ObjectListParameter<Algorithm<O, ?>> ALGORITHMS_PARAM = new ObjectListParameter<Algorithm<O, ?>>(OptionID.ALGORITHM, Algorithm.class);

  /**
   * The actual algorithms
   */
  private List<Algorithm<O, ?>> algorithms;

  /**
   * Voting strategy to use in the ensemble.
   */
  public final static OptionID VOTING_ID = OptionID.getOrCreateOptionID("ensemble.voting", "Voting strategy to use in the ensemble.");

  /**
   * Voting strategy parameter
   */
  private ObjectParameter<EnsembleVoting> VOTING_PARAM = new ObjectParameter<EnsembleVoting>(VOTING_ID, EnsembleVoting.class);

  /**
   * The voting in use.
   */
  private EnsembleVoting voting;

  /**
   * Feature bagging result ID
   */
  public static final AssociationID<Double> OUTLIERENSEMBLE_ID = AssociationID.getOrCreateAssociationID("ensemble-score", Double.class);

  /**
   * Constructor, adhering to
   * {@link de.lmu.ifi.dbs.elki.utilities.optionhandling.Parameterizable}
   * 
   * @param config Parameterization
   */
  public OutlierEnsemble(Parameterization config) {
    super();
    config = config.descend(this);
    if(config.grab(ALGORITHMS_PARAM)) {
      ListParameterization subconfig = new ListParameterization();
      ChainedParameterization chain = new ChainedParameterization(subconfig, config);
      chain.errorsTo(config);
      algorithms = ALGORITHMS_PARAM.instantiateClasses(chain);
      subconfig.logAndClearReportedErrors();
    }
    if(config.grab(VOTING_PARAM)) {
      voting = VOTING_PARAM.instantiateClass(config);
    }
  }

  @Override
  protected OutlierResult runInTime(Database<O> database) throws IllegalStateException {
    int num = algorithms.size();
    // Run inner outlier algorithms
    ArrayList<OutlierResult> results = new ArrayList<OutlierResult>(num);
    {
      FiniteProgress prog = logger.isVerbose() ? new FiniteProgress("Inner outlier algorithms", num, logger) : null;
      for(Algorithm<O, ?> alg : algorithms) {
        Result res = alg.run(database);
        for(OutlierResult ors : ResultUtil.getOutlierResults(res)) {
          results.add(ors);
        }
        if(prog != null) {
          prog.incrementProcessed(logger);
        }
      }
      if(prog != null) {
        prog.ensureCompleted(logger);
      }
    }
    // Combine
    WritableDataStore<Double> sumscore = DataStoreUtil.makeStorage(database.getIDs(), DataStoreFactory.HINT_STATIC, Double.class);
    MinMax<Double> minmax = new MinMax<Double>();
    {
      FiniteProgress cprog = logger.isVerbose() ? new FiniteProgress("Combining results", database.size(), logger) : null;
      for(DBID id : database) {
        ArrayList<Double> scores = new ArrayList<Double>(num);
        for(OutlierResult r : results) {
          Double score = r.getScores().getValueFor(id);
          if(score != null) {
            scores.add(score);
          }
          else {
            logger.warning("DBID " + id + " was not given a score by result " + r);
          }
        }
        if(scores.size() > 0) {
          double combined = voting.combine(scores);
          sumscore.put(id, combined);
          minmax.put(combined);
        }
        else {
          logger.warning("DBID " + id + " was not given any score at all.");
        }
        if(cprog != null) {
          cprog.incrementProcessed(logger);
        }
      }
      if(cprog != null) {
        cprog.ensureCompleted(logger);
      }
    }
    OutlierScoreMeta meta = new BasicOutlierScoreMeta(minmax.getMin(), minmax.getMax());
    AnnotationResult<Double> scores = new AnnotationFromDataStore<Double>(OUTLIERENSEMBLE_ID, sumscore);
    OrderingResult ordering = new OrderingFromDataStore<Double>(sumscore, true);
    return new OutlierResult(meta, scores, ordering);
  }

  @Override
  protected Logging getLogger() {
    return logger;
  }
}