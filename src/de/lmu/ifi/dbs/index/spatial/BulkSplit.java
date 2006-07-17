package de.lmu.ifi.dbs.index.spatial;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import de.lmu.ifi.dbs.logging.AbstractLoggable;
import de.lmu.ifi.dbs.logging.LoggingConfiguration;
import de.lmu.ifi.dbs.math.spacefillingcurves.ZCurve;

/**
 * Encapsulates the required parameters for a bulk split of a spatial index.
 *
 * @author Elke Achtert (<a
 *         href="mailto:achtert@dbs.ifi.lmu.de">achtert@dbs.ifi.lmu.de</a>)
 */
public class BulkSplit extends AbstractLoggable {
  /**
   * Available strategies for bulk loading.
   */
  public enum Strategy {
    ZCURVE,
    MAX_EXTENSION
  }

//  /**
//   * Holds the class specific debug status.
//   */
//  private static boolean DEBUG = LoggingConfiguration.DEBUG;
////  private static boolean DEBUG = true;
//
//  /**
//   * The logger of this class.
//   */
//  private Logger logger = Logger.getLogger(this.getClass().getName());

  public BulkSplit(){
	  super(LoggingConfiguration.DEBUG);
  }
  
  /**
   * Partitions the specified feature vectors according to the chosen strategy.
   *
   * @param spatialObjects the spatial objects to be partioned
   * @param minEntries     the minimum number of entries in a partition
   * @param maxEntries     the maximum number of entries in a partition
   * @param strategy       the bulk load strategy
   * @return the partition of the specified spatial objects according to the chosen strategy
   */
  public List<List<SpatialObject>> partition(List<SpatialObject> spatialObjects, int minEntries, int maxEntries,
                                             Strategy strategy) {
    if (strategy == Strategy.MAX_EXTENSION) {
      return maximalExtensionPartition(spatialObjects, minEntries, maxEntries);
    }

    else if (strategy == Strategy.ZCURVE) {
      return zValuePartition(spatialObjects, minEntries, maxEntries);
    }

    else throw new IllegalArgumentException("Unknown bulk load strategy!");
  }

  /**
   * Partitions the specified feature vectors where the split axes are the
   * dimensions with maximum extension
   *
   * @param spatialObjects the spatial objects to be partioned
   * @param minEntries     the minimum number of entries in a partition
   * @param maxEntries     the maximum number of entries in a partition
   * @return the partition of the specified spatial objects
   */
  private List<List<SpatialObject>> maximalExtensionPartition(List<SpatialObject> spatialObjects,
                                                              int minEntries, int maxEntries) {
    List<List<SpatialObject>> partitions = new ArrayList<List<SpatialObject>>();
    List<SpatialObject> objects = new ArrayList<SpatialObject>(spatialObjects);

    while (objects.size() > 0) {
      StringBuffer msg = new StringBuffer();

      // get the split axis and split point
      int splitAxis = chooseMaximalExtendedSplitAxis(objects);
      int splitPoint = chooseBulkSplitPoint(objects.size(), minEntries, maxEntries);
      if (this.debug) {
        msg.append("\nsplitAxis ").append(splitAxis);
        msg.append("\nsplitPoint ").append(splitPoint);
      }

      // sort in the right dimension
      Collections.sort(objects, new SpatialComparator(splitAxis, SpatialComparator.MIN));

      // insert into partition
      List<SpatialObject> partition = new ArrayList<SpatialObject>();
      for (int i = 0; i < splitPoint; i++) {
        SpatialObject o = objects.remove(0);
        partition.add(o);
      }
      partitions.add(partition);

      // copy array
      if (this.debug) {
        msg.append("\ncurrent partition " + partition);
        msg.append("\nremaining objects # ").append(objects.size());
        debugFine(msg.toString());
//        logger.fine(msg.toString());
      }
    }

    if (this.debug) {
    	debugFine("\npartitions " + partitions);
//      logger.fine("\npartitions " + partitions);
    }
    return partitions;
  }

