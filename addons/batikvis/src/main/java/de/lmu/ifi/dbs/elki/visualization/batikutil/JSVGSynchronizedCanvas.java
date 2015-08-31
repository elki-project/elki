package de.lmu.ifi.dbs.elki.visualization.batikutil;

/*
 This file is part of ELKI:
 Environment for Developing KDD-Applications Supported by Index-Structures

 Copyright (C) 2015
 Ludwig-Maximilians-Universität München
 Lehr- und Forschungseinheit für Datenbanksysteme
 ELKI Development Team

 This program is free software: you can redistribute it and/or modify
 it under the terms of the GNU Affero General Public License as published by
 the Free Software Foundation, either version 3 of the License, or
 (at your option) any later version.

 This program is distributed in the hope that it will be useful,
 but WITHOUT ANY WARRANTY; without even the implied warranty of
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 GNU Affero General Public License for more details.

 You should have received a copy of the GNU Affero General Public License
 along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

import org.apache.batik.bridge.UpdateManager;
import org.apache.batik.swing.JSVGCanvas;
import org.w3c.dom.Document;

import de.lmu.ifi.dbs.elki.logging.LoggingUtil;
import de.lmu.ifi.dbs.elki.visualization.svg.SVGPlot;

/**
 * An JSVGCanvas that allows easier synchronization of Updates for SVGPlot
 * objects.
 *
 * @author Erich Schubert
 *
 * @apiviz.composedOf JSVGUpdateSynchronizer
 * @apiviz.has SVGPlot oneway - - displays
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
   * Get the currently displayed SVG plot.
   *
   * @return current SVG plot. May be {@code null}!
   */
  public SVGPlot getPlot() {
    return this.plot;
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
   * Choose a new plot to display.
   *
   * @param newplot New plot to display. May be {@code null}!
   */
  public void setPlot(final SVGPlot newplot) {
    final SVGPlot oldplot = this.plot;
    this.plot = newplot;
    if(newplot == null) {
      super.setSVGDocument(null);
      if(oldplot != null) {
        scheduleDetach(oldplot);
      }
      return;
    }
    synchronized(synchronizer) {
      newplot.synchronizeWith(synchronizer);
      super.setSVGDocument(newplot.getDocument());
      super.setDisableInteractions(newplot.getDisableInteractions());
      // We only know we're detached when the synchronizer has run again.
      if(oldplot != null && oldplot != newplot) {
        scheduleDetach(oldplot);
      }
    }
  }

  /**
   * Schedule a detach.
   *
   * @param oldplot Plot to detach from.
   */
  private void scheduleDetach(final SVGPlot oldplot) {
    UpdateManager um = this.getUpdateManager();
    if(um == null) {
      return;
    }
    synchronized(um) {
      if(um.isRunning()) {
        // LoggingUtil.warning("Scheduling detach: " + this + " " + oldplot);
        um.getUpdateRunnableQueue().preemptLater(new Runnable() {
          @Override
          public void run() {
            detachPlot(oldplot);
          }
        });
        return;
      }
    }
    detachPlot(oldplot);
  }

  /**
   * Execute the detaching event.
   *
   * @param oldplot Plot to detach from.
   */
  protected void detachPlot(SVGPlot oldplot) {
    // LoggingUtil.warning("Detaching: " + this + " " + oldplot);
    if(oldplot == plot) {
      LoggingUtil.warning("Detaching from a plot I'm already attached to again?!?", new Throwable());
      return;
    }
    oldplot.unsynchronizeWith(JSVGSynchronizedCanvas.this.synchronizer);
  }
}