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

import java.util.Collection;
import java.util.Iterator;

import de.lmu.ifi.dbs.elki.data.Cluster;
import de.lmu.ifi.dbs.elki.data.Clustering;
import de.lmu.ifi.dbs.elki.data.NumberVector;
import de.lmu.ifi.dbs.elki.data.model.Model;
import de.lmu.ifi.dbs.elki.database.datastore.DataStoreEvent;
import de.lmu.ifi.dbs.elki.database.datastore.DataStoreListener;
import de.lmu.ifi.dbs.elki.database.ids.DBID;
import de.lmu.ifi.dbs.elki.result.HierarchicalResult;
import de.lmu.ifi.dbs.elki.result.Result;
import de.lmu.ifi.dbs.elki.result.ResultUtil;
import de.lmu.ifi.dbs.elki.utilities.exceptions.ObjectNotFoundException;
import de.lmu.ifi.dbs.elki.utilities.iterator.IterableUtil;
import de.lmu.ifi.dbs.elki.visualization.VisualizationTask;
import de.lmu.ifi.dbs.elki.visualization.projector.ScatterPlotProjector;
import de.lmu.ifi.dbs.elki.visualization.style.StyleLibrary;
import de.lmu.ifi.dbs.elki.visualization.style.marker.MarkerLibrary;
import de.lmu.ifi.dbs.elki.visualization.visualizers.AbstractVisFactory;
import de.lmu.ifi.dbs.elki.visualization.visualizers.Visualization;

/**
 * Visualize a clustering using different markers for different clusters.
 * 
 * @author Erich Schubert
 * 
 * @apiviz.has Clustering oneway - - visualizes
 * 
 * @param <NV> Type of the DatabaseObject being visualized.
 */
// TODO: ensure we have the right database for this
public class ClusteringVisualization<NV extends NumberVector<NV, ?>> extends P2DVisualization<NV> implements DataStoreListener {
  /**
   * The result we visualize
   */
  private Clustering<Model> clustering;

  /**
   * Constructor.
   * 
   * @param task Visualization task
   */
  public ClusteringVisualization(VisualizationTask task) {
    super(task);
    this.clustering = task.getResult();
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
    MarkerLibrary ml = context.getStyleLibrary().markers();
    double marker_size = context.getStyleLibrary().getSize(StyleLibrary.MARKERPLOT);
    // draw data
    Iterator<Cluster<Model>> ci = clustering.getAllClusters().iterator();
    for(int cnum = 0; cnum < clustering.getAllClusters().size(); cnum++) {
      Cluster<?> clus = ci.next();
      for(DBID objId : clus.getIDs()) {
        try {
          final NV vec = rep.get(objId);
          double[] v = proj.fastProjectDataToRenderSpace(vec);
          ml.useMarker(svgp, layer, v[0], v[1], cnum, marker_size);
        }
        catch(ObjectNotFoundException e) {
          // ignore.
        }
      }
    }
  }

  @Override
  public void contentChanged(@SuppressWarnings("unused") DataStoreEvent e) {
    synchronizedRedraw();
  }

  /**
   * Visualization factory
   * 
   * @author Erich Schubert
   * 
   * @apiviz.stereotype factory
   * @apiviz.uses ClusteringVisualization oneway - - «create»
   * 
   * @param <NV> Type of the DatabaseObject being visualized.
   */
  public static class Factory<NV extends NumberVector<NV, ?>> extends AbstractVisFactory {
    /**
     * A short name characterizing this Visualizer.
     */
    private static final String NAME = "Cluster Markers";

    /**
     * Constructor, adhering to
     * {@link de.lmu.ifi.dbs.elki.utilities.optionhandling.Parameterizable}
     */
    public Factory() {
      super();
    }

    @Override
    public Visualization makeVisualization(VisualizationTask task) {
      return new ClusteringVisualization<NV>(task);
    }

    @Override
    public void processNewResult(HierarchicalResult baseResult, Result result) {
      // Find clusterings we can visualize:
      Collection<Clustering<?>> clusterings = ResultUtil.filterResults(result, Clustering.class);
      for(Clustering<?> c : clusterings) {
        if(c.getAllClusters().size() > 0) {
          Iterator<ScatterPlotProjector<?>> ps = ResultUtil.filteredResults(baseResult, ScatterPlotProjector.class);
          for(ScatterPlotProjector<?> p : IterableUtil.fromIterator(ps)) {
            final VisualizationTask task = new VisualizationTask(NAME, c, p.getRelation(), this);
            task.put(VisualizationTask.META_LEVEL, VisualizationTask.LEVEL_DATA);
            baseResult.getHierarchy().add(c, task);
            baseResult.getHierarchy().add(p, task);
          }
        }
      }
    }
  }
}