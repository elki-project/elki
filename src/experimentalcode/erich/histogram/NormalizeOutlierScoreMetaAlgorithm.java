package experimentalcode.erich.histogram;

import java.util.HashMap;
import java.util.List;

import de.lmu.ifi.dbs.elki.algorithm.AbstractAlgorithm;
import de.lmu.ifi.dbs.elki.algorithm.Algorithm;
import de.lmu.ifi.dbs.elki.data.DatabaseObject;
import de.lmu.ifi.dbs.elki.database.AssociationID;
import de.lmu.ifi.dbs.elki.database.Database;
import de.lmu.ifi.dbs.elki.result.AnnotationFromHashMap;
import de.lmu.ifi.dbs.elki.result.AnnotationResult;
import de.lmu.ifi.dbs.elki.result.MultiResult;
import de.lmu.ifi.dbs.elki.result.Result;
import de.lmu.ifi.dbs.elki.result.ResultUtil;
import de.lmu.ifi.dbs.elki.utilities.Description;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.ClassParameter;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.OptionID;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.ParameterException;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.Parameterizable;
import de.lmu.ifi.dbs.elki.utilities.scaling.IdentityScaling;
import de.lmu.ifi.dbs.elki.utilities.scaling.ScalingFunction;
import de.lmu.ifi.dbs.elki.utilities.scaling.outlier.OutlierScalingFunction;

public class NormalizeOutlierScoreMetaAlgorithm<O extends DatabaseObject> extends AbstractAlgorithm<O, Result> {
  /**
   * Association ID for scaled values
   */
  public static final AssociationID<Double> SCALED_SCORE = AssociationID.getOrCreateAssociationID("SCALED_SCORE", Double.class);

  /**
   * OptionID for {@link #SCALING_PARAM}
   */
  public static final OptionID SCALING_ID = OptionID.getOrCreateOptionID("comphist.scaling", "Class to use as scaling function.");

  /**
   * Parameter to specify the algorithm to be applied, must extend
   * {@link de.lmu.ifi.dbs.elki.algorithm.Algorithm}.
   * <p>
   * Key: {@code -algorithm}
   * </p>
   */
  private final ClassParameter<Algorithm<O, Result>> ALGORITHM_PARAM = new ClassParameter<Algorithm<O, Result>>(OptionID.ALGORITHM, Algorithm.class);

  /**
   * Parameter to specify a scaling function to use.
   * <p>
   * Key: {@code -comphist.scaling}
   * </p>
   */
  private final ClassParameter<ScalingFunction> SCALING_PARAM = new ClassParameter<ScalingFunction>(SCALING_ID, ScalingFunction.class, IdentityScaling.class.getName());

  /**
   * Holds the algorithm to run.
   */
  private Algorithm<O, Result> algorithm;

  /**
   * Stores the result object.
   */
  private MultiResult result;

  /**
   * Scaling function to use
   */
  private ScalingFunction scaling;

  public NormalizeOutlierScoreMetaAlgorithm() {
    addOption(ALGORITHM_PARAM);
    addOption(SCALING_PARAM);
  }

  @Override
  protected Result runInTime(Database<O> database) throws IllegalStateException {
    Result innerresult = algorithm.run(database);

    AnnotationResult<Double> ann = getAnnotationResult(database, innerresult);
    if (scaling instanceof OutlierScalingFunction) {
      ((OutlierScalingFunction)scaling).prepare(database, innerresult, ann);
    }
    
    HashMap<Integer, Double> scaledscores = new HashMap<Integer, Double>(database.size());

    for(Integer id : database) {
      double val = ann.getValueFor(id);
      val = scaling.getScaled(val);
      scaledscores.put(id, val);
    }
    
    if (innerresult instanceof MultiResult) {
      result = (MultiResult) innerresult;
    } else {
      result = new MultiResult();
      result.addResult(innerresult);
    }
    result.prependResult(new AnnotationFromHashMap<Double>(SCALED_SCORE, scaledscores));
    
    return result;
  }
  
  /**
   * Find an AnnotationResult that contains Doubles.
   * 
   * @param database Database context
   * @param result Result object
   * @return Iterator to work with
   */
  @SuppressWarnings("unchecked")
  private AnnotationResult<Double> getAnnotationResult(Database<O> database, Result result) {
    List<AnnotationResult<?>> annotations = ResultUtil.getAnnotationResults(result);
    for(AnnotationResult<?> ann : annotations) {
      if(Double.class.isAssignableFrom(ann.getAssociationID().getType())) {
        return (AnnotationResult<Double>) ann;
      }
    }
    throw new IllegalStateException("Comparison algorithm expected at least one Annotation<Double> result, got " + annotations.size() + " annotation results.");
  }

  @Override
  public Description getDescription() {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public MultiResult getResult() {
    return result;
  }

  @Override
  public List<String> setParameters(List<String> args) throws ParameterException {
    List<String> remainingParameters = super.setParameters(args);
    
    // algorithm
    algorithm = ALGORITHM_PARAM.instantiateClass();
    addParameterizable(algorithm);
    remainingParameters = algorithm.setParameters(remainingParameters);
    // scaling function
    scaling = SCALING_PARAM.instantiateClass();
    if (scaling instanceof Parameterizable) {
      Parameterizable param = (Parameterizable) scaling;
      addParameterizable(param);
      remainingParameters = param.setParameters(remainingParameters);
    }
    
    return remainingParameters;
  }
}
