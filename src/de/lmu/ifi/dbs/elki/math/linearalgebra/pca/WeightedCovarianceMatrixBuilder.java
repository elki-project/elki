package de.lmu.ifi.dbs.elki.math.linearalgebra.pca;

import java.util.Collection;
import java.util.Iterator;
import java.util.List;

import de.lmu.ifi.dbs.elki.data.RealVector;
import de.lmu.ifi.dbs.elki.database.Database;
import de.lmu.ifi.dbs.elki.database.DistanceResultPair;
import de.lmu.ifi.dbs.elki.distance.DoubleDistance;
import de.lmu.ifi.dbs.elki.distance.NumberDistance;
import de.lmu.ifi.dbs.elki.distance.distancefunction.DistanceFunction;
import de.lmu.ifi.dbs.elki.distance.distancefunction.EuclideanDistanceFunction;
import de.lmu.ifi.dbs.elki.math.linearalgebra.Matrix;
import de.lmu.ifi.dbs.elki.math.linearalgebra.pca.weightfunctions.ConstantWeight;
import de.lmu.ifi.dbs.elki.math.linearalgebra.pca.weightfunctions.WeightFunction;
import de.lmu.ifi.dbs.elki.utilities.DatabaseUtil;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.ClassParameter;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.OptionID;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.ParameterException;

/**
 * {@link CovarianceMatrixBuilder} with weights.
 * 
 * This builder uses a weight function to weight points differently during build a covariance matrix.
 * Covariance can be canonically extended with weights, as shown in the article
 * 
 * A General Framework for Increasing the Robustness of PCA-Based Correlation Clustering Algorithms
 * Hans-Peter Kriegel and Peer Kr&ouml;ger and Erich Schubert and Arthur Zimek
 * In: Proc. 20th Int. Conf. on Scientific and Statistical Database Management (SSDBM), 2008, Hong Kong
 * Lecture Notes in Computer Science 5069, Springer
 * 
 * @author Erich Schubert
 *
 * @param <V> Vector class to use
 */
public class WeightedCovarianceMatrixBuilder<V extends RealVector<V, ?>, D extends NumberDistance<D,?>> extends CovarianceMatrixBuilder<V, D> {
  /**
   * OptionID for {@link #WEIGHT_PARAM}
   */
  public static final OptionID WEIGHT_ID = OptionID.getOrCreateOptionID("pca.weight", 
      "Weight function to use in weighted PCA.");

  /**
   * Parameter to specify the weight function to use in weighted PCA, must
   * implement {@link de.lmu.ifi.dbs.elki.math.linearalgebra.pca.weightfunctions.WeightFunction}.
   * <p>
   * Key: {@code -pca.weight}
   * </p>
   */
  private final ClassParameter<WeightFunction> WEIGHT_PARAM = 
    new ClassParameter<WeightFunction>(WEIGHT_ID, WeightFunction.class, 
        ConstantWeight.class.getName());

  /**
   * Holds the weight function.
   */
  public WeightFunction weightfunction;
  
  /**
   * Holds the distance function used for weight calculation
   */
  // TODO: make configureable
  private DistanceFunction<V, DoubleDistance> weightDistance = new EuclideanDistanceFunction<V>();

  /**
   * Constructor, setting up parameter.
   */
  public WeightedCovarianceMatrixBuilder() {
    super();
    addOption(WEIGHT_PARAM);
  }

  /**
   * Parse parameters.
   */
  @Override
  public List<String> setParameters(List<String> args) throws ParameterException {
    List<String> remainingParameters = super.setParameters(args);
    weightfunction = WEIGHT_PARAM.instantiateClass();
    rememberParametersExcept(args, remainingParameters);
    return remainingParameters;
  }

