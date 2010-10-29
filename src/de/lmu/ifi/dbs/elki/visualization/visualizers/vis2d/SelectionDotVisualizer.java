package de.lmu.ifi.dbs.elki.visualization.visualizers.vis2d;

import org.apache.batik.util.SVGConstants;
import org.w3c.dom.Element;

import de.lmu.ifi.dbs.elki.data.NumberVector;
import de.lmu.ifi.dbs.elki.database.Database;
import de.lmu.ifi.dbs.elki.database.datastore.DataStoreEvent;
import de.lmu.ifi.dbs.elki.database.datastore.DataStoreListener;
import de.lmu.ifi.dbs.elki.database.ids.DBID;
import de.lmu.ifi.dbs.elki.database.ids.DBIDs;
import de.lmu.ifi.dbs.elki.visualization.css.CSSClass;
import de.lmu.ifi.dbs.elki.visualization.projections.Projection2D;
import de.lmu.ifi.dbs.elki.visualization.style.StyleLibrary;
import de.lmu.ifi.dbs.elki.visualization.svg.SVGPlot;
import de.lmu.ifi.dbs.elki.visualization.svg.SVGUtil;
import de.lmu.ifi.dbs.elki.visualization.visualizers.DBIDSelection;
import de.lmu.ifi.dbs.elki.visualization.visualizers.Visualization;
import de.lmu.ifi.dbs.elki.visualization.visualizers.Visualizer;
import de.lmu.ifi.dbs.elki.visualization.visualizers.VisualizerContext;
import de.lmu.ifi.dbs.elki.visualization.visualizers.events.ContextChangeListener;
import de.lmu.ifi.dbs.elki.visualization.visualizers.events.ContextChangedEvent;
import de.lmu.ifi.dbs.elki.visualization.visualizers.events.SelectionChangedEvent;
import de.lmu.ifi.dbs.elki.visualization.visualizers.thumbs.ProjectedThumbnail;
import de.lmu.ifi.dbs.elki.visualization.visualizers.thumbs.ThumbnailVisualization;

/**
 * Factory for visualizers to generate an SVG-Element containing dots as markers
 * representing the selected Database's objects.
 * 
 * @author Heidi Kolb
 * 
 * @param <NV> Type of the NumberVector being visualized.
 */
public class SelectionDotVisualizer<NV extends NumberVector<NV, ?>> extends Projection2DVisualizer<NV> {
  /**
   * A short name characterizing this Visualizer.
   */
  private static final String NAME = "Selection";

  /**
   * Constructor
   */
  public SelectionDotVisualizer() {
    super(NAME);
    super.metadata.put(Visualizer.META_GROUP, Visualizer.GROUP_TOOLS);
  }

  @Override
  public Visualization visualize(SVGPlot svgp, Projection2D proj, double width, double height) {
    return new SelectionDotVisualization<NV>(context, svgp, proj, width, height);
  }

  @Override
  public Visualization makeThumbnail(SVGPlot svgp, Projection2D proj, double width, double height, int tresolution) {
    return new ProjectedThumbnail<NV, Projection2D>(this, context, svgp, proj, width, height, tresolution, ThumbnailVisualization.ON_DATA | ThumbnailVisualization.ON_SELECTION);
  }

  /**
   * Visualizer for generating an SVG-Element containing dots as markers
   * representing the selected Database's objects.
   * 
   * @author Heidi Kolb
   * 
   * @param <NV> Type of the NumberVector being visualized.
   */
  public static class SelectionDotVisualization<NV extends NumberVector<NV, ?>> extends Projection2DVisualization<NV> implements ContextChangeListener, DataStoreListener<NV> {
    /**
     * Generic tag to indicate the type of element. Used in IDs, CSS-Classes
     * etc.
     */
    public static final String MARKER = "selectionDotMarker";

    /**
     * The actual visualization instance, for a single projection
     * 
     * @param context The context
     * @param svgp The SVGPlot
     * @param proj The Projection
     * @param width The width
     * @param height The height
     */
    public SelectionDotVisualization(VisualizerContext<? extends NV> context, SVGPlot svgp, Projection2D proj, double width, double height) {
      super(context, svgp, proj, width, height, Visualizer.LEVEL_DATA - 1);
      context.addContextChangeListener(this);
      context.addDataStoreListener(this);
      incrementalRedraw();
    }

    @Override
    protected boolean testRedraw(ContextChangedEvent e) {
      return super.testRedraw(e) || (e instanceof SelectionChangedEvent);
    }

    @Override
    protected void redraw() {
      addCSSClasses(svgp);
      DBIDSelection selContext = context.getSelection();
      if(selContext != null) {
        Database<? extends NV> database = context.getDatabase();
        DBIDs selection = selContext.getSelectedIds();
        final double linewidth = 3 * context.getStyleLibrary().getLineWidth(StyleLibrary.PLOT);
        for(DBID i : selection) {
          double[] v = proj.fastProjectDataToRenderSpace(database.get(i));
          Element dot = svgp.svgCircle(v[0], v[1], linewidth);
          SVGUtil.addCSSClass(dot, MARKER);
          layer.appendChild(dot);
        }
      }
    }

    /**
     * Adds the required CSS-Classes
     * 
     * @param svgp SVG-Plot
     */
    private void addCSSClasses(SVGPlot svgp) {
      // Class for the dot markers
      if(!svgp.getCSSClassManager().contains(MARKER)) {
        CSSClass cls = new CSSClass(this, MARKER);
        cls.setStatement(SVGConstants.CSS_FILL_PROPERTY, SVGConstants.CSS_RED_VALUE);
        cls.setStatement(SVGConstants.CSS_OPACITY_PROPERTY, "0.3");
        svgp.addCSSClassOrLogError(cls);
      }
    }

    @Override
    public void contentChanged(@SuppressWarnings("unused") DataStoreEvent<NV> e) {
      synchronizedRedraw();
    }
  }
}
