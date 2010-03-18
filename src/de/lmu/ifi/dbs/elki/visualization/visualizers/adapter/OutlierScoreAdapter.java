package de.lmu.ifi.dbs.elki.visualization.visualizers.adapter;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import de.lmu.ifi.dbs.elki.data.NumberVector;
import de.lmu.ifi.dbs.elki.logging.LoggingUtil;
import de.lmu.ifi.dbs.elki.result.ResultUtil;
import de.lmu.ifi.dbs.elki.result.outlier.OutlierResult;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.ParameterException;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.MergedParameterization;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.Parameterization;
import de.lmu.ifi.dbs.elki.visualization.visualizers.Visualizer;
import de.lmu.ifi.dbs.elki.visualization.visualizers.VisualizerContext;
import de.lmu.ifi.dbs.elki.visualization.visualizers.vis2d.BubbleVisualizer;
import de.lmu.ifi.dbs.elki.visualization.visualizers.vis2d.TooltipVisualizer;

/**
 * This class activates bubble and tooltip visualizers when there is an Outlier
 * result found.
 * 
 * @author Erich Schubert
 * @author Remigius Wojdanowski
 * 
 * @param <NV>
 */
public class OutlierScoreAdapter<NV extends NumberVector<NV, ?>> implements AlgorithmAdapter {
  /**
   * Visualizes outlier-scores with bubbles.
   */
  private  BubbleVisualizer<NV> bubbleVisualizer;

  /**
   * Visualizes outlier-scores with tooltips.
   */
  private  TooltipVisualizer<NV> tooltipVisualizer;
  
  /**
   * Track parameters for subclasses for "replay".
   */
  private MergedParameterization reconfig;

  /**
   * Constructor, adhering to
   * {@link de.lmu.ifi.dbs.elki.utilities.optionhandling.Parameterizable}
   * 
   * @param config Parameterization
   */
  public OutlierScoreAdapter(Parameterization config) {
    super();
    this.reconfig = new MergedParameterization(config);
    bubbleVisualizer = new BubbleVisualizer<NV>(reconfig);
    tooltipVisualizer = new TooltipVisualizer<NV>(reconfig);
  }

  @Override
  public boolean canVisualize(VisualizerContext context) {
    return ResultUtil.filterResults(context.getResult(), OutlierResult.class).size() > 0;
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
    int cnt = 0;
    for(OutlierResult o : ors) {
      String postfix = (cnt > 0) ? (" "+cnt) : "";
      // Clone visualizers:
      reconfig.rewind();
      BubbleVisualizer<NV> bv = new BubbleVisualizer<NV>(reconfig);
      TooltipVisualizer<NV> tv = new TooltipVisualizer<NV>(reconfig);
      if (reconfig.getErrors().size() != 0) {
        for (ParameterException err : reconfig.getErrors()) {
          LoggingUtil.warning("Error in reconfiguration:", err);
        }
      }
      bv.init(BubbleVisualizer.NAME + postfix, context, o);
      tv.init(TooltipVisualizer.NAME + postfix, context, o);
      c.add(bv);
      c.add(tv);
      cnt++;
    }
    return c;
  }
}