package de.lmu.ifi.dbs.elki.index.tree.metrical.mtreevariants.mktrees.mkmax;

/*
 This file is part of ELKI:
 Environment for Developing KDD-Applications Supported by Index-Structures

 Copyright (C) 2013
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

import de.lmu.ifi.dbs.elki.database.relation.Relation;
import de.lmu.ifi.dbs.elki.distance.distancefunction.DistanceFunction;
import de.lmu.ifi.dbs.elki.distance.distancevalue.Distance;
import de.lmu.ifi.dbs.elki.index.tree.metrical.mtreevariants.mktrees.AbstractMkTreeUnifiedFactory;
import de.lmu.ifi.dbs.elki.index.tree.metrical.mtreevariants.strategies.split.MTreeSplit;
import de.lmu.ifi.dbs.elki.persistent.PageFile;
import de.lmu.ifi.dbs.elki.utilities.ClassGenericsUtil;

/**
 * Factory for MkMaxTrees
 * 
 * @author Erich Schubert
 * 
 * @apiviz.stereotype factory
 * @apiviz.uses MkMaxTreeIndex oneway - - «create»
 *
 * @param <O> Object type
 * @param <D> Distance type
 */
public class MkMaxTreeFactory<O, D extends Distance<D>> extends AbstractMkTreeUnifiedFactory<O, D, MkMaxTreeNode<O, D>, MkMaxEntry<D>, MkMaxTreeIndex<O, D>> {
  /**
   * Constructor.
   *
   * @param fileName Filename
   * @param pageSize Page size
   * @param cacheSize Cache size
   * @param distanceFunction Distance function
   * @param splitStrategy Split strategy
   * @param k_max Maximum k
   */
  public MkMaxTreeFactory(String fileName, int pageSize, long cacheSize, DistanceFunction<O, D> distanceFunction, MTreeSplit<O, D, MkMaxTreeNode<O, D>, MkMaxEntry<D>> splitStrategy, int k_max) {
    super(fileName, pageSize, cacheSize, distanceFunction, splitStrategy, k_max);
  }

  @Override
  public MkMaxTreeIndex<O, D> instantiate(Relation<O> relation) {
    PageFile<MkMaxTreeNode<O, D>> pagefile = makePageFile(getNodeClass());
    return new MkMaxTreeIndex<>(relation, pagefile, distanceFunction.instantiate(relation), distanceFunction, splitStrategy, k_max);
  }

  protected Class<MkMaxTreeNode<O, D>> getNodeClass() {
    return ClassGenericsUtil.uglyCastIntoSubclass(MkMaxTreeNode.class);
  }

  /**
   * Parameterization class.
   * 
   * @author Erich Schubert
   * 
   * @apiviz.exclude
   */
  public static class Parameterizer<O, D extends Distance<D>> extends AbstractMkTreeUnifiedFactory.Parameterizer<O, D, MkMaxTreeNode<O, D>, MkMaxEntry<D>> {
    @Override
    protected MkMaxTreeFactory<O, D> makeInstance() {
      return new MkMaxTreeFactory<>(fileName, pageSize, cacheSize, distanceFunction, splitStrategy, k_max);
    }
  }
}