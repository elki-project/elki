package de.lmu.ifi.dbs.elki.visualization.visualizers.visunproj;

import java.awt.image.RenderedImage;
import java.util.Collection;

import org.apache.batik.util.SVGConstants;
import org.w3c.dom.Element;

import de.lmu.ifi.dbs.elki.data.DatabaseObject;
import de.lmu.ifi.dbs.elki.database.ids.DBID;
import de.lmu.ifi.dbs.elki.database.query.DataQuery;
import de.lmu.ifi.dbs.elki.evaluation.similaritymatrix.ComputeSimilarityMatrixImage;
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
 * Visualize a similarity matrix with object labels
 * 
 * @author Erich Schubert
 * 
 * @apiviz.has ComputeSimilarityMatrixImage.SimilarityMatrix oneway - 1
 *             visualizes
 */
public class SimilarityMatrixVisualizer extends AbstractVisualization<DatabaseObject> {
  /**
   * Name for this visualizer.
   */
  private static final String NAME = "Similarity Matrix Visualizer";

  /**
   * The actual pixmap result.
   */
  private ComputeSimilarityMatrixImage.SimilarityMatrix result;

  /**
   * Constructor.
   * 
   * @param task Visualization task
   */
  public SimilarityMatrixVisualizer(VisualizationTask task) {
    super(task);
    this.result = task.getResult();
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

    RenderedImage img = result.getImage();
    // is ratio, target ratio
    double iratio = img.getHeight() / img.getWidth();
    double tratio = task.getHeight() / task.getWidth();
    // We want to place a (iratio, 1.0) object on a (tratio, 1.0) screen.
    // Both dimensions must fit:
    double zoom = (iratio >= tratio) ? Math.min(tratio / iratio, 1.0) : Math.max(iratio / tratio, 1.0);

    Element itag = svgp.svgElement(SVGConstants.SVG_IMAGE_TAG);
    SVGUtil.setAtt(itag, SVGConstants.SVG_IMAGE_RENDERING_ATTRIBUTE, SVGConstants.SVG_OPTIMIZE_SPEED_VALUE);
    SVGUtil.setAtt(itag, SVGConstants.SVG_X_ATTRIBUTE, margin * 0.75);
    SVGUtil.setAtt(itag, SVGConstants.SVG_Y_ATTRIBUTE, margin * 0.75);
    SVGUtil.setAtt(itag, SVGConstants.SVG_WIDTH_ATTRIBUTE, scale * zoom * iratio);
    SVGUtil.setAtt(itag, SVGConstants.SVG_HEIGHT_ATTRIBUTE, scale * zoom);
    itag.setAttributeNS(SVGConstants.XLINK_NAMESPACE_URI, SVGConstants.XLINK_HREF_QNAME, result.getAsFile().toURI().toString());
    layer.appendChild(itag);

    // Add object labels
    final int size = result.getIDs().size();
    final double hlsize = scale * zoom * iratio / size;
    final double vlsize = scale * zoom / size;
    int i = 0;
    final DataQuery<String> lrep = result.getDatabase().getObjectLabelQuery();
    for(DBID id : result.getIDs()) {
      String label = lrep.get(id);
      if(label != null) {
        // Label on horizontal axis
        final double hlx = margin * 0.75 + hlsize * (i + .8);
        final double hly = margin * 0.7;
        Element lbl = svgp.svgText(hlx, hly, label);
        SVGUtil.setAtt(lbl, SVGConstants.SVG_TRANSFORM_ATTRIBUTE, "rotate(-90," + hlx + "," + hly + ")");
        SVGUtil.setAtt(lbl, SVGConstants.SVG_STYLE_ATTRIBUTE, "font-size: " + hlsize * 0.8);
        layer.appendChild(lbl);
        // Label on vertical axis
        Element lbl2 = svgp.svgText(margin * 0.7, margin * 0.75 + vlsize * (i + .8), label);
        SVGUtil.setAtt(lbl2, SVGConstants.SVG_TEXT_ANCHOR_ATTRIBUTE, SVGConstants.SVG_END_VALUE);
        SVGUtil.setAtt(lbl2, SVGConstants.SVG_STYLE_ATTRIBUTE, "font-size: " + vlsize * 0.8);
        layer.appendChild(lbl2);
      }
      i++;
    }
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
      Collection<ComputeSimilarityMatrixImage.SimilarityMatrix> prs = ResultUtil.filterResults(result, ComputeSimilarityMatrixImage.SimilarityMatrix.class);
      for(ComputeSimilarityMatrixImage.SimilarityMatrix pr : prs) {
        // Add plots, attach visualizer
        final VisualizationTask task = new VisualizationTask(NAME, context, pr, this, null);
        task.put(VisualizationTask.META_LEVEL, VisualizationTask.LEVEL_STATIC);
        context.addVisualizer(pr, task);
      }
    }

    @Override
    public Visualization makeVisualization(VisualizationTask task) {
      return new SimilarityMatrixVisualizer(task);
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