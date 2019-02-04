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

import de.lmu.ifi.dbs.elki.data.ExternalID;
import de.lmu.ifi.dbs.elki.data.type.TypeUtil;
import de.lmu.ifi.dbs.elki.datasource.bundle.MultipleObjectsBundle;
import de.lmu.ifi.dbs.elki.datasource.filter.ObjectFilter;
import de.lmu.ifi.dbs.elki.logging.Logging;
import de.lmu.ifi.dbs.elki.utilities.exceptions.AbortException;
import de.lmu.ifi.dbs.elki.utilities.io.FormatUtil;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.OptionID;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.Parameterization;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.ObjectListParameter;

import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;

/**
 * Joins multiple data sources by their label
 *
 * @author Erich Schubert
 * @since 0.4.0
 *
 * @assoc - - - ExternalID
 */
public class ExternalIDJoinDatabaseConnection extends AbstractDatabaseConnection {
  /**
   * Logger
   */
  private static final Logging LOG = Logging.getLogger(ExternalIDJoinDatabaseConnection.class);

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
  public ExternalIDJoinDatabaseConnection(List<ObjectFilter> filters, List<DatabaseConnection> sources) {
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
    Object2IntOpenHashMap<ExternalID> labelmap = new Object2IntOpenHashMap<>(first.dataLength());
    labelmap.defaultReturnValue(-1);
    // Process first bundle
    {
      // Identify a label column
      final int lblcol;
      {
        int lblc = -1;
        for(int i = 0; i < first.metaLength(); i++) {
          if(TypeUtil.EXTERNALID.isAssignableFromType(first.meta(i))) {
            lblc = i;
            break;
          }
        }
        lblcol = lblc; // make static
      }
      if(lblcol == -1) {
        throw new AbortException("No external ID column found in primary source.");
      }
      for(int i = 0; i < first.dataLength(); i++) {
        ExternalID data = (ExternalID) first.data(i, lblcol);
        if(data == null) {
          LOG.debug("Object without ID encountered.");
          continue;
        }
        int old = labelmap.put(data, i);
        if(old != -1) {
          LOG.debug("Duplicate id encountered: " + data + " in rows " + old + " and " + i);
        }
      }
    }
    // Process additional columns
    for(int c = 1; c < sources.size(); c++) {
      MultipleObjectsBundle cur = bundles.get(c);
      final int lblcol;
      {
        int lblc = -1;
        for(int i = 0; i < cur.metaLength(); i++) {
          if(TypeUtil.EXTERNALID.isAssignableFromType(cur.meta(i))) {
            lblc = i;
            break;
          }
        }
        lblcol = lblc; // make static
      }
      if(lblcol == -1) {
        StringBuilder buf = new StringBuilder();
        for(int i = 0; i < cur.metaLength(); i++) {
          if(buf.length() > 0) {
            buf.append(',');
          }
          buf.append(cur.meta(i));
        }
        throw new AbortException("No external ID column found in source " + (c + 1) + " to join with. Got: " + buf.toString());
      }
      // Destination columns
      List<ArrayList<Object>> dcol = new ArrayList<>(cur.metaLength());
      for(int i = 0; i < cur.metaLength(); i++) {
        // Skip the label columns
        if(i == lblcol) {
          dcol.add(null);
          continue;
        }
        ArrayList<Object> newcol = new ArrayList<>(first.dataLength());
        // Pre-fill with nulls.
        for(int j = 0; j < first.dataLength(); j++) {
          newcol.add(null);
        }
        first.appendColumn(cur.meta(i), newcol);
        dcol.add(newcol);
      }
      for(int i = 0; i < cur.dataLength(); i++) {
        ExternalID data = (ExternalID) cur.data(i, lblcol);
        if(data == null) {
          LOG.warning("Object without label encountered.");
          continue;
        }
        int row = labelmap.getInt(data);
        if(row == -1) {
          LOG.debug("ID not found for join: " + data + " in row " + i);
          continue;
        }
        for(int d = 0; d < cur.metaLength(); d++) {
          if(d == lblcol) {
            continue;
          }
          List<Object> col = dcol.get(d);
          assert (col != null);
          col.set(row, cur.data(i, d));
        }
      }
    }
    for(int i = 0; i < first.dataLength(); i++) {
      for(int d = 0; d < first.metaLength(); d++) {
        if(first.data(i, d) == null) {
          StringBuilder buf = new StringBuilder();
          for(int d2 = 0; d2 < first.metaLength(); d2++) {
            if(buf.length() > 0) {
              buf.append(", ");
            }
            if(first.data(i, d2) == null) {
              buf.append("null");
            }
            else {
              buf.append(first.data(i, d2));
            }
          }
          LOG.warning("null value in joined data, row " + i + " column " + d + FormatUtil.NEWLINE + "[" + buf.toString() + "]");
          break;
        }
      }
    }

    return first;
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
    protected ExternalIDJoinDatabaseConnection makeInstance() {
      return new ExternalIDJoinDatabaseConnection(filters, sources);
    }
  }
}
