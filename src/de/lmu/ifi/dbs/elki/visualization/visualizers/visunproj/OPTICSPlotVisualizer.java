package de.lmu.ifi.dbs.elki.visualization.visualizers.visunproj;

import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;

import javax.imageio.ImageIO;

import org.apache.batik.util.SVGConstants;
import org.w3c.dom.Element;

import de.lmu.ifi.dbs.elki.data.Clustering;
import de.lmu.ifi.dbs.elki.data.cluster.Cluster;
import de.lmu.ifi.dbs.elki.distance.NumberDistance;
import de.lmu.ifi.dbs.elki.math.MinMax;
import de.lmu.ifi.dbs.elki.result.ClusterOrderEntry;
import de.lmu.ifi.dbs.elki.result.ClusterOrderResult;
import de.lmu.ifi.dbs.elki.visualization.colors.ColorLibrary;
import de.lmu.ifi.dbs.elki.visualization.css.CSSClassManager.CSSNamingConflict;
import de.lmu.ifi.dbs.elki.visualization.scales.LinearScale;
import de.lmu.ifi.dbs.elki.visualization.style.StyleLibrary;
import de.lmu.ifi.dbs.elki.visualization.svg.SVGPlot;
import de.lmu.ifi.dbs.elki.visualization.svg.SVGSimpleLinearAxis;
import de.lmu.ifi.dbs.elki.visualization.svg.SVGUtil;
import de.lmu.ifi.dbs.elki.visualization.visualizers.AbstractVisualizer;
import de.lmu.ifi.dbs.elki.visualization.visualizers.UnprojectedVisualizer;
import de.lmu.ifi.dbs.elki.visualization.visualizers.VisualizerContext;

/**
 * Visualize an OPTICS result by constructing an OPTICS plot for it.
 * 
 * @author Erich Schubert
 * 
 * @param <D> Distance type
 */
public class OPTICSPlotVisualizer<D extends NumberDistance<D, ?>> extends AbstractVisualizer implements UnprojectedVisualizer {
  /**
   * Name for this visualizer.
   */
  private static final String NAME = "OPTICS Plot";

  /**
   * Curve to visualize
   */
  ClusterOrderResult<D> co = null;

  /**
   * The image we generated.
   */
  private File imgfile;

  /**
   * The height/width ratio of the image.
   */
  private double imgratio;

  /**
   * The scale
   */
  private LinearScale linscale;

  /**
   * Initialization.
   * 
   * @param context context.
   * @param co Cluster order to visualize
   */
  public void init(VisualizerContext context, ClusterOrderResult<D> co) {
    super.init(NAME, context);
    this.co = co;
  }

  private void makePlot() throws IOException {
    ColorLibrary colors = context.getStyleLibrary().getColorSet(StyleLibrary.PLOT);

    List<ClusterOrderEntry<D>> order = co.getClusterOrder();

    // FIXME: always use a label based clustering?
    Clustering<?> refc = context.getOrCreateDefaultClustering();
    HashMap<Integer, Integer> idToCluster = new HashMap<Integer, Integer>(context.getDatabase().size());
    int cnum = 0;
    for(Cluster<?> clus : refc.getAllClusters()) {
      for(Integer id : clus) {
        idToCluster.put(id, cnum);
      }
      cnum++;
    }
    int cols[] = new int[cnum];
    for(int i = 0; i < cnum; i++) {
      Color color = SVGUtil.stringToColor(colors.getColor(i));
      if(color != null) {
        cols[i] = color.getRGB();
      }
      else {
        logger.warning("Could not parse color: " + colors.getColor(i));
        cols[i] = 0x7F7F7F7F;
      }
    }

    int width = order.size();
    int height = Math.min(200, (int) Math.ceil(width / 5));
    imgratio = height / (double) width;
    MinMax<Double> range = new MinMax<Double>();
    // calculate range
    for(ClusterOrderEntry<D> coe : order) {
      double reach = coe.getReachability().doubleValue();
      if(!Double.isInfinite(reach) && !Double.isNaN(reach)) {
        range.put(reach);
      }
    }
    // double min = range.getMin();
    // double scale = (height - 1) / (range.getMax() - range.getMin());

    // Avoid a null pointer exception when we don't have valid range values.
    if(range.getMin() == null) {
      range.put(0.0);
      range.put(1.0);
    }
    linscale = new LinearScale(range.getMin(), range.getMax());

    BufferedImage img = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);

