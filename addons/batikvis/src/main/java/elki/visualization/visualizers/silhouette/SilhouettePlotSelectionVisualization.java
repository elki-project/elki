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
package elki.visualization.visualizers.silhouette;

import org.apache.batik.dom.events.DOMMouseEvent;
import org.apache.batik.util.SVGConstants;
import org.w3c.dom.Element;
import org.w3c.dom.events.Event;
import org.w3c.dom.svg.SVGPoint;

import elki.database.ids.*;
import elki.logging.Logging;
import elki.result.DBIDSelection;
import elki.visualization.VisualizationTask;
import elki.visualization.VisualizationTask.UpdateFlag;
import elki.visualization.VisualizationTree;
import elki.visualization.VisualizerContext;
import elki.visualization.batikutil.DragableArea;
import elki.visualization.css.CSSClass;
import elki.visualization.gui.VisualizationPlot;
import elki.visualization.projections.Projection;
import elki.visualization.projector.SilhouettePlotProjector;
import elki.visualization.style.StyleLibrary;
import elki.visualization.svg.SVGUtil;
import elki.visualization.visualizers.VisFactory;
import elki.visualization.visualizers.Visualization;

/**
 * Handle the marker in an Silhouette plot.
 *
 * @author Robert Gehde
 *
 * @stereotype factory
 * @composed - - - Mode
 * @navassoc - create - Instance
 */
public class SilhouettePlotSelectionVisualization implements VisFactory {
  /**
   * The logger for this class.
   */
  private static final Logging LOG = Logging.getLogger(SilhouettePlotSelectionVisualization.class);

  /**
   * A short name characterizing this Visualizer.
   */
  private static final String NAME = "Silhouette Selection";

  /**
   * Input modes
   */
  // TODO: Refactor all Mode copies into a shared class?
  private enum Mode {
    REPLACE, ADD, INVERT
  }

  /**
   * Constructor.
   */
  public SilhouettePlotSelectionVisualization() {
    super();
  }

  @Override
  public void processNewResult(VisualizerContext context, Object result) {
    VisualizationTree.findVis(context, result).filter(SilhouettePlotProjector.class).forEach(p -> {
      context.addVis(p, new VisualizationTask(this, NAME, p.getResult(), null) //
          .level(VisualizationTask.LEVEL_INTERACTIVE).with(UpdateFlag.ON_SELECTION));
    });
  }

  @Override
  public Visualization makeVisualization(VisualizerContext context, VisualizationTask task, VisualizationPlot plot, double width, double height, Projection proj) {
    return new Instance(context, task, plot, width, height, proj);
  }

  @Override
  public boolean allowThumbnails(VisualizationTask task) {
    // Don't use thumbnails
    return false;
  }

  /**
   * Instance.
   *
   * @author Robert Gehde
   *
   * @has - visualizes 1 DBIDSelection
   */
  public class Instance extends AbstractSilhouetteVisualization implements DragableArea.DragListener {
    /**
     * CSS class for markers
     */
    protected static final String CSS_MARKER = "silhouettePlotMarker";

    /**
     * CSS class for markers
     */
    protected static final String CSS_RANGEMARKER = "silhouettePlotRangeMarker";

    /**
     * Element for the marker
     */
    private Element mtag;

    /**
     * Nmber of points pllus spaces in the plot
     */
    private int plotSize = -1;

    /**
     * Constructor.
     *
     * @param context Visualizer context
     * @param task Visualization task
     * @param plot Plot to draw to
     * @param width Embedding width
     * @param height Embedding height
     * @param proj Projection
     */
    public Instance(VisualizerContext context, VisualizationTask task, VisualizationPlot plot, double width, double height, Projection proj) {
      super(context, task, plot, width, height, proj);
      // calculate size
      ModifiableDoubleDBIDList[] values = getSilhouetteValues();

      plotSize = (values.length - 1) * 3; // spaces
      for(ModifiableDoubleDBIDList clusterValues : values) {
        plotSize += clusterValues.size();
      }
      addListeners();
    }

    @Override
    public void fullRedraw() {
      makeLayerElement();
      addCSSClasses();

      layer.appendChild(mtag = svgp.svgElement(SVGConstants.SVG_G_TAG));
      addMarker();

      layer.appendChild(new DragableArea(svgp, 0 - plotwidth * 0.1, 0, plotwidth * 1.1, plotheight, this).getElement());
    }

