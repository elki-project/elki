package de.lmu.ifi.dbs.elki.index.tree.metrical.mtreevariants.mktrees.mkcop;

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
import de.lmu.ifi.dbs.elki.distance.distancevalue.NumberDistance;
import de.lmu.ifi.dbs.elki.index.tree.metrical.mtreevariants.AbstractMTreeFactory;
import de.lmu.ifi.dbs.elki.index.tree.metrical.mtreevariants.mktrees.MkTreeSettings;
import de.lmu.ifi.dbs.elki.persistent.PageFile;
import de.lmu.ifi.dbs.elki.persistent.PageFileFactory;
import de.lmu.ifi.dbs.elki.utilities.ClassGenericsUtil;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.OptionID;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.constraints.CommonConstraints;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.Parameterization;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.IntParameter;

/**
 * Factory for a MkCoPTree-Tree
 * 
 * @author Erich Schubert
 * 
 * @apiviz.stereotype factory
 * @apiviz.uses MkCoPTreeIndex oneway - - «create»
 * 
 * @param <O> Object type
 * @param <D> Distance type
 */
public class MkCopTreeFactory<O, D extends NumberDistance<D, ?>> extends AbstractMTreeFactory<O, D, MkCoPTreeNode<O, D>, MkCoPEntry, MkCoPTreeIndex<O, D>, MkTreeSettings<O, D, MkCoPTreeNode<O, D>, MkCoPEntry>> {
  /**
   * Parameter for k
   */
  public static final OptionID K_ID = new OptionID("mkcop.k", "positive integer specifying the maximum number k of reverse k nearest neighbors to be supported.");

  /**
   * Constructor.
   * 
   * @param pageFileFactory Data storage
   * @param settings Tree settings
   */
  public MkCopTreeFactory(PageFileFactory<?> pageFileFactory, MkTreeSettings<O, D, MkCoPTreeNode<O, D>, MkCoPEntry> settings) {
    super(pageFileFactory, settings);
  }

  @Override
  public MkCoPTreeIndex<O, D> instantiate(Relation<O> relation) {
    PageFile<MkCoPTreeNode<O, D>> pagefile = makePageFile(getNodeClass());
    return new MkCoPTreeIndex<>(relation, pagefile, settings);
  }

  protected Class<MkCoPTreeNode<O, D>> getNodeClass() {
    return ClassGenericsUtil.uglyCastIntoSubclass(MkCoPTreeNode.class);
  }

  /**
   * Parameterization class.
   * 
   * @author Erich Schubert
   * 
   * @apiviz.exclude
   */
  public static class Parameterizer<O, D extends NumberDistance<D, ?>> extends AbstractMTreeFactory.Parameterizer<O, D, MkCoPTreeNode<O, D>, MkCoPEntry, MkTreeSettings<O, D, MkCoPTreeNode<O, D>, MkCoPEntry>> {
    @Override
    protected void makeOptions(Parameterization config) {
      super.makeOptions(config);
      IntParameter k_maxP = new IntParameter(K_ID);
      k_maxP.addConstraint(CommonConstraints.GREATER_EQUAL_ONE_INT);
      if(config.grab(k_maxP)) {
        settings.k_max = k_maxP.intValue();
      }
    }

    @Override
    protected MkCopTreeFactory<O, D> makeInstance() {
      return new MkCopTreeFactory<>(pageFileFactory, settings);
    }

    @Override
    protected MkTreeSettings<O, D, MkCoPTreeNode<O, D>, MkCoPEntry> makeSettings() {
      return new MkTreeSettings<>();
    }
  }
}
