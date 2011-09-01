package experimentalcode.students.roedler;

import java.util.ArrayList;
import java.util.Iterator;

import org.apache.batik.util.SVGConstants;
import org.w3c.dom.Element;

import de.lmu.ifi.dbs.elki.data.NumberVector;
import de.lmu.ifi.dbs.elki.database.datastore.DataStoreEvent;
import de.lmu.ifi.dbs.elki.database.datastore.DataStoreListener;
import de.lmu.ifi.dbs.elki.database.ids.DBID;
import de.lmu.ifi.dbs.elki.database.ids.DBIDs;
import de.lmu.ifi.dbs.elki.database.query.DistanceResultPair;
import de.lmu.ifi.dbs.elki.distance.distancefunction.ArcCosineDistanceFunction;
import de.lmu.ifi.dbs.elki.distance.distancefunction.CosineDistanceFunction;
import de.lmu.ifi.dbs.elki.distance.distancefunction.DistanceFunction;
import de.lmu.ifi.dbs.elki.distance.distancefunction.EuclideanDistanceFunction;
import de.lmu.ifi.dbs.elki.distance.distancefunction.LPNormDistanceFunction;
import de.lmu.ifi.dbs.elki.distance.distancefunction.ManhattanDistanceFunction;
import de.lmu.ifi.dbs.elki.distance.distancevalue.DoubleDistance;
import de.lmu.ifi.dbs.elki.distance.distancevalue.NumberDistance;
import de.lmu.ifi.dbs.elki.index.preprocessed.knn.AbstractMaterializeKNNPreprocessor;
import de.lmu.ifi.dbs.elki.index.preprocessed.knn.MaterializeKNNPreprocessor;
import de.lmu.ifi.dbs.elki.result.DBIDSelection;
import de.lmu.ifi.dbs.elki.result.HierarchicalResult;
import de.lmu.ifi.dbs.elki.result.Result;
import de.lmu.ifi.dbs.elki.result.ResultUtil;
import de.lmu.ifi.dbs.elki.result.SelectionResult;
import de.lmu.ifi.dbs.elki.utilities.exceptions.ObjectNotFoundException;
import de.lmu.ifi.dbs.elki.utilities.iterator.IterableUtil;
import de.lmu.ifi.dbs.elki.visualization.VisualizationTask;
import de.lmu.ifi.dbs.elki.visualization.css.CSSClass;
import de.lmu.ifi.dbs.elki.visualization.projector.ScatterPlotProjector;
import de.lmu.ifi.dbs.elki.visualization.style.StyleLibrary;
import de.lmu.ifi.dbs.elki.visualization.svg.SVGHyperSphere;
import de.lmu.ifi.dbs.elki.visualization.svg.SVGPlot;
import de.lmu.ifi.dbs.elki.visualization.svg.SVGUtil;
import de.lmu.ifi.dbs.elki.visualization.visualizers.AbstractVisFactory;
import de.lmu.ifi.dbs.elki.visualization.visualizers.Visualization;
import de.lmu.ifi.dbs.elki.visualization.visualizers.events.ContextChangeListener;
import de.lmu.ifi.dbs.elki.visualization.visualizers.thumbs.ThumbnailVisualization;
import de.lmu.ifi.dbs.elki.visualization.visualizers.vis2d.P2DVisualization;
import experimentalcode.students.roedler.utils.DistanceFunctionDrawUtils;

/**
 * Visualizer for generating an SVG-Element containing dots as markers
 * representing the kNN of the selected Database objects.
 * 
 * @author Robert Rödler
 * 
 * @apiviz.has SelectionResult oneway - - visualizes
 * @apiviz.has DBIDSelection oneway - - visualizes
 * 
 * @param <NV> Type of the NumberVector being visualized.
 */
public class DistanceFunctionVisualization<NV extends NumberVector<NV, ?>, D extends NumberDistance<D, ?>> extends P2DVisualization<NV> implements ContextChangeListener, DataStoreListener {
  /**
   * A short name characterizing this Visualizer.
   */
  private static final String NAME = "kNN + Distancefunction Visualization";

  /**
   * Generic tags to indicate the type of element. Used in IDs, CSS-Classes etc.
   */
  public static final String KNNMARKER = "kNNMarker";

  public static final String KNNDIST = "kNNDist";

  public static final String DISTANCEFUNCTION = "distancefunction";

