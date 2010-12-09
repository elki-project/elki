package de.lmu.ifi.dbs.elki.visualization.gui.overview;

import java.util.LinkedList;

import de.lmu.ifi.dbs.elki.visualization.projections.Projection;
import de.lmu.ifi.dbs.elki.visualization.visualizers.VisualizationTask;

/**
 * Item to collect visualization tasks on a specific position on the plot map.
 * 
 * Note: this is a {@code LinkedList<VisualizationTask>}!
 * 
 * @author Erich Schubert
 * 
 * @apiviz.composedOf Projection
 */
public class PlotItem extends LinkedList<VisualizationTask> {
  /**
   * Serial version
   */
  private static final long serialVersionUID = 1L;

  /**
   * Position: x
   */
  public final double x;

  /**
   * Position: y
   */
  public final double y;

  /**
   * Size: width
   */
  public final double w;

  /**
   * Size: height
   */
  public final double h;

  /**
   * Projection (may be {@code null}!)
   */
  public final Projection proj;
  
  /**
   * Constructor.
   * 
   * @param x Position: x
   * @param y Position: y
   * @param w Position: w
   * @param h Position: h
   * @param proj Projection
   */
  public PlotItem(double x, double y, double w, double h, Projection proj) {
    super();
    this.x = x;
    this.y = y;
    this.w = w;
    this.h = h;
    this.proj = proj;
  }

  @Override
  public int hashCode() {
    // We can't have our hashcode change with the list contents!
    return System.identityHashCode(this);
  }
}