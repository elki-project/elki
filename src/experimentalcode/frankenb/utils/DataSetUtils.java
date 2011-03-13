/**
 * 
 */
package experimentalcode.frankenb.utils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

import de.lmu.ifi.dbs.elki.data.IntegerVector;
import de.lmu.ifi.dbs.elki.data.NumberVector;
import de.lmu.ifi.dbs.elki.utilities.pairs.Pair;
import experimentalcode.frankenb.log.Log;
import experimentalcode.frankenb.model.ListDataSet;
import experimentalcode.frankenb.model.ReferenceDataSet;
import experimentalcode.frankenb.model.ifaces.IDataSet;

/**
 * No description given.
 * 
 * @author Florian Frankenberger
 */
public final class DataSetUtils {

  private DataSetUtils() {}
  
  /**
   * Splits a data set according to the dimension and position into two data sets. If dimension
   * is 1 the data gets split into two data sets where all points with x < position get into one
   * data set and all points >= position get into the other
   * 
   * @param dataSet
   * @param dimension
   * @param position
   */
  public static Pair<IDataSet, IDataSet> split(IDataSet dataSet, int dimension, double position) {
    ReferenceDataSet dataSetLower = new ReferenceDataSet(dataSet.getOriginal());
    ReferenceDataSet dataSetHigher = new ReferenceDataSet(dataSet.getOriginal());
    for (int id : dataSet.getIDs()) {
      NumberVector<?, ?> vector = dataSet.get(id);
      if (vector.doubleValue(dimension) < position) {
        dataSetLower.add(id);
      } else {
        dataSetHigher.add(id);
      }
    }
    return new Pair<IDataSet, IDataSet>(dataSetLower, dataSetHigher);
  }
  
  /**
   * Returns the median of a data set by using a sampling method.
   * 
   * @param dataSet
   * @param dimension
   * @return
   */
  public static double quickMedian(IDataSet dataSet, int dimension, int numberOfSamples) {
    int everyNthItem = (int) Math.max(1, Math.floor(dataSet.getSize() / (double) numberOfSamples));
    
    int counter = 0;
    List<Double> list = new ArrayList<Double>();
    for (int id : dataSet.getIDs()) {
      if (counter++ % everyNthItem == 0) {
        NumberVector<?, ?> vector = dataSet.get(id);
        list.add(vector.doubleValue(dimension));
      }
    }
    
    Collections.sort(list);
    Log.debug(list.toString());
    double median;
    if (list.size() == 1) {
      return list.get(0);
    } else
    if (list.size() % 2 == 1) {
      median = list.get(((list.size() + 1) / 2) - 1);
    } else {
      double v1 = list.get(list.size() / 2);
      double v2 = list.get((list.size() / 2) - 1);
      median = (v1 + v2) / 2.0;
    }
    return median;
  }
  
  public static double median(IDataSet dataSet, int dimension) {
    return quickMedian(dataSet, dimension, dataSet.getSize());
  }
  
  public static IDataSet createRandomDataSet(int n, int dimensions, int minValue, int maxValue) {
    Set<NumberVector<?, ?>> usedItems = new HashSet<NumberVector<?, ?>>(n);
    Random random = new Random(System.currentTimeMillis());

    List<NumberVector<?, ?>> items = new ArrayList<NumberVector<?, ?>>();
    for (int i = 0; i < n; ++i) {
      IntegerVector vector = null;
      do {
        int[] values = new int[dimensions];
        for (int j = 0; j < dimensions; ++j) {
          values[j] = (int)Math.round((random.nextDouble() * (maxValue - minValue)) + minValue);
        }
        vector = new IntegerVector(values);
      } while (usedItems.contains(vector));
      
      usedItems.add(vector);
      items.add(vector);
    }
    
    return new ListDataSet(dimensions, items);
  }
  
}
