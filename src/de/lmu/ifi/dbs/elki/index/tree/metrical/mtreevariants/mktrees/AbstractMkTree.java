package de.lmu.ifi.dbs.elki.index.tree.metrical.mtreevariants.mktrees;

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

import de.lmu.ifi.dbs.elki.database.ids.DBIDRef;
import de.lmu.ifi.dbs.elki.database.query.distance.DistanceQuery;
import de.lmu.ifi.dbs.elki.distance.distancefunction.DistanceFunction;
import de.lmu.ifi.dbs.elki.distance.distanceresultlist.DistanceDBIDResult;
import de.lmu.ifi.dbs.elki.distance.distancevalue.Distance;
import de.lmu.ifi.dbs.elki.index.tree.metrical.mtreevariants.AbstractMTree;
import de.lmu.ifi.dbs.elki.index.tree.metrical.mtreevariants.AbstractMTreeNode;
import de.lmu.ifi.dbs.elki.index.tree.metrical.mtreevariants.MTreeEntry;
import de.lmu.ifi.dbs.elki.persistent.PageFile;

/**
 * Abstract class for all M-Tree variants supporting processing of reverse
 * k-nearest neighbor queries by using the k-nn distances of the entries, where
 * k is less than or equal to the given parameter.
 * 
 * @author Elke Achtert
 * @param <O> the type of DatabaseObject to be stored in the metrical index
 * @param <D> the type of Distance used in the metrical index
 * @param <N> the type of MetricalNode used in the metrical index
 * @param <E> the type of MetricalEntry used in the metrical index
 */
public abstract class AbstractMkTree<O, D extends Distance<D>, N extends AbstractMTreeNode<O, D, N, E>, E extends MTreeEntry<D>> extends AbstractMTree<O, D, N, E> {
  /**
   * Constructor.
   * 
   * @param pagefile Page file
   * @param distanceQuery Distance query
   * @param distanceFunction Distance function
   */
  public AbstractMkTree(PageFile<N> pagefile, DistanceQuery<O, D> distanceQuery, DistanceFunction<O, D> distanceFunction) {
    super(pagefile, distanceQuery, distanceFunction);
  }
  
  /**
   * Performs a reverse k-nearest neighbor query for the given object ID. The
   * query result is in ascending order to the distance to the query object.
   * 
   * @param id the query object id
   * @param k the number of nearest neighbors to be returned
   * @return a List of the query results
   */
  public abstract DistanceDBIDResult<D> reverseKNNQuery(final DBIDRef id, int k);
}