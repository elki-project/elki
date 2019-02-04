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

import org.junit.Test;

import de.lmu.ifi.dbs.elki.database.Database;
import de.lmu.ifi.dbs.elki.result.FrequentItemsetsResult;
import de.lmu.ifi.dbs.elki.utilities.ELKIBuilder;

/**
 * Regression test for Eclat.
 *
 * @author Erich Schubert
 * @since 0.7.5
 */
public class EclatTest extends AbstractFrequentItemsetAlgorithmTest {
  @Test
  public void testMissing() {
    Database db = loadTransactions(UNITTEST + "itemsets/missing1.txt", 4);
    {
      FrequentItemsetsResult res = new ELKIBuilder<>(Eclat.class) //
          .with(Eclat.Parameterizer.MINSUPP_ID, 1).build().run(db);
      assertEquals("Size not as expected.", 14, res.getItemsets().size());
      for(Itemset i : res.getItemsets()) {
        assertEquals("Bad support", 4 - i.length(), i.getSupport());
      }
    }
    {
      FrequentItemsetsResult res = new ELKIBuilder<>(Eclat.class) //
          .with(Eclat.Parameterizer.MINSUPP_ID, 0.5).build().run(db);
      assertEquals("Size not as expected.", 10, res.getItemsets().size());
      for(Itemset i : res.getItemsets()) {
        assertEquals("Bad support", 4 - i.length(), i.getSupport());
      }
    }
    {
      FrequentItemsetsResult res = new ELKIBuilder<>(Eclat.class) //
          .with(Eclat.Parameterizer.MINSUPP_ID, 3).build().run(db);
      assertEquals("Size not as expected.", 4, res.getItemsets().size());
      for(Itemset i : res.getItemsets()) {
        assertEquals("Bad support", 4 - i.length(), i.getSupport());
      }
    }
    {
      FrequentItemsetsResult res = new ELKIBuilder<>(Eclat.class) //
          .with(Eclat.Parameterizer.MINSUPP_ID, 4).build().run(db);
      assertEquals("Size not as expected.", 0, res.getItemsets().size());
    }
    {
      FrequentItemsetsResult res = new ELKIBuilder<>(Eclat.class) //
          .with(Eclat.Parameterizer.MINSUPP_ID, 1) //
          .with(Eclat.Parameterizer.MINLENGTH_ID, 2) //
          .with(Eclat.Parameterizer.MAXLENGTH_ID, 3) //
          .build().run(db);
      assertEquals("Size not as expected.", 10, res.getItemsets().size());
      for(Itemset i : res.getItemsets()) {
        assertEquals("Bad support", 4 - i.length(), i.getSupport());
      }
    }
  }

  @Test
  public void testIncreasing() {
    Database db = loadTransactions(UNITTEST + "itemsets/increasing.txt", 4);
    {
      FrequentItemsetsResult res = new ELKIBuilder<>(Eclat.class) //
          .with(Eclat.Parameterizer.MINSUPP_ID, 1).build().run(db);
      assertEquals("Size not as expected.", 15, res.getItemsets().size());
    }
    {
      FrequentItemsetsResult res = new ELKIBuilder<>(Eclat.class) //
          .with(Eclat.Parameterizer.MINSUPP_ID, .5).build().run(db);
      assertEquals("Size not as expected.", 7, res.getItemsets().size());
    }
    {
      FrequentItemsetsResult res = new ELKIBuilder<>(Eclat.class) //
          .with(Eclat.Parameterizer.MINSUPP_ID, 3).build().run(db);
      assertEquals("Size not as expected.", 3, res.getItemsets().size());
    }
    {
      FrequentItemsetsResult res = new ELKIBuilder<>(Eclat.class) //
          .with(Eclat.Parameterizer.MINSUPP_ID, 4).build().run(db);
      assertEquals("Size not as expected.", 1, res.getItemsets().size());
    }
  }

  @Test
  public void testLarge() {
    Database db = loadTransactions(UNITTEST + "itemsets/zutaten.txt.gz", 16401);
    FrequentItemsetsResult res = new ELKIBuilder<>(Eclat.class) //
        .with(Eclat.Parameterizer.MINSUPP_ID, 200).build().run(db);
    assertEquals("Size not as expected.", 184, res.getItemsets().size());
  }
}
