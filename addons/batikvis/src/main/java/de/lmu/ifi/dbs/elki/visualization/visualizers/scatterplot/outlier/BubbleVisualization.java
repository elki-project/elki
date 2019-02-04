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
package de.lmu.ifi.dbs.elki.visualization.visualizers.scatterplot.outlier;

import org.apache.batik.util.SVGConstants;
import org.w3c.dom.Element;

import de.lmu.ifi.dbs.elki.data.NumberVector;
import de.lmu.ifi.dbs.elki.data.type.TypeUtil;
import de.lmu.ifi.dbs.elki.database.datastore.DataStoreListener;
import de.lmu.ifi.dbs.elki.database.ids.DBIDIter;
import de.lmu.ifi.dbs.elki.database.ids.DBIDRef;
import de.lmu.ifi.dbs.elki.database.relation.Relation;
import de.lmu.ifi.dbs.elki.result.outlier.OutlierResult;
import de.lmu.ifi.dbs.elki.utilities.documentation.Reference;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.AbstractParameterizer;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.OptionID;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.Parameterization;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.Flag;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.ObjectParameter;
import de.lmu.ifi.dbs.elki.utilities.scaling.ScalingFunction;
import de.lmu.ifi.dbs.elki.utilities.scaling.outlier.OutlierLinearScaling;
import de.lmu.ifi.dbs.elki.utilities.scaling.outlier.OutlierScaling;
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
import de.lmu.ifi.dbs.elki.visualization.svg.SVGPlot;
import de.lmu.ifi.dbs.elki.visualization.svg.SVGUtil;
import de.lmu.ifi.dbs.elki.visualization.visualizers.VisFactory;
import de.lmu.ifi.dbs.elki.visualization.visualizers.Visualization;
import de.lmu.ifi.dbs.elki.visualization.visualizers.scatterplot.AbstractScatterplotVisualization;

/**
 * Generates a SVG-Element containing bubbles. A Bubble is a circle visualizing
 * an outlierness-score, with its center at the position of the visualized
 * object and its radius depending on the objects score.
 * <p>
 * Reference:
 * <p>
 * Elke Achtert, Hans-Peter Kriegel, Lisa Reichert, Erich Schubert, Remigius
 * Wojdanowski, Arthur Zimek<br>
 * Visual Evaluation of Outlier Detection Models<br>
 * Proc. 15th Int. Conf. on Database Systems for Advanced Applications (DASFAA
 * 2010)
 *
 * @author Remigius Wojdanowski
 * @author Erich Schubert
 * @since 0.5.0
 *
 * @stereotype factory
 * @navassoc - create - Instance
 */
@Reference(authors = "Elke Achtert, Hans-Peter Kriegel, Lisa Reichert, Erich Schubert, Remigius Wojdanowski, Arthur Zimek", //
    title = "Visual Evaluation of Outlier Detection Models", //
    booktitle = "Proc. 15th Int. Conf. on Database Systems for Advanced Applications (DASFAA 2010)", //
    url = "https://doi.org/10.1007/978-3-642-12098-5_34", //
    bibkey = "DBLP:conf/dasfaa/AchtertKRSWZ10")
public class BubbleVisualization implements VisFactory {
  /**
   * Generic tag to indicate the type of element. Used in IDs, CSS-Classes etc.
   */
  public static final String BUBBLE = "bubble";

  /**
   * A short name characterizing this Visualizer.
   */
  public static final String NAME = "Outlier Bubbles";

  /**
   * Current settings
   */
  protected Parameterizer settings;

  /**
   * Constructor.
   *
   * @param settings Settings
   */
  public BubbleVisualization(Parameterizer settings) {
    super();
    this.settings = settings;
  }

  @Override
  public Visualization makeVisualization(VisualizerContext context, VisualizationTask task, VisualizationPlot plot, double width, double height, Projection proj) {
    if(settings.scaling != null && settings.scaling instanceof OutlierScaling) {
      final OutlierResult outlierResult = task.getResult();
      ((OutlierScaling) settings.scaling).prepare(outlierResult);
    }
    return new Instance(context, task, plot, width, height, proj);
  }

  @Override
  public void processNewResult(VisualizerContext context, Object start) {
    VisualizationTree.findNewSiblings(context, start, OutlierResult.class, ScatterPlotProjector.class, (o, p) -> {
      final Relation<?> rel = p.getRelation();
      if(!TypeUtil.NUMBER_VECTOR_FIELD.isAssignableFromType(rel.getDataTypeInformation())) {
        return;
      }
      boolean vis = true;
      // Quick and dirty hack: hide if parent result is also an outlier
      // result since that probably is already visible and we're redundant.
      if(o.getHierarchy().iterParents(o).filter(OutlierResult.class).valid()) {
        vis = false;
      }
      final VisualizationTask task = new VisualizationTask(this, NAME, o, rel) //
          .level(VisualizationTask.LEVEL_DATA) //
          .visibility(vis) //
          .with(UpdateFlag.ON_DATA).with(UpdateFlag.ON_SAMPLE).with(UpdateFlag.ON_STYLEPOLICY);
      context.addVis(o, task);
      context.addVis(p, task);
    });
  }

  /**
   * Factory for producing bubble visualizations
   *
   * @author Erich Schubert
   *
   * @navhas - visualizes - OutlierResult
   */
  public class Instance extends AbstractScatterplotVisualization implements DataStoreListener {
    /**
     * The outlier result to visualize
     */
    protected OutlierResult result;

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
      this.result = task.getResult();
      addListeners();
    }

