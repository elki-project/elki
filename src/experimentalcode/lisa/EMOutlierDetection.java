package experimentalcode.lisa;

import de.lmu.ifi.dbs.elki.algorithm.AbstractAlgorithm;
import de.lmu.ifi.dbs.elki.algorithm.clustering.EM;
import de.lmu.ifi.dbs.elki.algorithm.outlier.ABOD;
import de.lmu.ifi.dbs.elki.data.RealVector;
import de.lmu.ifi.dbs.elki.database.AssociationID;
import de.lmu.ifi.dbs.elki.database.Database;
import de.lmu.ifi.dbs.elki.result.AnnotationResult;
import de.lmu.ifi.dbs.elki.result.MultiResult;
import de.lmu.ifi.dbs.elki.result.Result;
import de.lmu.ifi.dbs.elki.utilities.Description;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.ParameterException;
import de.lmu.ifi.dbs.elki.utilities.pairs.Pair;

import java.util.Iterator;
import java.util.List;
/**
 * outlier detection algorithm using EM Clustering. If an object does not belong to any cluster it is supposed to be an outlier.
 * if the probability for an object to belong to the most probable cluster is still relatively low this object is an outlier 
 * @author lisa
 *
 * @param <V>
 */
public class EMOutlierDetection<V extends RealVector<V, ?>> extends AbstractAlgorithm<V, MultiResult>{
  EM emClustering;
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
      remainingParameters= emClustering.setParameters(remainingParameters);
      return remainingParameters;
  }

  /**
   * Runs the algorithm in the timed evaluation part.
   */

  @Override
  protected MultiResult runInTime(Database<V> database) throws IllegalStateException {
    AnnotationResult<List<Double>> probabilities; 
    emClustering.run(database);
    Iterator<Integer> iter = database.iterator();
    if (iter.hasNext()){
      
      List<AnnotationResult<List<Double>>> annotations = result.filterResults(AnnotationResult.class);
      for (AnnotationResult<List<Double>> annotation : annotations){
        for (Pair<String, ?> pair : annotation.getAnnotations(iter.next())){
          if (pair != null && pair.getFirst() != null && pair.getFirst().equals(AssociationID.PROBABILITY_CLUSTER_I_GIVEN_X)){
            probabilities= annotation; 
            probabilities.
          }
        }
      }
    } else {
      throw new RuntimeException("Database empty.");
    }
  
    
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
