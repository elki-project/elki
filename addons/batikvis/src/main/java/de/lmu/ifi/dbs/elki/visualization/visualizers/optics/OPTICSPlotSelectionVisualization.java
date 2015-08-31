package de.lmu.ifi.dbs.elki.visualization.visualizers.optics;

/*
 This file is part of ELKI:
 Environment for Developing KDD-Applications Supported by Index-Structures

 Copyright (C) 2011
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

import org.apache.batik.dom.events.DOMMouseEvent;
import org.apache.batik.util.SVGConstants;
import org.w3c.dom.Element;
import org.w3c.dom.events.Event;
import org.w3c.dom.svg.SVGPoint;

import de.lmu.ifi.dbs.elki.algorithm.clustering.optics.ClusterOrder;
import de.lmu.ifi.dbs.elki.database.ids.DBIDArrayIter;
import de.lmu.ifi.dbs.elki.database.ids.DBIDIter;
import de.lmu.ifi.dbs.elki.database.ids.DBIDUtil;
import de.lmu.ifi.dbs.elki.database.ids.DBIDs;
import de.lmu.ifi.dbs.elki.database.ids.HashSetModifiableDBIDs;
import de.lmu.ifi.dbs.elki.logging.Logging;
import de.lmu.ifi.dbs.elki.result.DBIDSelection;
import de.lmu.ifi.dbs.elki.utilities.datastructures.hierarchy.Hierarchy;
import de.lmu.ifi.dbs.elki.visualization.VisualizationTask;
import de.lmu.ifi.dbs.elki.visualization.VisualizationTree;
import de.lmu.ifi.dbs.elki.visualization.VisualizerContext;
import de.lmu.ifi.dbs.elki.visualization.batikutil.DragableArea;
import de.lmu.ifi.dbs.elki.visualization.css.CSSClass;
import de.lmu.ifi.dbs.elki.visualization.gui.VisualizationPlot;
import de.lmu.ifi.dbs.elki.visualization.projections.Projection;
import de.lmu.ifi.dbs.elki.visualization.projector.OPTICSProjector;
import de.lmu.ifi.dbs.elki.visualization.svg.SVGUtil;
import de.lmu.ifi.dbs.elki.visualization.visualizers.AbstractVisFactory;
import de.lmu.ifi.dbs.elki.visualization.visualizers.Visualization;

/**
 * Handle the marker in an OPTICS plot.
 *
 * @author Erich Schubert
 *
 * @apiviz.stereotype factory
 * @apiviz.uses Instance oneway - - «create»
 */
public class OPTICSPlotSelectionVisualization extends AbstractVisFactory {
  /**
   * The logger for this class.
   */
  private static final Logging LOG = Logging.getLogger(OPTICSPlotSelectionVisualization.class);

  /**
   * A short name characterizing this Visualizer.
   */
  private static final String NAME = "OPTICS Selection";

  /**
   * Input modes
   *
   * @apiviz.exclude
   */
  // TODO: Refactor all Mode copies into a shared class?
  private enum Mode {
    REPLACE, ADD, INVERT
  }

  /**
   * Constructor.
   */
  public OPTICSPlotSelectionVisualization() {
    super();
  }

  @Override
  public void processNewResult(VisualizerContext context, Object result) {
    Hierarchy.Iter<OPTICSProjector> it = VisualizationTree.filter(context, result, OPTICSProjector.class);
    for(; it.valid(); it.advance()) {
      OPTICSProjector p = it.get();
      final VisualizationTask task = new VisualizationTask(NAME, context, p.getResult(), null, this);
      task.level = VisualizationTask.LEVEL_INTERACTIVE;
      task.addUpdateFlags(VisualizationTask.ON_SELECTION);
      context.addVis(p, task);
    }
  }

  @Override
  public Visualization makeVisualization(VisualizationTask task, VisualizationPlot plot, double width, double height, Projection proj) {
    return new Instance(task, plot, width, height, proj);
  }

  @Override
  public boolean allowThumbnails(VisualizationTask task) {
    // Don't use thumbnails
    return false;
  }

