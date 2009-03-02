package de.lmu.ifi.dbs.elki.algorithm;

import java.util.ArrayList;
import java.util.Collection;

import de.lmu.ifi.dbs.elki.data.RealVector;
import de.lmu.ifi.dbs.elki.database.Database;
import de.lmu.ifi.dbs.elki.distance.DoubleDistance;
import de.lmu.ifi.dbs.elki.distance.distancefunction.DistanceFunction;
import de.lmu.ifi.dbs.elki.result.CollectionResult;
import de.lmu.ifi.dbs.elki.utilities.Description;
import de.lmu.ifi.dbs.elki.utilities.pairs.ComparableTriple;

/**
 * <p>
 * Algorithm to materialize all the distances in a data set.
 * </p>
 * 
 * <p>
 * The result can then be used with the DoubleDistanceParser and
 * MultipleFileInput to use cached distances.
 * </p>
 * 
 * <p>
 * Symmetry is assumed.
 * </p>
 * 
 * @author Erich Schubert
 */
public class MaterializeDistances<V extends RealVector<V, ?>> extends DistanceBasedAlgorithm<V, DoubleDistance, CollectionResult<ComparableTriple<Integer, Integer, Double>>> {
  private CollectionResult<ComparableTriple<Integer, Integer, Double>> result;

  /**
   * Empty constructor. Nothing to do.
   */
  public MaterializeDistances() {
    super();
  }

  /**
   * Iterates over all points in the database.
   */
  @Override
  protected CollectionResult<ComparableTriple<Integer, Integer, Double>> runInTime(Database<V> database) throws IllegalStateException {
    DistanceFunction<V, DoubleDistance> distFunc = getDistanceFunction();
    distFunc.setDatabase(database, isVerbose(), isTime());
    int size = database.size();

    Collection<ComparableTriple<Integer, Integer, Double>> r = new ArrayList<ComparableTriple<Integer, Integer, Double>>(size * (size + 1) / 2);

    for(Integer id1 : database.getIDs()) {
      for(Integer id2 : database.getIDs()) {
        // skip inverted pairs
        if(id2 < id1) {
          continue;
        }
        double d = distFunc.distance(id1, id2).getValue();
        r.add(new ComparableTriple<Integer, Integer, Double>(id1, id2, d));
      }
    }
    result = new CollectionResult<ComparableTriple<Integer, Integer, Double>>(r);

    return result;
  }

  /**
   * Describe the algorithm and it's use.
   */
  public Description getDescription() {
    return new Description("MaterializeDistances", "MaterializeDistances", "Materialize all distances in the data set to use as cached/precalculated data.", "");
  }

  /**
   * Return a result object
   */
  public CollectionResult<ComparableTriple<Integer, Integer, Double>> getResult() {
    return result;
  }
}
