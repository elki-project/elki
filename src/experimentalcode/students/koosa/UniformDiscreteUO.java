package experimentalcode.students.koosa;

import java.util.Arrays;
import java.util.List;

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

public class UniformDiscreteUO extends AbstractDiscreteUncertainObject<List<DoubleVector>> {

  private double sampleProbability;

  // Constructor
  public UniformDiscreteUO (final List<DoubleVector> samplePoints) {
    this.samplePoints = samplePoints;
    this.dimensions = samplePoints.get(0).getDimensionality();
  }
  
  @Override
  public double getSampleProbability(final int position) {
    // parameter may be ignored due to uniform distribution
    return this.sampleProbability;
  }

  @Override
  public DoubleVector drawSample() {
    // Since the probability is the same for each samplePoint and
    // precisely 1:samplePoints.size(), it should be fair enough
    // to simply draw a sample by returning the point at 
    // Index := random.mod(samplePoints.size())
    return samplePoints.get(rand.nextInt() % samplePoints.size());
  }
  
  public void addSamplePoint(final DoubleVector samplePoint) {
    this.samplePoints.add(samplePoint);
    this.sampleProbability = sampleProbability/(samplePoints.size() - 1) * samplePoints.size();
    setMBR();
  }
  
  protected void setMBR() {
    double min[] = new double[dimensions];
    Arrays.fill(min, Double.MAX_VALUE);
    double max[] = new double[dimensions];
    Arrays.fill(max, -Double.MAX_VALUE);
    for(DoubleVector samplePoint: samplePoints){
      for(int d = 0; d < dimensions; d++){
        min[d] = Math.min(min[d], samplePoint.doubleValue(d));
        max[d] = Math.max(max[d], samplePoint.doubleValue(d));
      }
    }
    this.mbr = new HyperBoundingBox(min, max);
  }
}