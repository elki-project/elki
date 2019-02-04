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
package de.lmu.ifi.dbs.elki.algorithm.itemsetmining;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import de.lmu.ifi.dbs.elki.algorithm.AbstractSimpleAlgorithmTest;
import de.lmu.ifi.dbs.elki.data.type.TypeUtil;
import de.lmu.ifi.dbs.elki.database.AbstractDatabase;
import de.lmu.ifi.dbs.elki.database.Database;
import de.lmu.ifi.dbs.elki.database.StaticArrayDatabase;
import de.lmu.ifi.dbs.elki.database.relation.Relation;
import de.lmu.ifi.dbs.elki.datasource.InputStreamDatabaseConnection;
import de.lmu.ifi.dbs.elki.datasource.filter.FixedDBIDsFilter;
import de.lmu.ifi.dbs.elki.datasource.filter.ObjectFilter;
import de.lmu.ifi.dbs.elki.datasource.parser.CSVReaderFormat;
import de.lmu.ifi.dbs.elki.datasource.parser.SimpleTransactionParser;
import de.lmu.ifi.dbs.elki.utilities.ELKIBuilder;

/**
 * Regression test for APRIORI.
 *
 * @author Erich Schubert
 * @since 0.7.5
 */
public abstract class AbstractFrequentItemsetAlgorithmTest extends AbstractSimpleAlgorithmTest {
  /**
   * Load a transaction database.
   * 
   * @param filename Filename
   * @param expectedSize Expected size
   * @return Database
   */
  public static <T> Database loadTransactions(String filename, int expectedSize) {
    // Allow loading test data from resources.
    try (InputStream is = open(filename)) {
      // Instantiate filters manually. TODO: redesign
      List<ObjectFilter> filterlist = new ArrayList<>();
      filterlist.add(new FixedDBIDsFilter(1));
      // Setup parser and data loading
      SimpleTransactionParser parser = new SimpleTransactionParser(CSVReaderFormat.DEFAULT_FORMAT);
      InputStreamDatabaseConnection dbc = new InputStreamDatabaseConnection(is, filterlist, parser);

      // We want to allow the use of indexes via "params"
      Database db = new ELKIBuilder<>(StaticArrayDatabase.class) //
          .with(AbstractDatabase.Parameterizer.DATABASE_CONNECTION_ID, dbc).build();

      db.initialize();
      Relation<?> rel = db.getRelation(TypeUtil.ANY);
      if(expectedSize > 0) {
        assertEquals("Database size does not match.", expectedSize, rel.size());
      }
      return db;
    }
    catch(IOException e) {
      fail("Test data " + filename + " not found.");
      return null; // Not reached.
    }
  }
}
