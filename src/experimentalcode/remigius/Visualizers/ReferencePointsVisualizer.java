package experimentalcode.remigius.Visualizers;

import java.util.Iterator;

import org.apache.batik.util.SVGConstants;
import org.w3c.dom.Element;

import de.lmu.ifi.dbs.elki.data.NumberVector;
import de.lmu.ifi.dbs.elki.data.NumberVector;
import de.lmu.ifi.dbs.elki.database.Database;
import de.lmu.ifi.dbs.elki.logging.LoggingUtil;
import de.lmu.ifi.dbs.elki.result.CollectionResult;
import de.lmu.ifi.dbs.elki.visualization.css.CSSClass;
import de.lmu.ifi.dbs.elki.visualization.css.CSSClassManager.CSSNamingConflict;
import de.lmu.ifi.dbs.elki.visualization.svg.SVGPlot;
import experimentalcode.remigius.ShapeLibrary;

public class ReferencePointsVisualizer<NV extends NumberVector<NV, N>, V extends NumberVector<V,N>, N extends Number> extends PlanarVisualizer<NV, N> {

	private CollectionResult<V> colResult;
	private static final String NAME = "ReferencePoints";

	public ReferencePointsVisualizer(){

	}
	
	public void init(Database<NV> database, CollectionResult<V> colResult){
		init(database, Integer.MAX_VALUE-2000, NAME);
		this.colResult = colResult;
	}

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
	
	private Double getPositioned(V v, int dim){
		return scales[dim].getScaled(v.getValue(dim).doubleValue());
	}

	@Override
	public Element visualize(SVGPlot svgp) {
	  setupCSS(svgp);
	  Element layer = ShapeLibrary.createSVG(svgp.getDocument());
		Iterator<V> iter = colResult.iterator();
		
		while (iter.hasNext()){
			V v = iter.next();
			layer.appendChild(
					ShapeLibrary.createRef(svgp.getDocument(), getPositioned(v, dimx), getPositioned(v, dimy), 0, dimx, dimy, toString())
			);
		}
		return layer;
	}
}
