/*
 * This file is part of ELKI:
 * Environment for Developing KDD-Applications Supported by Index-Structures
 *
 * Copyright (C) 2019
 * ELKI Development Team
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package de.lmu.ifi.dbs.elki.visualization.batikutil;

import java.util.concurrent.atomic.AtomicReference;

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
 * @since 0.3
 *
 * @composed - - - JSVGUpdateSynchronizer
 * @navhas - displays - SVGPlot
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
   * The latest attaching operation.
   */
  private final AtomicReference<Runnable> latest = new AtomicReference<>();

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
    synchronized(synchronizer) {
      super.setSVGDocument(null);
      scheduleSetPlot(this.plot, newplot);
    }
  }

  /**
   * Schedule a detach.
   *
   * @param oldplot Plot to detach from.
   */
  private void scheduleSetPlot(final SVGPlot oldplot, final SVGPlot newplot) {
    UpdateManager um = this.getUpdateManager();
    if(um != null) {
      synchronized(um) {
        if(um.isRunning()) {
          // LoggingUtil.warning("Scheduling detach: " + this + " " + oldplot);
          final Runnable detach = new Runnable() {
            @Override
            public void run() {
              if(latest.compareAndSet(this, null)) {
                detachPlot(oldplot);
                attachPlot(newplot);
              }
            }
          };
          latest.set(detach);
          um.getUpdateRunnableQueue().preemptLater(detach);
          return;
        }
      }
    }
    else {
      if(oldplot != null) {
        LoggingUtil.warning("No update manager, but a previous plot exists. Incorrectly initialized?");
      }
    }
    detachPlot(oldplot);
    attachPlot(newplot);
  }

  /**
   * Attach to a new plot, and display.
   *
   * @param newplot Plot to attach to.
   */
  private void attachPlot(SVGPlot newplot) {
    this.plot = newplot;
    if(newplot == null) {
      super.setSVGDocument(null);
      return;
    }
    newplot.synchronizeWith(synchronizer);
    super.setSVGDocument(newplot.getDocument());
    super.setDisableInteractions(newplot.getDisableInteractions());
  }

  /**
   * Execute the detaching event.
   *
   * @param oldplot Plot to detach from.
   */
  private void detachPlot(SVGPlot oldplot) {
    if(oldplot == null) {
      return;
    }
    this.plot = null;
    oldplot.unsynchronizeWith(synchronizer);
  }
}