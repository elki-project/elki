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
package de.lmu.ifi.dbs.elki.visualization.visualizers.scatterplot.uncertain;

import org.apache.batik.util.SVGConstants;
import org.w3c.dom.Element;

import de.lmu.ifi.dbs.elki.data.uncertain.UncertainObject;
import de.lmu.ifi.dbs.elki.database.datastore.ObjectNotFoundException;
import de.lmu.ifi.dbs.elki.database.ids.DBIDIter;
import de.lmu.ifi.dbs.elki.database.relation.Relation;
import de.lmu.ifi.dbs.elki.visualization.VisualizationTask;
import de.lmu.ifi.dbs.elki.visualization.VisualizationTask.UpdateFlag;
import de.lmu.ifi.dbs.elki.visualization.VisualizationTree;
import de.lmu.ifi.dbs.elki.visualization.VisualizerContext;
import de.lmu.ifi.dbs.elki.visualization.colors.ColorLibrary;
import de.lmu.ifi.dbs.elki.visualization.css.CSSClass;
import de.lmu.ifi.dbs.elki.visualization.gui.VisualizationPlot;
import de.lmu.ifi.dbs.elki.visualization.projections.Projection;
import de.lmu.ifi.dbs.elki.visualization.projector.ScatterPlotProjector;
import de.lmu.ifi.dbs.elki.visualization.style.ClassStylingPolicy;
import de.lmu.ifi.dbs.elki.visualization.style.StyleLibrary;
import de.lmu.ifi.dbs.elki.visualization.style.StylingPolicy;
import de.lmu.ifi.dbs.elki.visualization.svg.SVGHyperCube;
import de.lmu.ifi.dbs.elki.visualization.svg.SVGUtil;
import de.lmu.ifi.dbs.elki.visualization.visualizers.VisFactory;
import de.lmu.ifi.dbs.elki.visualization.visualizers.Visualization;
import de.lmu.ifi.dbs.elki.visualization.visualizers.scatterplot.AbstractScatterplotVisualization;

/**
 * Visualize uncertain objects by their bounding box.
 *
 * Note: this is currently a hack. Our projection only applies to vector field
 * relations currently, and this visualizer activates if such a relation (e.g. a
 * sample, or the center of mass) has a parent relation of type UncertainObject.
 * But it serves the purpose.
 *
 * @author Erich Schubert
 * @since 0.7.0
 *
 * @stereotype factory
 * @navassoc - create - Instance
 */
public class UncertainBoundingBoxVisualization implements VisFactory {
  /**
   * A short name characterizing this Visualizer.
   */
  private static final String NAME = "Uncertain Bounding Boxes";

  /**
   * Constructor.
   */
  public UncertainBoundingBoxVisualization() {
    super();
  }

  @Override
  public Visualization makeVisualization(VisualizerContext context, VisualizationTask task, VisualizationPlot plot, double width, double height, Projection proj) {
    return new Instance(context, task, plot, width, height, proj);
  }

  @Override
  public void processNewResult(VisualizerContext context, Object start) {
    VisualizationTree.findVis(context, start).filter(ScatterPlotProjector.class).forEach(p -> {
      Relation<?> r = p.getRelation();
      if(UncertainObject.UNCERTAIN_OBJECT_FIELD.isAssignableFromType(r.getDataTypeInformation())) {
        context.addVis(p, new VisualizationTask(this, NAME, p, r) //
            .level(VisualizationTask.LEVEL_DATA) // .defaultVisibility(false);
            .with(UpdateFlag.ON_DATA).with(UpdateFlag.ON_SAMPLE).with(UpdateFlag.ON_STYLEPOLICY));
      }
    });
  }

  /**
   * Instance.
   *
   * @author Erich Schubert
   *
   * @assoc - - - StylingPolicy
   */
  public class Instance extends AbstractScatterplotVisualization {
    /**
     * CSS class for uncertain bounding boxes.
     */
    public static final String CSS_CLASS = "uncertainbb";

    /**
     * The representation we visualize
     */
    final protected Relation<? extends UncertainObject> rel;

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
      this.rel = task.getRelation();
    }

    @Override
    public void fullRedraw() {
      setupCanvas();
      final double opac = .1; // Opacity
      final StyleLibrary style = context.getStyleLibrary();
      final double lw = .25 * style.getLineWidth(StyleLibrary.PLOT);
      final StylingPolicy spol = context.getStylingPolicy();
      final ColorLibrary colors = style.getColorSet(StyleLibrary.PLOT);

      if(spol instanceof ClassStylingPolicy) {
        ClassStylingPolicy cspol = (ClassStylingPolicy) spol;
        for(int cnum = cspol.getMinStyle(); cnum < cspol.getMaxStyle(); cnum++) {
          String css = CSS_CLASS + "_" + cnum;
          final String color = colors.getColor(cnum);
          CSSClass cls = new CSSClass(this, css);
          cls.setStatement(SVGConstants.CSS_STROKE_WIDTH_PROPERTY, lw);
          cls.setStatement(SVGConstants.CSS_STROKE_LINECAP_PROPERTY, SVGConstants.CSS_ROUND_VALUE);
          cls.setStatement(SVGConstants.CSS_STROKE_PROPERTY, color);
          cls.setStatement(SVGConstants.CSS_FILL_PROPERTY, color);
          cls.setStatement(SVGConstants.CSS_FILL_OPACITY_PROPERTY, opac);

          svgp.addCSSClassOrLogError(cls);

          for(DBIDIter iter = cspol.iterateClass(cnum); iter.valid(); iter.advance()) {
            if(!sample.getSample().contains(iter)) {
              continue; // TODO: can we test more efficiently than this?
            }
            try {
              final UncertainObject mbr = rel.get(iter);
              Element r = SVGHyperCube.drawFrame(svgp, proj, mbr);
              SVGUtil.addCSSClass(r, css);
              layer.appendChild(r);
            }
            catch(ObjectNotFoundException e) {
              // ignore.
            }
          }
        }
      }
      else {
        final String STROKE = SVGConstants.CSS_STROKE_PROPERTY + ":";
        // Color-based styling.
        for(DBIDIter iter = sample.getSample().iter(); iter.valid(); iter.advance()) {
          try {
            final UncertainObject mbr = rel.get(iter);
            Element r = SVGHyperCube.drawFrame(svgp, proj, mbr);
            SVGUtil.addCSSClass(r, CSS_CLASS);
            int col = spol.getColorForDBID(iter);
            SVGUtil.setAtt(r, SVGConstants.SVG_STYLE_ATTRIBUTE, STROKE + SVGUtil.colorToString(col));
            layer.appendChild(r);
          }
          catch(ObjectNotFoundException e) {
            // ignore.
          }
        }
      }
    }
  }
}
