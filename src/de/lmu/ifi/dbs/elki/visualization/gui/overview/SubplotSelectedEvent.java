package de.lmu.ifi.dbs.elki.visualization.gui.overview;

import java.awt.event.ActionEvent;

import de.lmu.ifi.dbs.elki.visualization.svg.SVGPlot;

/**
 * Event when a particular subplot was selected.
 * Plots are currently identified by their coordinates on the screen.
 * 
 * @author Erich Schubert
 */
public class SubplotSelectedEvent extends ActionEvent {
  /**
   * Serial version
   */
  private static final long serialVersionUID = 1L;
  /**
   * Parent overview plot.
   */
  OverviewPlot<?> overview;
  /**
   * X Coodinate
   */
  double x;
  /**
   * X Coodinate
   */
  double y;
  
  /**
   * Constructor. To be called by OverviewPlot only!
   * 
   * @param source
   * @param id
   * @param command
   * @param modifiers
   * @param x
   * @param y
   */
  public SubplotSelectedEvent(OverviewPlot<?> source, int id, String command, int modifiers, double x, double y) {
    super(source, id, command, modifiers);
    this.overview = source;
    this.x = x;
    this.y = y;
  }
  
  /**
   * Retrieve a materialized detail plot.
   * 
   * @return materialized detail plot
   */
  public SVGPlot makeSubplot() {
    return overview.makeDetailPlot(x, y);
  }
}
