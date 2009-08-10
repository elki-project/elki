package experimentalcode.remigius.Visualizers;

import org.w3c.dom.Element;

import de.lmu.ifi.dbs.elki.utilities.optionhandling.Parameterizable;
import de.lmu.ifi.dbs.elki.visualization.svg.SVGPlot;

/**
 * Defines the requirements for a visualizer.
 * 
 * @author Remigius Wojdanowski
 */
public interface Visualizer extends Parameterizable {

  /**
   * Returns a Element representing a visualization.
   * 
   * @param svgp the SVGPlot which will act as owner for the returned Element.
   * @param dimx the dimension to appear as horizontal (x-) dimension
   * @param dimy the dimension to appear as vertical (y-) dimension.
   * @return an {@link Element} representing a 2-dimensional visualization..
   */
  public Element visualize(SVGPlot svgp);

  /**
   * Returns a short name for the visualizer. Intended to be used for displaying
   * a representation of this Visualizer in the UI.
   * 
   * @return a short string characterizing the visualizer.
   */
  public String getName();

  /**
   * Returns an integer describing on which level the Visualization should be
   * placed. Lower numbers will result in a placing at the bottom, while higher
   * numbers will result in a placing at the top. If more Visualizers have the
   * same level, no ordering is guaranteed. <br>
   * Note that this value is only a recommendation, as it is totally up to the
   * framework do ignore it.
   * 
   * @return an integer describing on which level the Visualization should be
   *         placed.
   */
  public int getLevel();
}
