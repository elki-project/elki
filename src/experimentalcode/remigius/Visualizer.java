package experimentalcode.remigius;

import org.w3c.dom.Element;

import de.lmu.ifi.dbs.elki.data.DoubleVector;
import de.lmu.ifi.dbs.elki.database.Database;
import de.lmu.ifi.dbs.elki.visualization.colors.ColorLibrary;
import de.lmu.ifi.dbs.elki.visualization.colors.PublicationColorLibrary;
import de.lmu.ifi.dbs.elki.visualization.scales.LinearScale;
import de.lmu.ifi.dbs.elki.visualization.scales.Scales;
import de.lmu.ifi.dbs.elki.visualization.svg.SVGPlot;

public abstract class Visualizer<O extends DoubleVector> {
	
	protected Database<O> database;
	protected LinearScale[] scales;
	
	protected VisualizationManager<O> visManager;
	
	protected static final CommonSVGShapes SHAPEGEN = new CommonSVGShapes();
	protected static final ColorLibrary COLORS = new PublicationColorLibrary();
	
	public Visualizer(Database<O> db, VisualizationManager<O> v){
		this.database = db;
		this.scales = Scales.calcScales(db);
		this.visManager = v;
	}
	
	public Double getPositioned(O o, int dimx){
			return scales[dimx].getScaled(o.getValue(dimx));
	}
	
	public Visualization<O> visualize(SVGPlot svgp, int dimx, int dimy){
		Element layer = SHAPEGEN.createSVG(svgp.getDocument());
		return visualize(svgp, layer, dimx, dimy);
		
	}
	protected abstract Visualization<O> visualize(SVGPlot svgp, Element layer, int dimx, int dimy);
}
