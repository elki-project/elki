/*
 * This file is part of ELKI:
 * Environment for Developing KDD-Applications Supported by Index-Structures
 *
 * Copyright (C) 2022
 * ELKI Development Team
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package elki.clustering.correlation.cash;

import java.util.HashMap;
import java.util.Map;

import elki.data.HyperBoundingBox;
import elki.data.spatial.SpatialUtil;
import elki.database.datastore.DataStoreFactory;
import elki.database.datastore.WritableDoubleDataStore;
import elki.database.ids.*;
import elki.database.relation.Relation;
import elki.logging.Logging;
import elki.utilities.io.FormatUtil;

/**
 * Supports the splitting of CASH intervals.
 *
 * @author Elke Achtert
 * @since 0.1
 *
 * @depend - - - ParameterizationFunction
 */
public class CASHIntervalSplit {
  /**
   * The logger of the class.
   */
  private static final Logging LOG = Logging.getLogger(CASHIntervalSplit.class);

  /**
   * The database storing the parameterization functions.
   */
  private Relation<ParameterizationFunction> database;

  /**
   * Caches minimum function values for given intervals, used for better split
   * performance.
   */
  private Map<HyperBoundingBox, WritableDoubleDataStore> f_minima;

  /**
   * Caches maximum function values for given intervals, used for better split
   * performance.
   */
  private Map<HyperBoundingBox, WritableDoubleDataStore> f_maxima;

  /**
   * Minimum points.
   */
  private int minPts;

  /**
   * Initializes the logger and sets the debug status to the given value.
   * 
   * @param database the database storing the parameterization functions
   * @param minPts the number of minimum points
   */
  public CASHIntervalSplit(Relation<ParameterizationFunction> database, int minPts) {
    super();

    this.database = database;
    this.minPts = minPts;
    this.f_minima = new HashMap<>();
    this.f_maxima = new HashMap<>();
  }

  /**
   * Determines the ids belonging to the given interval, i.e. the
   * parameterization functions falling within the interval.
   * 
   * @param superSetIDs a superset of the ids to be determined
   * @param interval the hyper bounding box defining the interval of alpha
   *        values
   * @param d_min the minimum distance value for the interval
   * @param d_max the maximum distance value for the interval
   * @return the ids belonging to the given interval, if the number ids of
   *         exceeds minPts, null otherwise
   */
  public ModifiableDBIDs determineIDs(DBIDs superSetIDs, HyperBoundingBox interval, double d_min, double d_max) {
    StringBuilder msg = LOG.isDebugging() ? new StringBuilder() : null;
    if(msg != null) {
      msg.append("interval ").append(interval);
    }

    ModifiableDBIDs childIDs = DBIDUtil.newHashSet(superSetIDs.size());

    WritableDoubleDataStore minima = f_minima.get(interval);
    WritableDoubleDataStore maxima = f_maxima.get(interval);
    if(minima == null || maxima == null) {
      f_minima.put(interval, minima = DataStoreFactory.FACTORY.makeDoubleStorage(superSetIDs, DataStoreFactory.HINT_HOT | DataStoreFactory.HINT_TEMP, Double.NaN));
      f_maxima.put(interval, maxima = DataStoreFactory.FACTORY.makeDoubleStorage(superSetIDs, DataStoreFactory.HINT_HOT | DataStoreFactory.HINT_TEMP, Double.NaN));
    }

    for(DBIDIter iter = superSetIDs.iter(); iter.valid(); iter.advance()) {
      double f_min = minima.doubleValue(iter);
      double f_max = maxima.doubleValue(iter);

      if(Double.isNaN(f_min)) {
        ParameterizationFunction f = database.get(iter);
        HyperBoundingBox minMax = f.determineAlphaMinMax(interval);
        minima.put(iter, f_min = f.function(SpatialUtil.getMin(minMax)));
        maxima.put(iter, f_max = f.function(SpatialUtil.getMax(minMax)));
      }

      if(msg != null) {
        msg.append("\n\nf_min ").append(f_min) //
            .append("\nf_max ").append(f_max) //
            .append("\nd_min ").append(d_min) //
            .append("\nd_max ").append(d_max);
      }

      if(f_min - f_max > ParameterizationFunction.DELTA) {
        throw new IllegalArgumentException("Houston, we have a problem: f_min > f_max! " + "\nf_min[" + FormatUtil.format(SpatialUtil.centroid(interval)) + "] = " + f_min + "\nf_max[" + FormatUtil.format(SpatialUtil.centroid(interval)) + "] = " + f_max + "\nf " + database.get(iter));
      }

      if(f_min <= d_max && f_max >= d_min) {
        childIDs.add(iter);
        if(msg != null) {
          msg.append("\nid ").append(iter).append(" appended");
        }
      }
      else {
        if(msg != null) {
          msg.append("\nid ").append(iter).append(" NOT appended");
        }
      }
    }

    if(msg != null) {
      LOG.debugFine(msg.append("\nchildIds ").append(childIDs.size()).toString());
    }
    return childIDs.size() < minPts ? null : childIDs;
  }
}
