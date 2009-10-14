package experimentalcode.remigius.Visualizers;

import java.util.Iterator;

import org.apache.batik.util.SVGConstants;
import org.w3c.dom.Element;

import de.lmu.ifi.dbs.elki.data.NumberVector;
import de.lmu.ifi.dbs.elki.database.Database;
import de.lmu.ifi.dbs.elki.logging.LoggingUtil;
import de.lmu.ifi.dbs.elki.result.CollectionResult;
import de.lmu.ifi.dbs.elki.visualization.css.CSSClass;
import de.lmu.ifi.dbs.elki.visualization.css.CSSClassManager.CSSNamingConflict;
import de.lmu.ifi.dbs.elki.visualization.svg.SVGPlot;
import experimentalcode.remigius.ShapeLibrary;

/**
 * Generates a SVG-Element containing reference points.
 * TODO: Extend that documentation.
 * TODO: This class needs testing.
 * 
 * @author Remigius Wojdanowski
 *
 * @param <NV>
 */
public class ReferencePointsVisualizer<NV extends NumberVector<NV, ?>> extends PlanarVisualizer<NV> {
  
  /**
   * Serves reference points.
   */
	private CollectionResult<NV> colResult;
	
	/**
   * A short name characterizing this Visualizer.
   */
	private static final String NAME = "ReferencePoints";
	
	/**
   * Initializes this Visualizer.
   * 
   * @param database contains all objects to be processed.
	 * @param colResult contains all reference points.
	 */
	public void init(Database<NV> database, CollectionResult<NV> colResult){
		init(database, Integer.MAX_VALUE-2000, NAME);
		this.colResult = colResult;
	}
	
	/**
   * Registers the Reference-Point-CSS-Class at a SVGPlot.
   * 
   * @param svgp the SVGPlot to register the ToolTip-CSS-Class.
   */
	private void setupCSS(SVGPlot svgp){

			CSSClass refpoint = new CSSClass(svgp, ShapeLibrary.REFPOINT);
			refpoint.setStatement(SVGConstants.CSS_FILL_PROPERTY, "red");

			try {
        svgp.getCSSClassManager().addClass(refpoint);
        svgp.updateStyleElement();
      }
      catch(CSSNamingConflict e) {
        LoggingUtil.exception("Equally-named CSSClass with different owner already exists", e);
      }
	}
	

	@Override
	public Element visualize(SVGPlot svgp) {
	  setupCSS(svgp);
	  Element layer = ShapeLibrary.createG(svgp.getDocument());
		Iterator<NV> iter = colResult.iterator();
		
		while (iter.hasNext()){
			NV v = iter.next();
			layer.appendChild(
					ShapeLibrary.createRef(svgp.getDocument(), getProjected(v, dimx), getProjected(v, dimy))
			);
		}
		return layer;
	}
}
