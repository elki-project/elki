package experimentalcode.arthur;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import de.lmu.ifi.dbs.elki.algorithm.DistanceBasedAlgorithm;
import de.lmu.ifi.dbs.elki.data.DatabaseObject;
import de.lmu.ifi.dbs.elki.database.AssociationID;
import de.lmu.ifi.dbs.elki.database.Database;
import de.lmu.ifi.dbs.elki.database.DistanceResultPair;
import de.lmu.ifi.dbs.elki.distance.DoubleDistance;
import de.lmu.ifi.dbs.elki.preprocessing.MaterializeKNNPreprocessor;
import de.lmu.ifi.dbs.elki.result.AnnotationFromHashMap;
import de.lmu.ifi.dbs.elki.result.MultiResult;
import de.lmu.ifi.dbs.elki.result.OrderingFromHashMap;
import de.lmu.ifi.dbs.elki.result.ResultUtil;
import de.lmu.ifi.dbs.elki.utilities.Description;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.IntParameter;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.OptionID;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.OptionUtil;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.ParameterException;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.UnusedParameterException;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.constraints.GreaterConstraint;
import de.lmu.ifi.dbs.elki.utilities.progress.FiniteProgress;

/**
 * <p>Computes the LDOF (Local Distance-Based Outlier Factor) for all objects of a Database.</p>
 *
 * <p>Reference:<br>
 * K. Zhang, M. Hutter, H. Jin: A New Local Distance-Based Outlier Detection Approach for Scattered Real-World Data.<br>
 * In: Proc. 13th Pacific-Asia Conference on Advances in Knowledge Discovery and Data Mining (PAKDD 2009), Bangkok, Thailand, 2009.
 * </p>
 *
 * @author Arthur Zimek
 * @param <O> the type of DatabaseObjects handled by this Algorithm
 */
public class LDOF<O extends DatabaseObject> extends DistanceBasedAlgorithm<O, DoubleDistance, MultiResult> {

  /**
   * The association id to associate the LDOF_SCORE of an object for the
   * LDOF algorithm.
   */
  public static final AssociationID<Double> LDOF_SCORE = AssociationID.getOrCreateAssociationID("ldof", Double.class);

  /**
   * The association id to associate the maximum LDOF_SCORE of an algorithm run.
   */
  public static final AssociationID<Double> LDOF_MAX = AssociationID.getOrCreateAssociationID("ldofmax", Double.class);

  
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
  private final IntParameter K_PARAM = new IntParameter(K_ID, new GreaterConstraint(2));

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
   * Sets parameter {@link #K_PARAM} and initializes the {@link #knnPreprocessor}.
   */
  public LDOF(){
    addOption(K_PARAM);
    knnPreprocessor = new MaterializeKNNPreprocessor<O, DoubleDistance>();
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
    double ldofmax = 0;
    // compute the ldof values
    HashMap<Integer, Double> ldofs = new HashMap<Integer, Double>();
    // compute LOF_SCORE of each db object
    if(this.isVerbose()) {
      this.verbose("computing LDOFs");
    }

    FiniteProgress progressLDOFs = new FiniteProgress("LDOF_SCORE for objects", database.size());
    int counter = 0;
    for(Integer id : database) {
      counter ++;
      List<DistanceResultPair<DoubleDistance>> neighbors = kNearestNeighboorhoods.get(id);
      int nsize = neighbors.size() - 1;
      // skip the point itself
      double dxp = 0;
      double Dxp = 0;
      for(DistanceResultPair<DoubleDistance> neighbor1 : neighbors) {
        if (neighbor1.getID() != id) {
          dxp += neighbor1.getDistance().getValue();
          for(DistanceResultPair<DoubleDistance> neighbor2 : neighbors){
            if (neighbor1.getID() != neighbor2.getID() && neighbor2.getID() != id){
              Dxp += getDistanceFunction().distance(neighbor1.getID(), neighbor2.getID()).getValue();
            }
          }
        }
      }
      dxp /= nsize;
      Dxp /= (nsize * (nsize-1));
      Double ldof = dxp / Dxp;
      ldofs.put(id, ldof);
      // update maximum
      ldofmax = Math.max(ldofmax, ldof);
      
      if(this.isVerbose()) {
        progressLDOFs.setProcessed(counter);
        this.progress(progressLDOFs);
      }
    }
    if(this.isVerbose()) {
      this.verbose("LDOF finished");
    }
    
    // Build result representation.
    result = new MultiResult();
    result.addResult(new AnnotationFromHashMap<Double>(LDOF_SCORE, ldofs));
    result.addResult(new OrderingFromHashMap<Double>(ldofs, true));

    ResultUtil.setGlobalAssociation(result, LDOF_MAX, ldofmax);
    
    return this.result;
  }
  
  /**
   * Calls the super method and sets additionally the value of the parameter
   * {@link #K_PARAM} and accordingly parameterizes the preprocessor.
   * 
   */
  @Override
  public List<String> setParameters(List<String> args) throws ParameterException {
    List<String> remainingParameters = super.setParameters(args);

    // k
    k = K_PARAM.getValue();

    
    // configure preprocessor
    ArrayList<String> preprocParams1 = new ArrayList<String>();
    OptionUtil.addParameter(preprocParams1, MaterializeKNNPreprocessor.K_ID, Integer.toString(k+1));
    OptionUtil.addParameter(preprocParams1, MaterializeKNNPreprocessor.DISTANCE_FUNCTION_ID, getDistanceFunction().getClass().getCanonicalName());
    OptionUtil.addParameters(preprocParams1, getDistanceFunction().getParameters());
    List<String> remaining1 = knnPreprocessor.setParameters(preprocParams1);
    if (remaining1.size() > 0) {
      throw new UnusedParameterException("First preprocessor did not use all parameters.");
    }
    rememberParametersExcept(args, remainingParameters);
    return remainingParameters;
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
