package de.lmu.ifi.dbs.elki.preprocessing;

import java.util.HashMap;
import java.util.List;

import de.lmu.ifi.dbs.elki.data.KNNList;
import de.lmu.ifi.dbs.elki.data.NumberVector;
import de.lmu.ifi.dbs.elki.database.Database;
import de.lmu.ifi.dbs.elki.database.DistanceResultPair;
import de.lmu.ifi.dbs.elki.database.SpatialIndexDatabase;
import de.lmu.ifi.dbs.elki.distance.Distance;
import de.lmu.ifi.dbs.elki.distance.distancefunction.DistanceFunction;
import de.lmu.ifi.dbs.elki.distance.distancefunction.EuclideanDistanceFunction;
import de.lmu.ifi.dbs.elki.index.tree.spatial.SpatialEntry;
import de.lmu.ifi.dbs.elki.index.tree.spatial.SpatialIndex;
import de.lmu.ifi.dbs.elki.index.tree.spatial.SpatialNode;
import de.lmu.ifi.dbs.elki.math.MeanVariance;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.AbstractParameterizable;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.ClassParameter;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.IntParameter;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.OptionID;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.ParameterException;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.constraints.GreaterConstraint;
import de.lmu.ifi.dbs.elki.utilities.pairs.Pair;
import de.lmu.ifi.dbs.elki.utilities.progress.FiniteProgress;

/**
 * A preprocessor for annotation of the k nearest neighbors (and their
 * distances) to each database object.
 * 
 * Used for example by {@link de.lmu.ifi.dbs.elki.algorithm.outlier.LOF}.
 * 
 * @author Erich Schubert
 * @param <O> the type of database objects the preprocessor can be applied to
 * @param <D> the type of distance the used distance function will return
 */
public class SpatialApproximationMaterializeKNNPreprocessor<O extends NumberVector<O, ?>, D extends Distance<D>, N extends SpatialNode<N, E>, E extends SpatialEntry> extends AbstractParameterizable implements Preprocessor<O> {
  /**
   * OptionID for {@link #K_PARAM}
   */
  public static final OptionID K_ID = OptionID.getOrCreateOptionID("materialize.k", "The number of nearest neighbors of an object to be materialized.");

  /**
   * Parameter to specify the number of nearest neighbors of an object to be
   * materialized. must be an integer greater than 1.
   * <p>
   * Key: {@code -materialize.k}
   * </p>
   */
  private final IntParameter K_PARAM = new IntParameter(K_ID, new GreaterConstraint(1));

  /**
   * Holds the value of {@link #K_PARAM}.
   */
  int k;

  /**
   * OptionID for {@link #DISTANCE_FUNCTION_PARAM}
   */
  public static final OptionID DISTANCE_FUNCTION_ID = OptionID.getOrCreateOptionID("materialize.distance", "the distance function to materialize the nearest neighbors");

  /**
   * Parameter to indicate the distance function to be used to ascertain the
   * nearest neighbors.
   * <p/>
   * <p>
   * Default value: {@link EuclideanDistanceFunction}
   * </p>
   * <p>
   * Key: {@code materialize.distance}
   * </p>
   */
  public final ClassParameter<DistanceFunction<O, D>> DISTANCE_FUNCTION_PARAM = new ClassParameter<DistanceFunction<O, D>>(DISTANCE_FUNCTION_ID, DistanceFunction.class, EuclideanDistanceFunction.class.getName());

  /**
   * Hold the distance function to be used.
   */
  private DistanceFunction<O, D> distanceFunction;
  
  /**
   * Materialized neighborhood
   */
  private HashMap<Integer, List<DistanceResultPair<D>>> materialized;

  /**
   * Provides a k nearest neighbors Preprocessor.
   */
  public SpatialApproximationMaterializeKNNPreprocessor() {
    super();
    addOption(K_PARAM);
    addOption(DISTANCE_FUNCTION_PARAM);
  }