    @Override
    public void fullRedraw() {
      setupCanvas();
      StyleLibrary style = context.getStyleLibrary();
      StylingPolicy stylepolicy = context.getStylingPolicy();
      // bubble size
      final double bubble_size = style.getSize(StyleLibrary.BUBBLEPLOT);
      if(stylepolicy instanceof ClassStylingPolicy) {
        ClassStylingPolicy colors = (ClassStylingPolicy) stylepolicy;
        setupCSS(svgp, colors);
        // draw data
        for(DBIDIter objId = sample.getSample().iter(); objId.valid(); objId.advance()) {
          final double radius = getScaledForId(objId);
          if(radius > 0.01 && !Double.isInfinite(radius)) {
            final NumberVector vec = rel.get(objId);
            if(vec != null) {
              double[] v = proj.fastProjectDataToRenderSpace(vec);
              if(v[0] != v[0] || v[1] != v[1]) {
                continue; // NaN!
              }
              Element circle = svgp.svgCircle(v[0], v[1], radius * bubble_size);
              SVGUtil.addCSSClass(circle, BUBBLE + colors.getStyleForDBID(objId));
              layer.appendChild(circle);
            }
          }
        }
      }
      else {
        // draw data
        for(DBIDIter objId = sample.getSample().iter(); objId.valid(); objId.advance()) {
          final double radius = getScaledForId(objId);
          if(radius > 0.01 && !Double.isInfinite(radius)) {
            final NumberVector vec = rel.get(objId);
            if(vec != null) {
              double[] v = proj.fastProjectDataToRenderSpace(vec);
              if(v[0] != v[0] || v[1] != v[1]) {
                continue; // NaN!
              }
              Element circle = svgp.svgCircle(v[0], v[1], radius * bubble_size);
              int color = stylepolicy.getColorForDBID(objId);
              final StringBuilder cssstyle = new StringBuilder();
              if(settings.fill) {
                cssstyle.append(SVGConstants.CSS_FILL_PROPERTY).append(':').append(SVGUtil.colorToString(color));
                cssstyle.append(SVGConstants.CSS_FILL_OPACITY_PROPERTY).append(":0.5");
              }
              else {
                cssstyle.append(SVGConstants.CSS_STROKE_VALUE).append(':').append(SVGUtil.colorToString(color));
                cssstyle.append(SVGConstants.CSS_FILL_PROPERTY).append(':').append(SVGConstants.CSS_NONE_VALUE);
              }
              SVGUtil.setAtt(circle, SVGConstants.SVG_STYLE_ATTRIBUTE, cssstyle.toString());
              layer.appendChild(circle);
            }
          }
        }
      }
    }

    /**
     * Registers the Bubble-CSS-Class at a SVGPlot.
     *
     * @param svgp the SVGPlot to register the Tooltip-CSS-Class.
     * @param policy Clustering to use
     */
    private void setupCSS(SVGPlot svgp, ClassStylingPolicy policy) {
      final StyleLibrary style = context.getStyleLibrary();
      ColorLibrary colors = style.getColorSet(StyleLibrary.PLOT);

      // creating IDs manually because cluster often return a null-ID.
      for(int clusterID = policy.getMinStyle(); clusterID < policy.getMaxStyle(); clusterID++) {
        CSSClass bubble = new CSSClass(svgp, BUBBLE + clusterID);
        bubble.setStatement(SVGConstants.CSS_STROKE_WIDTH_PROPERTY, style.getLineWidth(StyleLibrary.PLOT));

        String color = colors.getColor(clusterID);

        if(settings.fill) {
          bubble.setStatement(SVGConstants.CSS_FILL_PROPERTY, color);
          bubble.setStatement(SVGConstants.CSS_FILL_OPACITY_PROPERTY, 0.5);
        }
        else {
          // for diamond-shaped strokes, see bugs.sun.com, bug ID 6294396
          bubble.setStatement(SVGConstants.CSS_STROKE_VALUE, color);
          bubble.setStatement(SVGConstants.CSS_FILL_PROPERTY, SVGConstants.CSS_NONE_VALUE);
        }

        svgp.addCSSClassOrLogError(bubble);
      }
    }

    /**
     * Convenience method to apply scalings in the right order.
     *
     * @param id object ID to get scaled score for
     * @return a Double representing a outlierness-score, after it has modified
     *         by the given scales.
     */
    protected double getScaledForId(DBIDRef id) {
      double d = result.getScores().doubleValue(id);
      if(Double.isNaN(d) || Double.isInfinite(d)) {
        return 0.0;
      }
      if(settings.scaling == null) {
        return result.getOutlierMeta().normalizeScore(d);
      }
      else {
        return settings.scaling.getScaled(d);
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
     * Flag for half-transparent filling of bubbles.
     */
    public static final OptionID FILL_ID = new OptionID("bubble.fill", "Half-transparent filling of bubbles.");

    /**
     * Parameter for scaling functions
     */
    public static final OptionID SCALING_ID = new OptionID("bubble.scaling", "Additional scaling function for bubbles.");

    /**
     * Fill parameter.
     */
    protected boolean fill;

    /**
     * Scaling function to use for Bubbles
     */
    protected ScalingFunction scaling;

    @Override
    protected void makeOptions(Parameterization config) {
      super.makeOptions(config);
      Flag fillF = new Flag(FILL_ID);
      if(config.grab(fillF)) {
        fill = fillF.isTrue();
      }

      ObjectParameter<ScalingFunction> scalingP = new ObjectParameter<>(SCALING_ID, ScalingFunction.class, OutlierLinearScaling.class);
      if(config.grab(scalingP)) {
        scaling = scalingP.instantiateClass(config);
      }
    }

    @Override
    protected BubbleVisualization makeInstance() {
      return new BubbleVisualization(this);
    }
  }
}
