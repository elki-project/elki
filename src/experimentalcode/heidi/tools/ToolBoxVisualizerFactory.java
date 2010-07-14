package experimentalcode.heidi.tools;

import de.lmu.ifi.dbs.elki.data.NumberVector;
import de.lmu.ifi.dbs.elki.visualization.VisualizationProjection;
import de.lmu.ifi.dbs.elki.visualization.svg.SVGPlot;
import de.lmu.ifi.dbs.elki.visualization.visualizers.Visualization;
import de.lmu.ifi.dbs.elki.visualization.visualizers.Visualizer;
import de.lmu.ifi.dbs.elki.visualization.visualizers.vis2d.Projection2DVisualizer;

/**
 * Factory for visualizers for a toolbox 
 *  
 * @author Heidi Kolb
 * 
 * @param <NV> Type of the NumberVector being visualized.
 */
public class ToolBoxVisualizerFactory<NV extends NumberVector<NV, ?>> extends Projection2DVisualizer<NV> {

  /**
   * A short name characterizing this Visualizer.
   */
  private static final String NAME = "ToolBoxVisualizer";

  /**
   * Generic tag to indicate the type of element. Used in IDs, CSS-Classes etc.
   */
  protected static final String MARKER = "selectionDotMarker";

  /**
   * Constructor
   */
  public ToolBoxVisualizerFactory() {
    super(NAME);
    super.metadata.put(Visualizer.META_NOTHUMB, true);
    super.metadata.put(Visualizer.META_NOEXPORT, true);
  }

  @Override
  public Visualization visualize(SVGPlot svgp, VisualizationProjection proj, double width, double height) {
    return new ToolBoxVisualizer<NV>(context, svgp, proj, width, height);
  }
}
