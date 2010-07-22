package de.lmu.ifi.dbs.elki.algorithm.outlier;

import java.util.List;

import de.lmu.ifi.dbs.elki.algorithm.AbstractDistanceBasedAlgorithm;
import de.lmu.ifi.dbs.elki.data.DatabaseObject;
import de.lmu.ifi.dbs.elki.database.AssociationID;
import de.lmu.ifi.dbs.elki.database.Database;
import de.lmu.ifi.dbs.elki.database.DistanceResultPair;
import de.lmu.ifi.dbs.elki.database.datastore.DataStoreFactory;
import de.lmu.ifi.dbs.elki.database.datastore.DataStoreUtil;
import de.lmu.ifi.dbs.elki.database.datastore.WritableDataStore;
import de.lmu.ifi.dbs.elki.database.ids.DBID;
import de.lmu.ifi.dbs.elki.database.query.DistanceQuery;
import de.lmu.ifi.dbs.elki.database.query.KNNQuery;
import de.lmu.ifi.dbs.elki.database.query.PreprocessorKNNQuery;
import de.lmu.ifi.dbs.elki.distance.distancevalue.DoubleDistance;
import de.lmu.ifi.dbs.elki.distance.distancevalue.NumberDistance;
import de.lmu.ifi.dbs.elki.logging.progress.FiniteProgress;
import de.lmu.ifi.dbs.elki.math.MinMax;
import de.lmu.ifi.dbs.elki.preprocessing.MaterializeKNNPreprocessor;
import de.lmu.ifi.dbs.elki.result.AnnotationFromDataStore;
import de.lmu.ifi.dbs.elki.result.AnnotationResult;
import de.lmu.ifi.dbs.elki.result.OrderingFromDataStore;
import de.lmu.ifi.dbs.elki.result.OrderingResult;
import de.lmu.ifi.dbs.elki.result.outlier.OutlierResult;
import de.lmu.ifi.dbs.elki.result.outlier.OutlierScoreMeta;
import de.lmu.ifi.dbs.elki.result.outlier.QuotientOutlierScoreMeta;
import de.lmu.ifi.dbs.elki.utilities.documentation.Description;
import de.lmu.ifi.dbs.elki.utilities.documentation.Reference;
import de.lmu.ifi.dbs.elki.utilities.documentation.Title;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.OptionID;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.constraints.GreaterConstraint;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.ChainedParameterization;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.ListParameterization;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.Parameterization;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.ClassParameter;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.IntParameter;

/**
 * <p>
 * Computes the LDOF (Local Distance-Based Outlier Factor) for all objects of a
 * Database.
 * </p>
 * 
 * <p>
 * Reference:<br>
 * K. Zhang, M. Hutter, H. Jin: A New Local Distance-Based Outlier Detection
 * Approach for Scattered Real-World Data.<br>
 * In: Proc. 13th Pacific-Asia Conference on Advances in Knowledge Discovery and
 * Data Mining (PAKDD 2009), Bangkok, Thailand, 2009.
 * </p>
 * 
 * @author Arthur Zimek
 * @param <O> the type of DatabaseObjects handled by this Algorithm
 */
@Title("LDOF: Local Distance-Based Outlier Factor")
@Description("Local outlier detection appraoch suitable for scattered data by averaging the kNN distance over all k nearest neighbors")
@Reference(authors = "K. Zhang, M. Hutter, H. Jin", title = "A New Local Distance-Based Outlier Detection Approach for Scattered Real-World Data", booktitle = "Proc. 13th Pacific-Asia Conference on Advances in Knowledge Discovery and Data Mining (PAKDD 2009), Bangkok, Thailand, 2009", url = "http://dx.doi.org/10.1007/978-3-642-01307-2_84")
public class LDOF<O extends DatabaseObject, D extends NumberDistance<D, ?>> extends AbstractDistanceBasedAlgorithm<O, D, OutlierResult> {
  /**
   * The baseline for LDOF values. The paper gives 0.5 for uniform
   * distributions, although one might also discuss using 1.0 as baseline.
   */
  private static final double LDOF_BASELINE = 0.5;

  /**
   * The association id to associate the LDOF_SCORE of an object for the LDOF
   * algorithm.
   */
  public static final AssociationID<Double> LDOF_SCORE = AssociationID.getOrCreateAssociationID("ldof", Double.class);

  /**
   * OptionID for {@link #K_PARAM}
   */
  public static final OptionID K_ID = OptionID.getOrCreateOptionID("ldof.k", "The number of nearest neighbors of an object to be considered for computing its LDOF_SCORE.");

  /**
   * Parameter to specify the number of nearest neighbors of an object to be
   * considered for computing its LDOF_SCORE, must be an integer greater than 1.
   * <p>
   * Key: {@code -ldof.k}
   * </p>
   */
  private final IntParameter K_PARAM = new IntParameter(K_ID, new GreaterConstraint(1));

