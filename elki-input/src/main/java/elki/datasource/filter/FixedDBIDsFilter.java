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
package elki.datasource.filter;

import elki.database.ids.DBIDFactory;
import elki.database.ids.DBIDRange;
import elki.datasource.bundle.BundleMeta;
import elki.datasource.bundle.MultipleObjectsBundle;
import elki.utilities.optionhandling.Parameterizer;
import elki.utilities.optionhandling.OptionID;
import elki.utilities.optionhandling.constraints.CommonConstraints;
import elki.utilities.optionhandling.parameterization.Parameterization;
import elki.utilities.optionhandling.parameters.IntParameter;

/**
 * This filter assigns static DBIDs, based on the sequence the objects appear in
 * the bundle by adding a column of DBID type to the bundle.
 *
 * @author Erich Schubert
 * @since 0.4.0
 *
 * @navhas - produces - DBIDRange
 */
public class FixedDBIDsFilter implements ObjectFilter {
  /**
   * The filtered meta
   */
  BundleMeta meta;

  /**
   * The next ID to assign
   */
  int curid = 0;

  /**
   * Constructor.
   *
   * @param startid ID to start enumerating with.
   */
  public FixedDBIDsFilter(int startid) {
    super();
    this.curid = startid;
  }

  @Override
  public MultipleObjectsBundle filter(MultipleObjectsBundle objects) {
    DBIDRange ids = DBIDFactory.FACTORY.generateStaticDBIDRange(curid, objects.dataLength());
    objects.setDBIDs(ids);
    curid += objects.dataLength();
    return objects;
  }

  /**
   * Parameterization class.
   *
   * @author Erich Schubert
   */
  public static class Par implements Parameterizer {
    /**
     * Optional parameter to specify the first object ID to use.
     */
    public static final OptionID IDSTART_ID = new OptionID("dbc.startid", "Object ID to start counting with");

    /**
     * First ID to use.
     */
    int startid = 0;

    @Override
    public void configure(Parameterization config) {
      new IntParameter(IDSTART_ID, 0) //
          .addConstraint(CommonConstraints.GREATER_EQUAL_ZERO_INT) //
          .grab(config, x -> startid = x);
    }

    @Override
    public FixedDBIDsFilter make() {
      return new FixedDBIDsFilter(startid);
    }
  }
}
