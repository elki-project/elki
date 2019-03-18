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
 * Yule's Q interestingness measure.
 * <p>
 * \[ \frac{P(X \cap Y) P(\neg X \cap \neg Y) - P(X \cap \neg Y) P(\neg X \cap Y)}{P(X \cap Y)
 *  P(\neg X \cap \neg Y) + P(X \cap \neg Y) P(\neg X \cap Y)}
 *   = \frac{\alpha - 1}{\alpha + 1} \]
 * <p>
 * Reference:
 * <p>
 * G.U. Yule<br>
 * On the association of attributes in statistics<br>
 * Philosophical Transactions of the Royal Society A 1900
 * 
 * @author Abhishek Sharma
 * @since 0.7.5
 */
@Reference(authors = "G.U. Yule", //
	title = "On the association of attributes in statistics", //
	booktitle = "Philosophical Transactions of the Royal Society A 1900", //
	bibkey = "Yule1900")
public class YulesQ implements InterestingnessMeasure {
	private OddsRatio oddsRatio;
	/**
	 * Constructor.
	 */
	public YulesQ() {
		super();
		this.oddsRatio = new OddsRatio();
	}
	
	@Override
	public double measure(int t, int sX, int sY, int sXY) {
	    double alpha = this.oddsRatio.measure(t, sX, sY, sXY);
	    return (alpha - 1) / (alpha + 1);
	}
}
