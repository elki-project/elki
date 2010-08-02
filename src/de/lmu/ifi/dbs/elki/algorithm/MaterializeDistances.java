package de.lmu.ifi.dbs.elki.algorithm;

import java.util.ArrayList;
import java.util.Collection;

import de.lmu.ifi.dbs.elki.data.DatabaseObject;
import de.lmu.ifi.dbs.elki.database.Database;
import de.lmu.ifi.dbs.elki.database.ids.DBID;
import de.lmu.ifi.dbs.elki.database.query.DistanceQuery;
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
public class MaterializeDistances<V extends DatabaseObject, D extends NumberDistance<D, N>, N extends Number> extends AbstractDistanceBasedAlgorithm<V, D, CollectionResult<CTriple<DBID, DBID, Double>>> {
  /**
   * Constructor, adhering to
   * {@link de.lmu.ifi.dbs.elki.utilities.optionhandling.Parameterizable}
   * 
   * @param config Parameterization
   */
  public MaterializeDistances(Parameterization config) {
    super(config);
    config = config.descend(this);
  }

  /**
   * Iterates over all points in the database.
   */
  @Override
  protected CollectionResult<CTriple<DBID, DBID, Double>> runInTime(Database<V> database) throws IllegalStateException {
    DistanceQuery<V, D> distFunc = getDistanceQuery(database);
    int size = database.size();

    Collection<CTriple<DBID, DBID, Double>> r = new ArrayList<CTriple<DBID, DBID, Double>>(size * (size + 1) / 2);

    for(DBID id1 : database.getIDs()) {
      for(DBID id2 : database.getIDs()) {
        // skip inverted pairs
        if(id2.compareTo(id1) > 0) {
          continue;
        }
        double d = distFunc.distance(id1, id2).doubleValue();
        r.add(new CTriple<DBID, DBID, Double>(id1, id2, d));
      }
    }
    return new CollectionResult<CTriple<DBID, DBID, Double>>(r);
  }
}