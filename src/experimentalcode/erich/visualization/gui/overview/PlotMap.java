package experimentalcode.erich.visualization.gui.overview;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import de.lmu.ifi.dbs.elki.math.MinMax;
import de.lmu.ifi.dbs.elki.utilities.pairs.DoubleDoublePair;

/**
 * Manage the Overview plot canvas.
 * 
 * @author Erich Schubert
 */
class PlotMap<NV> extends HashMap<DoubleDoublePair, ArrayList<VisualizationInfo>> {
  /**
   * Serial version
   */
  private static final long serialVersionUID = 1L;

  /**
   * X coordinates seen
   */
  MinMax<Double> minmaxx = new MinMax<Double>();

  /**
   * Y coordinates seen
   */
  MinMax<Double> minmaxy = new MinMax<Double>();

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
  void addVis(double x, double y, double w, double h, VisualizationInfo v) {
    ArrayList<VisualizationInfo> l = this.get(new DoubleDoublePair(x, y));
    if(l == null) {
      l = new ArrayList<VisualizationInfo>();
      this.put(new DoubleDoublePair(x, y), l);
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
  List<VisualizationInfo> get(double x, double y) {
    return this.get(new DoubleDoublePair(x, y));
  }
}