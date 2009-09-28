package experimentalcode.remigius.Visualizers;

import org.w3c.dom.Element;

import de.lmu.ifi.dbs.elki.data.NumberVector;
import de.lmu.ifi.dbs.elki.database.Database;
import de.lmu.ifi.dbs.elki.visualization.svg.SVGPlot;
import experimentalcode.remigius.ShapeLibrary;

/**
 * Generates a SVG-Element containing "dots" as markers representing the Database's
 * objects. <br>
 * Future versions may contain a parameter for switching the used markers.
 * 
 * @author Remigius Wojdanowski
 * 
 * @param <NV>
 */
public class DotVisualizer<NV extends NumberVector<NV, ?>> extends PlanarVisualizer<NV> {

  /**
   * A short name characterizing this Visualizer.
   */
  private static final String NAME = "Dots";
  
  /**
   * Initializes this Visualizer.
   * 
   * @param database contains all objects to be processed.
   */
  public void init(Database<NV> database) {
    init(database, Integer.MAX_VALUE - 1000, NAME);
  }

  @Override
  public Element visualize(SVGPlot svgp) {

    Element layer = ShapeLibrary.createG(svgp.getDocument());
    for(int id : database.getIDs()) {
      Element dot = ShapeLibrary.createMarkerDot(svgp.getDocument(), getPositioned(database.get(id), dimx), (1 - getPositioned(database.get(id), dimy)));
      // setting ID for efficient use of ToolTips.
      dot.setAttribute("id", ShapeLibrary.createID(ShapeLibrary.MARKER, id));
      layer.appendChild(dot);
      svgp.putIdElement(ShapeLibrary.createID(ShapeLibrary.MARKER, id), dot);
    }
    return layer;
  }
}
