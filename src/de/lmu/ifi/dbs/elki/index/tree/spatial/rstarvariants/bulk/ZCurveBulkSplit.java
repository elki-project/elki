package de.lmu.ifi.dbs.elki.index.tree.spatial.rstarvariants.bulk;
/*
This file is part of ELKI:
Environment for Developing KDD-Applications Supported by Index-Structures

Copyright (C) 2011
Ludwig-Maximilians-Universität München
Lehr- und Forschungseinheit für Datenbanksysteme
ELKI Development Team

This program is free software: you can redistribute it and/or modify
it under the terms of the GNU Affero General Public License as published by
the Free Software Foundation, either version 3 of the License, or
(at your option) any later version.

This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU Affero General Public License for more details.

You should have received a copy of the GNU Affero General Public License
along with this program.  If not, see <http://www.gnu.org/licenses/>.
*/

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import de.lmu.ifi.dbs.elki.data.spatial.SpatialComparable;
import de.lmu.ifi.dbs.elki.logging.Logging;
import de.lmu.ifi.dbs.elki.math.spacefillingcurves.ZCurve;

/**
 * Bulk split that orders object by their Z curve position, then splits them
 * into pages accordingly.
 * 
 * @author Elke Achtert
 */
public class ZCurveBulkSplit extends AbstractBulkSplit {
  /**
   * Logger.
   */
  private static final Logging logger = Logging.getLogger(ZCurveBulkSplit.class);

  /**
   * Constructor
   */
  public ZCurveBulkSplit() {
    // Nothing to do
  }

  /**
   * Partitions the specified feature vectors
   * 
   * @param spatialObjects the spatial objects to be partitioned
   * @param minEntries the minimum number of entries in a partition
   * @param maxEntries the maximum number of entries in a partition
   * @param <N> object type
   * @return the partition of the specified spatial objects
   */
  @Override
  public <N extends SpatialComparable> List<List<N>> partition(List<N> spatialObjects, int minEntries, int maxEntries) {
    List<List<N>> partitions = new ArrayList<List<N>>();
    List<N> objects = new ArrayList<N>(spatialObjects);

    // one dimensional special case
    if(spatialObjects.size() > 0 && spatialObjects.get(0).getDimensionality() == 1) {
      // TODO: move this Comparator into shared code.
      Collections.sort(objects, new Comparator<SpatialComparable>() {
        @Override
        public int compare(SpatialComparable o1, SpatialComparable o2) {
          return Double.compare(o1.getMin(1), o2.getMin(1));
        }
      });

      // build partitions
      // reinitialize array with correct size. Array will not use more space
      // than necessary.
      int numberPartitions = (int) Math.ceil(1d * spatialObjects.size() / maxEntries);
      partitions = new ArrayList<List<N>>(numberPartitions);
      List<N> onePartition = null;
      for(N o : objects) {
        if(onePartition == null || onePartition.size() >= maxEntries) {
          onePartition = new ArrayList<N>(maxEntries);
          partitions.add(onePartition);
        }
        onePartition.add(o);
      }

      // okay, check last partition for underfill
      // only check if there is more than 1 partition
      if(partitions.size() > 1) {
        List<N> last = partitions.get(partitions.size() - 1);
        List<N> nextToLast = partitions.get(partitions.size() - 2);
        while(last.size() < minEntries) {
          last.add(0, nextToLast.remove(nextToLast.size() - 1));
        }
      }
      return partitions;
    }

    // get z-values
    List<double[]> valuesList = new ArrayList<double[]>();
    for(SpatialComparable o : spatialObjects) {
      double[] values = new double[o.getDimensionality()];
      for(int d = 0; d < o.getDimensionality(); d++) {
        values[d] = o.getMin(d + 1);
      }
      valuesList.add(values);
    }
    if(logger.isDebugging()) {
      logger.debugFine(valuesList.toString());
    }
    List<byte[]> zValuesList = ZCurve.zValues(valuesList);

    // map z-values
    final Map<SpatialComparable, byte[]> zValues = new HashMap<SpatialComparable, byte[]>();
    for(int i = 0; i < spatialObjects.size(); i++) {
      SpatialComparable o = spatialObjects.get(i);
      byte[] zValue = zValuesList.get(i);
      zValues.put(o, zValue);
    }

    // create a comparator
    Comparator<SpatialComparable> comparator = new Comparator<SpatialComparable>() {
      @Override
      public int compare(SpatialComparable o1, SpatialComparable o2) {
        byte[] z1 = zValues.get(o1);
        byte[] z2 = zValues.get(o2);

        for(int i = 0; i < z1.length; i++) {
          byte z1_i = z1[i];
          byte z2_i = z2[i];
          if(z1_i < z2_i) {
            return -1;
          }
          else if(z1_i > z2_i) {
            return +1;
          }
        }
        if(o1 instanceof Comparable) {
          try {
            @SuppressWarnings("unchecked")
            final Comparable<Object> comparable = (Comparable<Object>) o1;
            return comparable.compareTo(o2);
          }
          catch(ClassCastException e) {
            // ignore
          }
        }
        return 0;
      }
    };
    Collections.sort(objects, comparator);

    // insert into partition
    while(objects.size() > 0) {
      StringBuffer msg = new StringBuffer();
      int splitPoint = chooseBulkSplitPoint(objects.size(), minEntries, maxEntries);
      List<N> partition1 = new ArrayList<N>();
      for(int i = 0; i < splitPoint; i++) {
        N o = objects.remove(0);
        partition1.add(o);
      }
      partitions.add(partition1);

      // copy array
      if(logger.isDebugging()) {
        msg.append("\ncurrent partition " + partition1);
        msg.append("\nremaining objects # ").append(objects.size());
        logger.debugFine(msg.toString());
      }
    }

    if(logger.isDebugging()) {
      logger.debugFine("partitions " + partitions);
    }
    return partitions;
  }
}
