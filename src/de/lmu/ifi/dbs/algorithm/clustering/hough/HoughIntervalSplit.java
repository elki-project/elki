package de.lmu.ifi.dbs.algorithm.clustering.hough;

import de.lmu.ifi.dbs.data.ParameterizationFunction;
import de.lmu.ifi.dbs.database.Database;
import de.lmu.ifi.dbs.logging.AbstractLoggable;
import de.lmu.ifi.dbs.logging.LoggingConfiguration;
import de.lmu.ifi.dbs.utilities.HyperBoundingBox;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Supports the splitting of hough intervals.
 *
 * @author Elke Achtert (<a href="mailto:achtert@dbs.ifi.lmu.de">achtert@dbs.ifi.lmu.de</a>)
 */
public class HoughIntervalSplit extends AbstractLoggable {
  /**
   * The database storing the parameterization functions.
   */
  private Database<ParameterizationFunction> database;

  /**
   * Stores minimum function values for given intervals, used for better split performance.
   */
  private Map<HyperBoundingBox, Map<Integer, Double>> f_minima;

  /**
   * Stores maximum function values for given intervals, used for better split performance.
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
   * @param minPts   the number of minimum points
   */
  public HoughIntervalSplit(Database<ParameterizationFunction> database, int minPts) {
    super(LoggingConfiguration.DEBUG);

    this.database = database;
    this.minPts = minPts;
    this.f_minima = new HashMap<HyperBoundingBox, Map<Integer, Double>>();
    this.f_maxima = new HashMap<HyperBoundingBox, Map<Integer, Double>>();
  }

  /**
   * Determines the ids belonging to the given interval, i.e.
   * the parametrization functions falling within the interval.
   *
   * @param superSetIDs a superset of the ids to be determined
   * @param interval    the interval
   * @return the ids belonging to the given interval, if the number ids of exceeds minPts,
   *         null otherwise
   */
  public Set<Integer> determineIDs(Set<Integer> superSetIDs, HyperBoundingBox interval) {
    StringBuffer msg = new StringBuffer();
    if (debug) {
      msg.append("\ninterval " + interval);
    }

    HyperBoundingBox alphaInterval = alphaInterval(interval);
    Set<Integer> childIDs = new HashSet<Integer>(superSetIDs.size());
    double d_min = interval.getMin(1);
    double d_max = interval.getMax(1);

    Map<Integer, Double> minima = f_minima.get(alphaInterval);
    Map<Integer, Double> maxima = f_maxima.get(alphaInterval);
    if (minima == null || maxima == null) {
      minima = new HashMap<Integer, Double>();
      f_minima.put(alphaInterval, minima);
      maxima = new HashMap<Integer, Double>();
      f_maxima.put(alphaInterval, maxima);
    }

    for (Integer id : superSetIDs) {
      Double f_min = minima.get(id);
      Double f_max = maxima.get(id);

      if (f_min == null) {
        ParameterizationFunction f = database.get(id);
        HyperBoundingBox minMax = f.determineAlphaMinMax(alphaInterval);
        f_min = f.function(minMax.getMin());
        f_max = f.function(minMax.getMax());
        minima.put(id, f_min);
        maxima.put(id, f_max);
      }

      if (debug) {
        msg.append("\n\nf_min " + f_min);
        msg.append("\nf_max " + f_max);
        msg.append("\nd_min " + d_min);
        msg.append("\nd_max " + d_max);
      }

      if (f_min > f_max) {
        throw new IllegalArgumentException("Houston, we have a problem!");
      }

      if (f_min <= d_max && f_max >= d_min) {
        childIDs.add(id);
        if (debug) {
          msg.append("\nid " + id + " appended");
        }
      }

      else {
        if (debug) {
          msg.append("\nid " + id + " NOT appended");
        }
      }
    }

    if (debug) {
      msg.append("\nchildIds " + childIDs.size());
      debugFine(msg.toString());
    }

    if (childIDs.size() < minPts) {
      return null;
    }
    else {
      return childIDs;
    }
  }

  /**
   * Computes the alpha interval from the given interval.
   *
   * @param interval the interval
   * @return the alpha interval from the given interval
   */
  private HyperBoundingBox alphaInterval(HyperBoundingBox interval) {
    double[] alpha_min = new double[interval.getDimensionality() - 1];
    double[] alpha_max = new double[interval.getDimensionality() - 1];
    for (int d = 1; d < interval.getDimensionality(); d++) {
      alpha_min[d - 1] = interval.getMin(d + 1);
      alpha_max[d - 1] = interval.getMax(d + 1);
    }
    return new HyperBoundingBox(alpha_min, alpha_max);
  }
}
