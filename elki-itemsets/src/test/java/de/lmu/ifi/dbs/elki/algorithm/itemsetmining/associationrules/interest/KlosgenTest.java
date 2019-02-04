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
package de.lmu.ifi.dbs.elki.algorithm.itemsetmining.associationrules.interest;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import de.lmu.ifi.dbs.elki.algorithm.itemsetmining.AbstractFrequentItemsetAlgorithmTest;
import de.lmu.ifi.dbs.elki.algorithm.itemsetmining.FPGrowth;
import de.lmu.ifi.dbs.elki.algorithm.itemsetmining.associationrules.AssociationRuleGeneration;
import de.lmu.ifi.dbs.elki.database.Database;
import de.lmu.ifi.dbs.elki.result.AssociationRuleResult;
import de.lmu.ifi.dbs.elki.utilities.ELKIBuilder;

/**
 * Unit test for the Klosgen metric.
 * 
 * @author Erich Schubert
 * @since 0.7.5
 */
public class KlosgenTest extends AbstractFrequentItemsetAlgorithmTest {
  @Test
  public void testToyExample() {
    Database db = loadTransactions(UNITTEST + "itemsets/increasing5.txt", 5);
    AssociationRuleResult res = new ELKIBuilder<>(AssociationRuleGeneration.class) //
        .with(FPGrowth.Parameterizer.MINSUPP_ID, 1) //
        .with(AssociationRuleGeneration.Parameterizer.MINMEASURE_ID, 0.26) //
        .with(AssociationRuleGeneration.Parameterizer.INTERESTMEASURE_ID, Klosgen.class) //
        .build().run(db);
    assertEquals("Size not as expected.", 27, res.getRules().size());
  }
}