  /**
   * Instance.
   *
   * @author Heidi Kolb
   * @author Erich Schubert
   *
   * @apiviz.uses DBIDSelection oneway - 1 visualizes
   */
  public class Instance extends AbstractOPTICSVisualization implements DragableArea.DragListener {
    /**
     * CSS class for markers
     */
    protected static final String CSS_MARKER = "opticsPlotMarker";

    /**
     * CSS class for markers
     */
    protected static final String CSS_RANGEMARKER = "opticsPlotRangeMarker";

    /**
     * Element for the events
     */
    private Element etag;

    /**
     * Element for the marker
     */
    private Element mtag;

    /**
     * Constructor.
     *
     * @param task Visualization task
     * @param plot Plot to draw to
     * @param width Embedding width
     * @param height Embedding height
     * @param proj Projection
     */
    public Instance(VisualizationTask task, VisualizationPlot plot, double width, double height, Projection proj) {
      super(task, plot, width, height, proj);
      addListeners();
    }

    @Override
    public void fullRedraw() {
      makeLayerElement();
      addCSSClasses();

      mtag = svgp.svgElement(SVGConstants.SVG_G_TAG);
      addMarker();

      DragableArea drag = new DragableArea(svgp, 0 - plotwidth * 0.1, 0, plotwidth * 1.1, plotheight, this);
      etag = drag.getElement();
      // mtag first, etag must be the top Element
      layer.appendChild(mtag);
      layer.appendChild(etag);
    }

    /**
     * Add marker for the selected IDs to mtag
     */
    public void addMarker() {
      ClusterOrder order = getClusterOrder();
      // TODO: replace mtag!
      DBIDSelection selContext = context.getSelection();
      if(selContext != null) {
        DBIDs selection = DBIDUtil.ensureSet(selContext.getSelectedIds());

        final double width = plotwidth / order.size();
        int begin = -1, j = 0;
        for(DBIDIter it = order.iter(); it.valid(); it.advance(), j++) {
          if(selection.contains(it)) {
            if(begin == -1) {
              begin = j;
            }
          }
          else {
            if(begin != -1) {
              Element marker = addMarkerRect(begin * width, (j - begin) * width);
              SVGUtil.addCSSClass(marker, CSS_MARKER);
              mtag.appendChild(marker);
              begin = -1;
            }
          }
        }
        // tail
        if(begin != -1) {
          Element marker = addMarkerRect(begin * width, (order.size() - begin) * width);
          SVGUtil.addCSSClass(marker, CSS_MARKER);
          mtag.appendChild(marker);
        }
      }
    }

    /**
     * Create a rectangle as marker (Marker higher than plot!)
     *
     * @param x1 X-Value for the marker
     * @param width Width of an entry
     * @return SVG-Element svg-rectangle
     */
    public Element addMarkerRect(double x1, double width) {
      return svgp.svgRect(x1, 0, width, plotheight);
    }

    @Override
    public boolean startDrag(SVGPoint startPoint, Event evt) {
      ClusterOrder order = getClusterOrder();
      int mouseActIndex = getSelectedIndex(order, startPoint);
      if(mouseActIndex >= 0 && mouseActIndex < order.size()) {
        double width = plotwidth / order.size();
        double x1 = mouseActIndex * width;
        Element marker = addMarkerRect(x1, width);
        SVGUtil.setCSSClass(marker, CSS_RANGEMARKER);
        mtag.appendChild(marker);
        return true;
      }
      return false;
    }

    @Override
    public boolean duringDrag(SVGPoint startPoint, SVGPoint dragPoint, Event evt, boolean inside) {
      ClusterOrder order = getClusterOrder();
      int mouseDownIndex = getSelectedIndex(order, startPoint);
      int mouseActIndex = getSelectedIndex(order, dragPoint);
      final int begin = Math.max(Math.min(mouseDownIndex, mouseActIndex), 0);
      final int end = Math.min(Math.max(mouseDownIndex, mouseActIndex), order.size());
      double width = plotwidth / order.size();
      double x1 = begin * width;
      double x2 = (end * width) + width;
      mtag.removeChild(mtag.getLastChild());
      Element marker = addMarkerRect(x1, x2 - x1);
      SVGUtil.setCSSClass(marker, CSS_RANGEMARKER);
      mtag.appendChild(marker);
      return true;
    }

