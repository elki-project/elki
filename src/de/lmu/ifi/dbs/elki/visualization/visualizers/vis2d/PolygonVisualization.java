package de.lmu.ifi.dbs.elki.visualization.visualizers.vis2d;

import java.util.ArrayList;

import org.w3c.dom.Element;

import de.lmu.ifi.dbs.elki.data.spatial.Polygon;
import de.lmu.ifi.dbs.elki.data.spatial.PolygonsObject;
import de.lmu.ifi.dbs.elki.data.type.TypeUtil;
import de.lmu.ifi.dbs.elki.database.datastore.DataStoreEvent;
import de.lmu.ifi.dbs.elki.database.datastore.DataStoreListener;
import de.lmu.ifi.dbs.elki.database.ids.DBID;
import de.lmu.ifi.dbs.elki.database.relation.Relation;
import de.lmu.ifi.dbs.elki.math.linearalgebra.Vector;
import de.lmu.ifi.dbs.elki.result.HierarchicalResult;
import de.lmu.ifi.dbs.elki.result.Result;
import de.lmu.ifi.dbs.elki.result.ResultUtil;
import de.lmu.ifi.dbs.elki.utilities.exceptions.ObjectNotFoundException;
import de.lmu.ifi.dbs.elki.visualization.projections.Projection;
import de.lmu.ifi.dbs.elki.visualization.projections.Projection2D;
import de.lmu.ifi.dbs.elki.visualization.svg.SVGPath;
import de.lmu.ifi.dbs.elki.visualization.svg.SVGUtil;
import de.lmu.ifi.dbs.elki.visualization.visualizers.AbstractVisFactory;
import de.lmu.ifi.dbs.elki.visualization.visualizers.AbstractVisualization;
import de.lmu.ifi.dbs.elki.visualization.visualizers.Visualization;
import de.lmu.ifi.dbs.elki.visualization.visualizers.VisualizationTask;
import de.lmu.ifi.dbs.elki.visualization.visualizers.VisualizerContext;
import de.lmu.ifi.dbs.elki.visualization.visualizers.VisualizerUtil;

/**
 * Renders PolygonsObject in the data set.
 * 
 * @author Erich Schubert
 * 
 * @apiviz.has PolygonsObject - - visualizes
 */
public class PolygonVisualization extends AbstractVisualization implements DataStoreListener {
  /**
   * A short name characterizing this Visualizer.
   */
  private static final String NAME = "Polygons";

  /**
   * Generic tag to indicate the type of element. Used in IDs, CSS-Classes etc.
   */
  public static final String MARKER = "polys";

  /**
   * The current projection
   */
  final protected Projection2D proj;

  /**
   * The representation we visualize
   */
  final protected Relation<PolygonsObject> rep;

  /**
   * Constructor.
   * 
   * @param task Task to visualize
   */
  public PolygonVisualization(VisualizationTask task) {
    super(task);
    this.proj = task.getProj();
    this.rep = task.getRelation();
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
    // draw data
    for(DBID id : rep.iterDBIDs()) {
      try {
        PolygonsObject poly = rep.get(id);
        SVGPath path = new SVGPath();
        for(Polygon ppoly : poly.getPolygons()) {
          Vector first = ppoly.get(0);
          double[] f = proj.fastProjectDataToRenderSpace(first);
          path.moveTo(f[0], f[1]);
          for(int i = 1; i < ppoly.size(); i++) {
            double[] p = proj.fastProjectDataToRenderSpace(first);
            path.drawTo(p[0], p[1]);
          }
          // close path.
          path.drawTo(f[0], f[1]);
        }
        Element e = path.makeElement(svgp);
        SVGUtil.addCSSClass(e, MARKER);
        layer.appendChild(e);
      }
      catch(ObjectNotFoundException e) {
        // ignore.
      }

    }
  }

  @Override
  public void contentChanged(@SuppressWarnings("unused") DataStoreEvent e) {
    synchronizedRedraw();
  }

  /**
   * The visualization factory
   * 
   * @author Erich Schubert
   * 
   * @apiviz.stereotype factory
   * @apiviz.uses PolygonVisualization oneway - - «create»
   */
  public static class Factory extends AbstractVisFactory {
    /**
     * Constructor
     */
    public Factory() {
      super();
    }

    @Override
    public Visualization makeVisualization(VisualizationTask task) {
      return new PolygonVisualization(task);
    }

    @Override
    public void processNewResult(HierarchicalResult baseResult, Result result) {
      final VisualizerContext context = VisualizerUtil.getContext(baseResult);
      ArrayList<Relation<?>> results = ResultUtil.filterResults(result, Relation.class);
      for(Relation<?> rel : results) {
        if(TypeUtil.POLYGON_TYPE.isAssignableFromType(rel.getDataTypeInformation())) {
          final VisualizationTask task = new VisualizationTask(NAME, rel, rel, this, P2DVisualization.class);
          task.put(VisualizationTask.META_LEVEL, VisualizationTask.LEVEL_DATA);
          context.addVisualizer(rel, task);
        }
      }
    }

    @Override
    public Class<? extends Projection> getProjectionType() {
      return Projection2D.class;
    }
  }
}