package de.lmu.ifi.dbs.elki.index.tree.metrical.mtreevariants.mtree;
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

import de.lmu.ifi.dbs.elki.database.relation.Relation;
import de.lmu.ifi.dbs.elki.distance.distancefunction.DistanceFunction;
import de.lmu.ifi.dbs.elki.distance.distancevalue.Distance;
import de.lmu.ifi.dbs.elki.index.tree.metrical.mtreevariants.AbstractMTreeFactory;
import de.lmu.ifi.dbs.elki.index.tree.metrical.mtreevariants.MTreeEntry;
import de.lmu.ifi.dbs.elki.persistent.PageFile;
import de.lmu.ifi.dbs.elki.utilities.ClassGenericsUtil;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.Parameterization;

/**
 * Factory for a M-Tree
 * 
 * @author Erich Schubert
 * 
 * @apiviz.stereotype factory
 * @apiviz.uses MTreeIndex oneway - - «create»
 * 
 * @param <O> Object type
 * @param <D> Distance type
 */
public class MTreeFactory<O, D extends Distance<D>> extends AbstractMTreeFactory<O, D, MTreeNode<O, D>, MTreeEntry<D>, MTreeIndex<O, D>> {
  /**
   * Constructor.
   * 
   * @param fileName file name
   * @param pageSize page size
   * @param cacheSize cache size
   * @param distanceFunction Distance function
   */
  public MTreeFactory(String fileName, int pageSize, long cacheSize, DistanceFunction<O, D> distanceFunction) {
    super(fileName, pageSize, cacheSize, distanceFunction);
  }

  @Override
  public MTreeIndex<O, D> instantiate(Relation<O> relation) {
    PageFile<MTreeNode<O, D>> pagefile = makePageFile(getNodeClass());
    return new MTreeIndex<O, D>(relation, pagefile, distanceFunction.instantiate(relation), distanceFunction);
  }

  protected Class<MTreeNode<O, D>> getNodeClass() {
    return ClassGenericsUtil.uglyCastIntoSubclass(MTreeNode.class);
  }

  /**
   * Parameterization class.
   * 
   * @author Erich Schubert
   * 
   * @apiviz.exclude
   */
  public static class Parameterizer<O, D extends Distance<D>> extends AbstractMTreeFactory.Parameterizer<O, D> {
    @Override
    protected void makeOptions(Parameterization config) {
      super.makeOptions(config);
    }

    @Override
    protected MTreeFactory<O, D> makeInstance() {
      return new MTreeFactory<O, D>(fileName, pageSize, cacheSize, distanceFunction);
    }
  }
}