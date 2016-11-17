package de.lmu.ifi.dbs.elki.algorithm.associationrulemining.interestingnessmeasure;
/*
 This file is part of ELKI:
 Environment for Developing KDD-Applications Supported by Index-Structures

 Copyright (C) 2016
 Ludwig-Maximilians-Universität München
 Lehr- und Forschungseinheit für Datenbanksysteme
 ELKI Development Team

 This program is free software: you can redistribute it and/or modify
 it under the terms of the GNU Affero General Public License as published by
 the Free Software Foundation, either version 3 of the License, or
 (at your option) any later version.

 This program is distributed in the hope that it will be useful,
 but WITHOUT ANY WARRANTY; without even the implied warranty of
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 GNU Affero General Public License for more details.

 You should have received a copy of the GNU Affero General Public License
 along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

import de.lmu.ifi.dbs.elki.utilities.documentation.Reference;

/**
 * Conviction interestingnss measure
 * 
 * Reference:
 * <p>
 * S. Brin, R. Motwani, J. D Ullman, and S Tsur<br />
 * Dynamic itemset counting and implication rules for market basket data<br />
 * In ACM SIGMOD Record, volume 26, ACM, 1997
 * </p>
 * 
 * @author Frederic Sautter
 *
 */
@Reference(authors = "S. Brin, R. Motwani, J. D Ullman, and S Tsur", //
title = "Dynamic itemset counting and implication rules for market basket data", //
booktitle = "ACM SIGMOD Record, volume 26, ACM, 1997")
public class Conviction extends AbstractInterestingnessMeasure {

  public Conviction() {
    // TODO Auto-generated constructor stub
  }

  @Override
  public double measure(int totalTransactions, int supportX, int supportY, int supportXY) {
    return (double) (supportX * (totalTransactions - supportY)) / (totalTransactions * (supportX - supportXY));
  }

}
