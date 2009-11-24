package experimentalcode.erich.visualization.gui.overview;

import java.awt.event.ActionEvent;

import de.lmu.ifi.dbs.elki.visualization.svg.SVGPlot;

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
  
  public SubplotSelectedEvent(OverviewPlot<?> source, int id, String command, int modifiers, double x, double y) {
    super(source, id, command, modifiers);
    this.overview = source;
    this.x = x;
    this.y = y;
  }
  
  public SVGPlot makeSubplot() {
    return overview.makeDetailPlot(x, y);
  }
}
