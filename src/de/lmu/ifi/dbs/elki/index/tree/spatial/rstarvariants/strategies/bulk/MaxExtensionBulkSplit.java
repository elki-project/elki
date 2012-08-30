package de.lmu.ifi.dbs.elki.index.tree.spatial.rstarvariants.strategies.bulk;

/*
 This file is part of ELKI:
 Environment for Developing KDD-Applications Supported by Index-Structures

 Copyright (C) 2012
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
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import de.lmu.ifi.dbs.elki.data.spatial.SpatialComparable;
import de.lmu.ifi.dbs.elki.index.tree.spatial.rstarvariants.util.SpatialComparator;
import de.lmu.ifi.dbs.elki.logging.Logging;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.AbstractParameterizer;

/**
 * Split strategy for bulk-loading a spatial tree where the split axes are the
 * dimensions with maximum extension.
 * 
 * @author Elke Achtert
 * 
 * @apiviz.uses SpatialComparator
 */
public class MaxExtensionBulkSplit extends AbstractBulkSplit {
  /**
   * Logger.
   */
  private static final Logging logger = Logging.getLogger(MaxExtensionBulkSplit.class);
  
  /**
   * Static instance
   */
  public static final MaxExtensionBulkSplit STATIC = new MaxExtensionBulkSplit();

  /**
   * Constructor
   */
  public MaxExtensionBulkSplit() {
    // Nothing to do
  }

  /**
   * Partitions the specified feature vectors where the split axes are the
   * dimensions with maximum extension.
   * 
   * @param spatialObjects the spatial objects to be partitioned
   * @param minEntries the minimum number of entries in a partition
   * @param maxEntries the maximum number of entries in a partition
   * @return the partition of the specified spatial objects
   */
  @Override
  public <N extends SpatialComparable> List<List<N>> partition(List<N> spatialObjects, int minEntries, int maxEntries) {
    List<List<N>> partitions = new ArrayList<List<N>>();
    List<N> objects = new ArrayList<N>(spatialObjects);

    while(objects.size() > 0) {
      StringBuffer msg = new StringBuffer();

      // get the split axis and split point
      int splitAxis = chooseMaximalExtendedSplitAxis(objects);
      int splitPoint = chooseBulkSplitPoint(objects.size(), minEntries, maxEntries);
      if(logger.isDebugging()) {
        msg.append("\nsplitAxis ").append(splitAxis);
        msg.append("\nsplitPoint ").append(splitPoint);
      }

      // sort in the right dimension
      Collections.sort(objects, new SpatialComparator(splitAxis, SpatialComparator.MIN));

      // insert into partition
      List<N> partition1 = new ArrayList<N>();
      for(int i = 0; i < splitPoint; i++) {
        N o = objects.remove(0);
        partition1.add(o);
      }
      partitions.add(partition1);

      // copy array
      if(logger.isDebugging()) {
        msg.append("\ncurrent partition ").append(partition1);
        msg.append("\nremaining objects # ").append(objects.size());
        logger.debugFine(msg.toString());
      }
    }

    if(logger.isDebugging()) {
      logger.debugFine("partitions " + partitions);
    }
    return partitions;
  }

  /**
   * Computes and returns the best split axis. The best split axis is the split
   * axes with the maximal extension.
   * 
   * @param objects the spatial objects to be split
   * @return the best split axis
   */
  private int chooseMaximalExtendedSplitAxis(List<? extends SpatialComparable> objects) {
    // maximum and minimum value for the extension
    int dimension = objects.get(0).getDimensionality();
    double[] maxExtension = new double[dimension];
    double[] minExtension = new double[dimension];
    Arrays.fill(minExtension, Double.MAX_VALUE);

    // compute min and max value in each dimension
    for(SpatialComparable object : objects) {
      for(int d = 1; d <= dimension; d++) {
        double min, max;
        min = object.getMin(d);
        max = object.getMax(d);

        if(maxExtension[d - 1] < max) {
          maxExtension[d - 1] = max;
        }

        if(minExtension[d - 1] > min) {
          minExtension[d - 1] = min;
        }
      }
    }

    // set split axis to dim with maximal extension
    int splitAxis = -1;
    double max = 0;
    for(int d = 1; d <= dimension; d++) {
      double currentExtension = maxExtension[d - 1] - minExtension[d - 1];
      if(max < currentExtension) {
        max = currentExtension;
        splitAxis = d;
      }
    }
    return splitAxis;
  }

  /**
   * Parameterization class.
   * 
   * @author Erich Schubert
   * 
   * @apiviz.exclude
   */
  public static class Parameterizer extends AbstractParameterizer {
    @Override
    protected MaxExtensionBulkSplit makeInstance() {
      return MaxExtensionBulkSplit.STATIC;
    }
  }
}