  /**
   * The selection result we work on
   */
  private MaterializeKNNPreprocessor<NV, DoubleDistance> result;

  /**
   * p[0] type of a Norm p[1] value of a Lp Norm
   */
  private double[] p;

  /**
   * Constructor
   * 
   * @param task VisualizationTask
   */
  public DistanceFunctionVisualization(VisualizationTask task) {
    super(task);
    this.result = task.getResult();
    context.addContextChangeListener(this);
    context.addDataStoreListener(this);
    context.addResultListener(this);
    incrementalRedraw();
  }

  @Override
  protected void redraw() {
    addCSSClasses(svgp);
    final double size = context.getStyleLibrary().getSize(StyleLibrary.SELECTION);
    DBIDSelection selContext = context.getSelection();
    p = getLPNormP(result);

    if(selContext != null) {
      DBIDs selection = selContext.getSelectedIds();
      int counter;
      Element dist = null;

      for(DBID i : selection) {
        counter = 1;

        drawDistancefunction: for(DistanceResultPair<DoubleDistance> j : result.get(i)) {
          try {
            double[] v = proj.fastProjectDataToRenderSpace(rep.get(j.getDBID()));
            Element dot = svgp.svgCircle(v[0], v[1], size);
            SVGUtil.addCSSClass(dot, KNNMARKER);
            layer.appendChild(dot);

            Element lbl = svgp.svgText(v[0] + size, v[1] + size, j.getDistance().toString());
            SVGUtil.addCSSClass(lbl, KNNDIST);
            layer.appendChild(lbl);

            if(result.getK() == counter) {
              switch((int) p[0]){
              case 1: {
                dist = SVGHyperSphere.drawManhattan(svgp, proj, rep.get(i), j.getDistance());
                break;
              }
              case 2: {
                dist = SVGHyperSphere.drawEuclidean(svgp, proj, rep.get(i), j.getDistance());
                break;
              }
              case 3: {
                dist = SVGHyperSphere.drawLp(svgp, proj, rep.get(i), j.getDistance(), p[1]);
                break;
              }
              case 4: {
                dist = DistanceFunctionDrawUtils.drawCosine(svgp, proj, rep.get(i), rep.get(j.getDBID()));
                break;
              }
              default: {
                break drawDistancefunction;
              }
              }

              SVGUtil.addCSSClass(dist, DISTANCEFUNCTION);
              layer.appendChild(dist);
            }
            counter++;
          }
          catch(ObjectNotFoundException e) {
            // ignore
          }
        }
      }
    }
  }

  /**
   * Adds the required CSS-Classes
   * 
   * @param svgp SVG-Plot
   */
  private void addCSSClasses(SVGPlot svgp) {
    final StyleLibrary style = context.getStyleLibrary();
    // Class for the distance markers
    if(!svgp.getCSSClassManager().contains(KNNMARKER)) {
      CSSClass cls = new CSSClass(this, KNNMARKER);
      cls.setStatement(SVGConstants.CSS_FILL_PROPERTY, SVGConstants.CSS_DARKGREEN_VALUE);
      cls.setStatement(SVGConstants.CSS_OPACITY_PROPERTY, style.getOpacity(StyleLibrary.SELECTION));
      svgp.addCSSClassOrLogError(cls);
    }
    // Class for the distance function
    if(!svgp.getCSSClassManager().contains(DISTANCEFUNCTION)) {
      CSSClass cls = new CSSClass(this, DISTANCEFUNCTION);
      cls.setStatement(SVGConstants.CSS_STROKE_PROPERTY, SVGConstants.CSS_RED_VALUE);
      cls.setStatement(SVGConstants.CSS_STROKE_WIDTH_PROPERTY, style.getLineWidth(StyleLibrary.PLOT));
      cls.setStatement(SVGConstants.CSS_FILL_PROPERTY, SVGConstants.CSS_NONE_VALUE);
      cls.setStatement(SVGConstants.CSS_STROKE_LINECAP_PROPERTY, SVGConstants.CSS_ROUND_VALUE);
      cls.setStatement(SVGConstants.CSS_STROKE_LINEJOIN_PROPERTY, SVGConstants.CSS_ROUND_VALUE);
      svgp.addCSSClassOrLogError(cls);
    }
    // Class for the distance label
    if(!svgp.getCSSClassManager().contains(KNNDIST)) {
      CSSClass cls = new CSSClass(this, KNNDIST);
      cls.setStatement(SVGConstants.CSS_FILL_PROPERTY, SVGConstants.CSS_BLACK_VALUE);
      cls.setStatement(SVGConstants.CSS_FONT_SIZE_PROPERTY, style.getTextSize(StyleLibrary.PLOT));
      cls.setStatement(SVGConstants.CSS_FONT_FAMILY_PROPERTY, style.getFontFamily(StyleLibrary.PLOT));
      svgp.addCSSClassOrLogError(cls);
    }
  }

