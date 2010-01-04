package de.lmu.ifi.dbs.elki.visualization.gui.overview;

import java.io.File;

import org.apache.batik.util.SVGConstants;
import org.w3c.dom.Element;

import de.lmu.ifi.dbs.elki.visualization.svg.SVGPlot;
import de.lmu.ifi.dbs.elki.visualization.svg.SVGUtil;
import de.lmu.ifi.dbs.elki.visualization.svg.Thumbnailer;
import de.lmu.ifi.dbs.elki.visualization.visualizers.Visualizer;

/**
 * Class representing a single visualization on the screen.
 * 
 * @author Erich Schubert
 */
abstract class VisualizationInfo {
  /**
   * Thumbnail reference.
   */
  protected File thumbnail = null;
  
  /**
   * Width
   */
  protected double width;
  
  /**
   * Height
   */
  protected double height;
  
  /**
   * Constructor.
   * 
   * @param width Width
   * @param height Height
   */
  public VisualizationInfo(double width, double height) {
    super();
    this.width = width;
    this.height = height;
  }

  /**
   * Build (render) the visualization into an SVG tree.
   * 
   * @param plot SVG plot context (factory)
   * @return SVG subtree
   */
  public abstract Element build(SVGPlot plot);

  /**
   * Access the existing thumbnail, or {@code null}.
   * 
   * @return Thumbnail for this plot.
   */
  File getThumbnailIfGenerated() {
    return thumbnail;
  }

  /**
   * Generate a thumbnail for this visualization.
   * 
   * This will also update the internal thumbnail reference, so the thumbnail can be
   * accessed again via {@link #getThumbnailIfGenerated()}.
   * 
   * @param t Thumbnailer to use
   * @param uwidth Thumbnail width
   * @return File reference of new thumbnail
   */
  File generateThumbnail(Thumbnailer t, int uwidth) {
    SVGPlot plot = new SVGPlot();
    plot.getRoot().setAttribute(SVGConstants.SVG_VIEW_BOX_ATTRIBUTE, "0 0 "+width+" "+height);
    Element e = build(plot);
    plot.getRoot().appendChild(e);
    plot.updateStyleElement();
    int wi = (int)(uwidth * width);
    int he = (int)(uwidth * height);
    synchronized(t) {
      thumbnail = t.thumbnail(plot, wi, he);
    }
    return thumbnail;
  }
  
  protected abstract Visualizer getVisualization();

  /**
   * Test whether a thumbnail is needed for this visualization.
   * 
   * @return Whether or not to generate a thumbnail.
   */
  public boolean thumbnailEnabled() {
    Boolean nothumb = getVisualization().getMetadata().get(Visualizer.META_NOTHUMB, Boolean.class);
    if (nothumb != null && nothumb) {
      return false;
    }
    return true;
  }
  
  /**
   * Test whether a detail view is available.
   * 
   * @return Whether or not a detail view is available.
   */
  public boolean hasDetails() {
    return true;
  }
  
  /**
   * Test whether the visualization is set to be visible.
   * 
   * @return Whether or not to show this visualization.
   */
  public boolean isVisible() {
    Boolean visible = getVisualization().getMetadata().get(Visualizer.META_VISIBLE, Boolean.class);
    if (visible != null) {
      return visible;
    }
    visible = getVisualization().getMetadata().get(Visualizer.META_VISIBLE_DEFAULT, Boolean.class);
    if (visible != null) {
      return visible;
    }
    return true;
  }
  
  /**
   * Make an element for this visualization.
   * 
   * @param plot Plot to insert into
   * @return SVG Element
   */
  public Element makeElement(SVGPlot plot) {
    if (getThumbnailIfGenerated() == null) {
      return null;
    }
    final Element i = plot.svgElement(SVGConstants.SVG_IMAGE_TAG);
    SVGUtil.setAtt(i, SVGConstants.SVG_X_ATTRIBUTE, 0);
    SVGUtil.setAtt(i, SVGConstants.SVG_Y_ATTRIBUTE, 0);
    SVGUtil.setAtt(i, SVGConstants.SVG_WIDTH_ATTRIBUTE, width);
    SVGUtil.setAtt(i, SVGConstants.SVG_HEIGHT_ATTRIBUTE, height);
    i.setAttributeNS(SVGConstants.XLINK_NAMESPACE_URI, SVGConstants.XLINK_HREF_QNAME, getThumbnailIfGenerated().toURI().toString());
    return i;
  }

  /**
   * Get the width
   * 
   * @return the width
   */
  protected double getWidth() {
    return width;
  }

  /**
   * Get the height
   * 
   * @return the height
   */
  protected double getHeight() {
    return height;
  }
}