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
 * Yule's Y interestingness measure.
 * <p>
 * \[ \frac{\sqrt{P(X \cap Y) P(\neg X \cap \neg Y)} - \sqrt{P(X \cap \neg Y) 
 * 		P(\neg X \cap Y)}}{\sqrt{P(X \cap Y) P(\neg X \cap \neg Y)} + 
 * 		\sqrt{P(X \cap \neg Y) P(\neg X \cap Y)}} = \frac{\sqrt{\alpha} - 1}{\sqrt{\alpha} + 1} \]
 * <p>
 * Reference:
 * <p>
 * G.U. Yule<br>
 * On the methods of measuring association between two attributes<br>
 * Journal of the Royal Statistical Society 1912
 * 
 * @author Abhishek Sharma
 * @since 0.7.5
 */
@Reference(authors = "G.U. Yule", //
	title = "On the methods of measuring association between two attributes", //
	booktitle = "Journal of the Royal Statistical Society 1912", //
	bibkey = "Yule1912")
public class YulesY implements InterestingnessMeasure {
	private OddsRatio oddsRatio;
	/**
	 * Constructor.
	 */
	public YulesY() {
		super();
		this.oddsRatio = new OddsRatio();
	}
	
	@Override
	public double measure(int t, int sX, int sY, int sXY) {
	    double alpha = this.oddsRatio.measure(t, sX, sY, sXY);
	    return (Math.sqrt(alpha) - 1) / (Math.sqrt(alpha) + 1);
	}
}
