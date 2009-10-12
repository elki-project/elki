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
    SVGPlot oldplot = this.plot;
    this.plot = null;
    super.setDocument(doc);
    
    if (oldplot != null) {
      oldplot.setUpdateSynchronizer(null);
    }
  }
  
  /**
   * Use {@link #setPlot} instead if you need synchronization!
   * 
   * @deprecated Document cannot be synchronized - use {@link #setPlot} and a {@link SVGPlot} object!
   */
  @Override
  @Deprecated
  public synchronized void setSVGDocument(SVGDocument doc) {
    SVGPlot oldplot = this.plot;
    this.plot = null;
    super.setSVGDocument(doc);
    
    if (oldplot != null) {
      oldplot.setUpdateSynchronizer(null);
    }
  }

  /**
   * Choose a new plot to display.
   * 
   * @param plot New plot to display.
   */
  public synchronized void setPlot(SVGPlot plot) {
    SVGPlot oldplot = this.plot;
    this.plot = plot;
    super.setDocument(this.plot.getDocument());
    
    if (oldplot != null) {
      oldplot.setUpdateSynchronizer(null);
    }
    plot.setUpdateSynchronizer(this.synchronizer);
  }
}
