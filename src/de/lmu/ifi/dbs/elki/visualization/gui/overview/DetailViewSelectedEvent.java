package de.lmu.ifi.dbs.elki.visualization.gui.overview;

import java.awt.event.ActionEvent;

import de.lmu.ifi.dbs.elki.visualization.gui.detail.DetailView;

/**
 * Event when a particular subplot was selected. Plots are currently identified
 * by their coordinates on the screen.
 * 
 * @author Erich Schubert
 */
public class DetailViewSelectedEvent extends ActionEvent {
  /**
   * Serial version
   */
  private static final long serialVersionUID = 1L;

  /**
   * Parent overview plot.
   */
  OverviewPlot overview;

  /**
   * X Coordinate
   */
  double x;

  /**
   * X Coordinate
   */
  double y;

  /**
   * Constructor. To be called by OverviewPlot only!
   * 
   * @param source source plot
   * @param id ID
   * @param command command that was invoked
   * @param modifiers modifiers
   * @param x x click
   * @param y y click
   */
  public DetailViewSelectedEvent(OverviewPlot source, int id, String command, int modifiers, double x, double y) {
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
  public DetailView makeDetailView() {
    return overview.makeDetailView(x, y);
  }
}