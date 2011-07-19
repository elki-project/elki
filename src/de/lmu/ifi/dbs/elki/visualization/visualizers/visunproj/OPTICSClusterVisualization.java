package de.lmu.ifi.dbs.elki.visualization.visualizers.visunproj;

import java.util.Collection;
import java.util.List;

import org.apache.batik.util.SVGConstants;
import org.w3c.dom.Element;

import de.lmu.ifi.dbs.elki.data.Cluster;
import de.lmu.ifi.dbs.elki.data.Clustering;
import de.lmu.ifi.dbs.elki.data.model.OPTICSModel;
import de.lmu.ifi.dbs.elki.distance.distancevalue.DoubleDistance;
import de.lmu.ifi.dbs.elki.logging.Logging;
import de.lmu.ifi.dbs.elki.result.HierarchicalResult;
import de.lmu.ifi.dbs.elki.result.Result;
import de.lmu.ifi.dbs.elki.result.ResultUtil;
import de.lmu.ifi.dbs.elki.result.SelectionResult;
import de.lmu.ifi.dbs.elki.result.optics.ClusterOrderResult;
import de.lmu.ifi.dbs.elki.visualization.css.CSSClass;
import de.lmu.ifi.dbs.elki.visualization.opticsplot.OPTICSPlot;
import de.lmu.ifi.dbs.elki.visualization.projections.Projection;
import de.lmu.ifi.dbs.elki.visualization.style.StyleLibrary;
import de.lmu.ifi.dbs.elki.visualization.svg.SVGUtil;
import de.lmu.ifi.dbs.elki.visualization.visualizers.AbstractVisFactory;
import de.lmu.ifi.dbs.elki.visualization.visualizers.AbstractVisualization;
import de.lmu.ifi.dbs.elki.visualization.visualizers.Visualization;
import de.lmu.ifi.dbs.elki.visualization.visualizers.VisualizationTask;
import de.lmu.ifi.dbs.elki.visualization.visualizers.VisualizerContext;
import de.lmu.ifi.dbs.elki.visualization.visualizers.VisualizerUtil;

/**
 * Visualize the clusters and cluster hierarchy found by OPTICS on the OPTICS
 * Plot.
 * 
 * @author Erich Schubert
 * 
 * @apiviz.uses ClusterOrderResult
 * @apiviz.uses OPTICSPlot
 */
public class OPTICSClusterVisualization extends AbstractVisualization {
  /**
   * The logger for this class.
   */
  private static final Logging logger = Logging.getLogger(OPTICSClusterVisualization.class);

  /**
   * A short name characterizing this Visualizer.
   */
  private static final String NAME = "OPTICS Cluster Range";

  /**
   * CSS class for markers
   */
  protected static final String CSS_BRACKET = "opticsBracket";

  /**
   * Our cluster order
   */
  private ClusterOrderResult<?> co;

  /**
   * Our clustering
   */
  Clustering<OPTICSModel> clus;

  /**
   * The plot
   */
  private OPTICSPlot<?> opticsplot;

  /**
   * Constructor.
   * 
   * @param task Visualization task
   */
  public OPTICSClusterVisualization(VisualizationTask task) {
    super(task);
    this.co = task.getResult();
    this.clus = findOPTICSClustering(this.co);
    this.opticsplot = OPTICSPlot.plotForClusterOrder(this.co, context);
    context.addResultListener(this);
    incrementalRedraw();
  }

  /**
   * Find the OPTICS clustering child of a cluster order.
   * 
   * @param co Cluster order
   * @return OPTICS clustering
   */
  @SuppressWarnings("unchecked")
  protected static Clustering<OPTICSModel> findOPTICSClustering(ClusterOrderResult<?> co) {
    for(Result r : co.getHierarchy().getChildren(co)) {
      if(!Clustering.class.isInstance(r)) {
        continue;
      }
      Clustering<?> clus = (Clustering<?>) r;
      if(clus.getToplevelClusters().size() == 0) {
        continue;
      }
      Cluster<?> firstcluster = clus.getToplevelClusters().iterator().next();
      if(firstcluster.getModel() instanceof OPTICSModel) {
        return (Clustering<OPTICSModel>) clus;
      }
    }
    return null;
  }

