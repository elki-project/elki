package experimentalcode.remigius.Visualizers;

import org.w3c.dom.Element;

import de.lmu.ifi.dbs.elki.data.NumberVector;
import de.lmu.ifi.dbs.elki.database.Database;
import de.lmu.ifi.dbs.elki.visualization.svg.SVGPlot;
import experimentalcode.remigius.ShapeLibrary;
import experimentalcode.remigius.VisualizationManager;

public class DotVisualizer<NV extends NumberVector<NV, N>, N extends Number> extends PlanarVisualizer<NV, N> {
  
  private static final String NAME = "Dots";
  
	public DotVisualizer(){
	}

	public void setup(Database<NV> database, VisualizationManager<NV> v){
		init(database, v, Integer.MAX_VALUE-1000, NAME);
	}

	@Override
	public Element visualize(SVGPlot svgp) {
	  
	  Element layer = ShapeLibrary.createSVG(svgp.getDocument()); 
		for (int id : database.getIDs()){
			Element dot = ShapeLibrary.createDot(svgp.getDocument(), getPositioned(database.get(id), dimx), (1 - getPositioned(database.get(id), dimy)), id, dimx, dimy);
			layer.appendChild(dot);
			svgp.putIdElement(ShapeLibrary.createID(ShapeLibrary.MARKER, id, dimx, dimy), dot);
		}
		return layer;
	}
}