    int x = 0;
    for(ClusterOrderEntry<D> coe : order) {
      double reach = coe.getReachability().doubleValue();
      final int y;
      if(!Double.isInfinite(reach) && !Double.isNaN(reach)) {
        y = (height - 1) - (int) Math.floor(linscale.getScaled(reach) * (height - 1));
      }
      else {
        y = 0;
      }
      // logger.warning("Drawing to "+x+","+y+" (on "+width+"x"+height+")");
      try {
        int col = cols[idToCluster.get(coe.getID())];
        for(int y2 = height - 1; y2 >= y; y2--) {
          img.setRGB(x, y2, col);
        }
        // img.setRGB(x, y, 0xFF000000);
      }
      catch(ArrayIndexOutOfBoundsException e) {
        logger.error("Plotting out of range: " + x + "," + y + " >= " + width + "x" + height);
      }
      x++;
    }

    imgfile = File.createTempFile("elki-optics-", ".png");
    imgfile.deleteOnExit();
    ImageIO.write(img, "PNG", imgfile);
  }

  @Override
  public Element visualize(SVGPlot svgp, double width, double height) {
    // TODO: Use width, height, imgratio, number of OPTICS plots!
    double scale = StyleLibrary.SCALE;
    final double sizex = scale;
    final double sizey = scale * height / width;
    final double margin = context.getStyleLibrary().getSize(StyleLibrary.MARGIN);
    Element layer = SVGUtil.svgElement(svgp.getDocument(), SVGConstants.SVG_G_TAG);
    final String transform = SVGUtil.makeMarginTransform(width, height, sizex, sizey, margin);
    SVGUtil.setAtt(layer, SVGConstants.SVG_TRANSFORM_ATTRIBUTE, transform);

    if(imgfile == null) {
      try {
        makePlot();
      }
      catch(IOException e) {
        logger.exception("Could not generate OPTICS plot.", e);
      }
    }

    Element itag = svgp.svgElement(SVGConstants.SVG_IMAGE_TAG);
    SVGUtil.setAtt(itag, SVGConstants.SVG_IMAGE_RENDERING_ATTRIBUTE, SVGConstants.SVG_OPTIMIZE_SPEED_VALUE);
    SVGUtil.setAtt(itag, SVGConstants.SVG_X_ATTRIBUTE, 0);
    SVGUtil.setAtt(itag, SVGConstants.SVG_Y_ATTRIBUTE, 0);
    SVGUtil.setAtt(itag, SVGConstants.SVG_WIDTH_ATTRIBUTE, scale);
    SVGUtil.setAtt(itag, SVGConstants.SVG_HEIGHT_ATTRIBUTE, scale * imgratio);
    itag.setAttributeNS(SVGConstants.XLINK_NAMESPACE_URI, SVGConstants.XLINK_HREF_QNAME, imgfile.toURI().toString());

    layer.appendChild(itag);

    try {
      SVGSimpleLinearAxis.drawAxis(svgp, layer, linscale, 0, scale * imgratio, 0, 0, true, false, context.getStyleLibrary());
      SVGSimpleLinearAxis.drawAxis(svgp, layer, linscale, scale, scale * imgratio, scale, 0, true, true, context.getStyleLibrary());
    }
    catch(CSSNamingConflict e) {
      logger.exception("CSS naming conflict for axes on OPTICS plot", e);
    }

    return layer;
  }
}