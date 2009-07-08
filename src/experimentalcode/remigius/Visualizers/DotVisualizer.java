package experimentalcode.remigius.Visualizers;

import org.w3c.dom.Element;
import org.w3c.dom.events.EventListener;

import de.lmu.ifi.dbs.elki.data.DoubleVector;
import de.lmu.ifi.dbs.elki.database.Database;
import de.lmu.ifi.dbs.elki.visualization.svg.SVGPlot;
import experimentalcode.remigius.NumberVisualization;
import experimentalcode.remigius.NumberVisualizer;
import experimentalcode.remigius.VisualizationManager;

public class DotVisualizer<O extends DoubleVector> extends NumberVisualizer<O> {

	private EventListener hoverer;

	// TODO: Fix ToolTips (change IDs to be unique & add visibility-modifier per layer to the GUI)  
	public DotVisualizer(){
	}


	public void setup(Database<O> database, VisualizationManager<O> v){
		init(database, v);
	}

	@Override
	protected NumberVisualization visualize(SVGPlot svgp, Element layer, int dimx,
			int dimy) {

		for (int id : database.getIDs()){

			Element dot = SHAPEGEN.createDot(svgp.getDocument(), getPositioned(database.get(id), dimx), (1 - getPositioned(database.get(id), dimy)), id, dimx, dimy);	
//			EventTarget targ = (EventTarget) dot;
//			targ.addEventListener(SVGConstants.SVG_MOUSEOVER_EVENT_TYPE, hoverer, false);
//			targ.addEventListener(SVGConstants.SVG_MOUSEOUT_EVENT_TYPE, hoverer, false);
//			targ.addEventListener(SVGConstants.SVG_CLICK_EVENT_TYPE, hoverer, false);
			layer.appendChild(dot);
		}
		return new NumberVisualization(dimx, dimy, layer);
	}

	public String getName(){
		return "Dots";
	}
}
