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
 * Phi Correlation Coefficient interestingness measure.
 * <p>
 * \[ \frac{n P(X \cap Y) - P(X) - P(Y)}{\sqrt{P(X)P(Y)P(\neg X)P(\neg Y)}} \]
 * <p>
 * Reference:
 * <p>
 * A. Agresti<br>
 * Categorical Data Analysis<br>
 * Categorical Data Analysis 1990
 * 
 * @author Abhishek Sharma
 * @since 0.7.5
 */
@Reference(authors = "A. Agresti", //
	title = "Categorical Data Analysis", //
	booktitle = "Categorical Data Analysis 1990", //
	bibkey = "Agresti1990")
public class PhiCorrelationCoefficient implements InterestingnessMeasure {
	/**
	 * Constructor.
	 */
	public PhiCorrelationCoefficient() {
		super();
	}
	
	@Override
	public double measure(int t, int sX, int sY, int sXY) {
	    double pXY = sXY / (double) t;
	    double pX = sX / (double) t;
	    double pY = sY / (double) t;
	    return (pXY - pX * pY) / Math.sqrt(pX * pY * (1 - pX) * (1 - pY));
	}
}