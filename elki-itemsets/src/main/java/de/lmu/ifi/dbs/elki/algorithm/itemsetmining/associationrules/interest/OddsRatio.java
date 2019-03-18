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
 * Odds Ratio interestingness measure.
 * <p>
 * \[ \frac{P(X \cap Y) P(\neg X \cap \neg Y)}{P(X \cap \neg Y) P(\neg X \cap Y)} \]
 * <p>
 * Reference:
 * <p>
 * Frederick Mosteller<br>
 * Association and Estimation in Contingency Tables<br>
 * Journal of the American Statistical Association 1968
 * 
 * @author Abhishek Sharma
 * @since 0.7.5
 */
@Reference(authors = "Frederick Mosteller", //
	title = "Association and Estimation in Contingency Tables", //
	booktitle = "Journal of the American Statistical Association 1968", //
	bibkey = "Mosteller1968")
public class OddsRatio implements InterestingnessMeasure {
	/**
	 * Constructor.
	 */
	public OddsRatio() {
		super();
	}
	
	@Override
	public double measure(int t, int sX, int sY, int sXY) {
	    double pXY = sXY / (double) t;
	    double pXnotY = (sX - sXY) / (double) t;
	    double pnotXY = (sY - sXY) / (double) t;
	    double pnotXnotY = 1 - ((sX + sY - sXY) / (double) t);
	    return (pXY * pnotXnotY) / (pXnotY * pnotXY);
	}
}
