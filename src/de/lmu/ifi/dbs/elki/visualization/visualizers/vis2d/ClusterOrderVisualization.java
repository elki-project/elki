package de.lmu.ifi.dbs.elki.visualization.visualizers.vis2d;

import java.util.Collection;

import org.w3c.dom.Element;

import de.lmu.ifi.dbs.elki.data.NumberVector;
import de.lmu.ifi.dbs.elki.database.Database;
import de.lmu.ifi.dbs.elki.database.datastore.DataStoreEvent;
import de.lmu.ifi.dbs.elki.database.datastore.DataStoreListener;
import de.lmu.ifi.dbs.elki.database.ids.DBID;
import de.lmu.ifi.dbs.elki.distance.distancevalue.DoubleDistance;
import de.lmu.ifi.dbs.elki.result.Result;
import de.lmu.ifi.dbs.elki.result.ClusterOrderEntry;
import de.lmu.ifi.dbs.elki.result.ClusterOrderResult;
import de.lmu.ifi.dbs.elki.result.ResultUtil;
import de.lmu.ifi.dbs.elki.visualization.css.CSSClass;
import de.lmu.ifi.dbs.elki.visualization.style.StyleLibrary;
import de.lmu.ifi.dbs.elki.visualization.svg.SVGUtil;
import de.lmu.ifi.dbs.elki.visualization.visualizers.VisFactory;
import de.lmu.ifi.dbs.elki.visualization.visualizers.Visualization;
import de.lmu.ifi.dbs.elki.visualization.visualizers.VisualizationTask;
import de.lmu.ifi.dbs.elki.visualization.visualizers.VisualizerContext;

/**
 * Cluster order visualizer.
 * 
 * @author Erich Schubert
 * 
 * @apiviz.has ClusterOrderResult oneway - - visualizes
 */
// TODO: listen for CLUSTER ORDER changes.
public class ClusterOrderVisualization<NV extends NumberVector<NV, ?>> extends P2DVisualization<NV> implements DataStoreListener<NV> {
  /**
   * A short name characterizing this Visualizer.
   */
  private static final String NAME = "Predecessor Graph";

  /**
   * CSS class name
   */
  private static final String CSSNAME = "predecessor";

  /**
   * The result we visualize
   */
  protected ClusterOrderResult<?> result;

  public ClusterOrderVisualization(VisualizationTask task) {
    super(task, VisFactory.LEVEL_STATIC);
    result = task.getResult();
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
    CSSClass cls = new CSSClass(this, CSSNAME);
    context.getLineStyleLibrary().formatCSSClass(cls, 0, context.getStyleLibrary().getLineWidth(StyleLibrary.CLUSTERORDER));

    svgp.addCSSClassOrLogError(cls);

    // get the Database
    Database<? extends NV> database = context.getDatabase();
    for(ClusterOrderEntry<?> ce : result) {
      DBID thisId = ce.getID();
      DBID prevId = ce.getPredecessorID();
      if(thisId == null || prevId == null) {
        continue;
      }
      double[] thisVec = proj.fastProjectDataToRenderSpace(database.get(thisId));
      double[] prevVec = proj.fastProjectDataToRenderSpace(database.get(prevId));

      Element arrow = svgp.svgLine(prevVec[0], prevVec[1], thisVec[0], thisVec[1]);
      SVGUtil.setCSSClass(arrow, cls.getName());

      layer.appendChild(arrow);
    }
  }

  @Override
  public void contentChanged(@SuppressWarnings("unused") DataStoreEvent<NV> e) {
    synchronizedRedraw();
  }

  /**
   * Visualize an OPTICS cluster order by drawing connection lines.
   * 
   * @author Erich Schubert
   * 
   * @apiviz.stereotype factory
   * @apiviz.uses ClusterOrderVisualization oneway - - «create»
   * 
   * @param <NV> object type
   */
  public static class Factory<NV extends NumberVector<NV, ?>> extends P2DVisFactory<NV> {
    /**
     * Constructor, adhering to
     * {@link de.lmu.ifi.dbs.elki.utilities.optionhandling.Parameterizable}
     */
    public Factory() {
      super(NAME);
      getMetadata().put(VisFactory.META_VISIBLE_DEFAULT, false);
    }
    
    @Override
    public Visualization makeVisualization(VisualizationTask task) {
      return new ClusterOrderVisualization<NV>(task);
    }

    @Override
    public void addVisualizers(VisualizerContext<? extends NV> context, Result result) {
      Collection<ClusterOrderResult<DoubleDistance>> cos = ResultUtil.filterResults(result, ClusterOrderResult.class);
      for(ClusterOrderResult<DoubleDistance> co : cos) {
        context.addVisualizer(co, this);
      }
    }
  }
}