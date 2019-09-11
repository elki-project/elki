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
package elki.index.tree.spatial.rstarvariants.rstar;

import elki.data.NumberVector;
import elki.database.relation.Relation;
import elki.index.tree.spatial.SpatialEntry;
import elki.index.tree.spatial.rstarvariants.AbstractRStarTreeFactory;
import elki.index.tree.spatial.rstarvariants.RTreeSettings;
import elki.persistent.PageFile;
import elki.persistent.PageFileFactory;
import elki.utilities.Alias;

/**
 * Factory for regular R*-Trees.
 *
 * @author Erich Schubert
 * @since 0.4.0
 *
 * @stereotype factory
 * @navassoc - create - RStarTreeIndex
 *
 * @param <O> Object type
 */
@Alias({ "rstar", "r*" })
public class RStarTreeFactory<O extends NumberVector> extends AbstractRStarTreeFactory<O, RStarTreeNode, SpatialEntry, RTreeSettings> {
  /**
   * Constructor.
   *
   * @param pageFileFactory Data storage
   * @param settings Tree settings
   */
  public RStarTreeFactory(PageFileFactory<?> pageFileFactory, RTreeSettings settings) {
    super(pageFileFactory, settings);
  }

  @Override
  public RStarTreeIndex<O> instantiate(Relation<O> relation) {
    PageFile<RStarTreeNode> pagefile = makePageFile(getNodeClass());
    return new RStarTreeIndex<>(relation, pagefile, settings);
  }

  protected Class<RStarTreeNode> getNodeClass() {
    return RStarTreeNode.class;
  }

  /**
   * Parameterization class.
   *
   * @author Erich Schubert
   *
   * @hidden
   *
   * @param <O> Object type
   */
  public static class Par<O extends NumberVector> extends AbstractRStarTreeFactory.Par<O, RTreeSettings> {
    @Override
    public RStarTreeFactory<O> make() {
      return new RStarTreeFactory<>(pageFileFactory, settings);
    }

    @Override
    protected RTreeSettings createSettings() {
      return new RTreeSettings();
    }
  }
}
