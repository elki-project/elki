package de.lmu.ifi.dbs.elki.data.uncertain;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

import de.lmu.ifi.dbs.elki.data.DoubleVector;
import de.lmu.ifi.dbs.elki.data.HyperBoundingBox;
import de.lmu.ifi.dbs.elki.data.NumberVector;
import de.lmu.ifi.dbs.elki.math.random.RandomFactory;

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

/**
 * This class is derived from {@link AbstractDiscreteUncertainObject} and models
 * Discrete-Uncertain-Data-Objects with a uniform distribution of their values
 * probabilities, i.e. every possible value has the same probability to be drawn
 * and they sum up to 1.
 *
 * @author Alexander Koos
 *
 */
public class UniformDiscreteUO extends AbstractDiscreteUncertainObject<List<DoubleVector>> {
  private double sampleProbability;

  private double minMin;

  private double maxMin;

  private double minMax;

  private double maxMax;

  private long multMin;

  private long multMax;

  private Random drand;

  // Constructor for uncertainifyFilter-use
  //
  // This one is basically constructing a Factory,
  // one could argue that it would be better practice
  // to actually implement such a factory, but for
  // the time being I'll stick with this kind of
  // under engineered approach, until everything is
  // fine and I can give more priority to beauty
  // than to functionality.
  public UniformDiscreteUO(final double minMin, final double maxMin, final double minMax, final double maxMax, final long multMin, final long multMax, final long distributionSeed, final RandomFactory randFac) {
    this.minMin = minMin;
    this.maxMin = maxMin;
    this.minMax = minMax;
    this.maxMax = maxMax;
    this.multMin = multMin;
    this.multMax = multMax;
    this.rand = randFac.getRandom();
    this.drand = (new RandomFactory(distributionSeed)).getRandom();
  }

  // Constructor
  public UniformDiscreteUO(final List<DoubleVector> samplePoints) {
    this(samplePoints, new RandomFactory(null));
  }

  // Constructor
  public UniformDiscreteUO(final List<DoubleVector> samplePoints, final RandomFactory randomFactory) {
    this.samplePoints = samplePoints;
    this.dimensions = samplePoints.get(0).getDimensionality();
    this.rand = randomFactory.getRandom();
    this.setBounds();
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
    return this.samplePoints.get(this.rand.nextInt(this.samplePoints.size()));
  }

  public void addSamplePoint(final DoubleVector samplePoint) {
    this.samplePoints.add(samplePoint);
    this.sampleProbability = this.sampleProbability * (this.samplePoints.size() - 1) / this.samplePoints.size();
    this.setBounds();
  }

  protected void setBounds() {
    final double min[] = new double[this.dimensions];
    Arrays.fill(min, Double.MAX_VALUE);
    final double max[] = new double[this.dimensions];
    Arrays.fill(max, -Double.MAX_VALUE);
    for(final DoubleVector samplePoint : this.samplePoints) {
      for(int d = 0; d < this.dimensions; d++) {
        min[d] = Math.min(min[d], samplePoint.doubleValue(d));
        max[d] = Math.max(max[d], samplePoint.doubleValue(d));
      }
    }
    this.bounds = new HyperBoundingBox(min, max);
  }

  @Override
  public UncertainObject<UOModel> uncertainify(final NumberVector vec, final boolean blur, final boolean uncertainify, final int dims) {
    final List<DoubleVector> sampleList = new ArrayList<DoubleVector>();
    if(uncertainify) {
      final int genuine = this.drand.nextInt(vec.getDimensionality());
      final double difMin = this.drand.nextDouble() * (this.maxMin - this.minMin) + this.minMin;
      final double difMax = this.drand.nextDouble() * (this.maxMax - this.minMax) + this.minMax;
      final double randDev = blur ? (this.drand.nextInt(2) == 0 ? this.drand.nextDouble() * -difMin : this.drand.nextDouble() * difMax) : 0;
      final int distributionSize = this.drand.nextInt((int) (this.multMax - this.multMin) + 1) + (int) this.multMin;
      for(int i = 0; i < distributionSize; i++) {
        if(i == genuine) {
          sampleList.add(new DoubleVector(vec.getColumnVector()));
          continue;
        }
        final double[] svec = new double[vec.getDimensionality()];
        for(int j = 0; j < vec.getDimensionality(); j++) {
          final double gtv = vec.doubleValue(j);
          svec[j] = gtv + this.drand.nextDouble() * difMax - this.drand.nextDouble() * difMin + randDev;
        }
        sampleList.add(new DoubleVector(svec));
      }
    }
    else {
      final double[] val = new double[dims];
      for(int i = 0; i < vec.getDimensionality(); i++) {
        val[i % dims] = vec.doubleValue(i);
        if(i % dims == dims - 1) {
          sampleList.add(new DoubleVector(val));
        }
      }
    }
    return new UncertainObject<UOModel>(new UniformDiscreteUO(sampleList, new RandomFactory(this.drand.nextLong())), new DoubleVector(vec.getColumnVector()));
  }

  public static class Parameterizer extends DistributedDiscreteUO.Parameterizer {
    @Override
    protected UniformDiscreteUO makeInstance() {
      return new UniformDiscreteUO(this.minMin, this.maxMin, this.minMax, this.maxMax, this.multMin, this.multMax, this.distributionSeed, this.randFac);
    }
  }

  @Override
  public DoubleVector getAnker() {
    final double[] references = new double[this.getDimensionality()];
    for(final DoubleVector vec : this.samplePoints) {
      for(int i = 0; i < this.getDimensionality(); i++) {
        references[i] += vec.doubleValue(i);
      }
    }
    for(int i = 0; i < references.length; i++) {
      references[i] /= this.samplePoints.size();
    }
    return new DoubleVector(references);
  }
}