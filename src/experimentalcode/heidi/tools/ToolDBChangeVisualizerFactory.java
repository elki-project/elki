package experimentalcode.heidi.tools;

import org.apache.batik.util.SVGConstants;

import de.lmu.ifi.dbs.elki.data.NumberVector;
import de.lmu.ifi.dbs.elki.visualization.VisualizationProjection;
import de.lmu.ifi.dbs.elki.visualization.css.CSSClass;
import de.lmu.ifi.dbs.elki.visualization.css.CSSClassManager.CSSNamingConflict;
import de.lmu.ifi.dbs.elki.visualization.svg.SVGPlot;
import de.lmu.ifi.dbs.elki.visualization.visualizers.Visualization;
import de.lmu.ifi.dbs.elki.visualization.visualizers.Visualizer;
import de.lmu.ifi.dbs.elki.visualization.visualizers.vis2d.Projection2DVisualizer;

/**
 * Factory for visualization an SVG-Element containing "dots" as markers
 * representing the selected Database's objects.
 * 
 * @author
 * 
 * @param <NV> Type of the NumberVector being visualized.
 */
public class ToolDBChangeVisualizerFactory<NV extends NumberVector<NV, ?>> extends Projection2DVisualizer<NV> {

  /**
   * A short name characterizing this Visualizer.
   */
  private static final String NAME = "Heidi ToolDBChangeVisualizer";

  /**
   * Generic tag to indicate the type of element. Used in IDs, CSS-Classes etc.
   */
  // TODO: protected, oder in DotVisualizer nochmal definieren?
  protected static final String MARKER = "selectionDotMarker";

  /**
   * Constructor, adhering to
   * {@link de.lmu.ifi.dbs.elki.utilities.optionhandling.Parameterizable}
   */
  public ToolDBChangeVisualizerFactory() {
    super(NAME);
    super.metadata.put(Visualizer.META_TOOL, true);
    super.metadata.put(Visualizer.META_NOTHUMB, true);
  }
  
  @Override
  public Visualization visualize(SVGPlot svgp, VisualizationProjection proj, double width, double height) {
    // TODO: disableInteractions should be handled by the plot window.
    svgp.setDisableInteractions(true);
    addCSSClasses(svgp);
    return new ToolDBChangeVisualizer<NV>(context, svgp, proj, width, height);
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
    }
  }
}
