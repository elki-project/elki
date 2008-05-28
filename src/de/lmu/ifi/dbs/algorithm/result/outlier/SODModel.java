package de.lmu.ifi.dbs.algorithm.result.outlier;

import de.lmu.ifi.dbs.algorithm.result.AbstractResult;
import de.lmu.ifi.dbs.data.RealVector;
import de.lmu.ifi.dbs.database.Database;
import de.lmu.ifi.dbs.distance.distancefunction.DimensionsSelectingEuklideanDistanceFunction;
import de.lmu.ifi.dbs.normalization.Normalization;
import de.lmu.ifi.dbs.utilities.UnableToComplyException;
import de.lmu.ifi.dbs.utilities.Util;
import de.lmu.ifi.dbs.utilities.optionhandling.AttributeSettings;

import java.io.PrintStream;
import java.util.BitSet;
import java.util.Iterator;
import java.util.List;

/**
 * @author Arthur Zimek
 */
public class SODModel<O extends RealVector<O,Double>> extends AbstractResult<O> {

  private final DimensionsSelectingEuklideanDistanceFunction<O> DISTANCE_FUNCTION = new DimensionsSelectingEuklideanDistanceFunction<O>();
  
  private double[] centerValues;
  
  private double[] variances;
  
  private double expectationOfVariance;
  
  private BitSet weightVector;
  
  private double sod;
  
  public SODModel(Database<O> database, List<Integer> neighborhood, double alpha, O queryObject){
    super(database);
    centerValues = new double[database.dimensionality()];
    variances = new double[centerValues.length];
    for(Iterator<Integer> iter = neighborhood.iterator(); iter.hasNext();){
      O databaseObject = database.get(iter.next());
      for(int d = 0; d < centerValues.length; d++){
        centerValues[d] += databaseObject.getValue(d+1);
      }
    }
    for(int d = 0; d < centerValues.length; d++){
      centerValues[d] /= neighborhood.size();
    }
    for(Iterator<Integer> iter = neighborhood.iterator(); iter.hasNext();){
      O databaseObject = database.get(iter.next());
      for(int d = 0; d < centerValues.length; d++){
        // distance
        double distance = centerValues[d] - databaseObject.getValue(d+1);
        // variance
        variances[d] += distance * distance; 
      }
    }
    expectationOfVariance = 0;
    for(int d = 0; d < variances.length; d++){
      variances[d] /= neighborhood.size();
      expectationOfVariance += variances[d];
    }
    expectationOfVariance /= variances.length;
    weightVector = new BitSet(variances.length);
    for(int d = 0; d < variances.length; d++){
      if(variances[d] < alpha * expectationOfVariance){
        weightVector.set(d,true);
      }
    }
    DISTANCE_FUNCTION.setSelectedDimensions(weightVector);
    sod = subspaceOutlierDegree(queryObject);
  }
  
  public double subspaceOutlierDegree(O queryObject){
    O center = queryObject.newInstance(centerValues);
    double distance = DISTANCE_FUNCTION.distance(queryObject, center).getDoubleValue();
    distance /= weightVector.cardinality();
    return distance;
  }
  
  
  /**
   * 
   * @see de.lmu.ifi.dbs.algorithm.result.Result#output(java.io.PrintStream, de.lmu.ifi.dbs.normalization.Normalization, java.util.List)
   */
  public void output(PrintStream outStream, Normalization<O> normalization, List<AttributeSettings> settings) throws UnableToComplyException {
    outStream.println("### " + this.getClass().getSimpleName() + ":");
    outStream.println("### relevant attributes (counting starts with 0): "+this.weightVector.toString());
    outStream.println("### center of neighborhood: "+ Util.format(centerValues));
    outStream.println("### subspace outlier degree: "+this.sod);
    outStream.println("################################################################################");
    outStream.flush();
  }

  public double getSod() {
    return this.sod;
  }

}
