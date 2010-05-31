package de.lmu.ifi.dbs.elki.visualization.visualizers.vis2d;

import org.w3c.dom.Element;

import de.lmu.ifi.dbs.elki.data.NumberVector;
import de.lmu.ifi.dbs.elki.database.Database;
import de.lmu.ifi.dbs.elki.database.ids.DBID;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.Parameterizable;
import de.lmu.ifi.dbs.elki.visualization.VisualizationProjection;
import de.lmu.ifi.dbs.elki.visualization.style.StyleLibrary;
import de.lmu.ifi.dbs.elki.visualization.svg.SVGPlot;
import de.lmu.ifi.dbs.elki.visualization.svg.SVGUtil;
import de.lmu.ifi.dbs.elki.visualization.visualizers.StaticVisualization;
import de.lmu.ifi.dbs.elki.visualization.visualizers.Visualization;
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
  public void init(VisualizerContext<? extends NV> context) {
    super.init(NAME, context);
    super.setLevel(Visualizer.LEVEL_DATA + 1);
  }

  @Override
  public Visualization visualize(SVGPlot svgp, VisualizationProjection proj, double width, double height) {
    double margin = context.getStyleLibrary().getSize(StyleLibrary.MARGIN);
    Element layer = Projection2DVisualization.setupCanvas(svgp, proj, margin, width, height);
    Database<? extends NV> database = context.getDatabase();
    double dot_size = context.getStyleLibrary().getSize(StyleLibrary.DOTPLOT);
    for(DBID id : database) {
      double[] v = proj.fastProjectDataToRenderSpace(database.get(id));
      Element dot = svgp.svgCircle(v[0], v[1], dot_size);
      SVGUtil.addCSSClass(dot, MARKER);
      layer.appendChild(dot);
    }
    Integer level = this.getMetadata().getGenerics(Visualizer.META_LEVEL, Integer.class);
    return new StaticVisualization(level, layer, width, height);
  }
}