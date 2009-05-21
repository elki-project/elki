package experimentalcode.remigius;

import java.util.Iterator;
import java.util.List;

import org.apache.batik.util.SVGConstants;
import org.w3c.dom.Element;

import de.lmu.ifi.dbs.elki.data.DoubleVector;
import de.lmu.ifi.dbs.elki.data.cluster.Cluster;
import de.lmu.ifi.dbs.elki.data.model.Model;
import de.lmu.ifi.dbs.elki.result.AnnotationResult;
import de.lmu.ifi.dbs.elki.result.MultiResult;
import de.lmu.ifi.dbs.elki.visualization.colors.ColorLibrary;
import de.lmu.ifi.dbs.elki.visualization.colors.PublicationColorLibrary;
import de.lmu.ifi.dbs.elki.visualization.svg.SVGPlot;

public abstract class Visualizer<O extends DoubleVector> {

	protected static final CommonSVGShapes SHAPEGEN = new CommonSVGShapes();
	protected static final ColorLibrary COLORS = new PublicationColorLibrary();

	protected VisualizationModel<O> model;

	protected MultiScale multiscale;

	public Visualizer(VisualizationModel<O> model){
		this.model = model;
	}

	public Element visualize(SVGPlot svgp, int dimx, int dimy){
		Element layer = svgp.svgElement(SVGConstants.SVG_SVG_TAG);

		Iterator<Cluster<Model>> iter = model.iterateCluster();
		int clusterID = 0;

		while (iter.hasNext()){
			Cluster<Model> c = iter.next();
			clusterID+=1;

			for (Integer i : c.getIDs()){
				visualize(svgp, model.getDatabaseObject(i), clusterID, dimx, dimy, layer);
			}
		}
		return layer;
	}

	public abstract List<AnnotationResult<?>> canVisualize(MultiResult result);

	protected abstract int getDimensionality();
	protected abstract String getDescription();

	protected abstract void visualize(SVGPlot svgp, O dbo, int clusterID, int dimx, int dimy, Element layer);
}
