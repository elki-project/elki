package experimentalcode.remigius.Visualizers;

import java.util.Iterator;

import org.w3c.dom.Element;

import de.lmu.ifi.dbs.elki.data.DoubleVector;
import de.lmu.ifi.dbs.elki.data.FeatureVector;
import de.lmu.ifi.dbs.elki.database.Database;
import de.lmu.ifi.dbs.elki.result.CollectionResult;
import de.lmu.ifi.dbs.elki.visualization.svg.SVGPlot;
import experimentalcode.remigius.NumberVisualization;
import experimentalcode.remigius.NumberVisualizer;
import experimentalcode.remigius.VisualizationManager;

// TODO Fix CSS, IDs and replace Dots.  

public class ReferencePointsVisualizer<O extends DoubleVector, V extends FeatureVector<V,N>,N extends Number> extends NumberVisualizer<O> {

	private CollectionResult<V> colResult;

	public ReferencePointsVisualizer(Database<O> database, CollectionResult<V> colResult, VisualizationManager<O> v){
		super(database, v);
		this.colResult = colResult;

		setupCSS();
	}

	private void setupCSS(){

//			CSSClass bubble = visManager.createCSSClass(CommonSVGShapes.CSS_BUBBLE_PREFIX);
//			bubble.setStatement(SVGConstants.CSS_STROKE_WIDTH_PROPERTY, "0.001");
//
//			visManager.registerCSSClass(bubble);
	}

	@Override
	protected NumberVisualization visualize(SVGPlot svgp, Element layer, int dimx, int dimy) {

		Iterator<V> iter = colResult.iter();
		
		while (iter.hasNext()){
			V v = iter.next();
			layer.appendChild(
					SHAPEGEN.createDot(svgp.getDocument(), v.getValue(dimx).doubleValue(), v.getValue(dimy).doubleValue(), (int)Math.random(), dimx, dimy)
			);
		}
		return new NumberVisualization(dimx, dimy, layer);
	}
	
	public String toString(){
		return "ReferencePoints";
	}
}
