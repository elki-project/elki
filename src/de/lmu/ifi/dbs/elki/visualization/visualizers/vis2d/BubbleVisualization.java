package de.lmu.ifi.dbs.elki.visualization.visualizers.vis2d;

import java.util.Iterator;
import java.util.List;

import org.apache.batik.util.SVGConstants;
import org.w3c.dom.Element;

import de.lmu.ifi.dbs.elki.data.Cluster;
import de.lmu.ifi.dbs.elki.data.Clustering;
import de.lmu.ifi.dbs.elki.data.NumberVector;
import de.lmu.ifi.dbs.elki.data.model.Model;
import de.lmu.ifi.dbs.elki.database.datastore.DataStoreEvent;
import de.lmu.ifi.dbs.elki.database.datastore.DataStoreListener;
import de.lmu.ifi.dbs.elki.database.ids.DBID;
import de.lmu.ifi.dbs.elki.database.relation.Relation;
import de.lmu.ifi.dbs.elki.result.HierarchicalResult;
import de.lmu.ifi.dbs.elki.result.Result;
import de.lmu.ifi.dbs.elki.result.ResultUtil;
import de.lmu.ifi.dbs.elki.result.outlier.OutlierResult;
import de.lmu.ifi.dbs.elki.utilities.documentation.Reference;
import de.lmu.ifi.dbs.elki.utilities.iterator.IterableUtil;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.AbstractParameterizer;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.OptionID;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.Parameterization;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.Flag;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.ObjectParameter;
import de.lmu.ifi.dbs.elki.utilities.scaling.ScalingFunction;
import de.lmu.ifi.dbs.elki.utilities.scaling.outlier.OutlierScalingFunction;
import de.lmu.ifi.dbs.elki.visualization.colors.ColorLibrary;
import de.lmu.ifi.dbs.elki.visualization.css.CSSClass;
import de.lmu.ifi.dbs.elki.visualization.projections.Projection;
import de.lmu.ifi.dbs.elki.visualization.projections.Projection2D;
import de.lmu.ifi.dbs.elki.visualization.style.StyleLibrary;
import de.lmu.ifi.dbs.elki.visualization.svg.SVGPlot;
import de.lmu.ifi.dbs.elki.visualization.svg.SVGUtil;
import de.lmu.ifi.dbs.elki.visualization.visualizers.AbstractVisFactory;
import de.lmu.ifi.dbs.elki.visualization.visualizers.Visualization;
import de.lmu.ifi.dbs.elki.visualization.visualizers.VisualizationTask;
import de.lmu.ifi.dbs.elki.visualization.visualizers.VisualizerContext;
import de.lmu.ifi.dbs.elki.visualization.visualizers.VisualizerUtil;

/**
 * Generates a SVG-Element containing bubbles. A Bubble is a circle visualizing
 * an outlierness-score, with its center at the position of the visualized
 * object and its radius depending on the objects score.
 * 
 * @author Remigius Wojdanowski
 * @author Erich Schubert
 * 
 * @apiviz.has OutlierResult oneway - - visualizes
 * 
 * @param <NV> Type of the DatabaseObject being visualized.
 */
@Reference(authors = "E. Achtert, H.-P. Kriegel, L. Reichert, E. Schubert, R. Wojdanowski, A. Zimek", title = "Visual Evaluation of Outlier Detection Models", booktitle = "Proceedings of the 15th International Conference on Database Systems for Advanced Applications (DASFAA), Tsukuba, Japan, 2010", url = "http://dx.doi.org/10.1007%2F978-3-642-12098-5_34")
public class BubbleVisualization<NV extends NumberVector<NV, ?>> extends P2DVisualization<NV> implements DataStoreListener {
  /**
   * Generic tag to indicate the type of element. Used in IDs, CSS-Classes etc.
   */
  public static final String BUBBLE = "bubble";