    @Override
    public boolean endDrag(SVGPoint startPoint, SVGPoint dragPoint, Event evt, boolean inside) {
      ClusterOrder order = getClusterOrder();
      int mouseDownIndex = getSelectedIndex(order, startPoint);
      int mouseActIndex = getSelectedIndex(order, dragPoint);
      Mode mode = getInputMode(evt);
      final int begin = Math.max(Math.min(mouseDownIndex, mouseActIndex), 0);
      final int end = Math.min(Math.max(mouseDownIndex, mouseActIndex), order.size());
      updateSelection(mode, begin, end);
      return true;
    }

    /**
     * Get the current input mode, on each mouse event.
     *
     * @param evt Mouse event.
     * @return Input mode
     */
    private Mode getInputMode(Event evt) {
      if(evt instanceof DOMMouseEvent) {
        DOMMouseEvent domme = (DOMMouseEvent) evt;
        // TODO: visual indication of mode possible?
        if(domme.getShiftKey()) {
          return Mode.ADD;
        }
        else if(domme.getCtrlKey()) {
          return Mode.INVERT;
        }
        else {
          return Mode.REPLACE;
        }
      }
      // Default mode is replace.
      return Mode.REPLACE;
    }

    /**
     * Gets the Index of the ClusterOrderEntry where the event occurred
     *
     * @param order List of ClusterOrderEntries
     * @param cPt clicked point
     * @return Index of the object
     */
    private int getSelectedIndex(ClusterOrder order, SVGPoint cPt) {
      int mouseActIndex = (int) ((cPt.getX() / plotwidth) * order.size());
      return mouseActIndex;
    }

    /**
     * Updates the selection for the given ClusterOrderEntry.
     *
     * @param mode Input mode
     * @param begin first index to select
     * @param end last index to select
     */
    protected void updateSelection(Mode mode, int begin, int end) {
      ClusterOrder order = getClusterOrder();
      if(begin < 0 || begin > end || end >= order.size()) {
        LOG.warning("Invalid range in updateSelection: " + begin + " .. " + end);
        return;
      }

      DBIDSelection selContext = context.getSelection();
      HashSetModifiableDBIDs selection;
      if(selContext == null || mode == Mode.REPLACE) {
        selection = DBIDUtil.newHashSet();
      }
      else {
        selection = DBIDUtil.newHashSet(selContext.getSelectedIds());
      }

      for(DBIDArrayIter it = order.iter().seek(begin); it.getOffset() <= end; it.advance()) {
        if(mode == Mode.INVERT) {
          if(!selection.contains(it)) {
            selection.add(it);
          }
          else {
            selection.remove(it);
          }
        }
        else {
          // In REPLACE and ADD, add objects.
          // The difference was done before by not re-using the selection.
          // Since we are using a set, we can just add in any case.
          selection.add(it);
        }
      }
      context.setSelection(new DBIDSelection(selection));
    }

    /**
     * Adds the required CSS-Classes
     */
    private void addCSSClasses() {
      // Class for the markers
      if(!svgp.getCSSClassManager().contains(CSS_MARKER)) {
        final CSSClass cls = new CSSClass(this, CSS_MARKER);
        cls.setStatement(SVGConstants.CSS_FILL_PROPERTY, SVGConstants.CSS_BLUE_VALUE);
        cls.setStatement(SVGConstants.CSS_OPACITY_PROPERTY, "0.2");
        svgp.addCSSClassOrLogError(cls);
      }

      // Class for the range marking
      if(!svgp.getCSSClassManager().contains(CSS_RANGEMARKER)) {
        final CSSClass rcls = new CSSClass(this, CSS_RANGEMARKER);
        rcls.setStatement(SVGConstants.CSS_FILL_PROPERTY, SVGConstants.CSS_RED_VALUE);
        rcls.setStatement(SVGConstants.CSS_OPACITY_PROPERTY, "0.2");
        svgp.addCSSClassOrLogError(rcls);
      }
    }
  }
}