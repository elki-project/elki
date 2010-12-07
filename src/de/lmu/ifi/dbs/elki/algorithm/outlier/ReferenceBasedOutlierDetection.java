package de.lmu.ifi.dbs.elki.algorithm.outlier;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import de.lmu.ifi.dbs.elki.algorithm.AbstractAlgorithm;
import de.lmu.ifi.dbs.elki.data.NumberVector;
import de.lmu.ifi.dbs.elki.database.AssociationID;
import de.lmu.ifi.dbs.elki.database.Database;
import de.lmu.ifi.dbs.elki.database.datastore.DataStoreFactory;
import de.lmu.ifi.dbs.elki.database.datastore.DataStoreUtil;
import de.lmu.ifi.dbs.elki.database.datastore.WritableDataStore;
import de.lmu.ifi.dbs.elki.database.ids.DBID;
import de.lmu.ifi.dbs.elki.database.ids.DBIDs;
import de.lmu.ifi.dbs.elki.database.query.DistanceResultPair;
import de.lmu.ifi.dbs.elki.database.query.distance.DistanceQuery;
import de.lmu.ifi.dbs.elki.distance.distancefunction.DistanceFunction;
import de.lmu.ifi.dbs.elki.distance.distancefunction.EuclideanDistanceFunction;
import de.lmu.ifi.dbs.elki.distance.distancefunction.PrimitiveDistanceFunction;
import de.lmu.ifi.dbs.elki.distance.distancevalue.NumberDistance;
import de.lmu.ifi.dbs.elki.logging.Logging;
import de.lmu.ifi.dbs.elki.result.AnnotationFromDataStore;
import de.lmu.ifi.dbs.elki.result.AnnotationResult;
import de.lmu.ifi.dbs.elki.result.ReferencePointsResult;
import de.lmu.ifi.dbs.elki.result.outlier.BasicOutlierScoreMeta;
import de.lmu.ifi.dbs.elki.result.outlier.OutlierResult;
import de.lmu.ifi.dbs.elki.result.outlier.OutlierScoreMeta;
import de.lmu.ifi.dbs.elki.utilities.documentation.Description;
import de.lmu.ifi.dbs.elki.utilities.documentation.Reference;
import de.lmu.ifi.dbs.elki.utilities.documentation.Title;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.OptionID;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.Parameterizable;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.constraints.GreaterConstraint;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.Parameterization;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.IntParameter;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.ObjectParameter;
import de.lmu.ifi.dbs.elki.utilities.referencepoints.GridBasedReferencePoints;
import de.lmu.ifi.dbs.elki.utilities.referencepoints.ReferencePointsHeuristic;

/**
 * <p>
 * provides the Reference-Based Outlier Detection algorithm, an algorithm that
 * computes kNN distances approximately, using reference points.
 * </p>
 * <p>
 * Reference:<br>
 * Y. Pei, O. R. Zaiane, Y. Gao: An Efficient Reference-Based Approach to
 * Outlier Detection in Large Datasets.</br> In: Proc. IEEE Int. Conf. on Data
 * Mining (ICDM'06), Hong Kong, China, 2006.
 * </p>
 * 
 * @author Lisa Reichert
 * @author Erich Schubert
 * 
 * @apiviz.composedOf ReferencePointsHeuristic
 * 
 * @param <V> a type of {@link NumberVector} as a suitable data object for this
 *        algorithm
 * @param <D> the distance type processed
 */
@Title("An Efficient Reference-based Approach to Outlier Detection in Large Datasets")
@Description("Computes kNN distances approximately, using reference points with various reference point strategies.")
@Reference(authors = "Y. Pei, O.R.Zaiane, Y. Gao", title = "An Efficient Reference-based Approach to Outlier Detection in Large Datasets", booktitle = "Proc. 19th IEEE Int. Conf. on Data Engineering (ICDE '03), Bangalore, India, 2003", url = "http://dx.doi.org/10.1109/ICDM.2006.17")
public class ReferenceBasedOutlierDetection<V extends NumberVector<?, ?>, D extends NumberDistance<D, ?>> extends AbstractAlgorithm<V, OutlierResult> implements OutlierAlgorithm<V, OutlierResult> {
  /**
   * The logger for this class.
   */
  private static final Logging logger = Logging.getLogger(ReferenceBasedOutlierDetection.class);
  
