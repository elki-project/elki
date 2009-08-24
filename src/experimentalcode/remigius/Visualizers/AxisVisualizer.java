package experimentalcode.remigius.Visualizers;

import org.w3c.dom.Element;

import de.lmu.ifi.dbs.elki.data.NumberVector;
import de.lmu.ifi.dbs.elki.database.Database;
import de.lmu.ifi.dbs.elki.logging.LoggingUtil;
import de.lmu.ifi.dbs.elki.visualization.css.CSSClassManager.CSSNamingConflict;
import de.lmu.ifi.dbs.elki.visualization.svg.SVGPlot;
import de.lmu.ifi.dbs.elki.visualization.svg.SVGSimpleLinearAxis;
import experimentalcode.remigius.ShapeLibrary;
import experimentalcode.remigius.VisualizationManager;

/**
 * Generates a SVG-Element containing axes, including descriptions.
 * 
 * @author Remigius Wojdanowski
 *
 * @param <O> the type of object to be processed.
 */
public class AxisVisualizer<NV extends NumberVector<NV, N>, N extends Number> extends PlanarVisualizer<NV, N>{
  
  // TODO: we have two name-attributes, maybe that's one too much. 
  private static final String NAME = "Axes";
  
	public void setup(Database<NV> database, VisualizationManager<NV> visManager){
		// We don't need the Database. Maybe another superclass / inheritance hierarchy would serve us better.
		init(database, visManager, NAME);
	}

	@Override
	public Element visualize(SVGPlot svgp) {
	  Element layer = ShapeLibrary.createSVG(svgp.getDocument());
		try {
			SVGSimpleLinearAxis.drawAxis(svgp, layer, scales[dimx], 0, 1, 1, 1, true, true);
			SVGSimpleLinearAxis.drawAxis(svgp, layer, scales[dimy], 0, 1, 0, 0, true, false);
			svgp.updateStyleElement();

		} catch (CSSNamingConflict e) {
		  LoggingUtil.exception(e);
		}

		return layer;
	}
}
