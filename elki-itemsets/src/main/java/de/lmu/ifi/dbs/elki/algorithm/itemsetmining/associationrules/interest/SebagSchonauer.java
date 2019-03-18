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

import de.lmu.ifi.dbs.elki.utilities.documentation.Reference;

/**
 * Sebag Schonauer interestingness measure.
 * <p>
 * \[ \frac{P(X \cap Y)}{P(X \cap \neg Y)} \]
 * <p>
 * Reference:
 * <p>
 * M. Sebag, M. Schoenauer<br>
 * Generation of rules with certainty and confidence factors from incomplete and incoherent learning bases<br>
 * In Proceedings of the European Knowledge Acquisition Workshop (EKAW'88)
 * 
 * @author Abhishek Sharma
 * @since 0.7.5
 */
@Reference(authors = "Peter Clark, Robin Boswell", //
	title = "Generation of rules with certainty and confidence factors from incomplete and incoherent learning bases", //
	booktitle = "In Proceedings of the European Knowledge Acquisition Workshop (EKAW'88)", //
	bibkey = "SebagSchonauer1988")
public class SebagSchonauer implements InterestingnessMeasure {
	/**
	 * Constructor.
	 */
	public SebagSchonauer() {
		super();
	}
	
	@Override
	public double measure(int t, int sX, int sY, int sXY) {
	    return sXY / (double) (sX - sXY);
	}
}