  /**
   * Annotates the nearest neighbors based on the values of
   * {@link #k} and {@link #distanceFunction} to each database
   * object.
   */
  @SuppressWarnings("unchecked")
  public void run(Database<O> database, boolean verbose, boolean time) {
    distanceFunction.setDatabase(database, verbose, time);

    if (!(database instanceof SpatialIndexDatabase)) {
      throw new IllegalStateException(
          "Database must be an instance of "
              + SpatialIndexDatabase.class.getName());
    }
    SpatialIndexDatabase<O, N, E> db = (SpatialIndexDatabase<O, N, E>) database;
    SpatialIndex<O, N, E> index = db.getIndex();

    materialized = new HashMap<Integer, List<DistanceResultPair<D>>>(database.size());
    MeanVariance pagesize = new MeanVariance();
    MeanVariance ksize = new MeanVariance();
    if(logger.isVerbose()) {
      logger.verbose("Approximating nearest neighbor lists to database objects");
    }
    
    List<E> leaves = index.getLeaves();
    FiniteProgress leaveprog = new FiniteProgress("Processing leave nodes.", leaves.size());
    int count = 0;
    for (E leaf : leaves) {
      N node = db.getIndex().getNode(leaf);
      int size = node.getNumEntries();
      pagesize.put(size);
      if(logger.isDebuggingFinest()) {
        logger.debugFinest("NumEntires = "+size);
      }
      // Collect the ids in this node.
      Integer[] ids = new Integer[size];
      for (int i = 0; i < size; i++) {
        ids[i] = node.getEntry(i).getID();
      }
      HashMap<Pair<Integer, Integer>, D> cache = new HashMap<Pair<Integer, Integer>, D>(size*size*3/8);
      for (Integer id : ids) {
        KNNList<D> kNN = new KNNList<D>(k, distanceFunction.infiniteDistance());
        for (Integer id2 : ids) {
          if (id == id2) {
            kNN.add(new DistanceResultPair<D>(distanceFunction.distance(id, id2), id2));
          } else {
            Pair<Integer, Integer> key = new Pair<Integer, Integer>(id, id2);
            D d = cache.get(key);
            if (d != null) {
              // consume the previous result.
              kNN.add(new DistanceResultPair<D>(d, id2));
              cache.remove(key);
            } else {
              // compute new and store the previous result.
              d = distanceFunction.distance(id, id2);
              kNN.add(new DistanceResultPair<D>(d, id2));
              // put it into the cache, but with the keys reversed
              key.first = id2;
              key.second = id;
              cache.put(key, d);
            }
          }
        }
        ksize.put(kNN.size());
        materialized.put(id, kNN.toList());
      }
      if (this.debug) {
        if (cache.size() > 0) {
          logger.warning("Cache should be empty after each run, but still has " + cache.size() + " elements.");
        }
      }
      if(logger.isVerbose()) {
        count++;
        leaveprog.setProcessed(count);
        logger.progress(leaveprog);
      }
    }
    if(logger.isVerbose()) {
      logger.verbose("Average page size = "+pagesize.getMean()+" +- "+pagesize.getStddev());
      logger.verbose("On average, "+ksize.getMean()+" +- "+ksize.getStddev()+" neighbors returned.");
    }
  }

  /**
   * Sets the parameter values of {@link #K_PARAM} and
   * {@link #DISTANCE_FUNCTION_PARAM} to {@link #k} and
   * {@link #distanceFunction}, respectively.
   */
  @Override
  public String[] setParameters(String[] args) throws ParameterException {
    String[] remainingParameters = super.setParameters(args);
    // number of neighbors
    k = K_PARAM.getValue();

    // distance function
    distanceFunction = DISTANCE_FUNCTION_PARAM.instantiateClass();
    remainingParameters = distanceFunction.setParameters(remainingParameters);
    addParameterizable(distanceFunction);
    
    rememberParametersExcept(args, remainingParameters);
    return remainingParameters;
  }

  /**
   * Provides a short description of the purpose of this class.
   */
  @Override
  public String parameterDescription() {
    StringBuffer description = new StringBuffer();
    description.append(SpatialApproximationMaterializeKNNPreprocessor.class.getName());
    description.append(" materializes the k nearest neighbors of objects of a database.\n");
    description.append(super.parameterDescription());
    return description.toString();
  }

  /**
   * Materialize a neighborhood.
   * 
   * @return the materialized neighborhoods
   */
  public HashMap<Integer, List<DistanceResultPair<D>>> getMaterialized() {
    return materialized;
  }
}