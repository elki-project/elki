package de.lmu.ifi.dbs.elki.visualization.visualizers.visunproj;

import java.awt.image.RenderedImage;
import java.io.File;
import java.io.IOException;
import java.util.Collection;

import javax.imageio.ImageIO;

import org.apache.batik.util.SVGConstants;
import org.w3c.dom.Element;

import de.lmu.ifi.dbs.elki.data.DatabaseObject;
import de.lmu.ifi.dbs.elki.logging.LoggingUtil;
import de.lmu.ifi.dbs.elki.result.PixmapResult;
import de.lmu.ifi.dbs.elki.result.Result;
import de.lmu.ifi.dbs.elki.result.ResultUtil;
import de.lmu.ifi.dbs.elki.visualization.projections.Projection;
import de.lmu.ifi.dbs.elki.visualization.style.StyleLibrary;
import de.lmu.ifi.dbs.elki.visualization.svg.SVGUtil;
import de.lmu.ifi.dbs.elki.visualization.visualizers.AbstractVisFactory;
import de.lmu.ifi.dbs.elki.visualization.visualizers.AbstractVisualization;
import de.lmu.ifi.dbs.elki.visualization.visualizers.Visualization;
import de.lmu.ifi.dbs.elki.visualization.visualizers.VisualizationTask;
import de.lmu.ifi.dbs.elki.visualization.visualizers.VisualizerContext;

/**
 * Visualize an arbitrary pixmap result.
 * 
 * @author Erich Schubert
 * 
 * @apiviz.has PixmapResult oneway - 1 visualizes
 */
public class PixmapVisualizer extends AbstractVisualization<DatabaseObject> {
  /**
   * Name for this visualizer.
   */
  private static final String NAME = "Pixmap visualizer";

  /**
   * Prefix for filenames
   */
  private static final String IMGFILEPREFIX = "elki-pixmap-";

  /**
   * The actual pixmap result.
   */
  private PixmapResult result;

  /**
   * The image file we generated.
   */
  private File imgfile;

  /**
   * Constructor.
   * 
   * @param task Visualization task
   */
  public PixmapVisualizer(VisualizationTask task) {
    super(task);
    this.result = task.getResult();
  }

  @Override
  protected void redraw() {
    // TODO: Use width, height, imgratio, number of OPTICS plots!
    double scale = StyleLibrary.SCALE;

    final double sizex = scale;
    final double sizey = scale * task.getHeight() / task.getWidth();
    final double margin = 0.0; // context.getStyleLibrary().getSize(StyleLibrary.MARGIN);
    layer = SVGUtil.svgElement(svgp.getDocument(), SVGConstants.SVG_G_TAG);
    final String transform = SVGUtil.makeMarginTransform(task.getWidth(), task.getHeight(), sizex, sizey, margin);
    SVGUtil.setAtt(layer, SVGConstants.SVG_TRANSFORM_ATTRIBUTE, transform);

    RenderedImage img = result.getImage();
    if(imgfile == null) {
      try {
        imgfile = File.createTempFile(IMGFILEPREFIX, ".png");
        imgfile.deleteOnExit();
        ImageIO.write(img, "PNG", imgfile);
      }
      catch(IOException e) {
        LoggingUtil.exception("Could not generate OPTICS plot.", e);
      }
    }
    // is ratio, target ratio
    double iratio = img.getHeight() / img.getWidth();
    double tratio = task.getHeight() / task.getWidth();
    // We want to place a (iratio, 1.0) object on a (tratio, 1.0) screen.
    // Both dimensions must fit:
    double zoom = (iratio >= tratio) ? Math.min(tratio / iratio, 1.0) : Math.max(iratio / tratio, 1.0);

    Element itag = svgp.svgElement(SVGConstants.SVG_IMAGE_TAG);
    SVGUtil.setAtt(itag, SVGConstants.SVG_IMAGE_RENDERING_ATTRIBUTE, SVGConstants.SVG_OPTIMIZE_SPEED_VALUE);
    SVGUtil.setAtt(itag, SVGConstants.SVG_X_ATTRIBUTE, 0);
    SVGUtil.setAtt(itag, SVGConstants.SVG_Y_ATTRIBUTE, 0);
    SVGUtil.setAtt(itag, SVGConstants.SVG_WIDTH_ATTRIBUTE, scale * zoom * iratio);
    SVGUtil.setAtt(itag, SVGConstants.SVG_HEIGHT_ATTRIBUTE, scale * zoom);
    itag.setAttributeNS(SVGConstants.XLINK_NAMESPACE_URI, SVGConstants.XLINK_HREF_QNAME, imgfile.toURI().toString());

    layer.appendChild(itag);
  }

  /**
   * Factory class for pixmap visualizers.
   * 
   * @author Erich Schubert
   * 
   * @apiviz.stereotype factory
   * @apiviz.uses PixmapVisualizer oneway - - «create»
   */
  public static class Factory extends AbstractVisFactory<DatabaseObject> {
    /**
     * Constructor, adhering to
     * {@link de.lmu.ifi.dbs.elki.utilities.optionhandling.Parameterizable}
     */
    public Factory() {
      super();
    }

    @Override
    public void addVisualizers(VisualizerContext<? extends DatabaseObject> context, Result result) {
      Collection<PixmapResult> prs = ResultUtil.filterResults(result, PixmapResult.class);
      for(PixmapResult pr : prs) {
        // Add plots, attach visualizer
        final VisualizationTask task = new VisualizationTask(NAME, context, pr, this, pr);
        task.put(VisualizationTask.META_LEVEL, VisualizationTask.LEVEL_STATIC);
        context.addVisualizer(pr, task);
      }
    }

    @Override
    public Visualization makeVisualization(VisualizationTask task) {
      return new PixmapVisualizer(task);
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