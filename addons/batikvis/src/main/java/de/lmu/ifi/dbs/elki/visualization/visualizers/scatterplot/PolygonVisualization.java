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
package de.lmu.ifi.dbs.elki.visualization.visualizers.scatterplot;

import org.apache.batik.util.SVGConstants;

import de.lmu.ifi.dbs.elki.data.spatial.Polygon;
import de.lmu.ifi.dbs.elki.data.spatial.PolygonsObject;
import de.lmu.ifi.dbs.elki.data.type.TypeUtil;
import de.lmu.ifi.dbs.elki.database.datastore.DataStoreListener;
import de.lmu.ifi.dbs.elki.database.datastore.ObjectNotFoundException;
import de.lmu.ifi.dbs.elki.database.ids.DBIDIter;
import de.lmu.ifi.dbs.elki.database.relation.Relation;
import de.lmu.ifi.dbs.elki.database.relation.RelationUtil;
import de.lmu.ifi.dbs.elki.utilities.datastructures.iterator.ArrayListIter;
import de.lmu.ifi.dbs.elki.visualization.VisualizationTask;
import de.lmu.ifi.dbs.elki.visualization.VisualizationTask.UpdateFlag;
import de.lmu.ifi.dbs.elki.visualization.VisualizationTree;
import de.lmu.ifi.dbs.elki.visualization.VisualizerContext;
import de.lmu.ifi.dbs.elki.visualization.css.CSSClass;
import de.lmu.ifi.dbs.elki.visualization.gui.VisualizationPlot;
import de.lmu.ifi.dbs.elki.visualization.projections.Projection;
import de.lmu.ifi.dbs.elki.visualization.projector.ScatterPlotProjector;
import de.lmu.ifi.dbs.elki.visualization.style.StyleLibrary;
import de.lmu.ifi.dbs.elki.visualization.svg.SVGPath;
import de.lmu.ifi.dbs.elki.visualization.visualizers.VisFactory;
import de.lmu.ifi.dbs.elki.visualization.visualizers.Visualization;

/**
 * Renders PolygonsObject in the data set.
 *
 * @author Erich Schubert
 * @since 0.4.0
 *
 * @stereotype factory
 * @navassoc - create - Instance
 */
public class PolygonVisualization implements VisFactory {
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
  public Visualization makeVisualization(VisualizerContext context, VisualizationTask task, VisualizationPlot plot, double width, double height, Projection proj) {
    return new Instance(context, task, plot, width, height, proj);
  }

  @SuppressWarnings("unchecked")
  @Override
  public void processNewResult(VisualizerContext context, Object result) {
    VisualizationTree.findNewResultVis(context, result, Relation.class, ScatterPlotProjector.class, (rel, p) -> {
      if(!TypeUtil.POLYGON_TYPE.isAssignableFromType(rel.getDataTypeInformation())) {
        return;
      }
      if(RelationUtil.dimensionality((Relation<? extends PolygonsObject>) rel) != 2) {
        return;
      }
      // Assume that a 2d projector is using the same coordinates as the
      // polygons.
      final VisualizationTask task = new VisualizationTask(this, NAME, rel, rel) //
          .level(VisualizationTask.LEVEL_DATA - 10) //
          .with(UpdateFlag.ON_DATA);
      context.addVis(rel, task);
      context.addVis(p, task);
    });
  }

  /**
   * Instance
   *
   * @author Erich Schubert
   *
   * @has - visualizes - PolygonsObject
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
     * @param context Visualizer context
     * @param task Task to visualize
     * @param plot Plot to draw to
     * @param width Embedding width
     * @param height Embedding height
     * @param proj Projection
     */
    public Instance(VisualizerContext context, VisualizationTask task, VisualizationPlot plot, double width, double height, Projection proj) {
      super(context, task, plot, width, height, proj);
      this.rep = task.getResult(); // Note: relation was used for projection
      addListeners();
    }

    @Override
    public void fullRedraw() {
      setupCanvas();
      final StyleLibrary style = context.getStyleLibrary();
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
            double[] first = ppoly.get(0);
            double[] f = proj.fastProjectDataToRenderSpace(first);
            path.moveTo(f[0], f[1]);
            for(ArrayListIter<double[]> it = ppoly.iter(); it.valid(); it.advance()) {
              if(it.getOffset() == 0) {
                continue;
              }
              double[] p = proj.fastProjectDataToRenderSpace(it.get());
              path.drawTo(p[0], p[1]);
            }
            // close path.
            path.drawTo(f[0], f[1]);
          }
          layer.appendChild(path.makeElement(svgp, POLYS));
        }
        catch(ObjectNotFoundException e) {
          // ignore.
        }
      }
    }
  }
}
