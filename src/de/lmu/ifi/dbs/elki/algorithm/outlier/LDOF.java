package de.lmu.ifi.dbs.elki.algorithm.outlier;

import java.util.HashMap;
import java.util.List;

import de.lmu.ifi.dbs.elki.algorithm.DistanceBasedAlgorithm;
import de.lmu.ifi.dbs.elki.data.DatabaseObject;
import de.lmu.ifi.dbs.elki.database.AssociationID;
import de.lmu.ifi.dbs.elki.database.Database;
import de.lmu.ifi.dbs.elki.database.DistanceResultPair;
import de.lmu.ifi.dbs.elki.distance.DoubleDistance;
import de.lmu.ifi.dbs.elki.logging.progress.FiniteProgress;
import de.lmu.ifi.dbs.elki.math.MinMax;
import de.lmu.ifi.dbs.elki.preprocessing.MaterializeKNNPreprocessor;
import de.lmu.ifi.dbs.elki.result.AnnotationFromHashMap;
import de.lmu.ifi.dbs.elki.result.AnnotationResult;
import de.lmu.ifi.dbs.elki.result.MultiResult;
import de.lmu.ifi.dbs.elki.result.OrderingFromHashMap;
import de.lmu.ifi.dbs.elki.result.OrderingResult;
import de.lmu.ifi.dbs.elki.result.outlier.OutlierResult;
import de.lmu.ifi.dbs.elki.result.outlier.OutlierScoreMeta;
import de.lmu.ifi.dbs.elki.result.outlier.QuotientOutlierScoreMeta;
import de.lmu.ifi.dbs.elki.utilities.Description;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.OptionID;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.constraints.GreaterConstraint;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.ChainedParameterization;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.ListParameterization;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.Parameterization;
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
public class LDOF<O extends DatabaseObject> extends DistanceBasedAlgorithm<O, DoubleDistance, MultiResult> {
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
   * Holds the value of {@link #K_PARAM}.
   */
  int k;

  /**
   * Preprocessor for materialization of kNN queries.
   */
  MaterializeKNNPreprocessor<O, DoubleDistance> knnPreprocessor;

  /**
   * Provides the result of the algorithm.
   */
  MultiResult result;

  /**
   * Provides the LDOF algorithm.
   * 
   * Sets parameter {@link #K_PARAM} and initializes the
   * {@link #knnPreprocessor}.
   */
  public LDOF(Parameterization config) {
    super(config);
    if (config.grab(K_PARAM)) {
      k = K_PARAM.getValue();
    }

    // configure preprocessor
    ListParameterization preprocParams1 = new ListParameterization();
    preprocParams1.addParameter(MaterializeKNNPreprocessor.K_ID, (k + 1));
    preprocParams1.addParameter(MaterializeKNNPreprocessor.DISTANCE_FUNCTION_ID, getDistanceFunction());
    ChainedParameterization chain = new ChainedParameterization(preprocParams1, config);
    chain.errorsTo(config);
    knnPreprocessor = new MaterializeKNNPreprocessor<O, DoubleDistance>(chain);
    preprocParams1.reportInternalParameterizationErrors(config);
  }

  /**
   * @see de.lmu.ifi.dbs.elki.algorithm.AbstractAlgorithm#runInTime(de.lmu.ifi.dbs.elki.database.Database)
   */
  @Override
  protected MultiResult runInTime(Database<O> database) throws IllegalStateException {
    getDistanceFunction().setDatabase(database, isVerbose(), isTime());
    // materialize neighborhoods
    if(this.isVerbose()) {
      this.verbose("Materializing k nearest neighborhoods.");
    }
    knnPreprocessor.run(database, isVerbose(), isTime());
    HashMap<Integer, List<DistanceResultPair<DoubleDistance>>> kNearestNeighboorhoods = knnPreprocessor.getMaterialized();

    // track the maximum value for normalization
    MinMax<Double> ldofminmax = new MinMax<Double>();
    // compute the ldof values
    HashMap<Integer, Double> ldofs = new HashMap<Integer, Double>();
    // compute LOF_SCORE of each db object
    if(this.isVerbose()) {
      this.verbose("computing LDOFs");
    }

    FiniteProgress progressLDOFs = new FiniteProgress("LDOF_SCORE for objects", database.size());
    int counter = 0;
    for(Integer id : database) {
      counter++;
      List<DistanceResultPair<DoubleDistance>> neighbors = kNearestNeighboorhoods.get(id);
      int nsize = neighbors.size() - 1;
      // skip the point itself
      double dxp = 0;
      double Dxp = 0;
      for(DistanceResultPair<DoubleDistance> neighbor1 : neighbors) {
        if(neighbor1.getID() != id) {
          dxp += neighbor1.getDistance().doubleValue();
          for(DistanceResultPair<DoubleDistance> neighbor2 : neighbors) {
            if(neighbor1.getID() != neighbor2.getID() && neighbor2.getID() != id) {
              Dxp += getDistanceFunction().distance(neighbor1.getID(), neighbor2.getID()).doubleValue();
            }
          }
        }
      }
      dxp /= nsize;
      Dxp /= (nsize * (nsize - 1));
      Double ldof = dxp / Dxp;
      ldofs.put(id, ldof);
      // update maximum
      ldofminmax.put(ldof);

      if(this.isVerbose()) {
        progressLDOFs.setProcessed(counter);
        this.progress(progressLDOFs);
      }
    }
    if(this.isVerbose()) {
      this.verbose("LDOF finished");
    }

    // Build result representation.
    AnnotationResult<Double> scoreResult = new AnnotationFromHashMap<Double>(LDOF_SCORE, ldofs);
    OrderingResult orderingResult = new OrderingFromHashMap<Double>(ldofs, true);
    OutlierScoreMeta scoreMeta = new QuotientOutlierScoreMeta(ldofminmax.getMin(), ldofminmax.getMax(), 0.0, Double.POSITIVE_INFINITY, LDOF_BASELINE);
    this.result = new OutlierResult(scoreMeta, scoreResult, orderingResult);
    return this.result;
  }

  /**
   * @see de.lmu.ifi.dbs.elki.algorithm.Algorithm#getDescription()
   */
  @Override
  public Description getDescription() {
    return new Description("LDOF", "Local Distance-Based Outlier Factor", "Local outlier detection appraoch suitable for scattered data by averaging the kNN distance over all k nearest neighbors", "K. Zhang, M. Hutter, H. Jin: A New Local Distance-Based Outlier Detection Approach for Scattered Real-World Data. In: Proc. 13th Pacific-Asia Conference on Advances in Knowledge Discovery and Data Mining (PAKDD 2009), Bangkok, Thailand, 2009.");
  }

  /**
   * @see de.lmu.ifi.dbs.elki.algorithm.Algorithm#getResult()
   */
  @Override
  public MultiResult getResult() {
    return this.result;
  }
}
