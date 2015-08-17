package de.lmu.ifi.dbs.elki.visualization.visualizers.scatterplot.uncertain;

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

import org.apache.batik.util.SVGConstants;
import org.w3c.dom.Element;

import de.lmu.ifi.dbs.elki.data.spatial.SpatialUtil;
import de.lmu.ifi.dbs.elki.data.type.TypeUtil;
import de.lmu.ifi.dbs.elki.data.uncertain.UncertainObject;
import de.lmu.ifi.dbs.elki.database.ids.DBIDIter;
import de.lmu.ifi.dbs.elki.database.relation.Relation;
import de.lmu.ifi.dbs.elki.utilities.datastructures.hierarchy.Hierarchy;
import de.lmu.ifi.dbs.elki.utilities.exceptions.ObjectNotFoundException;
import de.lmu.ifi.dbs.elki.visualization.VisualizationTask;
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
import de.lmu.ifi.dbs.elki.visualization.visualizers.AbstractVisFactory;
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
 *
 * @apiviz.stereotype factory
 * @apiviz.uses Instance oneway - - «create»
 */
public class UncertainBoundingBoxVisualization extends AbstractVisFactory {
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
  public Visualization makeVisualization(VisualizationTask task, VisualizationPlot plot, double width, double height, Projection proj) {
    return new Instance(task, plot, width, height, proj);
  }

  @Override
  public void processNewResult(VisualizerContext context, Object start) {
    Hierarchy.Iter<ScatterPlotProjector<?>> it = VisualizationTree.filter(context, start, ScatterPlotProjector.class);
    for(; it.valid(); it.advance()) {
      ScatterPlotProjector<?> p = it.get();
      Relation<?> r = p.getRelation();
      if(TypeUtil.UNCERTAIN_OBJECT_FIELD.isAssignableFromType(r.getDataTypeInformation())) {
        final VisualizationTask task = new VisualizationTask(NAME, context, p, r, this);
        task.level = VisualizationTask.LEVEL_DATA;
        task.addUpdateFlags(VisualizationTask.ON_DATA | VisualizationTask.ON_SAMPLE | VisualizationTask.ON_STYLEPOLICY);
        context.addVis(p, task);
        continue;
      }
      Hierarchy.Iter<Relation<?>> it2 = new VisualizationTree.FilteredIter<Relation<?>>(context.getHierarchy().iterParents(r), Relation.class);
      for(; it2.valid(); it2.advance()) {
        Relation<?> r2 = it2.get();
        if(TypeUtil.UNCERTAIN_OBJECT_FIELD.isAssignableFromType(r2.getDataTypeInformation())) {
          final VisualizationTask task = new VisualizationTask(NAME, context, p, r2, this);
          task.level = VisualizationTask.LEVEL_DATA;
          task.addUpdateFlags(VisualizationTask.ON_DATA | VisualizationTask.ON_SAMPLE | VisualizationTask.ON_STYLEPOLICY);
          context.addVis(p, task);
          continue;
        }
      }
    }
  }

  /**
   * Instance.
   *
   * @author Erich Schubert
   *
   * @apiviz.uses StyleResult
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
     * @param task Visualization task
     * @param plot Plot to draw to
     * @param width Embedding width
     * @param height Embedding height
     * @param proj Projection
     */
    public Instance(VisualizationTask task, VisualizationPlot plot, double width, double height, Projection proj) {
      super(task, plot, width, height, proj);
      addListeners();
      this.rel = task.getRelation();
    }

    @Override
    public void fullRedraw() {
      setupCanvas();
      final double opac = .1; // Opacity
      final StyleLibrary style = context.getStyleLibrary();
      final double lw = .5 * style.getLineWidth(StyleLibrary.PLOT);
      final StylingPolicy spol = context.getStylingPolicy();
      final ColorLibrary colors = style.getColorSet(StyleLibrary.PLOT);

      if(spol instanceof ClassStylingPolicy) {
        ClassStylingPolicy cspol = (ClassStylingPolicy) spol;
        for(int cnum = cspol.getMinStyle(); cnum < cspol.getMaxStyle(); cnum++) {
          String css = CSS_CLASS + "_" + cnum;
          CSSClass cls = new CSSClass(this, css);
          cls.setStatement(SVGConstants.CSS_STROKE_WIDTH_PROPERTY, lw);

          final String color = colors.getColor(cnum);
          cls.setStatement(SVGConstants.CSS_STROKE_PROPERTY, color);
          cls.setStatement(SVGConstants.CSS_FILL_PROPERTY, color);
          cls.setStatement(SVGConstants.CSS_FILL_OPACITY_PROPERTY, opac);

          svgp.addCSSClassOrLogError(cls);

          for(DBIDIter iter = cspol.iterateClass(cnum); iter.valid(); iter.advance()) {
            if(!sample.getSample().contains(iter)) {
              continue; // TODO: can we test more efficiently than this?
            }
            try {
              final UncertainObject vec = rel.get(iter);
              Element r = SVGHyperCube.drawFrame(svgp, proj, SpatialUtil.getMin(vec), SpatialUtil.getMax(vec));
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
            final UncertainObject vec = rel.get(iter);
            Element r = SVGHyperCube.drawFrame(svgp, proj, SpatialUtil.getMin(vec), SpatialUtil.getMax(vec));
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