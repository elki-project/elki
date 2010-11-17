package de.lmu.ifi.dbs.elki.database;

import java.util.Collection;
import java.util.List;

import de.lmu.ifi.dbs.elki.data.NumberVector;
import de.lmu.ifi.dbs.elki.database.query.distance.DistanceQuery;
import de.lmu.ifi.dbs.elki.distance.distancefunction.DistanceFunction;
import de.lmu.ifi.dbs.elki.distance.distancefunction.SpatialPrimitiveDistanceFunction;
import de.lmu.ifi.dbs.elki.distance.distancevalue.Distance;
import de.lmu.ifi.dbs.elki.index.tree.spatial.SpatialEntry;
import de.lmu.ifi.dbs.elki.index.tree.spatial.SpatialIndex;
import de.lmu.ifi.dbs.elki.index.tree.spatial.SpatialNode;
import de.lmu.ifi.dbs.elki.logging.Logging;
import de.lmu.ifi.dbs.elki.utilities.documentation.Description;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.OptionID;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.Parameterizable;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.Parameterization;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.TrackParameters;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.ObjectParameter;
import de.lmu.ifi.dbs.elki.utilities.pairs.Pair;

/**
 * SpatialIndexDatabase is a database implementation which is supported by a
 * spatial index structure.
 * 
 * @author Elke Achtert
 * @param <O> the type of FeatureVector as element of the database
 * @param <N> the type of SpatialNode stored in the index
 * @param <E> the type of SpatialEntry stored in the index
 */
@Description("Database using a spatial index")
public class SpatialIndexDatabase<O extends NumberVector<?, ?>, N extends SpatialNode<N, E>, E extends SpatialEntry> extends AbstractDatabase<O> implements Parameterizable {
  /**
   * The logger for this class.
   */
  private static final Logging logger = Logging.getLogger(SpatialIndexDatabase.class);
  
  /**
   * OptionID for {@link #INDEX_PARAM}
   */
  public static final OptionID INDEX_ID = OptionID.getOrCreateOptionID("spatialindexdb.index", "Spatial index class to use.");

  /**
   * Parameter to specify the spatial index to use.
   * <p>
   * Key: {@code -spatialindexdb.index}
   * </p>
   */
  private final ObjectParameter<SpatialIndex<O, N, E>> INDEX_PARAM = new ObjectParameter<SpatialIndex<O, N, E>>(INDEX_ID, SpatialIndex.class);

  /**
   * The index structure storing the data.
   */
  protected SpatialIndex<O, N, E> index;

  /**
   * Store own parameters, needed for partitioning.
   */
  private Collection<Pair<OptionID, Object>> params;

  /**
   * Constructor, adhering to
   * {@link de.lmu.ifi.dbs.elki.utilities.optionhandling.Parameterizable}
   * 
   * @param config Parameterization
   */
  public SpatialIndexDatabase(Parameterization config) {
    super();
    config = config.descend(this);
    TrackParameters track = new TrackParameters(config);
    if(track.grab(INDEX_PARAM)) {
      index = INDEX_PARAM.instantiateClass(track);
      index.setDatabase(this);
      addIndex(index);
    }
    params = track.getGivenParameters();
  }

  /**
   * Returns a list of the leaf nodes of the underlying spatial index of this
   * database.
   * 
   * @return a list of the leaf nodes of the underlying spatial index of this
   *         database
   */
  public List<E> getLeaves() {
    return index.getLeaves();
  }

  /**
   * Returns the id of the root of the underlying index.
   * 
   * @return the id of the root of the underlying index
   */
  public E getRootEntry() {
    return index.getRootEntry();
  }

  /**
   * Returns the index of this database.
   * 
   * @return the index of this database
   */
  @Deprecated
  public SpatialIndex<O, N, E> getIndex() {
    return index;
  }

  @Override
  protected Collection<Pair<OptionID, Object>> getParameters() {
    return new java.util.Vector<Pair<OptionID, Object>>(this.params);
  }

  /**
   * Throws an IllegalArgumentException if the specified distance function is
   * not a SpatialDistanceFunction.
   * 
   * @throws IllegalArgumentException
   * @param <T> distance type
   * @param distanceQuery the distance query to be checked
   */
  @SuppressWarnings("unchecked")
  private <T extends Distance<T>> SpatialPrimitiveDistanceFunction<O, T> checkDistanceFunction(DistanceQuery<O, T> distanceQuery) {
    DistanceFunction<? super O, T> distanceFunction = distanceQuery.getDistanceFunction();
    if(distanceFunction instanceof SpatialPrimitiveDistanceFunction<?, ?>) {
      return (SpatialPrimitiveDistanceFunction<O, T>) distanceFunction;
    }
    else {
      logger.warning("Querying the database with an unsupported distance function, fallback to sequential scan. Got: "+distanceQuery.getClass());
      return null;
      // throw new
      // IllegalArgumentException("Distance function must be an instance of SpatialDistanceFunction!");
    }
  }
}