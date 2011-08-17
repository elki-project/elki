package de.lmu.ifi.dbs.elki.visualization.visualizers.vis2d;
/*
This file is part of ELKI:
Environment for Developing KDD-Applications Supported by Index-Structures

Copyright (C) 2011
Ludwig-Maximilians-Universität München
Lehr- und Forschungseinheit für Datenbanksysteme
ELKI Development Team

This program is free software: you can redistribute it and/or modify
it under the terms of the GNU Affero General Public License as published by
the Free Software Foundation, either version 3 of the License, or
(at your option) any later version.

This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU Affero General Public License for more details.

You should have received a copy of the GNU Affero General Public License
along with this program.  If not, see <http://www.gnu.org/licenses/>.
*/

import java.util.ArrayList;

import org.apache.batik.util.SVGConstants;
import org.w3c.dom.Element;

import de.lmu.ifi.dbs.elki.data.DoubleVector;
import de.lmu.ifi.dbs.elki.data.NumberVector;
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
import de.lmu.ifi.dbs.elki.visualization.css.CSSClass;
import de.lmu.ifi.dbs.elki.visualization.projections.Projection;
import de.lmu.ifi.dbs.elki.visualization.projections.Projection2D;
import de.lmu.ifi.dbs.elki.visualization.style.StyleLibrary;
import de.lmu.ifi.dbs.elki.visualization.svg.SVGPath;
import de.lmu.ifi.dbs.elki.visualization.svg.SVGUtil;
import de.lmu.ifi.dbs.elki.visualization.visualizers.AbstractVisFactory;
import de.lmu.ifi.dbs.elki.visualization.visualizers.Visualization;
import de.lmu.ifi.dbs.elki.visualization.visualizers.VisualizationTask;

/**
 * Renders PolygonsObject in the data set.
 * 
 * @author Erich Schubert
 * 
 * @apiviz.has PolygonsObject - - visualizes
 */
public class PolygonVisualization<V extends NumberVector<?, ?>> extends P2DVisualization<V> implements DataStoreListener {
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
    CSSClass css = new CSSClass(svgp, MARKER);
    css.setStatement(SVGConstants.CSS_STROKE_WIDTH_PROPERTY, context.getStyleLibrary().getLineWidth(StyleLibrary.PLOT));
    css.setStatement(SVGConstants.CSS_STROKE_PROPERTY, context.getStyleLibrary().getColor(StyleLibrary.PLOT));
    svgp.addCSSClassOrLogError(css);
    svgp.updateStyleElement();

    // draw data
    for(DBID id : rep.iterDBIDs()) {
      try {
        PolygonsObject poly = rep.get(id);
        if(poly == null) {
          continue;
        }
        SVGPath path = new SVGPath();
        for(Polygon ppoly : poly.getPolygons()) {
          Vector first = ppoly.get(0);
          double[] f = proj.fastProjectDataToRenderSpace(first);
          path.moveTo(f[0], f[1]);
          for(Vector v : ppoly) {
            if(v == first) {
              continue;
            }
            double[] p = proj.fastProjectDataToRenderSpace(v);
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
      return new PolygonVisualization<DoubleVector>(task);
    }

    @Override
    public void processNewResult(HierarchicalResult baseResult, Result result) {
      ArrayList<Relation<?>> results = ResultUtil.filterResults(result, Relation.class);
      for(Relation<?> rel : results) {
        if(TypeUtil.POLYGON_TYPE.isAssignableFromType(rel.getDataTypeInformation())) {
          final VisualizationTask task = new VisualizationTask(NAME, rel, rel, this, P2DVisualization.class);
          task.put(VisualizationTask.META_LEVEL, VisualizationTask.LEVEL_DATA);
          baseResult.getHierarchy().add(rel, task);
        }
      }
    }

    @Override
    public Class<? extends Projection> getProjectionType() {
      return Projection2D.class;
    }
  }
}