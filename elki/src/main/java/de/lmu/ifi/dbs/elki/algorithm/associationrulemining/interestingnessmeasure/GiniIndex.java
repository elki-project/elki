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

/**
 * Gini index interestingnss measure
 * 
 * @author Frederic Sautter
 *
 */
public class GiniIndex extends AbstractInterestingnessMeasure {

  public GiniIndex() {
    // TODO Auto-generated constructor stub
  }
  
  @Override
  public double measure(int totalTransactions, int supportX, int supportY, int supportXY) {
    double pX = (double) supportX / totalTransactions;
    double pYlX = (double) supportXY / supportX;
    double pnotYlX = (double) (supportX - supportXY) / supportX;
    double pnotX = (double) (totalTransactions - supportX) / totalTransactions;
    double pYlnotX = (double) (supportY - supportXY) / (totalTransactions - supportX);
    double pnotYlnotX = (double) ((totalTransactions - supportX) - (supportY - supportXY)) / (totalTransactions - supportX);
    double pY = (double) supportY / totalTransactions;
    double pnotY = (double) (totalTransactions - supportY) / totalTransactions;
    return pX * (Math.pow(pYlX, 2) + Math.pow(pnotYlX, 2)) + pnotX * (Math.pow(pYlnotX, 2) + Math.pow(pnotYlnotX, 2)) - Math.pow(pY, 2) - Math.pow(pnotY, 2);
  }

}