  /**
   * Get the "p" value of an Lp norm.
   * 
   * @param kNN MaterializeKNNPreprocessor<?,?>
   * @return p[0] indicates the type of the Distancefunction, p[0] = 1 ->
   *         Manhattan = 2 -> Euclidean = 3 -> Lp Norm = 4 -> ArcCosine and
   *         Cosine =-1 -> visualization not implemented so far p[1] the p value
   *         itself
   */
  public static <NV extends NumberVector<NV, ?>, D extends NumberDistance<D, ?>> double[] getLPNormP(AbstractMaterializeKNNPreprocessor<NV, D> kNN) {
    double[] p = new double[2];
    // Note: we deliberately lose generics here, so the compilers complain less
    // on the next typecheck and cast!
    DistanceFunction<?, ?> distanceFunction = kNN.getDistanceQuery().getDistanceFunction();
    if(ManhattanDistanceFunction.class.isInstance(distanceFunction)) {
      p[0] = 1.0;
    }
    else if(EuclideanDistanceFunction.class.isInstance(distanceFunction)) {
      p[0] = 2.0;
    }
    else if(LPNormDistanceFunction.class.isInstance(distanceFunction)) {
      p[1] = ((LPNormDistanceFunction) distanceFunction).getP();
      p[0] = 3.0;
    }
    else if(CosineDistanceFunction.class.isInstance(distanceFunction) || ArcCosineDistanceFunction.class.isInstance(distanceFunction)) {
      p[0] = 4.0;
    }
    else {
      p[0] = -1.0;
    }
    return p;
  }

  @Override
  public void contentChanged(DataStoreEvent e) {
    synchronizedRedraw();
  }

  @Override
  public void resultChanged(Result current) {
    if(current instanceof SelectionResult) {
      synchronizedRedraw();
      return;
    }
    super.resultChanged(current);
  }

  @Override
  public void destroy() {
    super.destroy();
    context.removeContextChangeListener(this);
    context.removeDataStoreListener(this);
  }

  /**
   * Factory for visualizers to generate an SVG-Element containing dots as
   * markers representing the kNN of the selected Database objects.
   * 
   * @author Robert Rödler
   * 
   * @apiviz.stereotype factory
   * @apiviz.uses DistanceFunctionVisualisation oneway - - «create»
   * 
   * @param <NV> Type of the NumberVector being visualized.
   */
  public static class Factory<NV extends NumberVector<NV, ?>, D extends NumberDistance<D, ?>> extends AbstractVisFactory {
    /**
     * Constructor
     */
    public Factory() {
      super();
    }

    @Override
    public Visualization makeVisualization(VisualizationTask task) {
      return new DistanceFunctionVisualization<NV, DoubleDistance>(task);
    }

    @Override
    public Visualization makeVisualizationOrThumbnail(VisualizationTask task) {
      return new ThumbnailVisualization(this, task, ThumbnailVisualization.ON_DATA | ThumbnailVisualization.ON_SELECTION);
    }

    @Override
    public void processNewResult(HierarchicalResult baseResult, Result result) {
      final ArrayList<MaterializeKNNPreprocessor<NV, D>> kNNIndex = ResultUtil.filterResults(result, MaterializeKNNPreprocessor.class);
      for(AbstractMaterializeKNNPreprocessor<NV, D> kNN : kNNIndex) {
        Iterator<ScatterPlotProjector<?>> ps = ResultUtil.filteredResults(baseResult, ScatterPlotProjector.class);
        for(ScatterPlotProjector<?> p : IterableUtil.fromIterator(ps)) {
          final VisualizationTask task = new VisualizationTask(NAME, kNN, p.getRelation(), this);
          task.put(VisualizationTask.META_LEVEL, VisualizationTask.LEVEL_DATA - 1);
          baseResult.getHierarchy().add(kNN, task);
          baseResult.getHierarchy().add(p, task);
        }
      }
    }
  }
}