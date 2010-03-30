package de.lmu.ifi.dbs.elki.visualization.visualizers.vis2d;

import org.w3c.dom.Element;

import de.lmu.ifi.dbs.elki.data.NumberVector;
import de.lmu.ifi.dbs.elki.database.Database;
import de.lmu.ifi.dbs.elki.math.linearalgebra.Vector;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.Parameterizable;
import de.lmu.ifi.dbs.elki.visualization.VisualizationProjection;
import de.lmu.ifi.dbs.elki.visualization.style.StyleLibrary;
import de.lmu.ifi.dbs.elki.visualization.svg.SVGPlot;
import de.lmu.ifi.dbs.elki.visualization.svg.SVGUtil;
import de.lmu.ifi.dbs.elki.visualization.visualizers.Visualizer;
import de.lmu.ifi.dbs.elki.visualization.visualizers.VisualizerContext;

/**
 * Generates a SVG-Element containing "dots" as markers representing the Database's
 * objects.
 * 
 * @author Remigius Wojdanowski
 * 
 * @param <NV> Type of the NumberVector being visualized.
 */
public class DataDotVisualizer<NV extends NumberVector<NV, ?>> extends Projection2DVisualizer<NV> implements Parameterizable {
  /**
   * A short name characterizing this Visualizer.
   */
  private static final String NAME = "Data Dots";
  
  /**
   * Generic tag to indicate the type of element. Used in IDs, CSS-Classes etc.
   */
  public static final String MARKER = "marker";
  
  /**
   * Constructor, adhering to
   * {@link de.lmu.ifi.dbs.elki.utilities.optionhandling.Parameterizable}
   */ 
  public DataDotVisualizer() {
    super();
  }

  /**
   * Initializes this Visualizer.
   * 
   * @param context Visualization context
   */
  public void init(VisualizerContext context) {
    super.init(NAME, context);
    super.setLevel(Visualizer.LEVEL_BACKGROUND + 1);
  }

  @Override
  public Element visualize(SVGPlot svgp, VisualizationProjection proj, double width, double height) {
    double margin = context.getStyleLibrary().getSize(StyleLibrary.MARGIN);
    Element layer = super.setupCanvas(svgp, proj, margin, width, height);
    Database<NV> database = context.getDatabase();
    double dot_size = context.getStyleLibrary().getSize(StyleLibrary.DOTPLOT);
    for(int id : database) {
      Vector v = proj.projectDataToRenderSpace(database.get(id));
      Element dot = svgp.svgCircle(v.get(0), v.get(1), dot_size);
      SVGUtil.addCSSClass(dot, MARKER);
      layer.appendChild(dot);
    }
    return layer;
  }
}