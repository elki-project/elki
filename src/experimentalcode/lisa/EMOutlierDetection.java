package experimentalcode.lisa;

import de.lmu.ifi.dbs.elki.algorithm.AbstractAlgorithm;
import de.lmu.ifi.dbs.elki.data.RealVector;
import de.lmu.ifi.dbs.elki.database.Database;
import de.lmu.ifi.dbs.elki.result.MultiResult;
import de.lmu.ifi.dbs.elki.utilities.Description;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.ParameterException;
/**
 * outlier detection algorithm using EM Clustering. If an object does not belong to any cluster it is supposed to be an outlier.
 * if the probability for an object to belong to the most probable cluster is still relatively low this object is an outlier 
 * @author lisa
 *
 * @param <V>
 */
public class EMOutlierDetection<V extends RealVector<V, ?>> extends AbstractAlgorithm<V, MultiResult>{
  /**
   * Provides the result of the algorithm.
   */
  MultiResult result;

  /**
   * Constructor, adding options to option handler.
   */
  public EMOutlierDetection() {
    super();

    }
  
  /**
   * Calls the super method
   * and sets additionally the values of the parameter
   * {@link #K_PARAM}, {@link #N_PARAM} 
   */
  @Override
  public String[] setParameters(String[] args) throws ParameterException {
      String[] remainingParameters = super.setParameters(args);
      
      return remainingParameters;
  }

  /**
   * Runs the algorithm in the timed evaluation part.
   */

  @Override
  protected MultiResult runInTime(Database<V> database) throws IllegalStateException {
    
    
    return null;
  }

  @Override
  public Description getDescription() {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public MultiResult getResult() {
    // TODO Auto-generated method stub
    return null;
  }
  
}
