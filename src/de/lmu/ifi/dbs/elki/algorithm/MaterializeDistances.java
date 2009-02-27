package de.lmu.ifi.dbs.elki.algorithm;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Random;
import java.util.Set;
import java.util.TreeSet;

import de.lmu.ifi.dbs.elki.algorithm.DistanceBasedAlgorithm;
import de.lmu.ifi.dbs.elki.algorithm.clustering.ByLabelClustering;
import de.lmu.ifi.dbs.elki.data.DoubleVector;
import de.lmu.ifi.dbs.elki.data.RealVector;
import de.lmu.ifi.dbs.elki.data.cluster.Cluster;
import de.lmu.ifi.dbs.elki.data.model.Model;
import de.lmu.ifi.dbs.elki.database.Database;
import de.lmu.ifi.dbs.elki.distance.DoubleDistance;
import de.lmu.ifi.dbs.elki.distance.distancefunction.DistanceFunction;
import de.lmu.ifi.dbs.elki.math.Histogram;
import de.lmu.ifi.dbs.elki.math.MeanVariance;
import de.lmu.ifi.dbs.elki.math.Histogram.Constructor;
import de.lmu.ifi.dbs.elki.result.CollectionResult;
import de.lmu.ifi.dbs.elki.utilities.Description;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.Flag;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.OptionID;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.ParameterException;
import de.lmu.ifi.dbs.elki.utilities.pairs.ComparablePair;
import de.lmu.ifi.dbs.elki.utilities.pairs.ComparableTriple;
import de.lmu.ifi.dbs.elki.utilities.pairs.SimplePair;

/**
 * <p>Algorithm to materialize all the distances in a data set.</p>
 *
 * <p>The result can then be used with the DoubleDistanceParser and
 * MultipleFileInput to use cached distances.</p>
 *
 * <p>Symmetry is assumed.</p>
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
