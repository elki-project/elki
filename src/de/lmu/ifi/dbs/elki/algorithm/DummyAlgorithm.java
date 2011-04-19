package de.lmu.ifi.dbs.elki.algorithm;

import de.lmu.ifi.dbs.elki.data.NumberVector;
import de.lmu.ifi.dbs.elki.data.type.VectorFieldTypeInformation;
import de.lmu.ifi.dbs.elki.database.Database;
import de.lmu.ifi.dbs.elki.database.ids.DBID;
import de.lmu.ifi.dbs.elki.database.query.distance.DistanceQuery;
import de.lmu.ifi.dbs.elki.database.query.knn.KNNQuery;
import de.lmu.ifi.dbs.elki.database.relation.Relation;
import de.lmu.ifi.dbs.elki.distance.distancefunction.EuclideanDistanceFunction;
import de.lmu.ifi.dbs.elki.distance.distancevalue.DoubleDistance;
import de.lmu.ifi.dbs.elki.logging.Logging;
import de.lmu.ifi.dbs.elki.result.Result;
import de.lmu.ifi.dbs.elki.utilities.documentation.Description;
import de.lmu.ifi.dbs.elki.utilities.documentation.Title;

/**
 * Dummy Algorithm, which just iterates over all points once, doing a 10NN query
 * each. Useful in testing e.g. index structures and as template for custom
 * algorithms.
 * 
 * @author Erich Schubert
 * @param <V> Vector type
 */
@Title("Dummy Algorithm")
@Description("The algorithm executes a euclidena 10NN query on all data points, and can be used in unit testing")
public class DummyAlgorithm<O extends NumberVector<?, ?>> extends AbstractAlgorithm<O, Result> {
  /**
   * The logger for this class.
   */
  private static final Logging logger = Logging.getLogger(DummyAlgorithm.class);

  /**
   * Constructor, adhering to
   * {@link de.lmu.ifi.dbs.elki.utilities.optionhandling.Parameterizable}
   */
  public DummyAlgorithm() {
    super();
  }

  /**
   * Iterates over all points in the database.
   */
  @Override
  protected Result runInTime(Database database) throws IllegalStateException {
    // Bind to the database
    Relation<O> relation = getRelation(database);
    DistanceQuery<O, DoubleDistance> distQuery = database.getDistanceQuery(relation, EuclideanDistanceFunction.STATIC);
    KNNQuery<O, DoubleDistance> knnQuery = database.getKNNQuery(distQuery, 10);

    for(DBID id : relation.iterDBIDs()) {
      // Get the actual object from the database (but discard)
      relation.get(id);
      // run a 10NN query for each point (but discard)
      knnQuery.getKNNForDBID(id, 10);
    }
    return null;
  }

  @Override
  public VectorFieldTypeInformation<? super O> getInputTypeRestriction() {
    return EuclideanDistanceFunction.STATIC.getInputTypeRestriction();
  }

  @Override
  protected Logging getLogger() {
    return logger;
  }
}