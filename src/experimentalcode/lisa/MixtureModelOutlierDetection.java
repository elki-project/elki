package experimentalcode.lisa;



import de.lmu.ifi.dbs.elki.algorithm.AbstractAlgorithm;
import de.lmu.ifi.dbs.elki.data.RealVector;
import de.lmu.ifi.dbs.elki.database.AssociationID;
import de.lmu.ifi.dbs.elki.database.Database;
import de.lmu.ifi.dbs.elki.result.AnnotationsFromDatabase;
import de.lmu.ifi.dbs.elki.result.MultiResult;
import de.lmu.ifi.dbs.elki.result.OrderingFromAssociation;
import de.lmu.ifi.dbs.elki.utilities.Description;

import java.util.ArrayList;
import java.util.List;
/**
 * Assuming that a data set contains objects from a mixture of two probability distributions. 
 * Objects are put in a set of normal objects and the set of anomalous objects is empty. An iterative procedure then transfers 
 * objects from the ordinary set to the anomalous set as long as the transfer increases the overall likelihood of the data. 
 * 
 * @author lisa
 *
 * @param <V>
 */
public class MixtureModelOutlierDetection<V extends RealVector<V,Double>> extends AbstractAlgorithm<V,MultiResult> {
  public static final AssociationID<Boolean> MMOD_OFLAG = AssociationID.getOrCreateAssociationID("mmod.oflag", Boolean.class);
  
  MultiResult result;
  
  @Override
  protected MultiResult runInTime(Database<V> database) throws IllegalStateException {
    //TODO: threshold?
    int c = 0;
    //set of normal objects (containing all data in the beginning) and a 
    List<Integer> normalObjs = database.getIDs();
    //set of anomalous objects(empty in the beginning)
    List<Integer> anomalousObjs = new ArrayList<Integer>();
    //compute loglikelyhood
    double logLike = loglikelihood(normalObjs, anomalousObjs);
    for(int i= 0; i< normalObjs.size(); i++){
      //move object to anomalousObjs and test if the loglikelyhood increases significantly
      Integer x = normalObjs.get(i);
      anomalousObjs.add(x);
      normalObjs.remove(i);
      double currentLogLike = loglikelihood(normalObjs, anomalousObjs);
      double deltaLog = logLike - currentLogLike;
      //threshold
      if(deltaLog > c) {
        //flag as outlier 
        database.associate(MMOD_OFLAG, x, true);
        logLike = currentLogLike;   
      }
      else{
        //move object back to normalObjects
        normalObjs.add(i, x);
        anomalousObjs.remove(anomalousObjs.size()-1);
        //flag as non outlier
        database.associate(MMOD_OFLAG, x, false);
      }
    }
    AnnotationsFromDatabase<V, Boolean> res1 = new AnnotationsFromDatabase<V, Boolean>(database);
    res1.addAssociation("MMOD_OFLAG", MMOD_OFLAG);
    // Ordering
    OrderingFromAssociation<Boolean, V> res2 = new OrderingFromAssociation<Boolean, V>(database, MMOD_OFLAG, true); 
    // combine results.
    result = new MultiResult();
    result.addResult(res1);
    result.addResult(res2);
    return result;   
  }
  private double loglikelihood(List<Integer> normalObjs, List<Integer> anomalousObjs) {
    // TODO Auto-generated method stub
    return 0;
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

}
