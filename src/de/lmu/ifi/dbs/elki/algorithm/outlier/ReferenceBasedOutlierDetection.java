package de.lmu.ifi.dbs.elki.algorithm.outlier;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

import de.lmu.ifi.dbs.elki.algorithm.DistanceBasedAlgorithm;
import de.lmu.ifi.dbs.elki.data.NumberVector;
import de.lmu.ifi.dbs.elki.database.AssociationID;
import de.lmu.ifi.dbs.elki.database.Database;
import de.lmu.ifi.dbs.elki.database.DistanceResultPair;
import de.lmu.ifi.dbs.elki.distance.DoubleDistance;
import de.lmu.ifi.dbs.elki.result.AnnotationFromHashMap;
import de.lmu.ifi.dbs.elki.result.AnnotationResult;
import de.lmu.ifi.dbs.elki.result.OrderingFromHashMap;
import de.lmu.ifi.dbs.elki.result.OrderingResult;
import de.lmu.ifi.dbs.elki.result.ReferencePointsResult;
import de.lmu.ifi.dbs.elki.result.outlier.BasicOutlierScoreMeta;
import de.lmu.ifi.dbs.elki.result.outlier.OutlierResult;
import de.lmu.ifi.dbs.elki.result.outlier.OutlierScoreMeta;
import de.lmu.ifi.dbs.elki.utilities.documentation.Description;
import de.lmu.ifi.dbs.elki.utilities.documentation.Reference;
import de.lmu.ifi.dbs.elki.utilities.documentation.Title;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.OptionID;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.constraints.GreaterConstraint;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.Parameterization;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.IntParameter;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.ObjectParameter;
import de.lmu.ifi.dbs.elki.utilities.referencepoints.GridBasedReferencePoints;
import de.lmu.ifi.dbs.elki.utilities.referencepoints.ReferencePointsHeuristic;

/**
 * <p>
 * provides the Reference-Based Outlier Detection algorithm, an algorithm that
 * computes kNN distances approximately, using reference points. There are two
 * subclasses for this algorithm: One computes reference points that lay
 * randomly in the data space, the other one computes reference points that lay
 * on a grid in the data space.
 * </p>
 *<p>
 * Reference:<br>
 * Y. Pei, O. R. Zaiane, Y. Gao: An Efficient Reference-Based Approach to
 * Outlier Detection in Large Datasets.</br> In: Proc. IEEE Int. Conf. on Data
 * Mining (ICDM'06), Hong Kong, China, 2006.
 *</p>
 * 
 * @author Lisa Reichert
 * @author Erich Schubert
 * 
 * @param <V> a type of {@link NumberVector} as a suitable data object for this
 *        algorithm
 * @param <N> the type of the attributes of the feature vector
 */
@Title("An Efficient Reference-based Approach to Outlier Detection in Large Datasets")
@Description("Computes kNN distances approximately, using reference points with various reference point strategies.")
@Reference(authors = "Y. Pei, O.R.Zaiane, Y. Gao", title = "An Efficient Reference-based Approach to Outlier Detection in Large Datasets", booktitle = "Proc. 19th IEEE Int. Conf. on Data Engineering (ICDE '03), Bangalore, India, 2003", url = "http://dx.doi.org/10.1109/ICDM.2006.17")
public class ReferenceBasedOutlierDetection<V extends NumberVector<V, N>, N extends Number> extends DistanceBasedAlgorithm<V, DoubleDistance, OutlierResult> {
  /**
   * The association id to associate the REFOD_SCORE of an object for the
   * Reference based outlier detection algorithm.
   */
  public static final AssociationID<Double> REFOD_SCORE = AssociationID.getOrCreateAssociationID("REFOD_SCORE", Double.class);

  /**
   * OptionID for {@link #REFP_PARAM}
   */
  public static final OptionID REFP_ID = OptionID.getOrCreateOptionID("refod.refp", "The heuristic for finding reference points.");

  /**
   * Parameter for the reference points heuristic.
   * <p>
   * Key: {@code -refod.refp}
   * </p>
   */
  private final ObjectParameter<ReferencePointsHeuristic<V>> REFP_PARAM = new ObjectParameter<ReferencePointsHeuristic<V>>(REFP_ID, ReferencePointsHeuristic.class, GridBasedReferencePoints.class);

  /**
   * OptionID for {@link #K_PARAM}
   */
  public static final OptionID K_ID = OptionID.getOrCreateOptionID("refod.k", "The number of nearest neighbors");

  /**
   * Parameter to specify the number of nearest neighbors of an object, to be
   * considered for computing its REFOD_SCORE, must be an integer greater than
   * 1.
   * <p>
   * Key: {@code -refod.k}
   * </p>
   */
  private final IntParameter K_PARAM = new IntParameter(K_ID, new GreaterConstraint(1));

