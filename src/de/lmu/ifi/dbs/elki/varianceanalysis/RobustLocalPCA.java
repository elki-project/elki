package de.lmu.ifi.dbs.elki.varianceanalysis;

import de.lmu.ifi.dbs.elki.data.RealVector;
import de.lmu.ifi.dbs.elki.distance.DoubleDistance;
import de.lmu.ifi.dbs.elki.database.Database;
import de.lmu.ifi.dbs.elki.math.linearalgebra.Matrix;
import de.lmu.ifi.dbs.elki.properties.Properties;
import de.lmu.ifi.dbs.elki.varianceanalysis.weightfunctions.ConstantWeight;
import de.lmu.ifi.dbs.elki.varianceanalysis.weightfunctions.WeightFunction;
import de.lmu.ifi.dbs.elki.utilities.QueryResult;
import de.lmu.ifi.dbs.elki.utilities.Util;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.ClassParameter;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.OptionID;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.ParameterException;
import java.util.Collection;
import java.util.Iterator;

/**
 * Compute weighted PCA on the given data. 
 * 
 * @author Erich Schubert
 */
public class RobustLocalPCA<V extends RealVector<V, ?>> extends LocalPCA<V> {
  /**
   * OptionID for {@link #WEIGHT_PARAM}
   */
  public static final OptionID WEIGHT_ID = OptionID.getOrCreateOptionID(
      "pca.weight",
      "Classname of the weight function to use in PCA " + Properties.KDD_FRAMEWORK_PROPERTIES.restrictionString(WeightFunction.class) +
      ".");

  /**
   * Parameter to specify the weight function to use in weighted PCA, must
   * extend {@link de.lmu.ifi.dbs.elki.varianceanalysis.weightfunction}.
   * <p>
   * Key: {@code -pca.weight}
   * </p>
   */
  private final ClassParameter<WeightFunction> WEIGHT_PARAM = new ClassParameter<WeightFunction>(
      WEIGHT_ID,
      WeightFunction.class,
      ConstantWeight.class.getName());

  /**
   * Holds the weight function.
   */
  public WeightFunction<V> weightfunction;

  public RobustLocalPCA() {
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
   * Returns the covariance matrix of the specified ids.
   * FIXME: with hard-coded euclidean distance
   * 
   * @see LocalPCA#pcaMatrix(de.lmu.ifi.dbs.elki.database.Database, java.util.Collection)
   */
  public Matrix pcaMatrix(Database<V> database, Collection<Integer> ids) {
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
        // FIXME: this is a hardcoded Euclidean distance.
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
      // FIXME: hard coded distance... make parametrizable.
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
    // FIXME: if weightsum == 0.0, the matrix will be empty,
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
   * Returns the covariance matrix of the specified ids.
   * 
   * @see LocalPCA#pcaMatrixResults(de.lmu.ifi.dbs.elki.database.Database, java.util.Collection)
   */
  public Matrix pcaMatrixResults(Database<V> database, Collection<QueryResult<DoubleDistance>> results) {
    return pcaMatrixResults(database, results, results.size());
  }

  /**
   * Returns the covariance matrix of the specified ids.
   * 
   * @see LocalPCA#pcaMatrixResults(de.lmu.ifi.dbs.elki.database.Database, java.util.Collection)
   */
  public Matrix pcaMatrixResults(Database<V> database, Collection<QueryResult<DoubleDistance>> results, int len) {
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
      for(Iterator<QueryResult<DoubleDistance>> it = results.iterator(); it.hasNext() && i < len; i++) {
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
    for(Iterator<QueryResult<DoubleDistance>> it = results.iterator(); it.hasNext() && i < len; i++) {
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