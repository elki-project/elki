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
package de.lmu.ifi.dbs.elki.visualization.projector;

import de.lmu.ifi.dbs.elki.data.NumberVector;
import de.lmu.ifi.dbs.elki.data.spatial.SpatialComparable;
import de.lmu.ifi.dbs.elki.data.type.TypeUtil;
import de.lmu.ifi.dbs.elki.data.uncertain.UncertainObject;
import de.lmu.ifi.dbs.elki.database.relation.Relation;
import de.lmu.ifi.dbs.elki.database.relation.RelationUtil;
import de.lmu.ifi.dbs.elki.utilities.datastructures.iterator.It;
import de.lmu.ifi.dbs.elki.visualization.VisualizationTree;
import de.lmu.ifi.dbs.elki.visualization.VisualizerContext;

/**
 * Produce parallel axes projections.
 *
 * @author Robert Rödler
 * @since 0.5.0
 *
 * @has - - - ParallelPlotProjector
 */
public class ParallelPlotFactory implements ProjectorFactory {
  /**
   * Constructor.
   */
  public ParallelPlotFactory() {
    super();
  }

  @Override
  public void processNewResult(VisualizerContext context, Object start) {
    VisualizationTree.findNewResults(context, start).filter(Relation.class).forEach(rel -> {
      // TODO: multi-relational parallel plots?
      final int dim = dimensionality(rel);
      if(dim <= 1) {
        return;
      }
      // Do not enable nested relations by default:
      for(It<Relation<?>> it2 = context.getHierarchy().iterAncestors(rel).filter(Relation.class); it2.valid(); it2.advance()) {
        // Parent relation
        if(dimensionality(it2.get()) == dim) {
          // TODO: add Actions instead?
          return;
        }
      }
      @SuppressWarnings("unchecked")
      Relation<SpatialComparable> vrel = (Relation<SpatialComparable>) rel;
      ParallelPlotProjector<SpatialComparable> proj = new ParallelPlotProjector<>(vrel);
      context.addVis(vrel, proj);
    });
  }

  private int dimensionality(Relation<?> rel) {
    if(TypeUtil.NUMBER_VECTOR_FIELD.isAssignableFromType(rel.getDataTypeInformation())) {
      @SuppressWarnings("unchecked")
      Relation<NumberVector> vrel = (Relation<NumberVector>) rel;
      return RelationUtil.dimensionality(vrel);
    }
    if(UncertainObject.UNCERTAIN_OBJECT_FIELD.isAssignableFromType(rel.getDataTypeInformation())) {
      @SuppressWarnings("unchecked")
      Relation<UncertainObject> vrel = (Relation<UncertainObject>) rel;
      return RelationUtil.dimensionality(vrel);
    }
    // TODO: allow other spatial objects of fixed dimensionality!
    return 0;
  }
}
