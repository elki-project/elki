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
package elki.itemsetmining;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import elki.algorithm.AbstractSimpleAlgorithmTest;
import elki.database.Database;
import elki.datasource.InputStreamDatabaseConnection;
import elki.datasource.parser.SimpleTransactionParser;
import elki.result.FrequentItemsetsResult;
import elki.utilities.ELKIBuilder;
import elki.utilities.optionhandling.parameterization.ListParameterization;

/**
 * Regression test for Eclat.
 *
 * @author Erich Schubert
 * @since 0.7.5
 */
public class EclatTest extends AbstractSimpleAlgorithmTest {
  @Test
  public void testMissing() {
    Database db = makeSimpleDatabase(UNITTEST + "itemsets/missing1.txt", 4, new ListParameterization() //
        .addParameter(InputStreamDatabaseConnection.Par.PARSER_ID, SimpleTransactionParser.class));
    {
      FrequentItemsetsResult res = new ELKIBuilder<>(Eclat.class) //
          .with(Eclat.Par.MINSUPP_ID, 1).build().autorun(db);
      assertEquals("Size not as expected.", 14, res.getItemsets().size());
      for(Itemset i : res.getItemsets()) {
        assertEquals("Bad support", 4 - i.length(), i.getSupport());
      }
    }
    {
      FrequentItemsetsResult res = new ELKIBuilder<>(Eclat.class) //
          .with(Eclat.Par.MINSUPP_ID, 0.5).build().autorun(db);
      assertEquals("Size not as expected.", 10, res.getItemsets().size());
      for(Itemset i : res.getItemsets()) {
        assertEquals("Bad support", 4 - i.length(), i.getSupport());
      }
    }
    {
      FrequentItemsetsResult res = new ELKIBuilder<>(Eclat.class) //
          .with(Eclat.Par.MINSUPP_ID, 3).build().autorun(db);
      assertEquals("Size not as expected.", 4, res.getItemsets().size());
      for(Itemset i : res.getItemsets()) {
        assertEquals("Bad support", 4 - i.length(), i.getSupport());
      }
    }
    {
      FrequentItemsetsResult res = new ELKIBuilder<>(Eclat.class) //
          .with(Eclat.Par.MINSUPP_ID, 4).build().autorun(db);
      assertEquals("Size not as expected.", 0, res.getItemsets().size());
    }
    {
      FrequentItemsetsResult res = new ELKIBuilder<>(Eclat.class) //
          .with(Eclat.Par.MINSUPP_ID, 1) //
          .with(Eclat.Par.MINLENGTH_ID, 2) //
          .with(Eclat.Par.MAXLENGTH_ID, 3) //
          .build().autorun(db);
      assertEquals("Size not as expected.", 10, res.getItemsets().size());
      for(Itemset i : res.getItemsets()) {
        assertEquals("Bad support", 4 - i.length(), i.getSupport());
      }
    }
  }

  @Test
  public void testIncreasing() {
    Database db = makeSimpleDatabase(UNITTEST + "itemsets/increasing.txt", 4, new ListParameterization() //
        .addParameter(InputStreamDatabaseConnection.Par.PARSER_ID, SimpleTransactionParser.class));
    {
      FrequentItemsetsResult res = new ELKIBuilder<>(Eclat.class) //
          .with(Eclat.Par.MINSUPP_ID, 1).build().autorun(db);
      assertEquals("Size not as expected.", 15, res.getItemsets().size());
    }
    {
      FrequentItemsetsResult res = new ELKIBuilder<>(Eclat.class) //
          .with(Eclat.Par.MINSUPP_ID, .5).build().autorun(db);
      assertEquals("Size not as expected.", 7, res.getItemsets().size());
    }
    {
      FrequentItemsetsResult res = new ELKIBuilder<>(Eclat.class) //
          .with(Eclat.Par.MINSUPP_ID, 3).build().autorun(db);
      assertEquals("Size not as expected.", 3, res.getItemsets().size());
    }
    {
      FrequentItemsetsResult res = new ELKIBuilder<>(Eclat.class) //
          .with(Eclat.Par.MINSUPP_ID, 4).build().autorun(db);
      assertEquals("Size not as expected.", 1, res.getItemsets().size());
    }
  }

  @Test
  public void testLarge() {
    Database db = makeSimpleDatabase(UNITTEST + "itemsets/zutaten.txt.gz", 16401, new ListParameterization() //
        .addParameter(InputStreamDatabaseConnection.Par.PARSER_ID, SimpleTransactionParser.class));
    FrequentItemsetsResult res = new ELKIBuilder<>(Eclat.class) //
        .with(Eclat.Par.MINSUPP_ID, 200).build().autorun(db);
    assertEquals("Size not as expected.", 184, res.getItemsets().size());
  }
}
