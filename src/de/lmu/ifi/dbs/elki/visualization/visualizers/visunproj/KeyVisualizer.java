package de.lmu.ifi.dbs.elki.visualization.visualizers.visunproj;

import java.util.List;

import org.apache.batik.util.SVGConstants;
import org.w3c.dom.Element;

import de.lmu.ifi.dbs.elki.data.Clustering;
import de.lmu.ifi.dbs.elki.data.cluster.Cluster;
import de.lmu.ifi.dbs.elki.data.model.Model;
import de.lmu.ifi.dbs.elki.visualization.svg.MarkerLibrary;
import de.lmu.ifi.dbs.elki.visualization.svg.SVGPlot;
import de.lmu.ifi.dbs.elki.visualization.svg.SVGUtil;
import de.lmu.ifi.dbs.elki.visualization.visualizers.AbstractVisualizer;
import de.lmu.ifi.dbs.elki.visualization.visualizers.UnprojectedVisualizer;
import de.lmu.ifi.dbs.elki.visualization.visualizers.VisualizerContext;

/**
 * Pseudo-Visualizer, that gives the key for a clustering.
 * 
 * @author Erich Schubert
 */
public class KeyVisualizer extends AbstractVisualizer implements UnprojectedVisualizer {
  /**
   * Name for this visualizer.
   */
  private static final String NAME = "Key";

  /**
   * Constructor, Parameterizable style - does nothing.
   */
  public KeyVisualizer() {
    super();
  }

  /**
   * Initialization.
   * 
   * @param context context.
   */
  public void init(VisualizerContext context) {
    super.init(NAME, context);
  }

  @Override
  public Element visualize(SVGPlot svgp, double width, double height) {
    Clustering<Model> clustering = context.getOrCreateDefaultClustering();
    final List<Cluster<Model>> allcs = clustering.getAllClusters();
    int numc = allcs.size();
    
    // FIXME: Use CSS and style library.
    
    Element layer = svgp.svgElement(SVGConstants.SVG_G_TAG);
    
    MarkerLibrary ml = context.getMarkerLibrary();
    
    int i = 0;
    for (Cluster<Model> c : allcs) {
      Element marker = ml.useMarker(svgp, layer, 0.3, i+0.5, i, 0.3);
      Element label  = svgp.svgText(0.7, i+0.7, c.getNameAutomatic());
      label.setAttribute(SVGConstants.SVG_STYLE_ATTRIBUTE, "font-size: 0.6");
      layer.appendChild(marker);
      layer.appendChild(label);
      i++;
    }

    int size = Math.max(numc, 6);
    double scale = (1. / size);
    // scale
    // FIXME: use width, height
    SVGUtil.setAtt(layer, SVGConstants.SVG_TRANSFORM_ATTRIBUTE, "scale("+(0.9*scale)+") translate(0.08 0.02)");

    return layer;
  }
}
