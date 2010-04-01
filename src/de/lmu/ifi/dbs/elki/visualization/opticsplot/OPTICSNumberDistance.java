package de.lmu.ifi.dbs.elki.visualization.opticsplot;

import de.lmu.ifi.dbs.elki.distance.distancevalue.NumberDistance;
import de.lmu.ifi.dbs.elki.result.ClusterOrderEntry;

/**
 * Adapter that will map a regular number distance to its double value.
 * 
 * @author Erich Schubert
 */
public class OPTICSNumberDistance implements OPTICSDistanceAdapter<NumberDistance<?,?>> {
  /**
   * Default constructor.
   */
  public OPTICSNumberDistance() {
    super();
  }

  @Override
  public double getDoubleForEntry(ClusterOrderEntry<NumberDistance<?, ?>> coe) {
    return coe.getReachability().doubleValue();
  }
}