  /**
   * The association id to associate the REFOD_SCORE of an object for the
   * Reference based outlier detection algorithm.
   */
  public static final AssociationID<Double> REFOD_SCORE = AssociationID.getOrCreateAssociationID("REFOD_SCORE", Double.class);

  /**
   * Parameter for the reference points heuristic.
   */
  public static final OptionID REFP_ID = OptionID.getOrCreateOptionID("refod.refp", "The heuristic for finding reference points.");

  /**
   * Parameter to specify the number of nearest neighbors of an object, to be
   * considered for computing its REFOD_SCORE, must be an integer greater than
   * 1.
   */
  public static final OptionID K_ID = OptionID.getOrCreateOptionID("refod.k", "The number of nearest neighbors");

  /**
   * Holds the value of {@link #K_ID}.
   */
  private int k;

  /**
   * Stores the reference point strategy
   */
  private ReferencePointsHeuristic<V> refp;

  /**
   * Distance function to use.
   */
  private DistanceFunction<V, D> distanceFunction;

  /**
   * Constructor with parameters.
   * 
   * @param k k Parameter
   * @param distanceFunction distance function
   * @param refp Reference points heuristic
   */
  public ReferenceBasedOutlierDetection(int k, DistanceFunction<V, D> distanceFunction, ReferencePointsHeuristic<V> refp) {
    super();
    this.k = k;
    this.distanceFunction = distanceFunction;
    this.refp = refp;
  }

  /**
   * Runs the algorithm in the timed evaluation part.
   * 
   */
  @Override
  protected OutlierResult runInTime(Database<V> database) throws IllegalStateException {
    DistanceQuery<V, D> distFunc = database.getDistanceQuery(distanceFunction);
    Collection<V> refPoints = refp.getReferencePoints(database);

    DBIDs ids = database.getIDs();
    // storage of distance/score values.
    WritableDataStore<Double> rbod_score = DataStoreUtil.makeStorage(ids, DataStoreFactory.HINT_STATIC, Double.class);
    // compute density for one reference point, to initialize the first density
    // value for each object

    double density = 0;
    V firstRef = refPoints.iterator().next();
    // compute distance vector for the first reference point
    List<DistanceResultPair<D>> firstReferenceDists = computeDistanceVector(firstRef, database, distFunc);
    // order ascending
    Collections.sort(firstReferenceDists);
    for(int l = 0; l < firstReferenceDists.size(); l++) {
      density = computeDensity(firstReferenceDists, l);
      rbod_score.put(firstReferenceDists.get(l).getID(), density);
    }
    // compute density values for all remaining reference points
    for(V refPoint : refPoints) {
      List<DistanceResultPair<D>> referenceDists = computeDistanceVector(refPoint, database, distFunc);
      // order ascending
      Collections.sort(referenceDists);
      // compute density value for each object
      for(int l = 0; l < referenceDists.size(); l++) {
        density = computeDensity(referenceDists, l);
        if(density < rbod_score.get(referenceDists.get(l).getID())) {
          rbod_score.put(referenceDists.get(l).getID(), density);
        }
      }
    }
    // compute maximum density
    double maxDensity = 0.0;
    for(DBID id : database.getIDs()) {
      double dens = rbod_score.get(id);
      if(dens > maxDensity) {
        maxDensity = dens;
      }
    }
    // compute REFOD_SCORE
    for(DBID id : database) {
      double score = 1 - (rbod_score.get(id) / maxDensity);
      rbod_score.put(id, score);
    }

    // adds reference points to the result. header information for the
    // visualizer to find the reference points in the result
    ReferencePointsResult<V> refp = new ReferencePointsResult<V>("Reference points", "reference-points", refPoints);

    AnnotationResult<Double> scoreResult = new AnnotationFromDataStore<Double>("Reference-points Outlier Scores", "reference-outlier", REFOD_SCORE, rbod_score);
    OutlierScoreMeta scoreMeta = new BasicOutlierScoreMeta(0.0, 1.0, 0.0, 1.0, 0.0);
    OutlierResult result = new OutlierResult(scoreMeta, scoreResult);
    result.addChildResult(refp);
    return result;
  }

