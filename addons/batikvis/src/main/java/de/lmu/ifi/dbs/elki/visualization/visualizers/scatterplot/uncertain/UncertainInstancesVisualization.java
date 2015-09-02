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

import de.lmu.ifi.dbs.elki.data.Clustering;
import de.lmu.ifi.dbs.elki.data.NumberVector;
import de.lmu.ifi.dbs.elki.data.type.SimpleTypeInformation;
import de.lmu.ifi.dbs.elki.data.type.TypeUtil;
import de.lmu.ifi.dbs.elki.database.ids.DBIDIter;
import de.lmu.ifi.dbs.elki.database.relation.Relation;
import de.lmu.ifi.dbs.elki.database.relation.RelationUtil;
import de.lmu.ifi.dbs.elki.result.Result;
import de.lmu.ifi.dbs.elki.utilities.datastructures.hierarchy.Hierarchy;
import de.lmu.ifi.dbs.elki.utilities.exceptions.ObjectNotFoundException;
import de.lmu.ifi.dbs.elki.visualization.VisualizationTask;
import de.lmu.ifi.dbs.elki.visualization.VisualizationTree;
import de.lmu.ifi.dbs.elki.visualization.VisualizerContext;
import de.lmu.ifi.dbs.elki.visualization.gui.VisualizationPlot;
import de.lmu.ifi.dbs.elki.visualization.projections.Projection;
import de.lmu.ifi.dbs.elki.visualization.projector.ScatterPlotProjector;
import de.lmu.ifi.dbs.elki.visualization.style.ClusterStylingPolicy;
import de.lmu.ifi.dbs.elki.visualization.style.StyleLibrary;
import de.lmu.ifi.dbs.elki.visualization.style.StylingPolicy;
import de.lmu.ifi.dbs.elki.visualization.style.marker.MarkerLibrary;
import de.lmu.ifi.dbs.elki.visualization.visualizers.AbstractVisFactory;
import de.lmu.ifi.dbs.elki.visualization.visualizers.Visualization;
import de.lmu.ifi.dbs.elki.visualization.visualizers.scatterplot.AbstractScatterplotVisualization;

/**
 * Visualize a single derived sample from an uncertain database.
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
public class UncertainInstancesVisualization extends AbstractVisFactory {
  /**
   * A short name characterizing this Visualizer.
   */
  private static final String NAME = "Uncertain Instance";

  /**
   * Constructor.
   */
  public UncertainInstancesVisualization() {
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
      // Find a scatter plot visualizing uncertain objects:
      ScatterPlotProjector<?> p = it.get();
      Relation<?> r = p.getRelation();
      if(!TypeUtil.UNCERTAIN_OBJECT_FIELD.isAssignableFromType(r.getDataTypeInformation())) {
        continue;
      }
      final VisualizationTask task = new VisualizationTask(NAME, context, p, r, this);
      task.level = VisualizationTask.LEVEL_DATA;
      task.addUpdateFlags(VisualizationTask.ON_DATA | VisualizationTask.ON_SAMPLE | VisualizationTask.ON_STYLEPOLICY);
      context.addVis(p, task);
      continue;
    }
  }

  /**
   * Instance.
   *
   * @author Erich Schubert
   *
   * @apiviz.uses StylingPolicy
   */
  public class Instance extends AbstractScatterplotVisualization {
    /**
     * CSS class for uncertain bounding boxes.
     */
    public static final String CSS_CLASS = "uncertain-instances";

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
      setupCanvas();
      final StyleLibrary style = context.getStyleLibrary();
      final StylingPolicy spol = context.getStylingPolicy();
      final double size = style.getSize(StyleLibrary.MARKERPLOT);
      final MarkerLibrary ml = style.markers();

      // Only visualize cluster-based policies
      if(!(spol instanceof ClusterStylingPolicy)) {
        return;
      }
      ClusterStylingPolicy cspol = (ClusterStylingPolicy) spol;
      Clustering<?> c = cspol.getClustering();
      // If this is a sample from the uncertain database, it must have a parent
      // relation containing vectors, which is a child to the uncertain
      // database.
      Hierarchy.Iter<Result> it = context.getHierarchy().iterAncestors(c);
      Relation<? extends NumberVector> srel = null;
      boolean isChild = false;
      for(; it.valid(); it.advance()) {
        Result r = it.get();
        if(r == this.rel) {
          isChild = true;
        }
        else if(r instanceof Relation) {
          final SimpleTypeInformation<?> type = ((Relation<?>) r).getDataTypeInformation();
          if(TypeUtil.NUMBER_VECTOR_FIELD.isAssignableFromType(type)) {
            @SuppressWarnings("unchecked")
            Relation<? extends NumberVector> vr = (Relation<? extends NumberVector>) r;
            int dim = RelationUtil.dimensionality(vr);
            if(dim == RelationUtil.dimensionality(this.rel)) {
              srel = vr;
            }
          }
        }
        if(isChild && srel != null) {
          break;
        }
      }
      // Nothing found, probably in a different subtree.
      if(!isChild || srel == null) {
        return;
      }
      for(int cnum = cspol.getMinStyle(); cnum < cspol.getMaxStyle(); cnum++) {
        for(DBIDIter iter = cspol.iterateClass(cnum); iter.valid(); iter.advance()) {
          if(!sample.getSample().contains(iter)) {
            continue; // TODO: can we test more efficiently than this?
          }
          try {
            final NumberVector vec = srel.get(iter);
            double[] v = proj.fastProjectDataToRenderSpace(vec);
            if(v[0] != v[0] || v[1] != v[1]) {
              continue; // NaN!
            }
            ml.useMarker(svgp, layer, v[0], v[1], cnum, size);
          }
          catch(ObjectNotFoundException e) {
            // ignore.
          }
        }
      }
    }
  }
}