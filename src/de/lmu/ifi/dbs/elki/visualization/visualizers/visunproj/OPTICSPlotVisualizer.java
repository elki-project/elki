package de.lmu.ifi.dbs.elki.visualization.visualizers.visunproj;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.List;

import javax.imageio.ImageIO;

import org.apache.batik.util.SVGConstants;
import org.w3c.dom.Element;

import de.lmu.ifi.dbs.elki.distance.NumberDistance;
import de.lmu.ifi.dbs.elki.math.MinMax;
import de.lmu.ifi.dbs.elki.result.ClusterOrderEntry;
import de.lmu.ifi.dbs.elki.result.ClusterOrderResult;
import de.lmu.ifi.dbs.elki.visualization.svg.SVGPlot;
import de.lmu.ifi.dbs.elki.visualization.svg.SVGUtil;
import de.lmu.ifi.dbs.elki.visualization.visualizers.AbstractVisualizer;
import de.lmu.ifi.dbs.elki.visualization.visualizers.UnprojectedVisualizer;
import de.lmu.ifi.dbs.elki.visualization.visualizers.VisualizerContext;

public class OPTICSPlotVisualizer<D extends NumberDistance<D,?>> extends AbstractVisualizer implements UnprojectedVisualizer {
  /**
   * Name for this visualizer.
   */
  private static final String NAME = "OPTICSPlotVisualizer";

  /**
   * Curve to visualize
   */
  ClusterOrderResult<D> co = null;
  
  /**
   * Initialization.
   * 
   * @param context context.
   */
  public void init(VisualizerContext context, ClusterOrderResult<D> co) {
    super.init(NAME, context);
    this.co = co;
  }

  @Override
  public Element visualize(SVGPlot svgp) {
    Element layer = svgp.svgElement(SVGConstants.SVG_G_TAG);
    SVGUtil.setAtt(layer, SVGConstants.SVG_TRANSFORM_ATTRIBUTE, "scale(0.9) translate(0.05 0.05)");
    
    List<ClusterOrderEntry<D>> order = co.getClusterOrder();
    
    int width = order.size();
    int height = Math.min(50,(int) Math.ceil(width / 5));
    MinMax<Double> range = new MinMax<Double>();
    // calculate range
    for (ClusterOrderEntry<D> coe : order) {
      double reach = coe.getReachability().getValue().doubleValue();
      if (!Double.isInfinite(reach) && !Double.isNaN(reach)) {
        range.put(reach);
      }
    }
    double min = range.getMin();
    double scale = (height - 1) / (range.getMax() - range.getMin());
    
    BufferedImage img = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);

    int x = 0;
    for (ClusterOrderEntry<D> coe : order) {
      double reach = coe.getReachability().getValue().doubleValue();
      final int y;
      if (!Double.isInfinite(reach) && !Double.isNaN(reach)) {
        y = (height - 1) - (int)Math.floor((reach - min) * scale);
      } else {
        y = 0;
      }
      //logger.warning("Drawing to "+x+","+y+" (on "+width+"x"+height+")");
      try {
        img.setRGB(x, y, 0xFF000000);
      } catch (ArrayIndexOutOfBoundsException e) {
        logger.error("Plotting out of range: "+x+","+y+" >= "+width+"x"+height);
      }
      x++;
    }

    final File imgfile;
    try {
      imgfile = File.createTempFile("elki-optics-", ".png");
    }
    catch(IOException e) {
      logger.exception("Could not create temp file for OPTICS plot.", e);
      return layer;
    }
    try {
      ImageIO.write(img, "PNG", imgfile);
    }
    catch(IOException e) {
      logger.exception("Could not write OPTICS plot.", e);
      return layer;
    }
    Element itag = svgp.svgElement(SVGConstants.SVG_IMAGE_TAG);
    SVGUtil.setAtt(itag, SVGConstants.SVG_X_ATTRIBUTE, 0);
    SVGUtil.setAtt(itag, SVGConstants.SVG_Y_ATTRIBUTE, 0);
    SVGUtil.setAtt(itag, SVGConstants.SVG_WIDTH_ATTRIBUTE, 1);
    SVGUtil.setAtt(itag, SVGConstants.SVG_HEIGHT_ATTRIBUTE, height/(double)width);
    itag.setAttributeNS(SVGConstants.XLINK_NAMESPACE_URI, SVGConstants.XLINK_HREF_QNAME, imgfile.toURI().toString());
    
    layer.appendChild(itag);
    
    return layer;
  }

}
