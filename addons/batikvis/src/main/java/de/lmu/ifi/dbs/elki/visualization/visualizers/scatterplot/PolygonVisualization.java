package de.lmu.ifi.dbs.elki.visualization.visualizers.scatterplot;

import org.apache.batik.util.SVGConstants;
import org.w3c.dom.Element;

import de.lmu.ifi.dbs.elki.data.spatial.Polygon;
import de.lmu.ifi.dbs.elki.data.spatial.PolygonsObject;
import de.lmu.ifi.dbs.elki.data.type.TypeUtil;
import de.lmu.ifi.dbs.elki.database.datastore.DataStoreListener;
import de.lmu.ifi.dbs.elki.database.ids.DBIDIter;
import de.lmu.ifi.dbs.elki.database.relation.Relation;
import de.lmu.ifi.dbs.elki.database.relation.RelationUtil;
import de.lmu.ifi.dbs.elki.math.linearalgebra.Vector;
import de.lmu.ifi.dbs.elki.utilities.datastructures.iterator.ArrayListIter;
import de.lmu.ifi.dbs.elki.utilities.exceptions.ObjectNotFoundException;
import de.lmu.ifi.dbs.elki.visualization.VisualizationTask;
import de.lmu.ifi.dbs.elki.visualization.VisualizerContext;
import de.lmu.ifi.dbs.elki.visualization.css.CSSClass;
import de.lmu.ifi.dbs.elki.visualization.projector.ScatterPlotProjector;
import de.lmu.ifi.dbs.elki.visualization.style.StyleLibrary;
import de.lmu.ifi.dbs.elki.visualization.svg.SVGPath;
import de.lmu.ifi.dbs.elki.visualization.svg.SVGUtil;
import de.lmu.ifi.dbs.elki.visualization.visualizers.AbstractVisFactory;
import de.lmu.ifi.dbs.elki.visualization.visualizers.Visualization;
import de.lmu.ifi.dbs.elki.visualization.VisualizationTree;

/**
 * Renders PolygonsObject in the data set.
 *
 * @author Erich Schubert
 *
 * @apiviz.stereotype factory
 * @apiviz.uses Instance oneway - - «create»
 */
public class PolygonVisualization extends AbstractVisFactory {
  /**
   * A short name characterizing this Visualizer.
   */
  private static final String NAME = "Polygons";

  /**
   * Constructor
   */
  public PolygonVisualization() {
    super();
  }

  @Override
  public Visualization makeVisualization(VisualizationTask task) {
    return new Instance(task);
  }

  @Override
  public void processNewResult(VisualizerContext context, Object result) {
    VisualizationTree.findNewResultVis(context, result, Relation.class, ScatterPlotProjector.class, new VisualizationTree.Handler2<Relation<?>, ScatterPlotProjector<?>>() {
      @Override
      public void process(VisualizerContext context, Relation<?> rel, ScatterPlotProjector<?> p) {
        if(!TypeUtil.POLYGON_TYPE.isAssignableFromType(rel.getDataTypeInformation())) {
          return;
        }
        if(RelationUtil.dimensionality(p.getRelation()) != 2) {
          return;
        }
        // Assume that a 2d projector is using the same coordinates as the
        // polygons.
        final VisualizationTask task = new VisualizationTask(NAME, rel, rel, PolygonVisualization.this);
        task.level = VisualizationTask.LEVEL_DATA - 10;
        context.addVis(rel, task);
        context.addVis(p, task);
      }
    });
  }

  /**
   * Instance
   *
   * @author Erich Schubert
   *
   * @apiviz.has PolygonsObject - - visualizes
   */
  public class Instance extends AbstractScatterplotVisualization implements DataStoreListener {
    /**
     * Generic tag to indicate the type of element. Used in IDs, CSS-Classes
     * etc.
     */
    public static final String POLYS = "polys";

    /**
     * The representation we visualize
     */
    final protected Relation<PolygonsObject> rep;

    /**
     * Constructor.
     *
     * @param task Task to visualize
     */
    public Instance(VisualizationTask task) {
      super(task);
      this.rep = task.getResult(); // Note: relation was used for projection
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
      final StyleLibrary style = context.getStyleResult().getStyleLibrary();
      CSSClass css = new CSSClass(svgp, POLYS);
      // TODO: separate fill and line colors?
      css.setStatement(SVGConstants.CSS_STROKE_WIDTH_PROPERTY, style.getLineWidth(StyleLibrary.POLYGONS));
      css.setStatement(SVGConstants.CSS_STROKE_PROPERTY, style.getColor(StyleLibrary.POLYGONS));
      css.setStatement(SVGConstants.CSS_FILL_PROPERTY, SVGConstants.CSS_NONE_VALUE);
      svgp.addCSSClassOrLogError(css);
      svgp.updateStyleElement();

      // draw data
      for(DBIDIter iditer = rep.iterDBIDs(); iditer.valid(); iditer.advance()) {
        try {
          PolygonsObject poly = rep.get(iditer);
          if(poly == null) {
            continue;
          }
          SVGPath path = new SVGPath();
          for(Polygon ppoly : poly.getPolygons()) {
            Vector first = ppoly.get(0);
            double[] f = proj.fastProjectDataToRenderSpace(first.getArrayRef());
            path.moveTo(f[0], f[1]);
            for(ArrayListIter<Vector> it = ppoly.iter(); it.valid(); it.advance()) {
              if(it.getOffset() == 0) {
                continue;
              }
              double[] p = proj.fastProjectDataToRenderSpace(it.get().getArrayRef());
              path.drawTo(p[0], p[1]);
            }
            // close path.
            path.drawTo(f[0], f[1]);
          }
          Element e = path.makeElement(svgp);
          SVGUtil.addCSSClass(e, POLYS);
          layer.appendChild(e);
        }
        catch(ObjectNotFoundException e) {
          // ignore.
        }
      }
    }
  }
}