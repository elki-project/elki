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
  File thumbnail = null;

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
   * @param width Thumbnail width
   * @return File reference of new thumbnail
   */
  File generateThumbnail(Thumbnailer t, int width) {
    double ratio = 1.0;
    SVGPlot plot = new SVGPlot();
    plot.getRoot().setAttribute(SVGConstants.SVG_VIEW_BOX_ATTRIBUTE, "0 0 "+ratio+" 1");
    Element e = build(plot);
    plot.getRoot().appendChild(e);
    plot.updateStyleElement();
    int wi = width;
    int he = (int)(width / ratio);
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
    SVGUtil.setAtt(i, SVGConstants.SVG_WIDTH_ATTRIBUTE, 1);
    SVGUtil.setAtt(i, SVGConstants.SVG_HEIGHT_ATTRIBUTE, 1);
    i.setAttributeNS(SVGConstants.XLINK_NAMESPACE_URI, SVGConstants.XLINK_HREF_QNAME, getThumbnailIfGenerated().toURI().toString());
    return i;
  }

}