package de.lmu.ifi.dbs.elki.database;

import java.util.Collection;

import de.lmu.ifi.dbs.elki.data.DatabaseObject;
import de.lmu.ifi.dbs.elki.database.query.distance.DistanceQuery;
import de.lmu.ifi.dbs.elki.distance.distancefunction.DistanceFunction;
import de.lmu.ifi.dbs.elki.distance.distancevalue.Distance;
import de.lmu.ifi.dbs.elki.index.tree.metrical.MetricalIndex;
import de.lmu.ifi.dbs.elki.index.tree.metrical.MetricalNode;
import de.lmu.ifi.dbs.elki.index.tree.metrical.mtreevariants.MTreeEntry;
import de.lmu.ifi.dbs.elki.logging.Logging;
import de.lmu.ifi.dbs.elki.utilities.documentation.Description;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.OptionID;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.Parameterizable;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.Parameterization;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.TrackParameters;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.ObjectParameter;
import de.lmu.ifi.dbs.elki.utilities.pairs.Pair;

/**
 * MetricalIndexDatabase is a database implementation which is supported by a
 * metrical index structure.
 * 
 * @author Elke Achtert
 * @param <O> the type of FeatureVector as element of the database
 * @param <D> Distance type
 * @param <N> Node type
 * @param <E> Entry type
 */
@Description("Database using a metrical index")
public class MetricalIndexDatabase<O extends DatabaseObject, D extends Distance<D>, N extends MetricalNode<N, E>, E extends MTreeEntry<D>> extends AbstractDatabase<O> implements Parameterizable {
  /**
   * The logger for this class.
   */
  private static final Logging logger = Logging.getLogger(MetricalIndexDatabase.class);

  /**
   * OptionID for {@link #INDEX_PARAM}
   */
  public static final OptionID INDEX_ID = OptionID.getOrCreateOptionID("metricalindexdb.index", "Metrical index class to use.");

  /**
   * Parameter to specify the metrical index to use.
   * <p>
   * Key: {@code -metricalindexdb.index}
   * </p>
   */
  private final ObjectParameter<MetricalIndex<O, D, N, E>> INDEX_PARAM = new ObjectParameter<MetricalIndex<O, D, N, E>>(INDEX_ID, MetricalIndex.class);

  /**
   * The metrical index storing the data.
   */
  MetricalIndex<O, D, N, E> index;

  /**
   * Store own parameters, for partitioning.
   */
  private Collection<Pair<OptionID, Object>> params;

  /**
   * Constructor, adhering to
   * {@link de.lmu.ifi.dbs.elki.utilities.optionhandling.Parameterizable}
   * 
   * @param config Parameterization
   */
  public MetricalIndexDatabase(Parameterization config) {
    super();
    config = config.descend(this);
    TrackParameters track = new TrackParameters(config);
    if(track.grab(INDEX_PARAM)) {
      index = INDEX_PARAM.instantiateClass(track);
      index.setDatabase(this);
    }
    addIndex(index);
    params = track.getGivenParameters();
  }

  /**
   * Returns the index of this database.
   * 
   * @return the index of this database
   */
  public MetricalIndex<O, D, N, E> getIndex() {
    return index;
  }

  @Override
  protected Collection<Pair<OptionID, Object>> getParameters() {
    return new java.util.Vector<Pair<OptionID, Object>>(this.params);
  }

  /**
   * Throws an IllegalArgumentException if the specified distance function is
   * not an instance of the distance function used by the underlying index of
   * this database.
   * 
   * @throws IllegalArgumentException
   * @param <F> query type
   * @param distanceQuery the distance query to be checked
   */
  private <F extends DistanceQuery<? super O, ?>> F checkDistanceFunction(F distanceQuery) {
    DistanceFunction<? super O, ?> distanceFunction = distanceQuery.getDistanceFunction();
    // todo: the same class does not necessarily indicate the same
    // distancefunction!!! (e.g. dim selecting df!)
    if(!distanceFunction.getClass().equals(index.getDistanceFunction().getClass())) {
      logger.warning("Querying the database with an unsupported distance function, fallback to sequential scan.");
      return null;
      // throw new
      // IllegalArgumentException("Parameter distanceFunction must be an instance of "
      // + index.getDistanceFunction().getClass() + ", but is " +
      // distanceFunction.getClass());
    }
    return distanceQuery;
  }
}