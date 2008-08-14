package de.lmu.ifi.dbs.elki.varianceanalysis;

import java.util.Collection;
import java.util.Iterator;

import de.lmu.ifi.dbs.elki.data.RealVector;
import de.lmu.ifi.dbs.elki.database.Database;
import de.lmu.ifi.dbs.elki.distance.DoubleDistance;
import de.lmu.ifi.dbs.elki.math.linearalgebra.Matrix;
import de.lmu.ifi.dbs.elki.properties.Properties;
import de.lmu.ifi.dbs.elki.utilities.QueryResult;
import de.lmu.ifi.dbs.elki.utilities.UnableToComplyException;
import de.lmu.ifi.dbs.elki.utilities.Util;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.ClassParameter;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.OptionID;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.ParameterException;
import de.lmu.ifi.dbs.elki.varianceanalysis.weightfunctions.ConstantWeight;
import de.lmu.ifi.dbs.elki.varianceanalysis.weightfunctions.WeightFunction;

public class WeightedCovarianceMatrixBuilder<V extends RealVector<V, ?>> extends CovarianceMatrixBuilder<V> {
  /**
   * OptionID for {@link #WEIGHT_PARAM}
   */
  public static final OptionID WEIGHT_ID = OptionID.getOrCreateOptionID("pca.weight", "Classname of the weight function to use in PCA " + Properties.KDD_FRAMEWORK_PROPERTIES.restrictionString(WeightFunction.class) + ".");

  /**
   * Parameter to specify the weight function to use in weighted PCA, must
   * extend {@link de.lmu.ifi.dbs.elki.varianceanalysis.weightfunction}.
   * <p>
   * Key: {@code -pca.weight}
   * </p>
   */
  private final ClassParameter<WeightFunction> WEIGHT_PARAM = new ClassParameter<WeightFunction>(WEIGHT_ID, WeightFunction.class, ConstantWeight.class.getName());

  /**
   * Holds the weight function.
   */
  public WeightFunction<V> weightfunction;

  /**
   * 
   */
  public WeightedCovarianceMatrixBuilder() {
    super();
    addOption(WEIGHT_PARAM);
  }

  /**
   * @see de.lmu.ifi.dbs.elki.utilities.optionhandling.Parameterizable#setParameters(String[])
   */
  @SuppressWarnings("unchecked")
  public String[] setParameters(String[] args) throws ParameterException {
    String[] remainingParameters = super.setParameters(args);
    weightfunction = WEIGHT_PARAM.instantiateClass();
    setParameters(args, remainingParameters);
    return remainingParameters;
  }

  /**
   * Weighted Covariance Matrix for a set of IDs. Since we are not supplied any
   * distance information, we'll need to compute it ourselves. Covariance is
   * tied to Euklidean distance, so it probably does not make much sense to add
   * support for other distance functions?
   */
  public Matrix processIds(Collection<Integer> ids, Database<V> database) {
    int dim = database.dimensionality();
    // collecting the sums in each dimension
    double[] sums = new double[dim];
    // collecting the products of any two dimensions
    double[][] squares = new double[dim][dim];
    // for collecting weights
    double weightsum = 0.0;
    // get centroid
    V centroid = Util.centroid((Database<V>) database, ids);

    // find maximum distance
    double maxdist = 0.0;
    double stddev = 0.0;
    {
      for(Iterator<Integer> it = ids.iterator(); it.hasNext();) {
        V obj = database.get(it.next());
        double distance = 0.0;
        // TODO: this is a hardcoded Euklidean distance.
        for(int d = 0; d < dim; d++) {
          double delta = centroid.getValue(d + 1).doubleValue() - obj.getValue(d + 1).doubleValue();
          distance += delta * delta;
        }
        stddev += distance; // still squared distance!
        distance = java.lang.Math.sqrt(distance);
        if(distance > maxdist)
          maxdist = distance;
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
    // TODO: if weightsum == 0.0, the matrix will be empty,
    // do we need to manually default to identity matrix then?
    assert (weightsum > 0.0);
    for(int d1 = 0; d1 < dim; d1++) {
      for(int d2 = d1; d2 < dim; d2++) {
        // squares[d1][d2] = squares[d1][d2] / weightsum - (sums[d1] /
        // weightsum) * (sums[d2] / weightsum);
        squares[d1][d2] = squares[d1][d2] - sums[d1] * sums[d2] / weightsum;
        // use symmetry
        squares[d2][d1] = squares[d1][d2];
      }
    }
    return new Matrix(squares);
  }

  /**
   * Compute Covariance Matrix for a QueryResult Collection
   * 
   * By default it will just collect the ids and run processIds
   * 
   * @param results a collection of QueryResults
   * @param database the database used
   * @return Covariance Matrix
   */
  public Matrix processQueryResults(Collection<QueryResult<DoubleDistance>> results, Database<V> database) {
    int dim = database.dimensionality();
    // collecting the sums in each dimension
    double[] sums = new double[dim];
    // collecting the products of any two dimensions
    double[][] squares = new double[dim][dim];
    // for collecting weights
    double weightsum = 0.0;

    // find maximum distance
    double maxdist = 0.0;
    double stddev = 0.0;
    {
      int i = 0;
      for(Iterator<QueryResult<DoubleDistance>> it = results.iterator(); it.hasNext() && i < results.size(); i++) {
        QueryResult<DoubleDistance> res = it.next();
        double dist = res.getDistance().getValue();
        stddev += dist * dist;
        if(dist > maxdist)
          maxdist = dist;
      }
      if(maxdist == 0.0)
        maxdist = 1.0;
      stddev = Math.sqrt(stddev / results.size());
    }

    // calculate weighted PCA
    int i = 0;
    for(Iterator<QueryResult<DoubleDistance>> it = results.iterator(); it.hasNext() && i < results.size(); i++) {
      QueryResult<DoubleDistance> res = it.next();
      V obj = database.get(res.getID());
      double weight = weightfunction.getWeight(res.getDistance().getValue(), maxdist, stddev);
      for(int d1 = 0; d1 < dim; d1++) {
        /* We're exploiting symmetry here, start with d2 == d1 */
        for(int d2 = d1; d2 < dim; d2++) {
          squares[d1][d2] += obj.getValue(d1 + 1).doubleValue() * obj.getValue(d2 + 1).doubleValue() * weight;
        }
        sums[d1] += obj.getValue(d1 + 1).doubleValue() * weight;
      }
      weightsum += weight;
    }
    for(int d1 = 0; d1 < dim; d1++) {
      for(int d2 = d1; d2 < dim; d2++) {
        // squares[d1][d2] = squares[d1][d2] / weightsum - (sums[d1] /
        // weightsum) * (sums[d2] / weightsum);
        squares[d1][d2] = squares[d1][d2] - sums[d1] * sums[d2] / weightsum;
        // use symmetry
        squares[d2][d1] = squares[d1][d2];
      }
    }
    return new Matrix(squares);
  }
}
