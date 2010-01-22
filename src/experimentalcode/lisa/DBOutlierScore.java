package experimentalcode.lisa;

import java.util.HashMap;

import java.util.List;

import de.lmu.ifi.dbs.elki.algorithm.DistanceBasedAlgorithm;
import de.lmu.ifi.dbs.elki.data.DatabaseObject;
import de.lmu.ifi.dbs.elki.database.AssociationID;
import de.lmu.ifi.dbs.elki.database.Database;
import de.lmu.ifi.dbs.elki.distance.Distance;
import de.lmu.ifi.dbs.elki.result.AnnotationFromDatabase;
import de.lmu.ifi.dbs.elki.result.MultiResult;
import de.lmu.ifi.dbs.elki.result.OrderingFromAssociation;
import de.lmu.ifi.dbs.elki.utilities.Description;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.OptionID;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.ParameterException;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.PatternParameter;

/**
  
      // compute percentage of neighbors in the given neighborhood with size d
 * @author Lisa Reichert
 *
 * @param <O>
 * @param <D>
 */
public  class DBOutlierScore<O extends DatabaseObject, D extends Distance<D>> extends AbstractDBOutlierDetection<O,D> {

  @Override
  protected HashMap<Integer, Double> computeOutlierScores(Database<O> database, String d) {
    double n;

    HashMap<Integer, Double> scores= new HashMap<Integer, Double>();
    for(Integer id : database) {
      // compute percentage of neighbors in the given neighborhood with size d
      n = (database.rangeQuery(id, d, getDistanceFunction()).size()) / (double) database.size();
      scores.put(id, 1-n);
    }
    scores.toString();
    return scores;
  }

  @Override
  public Description getDescription() {
    // TODO Auto-generated method stub
    return null;
  }
}
