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
package elki.itemsetmining.associationrules.interest;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import elki.itemsetmining.AbstractFrequentItemsetAlgorithmTest;
import elki.itemsetmining.FPGrowth;
import elki.itemsetmining.associationrules.AssociationRuleGeneration;
import elki.database.Database;
import elki.result.AssociationRuleResult;
import elki.utilities.ELKIBuilder;

/**
 * Unit test for the J-measure metric.
 * 
 * @author Erich Schubert
 * @since 0.7.5
 */
public class JMeasureTest extends AbstractFrequentItemsetAlgorithmTest {
  @Test
  public void testToyExample() {
    Database db = loadTransactions(UNITTEST + "itemsets/increasing5.txt", 5);
    AssociationRuleResult res = new ELKIBuilder<>(AssociationRuleGeneration.class) //
        .with(FPGrowth.Par.MINSUPP_ID, 2) //
        .with(AssociationRuleGeneration.Par.MINMEASURE_ID, 0.1) //
        .with(AssociationRuleGeneration.Par.INTERESTMEASURE_ID, JMeasure.class) //
        .build().run(db);
    assertEquals("Size not as expected.", 9, res.getRules().size());
  }
}
