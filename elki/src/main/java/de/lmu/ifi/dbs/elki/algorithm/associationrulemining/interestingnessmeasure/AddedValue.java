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
 * Added value interestingnss measure
 * 
 * Reference:
 * <p>
 * S. Sahar, Sigal, Y. Mansour<br />
 * Empirical evaluation of interest-level criteria<br />
 * AeroSense'99. International Society for Optics and Photonics, 1999
 * </p>
 * 
 * @author Frederic Sautter
 *
 */
@Reference(authors = "S. Sahar, Sigal, Y. Mansour", //
title = "Empirical evaluation of interest-level criteria", //
booktitle = "AeroSense'99. International Society for Optics and Photonics, 1999")
public class AddedValue extends AbstractInterestingnessMeasure {

  public AddedValue() {
    // TODO Auto-generated constructor stub
  }

  @Override
  public double measure(int totalTransactions, int supportX, int supportY, int supportXY) {
    return ((double) supportXY / supportX) - ((double) supportY / totalTransactions);
  }

}
