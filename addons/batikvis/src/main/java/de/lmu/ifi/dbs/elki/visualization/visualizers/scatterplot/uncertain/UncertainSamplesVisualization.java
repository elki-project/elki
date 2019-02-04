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

import java.util.Random;

import org.apache.batik.util.SVGConstants;
import org.w3c.dom.Element;

import de.lmu.ifi.dbs.elki.data.NumberVector;
import de.lmu.ifi.dbs.elki.data.uncertain.DiscreteUncertainObject;
import de.lmu.ifi.dbs.elki.data.uncertain.UncertainObject;
import de.lmu.ifi.dbs.elki.database.datastore.ObjectNotFoundException;
import de.lmu.ifi.dbs.elki.database.ids.DBIDIter;
import de.lmu.ifi.dbs.elki.database.relation.Relation;
import de.lmu.ifi.dbs.elki.utilities.random.RandomFactory;
import de.lmu.ifi.dbs.elki.visualization.VisualizationTask;
import de.lmu.ifi.dbs.elki.visualization.VisualizationTask.UpdateFlag;
import de.lmu.ifi.dbs.elki.visualization.VisualizationTree;
import de.lmu.ifi.dbs.elki.visualization.VisualizerContext;
import de.lmu.ifi.dbs.elki.visualization.gui.VisualizationPlot;
import de.lmu.ifi.dbs.elki.visualization.projections.Projection;
import de.lmu.ifi.dbs.elki.visualization.projector.ScatterPlotProjector;
import de.lmu.ifi.dbs.elki.visualization.style.ClassStylingPolicy;
import de.lmu.ifi.dbs.elki.visualization.style.StyleLibrary;
import de.lmu.ifi.dbs.elki.visualization.style.StylingPolicy;
import de.lmu.ifi.dbs.elki.visualization.style.marker.MarkerLibrary;
import de.lmu.ifi.dbs.elki.visualization.svg.SVGUtil;
import de.lmu.ifi.dbs.elki.visualization.visualizers.VisFactory;
import de.lmu.ifi.dbs.elki.visualization.visualizers.Visualization;
import de.lmu.ifi.dbs.elki.visualization.visualizers.scatterplot.AbstractScatterplotVisualization;

import net.jafama.FastMath;

