package experimentalcode.lisa;

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
 
 * @author Lisa
 *
 * @param <O>
 * @param <D>
 */
public  class DBOutlierScore<O extends DatabaseObject, D extends Distance<D>> extends DistanceBasedAlgorithm<O, D, MultiResult> {

  public static final OptionID D_ID = OptionID.getOrCreateOptionID("dbos.d", "size of the D-neighborhood");

  /**
   * Parameter to specify the size of the D-neighborhood,
   * 
   * <p>
   * Key: {@code -dbos.d}
   * </p>
   */
  private final PatternParameter D_PARAM = new PatternParameter(D_ID);

  /**
   * Holds the value of {@link #D_PARAM}.
   */
  private String d;

  /**
   * Provides the result of the algorithm.
   */
  MultiResult result;

  /**
   * Constructor, adding options to option handler.
   */
  public DBOutlierScore() {
    super();
    // neighborhood size
    addOption(D_PARAM);
  }

  /**
   * Calls the super method and sets additionally the values of the parameter
   * {@link #D_PARAM}
   */
  @Override
  public List<String> setParameters(List<String> args) throws ParameterException {
    List<String> remainingParameters = super.setParameters(args);

    // neighborhood size
    d = D_PARAM.getValue();

    return remainingParameters;
  }

  public static final AssociationID<Double> DBOS_ODEGREE = AssociationID.getOrCreateAssociationID("dbos.odegree", Double.class);

  /**
   * Runs the algorithm in the timed evaluation part.
   */
  @Override
  protected MultiResult runInTime(Database<O> database) throws IllegalStateException {
    getDistanceFunction().setDatabase(database, isVerbose(), isTime());
    double n;
    for(Integer id : database) {
      // compute percentage of neighbors in the given neighborhood with size d
      n = (database.rangeQuery(id, d, getDistanceFunction()).size()) / (double) database.size();

      
      database.associate(DBOS_ODEGREE, id, 1- n);
    }
    AnnotationFromDatabase<Double, O> res1 = new AnnotationFromDatabase<Double, O>(database, DBOS_ODEGREE);
    // Ordering
    OrderingFromAssociation<Double, O> res2 = new OrderingFromAssociation<Double, O>(database, DBOS_ODEGREE, true);
    // combine results.
    result = new MultiResult();
    result.addResult(res1);
    result.addResult(res2);
    return result;

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
