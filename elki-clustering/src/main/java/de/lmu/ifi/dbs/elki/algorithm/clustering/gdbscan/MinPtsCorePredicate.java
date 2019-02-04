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
package de.lmu.ifi.dbs.elki.algorithm.clustering.gdbscan;

import de.lmu.ifi.dbs.elki.algorithm.clustering.DBSCAN;
import de.lmu.ifi.dbs.elki.data.type.SimpleTypeInformation;
import de.lmu.ifi.dbs.elki.data.type.TypeUtil;
import de.lmu.ifi.dbs.elki.database.Database;
import de.lmu.ifi.dbs.elki.database.ids.DBIDRef;
import de.lmu.ifi.dbs.elki.database.ids.DBIDs;
import de.lmu.ifi.dbs.elki.logging.Logging;
import de.lmu.ifi.dbs.elki.utilities.documentation.Reference;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.AbstractParameterizer;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.constraints.CommonConstraints;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.Parameterization;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.IntParameter;

/**
 * The DBSCAN default core point predicate -- having at least {@link #minpts}
 * neighbors.
 * <p>
 * Reference:
 * <p>
 * Martin Ester, Hans-Peter Kriegel, Jörg Sander, Xiaowei Xu<br>
 * A Density-Based Algorithm for Discovering Clusters in Large Spatial Databases
 * with Noise<br>
 * Proc. 2nd Int. Conf. on Knowledge Discovery and Data Mining (KDD '96)
 *
 * @author Erich Schubert
 * @since 0.5.0
 *
 * @has - - - Instance
 */
@Reference(authors = "Martin Ester, Hans-Peter Kriegel, Jörg Sander, Xiaowei Xu", //
    title = "A Density-Based Algorithm for Discovering Clusters in Large Spatial Databases with Noise", //
    booktitle = "Proc. 2nd Int. Conf. on Knowledge Discovery and Data Mining (KDD '96)", //
    url = "http://www.aaai.org/Library/KDD/1996/kdd96-037.php", //
    bibkey = "DBLP:conf/kdd/EsterKSX96")
public class MinPtsCorePredicate implements CorePredicate<DBIDs> {
  /**
   * Class logger.
   */
  public static final Logging LOG = Logging.getLogger(MinPtsCorePredicate.class);

  /**
   * The minpts parameter.
   */
  protected int minpts;

  /**
   * Default constructor.
   *
   * @param minpts Minimum number of neighbors to be a core point.
   */
  public MinPtsCorePredicate(int minpts) {
    super();
    this.minpts = minpts;
  }

  @Override
  public Instance instantiate(Database database) {
    return new Instance(minpts);
  }

  @Override
  public boolean acceptsType(SimpleTypeInformation<? extends DBIDs> type) {
    return TypeUtil.DBIDS.isAssignableFromType(type) //
        || TypeUtil.NEIGHBORLIST.isAssignableFromType(type);
  }

  /**
   * Instance for a particular data set.
   *
   * @author Erich Schubert
   */
  public static class Instance implements CorePredicate.Instance<DBIDs> {
    /**
     * The minpts parameter.
     */
    protected int minpts;

    /**
     * Constructor for this predicate.
     *
     * @param minpts MinPts parameter
     */
    public Instance(int minpts) {
      super();
      this.minpts = minpts;
    }

    @Override
    public boolean isCorePoint(DBIDRef point, DBIDs neighbors) {
      return neighbors.size() >= minpts;
    }
  }

  /**
   * Parameterization class
   *
   * @author Erich Schubert
   */
  public static class Parameterizer extends AbstractParameterizer {
    /**
     * Minpts value
     */
    protected int minpts;

    @Override
    protected void makeOptions(Parameterization config) {
      super.makeOptions(config);
      // Get the minpts parameter
      IntParameter minptsP = new IntParameter(DBSCAN.Parameterizer.MINPTS_ID) //
          .addConstraint(CommonConstraints.GREATER_EQUAL_ONE_INT);
      if(config.grab(minptsP)) {
        minpts = minptsP.intValue();
        if(minpts <= 2) {
          LOG.warning("DBSCAN with minPts <= 2 is equivalent to single-link clustering at a single height. Consider using larger values of minPts.");
        }
      }
    }

    @Override
    protected MinPtsCorePredicate makeInstance() {
      return new MinPtsCorePredicate(minpts);
    }
  }
}
