/*
 * This file is part of ELKI:
 * Environment for Developing KDD-Applications Supported by Index-Structures
 *
 * Copyright (C) 2019
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
package de.lmu.ifi.dbs.elki.algorithm.clustering.correlation.cash;

import java.util.HashMap;
import java.util.Map;

import de.lmu.ifi.dbs.elki.data.HyperBoundingBox;
import de.lmu.ifi.dbs.elki.data.spatial.SpatialUtil;
import de.lmu.ifi.dbs.elki.database.ids.DBID;
import de.lmu.ifi.dbs.elki.database.ids.DBIDIter;
import de.lmu.ifi.dbs.elki.database.ids.DBIDUtil;
import de.lmu.ifi.dbs.elki.database.ids.DBIDs;
import de.lmu.ifi.dbs.elki.database.ids.ModifiableDBIDs;
import de.lmu.ifi.dbs.elki.database.relation.Relation;
import de.lmu.ifi.dbs.elki.logging.Logging;
import de.lmu.ifi.dbs.elki.utilities.io.FormatUtil;

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
   * The database storing the parameterization functions.
   */
  private Relation<ParameterizationFunction> database;

  /**
   * Caches minimum function values for given intervals, used for better split
   * performance.
   */
  private Map<HyperBoundingBox, Map<DBID, Double>> f_minima;

  /**
   * Caches maximum function values for given intervals, used for better split
   * performance.
   */
  private Map<HyperBoundingBox, Map<DBID, Double>> f_maxima;

  /**
   * Minimum points.
   */
  private int minPts;

  /**
   * The logger of the class.
   */
  private static final Logging LOG = Logging.getLogger(CASHIntervalSplit.class);

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

    Map<DBID, Double> minima = f_minima.get(interval);
    Map<DBID, Double> maxima = f_maxima.get(interval);
    if(minima == null || maxima == null) {
      minima = new HashMap<>();
      f_minima.put(interval, minima);
      maxima = new HashMap<>();
      f_maxima.put(interval, maxima);
    }

    for(DBIDIter iter = superSetIDs.iter(); iter.valid(); iter.advance()) {
      DBID id = DBIDUtil.deref(iter);
      Double f_min = minima.get(id);
      Double f_max = maxima.get(id);

      if(f_min == null) {
        ParameterizationFunction f = database.get(id);
        HyperBoundingBox minMax = f.determineAlphaMinMax(interval);
        f_min = f.function(SpatialUtil.getMin(minMax));
        f_max = f.function(SpatialUtil.getMax(minMax));
        minima.put(id, f_min);
        maxima.put(id, f_max);
      }

      if(msg != null) {
        msg.append("\n\nf_min ").append(f_min);
        msg.append("\nf_max ").append(f_max);
        msg.append("\nd_min ").append(d_min);
        msg.append("\nd_max ").append(d_max);
      }

      if(f_min - f_max > ParameterizationFunction.DELTA) {
        throw new IllegalArgumentException("Houston, we have a problem: f_min > f_max! " + "\nf_min[" + FormatUtil.format(SpatialUtil.centroid(interval)) + "] = " + f_min + "\nf_max[" + FormatUtil.format(SpatialUtil.centroid(interval)) + "] = " + f_max + "\nf " + database.get(id));
      }

      if(f_min <= d_max && f_max >= d_min) {
        childIDs.add(id);
        if(msg != null) {
          msg.append("\nid ").append(id).append(" appended");
        }
      }

      else {
        if(msg != null) {
          msg.append("\nid ").append(id).append(" NOT appended");
        }
      }
    }

    if(msg != null) {
      msg.append("\nchildIds ").append(childIDs.size());
      LOG.debugFine(msg.toString());
    }

    if(childIDs.size() < minPts) {
      return null;
    }
    else {
      return childIDs;
    }
  }
}
