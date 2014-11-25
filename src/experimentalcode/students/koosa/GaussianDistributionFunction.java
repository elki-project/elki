package experimentalcode.students.koosa;

import java.util.Random;

import de.lmu.ifi.dbs.elki.data.DoubleVector;
import de.lmu.ifi.dbs.elki.data.HyperBoundingBox;
/*
 This file is part of ELKI:
 Environment for Developing KDD-Applications Supported by Index-Structures

 Copyright (C) 2014
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

public class GaussianDistributionFunction implements ProbabilityFunction {

  private double mean;
  private double variance;
  
  // Constructor
  public GaussianDistributionFunction(final double mean, final double variance) {
    this.mean = mean;
    this.variance = variance;
  }
  
  @Override
  public DoubleVector drawValue(HyperBoundingBox mbr, Random rand) {
    double[] values = new double[mbr.getDimensionality()];

    for(int i = 0; i < mbr.getDimensionality(); i++) {
      double randomDraw = mean + rand.nextGaussian() * variance;
      if(randomDraw < mbr.getMin(i) || randomDraw > mbr.getMax(i)) {
        // repeat the draw until there is a valid value
        i--;
      } else {
        values[i] = randomDraw;
      }
    }
    
    return new DoubleVector(values);
  }

}
