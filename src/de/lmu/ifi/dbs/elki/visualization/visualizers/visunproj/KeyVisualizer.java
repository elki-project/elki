package de.lmu.ifi.dbs.elki.visualization.visualizers.visunproj;

import java.util.List;

import org.apache.batik.util.SVGConstants;
import org.w3c.dom.Element;

import de.lmu.ifi.dbs.elki.data.Clustering;
import de.lmu.ifi.dbs.elki.data.DatabaseObject;
import de.lmu.ifi.dbs.elki.data.cluster.Cluster;
import de.lmu.ifi.dbs.elki.data.model.Model;
import de.lmu.ifi.dbs.elki.visualization.style.StyleLibrary;
import de.lmu.ifi.dbs.elki.visualization.svg.MarkerLibrary;
import de.lmu.ifi.dbs.elki.visualization.svg.SVGPlot;
import de.lmu.ifi.dbs.elki.visualization.svg.SVGUtil;
import de.lmu.ifi.dbs.elki.visualization.visualizers.StaticVisualization;
import de.lmu.ifi.dbs.elki.visualization.visualizers.Visualization;
import de.lmu.ifi.dbs.elki.visualization.visualizers.Visualizer;
import de.lmu.ifi.dbs.elki.visualization.visualizers.VisualizerContext;

/**
 * Pseudo-Visualizer, that gives the key for a clustering.
 * 
 * @author Erich Schubert
 */
public class KeyVisualizer extends AbstractUnprojectedVisualizer<DatabaseObject> {
  /**
   * Name for this visualizer.
   */
  private static final String NAME = "Cluster Key";
  
  /**
   * The clustering to visualize
   */
  private Clustering<Model> clustering;

  /**
   * Constructor, adhering to
   * {@link de.lmu.ifi.dbs.elki.utilities.optionhandling.Parameterizable}
   */
  public KeyVisualizer() {
    super(NAME);
  }

  /**
   * Initialization.
   * 
   * @param context context.
   * @param clustering Clustering to visualize
   */
  @SuppressWarnings("unchecked")
  public void init(VisualizerContext<? extends DatabaseObject> context, Clustering<?> clustering) {
    super.init(context);
    this.clustering = (Clustering<Model>) clustering;
  }

  @Override
  public Visualization visualize(SVGPlot svgp, double width, double height) {
    final List<Cluster<Model>> allcs = clustering.getAllClusters();
    int numc = allcs.size();
    
    // FIXME: Use CSS and style library.
    
    Element layer = svgp.svgElement(SVGConstants.SVG_G_TAG);
    
    MarkerLibrary ml = context.getMarkerLibrary();
    
    int i = 0;
    for (Cluster<Model> c : allcs) {
      ml.useMarker(svgp, layer, 0.3, i+0.5, i, 0.3);
      Element label  = svgp.svgText(0.7, i+0.7, c.getNameAutomatic());
      label.setAttribute(SVGConstants.SVG_STYLE_ATTRIBUTE, "font-size: 0.6");
      layer.appendChild(label);
      i++;
    }

    int cols = Math.max(6, (int)(numc * height / width));
    int rows = numc;
    final double margin = context.getStyleLibrary().getSize(StyleLibrary.MARGIN);
    final String transform = SVGUtil.makeMarginTransform(width, height, cols, rows, margin / StyleLibrary.SCALE);
    SVGUtil.setAtt(layer, SVGConstants.SVG_TRANSFORM_ATTRIBUTE, transform);

    Integer level = this.getMetadata().getGenerics(Visualizer.META_LEVEL, Integer.class);
    return new StaticVisualization(context, svgp, level, layer, width, height);
  }
}