  /**
   * A short name characterizing this Visualizer.
   */
  public static final String NAME = "Outlier Bubbles";

  /**
   * Fill parameter.
   */
  protected boolean fill;

  /**
   * Scaling function to use for Bubbles
   */
  protected ScalingFunction scaling;

  /**
   * The outlier result to visualize
   */
  protected OutlierResult result;

  /**
   * Constructor.
   * 
   * @param task Visualization task
   * @param scaling Scaling function
   */
  public BubbleVisualization(VisualizationTask task, ScalingFunction scaling) {
    super(task);
    this.result = task.getResult();
    this.scaling = scaling;
    context.addDataStoreListener(this);
    incrementalRedraw();
  }

  @Override
  public void destroy() {
    super.destroy();
    context.removeDataStoreListener(this);
  }

  @Override
  public void redraw() {
    Clustering<Model> clustering = context.getOrCreateDefaultClustering();
    setupCSS(svgp, clustering);
    // bubble size
    double bubble_size = context.getStyleLibrary().getSize(StyleLibrary.BUBBLEPLOT);
    // draw data
    Iterator<Cluster<Model>> ci = clustering.getAllClusters().iterator();
    for(int cnum = 0; cnum < clustering.getAllClusters().size(); cnum++) {
      Cluster<?> clus = ci.next();
      for(DBID objId : clus.getIDs()) {
        final Double radius = getScaledForId(objId);
        if(radius > 0.01) {
          final NV vec = rep.get(objId);
          if(vec != null) {
            double[] v = proj.fastProjectDataToRenderSpace(vec);
            Element circle = svgp.svgCircle(v[0], v[1], radius * bubble_size);
            SVGUtil.addCSSClass(circle, BUBBLE + cnum);
            layer.appendChild(circle);
          }
        }
      }
    }
  }

  @Override
  public void contentChanged(@SuppressWarnings("unused") DataStoreEvent e) {
    synchronizedRedraw();
  }

  /**
   * Registers the Bubble-CSS-Class at a SVGPlot.
   * 
   * @param svgp the SVGPlot to register the Tooltip-CSS-Class.
   * @param clustering Clustering to use
   */
  private void setupCSS(SVGPlot svgp, Clustering<? extends Model> clustering) {
    ColorLibrary colors = context.getStyleLibrary().getColorSet(StyleLibrary.PLOT);

    // creating IDs manually because cluster often return a null-ID.
    int clusterID = 0;

    for(@SuppressWarnings("unused")
    Cluster<?> cluster : clustering.getAllClusters()) {
      CSSClass bubble = new CSSClass(svgp, BUBBLE + clusterID);
      bubble.setStatement(SVGConstants.CSS_STROKE_WIDTH_PROPERTY, context.getStyleLibrary().getLineWidth(StyleLibrary.PLOT));

      String color;

      if(clustering.getAllClusters().size() == 1) {
        color = "black";
      }
      else {
        color = colors.getColor(clusterID);
      }

      if(fill) {
        bubble.setStatement(SVGConstants.CSS_FILL_PROPERTY, color);
        bubble.setStatement(SVGConstants.CSS_FILL_OPACITY_PROPERTY, 0.5);
      }
      else {
        // for diamond-shaped strokes, see bugs.sun.com, bug ID 6294396
        bubble.setStatement(SVGConstants.CSS_STROKE_VALUE, color);
        bubble.setStatement(SVGConstants.CSS_FILL_PROPERTY, SVGConstants.CSS_NONE_VALUE);
      }

      svgp.addCSSClassOrLogError(bubble);
      clusterID += 1;
    }
  }

  /**
   * Convenience method to apply scalings in the right order.
   * 
   * @param id object ID to get scaled score for
   * @return a Double representing a outlierness-score, after it has modified by
   *         the given scales.
   */
  protected Double getScaledForId(DBID id) {
    Double d = result.getScores().getValueFor(id).doubleValue();
    if(d == null) {
      return 0.0;
    }
    if(scaling == null) {
      return result.getOutlierMeta().normalizeScore(d);
    }
    else {
      return scaling.getScaled(d);
    }
  }