  /**
   * Partitions the spatial objects according to their z-values.
   *
   * @param spatialObjects the spatial objects to be partitioned
   * @param minEntries     the minimum number of entries in a partition
   * @param maxEntries     the maximum number of entries in a partition
   * @return A partition of the spatial objects according to their z-values
   */
  private List<List<SpatialObject>> zValuePartition(List<SpatialObject> spatialObjects, int minEntries, int maxEntries) {
    List<List<SpatialObject>> partitions = new ArrayList<List<SpatialObject>>();
    List<SpatialObject> objects = new ArrayList<SpatialObject>(spatialObjects);

    // get z-values
    List<double[]> valuesList = new ArrayList<double[]>();
    for (SpatialObject o : spatialObjects) {
      double[] values = new double[o.getDimensionality()];
      for (int d = 0; d < o.getDimensionality(); d++) {
        values[d] = o.getMin(d + 1);
      }
      valuesList.add(values);
    }
//    System.out.println(valuesList);
    List<byte[]> zValuesList = ZCurve.zValues(valuesList);

    // map z-values
    final Map<Integer, byte[]> zValues = new HashMap<Integer, byte[]>();
    for (int i = 0; i < spatialObjects.size(); i++) {
      SpatialObject o = spatialObjects.get(i);
      byte[] zValue = zValuesList.get(i);
      zValues.put(o.getID(), zValue);
    }

    // create a comparator
    Comparator<SpatialObject> comparator = new Comparator<SpatialObject>() {
      public int compare(SpatialObject o1, SpatialObject o2) {
        byte[] z1 = zValues.get(o1.getID());
        byte[] z2 = zValues.get(o1.getID());

        for (int i = 0; i < z1.length; i++) {
          byte z1_i = z1[i];
          byte z2_i = z2[i];
          if (z1_i < z2_i) return -1;
          else if (z1_i > z2_i) return +1;
        }
        return o1.getID() - o2.getID();
      }
    };
    Collections.sort(objects, comparator);

    // insert into partition
    while (objects.size() > 0) {
      StringBuffer msg = new StringBuffer();
      int splitPoint = chooseBulkSplitPoint(objects.size(), minEntries, maxEntries);
      List<SpatialObject> partition = new ArrayList<SpatialObject>();
      for (int i = 0; i < splitPoint; i++) {
        SpatialObject o = objects.remove(0);
        partition.add(o);
      }
      partitions.add(partition);

      // copy array
      if (this.debug) {
        msg.append("\ncurrent partition " + partition);
        msg.append("\nremaining objects # ").append(objects.size());
        debugFine(msg.toString());
//        logger.fine(msg.toString());
      }
    }

    if (this.debug) {
    	debugFine("\npartitions " + partitions);
//      logger.fine("\npartitions " + partitions);
    }
    return partitions;
  }

  /**
   * Computes and returns the best split axis. The best split axis
   * is the split axes with the maximal extension.
   *
   * @param objects the spatial objects to be splitted
   */
  private int chooseMaximalExtendedSplitAxis(List<SpatialObject> objects) {
    // maximum and minimum value for the extension
    int dimension = objects.get(0).getDimensionality();
    double[] maxExtension = new double[dimension];
    double[] minExtension = new double[dimension];
    Arrays.fill(minExtension, Double.MAX_VALUE);

    // compute min and max value in each dimension
    for (SpatialObject object : objects) {
      for (int d = 1; d <= dimension; d++) {
        double min, max;
        min = object.getMin(d);
        max = object.getMax(d);

        if (maxExtension[d - 1] < max)
          maxExtension[d - 1] = max;

        if (minExtension[d - 1] > min)
          minExtension[d - 1] = min;
      }
    }

    // set split axis to dim with maximal extension
    int splitAxis = -1;
    double max = 0;
    for (int d = 1; d <= dimension; d++) {
      double currentExtension = maxExtension[d - 1] - minExtension[d - 1];
      if (max < currentExtension) {
        max = currentExtension;
        splitAxis = d;
      }
    }
    return splitAxis;
  }

  /**
   * Computes and returns the best split point.
   *
   * @param numEntries the number of entries to be split
   * @param minEntries the number of minimum entries in the node to be split
   * @param maxEntries number of maximum entries in the node to be split
   */
  private int chooseBulkSplitPoint(int numEntries, int minEntries, int maxEntries) {
    if (numEntries < minEntries) {
      throw new IllegalArgumentException("numEntries < minEntries!");
    }

    if (numEntries <= maxEntries) {
      return numEntries;
    }

    else if (numEntries < maxEntries + minEntries) {
      return (numEntries - minEntries);
    }

    else {
      return maxEntries;
    }
  }
}
