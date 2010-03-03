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
import de.lmu.ifi.dbs.elki.result.outlier.OutlierScoreMeta;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.OptionID;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.ListParameterization;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.Parameterization;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.DoubleParameter;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.Flag;
import de.lmu.ifi.dbs.elki.utilities.scaling.ClipScaling;
import de.lmu.ifi.dbs.elki.utilities.scaling.GammaScaling;
import de.lmu.ifi.dbs.elki.utilities.scaling.LinearScaling;
import de.lmu.ifi.dbs.elki.utilities.scaling.StaticScalingFunction;
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
public class BubbleVisualizer<NV extends NumberVector<NV, ?>> extends Projection2DVisualizer<NV> {
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
   * OptionID for {@link #CUTOFF_PARAM}.
   */
  public static final OptionID CUTOFF_ID = OptionID.getOrCreateOptionID("bubble.cutoff", "Cut-off on normalized data.");

  /**
   * Parameter for the cutoff on normalized data. <br>
   * Negative values will have the same effect as 0 (no cut-off at all), while
   * values &gt; 1.0 will have the same effect as 1.0 (cut off everything).
   * 
   * <p>
   * Key: {@code -bubble.cutoff}
   * </p>
   * 
   * <p>
   * Default value: 0.0
   * < /p>
   */
  private final DoubleParameter CUTOFF_PARAM = new DoubleParameter(CUTOFF_ID, 0.0);

  /**
   * Cut-off parameter.
   */
  private Double cutOff;

  /**
   * Used for normalizing coordinates.
   */
  private OutlierScoreMeta outlierMeta;

  /**
   * TODO: Find out & document what this scale was for. I can't remember, but it
   * seems essential.
   */
  private StaticScalingFunction plotScale;

  /**
   * Used for Gamma-Correction.
   * 
   * TODO: Make the gamma-function exchangeable (inc. Parameter etc.).
   */
  private GammaScaling gammaScaling;

  /**
   * Used for cut-off on normalized data.
   * 
   * @see #CUTOFF_ID
   * @see #CUTOFF_PARAM
   * @see #cutOff
   */
  private ClipScaling cutOffScale;

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
   * The default constructor only registers parameters.
   */
  public BubbleVisualizer(Parameterization config) {
    super();
    if(config.grab(this, GAMMA_PARAM)) {
      gamma = GAMMA_PARAM.getValue();
    }
    if(config.grab(this, FILL_FLAG)) {
      fill = FILL_FLAG.getValue();
    }
    if(config.grab(this, CUTOFF_PARAM)) {
      cutOff = CUTOFF_PARAM.getValue();
    }
  }

  /**
   * Initializes this Visualizer.
   * 
   * @param name Visualizer name
   * @param context Visualization context
   * @param anResult contains "outlierness-scores", corresponding to the
   *        database.
   * @param normalizationScale normalizes coordinates.
   */
  public void init(String name, VisualizerContext context, AnnotationResult<? extends Number> anResult, OutlierScoreMeta normalizationScale) {
    super.init(name, context);
    this.anResult = anResult;
    this.clustering = context.getOrCreateDefaultClustering();

    this.outlierMeta = normalizationScale;
    this.plotScale = new LinearScaling(0.2);
    this.gammaScaling = new GammaScaling(gamma);
    ListParameterization options = new ListParameterization();
    options.addParameter(ClipScaling.MIN_ID, Double.toString(cutOff));
    this.cutOffScale = new ClipScaling(options);
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
      bubble.setStatement(SVGConstants.CSS_STROKE_WIDTH_PROPERTY, 0.005 * context.getStyleLibrary().getLineWidth(StyleLibrary.PLOT));

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

  private double getValue(int id) {
    return anResult.getValueFor(id).doubleValue();
  }

  /**
   * Convenience method to apply scalings in the right order.
   * 
   * @param d representing a outlierness-score to be scaled.
   * @return a Double representing a outlierness-score, after it has modified by
   *         the given scales.
   */
  private Double getScaled(Double d) {
    if(d == null) {
      return 0.0;
    }
    return plotScale.getScaled(gammaScaling.getScaled(cutOffScale.getScaled(outlierMeta.normalizeScore(d))));
  }

  @Override
  public Element visualize(SVGPlot svgp, VisualizationProjection proj, double width, double height) {
    Element layer = super.setupCanvas(svgp, proj, width, height);
    setupCSS(svgp);
    int clusterID = 0;

    Database<NV> database = context.getDatabase();
    for(Cluster<Model> cluster : clustering.getAllClusters()) {
      for(int id : cluster.getIDs()) {
        final Double radius = getScaled(getValue(id));
        if(radius > 0.01) {
          Vector v = proj.projectDataToRenderSpace(database.get(id));
          Element circle = SVGUtil.svgCircle(svgp.getDocument(), v.get(0), v.get(1), radius);
          SVGUtil.addCSSClass(circle, BUBBLE + clusterID);
          layer.appendChild(circle);
        }
      }
      clusterID += 1;
    }
    return layer;
  }
}
