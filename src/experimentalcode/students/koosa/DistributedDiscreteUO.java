package experimentalcode.students.koosa;

import java.util.Arrays;
import java.util.List;

import de.lmu.ifi.dbs.elki.data.DoubleVector;
import de.lmu.ifi.dbs.elki.data.HyperBoundingBox;
import de.lmu.ifi.dbs.elki.math.random.RandomFactory;
import de.lmu.ifi.dbs.elki.utilities.pairs.Pair;
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

public class DistributedDiscreteUO extends AbstractDiscreteUncertainObject<List<Pair<DoubleVector,Double>>> {

  private int totalProbability;
  private static DoubleVector noObjectChosen;
  
  static {
    double[] values = {Double.NEGATIVE_INFINITY};
    noObjectChosen = new DoubleVector(values);
  }
  
  // Constructor
  public DistributedDiscreteUO(final List<Pair<DoubleVector,Double>> samplePoints) {
    this(samplePoints, new RandomFactory(null));
  }
  
  // Constructor
  public DistributedDiscreteUO(final List<Pair<DoubleVector,Double>> samplePoints, final RandomFactory randomFactory) {
    double check = 0;
    for(Pair<DoubleVector,Double> pair: samplePoints) {
      if(pair.getSecond() < 0) {
        throw new IllegalArgumentException("[W: ]\tA probability less than 0 is not possible.");
      }
      check += pair.getSecond();
    }
    
    // User of this class should think of a way to handle possible exception at this point
    // to not find their program crashing without need.
    // To avoid misunderstanding one could compile a ".*total of 1\.$"-like pattern against
    // raised IllegalArgumentExceptions and thereby customize his handle for this case.
    if(check > 1) {
      throw new IllegalArgumentException("[W: ]\tThe sum of probabilities exceeded a total of 1.");
    }
    this.samplePoints = samplePoints;
    // I normalize the totalProbability with 10000, to allow 
    // up to 2 decimal places for the probability.
    this.totalProbability = (int) Math.ceil(check * 10000);
    this.dimensions = samplePoints.get(0).getFirst().getDimensionality();
    this.rand = randomFactory.getRandom();
  }
  
  // note that the user has to be certain, he looks upon the
  // correct Point in the list
  @Override
  public double getSampleProbability(final int position) {
    return samplePoints.get(position).getSecond();
  }

  @Override
  public DoubleVector drawSample() {
    // Due to nextInt(n)'s exclusion of n
    // we have an exact range of 10000 for
    // our possible draws by starting at
    // sum == 0.
    final int index = rand.nextInt(10000);
    int sum = 0;
    int i = 0;
    
    do {
      sum += samplePoints.get(i++).getSecond() * 10000;
    } while(sum < index && sum < totalProbability);
    
    if(sum > totalProbability) {
      return noObjectChosen;
    }
    // Due to incrementation of i in the while-loop
    // we have to decrement it here for a correct (and in
    // edge-cases even valid) draw.
    return samplePoints.get(--i).getFirst();
  }
  
  public void addSamplePoint(final Pair<DoubleVector,Double> samplePoint) {
    this.samplePoints.add(samplePoint);
    double check = totalProbability/10000.0;
    if(check + samplePoint.getSecond() > 1) {
      throw new IllegalArgumentException("[W: ]\tThe new sum of probabilities exceeded a total of 1.");
    }
    this.totalProbability = (int) check * 10000;
    setBounds();
  }
  
  protected void setBounds() {
    double min[] = new double[dimensions];
    Arrays.fill(min, Double.MAX_VALUE);
    double max[] = new double[dimensions];
    Arrays.fill(max, -Double.MAX_VALUE);
    for(Pair<DoubleVector,Double> samplePoint: samplePoints){
      for(int d = 0; d < dimensions; d++){
        min[d] = Math.min(min[d], samplePoint.getFirst().doubleValue(d));
        max[d] = Math.max(max[d], samplePoint.getFirst().doubleValue(d));
      }
    }
    this.bounds = new HyperBoundingBox(min, max);
  }
  
  @Override
  public int getWeight() {
    return this.samplePoints.size();
  }
}