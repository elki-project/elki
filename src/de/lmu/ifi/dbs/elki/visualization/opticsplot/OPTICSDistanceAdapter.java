package de.lmu.ifi.dbs.elki.visualization.opticsplot;

import de.lmu.ifi.dbs.elki.distance.distancevalue.Distance;
import de.lmu.ifi.dbs.elki.result.ClusterOrderEntry;

/**
 * Interface to map ClusterOrderEntries to double values to use in the OPTICS plot.
 * 
 * @author Erich Schubert
 *
 * @param <D> Distance type
 */
public interface OPTICSDistanceAdapter<D extends Distance<?>> {
  /**
   * Get the double value for plotting for a cluster order entry.
   * 
   * @param coe Cluster Order Entry
   * @return Double value (height)
   */
  public double getDoubleForEntry(ClusterOrderEntry<D> coe);
}
