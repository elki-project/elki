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
package elki.itemsetmining.associationrules.interest;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import elki.algorithm.AbstractSimpleAlgorithmTest;
import elki.database.Database;
import elki.datasource.InputStreamDatabaseConnection;
import elki.datasource.parser.SimpleTransactionParser;
import elki.itemsetmining.FPGrowth;
import elki.itemsetmining.associationrules.AssociationRuleGeneration;
import elki.result.AssociationRuleResult;
import elki.utilities.ELKIBuilder;
import elki.utilities.optionhandling.parameterization.ListParameterization;

/**
 * Unit test for the Laplace Corrected Confidence metric.
 * 
 * @author Abhishek Sharma
 * @since 0.7.5
 */
public class LaplaceCorrectedConfidenceTest extends AbstractSimpleAlgorithmTest {
  @Test
  public void testToyExample() {
    Database db = makeSimpleDatabase(UNITTEST + "itemsets/subsets3.txt", 7, new ListParameterization() //
        .addParameter(InputStreamDatabaseConnection.Par.PARSER_ID, SimpleTransactionParser.class));
    AssociationRuleResult res = new ELKIBuilder<>(AssociationRuleGeneration.class) //
        .with(FPGrowth.Par.MINSUPP_ID, 1) //
        .with(AssociationRuleGeneration.Par.MINMEASURE_ID, 0.5) //
        .with(AssociationRuleGeneration.Par.INTERESTMEASURE_ID, LaplaceCorrectedConfidence.class) //
        .build().autorun(db);
    assertEquals("Size not as expected.", 9, res.getRules().size());
  }
}
