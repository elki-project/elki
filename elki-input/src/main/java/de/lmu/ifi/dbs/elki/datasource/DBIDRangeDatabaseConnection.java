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
package de.lmu.ifi.dbs.elki.datasource;

import de.lmu.ifi.dbs.elki.database.ids.DBIDFactory;
import de.lmu.ifi.dbs.elki.datasource.bundle.MultipleObjectsBundle;
import de.lmu.ifi.dbs.elki.utilities.documentation.Description;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.AbstractParameterizer;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.OptionID;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.Parameterization;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.IntParameter;

/**
 * This is a fake datasource that produces a static DBID range only.
 * 
 * This is useful when using e.g. a distance matrix to access external data.
 * 
 * @author Erich Schubert
 * @since 0.5.0
 */
@Description("This class generates a sequence of DBIDs to 'load' into a database. This is useful when using an external data matrix, and not requiring access to the actual vectors.")
public class DBIDRangeDatabaseConnection implements DatabaseConnection {
  /**
   * Begin of interval
   */
  int start;

  /**
   * Number of records to produce
   */
  int count;

  /**
   * Constructor.
   * 
   * @param start Starting ID
   * @param count Number of records to produce
   */
  public DBIDRangeDatabaseConnection(int start, int count) {
    super();
    this.start = start;
    this.count = count;
  }

  @Override
  public MultipleObjectsBundle loadData() {
    MultipleObjectsBundle b = new MultipleObjectsBundle();
    b.setDBIDs(DBIDFactory.FACTORY.generateStaticDBIDRange(start, count));
    return b;
  }

  /**
   * Parameterization class.
   * 
   * @author Erich Schubert
   */
  public static class Parameterizer extends AbstractParameterizer {
    /**
     * Parameter for starting ID to generate
     */
    public static final OptionID START_ID = new OptionID("idgen.start", "First integer DBID to generate.");

    /**
     * Parameter for number of IDs to generate
     */
    public static final OptionID COUNT_ID = new OptionID("idgen.count", "Number of DBID to generate.");

    /**
     * Begin of interval
     */
    int start;

    /**
     * Number of records to produce
     */
    int count;

    @Override
    protected void makeOptions(Parameterization config) {
      super.makeOptions(config);
      IntParameter startp = new IntParameter(START_ID, 0);
      if(config.grab(startp)) {
        start = startp.intValue();
      }
      IntParameter countp = new IntParameter(COUNT_ID);
      if(config.grab(countp)) {
        count = countp.intValue();
      }
    }

    @Override
    protected DBIDRangeDatabaseConnection makeInstance() {
      return new DBIDRangeDatabaseConnection(start, count);
    }
  }
}