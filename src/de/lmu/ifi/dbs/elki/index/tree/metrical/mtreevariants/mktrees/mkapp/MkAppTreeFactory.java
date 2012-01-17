package de.lmu.ifi.dbs.elki.index.tree.metrical.mtreevariants.mktrees.mkapp;

/*
 This file is part of ELKI:
 Environment for Developing KDD-Applications Supported by Index-Structures

 Copyright (C) 2012
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
import de.lmu.ifi.dbs.elki.distance.distancevalue.NumberDistance;
import de.lmu.ifi.dbs.elki.index.tree.metrical.mtreevariants.AbstractMTreeFactory;
import de.lmu.ifi.dbs.elki.persistent.PageFile;
import de.lmu.ifi.dbs.elki.utilities.ClassGenericsUtil;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.OptionID;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.constraints.GreaterConstraint;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.Parameterization;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.Flag;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.IntParameter;

/**
 * Factory for a MkApp-Tree
 * 
 * @author Erich Schubert
 * 
 * @apiviz.stereotype factory
 * @apiviz.uses MkAppTreeIndex oneway - - «create»
 * 
 * @param <O> Object type
 * @param <D> Distance type
 */
public class MkAppTreeFactory<O, D extends NumberDistance<D, ?>> extends AbstractMTreeFactory<O, D, MkAppTreeNode<O, D>, MkAppEntry<D>, MkAppTreeIndex<O, D>> {
  /**
   * Parameter for nolog
   */
  public static final OptionID NOLOG_ID = OptionID.getOrCreateOptionID("mkapp.nolog", "Flag to indicate that the approximation is done in the ''normal'' space instead of the log-log space (which is default).");

  /**
   * Parameter for k
   */
  public static final OptionID K_ID = OptionID.getOrCreateOptionID("mkapp.k", "positive integer specifying the maximum number k of reverse k nearest neighbors to be supported.");

  /**
   * Parameter for p
   */
  public static final OptionID P_ID = OptionID.getOrCreateOptionID("mkapp.p", "positive integer specifying the order of the polynomial approximation.");

  /**
   * Parameter k.
   */
  private int k_max;

  /**
   * Parameter p.
   */
  private int p;

  /**
   * Flag log.
   */
  private boolean log;

  /**
   * Constructor.
   * 
   * @param fileName
   * @param pageSize
   * @param cacheSize
   * @param distanceFunction
   * @param k_max
   * @param p
   * @param log
   */
  public MkAppTreeFactory(String fileName, int pageSize, long cacheSize, DistanceFunction<O, D> distanceFunction, int k_max, int p, boolean log) {
    super(fileName, pageSize, cacheSize, distanceFunction);
    this.k_max = k_max;
    this.p = p;
    this.log = log;
  }

  @Override
  public MkAppTreeIndex<O, D> instantiate(Relation<O> relation) {
    PageFile<MkAppTreeNode<O, D>> pagefile = makePageFile(getNodeClass());
    return new MkAppTreeIndex<O, D>(relation, pagefile, distanceFunction.instantiate(relation), distanceFunction, k_max, p, log);
  }

  protected Class<MkAppTreeNode<O, D>> getNodeClass() {
    return ClassGenericsUtil.uglyCastIntoSubclass(MkAppTreeNode.class);
  }

  /**
   * Parameterization class.
   * 
   * @author Erich Schubert
   * 
   * @apiviz.exclude
   */
  public static class Parameterizer<O, D extends NumberDistance<D, ?>> extends AbstractMTreeFactory.Parameterizer<O, D> {
    /**
     * Parameter k.
     */
    protected int k_max;

    /**
     * Parameter p.
     */
    protected int p;

    /**
     * Flag log.
     */
    protected boolean log;

    @Override
    protected void makeOptions(Parameterization config) {
      super.makeOptions(config);
      IntParameter K_PARAM = new IntParameter(K_ID, new GreaterConstraint(0));
      if(config.grab(K_PARAM)) {
        k_max = K_PARAM.getValue();
      }

      IntParameter P_PARAM = new IntParameter(P_ID, new GreaterConstraint(0));
      if(config.grab(P_PARAM)) {
        p = P_PARAM.getValue();
      }

      Flag NOLOG_FLAG = new Flag(NOLOG_ID);
      if(config.grab(NOLOG_FLAG)) {
        log = !NOLOG_FLAG.getValue();
      }
    }

    @Override
    protected MkAppTreeFactory<O, D> makeInstance() {
      return new MkAppTreeFactory<O, D>(fileName, pageSize, cacheSize, distanceFunction, k_max, p, log);
    }
  }
}