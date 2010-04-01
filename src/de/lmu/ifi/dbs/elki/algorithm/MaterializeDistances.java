package de.lmu.ifi.dbs.elki.algorithm;

import java.util.ArrayList;
import java.util.Collection;

import de.lmu.ifi.dbs.elki.data.DatabaseObject;
import de.lmu.ifi.dbs.elki.database.Database;
import de.lmu.ifi.dbs.elki.distance.distancefunction.DistanceFunction;
import de.lmu.ifi.dbs.elki.distance.distancevalue.NumberDistance;
import de.lmu.ifi.dbs.elki.result.CollectionResult;
import de.lmu.ifi.dbs.elki.utilities.documentation.Description;
import de.lmu.ifi.dbs.elki.utilities.documentation.Title;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.Parameterization;
import de.lmu.ifi.dbs.elki.utilities.pairs.CTriple;

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
 * @param <V> Vector type
 * @param <D> Distance type
 * @param <N> Number type for distance
 */
@Title("MaterializeDistances")
@Description("Materialize all distances in the data set to use as cached/precalculated data.")
public class MaterializeDistances<V extends DatabaseObject, D extends NumberDistance<D, N>, N extends Number> extends DistanceBasedAlgorithm<V, D, CollectionResult<CTriple<Integer, Integer, Double>>> {
  /**
   * Constructor, adhering to
   * {@link de.lmu.ifi.dbs.elki.utilities.optionhandling.Parameterizable}
   * 
   * @param config Parameterization
   */
  public MaterializeDistances(Parameterization config) {
    super(config);
  }

  /**
   * Iterates over all points in the database.
   */
  @Override
  protected CollectionResult<CTriple<Integer, Integer, Double>> runInTime(Database<V> database) throws IllegalStateException {
    DistanceFunction<V, D> distFunc = getDistanceFunction();
    distFunc.setDatabase(database);
    int size = database.size();

    Collection<CTriple<Integer, Integer, Double>> r = new ArrayList<CTriple<Integer, Integer, Double>>(size * (size + 1) / 2);

    for(Integer id1 : database.getIDs()) {
      for(Integer id2 : database.getIDs()) {
        // skip inverted pairs
        if(id2 < id1) {
          continue;
        }
        double d = distFunc.distance(id1, id2).doubleValue();
        r.add(new CTriple<Integer, Integer, Double>(id1, id2, d));
      }
    }
    return new CollectionResult<CTriple<Integer, Integer, Double>>(r);
  }
}
