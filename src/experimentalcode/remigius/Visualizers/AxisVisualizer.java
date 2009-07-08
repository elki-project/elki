package experimentalcode.remigius.Visualizers;

import org.w3c.dom.Element;

import de.lmu.ifi.dbs.elki.data.DoubleVector;
import de.lmu.ifi.dbs.elki.database.Database;
import de.lmu.ifi.dbs.elki.visualization.css.CSSClassManager.CSSNamingConflict;
import de.lmu.ifi.dbs.elki.visualization.svg.SVGPlot;
import de.lmu.ifi.dbs.elki.visualization.svg.SVGSimpleLinearAxis;
import experimentalcode.remigius.NumberVisualization;
import experimentalcode.remigius.NumberVisualizer;
import experimentalcode.remigius.VisualizationManager;

public class AxisVisualizer<O extends DoubleVector> extends NumberVisualizer<O> {
	
	public AxisVisualizer(){
	
	}


	public void setup(Database<O> database, VisualizationManager<O> visManager){
		// We don't need the Database. Maybe another superclass / inheritance hierarchy would serve us better.
		init(database, visManager);
	}

	@Override
	protected NumberVisualization visualize(SVGPlot svgp, Element layer, int dimx,
			int dimy) {
		try {
			SVGSimpleLinearAxis.drawAxis(svgp, layer, scales[dimx], 0, 1, 1, 1, true, true);
			SVGSimpleLinearAxis.drawAxis(svgp, layer, scales[dimy], 0, 1, 0, 0, true, false);
			svgp.updateStyleElement();

		} catch (CSSNamingConflict e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		return new NumberVisualization(dimx, dimy, layer);
	}

	public String getName(){
		return "Axis";
	}

}
