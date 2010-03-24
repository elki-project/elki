package de.lmu.ifi.dbs.elki.visualization.visualizers.vis2d;

import org.apache.batik.util.SVGConstants;
import org.w3c.dom.Element;

import de.lmu.ifi.dbs.elki.data.Clustering;
import de.lmu.ifi.dbs.elki.data.NumberVector;
import de.lmu.ifi.dbs.elki.data.cluster.Cluster;
import de.lmu.ifi.dbs.elki.data.model.Model;
import de.lmu.ifi.dbs.elki.database.Database;
import de.lmu.ifi.dbs.elki.logging.LoggingUtil;
import de.lmu.ifi.dbs.elki.math.linearalgebra.Vector;
import de.lmu.ifi.dbs.elki.result.AnnotationResult;
import de.lmu.ifi.dbs.elki.result.outlier.OutlierResult;
import de.lmu.ifi.dbs.elki.result.outlier.OutlierScoreMeta;
import de.lmu.ifi.dbs.elki.utilities.documentation.Reference;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.OptionID;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.Parameterization;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.DoubleParameter;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.Flag;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.ObjectParameter;
import de.lmu.ifi.dbs.elki.utilities.scaling.GammaScaling;
import de.lmu.ifi.dbs.elki.utilities.scaling.ScalingFunction;
import de.lmu.ifi.dbs.elki.utilities.scaling.outlier.OutlierScalingFunction;
import de.lmu.ifi.dbs.elki.visualization.VisualizationProjection;
import de.lmu.ifi.dbs.elki.visualization.colors.ColorLibrary;
import de.lmu.ifi.dbs.elki.visualization.css.CSSClass;
import de.lmu.ifi.dbs.elki.visualization.css.CSSClassManager.CSSNamingConflict;
import de.lmu.ifi.dbs.elki.visualization.style.StyleLibrary;
import de.lmu.ifi.dbs.elki.visualization.svg.SVGPlot;
import de.lmu.ifi.dbs.elki.visualization.svg.SVGUtil;
import de.lmu.ifi.dbs.elki.visualization.visualizers.VisualizerContext;

/**
 * Generates a SVG-Element containing bubbles. A Bubble is a circle visualizing
 * an outlierness-score, with its center at the position of the visualized
 * object and its radius depending on the objects score.
 * 
 * @author Remigius Wojdanowski
 * 
 * @param <NV> Type of the DatabaseObject being visualized.
 */
// TODO: add DOI once available.
@Reference(authors = "E. Achtert, H.-P. Kriegel, L. Reichert, E. Schubert, R. Wojdanowski, A. Zimek", title = "Visual Evaluation of Outlier Detection Models", booktitle = "Proceedings of the 15th International Conference on Database Systems for Advanced Applications (DASFAA), Tsukuba, Japan, 2010")
public class BubbleVisualizer<NV extends NumberVector<NV, ?>> extends Projection2DVisualizer<NV> {
  /**
   * Size of r=1.0 bubbles
   */
  private static final double BUBBLE_SIZE = 0.1 * VisualizationProjection.SCALE;

  /**
   * OptionID for {@link #GAMMA_PARAM}.
   */
  public static final OptionID GAMMA_ID = OptionID.getOrCreateOptionID("bubble.gamma", "A gamma-correction.");

  /**
   * Parameter for the gamma-correction.
   * 
   * <p>
   * Key: {@code -bubble.gamma}
   * </p>
   * 
   * <p>
   * Default value: 1.0
   * < /p>
   */
  private final DoubleParameter GAMMA_PARAM = new DoubleParameter(GAMMA_ID, 1.0);

  /**
   * Gamma parameter.
   */
  private double gamma;

  /**
   * OptionID for {@link #FILL_FLAG}.
   */
  public static final OptionID FILL_ID = OptionID.getOrCreateOptionID("bubble.fill", "Half-transparent filling of bubbles.");

  /**
   * Flag for half-transparent filling of bubbles.
   * 
   * <p>
   * Key: {@code -bubble.fill}
   * </p>
   */
  private final Flag FILL_FLAG = new Flag(FILL_ID);

  /**
   * Fill parameter.
   */
  private boolean fill;

  /**
   * OptionID for {@link #SCALING_PARAM}
   */
  public static final OptionID SCALING_ID = OptionID.getOrCreateOptionID("bubble.scaling", "Additional scaling function for bubbles.");

  /**
   * Parameter for scaling functions
   * 
   * <p>
   * Key: {@code -bubble.scaling}
   * </p>
   */
  private final ObjectParameter<ScalingFunction> SCALING_PARAM = new ObjectParameter<ScalingFunction>(SCALING_ID, OutlierScalingFunction.class, true);

