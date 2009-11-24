package experimentalcode.erich.visualization.visualizers.adapter;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import de.lmu.ifi.dbs.elki.data.NumberVector;
import de.lmu.ifi.dbs.elki.result.Result;
import de.lmu.ifi.dbs.elki.result.ResultUtil;
import de.lmu.ifi.dbs.elki.result.outlier.OutlierResult;
import de.lmu.ifi.dbs.elki.result.outlier.OutlierScoreMeta;
import experimentalcode.erich.visualization.visualizers.Visualizer;
import experimentalcode.erich.visualization.visualizers.VisualizerContext;
import experimentalcode.erich.visualization.visualizers.vis2d.BubbleVisualizer;
import experimentalcode.erich.visualization.visualizers.vis2d.TooltipVisualizer;

public class OutlierScoreAdapter<NV extends NumberVector<NV, ?>> implements AlgorithmAdapter<NV> {
  /**
   * Visualizes outlier-scores with bubbles.
   */
  protected BubbleVisualizer<NV> bubbleVisualizer;

  /**
   * Visualizes outlier-scores with tooltips.
   */
  protected TooltipVisualizer<NV> tooltipVisualizer;

  public OutlierScoreAdapter() {
    bubbleVisualizer = new BubbleVisualizer<NV>();
    tooltipVisualizer = new TooltipVisualizer<NV>();
  }

  // TODO: This should be done by the adapter itself (in a more specific way).
  // Eliminates the AssociationID here, too.
  public boolean canVisualize(Result result) {
    return ResultUtil.filterResults(result, OutlierResult.class).size() > 0;
  }

  @Override
  public Collection<Visualizer> getProvidedVisualizers() {
    Collection<Visualizer> c = new ArrayList<Visualizer>(2);
    c.add(bubbleVisualizer);
    c.add(tooltipVisualizer);
    return c;
  }

  @Override
  public Collection<Visualizer> getUsableVisualizers(VisualizerContext context) {
    List<OutlierResult> ors = ResultUtil.filterResults(context.getResult(), OutlierResult.class);
    Collection<Visualizer> c = new ArrayList<Visualizer>(2 * ors.size());
    for(OutlierResult o : ors) {
      OutlierScoreMeta meta = o.getOutlierMeta();
      // TODO: generate names.
      bubbleVisualizer.init(BubbleVisualizer.NAME, context, o.getScores(), meta);
      tooltipVisualizer.init(TooltipVisualizer.NAME, context, o.getScores());
      c.add(bubbleVisualizer);
      c.add(tooltipVisualizer);
    }
    return c;
  }
}
