package de.lmu.ifi.dbs.elki.visualization.visualizers.adapter;

import java.util.ArrayList;
import java.util.Collection;

import de.lmu.ifi.dbs.elki.data.NumberVector;
import de.lmu.ifi.dbs.elki.database.Database;
import de.lmu.ifi.dbs.elki.result.AnnotationBuiltins;
import de.lmu.ifi.dbs.elki.result.AnyResult;
import de.lmu.ifi.dbs.elki.result.ResultUtil;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.MergedParameterization;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.Parameterization;
import de.lmu.ifi.dbs.elki.visualization.visualizers.SelectionResult;
import de.lmu.ifi.dbs.elki.visualization.visualizers.Visualizer;
import de.lmu.ifi.dbs.elki.visualization.visualizers.VisualizerContext;
import de.lmu.ifi.dbs.elki.visualization.visualizers.VisualizerUtil;
import de.lmu.ifi.dbs.elki.visualization.visualizers.vis1d.Projection1DHistogramVisualizer;
import de.lmu.ifi.dbs.elki.visualization.visualizers.vis2d.AxisVisualizer;
import de.lmu.ifi.dbs.elki.visualization.visualizers.vis2d.SelectionCubeVisualizer;
import de.lmu.ifi.dbs.elki.visualization.visualizers.vis2d.SelectionDotVisualizer;
import de.lmu.ifi.dbs.elki.visualization.visualizers.vis2d.SelectionToolCubeVisualizer;
import de.lmu.ifi.dbs.elki.visualization.visualizers.vis2d.SelectionToolDotVisualizer;
import de.lmu.ifi.dbs.elki.visualization.visualizers.vis2d.ToolBox2D;
import de.lmu.ifi.dbs.elki.visualization.visualizers.vis2d.TooltipClassLabelVisualizer;
import de.lmu.ifi.dbs.elki.visualization.visualizers.vis2d.TooltipObjectLabelVisualizer;

/**
 * Class to add various default visualizations.
 * 
 * @author Erich Schubert
 * 
 * @param <NV> Vector type
 */
public class NumberVectorAdapter<NV extends NumberVector<NV, ?>> implements AlgorithmAdapter<NV> {
  /**
   * Visualizer to draw the axes on 2D projections
   */
  private AxisVisualizer<NV> axisVisualizer;

  /**
   * Visualizer to draw 1D distribution histograms
   */
  private Projection1DHistogramVisualizer<NV> histoVisualizer;

  /**
   * Visualizer for drawing a tool box in the 2D projections
   */
  private ToolBox2D<NV> toolBoxVisualizer;

  /**
   * Visualizer for class labels
   */
  private TooltipClassLabelVisualizer<NV> tooltipClassLabelVisualizer;

  /**
   * Visualizer for object labels
   */
  private TooltipObjectLabelVisualizer<NV> tooltipObjectLabelVisualizer;

  /**
   * Selection visualizer
   */
  private SelectionDotVisualizer<NV> selectionDotVisualizer;

  /**
   * Range selection visualizer
   */
  private SelectionCubeVisualizer<NV> selectionCubeVisualizer;

  /**
   * Tool to select arbitrary points
   */
  private SelectionToolDotVisualizer<NV> selectionToolDotVisualizer;

  /**
   * Tool for multidimensional range selection
   */
  private SelectionToolCubeVisualizer<NV> selectionToolRangeVisualizer;

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
  public NumberVectorAdapter(Parameterization config) {
    super();
    config = config.descend(this);
    reconfig = new MergedParameterization(config);
    axisVisualizer = new AxisVisualizer<NV>();
    histoVisualizer = new Projection1DHistogramVisualizer<NV>(reconfig);
    toolBoxVisualizer = new ToolBox2D<NV>();
    tooltipClassLabelVisualizer = new TooltipClassLabelVisualizer<NV>();
    tooltipObjectLabelVisualizer = new TooltipObjectLabelVisualizer<NV>();
    selectionDotVisualizer = new SelectionDotVisualizer<NV>();
    selectionCubeVisualizer = new SelectionCubeVisualizer<NV>(reconfig);
    selectionToolDotVisualizer = new SelectionToolDotVisualizer<NV>();
    selectionToolRangeVisualizer = new SelectionToolCubeVisualizer<NV>();
  }

  @Override
  public Collection<Visualizer> getProvidedVisualizers() {
    ArrayList<Visualizer> providedVisualizers = new ArrayList<Visualizer>(9);
    providedVisualizers.add(axisVisualizer);
    providedVisualizers.add(histoVisualizer);
    providedVisualizers.add(toolBoxVisualizer);
    providedVisualizers.add(tooltipClassLabelVisualizer);
    providedVisualizers.add(tooltipObjectLabelVisualizer);
    providedVisualizers.add(selectionDotVisualizer);
    providedVisualizers.add(selectionCubeVisualizer);
    providedVisualizers.add(selectionToolDotVisualizer);
    providedVisualizers.add(selectionToolRangeVisualizer);
    return providedVisualizers;
  }

  @Override
  public void addVisualizers(VisualizerContext<? extends NV> context, AnyResult result) {
    ArrayList<Database<?>> databases = ResultUtil.filterResults(result, Database.class);
    for(Database<?> database : databases) {
      if(!VisualizerUtil.isNumberVectorDatabase(database)) {
        return;
      }
      // Create and init new visualizers.
      AxisVisualizer<NV> aVis = new AxisVisualizer<NV>();
      aVis.init(context);
      context.addVisualization(database, aVis);

      reconfig.rewind();
      Projection1DHistogramVisualizer<NV> hVis = new Projection1DHistogramVisualizer<NV>(reconfig);
      hVis.init(context);
      context.addVisualization(database, hVis);

      ToolBox2D<NV> tbVis = new ToolBox2D<NV>();
      tbVis.init(context);
      context.addVisualization(database, tbVis);
    }
    // Label results.
    ArrayList<AnnotationBuiltins.ClassLabelAnnotation> classlabels = ResultUtil.filterResults(result, AnnotationBuiltins.ClassLabelAnnotation.class);
    for (AnnotationBuiltins.ClassLabelAnnotation clr : classlabels) {
      TooltipClassLabelVisualizer<NV> cvis = new TooltipClassLabelVisualizer<NV>();
      cvis.init(context, clr);
      context.addVisualization(clr, cvis);
    }
    ArrayList<AnnotationBuiltins.ObjectLabelAnnotation> objlabels = ResultUtil.filterResults(result, AnnotationBuiltins.ObjectLabelAnnotation.class);
    for (AnnotationBuiltins.ObjectLabelAnnotation olr : objlabels) {
      TooltipObjectLabelVisualizer<NV> ovis = new TooltipObjectLabelVisualizer<NV>();
      ovis.init(context, olr);
      context.addVisualization(olr, ovis);
    }
    // Add the selection visualizers and tools to the root result
    ArrayList<SelectionResult> selections = ResultUtil.filterResults(result, SelectionResult.class);
    for(SelectionResult selRes : selections) {
      selectionDotVisualizer.init(context);
      selectionCubeVisualizer.init(context);
      selectionToolDotVisualizer.init(context);
      selectionToolRangeVisualizer.init(context);
      context.addVisualization(selRes, selectionDotVisualizer);
      context.addVisualization(selRes, selectionCubeVisualizer);
      context.addVisualization(selRes, selectionToolDotVisualizer);
      context.addVisualization(selRes, selectionToolRangeVisualizer);
    }
  }
}