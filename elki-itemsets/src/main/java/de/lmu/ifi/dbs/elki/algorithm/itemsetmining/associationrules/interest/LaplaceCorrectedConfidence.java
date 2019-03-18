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
 * Laplace Corrected Confidence interestingness measure.
 * <p>
 * \[ \frac{nP(X \cap Y) + 1}{nP(X) + 2} \]
 * <p>
 * Reference:
 * <p>
 * Peter Clark, Robin Boswell<br>
 * Rule induction with CN2: Some recent improvements<br>
 * European Working Session on Learning 1991
 * 
 * @author Abhishek Sharma
 * @since 0.7.5
 */
@Reference(authors = "Peter Clark, Robin Boswell", //
	title = "Rule induction with CN2: Some recent improvements", //
	booktitle = "European Working Session on Learning 1991", //
	bibkey = "ClarkBoswell1991")
public class LaplaceCorrectedConfidence implements InterestingnessMeasure {
	/**
	 * Constructor.
	 */
	public LaplaceCorrectedConfidence() {
		super();
	}
	
	@Override
	public double measure(int t, int sX, int sY, int sXY) {
	    return (sXY + 1) / (double) (sX + 2);
	}
}
