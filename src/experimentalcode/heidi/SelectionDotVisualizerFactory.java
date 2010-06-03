  package experimentalcode.heidi;

  import org.apache.batik.util.SVGConstants;
  import org.w3c.dom.Element;

  import de.lmu.ifi.dbs.elki.data.NumberVector;
  import de.lmu.ifi.dbs.elki.database.Database;
  import de.lmu.ifi.dbs.elki.database.ids.DBID;
  import de.lmu.ifi.dbs.elki.database.ids.DBIDs;
  import de.lmu.ifi.dbs.elki.math.linearalgebra.Vector;
  import de.lmu.ifi.dbs.elki.visualization.VisualizationProjection;
  import de.lmu.ifi.dbs.elki.visualization.css.CSSClass;
  import de.lmu.ifi.dbs.elki.visualization.css.CSSClassManager.CSSNamingConflict;
  import de.lmu.ifi.dbs.elki.visualization.style.StyleLibrary;
  import de.lmu.ifi.dbs.elki.visualization.svg.SVGPlot;
  import de.lmu.ifi.dbs.elki.visualization.svg.SVGUtil;
  import de.lmu.ifi.dbs.elki.visualization.visualizers.StaticVisualization;
  import de.lmu.ifi.dbs.elki.visualization.visualizers.Visualization;
  import de.lmu.ifi.dbs.elki.visualization.visualizers.Visualizer;
  import de.lmu.ifi.dbs.elki.visualization.visualizers.VisualizerContext;
  import de.lmu.ifi.dbs.elki.visualization.visualizers.events.ContextChangeListener;
  import de.lmu.ifi.dbs.elki.visualization.visualizers.events.ContextChangedEvent;
  import de.lmu.ifi.dbs.elki.visualization.visualizers.vis2d.Projection2DVisualization;
import de.lmu.ifi.dbs.elki.visualization.visualizers.vis2d.Projection2DVisualizer;

  /**
   * Generates an SVG-Element containing "dots" as markers representing the
   * selected Database's objects.
   * 
   * @author
   * 
   * @param <NV> Type of the NumberVector being visualized.
   */
  public class SelectionDotVisualizerFactory<NV extends NumberVector<NV, ?>> extends Projection2DVisualizer<NV> {

    /**
     * A short name characterizing this Visualizer.
     */
    private static final String NAME = "Heidi SelectionDotVisualizer";

    /**
     * Generic tag to indicate the type of element. Used in IDs, CSS-Classes etc.
     */
    public static final String MARKER = "selectionDotMarker";

    Element layer;

    Element svgTag;

    SVGPlot svgp;

    VisualizationProjection proj;

    /**
     * Initializes this Visualizer.
     * 
     * @param context Visualization context
     */
    public void init(VisualizerContext<? extends NV> context) {
      super.init(NAME, context);
    }

    @Override
    public Visualization visualize(SVGPlot svgpPlot, VisualizationProjection proj, double width, double height) {

      final SVGPlot svgp = svgpPlot;
      addCSSClasses(svgp);
      svgp.setDisableInteractions(true);

      // TODO: Selection class in context
      // Selection selection = context.get("selection", Selection.class);
      // context.put("selection", selection);

      try {
        SelectionDotVisualizer<NV> dotvis = getVisualizer();
        dotvis.init(context);
        Visualization layer = dotvis.visualize(svgp, proj, width, height);
        return layer;
      }
      catch(Exception e) {
        logger.exception(e);
        if(e instanceof RuntimeException) {
          throw (RuntimeException) e;
        }
        throw new RuntimeException(e);
      }
    }
    /**
     * Provides a new Visualizer (Factory-Pattern)
     * 
     * @return SelectionDefinition<NV>
     */
    public SelectionDotVisualizer<NV> getVisualizer() {
      return new SelectionDotVisualizer<NV>();
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
        cls.setStatement(SVGConstants.CSS_OPACITY_PROPERTY, "0.5");
        try {
          svgp.getCSSClassManager().addClass(cls);
        }
        catch(CSSNamingConflict e) {
          de.lmu.ifi.dbs.elki.logging.LoggingUtil.exception(e);
        }
        catch(Exception e) {
          de.lmu.ifi.dbs.elki.logging.LoggingUtil.exception(e);
        }
      }
    }
  }
