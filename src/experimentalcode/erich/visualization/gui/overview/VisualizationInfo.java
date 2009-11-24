package experimentalcode.erich.visualization.gui.overview;

import java.io.File;

import org.apache.batik.util.SVGConstants;
import org.w3c.dom.Element;

import de.lmu.ifi.dbs.elki.visualization.svg.SVGPlot;
import de.lmu.ifi.dbs.elki.visualization.svg.Thumbnailer;

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
   * @return File reference of new thumbnail
   */
  File makeThumbnail(Thumbnailer t) {
    SVGPlot plot = new SVGPlot();
    plot.getRoot().setAttribute(SVGConstants.SVG_VIEW_BOX_ATTRIBUTE, "0 0 1 1");
    Element e = build(plot);
    plot.getRoot().appendChild(e);
    plot.updateStyleElement();
    synchronized(t) {
      thumbnail = t.thumbnail(plot, 512);
    }
    return thumbnail;
  }
}