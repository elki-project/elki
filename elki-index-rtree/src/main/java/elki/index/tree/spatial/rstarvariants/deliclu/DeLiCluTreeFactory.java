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
package elki.index.tree.spatial.rstarvariants.deliclu;

import elki.data.NumberVector;
import elki.database.relation.Relation;
import elki.index.tree.spatial.rstarvariants.AbstractRStarTreeFactory;
import elki.index.tree.spatial.rstarvariants.RTreeSettings;
import elki.persistent.PageFile;
import elki.persistent.PageFileFactory;

/**
 * Factory for DeLiClu R*-Trees.
 * 
 * @author Erich Schubert
 * @since 0.4.0
 * 
 * @stereotype factory
 * @navassoc - create - DeLiCluTreeIndex
 * 
 * @param <O> Object type
 */
public class DeLiCluTreeFactory<O extends NumberVector> extends AbstractRStarTreeFactory<O, DeLiCluNode, DeLiCluEntry, RTreeSettings> {
  /**
   * Constructor.
   * 
   * @param pageFileFactory Page file factory
   * @param settings Settings
   */
  public DeLiCluTreeFactory(PageFileFactory<?> pageFileFactory, RTreeSettings settings) {
    super(pageFileFactory, settings);
  }

  @Override
  public DeLiCluTreeIndex<O> instantiate(Relation<O> relation) {
    PageFile<DeLiCluNode> pagefile = makePageFile(getNodeClass());
    DeLiCluTreeIndex<O> index = new DeLiCluTreeIndex<>(relation, pagefile, settings);
    return index;
  }

  protected Class<DeLiCluNode> getNodeClass() {
    return DeLiCluNode.class;
  }

  /**
   * Parameterization class.
   * 
   * @author Erich Schubert
   */
  public static class Par<O extends NumberVector> extends AbstractRStarTreeFactory.Par<O, RTreeSettings> {
    @Override
    public DeLiCluTreeFactory<O> make() {
      return new DeLiCluTreeFactory<>(pageFileFactory, settings);
    }

    @Override
    protected RTreeSettings createSettings() {
      return new RTreeSettings();
    }
  }
}