  /**
   * OptionID for {@link #KNNQUERY_PARAM}
   */
  public static final OptionID KNNQUERY_ID = OptionID.getOrCreateOptionID("ldof.knnquery", "kNN query to use");

  /**
   * The preprocessor used to materialize the kNN neighborhoods.
   * 
   * Default value: {@link MaterializeKNNPreprocessor} </p>
   * <p>
   * Key: {@code -ldof.knnquery}
   * </p>
   */
  private final ClassParameter<KNNQuery<O, DoubleDistance>> KNNQUERY_PARAM = new ClassParameter<KNNQuery<O, DoubleDistance>>(KNNQUERY_ID, KNNQuery.class, PreprocessorKNNQuery.class);

  /**
   * Holds the value of {@link #K_PARAM}.
   */
  int k;

  /**
   * Preprocessor Step 1
   */
  protected KNNQuery<O, DoubleDistance> knnQuery;

  /**
   * Constructor, adhering to
   * {@link de.lmu.ifi.dbs.elki.utilities.optionhandling.Parameterizable}
   * 
   * @param config Parameterization
   */
  public LDOF(Parameterization config) {
    super(config);
    if(config.grab(K_PARAM)) {
      k = K_PARAM.getValue();
    }

    // configure kNN query
    if(config.grab(KNNQUERY_PARAM) && DISTANCE_FUNCTION_PARAM.isDefined()) {
      ListParameterization knnParams = new ListParameterization();
      knnParams.addParameter(KNNQuery.K_ID, (k + 1));
      knnParams.addParameter(KNNQuery.DISTANCE_FUNCTION_ID, getDistanceFunction());
      ChainedParameterization chain = new ChainedParameterization(knnParams, config);
      chain.errorsTo(config);
      knnQuery = KNNQUERY_PARAM.instantiateClass(chain);
      knnParams.reportInternalParameterizationErrors(config);
    }
  }

  @Override
  protected OutlierResult runInTime(Database<O> database) throws IllegalStateException {
    DistanceQuery<O, D> distFunc = getDistanceQuery(database);
    // materialize neighborhoods
    if(this.isVerbose()) {
      this.verbose("Materializing k nearest neighborhoods.");
    }
    knnQuery.setDatabase(database);

    // track the maximum value for normalization
    MinMax<Double> ldofminmax = new MinMax<Double>();
    // compute the ldof values
    WritableDataStore<Double> ldofs = DataStoreUtil.makeStorage(database.getIDs(), DataStoreFactory.HINT_HOT | DataStoreFactory.HINT_TEMP, Double.class);
    // compute LOF_SCORE of each db object
    if(this.isVerbose()) {
      this.verbose("computing LDOFs");
    }

    FiniteProgress progressLDOFs = logger.isVerbose() ? new FiniteProgress("LDOF_SCORE for objects", database.size(), logger) : null;
    int counter = 0;
    for(DBID id : database) {
      counter++;
      List<DistanceResultPair<DoubleDistance>> neighbors = knnQuery.get(id);
      int nsize = neighbors.size() - 1;
      // skip the point itself
      double dxp = 0;
      double Dxp = 0;
      for(DistanceResultPair<DoubleDistance> neighbor1 : neighbors) {
        if(neighbor1.getID() != id) {
          dxp += neighbor1.getDistance().doubleValue();
          for(DistanceResultPair<DoubleDistance> neighbor2 : neighbors) {
            if(neighbor1.getID() != neighbor2.getID() && neighbor2.getID() != id) {
              Dxp += distFunc.distance(neighbor1.getID(), neighbor2.getID()).doubleValue();
            }
          }
        }
      }
      dxp /= nsize;
      Dxp /= (nsize * (nsize - 1));
      Double ldof = dxp / Dxp;
      if (ldof.isNaN() || ldof.isInfinite()) {
        ldof = 1.0;
      }
      ldofs.put(id, ldof);
      // update maximum
      ldofminmax.put(ldof);

      if(progressLDOFs != null) {
        progressLDOFs.setProcessed(counter, logger);
      }
    }
    if(progressLDOFs != null) {
      progressLDOFs.ensureCompleted(logger);
    }

    // Build result representation.
    AnnotationResult<Double> scoreResult = new AnnotationFromDataStore<Double>(LDOF_SCORE, ldofs);
    OrderingResult orderingResult = new OrderingFromDataStore<Double>(ldofs, true);
    OutlierScoreMeta scoreMeta = new QuotientOutlierScoreMeta(ldofminmax.getMin(), ldofminmax.getMax(), 0.0, Double.POSITIVE_INFINITY, LDOF_BASELINE);
    return new OutlierResult(scoreMeta, scoreResult, orderingResult);
  }
}