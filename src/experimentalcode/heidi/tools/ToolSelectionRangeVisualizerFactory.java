package experimentalcode.heidi.tools;

import de.lmu.ifi.dbs.elki.data.NumberVector;
import de.lmu.ifi.dbs.elki.visualization.VisualizationProjection;
import de.lmu.ifi.dbs.elki.visualization.svg.SVGPlot;
import de.lmu.ifi.dbs.elki.visualization.visualizers.Visualization;
import de.lmu.ifi.dbs.elki.visualization.visualizers.Visualizer;
import de.lmu.ifi.dbs.elki.visualization.visualizers.vis2d.Projection2DVisualizer;

/**
 * Factory for tool visualizations for selecting ranges and the inclosed objects
 * 
 * @author Heidi Kolb
 * 
 * @param <NV> Type of the NumberVector being visualized.
 */
public class ToolSelectionRangeVisualizerFactory<NV extends NumberVector<NV, ?>> extends Projection2DVisualizer<NV> {

  /**
   * A short name characterizing this Visualizer.
   */
  private static final String NAME = "Range Selection";

  /**
   * Constructor, adhering to
   * {@link de.lmu.ifi.dbs.elki.utilities.optionhandling.Parameterizable}
   */
  public ToolSelectionRangeVisualizerFactory() {
    super(NAME);
    super.metadata.put(Visualizer.META_TOOL, true);
    super.metadata.put(Visualizer.META_GROUP, Visualizer.GROUP_TOOLS);
    super.metadata.put(Visualizer.META_NOTHUMB, true);
    super.metadata.put(Visualizer.META_NOEXPORT, true);
  }

  @Override
  public Visualization visualize(SVGPlot svgp, VisualizationProjection proj, double width, double height) {
    return new ToolSelectionRangeVisualizer<NV>(context, svgp, proj, width, height);
  }
}