  /**
   * Holds the value of {@link #K_PARAM}.
   */
  private int k;

  /**
   * Stores the reference point strategy
   */
  private ReferencePointsHeuristic<V> refp;

  /**
   * Constructor, adhering to
   * {@link de.lmu.ifi.dbs.elki.utilities.optionhandling.Parameterizable}
   * 
   * @param config Parameterization
   */
  public ReferenceBasedOutlierDetection(Parameterization config) {
    super(config);
    // Reference point strategy
    if(config.grab(REFP_PARAM)) {
      refp = REFP_PARAM.instantiateClass(config);
    }
    // k nearest neighbors
    if(config.grab(K_PARAM)) {
      k = K_PARAM.getValue();
    }
  }

  /**
   * Runs the algorithm in the timed evaluation part.
   * 
   */
  @Override
  protected OutlierResult runInTime(Database<V> database) throws IllegalStateException {
    getDistanceFunction().setDatabase(database, isVerbose(), isTime());

    // storage of distance/score values.
    HashMap<Integer, Double> rbod_score = new HashMap<Integer, Double>(database.size());
    // compute density for one reference point, to initialize the first density
    // value for each object

    double density = 0;
    Collection<V> refPoints = computeReferencePoints(database);
    V firstRef = refPoints.iterator().next();
    // compute distance vector for the first reference point
    List<DistanceResultPair<DoubleDistance>> firstReferenceDists = computeDistanceVector(firstRef, database);
    // order ascending
    Collections.sort(firstReferenceDists);
    for(int l = 0; l < firstReferenceDists.size(); l++) {
      density = computeDensity(firstReferenceDists, l);
      rbod_score.put(firstReferenceDists.get(l).getID(), density);
    }
    // compute density values for all remaining reference points
    for(V refPoint : refPoints) {
      List<DistanceResultPair<DoubleDistance>> referenceDists = computeDistanceVector(refPoint, database);
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
    for(Integer id : database.getIDs()) {
      double dens = rbod_score.get(id);
      if(dens > maxDensity) {
        maxDensity = dens;
      }
    }
    // compute REFOD_SCORE
    for(Integer id : database) {
      double score = 1 - (rbod_score.get(id) / maxDensity);
      rbod_score.put(id, score);
    }

    // adds reference points to the result. header information for the
    // visualizer to find the reference points in the result
    ReferencePointsResult<V> refp = new ReferencePointsResult<V>(refPoints);

    AnnotationResult<Double> scoreResult = new AnnotationFromHashMap<Double>(REFOD_SCORE, rbod_score);
    OrderingResult orderingResult = new OrderingFromHashMap<Double>(rbod_score, true);
    OutlierScoreMeta scoreMeta = new BasicOutlierScoreMeta(0.0, 1.0, 0.0, 1.0, 0.0);
    OutlierResult result = new OutlierResult(scoreMeta, scoreResult, orderingResult);
    result.addResult(refp);
    return result;
  }

  /**
   * Computes the reference points.
   * 
   * @param database Database to build reference points for 
   * @return List of Reference Points
   */
  public Collection<V> computeReferencePoints(Database<V> database) {
    return refp.getReferencePoints(database);
  }

  /**
   * Computes for each object the distance to one reference point. (one
   * dimensional representation of the data set)
   * 
   * @param refPoint Reference Point Feature Vector
   * @param database
   * @return array containing the distance to one reference point for each
   *         database object and the object id
   */
  public List<DistanceResultPair<DoubleDistance>> computeDistanceVector(V refPoint, Database<V> database) {
    List<DistanceResultPair<DoubleDistance>> referenceDists = new ArrayList<DistanceResultPair<DoubleDistance>>(database.size());
    int counter = 0;
    for(Iterator<Integer> iter = database.iterator(); iter.hasNext(); counter++) {
      Integer id = iter.next();
      DistanceResultPair<DoubleDistance> referenceDist = new DistanceResultPair<DoubleDistance>(getDistanceFunction().distance(id, refPoint), id);
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
  public double computeDensity(List<DistanceResultPair<DoubleDistance>> referenceDists, int index) {
    double density = 0.0;
    DistanceResultPair<DoubleDistance> x = referenceDists.get(index);
    double xDist = x.getDistance().getValue();

    int j = 0;
    int n = index - 1;
    int m = index + 1;
    while(j < k) {
      double mdist = 0;
      double ndist = 0;
      if(n >= 0) {
        ndist = referenceDists.get(n).getDistance().getValue();
        if(m < referenceDists.size()) {
          mdist = referenceDists.get(m).getDistance().getValue();
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
        mdist = referenceDists.get(m).getDistance().getValue();
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
}