  /**
   * Scaling function to use for Bubbles
   */
  private ScalingFunction scaling;

  /**
   * Used for normalizing coordinates.
   */
  private OutlierScoreMeta outlierMeta;

  /**
   * Used for Gamma-Correction.
   * 
   * TODO: Make the gamma-function exchangeable (inc. Parameter etc.).
   */
  private GammaScaling gammaScaling;

  /**
   * Contains the "outlierness-scores" to be displayed as Tooltips. If this
   * result does not contain <b>all</b> IDs the database contains, behavior is
   * undefined.
   */
  private AnnotationResult<? extends Number> anResult;

  /**
   * A clustering of the database.
   */
  private Clustering<Model> clustering;

  /**
   * Generic tag to indicate the type of element. Used in IDs, CSS-Classes etc.
   */
  public static final String BUBBLE = "bubble";

  /**
   * A short name characterizing this Visualizer.
   */
  public static final String NAME = "Bubbles";

  /**
   * Constructor, adhering to
   * {@link de.lmu.ifi.dbs.elki.utilities.optionhandling.Parameterizable}
   * 
   * @param config Parameterization
   */
  public BubbleVisualizer(Parameterization config) {
    super();
    if(config.grab(FILL_FLAG)) {
      fill = FILL_FLAG.getValue();
    }
    if(config.grab(SCALING_PARAM)) {
      scaling = SCALING_PARAM.instantiateClass(config);
    }
    if(config.grab(GAMMA_PARAM)) {
      gamma = GAMMA_PARAM.getValue();
    }
  }

  /**
   * Initializes this Visualizer.
   * 
   * @param name Visualizer name
   * @param context Visualization context
   * @param result contains "outlierness-scores", corresponding to the database.
   */
  public void init(String name, VisualizerContext context, OutlierResult result) {
    super.init(name, context);
    this.anResult = result.getScores();
    this.clustering = context.getOrCreateDefaultClustering();

    this.outlierMeta = result.getOutlierMeta();
    this.gammaScaling = new GammaScaling(gamma);

    if(this.scaling != null && this.scaling instanceof OutlierScalingFunction) {
      ((OutlierScalingFunction) this.scaling).prepare(context.getDatabase(), context.getResult(), result);
    }
  }

  /**
   * Registers the Bubble-CSS-Class at a SVGPlot. This class depends on the
   * {@link #FILL_FLAG}.
   * 
   * @param svgp the SVGPlot to register the Tooltip-CSS-Class.
   */
  private void setupCSS(SVGPlot svgp) {
    ColorLibrary colors = context.getStyleLibrary().getColorSet(StyleLibrary.PLOT);

    // creating IDs manually because cluster often return a null-ID.
    int clusterID = 0;

    for(@SuppressWarnings("unused")
    Cluster<Model> cluster : clustering.getAllClusters()) {

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

      // TODO: try/catch-structure is equal for almost all Visualizers, maybe
      // put that into a superclass.
      try {
        svgp.getCSSClassManager().addClass(bubble);
      }
      catch(CSSNamingConflict e) {
        LoggingUtil.exception("Equally-named CSSClass with different owner already exists", e);
      }
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
  private Double getScaledForId(int id) {
    Double d = anResult.getValueFor(id).doubleValue();
    if(d == null) {
      return 0.0;
    }
    if(scaling == null) {
      double ret = gammaScaling.getScaled(outlierMeta.normalizeScore(d));
      return ret;
    }
    else {
      double ret = gammaScaling.getScaled(scaling.getScaled(d));
      return ret;
    }
  }

  @Override
  public Element visualize(SVGPlot svgp, VisualizationProjection proj, double width, double height) {
    Element layer = super.setupCanvas(svgp, proj, width, height);
    setupCSS(svgp);
    int clusterID = 0;

    Database<NV> database = context.getDatabase();
    for(Cluster<Model> cluster : clustering.getAllClusters()) {
      for(int id : cluster.getIDs()) {
        final Double radius = getScaledForId(id);
        if(radius > 0.01) {
          Vector v = proj.projectDataToRenderSpace(database.get(id));
          Element circle = SVGUtil.svgCircle(svgp.getDocument(), v.get(0), v.get(1), radius * BUBBLE_SIZE);
          SVGUtil.addCSSClass(circle, BUBBLE + clusterID);
          layer.appendChild(circle);
        }
      }
      clusterID += 1;
    }
    return layer;
  }
}