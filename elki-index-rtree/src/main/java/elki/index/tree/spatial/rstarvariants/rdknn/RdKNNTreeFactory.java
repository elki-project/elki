/*
 * This file is part of ELKI:
 * Environment for Developing KDD-Applications Supported by Index-Structures
 *
 * Copyright (C) 2022
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
package elki.index.tree.spatial.rstarvariants.rdknn;

import elki.data.NumberVector;
import elki.database.relation.Relation;
import elki.distance.SpatialPrimitiveDistance;
import elki.distance.minkowski.EuclideanDistance;
import elki.index.tree.spatial.rstarvariants.AbstractRStarTreeFactory;
import elki.persistent.PageFile;
import elki.persistent.PageFileFactory;
import elki.utilities.ClassGenericsUtil;
import elki.utilities.optionhandling.OptionID;
import elki.utilities.optionhandling.constraints.CommonConstraints;
import elki.utilities.optionhandling.parameterization.Parameterization;
import elki.utilities.optionhandling.parameters.IntParameter;
import elki.utilities.optionhandling.parameters.ObjectParameter;

/**
 * Factory for RdKNN R*-Trees.
 *
 * @author Erich Schubert
 * @since 0.7.0
 *
 * @stereotype factory
 * @navassoc - create - RdKNNTree
 *
 * @param <O> Object type
 */
public class RdKNNTreeFactory<O extends NumberVector> extends AbstractRStarTreeFactory<O, RdKNNNode, RdKNNEntry, RdkNNSettings> {
  /**
   * Parameter for k
   */
  public static final OptionID K_ID = new OptionID("rdknn.k", "positive integer specifying the maximal number k of reverse " + "k nearest neighbors to be supported.");

  /**
   * The default distance function.
   */
  public static final Class<?> DEFAULT_DISTANCE_FUNCTION = EuclideanDistance.class;

  /**
   * Parameter for distance function
   */
  public static final OptionID DISTANCE_FUNCTION_ID = new OptionID("rdknn.distancefunction", "Distance function to determine the distance between database objects.");

  /**
   * Constructor.
   *
   * @param pageFileFactory Data storage
   * @param settings Settings class
   */
  public RdKNNTreeFactory(PageFileFactory<?> pageFileFactory, RdkNNSettings settings) {
    super(pageFileFactory, settings);
  }

  @Override
  public RdKNNTree<O> instantiate(Relation<O> relation) {
    PageFile<RdKNNNode> pagefile = makePageFile(getNodeClass());
    RdKNNTree<O> index = new RdKNNTree<>(relation, pagefile, settings);
    return index;
  }

  protected Class<RdKNNNode> getNodeClass() {
    return ClassGenericsUtil.uglyCastIntoSubclass(RdKNNNode.class);
  }

  /**
   * Parameterization class.
   *
   * @author Erich Schubert
   */
  public static class Par<O extends NumberVector> extends AbstractRStarTreeFactory.Par<O, RdkNNSettings> {
    @Override
    public void configure(Parameterization config) {
      super.configure(config);
      new IntParameter(K_ID) //
          .addConstraint(CommonConstraints.GREATER_EQUAL_ONE_INT) //
          .grab(config, x -> settings.k_max = x);
      new ObjectParameter<SpatialPrimitiveDistance<NumberVector>>(DISTANCE_FUNCTION_ID, SpatialPrimitiveDistance.class, DEFAULT_DISTANCE_FUNCTION) //
          .grab(config, x -> settings.distance = x);
    }

    @Override
    public RdKNNTreeFactory<O> make() {
      return new RdKNNTreeFactory<>(pageFileFactory, settings);
    }

    @Override
    protected RdkNNSettings createSettings() {
      return new RdkNNSettings(1, EuclideanDistance.STATIC);
    }
  }
}
