package de.lmu.ifi.dbs.elki.visualization.gui.overview;

import java.util.HashMap;

import de.lmu.ifi.dbs.elki.logging.LoggingUtil;
import de.lmu.ifi.dbs.elki.math.DoubleMinMax;
import de.lmu.ifi.dbs.elki.utilities.pairs.DoubleDoublePair;
import de.lmu.ifi.dbs.elki.visualization.projections.Projection;
import de.lmu.ifi.dbs.elki.visualization.visualizers.VisualizationTask;

/**
 * Manage the Overview plot canvas.
 * 
 * @author Erich Schubert
 * 
 * @apiviz.composedOf PlotItem
 */
class PlotMap<NV> extends HashMap<DoubleDoublePair, PlotItem> {
  /**
   * Serial version
   */
  private static final long serialVersionUID = 1L;

  /**
   * X coordinates seen
   */
  DoubleMinMax minmaxx = new DoubleMinMax();

  /**
   * Y coordinates seen
   */
  DoubleMinMax minmaxy = new DoubleMinMax();

  /**
   * Constructor.
   */
  PlotMap() {
    super();
  }

  /**
   * Place a new visualization on the chart.
   * 
   * @param x X coordinate
   * @param y Y coordinate
   * @param w Width
   * @param h Height
   * @param v Visualization
   */
  void addVis(double x, double y, double w, double h, Projection proj, VisualizationTask v) {
    final DoubleDoublePair pos = new DoubleDoublePair(x, y);
    PlotItem l = this.get(pos);
    if(l == null) {
      l = new PlotItem(x, y, w, h, proj);
      this.put(pos, l);
    }
    else {
      // Sanity check
      if(l.w != w || l.h != h) {
        LoggingUtil.warning("Layout error - different object sizes at the same map position!");
      }
      if(l.proj != proj) {
        LoggingUtil.warning("Layout error - two different projections used at the same map position.");
      }
    }
    l.add(v);
    // Update min/max
    minmaxx.put(x);
    minmaxx.put(x + w);
    minmaxy.put(y);
    minmaxy.put(y + h);
  }

  /**
   * Get the visualization on the given coordinates.
   * 
   * @param x First coordinate
   * @param y Second coordinate
   * @return Visualizations at this position.
   */
  PlotItem get(double x, double y) {
    return this.get(new DoubleDoublePair(x, y));
  }

  /**
   * Get width in plot units
   * 
   * @return width
   */
  public double getWidth() {
    return minmaxx.getMax() - minmaxx.getMin();
  }

  /**
   * Get height in plot units.
   * 
   * @return height
   */
  public double getHeight() {
    return minmaxy.getMax() - minmaxy.getMin();
  }
}