  /**
   * Factory for producing bubble visualizations
   * 
   * @author Erich Schubert
   * 
   * @apiviz.stereotype factory
   * @apiviz.uses BubbleVisualization oneway - - «create»
   * 
   * @param <NV> Type of the DatabaseObject being visualized.
   */
  public static class Factory<NV extends NumberVector<NV, ?>> extends AbstractVisFactory {
    /**
     * Flag for half-transparent filling of bubbles.
     * 
     * <p>
     * Key: {@code -bubble.fill}
     * </p>
     */
    public static final OptionID FILL_ID = OptionID.getOrCreateOptionID("bubble.fill", "Half-transparent filling of bubbles.");

    /**
     * Parameter for scaling functions
     * 
     * <p>
     * Key: {@code -bubble.scaling}
     * </p>
     */
    public static final OptionID SCALING_ID = OptionID.getOrCreateOptionID("bubble.scaling", "Additional scaling function for bubbles.");

    /**
     * Fill parameter.
     */
    protected boolean fill;

    /**
     * Scaling function to use for Bubbles
     */
    protected ScalingFunction scaling;

    /**
     * Constructor.
     * 
     * @param fill
     * @param scaling
     */
    public Factory(boolean fill, ScalingFunction scaling) {
      super();
      this.fill = fill;
      this.scaling = scaling;
    }

    @Override
    public Visualization makeVisualization(VisualizationTask task) {
      if(this.scaling != null && this.scaling instanceof OutlierScalingFunction) {
        final OutlierResult outlierResult = task.getResult();
        ((OutlierScalingFunction) this.scaling).prepare(task.getContext().getDatabase().getDBIDs(), outlierResult);
      }
      return new BubbleVisualization<NV>(task, scaling);
    }

    @Override
    public void processNewResult(HierarchicalResult baseResult, Result result) {
      final VisualizerContext context = VisualizerUtil.getContext(baseResult);
      Iterator<Relation<? extends NumberVector<?, ?>>> reps = VisualizerUtil.iterateVectorFieldRepresentations(baseResult);
      for(Relation<? extends NumberVector<?, ?>> rep : IterableUtil.fromIterator(reps)) {
        List<OutlierResult> ors = ResultUtil.filterResults(result, OutlierResult.class);
        for(OutlierResult o : ors) {
          final VisualizationTask task = new VisualizationTask(NAME, o, rep, this, P2DVisualization.class);
          task.put(VisualizationTask.META_LEVEL, VisualizationTask.LEVEL_DATA);
          context.addVisualizer(o, task);
        }
      }
    }

    @Override
    public Class<? extends Projection> getProjectionType() {
      return Projection2D.class;
    }

    /**
     * Parameterization class.
     * 
     * @author Erich Schubert
     * 
     * @apiviz.exclude
     */
    public static class Parameterizer<NV extends NumberVector<NV, ?>> extends AbstractParameterizer {
      /**
       * Fill parameter.
       */
      protected boolean fill = false;

      /**
       * Scaling function to use for Bubbles
       */
      protected ScalingFunction scaling = null;

      @Override
      protected void makeOptions(Parameterization config) {
        super.makeOptions(config);
        Flag fillF = new Flag(FILL_ID);
        if(config.grab(fillF)) {
          fill = fillF.getValue();
        }

        ObjectParameter<ScalingFunction> scalingP = new ObjectParameter<ScalingFunction>(SCALING_ID, OutlierScalingFunction.class, true);
        if(config.grab(scalingP)) {
          scaling = scalingP.instantiateClass(config);
        }
      }

      @Override
      protected Factory<NV> makeInstance() {
        return new Factory<NV>(fill, scaling);
      }
    }
  }
}