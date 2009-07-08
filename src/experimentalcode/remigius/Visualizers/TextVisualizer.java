package experimentalcode.remigius.Visualizers;

import org.apache.batik.util.SVGConstants;
import org.w3c.dom.Element;

import de.lmu.ifi.dbs.elki.data.DoubleVector;
import de.lmu.ifi.dbs.elki.database.Database;
import de.lmu.ifi.dbs.elki.result.AnnotationResult;
import de.lmu.ifi.dbs.elki.visualization.css.CSSClass;
import de.lmu.ifi.dbs.elki.visualization.svg.SVGPlot;
import experimentalcode.remigius.CommonSVGShapes;
import experimentalcode.remigius.NumberVisualization;
import experimentalcode.remigius.NumberVisualizer;
import experimentalcode.remigius.VisualizationManager;

public class TextVisualizer<O extends DoubleVector> extends NumberVisualizer<O> {

	AnnotationResult<Double> anResult;

	public TextVisualizer() {

	}

	public void setup(Database<O> database, AnnotationResult<Double> anResult, VisualizationManager<O> vvisManager) {

		init(database, visManager);
		this.anResult = anResult;
		setupCSS();
	}

	private void setupCSS() {

		CSSClass tooltip = visManager.createCSSClass(CommonSVGShapes.CSS_TOOLTIP_CLASS);
		tooltip.setStatement(SVGConstants.CSS_FONT_SIZE_PROPERTY, "0.1%");
		visManager.registerCSSClass(tooltip);
	}

	private Double getValue(int id) {

		return anResult.getValueFor(id);
	}

	@Override
	protected NumberVisualization visualize(SVGPlot svgp, Element layer, int dimx, int dimy) {

		for (int id : database.getIDs()) {
			layer.appendChild(SHAPEGEN.createToolTip(svgp.getDocument(),
					getPositioned(database.get(id), dimx), (1 - getPositioned(
							database.get(id), dimy)), getValue(id), id, dimx,
					dimy));
		}
		return new NumberVisualization(dimx, dimy, layer);
	}

	@Override
	public String getName() {

		return "Text";
	}
}
