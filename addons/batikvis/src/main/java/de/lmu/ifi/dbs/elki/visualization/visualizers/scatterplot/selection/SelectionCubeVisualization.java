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
package de.lmu.ifi.dbs.elki.visualization.visualizers.scatterplot.selection;

import org.apache.batik.util.SVGConstants;
import org.w3c.dom.Element;

import de.lmu.ifi.dbs.elki.data.HyperBoundingBox;
import de.lmu.ifi.dbs.elki.data.type.TypeUtil;
import de.lmu.ifi.dbs.elki.database.relation.Relation;
import de.lmu.ifi.dbs.elki.result.DBIDSelection;
import de.lmu.ifi.dbs.elki.result.RangeSelection;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.AbstractParameterizer;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.OptionID;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.Parameterization;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.Flag;
import de.lmu.ifi.dbs.elki.visualization.VisualizationTask;
import de.lmu.ifi.dbs.elki.visualization.VisualizationTask.UpdateFlag;
import de.lmu.ifi.dbs.elki.visualization.VisualizationTree;
import de.lmu.ifi.dbs.elki.visualization.VisualizerContext;
import de.lmu.ifi.dbs.elki.visualization.css.CSSClass;
import de.lmu.ifi.dbs.elki.visualization.gui.VisualizationPlot;
import de.lmu.ifi.dbs.elki.visualization.projections.Projection;
import de.lmu.ifi.dbs.elki.visualization.projections.Projection2D;
import de.lmu.ifi.dbs.elki.visualization.projector.ScatterPlotProjector;
import de.lmu.ifi.dbs.elki.visualization.style.StyleLibrary;
import de.lmu.ifi.dbs.elki.visualization.svg.SVGHyperCube;
import de.lmu.ifi.dbs.elki.visualization.svg.SVGPlot;
import de.lmu.ifi.dbs.elki.visualization.svg.SVGUtil;
import de.lmu.ifi.dbs.elki.visualization.visualizers.VisFactory;
import de.lmu.ifi.dbs.elki.visualization.visualizers.Visualization;
import de.lmu.ifi.dbs.elki.visualization.visualizers.scatterplot.AbstractScatterplotVisualization;

/**
 * Visualizer for generating an SVG-Element containing a cube as marker
 * representing the selected range for each dimension
 *
 * @author Heidi Kolb
 * @since 0.4.0
 *
 * @stereotype factory
 * @navassoc - create - Instance
 */
// TODO: Does not use the relation. Always enable, but hide in the menu?
public class SelectionCubeVisualization implements VisFactory {
  /**
   * A short name characterizing this Visualizer.
   */
  private static final String NAME = "Selection Range";

  /**
   * Settings
   */
  protected Parameterizer settings;

  /**
   * Constructor.
   *
   * @param settings Settings
   */
  public SelectionCubeVisualization(Parameterizer settings) {
    super();
    this.settings = settings;
  }

  @Override
  public Visualization makeVisualization(VisualizerContext context, VisualizationTask task, VisualizationPlot plot, double width, double height, Projection proj) {
    return new Instance(context, task, plot, width, height, proj);
  }

  @Override
  public void processNewResult(VisualizerContext context, Object start) {
    VisualizationTree.findVis(context, start).filter(ScatterPlotProjector.class).forEach(p -> {
      Relation<?> rel = p.getRelation();
      if(!TypeUtil.NUMBER_VECTOR_FIELD.isAssignableFromType(rel.getDataTypeInformation())) {
        return;
      }
      final VisualizationTask task = new VisualizationTask(this, NAME, context.getSelectionResult(), rel) //
          .level(VisualizationTask.LEVEL_DATA - 2) //
          .with(UpdateFlag.ON_SELECTION);
      context.addVis(context.getSelectionResult(), task);
      context.addVis(p, task);
    });
  }

  /**
   * Instance.
   *
   * @author Heidi Kolb
   *
   * @navhas - visualizes - RangeSelection
   * @assoc - - - SVGHyperCube
   */
  public class Instance extends AbstractScatterplotVisualization {
    /**
     * Generic tag to indicate the type of element. Used in IDs, CSS-Classes
     * etc.
     */
    public static final String MARKER = "selectionCubeMarker";

    /**
     * CSS class for the filled cube
     */
    public static final String CSS_CUBE = "selectionCube";

