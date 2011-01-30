/**
 * 
 */
package experimentalcode.frankenb.utils;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import de.lmu.ifi.dbs.elki.data.NumberVector;
import de.lmu.ifi.dbs.elki.utilities.pairs.Pair;
import experimentalcode.frankenb.model.ifaces.IDataSet;

/**
 * A convenient implementation of ZCurve.
 * 
 * @author Florian Frankenberger
 */
public class ZCurve {

  private ZCurve() {
  }
  
  /**
   * Projects the given dataSet to one dimensional space according to the zcurve pattern. Each vector is assigned
   * a BigInteger representing it's new value in 1D space.
   * 
   * @param dataSet a list of pairs of id and the projected 1D position as BigInteger
   * @return
   */
  public static List<Pair<Integer, BigInteger>> projectToZCurve(IDataSet dataSet) {
    double[] minValues = new double[dataSet.getDimensionality()];
    double[] maxValues = new double[dataSet.getDimensionality()];
    
    Arrays.fill(minValues, Double.POSITIVE_INFINITY);
    Arrays.fill(maxValues, Double.NEGATIVE_INFINITY);
    
    for (int id : dataSet.getIDs()) {
      NumberVector<?, ?> vector = dataSet.get(id);
      for (int dim = 0; dim < dataSet.getDimensionality(); ++dim) {
        double dimValue = vector.doubleValue(dim + 1);
        minValues[dim] = Math.min(minValues[dim], dimValue);
        maxValues[dim] = Math.max(maxValues[dim], dimValue);
      }
    }
    
    List<Pair<Integer, BigInteger>> zCurvePositions = new ArrayList<Pair<Integer, BigInteger>>();
    for (int id : dataSet.getIDs()) {
      NumberVector<?, ?> vector = dataSet.get(id);
      long[] longValueList = new long[dataSet.getDimensionality()];
      
      for (int dim = 0; dim < dataSet.getDimensionality(); ++dim) {
        double dimValue = vector.doubleValue(dim + 1);
        double minValue = minValues[dim];
        double maxValue = maxValues[dim];
        
        dimValue = (dimValue - minValue) / (maxValue - minValue);
        long longValue = (long) (dimValue * (Long.MAX_VALUE));
        longValueList[dim] = longValue;
      }

      byte[] bytes = new byte[Long.SIZE * dataSet.getDimensionality() * (Long.SIZE / Byte.SIZE)];
      int shiftCounter = 0;
      for (int i = 0; i < Long.SIZE; ++i) {
        for (int dim = 0; dim < dataSet.getDimensionality(); ++dim) {
          long byteValue = longValueList[dim];

          int localShift = shiftCounter % Byte.SIZE;
          bytes[(bytes.length - 1)  - (shiftCounter / Byte.SIZE)] |= ((byteValue >> i) & 0x01) << localShift;
          
          shiftCounter++;
        }
      }
      
      BigInteger total = new BigInteger(bytes);
      zCurvePositions.add(new Pair<Integer, BigInteger>(id, total));
    }    
    
    return zCurvePositions;
  }
  
}
