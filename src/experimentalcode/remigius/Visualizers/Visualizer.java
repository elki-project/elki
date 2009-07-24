package experimentalcode.remigius.Visualizers;

import de.lmu.ifi.dbs.elki.data.DatabaseObject;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.Parameterizable;
import de.lmu.ifi.dbs.elki.visualization.svg.SVGPlot;
import experimentalcode.remigius.visualization.Visualization;

/**
 * Defines the requirements for a visualizer.
 * 
 * @author Remigius Wojdanowski
 *
 * @param <O> the type of objects this visualizer will process.
 * @param <V> the type of Visualization this visualizer will produce.
 */
public interface Visualizer<O extends DatabaseObject, V extends Visualization> extends Parameterizable {

  /**
   * Returns a two-dimensional Visualization.
   * 
   * @param svgp the {@link SVGPlot} the Visualization will be used.
   * @param dimx the dimension to appear as horizontal (x-) dimension
   * @param dimy the dimension to appear as vertical (y-) dimension.
   * @return a two-dimensional {@link Visualization}.
   */
  public V visualize(SVGPlot svgp);

  /**
   * Returns a name for the visualizer. Intended to be used for displaying a
   * representation of this Visualizer in the UI.
   * 
   * @return a short string characterizing the visualizer.
   */
  public String getName();
  
  /**
   * Returns an integer describing on which level the Visualization should be placed. 
   * 
   * @return an integer describing on which level the Visualization should be placed.
   */
  public int getLevel();
}
