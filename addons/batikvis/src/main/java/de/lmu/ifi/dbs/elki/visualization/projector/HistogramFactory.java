package de.lmu.ifi.dbs.elki.visualization.projector;

/*
 This file is part of ELKI:
 Environment for Developing KDD-Applications Supported by Index-Structures

 Copyright (C) 2015
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

import de.lmu.ifi.dbs.elki.data.NumberVector;
import de.lmu.ifi.dbs.elki.data.type.TypeUtil;
import de.lmu.ifi.dbs.elki.database.relation.Relation;
import de.lmu.ifi.dbs.elki.database.relation.RelationUtil;
import de.lmu.ifi.dbs.elki.utilities.datastructures.hierarchy.FilteredIter;
import de.lmu.ifi.dbs.elki.utilities.datastructures.hierarchy.Hierarchy;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.AbstractParameterizer;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.constraints.CommonConstraints;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.Parameterization;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.IntParameter;
import de.lmu.ifi.dbs.elki.visualization.VisualizationTree;
import de.lmu.ifi.dbs.elki.visualization.VisualizerContext;

/**
 * Produce one-dimensional projections.
 *
 * @author Erich Schubert
 *
 * @apiviz.has HistogramProjector
 */
public class HistogramFactory implements ProjectorFactory {
  /**
   * Maximum dimensionality.
   */
  private int maxdim = ScatterPlotFactory.MAX_DIMENSIONS_DEFAULT;

  /**
   * Constructor.
   *
   * @param maxdim Maximum dimensionality
   */
  public HistogramFactory(int maxdim) {
    super();
    this.maxdim = maxdim;
  }

  @Override
  public void processNewResult(VisualizerContext context, Object start) {
    Hierarchy.Iter<Relation<?>> it1 = VisualizationTree.filterResults(context, start, Relation.class);
    candidate: for(; it1.valid(); it1.advance()) {
      Relation<?> rel = it1.get();
      if(!TypeUtil.NUMBER_VECTOR_FIELD.isAssignableFromType(rel.getDataTypeInformation())) {
        continue;
      }
      // Do not enable nested relations by default:
      Hierarchy.Iter<Relation<?>> it2 = new FilteredIter<>(context.getHierarchy().iterAncestors(rel), Relation.class);
      for(; it2.valid(); it2.advance()) {
        // Parent relation
        final Relation<?> rel2 = (Relation<?>) it2.get();
        if(TypeUtil.SPATIAL_OBJECT.isAssignableFromType(rel2.getDataTypeInformation())) {
          continue candidate;
        }
        // TODO: add Actions instead.
      }
      @SuppressWarnings("unchecked")
      Relation<NumberVector> vrel = (Relation<NumberVector>) rel;
      final int dim = RelationUtil.dimensionality(vrel);
      HistogramProjector<NumberVector> proj = new HistogramProjector<>(vrel, Math.min(dim, maxdim));
      context.addVis(vrel, proj);
    }
  }

  /**
   * Parameterization class.
   *
   * @author Erich Schubert
   *
   * @apiviz.exclude
   */
  public static class Parameterizer extends AbstractParameterizer {
    /**
     * Stores the maximum number of dimensions to show.
     */
    private int maxdim = ScatterPlotFactory.MAX_DIMENSIONS_DEFAULT;

    @Override
    protected void makeOptions(Parameterization config) {
      super.makeOptions(config);
      IntParameter maxdimP = new IntParameter(ScatterPlotFactory.Parameterizer.MAXDIM_ID, ScatterPlotFactory.MAX_DIMENSIONS_DEFAULT);
      maxdimP.addConstraint(CommonConstraints.GREATER_EQUAL_ZERO_INT);
      if(config.grab(maxdimP)) {
        maxdim = maxdimP.intValue();
      }
    }

    @Override
    protected HistogramFactory makeInstance() {
      return new HistogramFactory(maxdim);
    }
  }
}