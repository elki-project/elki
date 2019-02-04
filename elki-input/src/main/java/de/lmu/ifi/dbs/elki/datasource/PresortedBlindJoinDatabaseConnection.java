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

import java.util.ArrayList;
import java.util.List;

import de.lmu.ifi.dbs.elki.datasource.bundle.MultipleObjectsBundle;
import de.lmu.ifi.dbs.elki.datasource.filter.ObjectFilter;
import de.lmu.ifi.dbs.elki.logging.Logging;
import de.lmu.ifi.dbs.elki.utilities.documentation.Description;
import de.lmu.ifi.dbs.elki.utilities.exceptions.AbortException;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.OptionID;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.Parameterization;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.ObjectListParameter;

/**
 * Joins multiple data sources by their existing order. Make sure the data
 * sources are ordered appropriately!
 * 
 * @author Erich Schubert
 * @since 0.5.0
 */
@Description("Blindly joins multiple data sources, assuming they are ordered the same way.")
public class PresortedBlindJoinDatabaseConnection extends AbstractDatabaseConnection {
  /**
   * Logger
   */
  private static final Logging LOG = Logging.getLogger(PresortedBlindJoinDatabaseConnection.class);

  /**
   * The filters to invoke
   */
  final protected List<DatabaseConnection> sources;

  /**
   * Constructor.
   * 
   * @param filters Filters to use.
   * @param sources Data sources to join.
   */
  public PresortedBlindJoinDatabaseConnection(List<ObjectFilter> filters, List<DatabaseConnection> sources) {
    super(filters);
    this.sources = sources;
  }

  @Override
  public MultipleObjectsBundle loadData() {
    List<MultipleObjectsBundle> bundles = new ArrayList<>(sources.size());
    for(DatabaseConnection dbc : sources) {
      bundles.add(dbc.loadData());
    }

    MultipleObjectsBundle first = bundles.get(0);
    // Process additional columns
    for(int c = 1; c < sources.size(); c++) {
      MultipleObjectsBundle cur = bundles.get(c);
      if(cur.dataLength() != first.dataLength()) {
        throw new AbortException("Data set sizes do not agree - cannot join!");
      }
      for(int i = 0; i < cur.metaLength(); i++) {
        first.appendColumn(cur.meta(i), cur.getColumn(i));
      }
    }

    return invokeBundleFilters(first);
  }

  @Override
  protected Logging getLogger() {
    return LOG;
  }

  /**
   * Parameterization class.
   * 
   * @author Erich Schubert
   */
  public static class Parameterizer extends AbstractDatabaseConnection.Parameterizer {
    /**
     * The static option ID
     */
    public static final OptionID SOURCES_ID = new OptionID("join.sources", "The data sources to join.");

    /**
     * The data souces to use.
     */
    protected List<DatabaseConnection> sources;

    @Override
    protected void makeOptions(Parameterization config) {
      super.makeOptions(config);
      super.configFilters(config);
      final ObjectListParameter<DatabaseConnection> sourcesParam = new ObjectListParameter<>(SOURCES_ID, DatabaseConnection.class);
      if(config.grab(sourcesParam)) {
        sources = sourcesParam.instantiateClasses(config);
      }
    }

    @Override
    protected PresortedBlindJoinDatabaseConnection makeInstance() {
      return new PresortedBlindJoinDatabaseConnection(filters, sources);
    }
  }
}