  @Override
  protected void redraw() {
    final double scale = StyleLibrary.SCALE;
    final double sizex = scale;
    final double sizey = scale * task.getHeight() / task.getWidth();
    final double margin = context.getStyleLibrary().getSize(StyleLibrary.MARGIN);
    layer = SVGUtil.svgElement(svgp.getDocument(), SVGConstants.SVG_G_TAG);
    final String transform = SVGUtil.makeMarginTransform(task.getWidth(), task.getHeight(), sizex, sizey, margin);
    SVGUtil.setAtt(layer, SVGConstants.SVG_TRANSFORM_ATTRIBUTE, transform);

    addCSSClasses();

    drawClusters(scale, scale / opticsplot.getRatio(), clus.getToplevelClusters(), 1);
  }

  /**
   * Recursively draw clusters
   * 
   * @param sizex Width
   * @param sizey Height of optics plot
   * @param clusters Current set of clusters
   * @param depth Recursion depth
   */
  private void drawClusters(double sizex, double sizey, List<Cluster<OPTICSModel>> clusters, int depth) {
    final double scale = StyleLibrary.SCALE;
    for(Cluster<OPTICSModel> cluster : clusters) {
      try {
        OPTICSModel model = cluster.getModel();
        final double x1 = sizex * ((model.getStartIndex() + .25) / this.co.getClusterOrder().size());
        final double x2 = sizex * ((model.getEndIndex() + .75) / this.co.getClusterOrder().size());
        final double y = sizey + depth * scale * 0.01;
        Element e = svgp.svgLine(x1, y, x2, y);
        SVGUtil.addCSSClass(e, CSS_BRACKET);
        layer.appendChild(e);
      }
      catch(ClassCastException e) {
        logger.warning("Expected OPTICSModel, got: " + cluster.getModel().getClass().getSimpleName());
      }
      // Descend
      final List<Cluster<OPTICSModel>> children = cluster.getChildren();
      if(children != null) {
        drawClusters(sizex, sizey, children, depth + 1);
      }
    }
  }

  /**
   * Adds the required CSS-Classes
   */
  private void addCSSClasses() {
    // Class for the markers
    if(!svgp.getCSSClassManager().contains(CSS_BRACKET)) {
      final CSSClass cls = new CSSClass(this, CSS_BRACKET);
      cls.setStatement(SVGConstants.CSS_STROKE_PROPERTY, context.getStyleLibrary().getColor(StyleLibrary.PLOT));
      cls.setStatement(SVGConstants.CSS_STROKE_WIDTH_PROPERTY, context.getStyleLibrary().getLineWidth(StyleLibrary.PLOT));
      svgp.addCSSClassOrLogError(cls);
    }
  }

  @Override
  public void resultChanged(Result current) {
    if(current instanceof SelectionResult || current == co || current == opticsplot) {
      synchronizedRedraw();
      return;
    }
    super.resultChanged(current);
  }

  /**
   * Factory class for OPTICS plot selections.
   * 
   * @author Erich Schubert
   * 
   * @apiviz.stereotype factory
   * @apiviz.uses OPTICSPlotSelectionVisualization oneway - - «create»
   */
  public static class Factory extends AbstractVisFactory {
    /**
     * Constructor, adhering to
     * {@link de.lmu.ifi.dbs.elki.utilities.optionhandling.Parameterizable}
     */
    public Factory() {
      super();
    }

    @Override
    public void processNewResult(HierarchicalResult baseResult, Result result) {
      VisualizerContext context = VisualizerUtil.getContext(baseResult);
      Collection<ClusterOrderResult<DoubleDistance>> cos = ResultUtil.filterResults(result, ClusterOrderResult.class);
      for(ClusterOrderResult<DoubleDistance> co : cos) {
        // Add plots, attach visualizer
        OPTICSPlot<?> plot = OPTICSPlot.plotForClusterOrder(co, context);
        if(plot != null && findOPTICSClustering(co) != null) {
          final VisualizationTask task = new VisualizationTask(NAME, co, null, this, plot);
          task.put(VisualizationTask.META_LEVEL, VisualizationTask.LEVEL_INTERACTIVE);
          baseResult.getHierarchy().add(plot, task);
        }
      }
    }

    @Override
    public Visualization makeVisualization(VisualizationTask task) {
      return new OPTICSClusterVisualization(task);
    }

    @Override
    public boolean allowThumbnails(@SuppressWarnings("unused") VisualizationTask task) {
      // Don't use thumbnails
      return false;
    }

    @Override
    public Class<? extends Projection> getProjectionType() {
      return null;
    }
  }
}