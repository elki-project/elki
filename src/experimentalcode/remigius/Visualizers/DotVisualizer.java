package experimentalcode.remigius.Visualizers;

import org.apache.batik.util.SVGConstants;
import org.w3c.dom.Element;
import org.w3c.dom.events.EventListener;
import org.w3c.dom.events.EventTarget;

import de.lmu.ifi.dbs.elki.data.DoubleVector;
import de.lmu.ifi.dbs.elki.database.Database;
import de.lmu.ifi.dbs.elki.visualization.css.CSSClassManager.CSSNamingConflict;
import de.lmu.ifi.dbs.elki.visualization.svg.SVGPlot;
import de.lmu.ifi.dbs.elki.visualization.svg.SVGSimpleLinearAxis;
import experimentalcode.remigius.NumberVisualization;
import experimentalcode.remigius.NumberVisualizer;
import experimentalcode.remigius.VisualizationManager;

public class DotVisualizer<O extends DoubleVector> extends NumberVisualizer<O> {

	private EventListener hoverer;

	// TODO: Make a visualizer dedicated to drawing axes.
	// TODO: Fix ToolTips (change IDs to be unique & add visibility-modifier per layer to the GUI)  
	public DotVisualizer(){
	}


	public void setup(Database<O> database, VisualizationManager<O> v){
		init(database, v);
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
