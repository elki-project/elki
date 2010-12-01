package de.lmu.ifi.dbs.elki.visualization.visualizers.visunproj;

import java.io.File;
import java.io.IOException;
import java.util.Collection;

import org.apache.batik.util.SVGConstants;
import org.w3c.dom.Element;

import de.lmu.ifi.dbs.elki.data.Clustering;
import de.lmu.ifi.dbs.elki.data.DatabaseObject;
import de.lmu.ifi.dbs.elki.distance.distancevalue.Distance;
import de.lmu.ifi.dbs.elki.distance.distancevalue.DoubleDistance;
import de.lmu.ifi.dbs.elki.logging.LoggingUtil;
import de.lmu.ifi.dbs.elki.result.AnyResult;
import de.lmu.ifi.dbs.elki.result.ClusterOrderResult;
import de.lmu.ifi.dbs.elki.result.ResultUtil;
import de.lmu.ifi.dbs.elki.visualization.colors.ColorLibrary;
import de.lmu.ifi.dbs.elki.visualization.css.CSSClassManager.CSSNamingConflict;
import de.lmu.ifi.dbs.elki.visualization.opticsplot.OPTICSColorAdapter;
import de.lmu.ifi.dbs.elki.visualization.opticsplot.OPTICSColorFromClustering;
import de.lmu.ifi.dbs.elki.visualization.opticsplot.OPTICSPlot;
import de.lmu.ifi.dbs.elki.visualization.style.StyleLibrary;
import de.lmu.ifi.dbs.elki.visualization.svg.SVGSimpleLinearAxis;
import de.lmu.ifi.dbs.elki.visualization.svg.SVGUtil;
import de.lmu.ifi.dbs.elki.visualization.visualizers.AbstractVisualization;
import de.lmu.ifi.dbs.elki.visualization.visualizers.VisFactory;
import de.lmu.ifi.dbs.elki.visualization.visualizers.Visualization;
import de.lmu.ifi.dbs.elki.visualization.visualizers.VisualizationTask;
import de.lmu.ifi.dbs.elki.visualization.visualizers.VisualizerContext;

/**
 * Visualize an OPTICS result by constructing an OPTICS plot for it.
 * 
 * @author Erich Schubert
 * 
 * @apiviz.has StaticVisualization oneway - - produces
 * @apiviz.uses OPTICSPlot
 * @apiviz.has ClusterOrderResult oneway - - visualizes
 * 
 * @param <D> Distance type
 */
public class OPTICSPlotVisualizer<D extends Distance<D>> extends AbstractVisualization<DatabaseObject> {
  /**
   * Name for this visualizer.
   */
  private static final String NAME = "OPTICS Plot";

  /**
   * Curve to visualize
   */
  private final ClusterOrderResult<D> co;

  /**
   * The actual plot object.
   */
  private OPTICSPlot<D> opticsplot;

  /**
   * The image we generated.
   */
  private File imgfile;

  public OPTICSPlotVisualizer(VisualizationTask task) {
    super(task, VisFactory.LEVEL_STATIC);
    this.co = task.getResult();
  }

  /**
   * Make the optics plot
   * 
   * @throws IOException
   */
  protected void makePlot() throws IOException {
    final ColorLibrary colors = context.getStyleLibrary().getColorSet(StyleLibrary.PLOT);
    final Clustering<?> refc = context.getOrCreateDefaultClustering();
    final OPTICSColorAdapter opcolor = new OPTICSColorFromClustering(colors, refc);

    opticsplot = new OPTICSPlot<D>(co, opcolor);
    imgfile = opticsplot.getAsTempFile();
    opticsplot.forgetRenderedImage();
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
        makePlot();
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
   * @apiviz.has OPTICSPlotVisualizer
   */
  public static class Factory extends AbstractUnprojectedVisFactory<DatabaseObject> {
    /**
     * Constructor, adhering to
     * {@link de.lmu.ifi.dbs.elki.utilities.optionhandling.Parameterizable}
     */
    public Factory() {
      super(NAME);
    }

    @Override
    public void addVisualizers(VisualizerContext<? extends DatabaseObject> context, AnyResult result) {
      Collection<ClusterOrderResult<DoubleDistance>> cos = ResultUtil.filterResults(result, ClusterOrderResult.class);
      for(ClusterOrderResult<DoubleDistance> co : cos) {
        if(OPTICSPlot.canPlot(co)) {
          context.addVisualizer(co, this);
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