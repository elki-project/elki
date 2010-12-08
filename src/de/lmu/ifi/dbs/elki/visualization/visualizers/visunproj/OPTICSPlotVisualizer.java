package de.lmu.ifi.dbs.elki.visualization.visualizers.visunproj;

import java.io.File;
import java.io.IOException;
import java.util.Collection;

import org.apache.batik.util.SVGConstants;
import org.w3c.dom.Element;

import de.lmu.ifi.dbs.elki.data.DatabaseObject;
import de.lmu.ifi.dbs.elki.distance.distancevalue.Distance;
import de.lmu.ifi.dbs.elki.distance.distancevalue.DoubleDistance;
import de.lmu.ifi.dbs.elki.logging.LoggingUtil;
import de.lmu.ifi.dbs.elki.result.ClusterOrderResult;
import de.lmu.ifi.dbs.elki.result.Result;
import de.lmu.ifi.dbs.elki.result.ResultUtil;
import de.lmu.ifi.dbs.elki.visualization.css.CSSClassManager.CSSNamingConflict;
import de.lmu.ifi.dbs.elki.visualization.opticsplot.OPTICSPlot;
import de.lmu.ifi.dbs.elki.visualization.style.StyleLibrary;
import de.lmu.ifi.dbs.elki.visualization.svg.SVGSimpleLinearAxis;
import de.lmu.ifi.dbs.elki.visualization.svg.SVGUtil;
import de.lmu.ifi.dbs.elki.visualization.visualizers.AbstractVisualization;
import de.lmu.ifi.dbs.elki.visualization.visualizers.Visualization;
import de.lmu.ifi.dbs.elki.visualization.visualizers.VisualizationTask;
import de.lmu.ifi.dbs.elki.visualization.visualizers.VisualizerContext;

/**
 * Visualize an OPTICS result by constructing an OPTICS plot for it.
 * 
 * @author Erich Schubert
 * 
 * @apiviz.has OPTICSPlot oneway - 1 visualizes
 * @apiviz.has ClusterOrderResult oneway - 1 visualizes
 * 
 * @param <D> Distance type
 */
public class OPTICSPlotVisualizer<D extends Distance<D>> extends AbstractVisualization<DatabaseObject> {
  /**
   * Name for this visualizer.
   */
  private static final String NAME = "OPTICS Plot";

  /**
   * The actual plot object.
   */
  private OPTICSPlot<D> opticsplot;

  /**
   * The image we generated.
   */
  private File imgfile;

  public OPTICSPlotVisualizer(VisualizationTask task) {
    super(task, VisualizationTask.LEVEL_STATIC);
    this.opticsplot = task.getResult();
  }

  @Override
  protected void redraw() {
    // TODO: Use width, height, imgratio, number of OPTICS plots!
    double scale = StyleLibrary.SCALE;
    final double sizex = scale;
    final double sizey = scale * task.getHeight() / task.getWidth();
    final double margin = context.getStyleLibrary().getSize(StyleLibrary.MARGIN);
    layer = SVGUtil.svgElement(svgp.getDocument(), SVGConstants.SVG_G_TAG);
    final String transform = SVGUtil.makeMarginTransform(task.getWidth(), task.getHeight(), sizex, sizey, margin);
    SVGUtil.setAtt(layer, SVGConstants.SVG_TRANSFORM_ATTRIBUTE, transform);

    if(imgfile == null) {
      try {
        imgfile = opticsplot.getAsTempFile();
      }
      catch(IOException e) {
        LoggingUtil.exception("Could not generate OPTICS plot.", e);
      }
    }

    Element itag = svgp.svgElement(SVGConstants.SVG_IMAGE_TAG);
    SVGUtil.setAtt(itag, SVGConstants.SVG_IMAGE_RENDERING_ATTRIBUTE, SVGConstants.SVG_OPTIMIZE_SPEED_VALUE);
    SVGUtil.setAtt(itag, SVGConstants.SVG_X_ATTRIBUTE, 0);
    SVGUtil.setAtt(itag, SVGConstants.SVG_Y_ATTRIBUTE, 0);
    SVGUtil.setAtt(itag, SVGConstants.SVG_WIDTH_ATTRIBUTE, scale);
    SVGUtil.setAtt(itag, SVGConstants.SVG_HEIGHT_ATTRIBUTE, scale / opticsplot.getRatio());
    itag.setAttributeNS(SVGConstants.XLINK_NAMESPACE_URI, SVGConstants.XLINK_HREF_QNAME, imgfile.toURI().toString());

    layer.appendChild(itag);

    try {
      SVGSimpleLinearAxis.drawAxis(svgp, layer, opticsplot.getScale(), 0, scale / opticsplot.getRatio(), 0, 0, true, false, context.getStyleLibrary());
      SVGSimpleLinearAxis.drawAxis(svgp, layer, opticsplot.getScale(), scale, scale / opticsplot.getRatio(), scale, 0, true, true, context.getStyleLibrary());
    }
    catch(CSSNamingConflict e) {
      LoggingUtil.exception("CSS naming conflict for axes on OPTICS plot", e);
    }
  }

  /**
   * Factory class for OPTICS plot.
   * 
   * @author Erich Schubert
   * 
   * @apiviz.stereotype factory
   * @apiviz.uses OPTICSPlotVisualizer oneway - - «create»
   */
  public static class Factory extends UnpVisFactory<DatabaseObject> {
    /**
     * Constructor, adhering to
     * {@link de.lmu.ifi.dbs.elki.utilities.optionhandling.Parameterizable}
     */
    public Factory() {
      super();
    }

    @Override
    public void addVisualizers(VisualizerContext<? extends DatabaseObject> context, Result result) {
      Collection<ClusterOrderResult<DoubleDistance>> cos = ResultUtil.filterResults(result, ClusterOrderResult.class);
      for(ClusterOrderResult<DoubleDistance> co : cos) {
        // Add plots, attach visualizer
        OPTICSPlot<?> plot = OPTICSPlot.plotForClusterOrder(co, context);
        if(plot != null) {
          context.addVisualizer(plot, new VisualizationTask(NAME, context, plot, this));
        }
      }
    }

    @Override
    public Visualization makeVisualization(VisualizationTask task) {
      return new OPTICSPlotVisualizer<DoubleDistance>(task);
    }

    @Override
    public boolean allowThumbnails(@SuppressWarnings("unused") VisualizationTask task) {
      // Don't use thumbnails
      return false;
    }
  }
}