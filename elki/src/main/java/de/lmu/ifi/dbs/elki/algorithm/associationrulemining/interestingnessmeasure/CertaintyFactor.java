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
 * Certainty factor interestingnss measure
 * 
 * Reference:
 * <p>
 * F. Berzal, I. Blanco, M. Vila and others<br />
 * Measuring the accuracy and interest of association rules: A new framework<br />
 * Intelligent Data Analysis, 6(3), 2002
 * </p>
 * 
 * @author Frederic Sautter
 *
 */
@Reference(authors = "F. Berzal, I. Blanco, M. Vila and others", //
title = "Measuring the accuracy and interest of association rules: A new framework", //
booktitle = "Intelligent Data Analysis, 6(3), 2002")
public class CertaintyFactor extends AbstractInterestingnessMeasure {

  public CertaintyFactor() {
    // TODO Auto-generated constructor stub
  }

  @Override
  public double measure(int totalTransactions, int supportX, int supportY, int supportXY) {
    double dividend = ((double) supportXY / supportX) - ((double) supportY / totalTransactions);
    double divisor = (double) (totalTransactions - supportY) / totalTransactions;
    return dividend / divisor;
  }

}