    /**
     * CSS class for the cube frame
     */
    public static final String CSS_CUBEFRAME = "selectionCubeFrame";

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
      addListeners();
    }

    /**
     * Adds the required CSS-Classes
     *
     * @param svgp SVG-Plot
     */
    private void addCSSClasses(SVGPlot svgp) {
      final StyleLibrary style = context.getStyleLibrary();
      // Class for the cube
      if(!svgp.getCSSClassManager().contains(CSS_CUBE)) {
        CSSClass cls = new CSSClass(this, CSS_CUBE);
        cls.setStatement(SVGConstants.CSS_STROKE_VALUE, style.getColor(StyleLibrary.SELECTION));
        cls.setStatement(SVGConstants.CSS_STROKE_OPACITY_PROPERTY, style.getOpacity(StyleLibrary.SELECTION));
        cls.setStatement(SVGConstants.CSS_STROKE_WIDTH_PROPERTY, style.getLineWidth(StyleLibrary.PLOT));
        cls.setStatement(SVGConstants.CSS_STROKE_LINECAP_PROPERTY, SVGConstants.CSS_ROUND_VALUE);
        cls.setStatement(SVGConstants.CSS_STROKE_LINEJOIN_PROPERTY, SVGConstants.CSS_ROUND_VALUE);
        if(settings.nofill) {
          cls.setStatement(SVGConstants.CSS_FILL_PROPERTY, SVGConstants.CSS_NONE_VALUE);
        }
        else {
          cls.setStatement(SVGConstants.CSS_FILL_PROPERTY, style.getColor(StyleLibrary.SELECTION));
          cls.setStatement(SVGConstants.CSS_FILL_OPACITY_PROPERTY, style.getOpacity(StyleLibrary.SELECTION));
        }
        svgp.addCSSClassOrLogError(cls);
      }
      // Class for the cube frame
      if(!svgp.getCSSClassManager().contains(CSS_CUBEFRAME)) {
        CSSClass cls = new CSSClass(this, CSS_CUBEFRAME);
        cls.setStatement(SVGConstants.CSS_STROKE_VALUE, style.getColor(StyleLibrary.SELECTION));
        cls.setStatement(SVGConstants.CSS_STROKE_OPACITY_PROPERTY, style.getOpacity(StyleLibrary.SELECTION));
        cls.setStatement(SVGConstants.CSS_STROKE_WIDTH_PROPERTY, style.getLineWidth(StyleLibrary.SELECTION));

        svgp.addCSSClassOrLogError(cls);
      }
    }

    /**
     * Generates a cube and a frame depending on the selection stored in the
     * context
     *
     * @param svgp The plot
     * @param proj The projection
     */
    private void setSVGRect(SVGPlot svgp, Projection2D proj) {
      DBIDSelection selContext = context.getSelection();
      if(selContext instanceof RangeSelection) {
        HyperBoundingBox ranges = ((RangeSelection) selContext).getRanges();
        if(settings.nofill) {
          Element r = SVGHyperCube.drawFrame(svgp, proj, ranges);
          SVGUtil.setCSSClass(r, CSS_CUBEFRAME);
          layer.appendChild(r);
        }
        else {
          Element r = SVGHyperCube.drawFilled(svgp, CSS_CUBE, proj, ranges);
          layer.appendChild(r);
        }

      }
    }

    @Override
    public void fullRedraw() {
      setupCanvas();
      addCSSClasses(svgp);
      DBIDSelection selContext = context.getSelection();
      if(selContext instanceof RangeSelection) {
        setSVGRect(svgp, proj);
      }
    }
  }

  /**
   * Parameterization class.
   *
   * @author Erich Schubert
   */
  public static class Parameterizer extends AbstractParameterizer {
    /**
     * Flag for half-transparent filling of selection cubes.
     */
    public static final OptionID NOFILL_ID = new OptionID("selectionrange.nofill", "Use wireframe style for selection ranges.");

    /**
     * Fill parameter.
     */
    protected boolean nofill;

    @Override
    protected void makeOptions(Parameterization config) {
      super.makeOptions(config);
      Flag nofillF = new Flag(NOFILL_ID);
      if(config.grab(nofillF)) {
        nofill = nofillF.isTrue();
      }
    }

    @Override
    protected SelectionCubeVisualization makeInstance() {
      return new SelectionCubeVisualization(this);
    }
  }
}
