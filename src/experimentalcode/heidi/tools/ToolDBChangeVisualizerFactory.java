package experimentalcode.heidi.tools;

import de.lmu.ifi.dbs.elki.data.NumberVector;
import de.lmu.ifi.dbs.elki.visualization.VisualizationProjection;
import de.lmu.ifi.dbs.elki.visualization.svg.SVGPlot;
import de.lmu.ifi.dbs.elki.visualization.visualizers.Visualization;
import de.lmu.ifi.dbs.elki.visualization.visualizers.Visualizer;
import de.lmu.ifi.dbs.elki.visualization.visualizers.vis2d.Projection2DVisualizer;

/**
 * Factory for tool visualizations for changing objects in the database
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
//    svgp.setDisableInteractions(true);
    return new ToolDBChangeVisualizer<NV>(context, svgp, proj, width, height);
  }

}
