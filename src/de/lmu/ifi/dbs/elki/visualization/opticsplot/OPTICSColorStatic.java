package de.lmu.ifi.dbs.elki.visualization.opticsplot;

import de.lmu.ifi.dbs.elki.result.ClusterOrderEntry;

/**
 * Pseudo-coloring for OPTICS plot that just uses a static color.
 * 
 * @author Erich Schubert
 */
public class OPTICSColorStatic implements OPTICSColorAdapter {
  /**
   * Static color to use
   */
  int color;
  
  /**
   * Constructor.
   * 
   * @param color Static color to use.
   */
  public OPTICSColorStatic(int color) {
    super();
    this.color = color;
  }

  @Override
  public int getColorForEntry(@SuppressWarnings("unused") ClusterOrderEntry<?> coe) {
    return color;
  }
}
