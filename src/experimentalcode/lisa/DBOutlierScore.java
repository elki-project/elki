package experimentalcode.lisa;

import de.lmu.ifi.dbs.elki.algorithm.DistanceBasedAlgorithm;
import de.lmu.ifi.dbs.elki.data.DatabaseObject;
import de.lmu.ifi.dbs.elki.database.AssociationID;
import de.lmu.ifi.dbs.elki.database.Database;
import de.lmu.ifi.dbs.elki.database.DistanceResultPair;
import de.lmu.ifi.dbs.elki.database.IndexDatabase;
import de.lmu.ifi.dbs.elki.distance.Distance;
import de.lmu.ifi.dbs.elki.distance.DoubleDistance;
import de.lmu.ifi.dbs.elki.result.AnnotationFromDatabase;
import de.lmu.ifi.dbs.elki.result.MultiResult;
import de.lmu.ifi.dbs.elki.result.OrderingFromAssociation;
import de.lmu.ifi.dbs.elki.utilities.Description;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.DoubleParameter;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.OptionID;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.PatternParameter;

import java.util.Iterator;
import java.util.List;

public abstract class DBOutlierScore<O extends DatabaseObject, D extends Distance<D>> extends DistanceBasedAlgorithm<O, D, MultiResult> {

  public static final OptionID D_ID = OptionID.getOrCreateOptionID("dbod.d", "size of the D-neighborhood");

  public static final OptionID P_ID = OptionID.getOrCreateOptionID("dbod.p", "minimum fraction of objects that must be outside the D-neigborhood of an outlier");

  /**
   * Parameter to specify the size of the D-neighborhood,
   * 
   * <p>
   * Key: {@code -dbod.d}
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

  public static final AssociationID<Integer> DBOD_ODEGREE = AssociationID.getOrCreateAssociationID("dbod.odegree", Integer.class);

  /**
   * Runs the algorithm in the timed evaluation part.
   */
  @Override
  protected MultiResult runInTime(Database<O> database) throws IllegalStateException {
    getDistanceFunction().setDatabase(database, isVerbose(), isTime());

    for(Integer id : database) {
      // compute the number of neighbors in the given neighborhood with size d
      int number = database.rangeQuery(id, d, getDistanceFunction()).size();

      // flag as outlier
      database.associate(DBOD_ODEGREE, id, number);
    }
    AnnotationFromDatabase<Integer, O> res1 = new AnnotationFromDatabase<Integer, O>(database, DBOD_ODEGREE);
    // Ordering
    OrderingFromAssociation<Integer, O> res2 = new OrderingFromAssociation<Integer, O>(database, DBOD_ODEGREE, true);
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
