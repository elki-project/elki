package experimentalcode.students.roedler.parallelCoordinates.projector;

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
import java.util.Collection;
import java.util.List;

import de.lmu.ifi.dbs.elki.data.NumberVector;
import de.lmu.ifi.dbs.elki.database.relation.Relation;
import de.lmu.ifi.dbs.elki.result.AbstractHierarchicalResult;
import de.lmu.ifi.dbs.elki.result.ResultUtil;
import de.lmu.ifi.dbs.elki.visualization.VisualizationTask;
import de.lmu.ifi.dbs.elki.visualization.gui.overview.PlotItem;
import de.lmu.ifi.dbs.elki.visualization.projector.Projector;
import de.lmu.ifi.dbs.elki.visualization.scales.LinearScale;
import de.lmu.ifi.dbs.elki.visualization.scales.Scales;
import experimentalcode.students.roedler.parallelCoordinates.projections.ProjectionParallel;
import experimentalcode.students.roedler.parallelCoordinates.projections.SimpleParallel;

/**
 * ParallelPlotProjector is responsible for producing a parallelplot
 * visualization.
 * 
 * @author Robert Rödler
 * 
 * @param <V> Vector type
 */

public class ParallelPlotProjector<V extends NumberVector<?, ?>> extends AbstractHierarchicalResult implements Projector {
  /**
   * Relation we project
   */
  Relation<V> rel;

  /**
   * Axis scales
   */
  LinearScale[] scales;

  /**
   * Constructor.
   * 
   * @param rel Relation
   */
  public ParallelPlotProjector(Relation<V> rel) {
    super();
    this.rel = rel;
    this.scales = Scales.calcScales(rel);
  }

  @Override
  public Collection<PlotItem> arrange() {
    List<PlotItem> col = new ArrayList<PlotItem>(1);
    List<VisualizationTask> tasks = ResultUtil.filterResults(this, VisualizationTask.class);
    if (tasks.size() > 0) {
      ProjectionParallel proj = new SimpleParallel(scales, scales.length, new double[]{0.071, 0.071}, new double[]{1.8, 1.}, 0.71, 100.);
      
      final PlotItem it = new PlotItem(2, 1., proj);
      it.visualizations = tasks;
      col.add(it);
    }
    return col;
  }

  @Override
  public String getLongName() {
    return "Parallelplot";
  }

  @Override
  public String getShortName() {
    return "parallelplot";
  }

  /**
   * The relation we project.
   * 
   * @return Relation
   */
  public Relation<V> getRelation() {
    return rel;
  }
}