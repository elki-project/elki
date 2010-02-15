package de.lmu.ifi.dbs.elki.algorithm.outlier;

import java.util.BitSet;
import java.util.List;

import de.lmu.ifi.dbs.elki.data.NumberVector;
import de.lmu.ifi.dbs.elki.database.Database;
import de.lmu.ifi.dbs.elki.distance.distancefunction.subspace.DimensionsSelectingEuclideanDistanceFunction;
import de.lmu.ifi.dbs.elki.normalization.NonNumericFeaturesException;
import de.lmu.ifi.dbs.elki.result.textwriter.TextWriteable;
import de.lmu.ifi.dbs.elki.result.textwriter.TextWriterStream;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.EmptyParameterization;

//TODO: arthur comment

/**
 * 
 * 
 * @author Arthur Zimek
 * @param <O> the type of DatabaseObjects handled by this Result
 */
public class SODModel<O extends NumberVector<O, ?>> implements TextWriteable, Comparable<SODModel<?>> {
  private final DimensionsSelectingEuclideanDistanceFunction<O> DISTANCE_FUNCTION;

  private double[] centerValues;

  private O center;

  private double[] variances;

  private double expectationOfVariance;

  private BitSet weightVector;

  private double sod;

  /**
   * 
   * 
   * @param database
   * @param neighborhood
   * @param alpha
   * @param queryObject
   */
  public SODModel(Database<O> database, List<Integer> neighborhood, double alpha, O queryObject) {
    // TODO: store database link?
    centerValues = new double[database.dimensionality()];
    variances = new double[centerValues.length];
    for(Integer id : neighborhood) {
      O databaseObject = database.get(id);
      for(int d = 0; d < centerValues.length; d++) {
        centerValues[d] += databaseObject.doubleValue(d + 1);
      }
    }
    for(int d = 0; d < centerValues.length; d++) {
      centerValues[d] /= neighborhood.size();
    }
    for(Integer id : neighborhood) {
      O databaseObject = database.get(id);
      for(int d = 0; d < centerValues.length; d++) {
        // distance
        double distance = centerValues[d] - databaseObject.doubleValue(d + 1);
        // variance
        variances[d] += distance * distance;
      }
    }
    expectationOfVariance = 0;
    for(int d = 0; d < variances.length; d++) {
      variances[d] /= neighborhood.size();
      expectationOfVariance += variances[d];
    }
    expectationOfVariance /= variances.length;
    weightVector = new BitSet(variances.length);
    for(int d = 0; d < variances.length; d++) {
      if(variances[d] < alpha * expectationOfVariance) {
        weightVector.set(d, true);
      }
    }
    center = queryObject.newInstance(centerValues);
    sod = subspaceOutlierDegree(queryObject, center, weightVector);
    
    DISTANCE_FUNCTION = new DimensionsSelectingEuclideanDistanceFunction<O>(new EmptyParameterization());
  }

  /**
   * 
   * 
   * @param queryObject
   * @param center
   * @param weightVector
   * @return sod value
   */
  private double subspaceOutlierDegree(O queryObject, O center, BitSet weightVector) {
    DISTANCE_FUNCTION.setSelectedDimensions(weightVector);
    double distance = DISTANCE_FUNCTION.distance(queryObject, center).doubleValue();
    distance /= weightVector.cardinality();
    return distance;
  }

  /**
   * Return the SOD of the point.
   * 
   * @return sod value
   */
  public double getSod() {
    return this.sod;
  }

  @Override
  public void writeToText(TextWriterStream out, String label) {
    out.inlinePrint(label + "=" + this.sod);
    out.commentPrintLn(this.getClass().getSimpleName() + ":");
    out.commentPrintLn("relevant attributes (counting starts with 0): " + this.weightVector.toString());
    try {
      out.commentPrintLn("center of neighborhood: " + out.normalizationRestore(center).toString());
    }
    catch(NonNumericFeaturesException e) {
      e.printStackTrace();
    }
    out.commentPrintLn("subspace outlier degree: " + this.sod);
    out.commentPrintSeparator();
  }

  @Override
  public int compareTo(SODModel<?> o) {
    return Double.compare(this.getSod(), o.getSod());
  }

}
