package de.lmu.ifi.dbs.elki.visualization.visualizers.adapter;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import de.lmu.ifi.dbs.elki.data.NumberVector;
import de.lmu.ifi.dbs.elki.result.HistogramResult;
import de.lmu.ifi.dbs.elki.result.ResultUtil;
import de.lmu.ifi.dbs.elki.visualization.visualizers.Visualizer;
import de.lmu.ifi.dbs.elki.visualization.visualizers.VisualizerContext;
import de.lmu.ifi.dbs.elki.visualization.visualizers.visunproj.HistogramVisualizer;

/**
 * Adapter to visualize general Histogram results found in the result.
 * 
 * @author Erich Schubert
 */
public class HistogramAdapter implements AlgorithmAdapter {
  /**
   * Prototype, for parameterization.
   */
  private HistogramVisualizer vis = new HistogramVisualizer();

  /**
   * Constructor.
   */
  public HistogramAdapter() {
    super();
  }

  @Override
  public boolean canVisualize(VisualizerContext context) {
    List<HistogramResult<?>> histograms = ResultUtil.filterResults(context.getResult(), HistogramResult.class);
    return histograms.size() > 0;
  }

  @Override
  public Collection<Visualizer> getProvidedVisualizers() {
    ArrayList<Visualizer> providedVisualizers = new ArrayList<Visualizer>(1);
    providedVisualizers.add(vis);
    return providedVisualizers;
  }

  @Override
  public Collection<Visualizer> getUsableVisualizers(VisualizerContext context) {
    List<HistogramResult<? extends NumberVector<?,?>>> histograms = ResultUtil.filterResults(context.getResult(), HistogramResult.class);
    ArrayList<Visualizer> usableVisualizers = new ArrayList<Visualizer>(histograms.size());
    for (HistogramResult<? extends NumberVector<?,?>> histogram : histograms) {
      // TODO: check that the histogram bears number vectors!
      HistogramVisualizer histVis = new HistogramVisualizer();
      histVis.init(context, histogram);
      usableVisualizers.add(histVis);
    }
    return usableVisualizers;
  }
}