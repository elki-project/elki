package de.lmu.ifi.dbs.elki.visualization.opticsplot;

import de.lmu.ifi.dbs.elki.result.ClusterOrderEntry;

/**
 * Class to handle coloring of the OPTICS plot.
 * 
 * @author Erich Schubert
 */
public interface OPTICSColorAdapter {
  /**
   * Get the color value for a particular cluster order entry.
   * 
   * @param coe Cluster order entry
   * @return Color value (rgba integer)
   */
  public int getColorForEntry(ClusterOrderEntry<?> coe);
}