  /**
   * Computes for each object the distance to one reference point. (one
   * dimensional representation of the data set)
   * 
   * @param refPoint Reference Point Feature Vector
   * @param database database to work on
   * @param distFunc Distance function to use
   * @return array containing the distance to one reference point for each
   *         database object and the object id
   */
  public List<DistanceResultPair<D>> computeDistanceVector(V refPoint, Database<V> database, DistanceQuery<V, D> distFunc) {
    List<DistanceResultPair<D>> referenceDists = new ArrayList<DistanceResultPair<D>>(database.size());
    int counter = 0;
    for(Iterator<DBID> iter = database.iterator(); iter.hasNext(); counter++) {
      DBID id = iter.next();
      DistanceResultPair<D> referenceDist = new DistanceResultPair<D>(distFunc.distance(id, refPoint), id);
      referenceDists.add(counter, referenceDist);
    }
    return referenceDists;
  }

  /**
   * Computes the density of an object. The density of an object is the
   * distances to the k nearest neighbors. Neighbors and distances are computed
   * approximately. (approximation for kNN distance: instead of a normal NN
   * search the NN of an object are those objects that have a similar distance
   * to a reference point. The k- nearest neighbors of an object are those
   * objects that lay close to the object in the reference distance vector)
   * 
   * @param referenceDists vector of the reference distances,
   * @param index index of the current object
   * @return density for one object and reference point
   */
  public double computeDensity(List<DistanceResultPair<D>> referenceDists, int index) {
    double density = 0.0;
    DistanceResultPair<D> x = referenceDists.get(index);
    double xDist = x.getDistance().doubleValue();

    int j = 0;
    int n = index - 1;
    int m = index + 1;
    while(j < k) {
      double mdist = 0;
      double ndist = 0;
      if(n >= 0) {
        ndist = referenceDists.get(n).getDistance().doubleValue();
        if(m < referenceDists.size()) {
          mdist = referenceDists.get(m).getDistance().doubleValue();
          if(Math.abs(ndist - xDist) < Math.abs(mdist - xDist)) {
            density += Math.abs(ndist - xDist);
            n--;
            j++;
          }
          else {
            density += Math.abs(mdist - xDist);
            m++;
            j++;
          }
        }
        else {
          density += Math.abs(ndist - xDist);
          n--;
          j++;
        }
      }
      else if(m < referenceDists.size()) {
        mdist = referenceDists.get(m).getDistance().doubleValue();
        density += Math.abs(mdist - xDist);
        m++;
        j++;
      }
      else {
        throw new IndexOutOfBoundsException();
      }
    }

    double densityDegree = 1.0 / ((1.0 / k) * density);

    return densityDegree;
  }

  /**
   * Factory method for {@link Parameterizable}
   * 
   * @param config Parameterization
   * @return KNN outlier detection algorithm
   */
  public static <V extends NumberVector<V, ?>, D extends NumberDistance<D, ?>> ReferenceBasedOutlierDetection<V, D> parameterize(Parameterization config) {
    int k = getParameterK(config);
    DistanceFunction<V, D> distanceFunction = getParameterDistanceFunction(config, EuclideanDistanceFunction.class, PrimitiveDistanceFunction.class);
    ReferencePointsHeuristic<V> refp = getParameterReferencePoints(config);

    if(config.hasErrors()) {
      return null;
    }
    return new ReferenceBasedOutlierDetection<V, D>(k, distanceFunction, refp);
  }

  /**
   * Get the reference points parameter
   * 
   * @param config Parameterization
   * @return reference points parameter
   */
  protected static <V extends NumberVector<?, ?>> ReferencePointsHeuristic<V> getParameterReferencePoints(Parameterization config) {
    final ObjectParameter<ReferencePointsHeuristic<V>> param = new ObjectParameter<ReferencePointsHeuristic<V>>(REFP_ID, ReferencePointsHeuristic.class, GridBasedReferencePoints.class);
    if(config.grab(param)) {
      return param.instantiateClass(config);
    }
    return null;
  }

  /**
   * Get the k parameter for the knn query
   * 
   * @param config Parameterization
   * @return k parameter
   */
  protected static int getParameterK(Parameterization config) {
    final IntParameter param = new IntParameter(K_ID, new GreaterConstraint(1));
    if(config.grab(param)) {
      return param.getValue();
    }
    return -1;
  }

  @Override
  protected Logging getLogger() {
    return logger;
  }
}