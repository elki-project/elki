package de.lmu.ifi.dbs.elki.algorithm.clustering.gdbscan;

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

import java.util.List;

import de.lmu.ifi.dbs.elki.data.type.SimpleTypeInformation;
import de.lmu.ifi.dbs.elki.data.type.TypeUtil;
import de.lmu.ifi.dbs.elki.database.Database;
import de.lmu.ifi.dbs.elki.database.ids.DBIDRef;
import de.lmu.ifi.dbs.elki.database.ids.DBIDs;
import de.lmu.ifi.dbs.elki.database.query.DistanceResultPair;
import de.lmu.ifi.dbs.elki.utilities.documentation.Reference;
import de.lmu.ifi.dbs.elki.utilities.exceptions.AbortException;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.AbstractParameterizer;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.Parameterization;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.IntParameter;

/**
 * The DBSCAN default core point predicate -- having at least {@link #minpts}
 * neighbors.
 * 
 * <p>
 * Reference: <br>
 * M. Ester, H.-P. Kriegel, J. Sander, and X. Xu: A Density-Based Algorithm for
 * Discovering Clusters in Large Spatial Databases with Noise. <br>
 * In Proc. 2nd Int. Conf. on Knowledge Discovery and Data Mining (KDD '96),
 * Portland, OR, 1996.
 * </p>
 * 
 * @author Erich Schubert
 * 
 * @apiviz.has Instance
 */
@Reference(authors = "M. Ester, H.-P. Kriegel, J. Sander, and X. Xu", title = "A Density-Based Algorithm for Discovering Clusters in Large Spatial Databases with Noise", booktitle = "Proc. 2nd Int. Conf. on Knowledge Discovery and Data Mining (KDD '96), Portland, OR, 1996", url = "http://citeseerx.ist.psu.edu/viewdoc/summary?doi=10.1.1.71.1980")
public class MinPtsCorePredicate implements CorePredicate {
  /**
   * The minpts parameter.
   */
  int minpts;

  /**
   * Default constructor.
   * 
   * @param minpts Minimum number of neighbors to be a core point.
   */
  public MinPtsCorePredicate(int minpts) {
    super();
    this.minpts = minpts;
  }

  @SuppressWarnings("unchecked")
  @Override
  public <T> Instance<T> instantiate(Database database, SimpleTypeInformation<?> type) {
    if(TypeUtil.DBIDS.isAssignableFromType(type)) {
      return (Instance<T>) new DBIDsInstance(minpts);
    }
    if(TypeUtil.NEIGHBORLIST.isAssignableFromType(type)) {
      return (Instance<T>) new NeighborListInstance(minpts);
    }
    throw new AbortException("Incompatible predicate types");
  }

  @Override
  public boolean acceptsType(SimpleTypeInformation<?> type) {
    if(TypeUtil.DBIDS.isAssignableFromType(type)) {
      return true;
    }
    if(TypeUtil.NEIGHBORLIST.isAssignableFromType(type)) {
      return true;
    }
    return false;
  }

  /**
   * Instance for a particular data set.
   * 
   * @author Erich Schubert
   */
  public static class DBIDsInstance implements CorePredicate.Instance<DBIDs> {
    /**
     * The minpts parameter.
     */
    int minpts;

    /**
     * Constructor for this predicate.
     * 
     * @param minpts MinPts parameter
     */
    public DBIDsInstance(int minpts) {
      super();
      this.minpts = minpts;
    }

    @Override
    public boolean isCorePoint(DBIDRef point, DBIDs neighbors) {
      return neighbors.size() >= minpts;
    }
  }

  /**
   * Instance for a particular data set.
   * 
   * @author Erich Schubert
   */
  public static class NeighborListInstance implements CorePredicate.Instance<List<? extends DistanceResultPair<?>>> {
    /**
     * The minpts parameter.
     */
    int minpts;

    /**
     * Constructor for this predicate.
     * 
     * @param minpts MinPts parameter
     */
    public NeighborListInstance(int minpts) {
      super();
      this.minpts = minpts;
    }

    @Override
    public boolean isCorePoint(DBIDRef point, List<? extends DistanceResultPair<?>> neighbors) {
      return neighbors.size() >= minpts;
    }
  }

  /**
   * Parameterization class
   * 
   * @author Erich Schubert
   * 
   * @apiviz.exclude
   */
  public static class Parameterizer extends AbstractParameterizer {
    /**
     * Minpts value
     */
    int minpts;

    @Override
    protected void makeOptions(Parameterization config) {
      super.makeOptions(config);
      // Get the minpts parameter
      IntParameter minptsP = new IntParameter(de.lmu.ifi.dbs.elki.algorithm.clustering.DBSCAN.MINPTS_ID);
      if(config.grab(minptsP)) {
        minpts = minptsP.getValue();
      }
    }

    @Override
    protected MinPtsCorePredicate makeInstance() {
      return new MinPtsCorePredicate(minpts);
    }
  }
}