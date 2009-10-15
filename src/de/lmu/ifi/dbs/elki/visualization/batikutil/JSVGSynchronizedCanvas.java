package de.lmu.ifi.dbs.elki.visualization.batikutil;

import org.apache.batik.swing.JSVGCanvas;
import org.w3c.dom.Document;
import org.w3c.dom.svg.SVGDocument;

import de.lmu.ifi.dbs.elki.visualization.svg.SVGPlot;

/**
 * An JSVGCanvas that allows easier synchronization of Updates for SVGPlot objects.
 * 
 * @author Erich Schubert
 *
 */
public class JSVGSynchronizedCanvas extends JSVGCanvas {
  /**
   * Serial version number. 
   */
  private static final long serialVersionUID = 1L;
  
  /**
   * Synchronizer to use when synchronizing SVG plots
   */
  final private JSVGUpdateSynchronizer synchronizer;
  
  /**
   * Current SVG plot.
   */
  private SVGPlot plot = null;

  /**
   * Constructor
   */
  public JSVGSynchronizedCanvas() {
    super();
    this.synchronizer = new JSVGUpdateSynchronizer(this);
  }

  /**
   * Use {@link #setPlot} instead if you need synchronization!
   * 
   * @deprecated Document cannot be synchronized - use {@link #setPlot} and a {@link SVGPlot} object!
   */
  @Override
  @Deprecated
  public synchronized void setDocument(Document doc) {
    // Note: this will call this.setSVGDocument!
    super.setDocument(doc);    
  }
  
  /**
   * Use {@link #setPlot} instead if you need synchronization!
   * 
   * @deprecated Document cannot be synchronized - use {@link #setPlot} and a {@link SVGPlot} object!
   */
  @Override
  @Deprecated
  public synchronized void setSVGDocument(SVGDocument doc) {
    // Don't reset if this is our current document.
    if (this.plot.getDocument() != doc) {
      super.setDocument(null);
      this.plot.setUpdateSynchronizer(null);
      this.plot = null;
    }
    super.setSVGDocument(doc);
  }

  /**
   * Choose a new plot to display.
   * 
   * @param plot New plot to display. May be null!
   */
  public synchronized void setPlot(SVGPlot plot) {
    if (this.plot != null) {
      this.plot.setUpdateSynchronizer(null);
    }
    
    this.plot = plot;
    super.setSVGDocument((plot != null) ? plot.getDocument() : null);
    
    if (this.plot != null) {
      this.plot.setUpdateSynchronizer(this.synchronizer);
    }
  }
 
  /**
   * Get the currently displayed SVG plot.
   * 
   * @return current SVG plot. May be {@code null}!
   */
  public SVGPlot getPlot() {
    return this.plot;
  }
}