    /**
     * Add marker for the selected IDs to mtag
     */
    public void addMarker() {
      ModifiableDoubleDBIDList[] values = getSilhouetteValues();
      // TODO: replace mtag!
      DBIDSelection selContext = context.getSelection();
      if(selContext != null) {
        DBIDs selection = DBIDUtil.ensureSet(selContext.getSelectedIds());

        final double width = plotwidth / plotSize;
        int j = 0;
        for(int i = 0; i < values.length; i++) {
          int begin = -1;
          ModifiableDoubleDBIDList currentList = values[i];
          for(DBIDIter it = currentList.iter(); it.valid(); it.advance(), j++) {
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
            Element marker = addMarkerRect(begin * width, (j - begin) * width);
            SVGUtil.addCSSClass(marker, CSS_MARKER);
            mtag.appendChild(marker);
          }
          j += 3;
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
      int mouseActIndex = getSelectedIndex(startPoint);
      if(mouseActIndex >= 0 && mouseActIndex < plotSize) {
        double width = plotwidth / plotSize;
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
      int mouseDownIndex = getSelectedIndex(startPoint);
      int mouseActIndex = getSelectedIndex(dragPoint);
      final int begin = Math.max(Math.min(mouseDownIndex, mouseActIndex), 0);
      final int end = Math.min(Math.max(mouseDownIndex, mouseActIndex), plotSize);
      double width = plotwidth / plotSize;
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
      int mouseDownIndex = getSelectedIndex(startPoint);
      int mouseActIndex = getSelectedIndex(dragPoint);
      Mode mode = getInputMode(evt);
      final int begin = Math.max(Math.min(mouseDownIndex, mouseActIndex), 0);
      final int end = Math.min(Math.max(mouseDownIndex, mouseActIndex), plotSize);
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
        // TODO: visual indication ofF mode possible?
        return domme.getShiftKey() ? Mode.ADD : domme.getCtrlKey() ? Mode.INVERT : Mode.REPLACE;
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
    private int getSelectedIndex(SVGPoint cPt) {
      return (int) ((cPt.getX() / plotwidth) * plotSize);
    }

    /**
     * Updates the selection for the given ClusterOrderEntry.
     *
     * @param mode Input mode
     * @param begin first index to select
     * @param end last index to select
     */
    protected void updateSelection(Mode mode, int begin, int end) {
      ModifiableDoubleDBIDList[] values = getSilhouetteValues();
      if(begin < 0 || begin > end || end >= plotSize) {
        LOG.warning("Invalid range in updateSelection: " + begin + " .. " + end);
        return;
      }

      DBIDSelection selContext = context.getSelection();
      HashSetModifiableDBIDs selection = (selContext == null || mode == Mode.REPLACE) ? DBIDUtil.newHashSet() //
          : DBIDUtil.newHashSet(selContext.getSelectedIds());

      int clusbegin = 0;
      for(int i = 0; i < values.length; i++) {
        ModifiableDoubleDBIDList cluster = values[i];
        int tbegin = begin, tend = end;
        if(begin < clusbegin) {
          tbegin = 0;
        }
        else {
          tbegin = begin - clusbegin;
        }
        if(end >= clusbegin + cluster.size()) {
          tend = cluster.size() - 1;
        }
        else {
          tend = end - clusbegin;
        }
        for(DBIDArrayIter it = cluster.iter().seek(tbegin); it.getOffset() <= tend; it.advance()) {
          if(mode == Mode.INVERT) {
            if(!selection.add(it)) {
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
        clusbegin += cluster.size() + 3;
      }
      context.setSelection(new DBIDSelection(selection));
    }

    /**
     * Adds the required CSS-Classes
     */
    private void addCSSClasses() {
      final StyleLibrary style = context.getStyleLibrary();
      // Class for the markers
      if(!svgp.getCSSClassManager().contains(CSS_MARKER)) {
        final CSSClass cls = new CSSClass(this, CSS_MARKER);
        cls.setStatement(SVGConstants.CSS_FILL_PROPERTY, style.getColor(StyleLibrary.SELECTION));
        cls.setStatement(SVGConstants.CSS_OPACITY_PROPERTY, style.getOpacity(StyleLibrary.SELECTION));
        svgp.addCSSClassOrLogError(cls);
      }

      // Class for the range marking
      if(!svgp.getCSSClassManager().contains(CSS_RANGEMARKER)) {
        final CSSClass rcls = new CSSClass(this, CSS_RANGEMARKER);
        rcls.setStatement(SVGConstants.CSS_FILL_PROPERTY, style.getColor(StyleLibrary.SELECTION));
        rcls.setStatement(SVGConstants.CSS_OPACITY_PROPERTY, style.getOpacity(StyleLibrary.SELECTION));
        svgp.addCSSClassOrLogError(rcls);
      }
    }
  }
}
