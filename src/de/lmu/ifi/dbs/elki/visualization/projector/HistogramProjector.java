package de.lmu.ifi.dbs.elki.visualization.projector;

/*
 This file is part of ELKI:
 Environment for Developing KDD-Applications Supported by Index-Structures

 Copyright (C) 2012
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
import java.util.Collection;
import java.util.List;

import de.lmu.ifi.dbs.elki.data.NumberVector;
import de.lmu.ifi.dbs.elki.database.relation.Relation;
import de.lmu.ifi.dbs.elki.math.scales.LinearScale;
import de.lmu.ifi.dbs.elki.math.scales.Scales;
import de.lmu.ifi.dbs.elki.result.AbstractHierarchicalResult;
import de.lmu.ifi.dbs.elki.result.ResultUtil;
import de.lmu.ifi.dbs.elki.utilities.DatabaseUtil;
import de.lmu.ifi.dbs.elki.visualization.VisualizationTask;
import de.lmu.ifi.dbs.elki.visualization.gui.overview.PlotItem;
import de.lmu.ifi.dbs.elki.visualization.projections.Projection1D;
import de.lmu.ifi.dbs.elki.visualization.projections.Simple1D;
import de.lmu.ifi.dbs.elki.visualization.visualizers.visunproj.LabelVisFactory;

/**
 * ScatterPlotProjector is responsible for producing a set of scatterplot
 * visualizations.
 * 
 * @author Erich Schubert
 * 
 * @apiviz.has LinearScale
 * 
 * @param <V> Vector type
 */
public class HistogramProjector<V extends NumberVector<?, ?>> extends AbstractHierarchicalResult implements Projector {
  /**
   * Relation we project
   */
  Relation<V> rel;

  /**
   * Database dimensionality
   */
  int dmax;

  /**
   * Axis scales
   */
  LinearScale[] scales;

  /**
   * Constructor.
   * 
   * @param rel Relation
   * @param maxdim Maximum dimension to use
   */
  public HistogramProjector(Relation<V> rel, int maxdim) {
    super();
    this.rel = rel;
    this.dmax = maxdim;
    this.scales = Scales.calcScales(rel);
    assert (maxdim <= DatabaseUtil.dimensionality(rel)) : "Requested dimensionality larger than data dimensionality?!?";
  }

  @Override
  public Collection<PlotItem> arrange() {
    List<PlotItem> layout = new ArrayList<PlotItem>(1 + dmax);
    List<VisualizationTask> tasks = ResultUtil.filterResults(this, VisualizationTask.class);
    if (tasks.size() > 0){
      final double xoff = (dmax > 1) ? .1 : 0.;
      final double hheight = .5;
      final double lheight = .1;
      PlotItem master = new PlotItem(dmax + xoff, hheight + lheight, null);
      for(int d1 = 0; d1 < dmax; d1++) {
        Projection1D proj = new Simple1D(scales, d1 + 1);
        final PlotItem it = new PlotItem(d1 + xoff, lheight, 1., hheight, proj);
        it.visualizations = tasks;
        master.subitems.add(it);
      }
      layout.add(master);
      // Add labels
      for(int d1 = 0; d1 < dmax; d1++) {
        PlotItem it = new PlotItem(d1 + xoff, 0, 1., lheight, null);
        LabelVisFactory lbl = new LabelVisFactory(DatabaseUtil.getColumnLabel(rel, d1 + 1));
        final VisualizationTask task = new VisualizationTask("", null, null, lbl);
        task.height = lheight;
        task.width = 1;
        task.put(VisualizationTask.META_NODETAIL, true); 
        it.visualizations.add(task);
        master.subitems.add(it);
      }
    }
    return layout;
  }

  @Override
  public String getLongName() {
    return "Axis plot";
  }

  @Override
  public String getShortName() {
    return "axisplot";
  }

  /**
   * Get the relation we project.
   * 
   * @return Relation
   */
  public Relation<V> getRelation() {
    return rel;
  }
}