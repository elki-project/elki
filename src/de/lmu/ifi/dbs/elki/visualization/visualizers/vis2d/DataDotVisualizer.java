package de.lmu.ifi.dbs.elki.visualization.visualizers.vis2d;

import org.w3c.dom.Element;

import de.lmu.ifi.dbs.elki.data.NumberVector;
import de.lmu.ifi.dbs.elki.database.Database;
import de.lmu.ifi.dbs.elki.database.DatabaseEvent;
import de.lmu.ifi.dbs.elki.database.DatabaseListener;
import de.lmu.ifi.dbs.elki.database.ids.DBID;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.Parameterizable;
import de.lmu.ifi.dbs.elki.visualization.VisualizationProjection;
import de.lmu.ifi.dbs.elki.visualization.style.StyleLibrary;
import de.lmu.ifi.dbs.elki.visualization.svg.SVGPlot;
import de.lmu.ifi.dbs.elki.visualization.svg.SVGUtil;
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
    return new DotVisualization(context, svgp, proj, width, height);
  }

  /**
   * The actual visualization instance, for a single projection
   * 
   * @author Erich Schubert
   */
  protected class DotVisualization extends Projection2DVisualization<NV> implements DatabaseListener<NV> {
    /**
     * Constructor.
     * 
     * @param context Context
     * @param svgp Plot
     * @param proj Projection
     * @param width Width
     * @param height Height
     */
    public DotVisualization(VisualizerContext<? extends NV> context, SVGPlot svgp, VisualizationProjection proj, double width, double height) {
      super(context, svgp, proj, width, height, Visualizer.LEVEL_DATA);
      context.addDatabaseListener(this);
      incrementalRedraw();
    }

    @Override
    public void destroy() {
      super.destroy();
      context.removeDatabaseListener(this);
    }

    @Override
    public void redraw() {
      // get the Database
      Database<? extends NV> database = context.getDatabase();
      // draw data
      double dot_size = context.getStyleLibrary().getSize(StyleLibrary.DOTPLOT);
      for(DBID id : database) {
        double[] v = proj.fastProjectDataToRenderSpace(database.get(id));
        Element dot = svgp.svgCircle(v[0], v[1], dot_size);
        SVGUtil.addCSSClass(dot, MARKER);
        layer.appendChild(dot);
      }
    }

    @Override
    public void objectsChanged(@SuppressWarnings("unused") DatabaseEvent<NV> e) {
      synchronizedRedraw();
    }

    @Override
    public void objectsInserted(@SuppressWarnings("unused") DatabaseEvent<NV> e) {
      synchronizedRedraw();
    }

    @Override
    public void objectsRemoved(@SuppressWarnings("unused") DatabaseEvent<NV> e) {
      synchronizedRedraw();
    }
  }
}