/*
 * This file is part of ELKI:
 * Environment for Developing KDD-Applications Supported by Index-Structures
 *
 * Copyright (C) 2022
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

import org.apache.batik.util.SVGConstants;
import org.w3c.dom.Element;
import elki.database.ids.*;
import elki.result.DBIDSelection;
import elki.visualization.VisualizationTask;
import elki.visualization.VisualizationTask.UpdateFlag;
import elki.visualization.VisualizationTree;
import elki.visualization.VisualizerContext;
import elki.visualization.css.CSSClass;
import elki.visualization.gui.VisualizationPlot;
import elki.visualization.projections.Projection;
import elki.visualization.projector.SilhouettePlotProjector;
import elki.visualization.style.StyleLibrary;
import elki.visualization.svg.SVGUtil;
import elki.visualization.visualizers.VisFactory;
import elki.visualization.visualizers.Visualization;

/**
 * Visualize the selection in a Silhouette Plot.
 *
 * @author Robert Gehde
 * @since 0.8.0
 *
 * @stereotype factory
 * @composed - - - Mode
 * @navassoc - create - Instance
 */
public class SilhouettePlotSelectionVisualization implements VisFactory {
  /**
   * A short name characterizing this Visualizer.
   */
  private static final String NAME = "Silhouette Selection";

  /**
   * Constructor.
   */
  public SilhouettePlotSelectionVisualization() {
    super();
  }

  @Override
  public void processNewResult(VisualizerContext context, Object result) {
    VisualizationTree.findVis(context, result).filter(SilhouettePlotProjector.class).forEach(p -> {
      VisualizationTask task = new VisualizationTask(this, NAME, p.getResult(), null) //
          .level(VisualizationTask.LEVEL_INTERACTIVE).with(UpdateFlag.ON_SELECTION);
      context.addVis(p, task);
      context.addVis(context.getSelectionResult(), task);
    });
  }

  @Override
  public Visualization makeVisualization(VisualizerContext context, VisualizationTask task, VisualizationPlot plot, double width, double height, Projection proj) {
    return new Instance(context, task, plot, width, height, proj);
  }

  /**
   * Instance.
   *
   * @author Robert Gehde
   *
   * @has - visualizes 1 DBIDSelection
   */
  public static class Instance extends AbstractSilhouetteVisualization {
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
      DoubleDBIDList[] values = silhouette.getSilhouetteValues();

      plotSize = (values.length - 1) * 3; // spaces
      for(DoubleDBIDList clusterValues : values) {
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
    }

    /**
     * Add marker for the selected IDs to mtag
     */
    public void addMarker() {
      DoubleDBIDList[] values = silhouette.getSilhouetteValues();
      // TODO: replace mtag!
      DBIDSelection selContext = context.getSelection();
      if(selContext != null) {
        DBIDs selection = DBIDUtil.ensureSet(selContext.getSelectedIds());

        final double width = plotwidth / plotSize;
        int j = 0;
        for(int i = 0; i < values.length; i++) {
          int begin = -1;
          DoubleDBIDList currentList = values[i];
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
    private Element addMarkerRect(double x1, double width) {
      return svgp.svgRect(x1, 0, width, plotheight);
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
