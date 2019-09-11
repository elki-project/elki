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
package elki.visualization.projector;

import elki.data.NumberVector;
import elki.data.spatial.SpatialComparable;
import elki.data.type.TypeUtil;
import elki.data.uncertain.UncertainObject;
import elki.database.relation.Relation;
import elki.database.relation.RelationUtil;
import elki.result.Metadata;
import elki.utilities.datastructures.iterator.It;
import elki.utilities.optionhandling.Parameterizer;
import elki.utilities.optionhandling.OptionID;
import elki.utilities.optionhandling.constraints.CommonConstraints;
import elki.utilities.optionhandling.parameterization.Parameterization;
import elki.utilities.optionhandling.parameters.IntParameter;
import elki.visualization.VisualizationTree;
import elki.visualization.VisualizerContext;

/**
 * Produce scatterplot projections.
 *
 * @author Erich Schubert
 * @since 0.4.0
 *
 * @has - - - ScatterPlotProjector
 */
public class ScatterPlotFactory implements ProjectorFactory {
  /**
   * Maximum number of dimensions to visualize.
   * <p>
   * FIXME: add scrolling function for higher dimensionality!
   */
  public static final int MAX_DIMENSIONS_DEFAULT = 10;

  /**
   * Stores the maximum number of dimensions to show.
   */
  private int maxdim = MAX_DIMENSIONS_DEFAULT;

  /**
   * Constructor.
   *
   * @param maxdim Maximum number of dimensions to show.
   */
  public ScatterPlotFactory(int maxdim) {
    super();
    this.maxdim = maxdim;
  }

  @Override
  public void processNewResult(VisualizerContext context, Object start) {
    VisualizationTree.findNewResults(context, start).filter(Relation.class).forEach(rel -> {
      final int dim = dimensionality(rel);
      if(dim < 1) {
        return;
      }
      // Do not enable nested relations by default:
      for(It<Relation<?>> it2 = Metadata.hierarchyOf(rel).iterAncestors().filter(Relation.class); it2.valid(); it2.advance()) {
        // Parent relation
        if(dimensionality(it2.get()) == dim) {
          // TODO: add Actions instead?
          return;
        }
      }
      @SuppressWarnings("unchecked")
      Relation<SpatialComparable> vrel = (Relation<SpatialComparable>) rel;
      ScatterPlotProjector<SpatialComparable> proj = new ScatterPlotProjector<>(vrel, Math.min(maxdim, dim));
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

  /**
   * Parameterization class.
   *
   * @author Erich Schubert
   */
  public static class Par implements Parameterizer {
    /**
     * Parameter for the maximum number of dimensions.
     */
    public static final OptionID MAXDIM_ID = new OptionID("vis.maxdim", "Maximum number of dimensions to display.");

    /**
     * Stores the maximum number of dimensions to show.
     */
    private int maxdim = MAX_DIMENSIONS_DEFAULT;

    @Override
    public void configure(Parameterization config) {
      new IntParameter(MAXDIM_ID, MAX_DIMENSIONS_DEFAULT) //
          .addConstraint(CommonConstraints.GREATER_EQUAL_ZERO_INT) //
          .grab(config, x -> maxdim = x);
    }

    @Override
    public ScatterPlotFactory make() {
      return new ScatterPlotFactory(maxdim);
    }
  }
}