/**
 * Visualize uncertain objects by multiple samples.
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
public class UncertainSamplesVisualization implements VisFactory {
  /**
   * A short name characterizing this Visualizer.
   */
  private static final String NAME = "Uncertain Samples";

  /**
   * Number of samples to draw for uncertain objects.
   */
  protected int samples = 10;

  /**
   * Constructor.
   */
  public UncertainSamplesVisualization() {
    super();
  }

  @Override
  public Visualization makeVisualization(VisualizerContext context, VisualizationTask task, VisualizationPlot plot, double width, double height, Projection proj) {
    return new Instance(context, task, plot, width, height, proj);
  }

  @Override
  public void processNewResult(VisualizerContext context, Object start) {
    VisualizationTree.findVis(context, start).filter(ScatterPlotProjector.class).forEach(p -> {
      context.getHierarchy().iterAncestorsSelf((Relation<?>) p.getRelation()).filter(Relation.class).forEach(r2 -> {
        if(UncertainObject.UNCERTAIN_OBJECT_FIELD.isAssignableFromType(r2.getDataTypeInformation())) {
          context.addVis(p, new VisualizationTask(this, NAME, p, r2) //
              .level(VisualizationTask.LEVEL_DATA).visibility(false) //
              .with(UpdateFlag.ON_DATA).with(UpdateFlag.ON_SAMPLE).with(UpdateFlag.ON_STYLEPOLICY));
        }
      });
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
    public static final String CSS_CLASS = "uncertain-sample";

    /**
     * The representation we visualize
     */
    final protected Relation<? extends UncertainObject> rel;

    /**
     * Random factory.
     */
    final protected RandomFactory random = RandomFactory.DEFAULT;

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
      final StyleLibrary style = context.getStyleLibrary();
      final StylingPolicy spol = context.getStylingPolicy();
      final double size = style.getSize(StyleLibrary.MARKERPLOT);
      final double ssize = size / FastMath.sqrt(samples);
      final MarkerLibrary ml = style.markers();

      Random rand = random.getSingleThreadedRandom();

      if(spol instanceof ClassStylingPolicy) {
        ClassStylingPolicy cspol = (ClassStylingPolicy) spol;
        for(int cnum = cspol.getMinStyle(); cnum < cspol.getMaxStyle(); cnum++) {
          for(DBIDIter iter = cspol.iterateClass(cnum); iter.valid(); iter.advance()) {
            if(!sample.getSample().contains(iter)) {
              continue; // TODO: can we test more efficiently than this?
            }
            try {
              final UncertainObject uo = rel.get(iter);
              if(uo instanceof DiscreteUncertainObject) {
                drawDiscete((DiscreteUncertainObject) uo, ml, cnum, size);
              }
              else {
                drawContinuous(uo, ml, cnum, ssize, rand);
              }
            }
            catch(ObjectNotFoundException e) {
              // ignore.
            }
          }
        }
      }
      else {
        // Color-based styling.
        for(DBIDIter iter = sample.getSample().iter(); iter.valid(); iter.advance()) {
          try {
            final int col = spol.getColorForDBID(iter);
            final UncertainObject uo = rel.get(iter);
            if(uo instanceof DiscreteUncertainObject) {
              drawDiscreteDefault((DiscreteUncertainObject) uo, col, size);
            }
            else {
              drawContinuousDefault(uo, col, size, rand);
            }
          }
          catch(ObjectNotFoundException e) {
            // ignore.
          }
        }
      }
    }

    /**
     * Visualize a discrete uncertain object
     *
     * @param uo Uncertain object
     * @param ml Marker library
     * @param cnum Cluster number
     * @param size Size
     */
    private void drawDiscete(DiscreteUncertainObject uo, MarkerLibrary ml, int cnum, double size) {
      final int e = uo.getNumberSamples();
      final double ssize = size * FastMath.sqrt(e);
      for(int i = 0; i < e; i++) {
        final NumberVector s = uo.getSample(i);
        if(s == null) {
          continue;
        }
        double[] v = proj.fastProjectDataToRenderSpace(s);
        if(v[0] != v[0] || v[1] != v[1]) {
          continue; // NaN!
        }
        ml.useMarker(svgp, layer, v[0], v[1], cnum, uo.getWeight(i) * ssize);
      }
    }

    /**
     * Visualize random samples
     *
     * @param uo Uncertain object
     * @param ml Marker library
     * @param cnum Cluster number
     * @param size Marker size
     * @param rand Random generator
     */
    private void drawContinuous(UncertainObject uo, MarkerLibrary ml, int cnum, double size, Random rand) {
      for(int i = 0; i < samples; i++) {
        double[] v = proj.fastProjectDataToRenderSpace(uo.drawSample(rand));
        if(v[0] != v[0] || v[1] != v[1]) {
          continue; // NaN!
        }
        ml.useMarker(svgp, layer, v[0], v[1], cnum, size);
      }
    }

    /**
     * String constant.
     */
    private final static String FILL = SVGConstants.CSS_FILL_PROPERTY + ":";

    /**
     * Visualize discrete object
     *
     * @param uo Uncertain object
     * @param col Color
     * @param size Size
     */
    private void drawDiscreteDefault(DiscreteUncertainObject uo, int col, double size) {
      final int e = uo.getNumberSamples();
      final double ssize = size * FastMath.sqrt(e);
      for(int i = 0; i < e; i++) {
        final NumberVector s = uo.getSample(i);
        if(s == null) {
          continue;
        }
        double[] v = proj.fastProjectDataToRenderSpace(s);
        Element dot = svgp.svgCircle(v[0], v[1], ssize * uo.getWeight(i));
        SVGUtil.addCSSClass(dot, CSS_CLASS);
        SVGUtil.setAtt(dot, SVGConstants.SVG_STYLE_ATTRIBUTE, FILL + SVGUtil.colorToString(col));
        layer.appendChild(dot);
      }
    }

    /**
     * Visualize random samples
     *
     * @param uo Uncertain object
     * @param col Color
     * @param size Size
     * @param rand Random generator
     */
    private void drawContinuousDefault(UncertainObject uo, int col, double size, Random rand) {
      for(int i = 0; i < samples; i++) {
        double[] v = proj.fastProjectDataToRenderSpace(uo.drawSample(rand));
        Element dot = svgp.svgCircle(v[0], v[1], size);
        SVGUtil.addCSSClass(dot, CSS_CLASS);
        SVGUtil.setAtt(dot, SVGConstants.SVG_STYLE_ATTRIBUTE, FILL + SVGUtil.colorToString(col));
        layer.appendChild(dot);
      }
    }
  }
}
