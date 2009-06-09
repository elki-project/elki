package experimentalcode.remigius.Visualizers;

import org.apache.batik.util.SVGConstants;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.events.EventListener;
import org.w3c.dom.events.EventTarget;

import de.lmu.ifi.dbs.elki.data.DoubleVector;
import de.lmu.ifi.dbs.elki.database.Database;
import de.lmu.ifi.dbs.elki.visualization.css.CSSClassManager.CSSNamingConflict;
import experimentalcode.erich.visualization.svg.SVGAxis;
import experimentalcode.remigius.ToolTipListener;
import experimentalcode.remigius.Visualization;
import experimentalcode.remigius.VisualizationManager;
import experimentalcode.remigius.Visualizer;

public class DotVisualizer<O extends DoubleVector> extends Visualizer<O> {

	private EventListener hoverer;
	
	public DotVisualizer(Database<O> database, VisualizationManager<O> v){
		super(database, v);
		this.hoverer = new ToolTipListener(v.getDocument());
	}

	@Override
	public Visualization<O> visualizeElements(Document doc, Element layer, int dimx, int dimy) {
		
		try {
			SVGAxis.drawAxis(visManager.getPlot(), layer, scales[dimx], 0, 1, 1, 1, true, true);
			SVGAxis.drawAxis(visManager.getPlot(), layer, scales[dimy], 0, 1, 0, 0, true, false);
			visManager.getPlot().updateStyleElement();
			
		} catch (CSSNamingConflict e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		
		for (int id : database.getIDs()){

			Element dot = SHAPEGEN.createDot(doc, getPositioned(database.get(id), dimx), (1 - getPositioned(database.get(id), dimy)), id, dimx, dimy);	
						EventTarget targ = (EventTarget) dot;
						targ.addEventListener(SVGConstants.SVG_MOUSEOVER_EVENT_TYPE, hoverer, false);
						targ.addEventListener(SVGConstants.SVG_MOUSEOUT_EVENT_TYPE, hoverer, false);
						targ.addEventListener(SVGConstants.SVG_CLICK_EVENT_TYPE, hoverer, false);
						layer.appendChild(dot);

			layer.appendChild(
					dot
			);
		}
		return new Visualization<O>(this, dimx, dimy, layer);
	}
	
	public String toString(){
		return "Dots";
	}
}
