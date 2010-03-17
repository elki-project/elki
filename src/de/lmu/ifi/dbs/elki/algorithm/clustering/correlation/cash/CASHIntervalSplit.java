package de.lmu.ifi.dbs.elki.algorithm.clustering.correlation.cash;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import de.lmu.ifi.dbs.elki.data.ParameterizationFunction;
import de.lmu.ifi.dbs.elki.database.Database;
import de.lmu.ifi.dbs.elki.logging.AbstractLoggable;
import de.lmu.ifi.dbs.elki.utilities.FormatUtil;
import de.lmu.ifi.dbs.elki.utilities.HyperBoundingBox;

/**
 * Supports the splitting of CASH intervals.
 * 
 * @author Elke Achtert
 */
public class CASHIntervalSplit extends AbstractLoggable {
  /**
   * The database storing the parameterization functions.
   */
  private Database<ParameterizationFunction> database;

  /**
   * Caches minimum function values for given intervals, used for better split
   * performance.
   */
  private Map<HyperBoundingBox, Map<Integer, Double>> f_minima;

  /**
   * Caches maximum function values for given intervals, used for better split
   * performance.
   */
  private Map<HyperBoundingBox, Map<Integer, Double>> f_maxima;

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
  public CASHIntervalSplit(Database<ParameterizationFunction> database, int minPts) {
    super();

    this.database = database;
    this.minPts = minPts;
    this.f_minima = new HashMap<HyperBoundingBox, Map<Integer, Double>>();
    this.f_maxima = new HashMap<HyperBoundingBox, Map<Integer, Double>>();
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
  public Set<Integer> determineIDs(Set<Integer> superSetIDs, HyperBoundingBox interval, double d_min, double d_max) {
    StringBuffer msg = new StringBuffer();
    if(logger.isDebugging()) {
      msg.append("interval ").append(interval);
    }

    Set<Integer> childIDs = new HashSet<Integer>(superSetIDs.size());

    Map<Integer, Double> minima = f_minima.get(interval);
    Map<Integer, Double> maxima = f_maxima.get(interval);
    if(minima == null || maxima == null) {
      minima = new HashMap<Integer, Double>();
      f_minima.put(interval, minima);
      maxima = new HashMap<Integer, Double>();
      f_maxima.put(interval, maxima);
    }

    for(Integer id : superSetIDs) {
      Double f_min = minima.get(id);
      Double f_max = maxima.get(id);

      if(f_min == null) {
        ParameterizationFunction f = database.get(id);
        HyperBoundingBox minMax = f.determineAlphaMinMax(interval);
        f_min = f.function(minMax.getMin());
        f_max = f.function(minMax.getMax());
        minima.put(id, f_min);
        maxima.put(id, f_max);
      }

      if(logger.isDebugging()) {
        msg.append("\n\nf_min ").append(f_min);
        msg.append("\nf_max ").append(f_max);
        msg.append("\nd_min ").append(d_min);
        msg.append("\nd_max ").append(d_max);
      }

      if(f_min - f_max > ParameterizationFunction.DELTA) {
        throw new IllegalArgumentException("Houston, we have a problem: f_min > f_max! " + "\nf_min[" + FormatUtil.format(interval.centroid()) + "] = " + f_min + "\nf_max[" + FormatUtil.format(interval.centroid()) + "] = " + f_max + "\nf " + database.get(id));
      }

      if(f_min <= d_max && f_max >= d_min) {
        childIDs.add(id);
        if(logger.isDebugging()) {
          msg.append("\nid ").append(id).append(" appended");
        }
      }

      else {
        if(logger.isDebugging()) {
          msg.append("\nid ").append(id).append(" NOT appended");
        }
      }
    }

    if(logger.isDebugging()) {
      msg.append("\nchildIds ").append(childIDs.size());
      logger.debugFine(msg.toString());
    }

    if(childIDs.size() < minPts) {
      return null;
    }
    else {
      return childIDs;
    }
  }
}
