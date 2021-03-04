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
package elki.visualization.visualizers.scatterplot;

import org.apache.batik.util.SVGConstants;
import org.w3c.dom.Element;

import elki.data.Clustering;
import elki.data.NumberVector;
import elki.data.model.Model;
import elki.data.type.TypeUtil;
import elki.database.datastore.ObjectNotFoundException;
import elki.database.ids.DBIDIter;
import elki.database.relation.MaterializedRelation;
import elki.database.relation.Relation;
import elki.result.Metadata;
import elki.utilities.datastructures.iterator.It;
import elki.utilities.optionhandling.OptionID;
import elki.utilities.optionhandling.Parameterizer;
import elki.utilities.optionhandling.parameterization.Parameterization;
import elki.utilities.optionhandling.parameters.Flag;
import elki.visualization.VisualizationTask;
import elki.visualization.VisualizationTask.UpdateFlag;
import elki.visualization.VisualizationTree;
import elki.visualization.VisualizerContext;
import elki.visualization.gui.VisualizationPlot;
import elki.visualization.projections.Projection;
import elki.visualization.projector.ScatterPlotProjector;
import elki.visualization.style.ClassStylingPolicy;
import elki.visualization.style.ClusterStylingPolicy;
import elki.visualization.style.StyleLibrary;
import elki.visualization.style.StylingPolicy;
import elki.visualization.style.marker.MarkerLibrary;
import elki.visualization.svg.SVGUtil;
import elki.visualization.visualizers.VisFactory;
import elki.visualization.visualizers.Visualization;

/**
 * Visualize a clustering using different markers for different clusters.
 * This visualizer is not constrained to clusters. It can in fact visualize any
 * kind of result we have a style source for.
 *
 * @author Erich Schubert
 * @since 0.5.0
 *
 * @stereotype factory
 * @navassoc - create - Instance
 */
public class MarkerVisualization implements VisFactory {
  /**
   * A short name characterizing this Visualizer.
   */
  private static final String NAME = "Markers";
  /**
   * Use soft markers if possible.
   */
  private boolean softMarkers;
  /**
   * Constructor.
   */
  public MarkerVisualization(boolean soft) {
    super();
    this.softMarkers = soft;
  }

  @Override
  public Visualization makeVisualization(VisualizerContext context, VisualizationTask task, VisualizationPlot plot, double width, double height, Projection proj) {
    return new Instance(context, task, plot, width, height, proj);
  }

  @Override
  public void processNewResult(VisualizerContext context, Object start) {
    VisualizationTree.findVis(context, start).filter(ScatterPlotProjector.class).forEach(p -> {
      final Relation<?> rel = p.getRelation();
      if(!TypeUtil.NUMBER_VECTOR_FIELD.isAssignableFromType(rel.getDataTypeInformation())) {
        return;
      }
      context.addVis(p, new VisualizationTask(this, NAME, p, rel) //
          .level(VisualizationTask.LEVEL_DATA) //
          .with(UpdateFlag.ON_DATA).with(UpdateFlag.ON_SAMPLE).with(UpdateFlag.ON_STYLEPOLICY));
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
     * Generic tag to indicate the type of element. Used in IDs, CSS-Classes
     * etc.
     */
    public static final String DOTMARKER = "dot";

    private MaterializedRelation<double[]> softAssignments;

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
      final StylingPolicy spol = context.getStylingPolicy();
      if(softMarkers && (spol instanceof ClusterStylingPolicy)) {
        // this loop gives EM Cluster Probabilites as output, thats the long
        // name of the soft results
        @SuppressWarnings("unchecked")
        Clustering<Model> clustering = (Clustering<Model>) ((ClusterStylingPolicy) spol).getClustering();
        for(It<MaterializedRelation<double[]>> it = Metadata.hierarchyOf(clustering).iterChildren().filter(MaterializedRelation.class); it.valid(); it.advance()) {
          if(it.get().getLongName().equals("EM Cluster Probabilities")) {
            this.softAssignments = it.get();
          }
        }
      }
    }

    @Override
    public void fullRedraw() {
      setupCanvas();
      final StyleLibrary style = context.getStyleLibrary();
      final MarkerLibrary ml = style.markers();
      final double marker_size = style.getSize(StyleLibrary.MARKERPLOT);
      final StylingPolicy spol = context.getStylingPolicy();

      if(spol instanceof ClassStylingPolicy && softMarkers && softAssignments != null) {
        ClassStylingPolicy cspol = (ClassStylingPolicy) spol;
        for(DBIDIter iter = sample.getSample().iter(); iter.valid(); iter.advance()) {
          try {
            final NumberVector vec = rel.get(iter);
            double[] v = proj.fastProjectDataToRenderSpace(vec);
            if(v[0] != v[0] || v[1] != v[1]) {
              continue; // NaN!
            }
            double in  = cspol.getIntensityForDBID(iter);
            ml.useMarker(svgp, layer, v[0], v[1], cspol.getStyleForDBID(iter), marker_size, in);
          }
          catch(ObjectNotFoundException e) {
            // ignore.
          }
        }
      }
      else if(spol instanceof ClassStylingPolicy) {
        ClassStylingPolicy cspol = (ClassStylingPolicy) spol;
        for(DBIDIter iter = sample.getSample().iter(); iter.valid(); iter.advance()) {
          try {
            final NumberVector vec = rel.get(iter);
            double[] v = proj.fastProjectDataToRenderSpace(vec);
            if(v[0] != v[0] || v[1] != v[1]) {
              continue; // NaN!
            }
            ml.useMarker(svgp, layer, v[0], v[1], cspol.getStyleForDBID(iter), marker_size);
          }
          catch(ObjectNotFoundException e) {
            // ignore.
          }
        }
      }
      else {
        final String FILL = SVGConstants.CSS_FILL_PROPERTY + ":";
        // Color-based styling. Fall back to dots
        for(DBIDIter iter = sample.getSample().iter(); iter.valid(); iter.advance()) {
          try {
            double[] v = proj.fastProjectDataToRenderSpace(rel.get(iter));
            Element dot = svgp.svgCircle(v[0], v[1], marker_size);
            SVGUtil.addCSSClass(dot, DOTMARKER);
            int col = spol.getColorForDBID(iter);
            SVGUtil.setAtt(dot, SVGConstants.SVG_STYLE_ATTRIBUTE, FILL + SVGUtil.colorToString(col));
            layer.appendChild(dot);
          }
          catch(ObjectNotFoundException e) {
            // ignore.
          }
        }
      }
    }
  }
  

  /**
   * Parameterization class.
   *
   * @author Robert Gehde
   */
  public static class Par implements Parameterizer {
    /**
     * Option string to draw straight lines for hull.
     */
    public static final OptionID SOFT_ID = new OptionID("marker.soft", "Use soft markers for Visualization.");

    /**
     * Use bend curves
     */
    private boolean soft = false;

    @Override
    public void configure(Parameterization config) {
      new Flag(SOFT_ID).grab(config, x -> soft = x);
    }

    @Override
    public MarkerVisualization make() {
      return new MarkerVisualization(soft);
    }
  }
}
