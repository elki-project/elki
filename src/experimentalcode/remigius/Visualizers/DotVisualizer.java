package experimentalcode.remigius.Visualizers;

import org.w3c.dom.Element;

import de.lmu.ifi.dbs.elki.data.DoubleVector;
import de.lmu.ifi.dbs.elki.database.Database;
import de.lmu.ifi.dbs.elki.visualization.svg.SVGPlot;
import experimentalcode.remigius.NumberVisualization;
import experimentalcode.remigius.NumberVisualizer;
import experimentalcode.remigius.ShapeLibrary;
import experimentalcode.remigius.VisualizationManager;

public class DotVisualizer<O extends DoubleVector> extends NumberVisualizer<O> {
  
	public DotVisualizer(){
	}

	public void setup(Database<O> database, VisualizationManager<O> v){
		init(database, v, Integer.MAX_VALUE-1000);
	}

	@Override
	protected NumberVisualization visualize(SVGPlot svgp, Element layer, int dimx,
			int dimy) {

		for (int id : database.getIDs()){

			Element dot = ShapeLibrary.createDot(svgp.getDocument(), getPositioned(database.get(id), dimx), (1 - getPositioned(database.get(id), dimy)), id, dimx, dimy);
			layer.appendChild(dot);
		}
		return new NumberVisualization(dimx, dimy, layer);
	}

	public String getName(){
		return "Dots";
	}
}