  /**
   * Weighted Covariance Matrix for a set of IDs. Since we are not supplied any
   * distance information, we'll need to compute it ourselves. Covariance is
   * tied to Euclidean distance, so it probably does not make much sense to add
   * support for other distance functions?
   */
  @Override
  public Matrix processIds(Collection<Integer> ids, Database<V> database) {
    int dim = database.dimensionality();
    // collecting the sums in each dimension
    double[] sums = new double[dim];
    // collecting the products of any two dimensions
    double[][] squares = new double[dim][dim];
    // for collecting weights
    double weightsum = 0.0;
    // get centroid
    V centroid = DatabaseUtil.centroid(database, ids);

    // find maximum distance
    double maxdist = 0.0;
    double stddev = 0.0;
    {
      for(Iterator<Integer> it = ids.iterator(); it.hasNext();) {
        V obj = database.get(it.next());
        double distance = weightDistance.distance(centroid, obj).getValue();
        stddev += distance * distance;
        if(distance > maxdist) maxdist = distance;
      }
      if(maxdist == 0.0)
        maxdist = 1.0;
      // compute standard deviation.
      stddev = Math.sqrt(stddev / ids.size());
    }

    int i = 0;
    for(Iterator<Integer> it = ids.iterator(); it.hasNext(); i++) {
      V obj = database.get(it.next());
      // TODO: hard coded distance... make parametrizable?
      double distance = 0.0;
      for(int d = 0; d < dim; d++) {
        double delta = centroid.getValue(d + 1).doubleValue() - obj.getValue(d + 1).doubleValue();
        distance += delta * delta;
      }
      distance = java.lang.Math.sqrt(distance);
      double weight = weightfunction.getWeight(distance, maxdist, stddev);
      for(int d1 = 0; d1 < dim; d1++) {
        /* We're exploiting symmetry here, start with d2 == d1 */
        for(int d2 = d1; d2 < dim; d2++) {
          squares[d1][d2] += obj.getValue(d1 + 1).doubleValue() * obj.getValue(d2 + 1).doubleValue() * weight;
        }
        sums[d1] += obj.getValue(d1 + 1).doubleValue() * weight;
      }
      weightsum += weight;
    }
    return new Matrix(finishCovarianceMatrix(sums, squares, weightsum));
  }

  /**
   * Compute Covariance Matrix for a QueryResult Collection
   * 
   * By default it will just collect the ids and run processIds
   * 
   * @param results a collection of QueryResults
   * @param database the database used
   * @param k number of elements to process
   * @return Covariance Matrix
   */
  @Override
  public Matrix processQueryResults(Collection<DistanceResultPair<D>> results, Database<V> database, int k) {
    int dim = database.dimensionality();
    // collecting the sums in each dimension
    double[] sums = new double[dim];
    // collecting the products of any two dimensions
    double[][] squares = new double[dim][dim];
    // for collecting weights
    double weightsum = 0.0;

    // avoid bad parameters
    if (k > results.size()) k = results.size();
    
    // find maximum distance
    double maxdist = 0.0;
    double stddev = 0.0;
    {
      int i = 0;
      for(Iterator<DistanceResultPair<D>> it = results.iterator(); it.hasNext() && i < k; i++) {
        DistanceResultPair<D> res = it.next();
        double dist = res.getDistance().getValue().doubleValue();
        stddev += dist * dist;
        if(dist > maxdist)
          maxdist = dist;
      }
      if(maxdist == 0.0)
        maxdist = 1.0;
      stddev = Math.sqrt(stddev / k);
    }

    // calculate weighted PCA
    int i = 0;
    for(Iterator<DistanceResultPair<D>> it = results.iterator(); it.hasNext() && i < k; i++) {
      DistanceResultPair<D> res = it.next();
      V obj = database.get(res.getID());
      double weight = weightfunction.getWeight(res.getDistance().getValue().doubleValue(), maxdist, stddev);
      for(int d1 = 0; d1 < dim; d1++) {
        /* We're exploiting symmetry here, start with d2 == d1 */
        for(int d2 = d1; d2 < dim; d2++) {
          squares[d1][d2] += obj.getValue(d1 + 1).doubleValue() * obj.getValue(d2 + 1).doubleValue() * weight;
        }
        sums[d1] += obj.getValue(d1 + 1).doubleValue() * weight;
      }
      weightsum += weight;
    }
    return new Matrix(finishCovarianceMatrix(sums, squares, weightsum));
  }

  /**
   * Finish the Covariance matrix in array "squares".
   * 
   * @param sums Sums of values.
   * @param squares Sums of squares. Contents are destroyed and replaced with Covariance Matrix!
   * @param weightsum Sum of weights.
   * @return modified squares array
   */
  private double[][] finishCovarianceMatrix(double[] sums, double[][] squares, double weightsum) {
    if (weightsum > 0) {
      // reasonable weights - finish up matrix.
      for(int d1 = 0; d1 < sums.length; d1++) {
        for(int d2 = d1; d2 < sums.length; d2++) {
          squares[d1][d2] = squares[d1][d2] - sums[d1] * sums[d2] / weightsum;
          // use symmetry
          squares[d2][d1] = squares[d1][d2];
        }
      }
    } else {
      // No weights = no data. Use identity.
      // TODO: Warn about a bad weight function? Fail?
      for(int d1 = 0; d1 < sums.length; d1++) {
        for(int d2 = d1 + 1; d2 < sums.length; d2++)
          squares[d1][d2] = 0;
        squares[d1][d1] = 1;
      }
    }
    return squares;
  }
}
