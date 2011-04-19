package de.lmu.ifi.dbs.elki.algorithm;

import java.util.ArrayList;
import java.util.Collection;

import de.lmu.ifi.dbs.elki.data.type.TypeInformation;
import de.lmu.ifi.dbs.elki.database.Database;
import de.lmu.ifi.dbs.elki.database.ids.DBID;
import de.lmu.ifi.dbs.elki.database.query.distance.DistanceQuery;
import de.lmu.ifi.dbs.elki.distance.distancefunction.DistanceFunction;
import de.lmu.ifi.dbs.elki.distance.distancevalue.NumberDistance;
import de.lmu.ifi.dbs.elki.logging.Logging;
import de.lmu.ifi.dbs.elki.result.CollectionResult;
import de.lmu.ifi.dbs.elki.utilities.documentation.Description;
import de.lmu.ifi.dbs.elki.utilities.documentation.Title;
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
 * @param <O> Object type
 * @param <D> Distance type
 */
@Title("MaterializeDistances")
@Description("Materialize all distances in the data set to use as cached/precalculated data.")
public class MaterializeDistances<O, D extends NumberDistance<D, ?>> extends AbstractDistanceBasedAlgorithm<O, D, CollectionResult<CTriple<DBID, DBID, Double>>> {
  /**
   * The logger for this class.
   */
  private static final Logging logger = Logging.getLogger(MaterializeDistances.class);

  /**
   * Constructor.
   * 
   * @param distanceFunction Parameterization
   */
  public MaterializeDistances(DistanceFunction<? super O, D> distanceFunction) {
    super(distanceFunction);
  }

  /**
   * Iterates over all points in the database.
   */
  @Override
  protected CollectionResult<CTriple<DBID, DBID, Double>> runInTime(Database database) throws IllegalStateException {
    DistanceQuery<O, D> distFunc = getDistanceQuery(database);
    int size = distFunc.getRepresentation().size();

    Collection<CTriple<DBID, DBID, Double>> r = new ArrayList<CTriple<DBID, DBID, Double>>(size * (size + 1) / 2);

    for(DBID id1 : distFunc.getRepresentation().iterDBIDs()) {
      for(DBID id2 : distFunc.getRepresentation().iterDBIDs()) {
        // skip inverted pairs
        if(id2.compareTo(id1) > 0) {
          continue;
        }
        double d = distFunc.distance(id1, id2).doubleValue();
        r.add(new CTriple<DBID, DBID, Double>(id1, id2, d));
      }
    }
    return new CollectionResult<CTriple<DBID, DBID, Double>>("Distance Matrix", "distance-matrix", r);
  }

  @Override
  protected Logging getLogger() {
    return logger;
  }

  @Override
  public TypeInformation getInputTypeRestriction() {
    return getDistanceFunction().getInputTypeRestriction();
  }

  /**
   * Parameterization class.
   * 
   * @author Erich Schubert
   * 
   * @apiviz.exclude
   */
  public static class Parameterizer<O, D extends NumberDistance<D, ?>> extends AbstractDistanceBasedAlgorithm.Parameterizer<O, D> {
    @Override
    protected MaterializeDistances<O, D> makeInstance() {
      return new MaterializeDistances<O, D>(distanceFunction);
    }
  }
}