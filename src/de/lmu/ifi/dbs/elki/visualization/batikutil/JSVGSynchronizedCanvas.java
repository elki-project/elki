package de.lmu.ifi.dbs.elki.visualization.batikutil;

import org.apache.batik.swing.JSVGCanvas;
import org.w3c.dom.Document;
import org.w3c.dom.svg.SVGDocument;

import de.lmu.ifi.dbs.elki.logging.LoggingUtil;
import de.lmu.ifi.dbs.elki.visualization.svg.SVGPlot;

/**
 * An JSVGCanvas that allows easier synchronization of Updates for SVGPlot
 * objects.
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
    super.setDocumentState(JSVGCanvas.ALWAYS_DYNAMIC);
  }

  /**
   * Use {@link #setPlot} instead if you need synchronization!
   * 
   * @deprecated Document cannot be synchronized - use {@link #setPlot} and a
   *             {@link SVGPlot} object!
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
   * @deprecated Document cannot be synchronized - use {@link #setPlot} and a
   *             {@link SVGPlot} object!
   */
  @Override
  @Deprecated
  public synchronized void setSVGDocument(SVGDocument doc) {
    setPlot(null);
    super.setSVGDocument(doc);
  }

  /**
   * Choose a new plot to display.
   * 
   * @param newplot New plot to display. May be {@code null}!
   */
  public synchronized void setPlot(final SVGPlot newplot) {
    final SVGPlot oldplot = this.plot;

    this.plot = newplot;
    if(newplot != null) {
      newplot.synchronizeWith(this.synchronizer);
      super.setSVGDocument(newplot.getDocument());
    }
    else {
      super.setSVGDocument(null);
    }
    // We only know we're detached when the synchronizer has run again.
    if(oldplot != null) {
      oldplot.scheduleUpdate(new Runnable() {
        @Override
        public void run() {
          detachPlot(oldplot);
        }
      });
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

  /**
   * Execute the detaching event.
   * 
   * @param oldplot Plot to detach from.
   */
  protected void detachPlot(SVGPlot oldplot) {
    if(oldplot != plot) {
      oldplot.unsynchronizeWith(JSVGSynchronizedCanvas.this.synchronizer);
    }
    else {
      LoggingUtil.warning("Detaching from a plot I'm already attached to again?!?");
    }
  }
}