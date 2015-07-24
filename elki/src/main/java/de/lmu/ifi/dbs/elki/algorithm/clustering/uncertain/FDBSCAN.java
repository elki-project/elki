package de.lmu.ifi.dbs.elki.algorithm.clustering.uncertain;

import java.util.List;

import de.lmu.ifi.dbs.elki.algorithm.clustering.DBSCAN;
import de.lmu.ifi.dbs.elki.data.DoubleVector;
import de.lmu.ifi.dbs.elki.data.NumberVector;
import de.lmu.ifi.dbs.elki.data.type.SimpleTypeInformation;
import de.lmu.ifi.dbs.elki.data.type.TypeInformation;
import de.lmu.ifi.dbs.elki.data.type.TypeUtil;
import de.lmu.ifi.dbs.elki.data.uncertain.UOModel;
import de.lmu.ifi.dbs.elki.data.uncertain.UncertainObject;
import de.lmu.ifi.dbs.elki.data.uncertain.UniformDiscreteUO;
import de.lmu.ifi.dbs.elki.database.ids.DBIDIter;
import de.lmu.ifi.dbs.elki.database.ids.DBIDMIter;
import de.lmu.ifi.dbs.elki.database.ids.DBIDRef;
import de.lmu.ifi.dbs.elki.database.ids.DBIDUtil;
import de.lmu.ifi.dbs.elki.database.ids.DBIDs;
import de.lmu.ifi.dbs.elki.database.ids.ModifiableDBIDs;
import de.lmu.ifi.dbs.elki.database.query.range.RangeQuery;
import de.lmu.ifi.dbs.elki.database.relation.Relation;
import de.lmu.ifi.dbs.elki.distance.distancefunction.AbstractPrimitiveDistanceFunction;
import de.lmu.ifi.dbs.elki.distance.distancefunction.DistanceFunction;
import de.lmu.ifi.dbs.elki.logging.Logging;
import de.lmu.ifi.dbs.elki.logging.progress.FiniteProgress;
import de.lmu.ifi.dbs.elki.logging.progress.IndefiniteProgress;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.OptionID;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.Parameterization;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.DoubleParameter;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.IntParameter;
/*
 This file is part of ELKI:
 Environment for Developing KDD-Applications Supported by Index-Structures

 Copyright (C) 2015
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

public class FDBSCAN<O extends UncertainObject<UOModel>> extends DBSCAN<O> {

  private final static Logging LOG = Logging.getLogger(FDBSCAN.class);

  protected double threshold;

  protected int sampleSize;

  protected int constraint;

  protected FDBSCAN(final DistanceFunction<? super O> distanceFunction, final double epsilon, final double threshold, final int minpts, final int sampleSize) {
    super(distanceFunction, epsilon, minpts);
    this.threshold = threshold;
    this.sampleSize = sampleSize;
    this.constraint = (int) (sampleSize * sampleSize * threshold);
  }

  protected void expandCluster(final Relation<O> relation, final RangeQuery<O> rangeQuery, final DBIDRef startObjectID, final FiniteProgress objprog, final IndefiniteProgress clusprog) {
    final DBIDs epsilonNeighbours = this.getEpsilonNeighbours(startObjectID, relation, this.epsilon);


    // startObject is no core-object
    if(epsilonNeighbours.size() < this.minpts) {
      this.noise.add(startObjectID);
      this.processedIDs.add(startObjectID);
      if(objprog != null && clusprog != null) {
        objprog.setProcessed(this.processedIDs.size(), FDBSCAN.LOG);
        clusprog.setProcessed(this.resultList.size(), FDBSCAN.LOG);
      }
      return;
    }

    // try to expand the cluster
    final ModifiableDBIDs seeds = DBIDUtil.newHashSet();
    final ModifiableDBIDs currentCluster = DBIDUtil.newArray();
    for(final DBIDIter seed = epsilonNeighbours.iter(); seed.valid(); seed.advance()) {
      if(!this.processedIDs.contains(seed)) {
        currentCluster.add(seed);
        this.processedIDs.add(seed);
        seeds.add(seed);
      }
      else if(this.noise.contains(seed)) {
        currentCluster.add(seed);
        this.noise.remove(seed);
      }
    }
    seeds.remove(startObjectID);

    while(!seeds.isEmpty()) {
      final DBIDMIter o = seeds.iter();
      final DBIDs neighborhood = this.getEpsilonNeighbours(o, relation, this.epsilon);
      o.remove();

      if(neighborhood.size() >= this.minpts) {
        for(final DBIDIter neighbor = neighborhood.iter(); neighbor.valid(); neighbor.advance()) {
          final boolean inNoise = this.noise.contains(neighbor);
          final boolean unclassified = !this.processedIDs.contains(neighbor);
          if(inNoise || unclassified) {
            if(unclassified) {
              seeds.add(neighbor);
            }
            currentCluster.add(neighbor);
            this.processedIDs.add(neighbor);
            if(inNoise) {
              this.noise.remove(neighbor);
            }
          }
        }
      }

      if(this.processedIDs.size() == relation.size() && this.noise.size() == 0) {
        break;
      }

      if(objprog != null && clusprog != null) {
        objprog.setProcessed(this.processedIDs.size(), FDBSCAN.LOG);
        final int numClusters = currentCluster.size() > this.minpts ? this.resultList.size() + 1 : this.resultList.size();
        clusprog.setProcessed(numClusters, FDBSCAN.LOG);
      }
    }
    if(currentCluster.size() >= this.minpts) {
      this.resultList.add(currentCluster);
    }
    else {
      this.noise.addDBIDs(currentCluster);
      this.noise.add(startObjectID);
      this.processedIDs.add(startObjectID);
    }
  }

  @SuppressWarnings("unchecked")
  private DBIDs getEpsilonNeighbours(final DBIDRef startObjectID, final Relation<O> relation, final double epsilon) {
    final ModifiableDBIDs epsilonNeighbours = DBIDUtil.newArray();
    final UncertainObject<UOModel> uo = relation.get(startObjectID);

    for(final DBIDIter iter = relation.getDBIDs().iter(); iter.valid(); iter.advance()) {
      if(iter.internalGetIndex() == startObjectID.internalGetIndex()) {
        continue;
      }
      final UncertainObject<UOModel> possibleNeighbour = relation.get(iter);

      boolean inside = true, possible = true;
      for(int i = 0; i < uo.getDimensionality(); i++) {
        final double minBound = uo.getMin(i) - epsilon;
        final double maxBound = uo.getMax(i) + epsilon;
        if(possibleNeighbour.getMin(i) < minBound || possibleNeighbour.getMax(i) > maxBound) {
          inside = false;
          if(possibleNeighbour.getMin(i) > maxBound || possibleNeighbour.getMax(i) < minBound) {
            possible = false;
            break;
          }
        }
      }
      if(inside) {
        epsilonNeighbours.add(iter);
        continue;
      } else if (!possible) {
        continue;
      }

      if(uo.getModel() instanceof UniformDiscreteUO) {
        final List<?> samplesUO = ((UniformDiscreteUO) uo.getModel()).getObservationsCopy();
        final List<?> samplesPossibleNeighbour = ((UniformDiscreteUO) uo.getModel()).getObservationsCopy();
        int matches = 0;
        for(final DoubleVector uoSample : ((List<DoubleVector>) samplesUO)) {
          for(final DoubleVector possibleNeighbourSample : ((List<DoubleVector>) samplesPossibleNeighbour)) {
            if(((AbstractPrimitiveDistanceFunction<NumberVector>) this.getDistanceFunction()).distance(uoSample, possibleNeighbourSample) <= epsilon) {
              matches++;
            }
          }
        }
        if(matches >= (int) (samplesUO.size() * samplesPossibleNeighbour.size() * this.threshold)) {
          epsilonNeighbours.add(iter);
        }
      } else {
        int matches = 0;
        for(int i = 0; i < this.sampleSize; i++) {
          for(int j = 0; j < this.sampleSize; j++) {
            if(((AbstractPrimitiveDistanceFunction<NumberVector>) this.getDistanceFunction()).distance(uo.drawSample(), possibleNeighbour.drawSample()) <= epsilon) {
              matches++;
            }
          }
        }
        if(matches >= this.constraint) {
          epsilonNeighbours.add(iter);
        }
      }
    }
    return epsilonNeighbours;
  }

  @Override
  public TypeInformation[] getInputTypeRestriction() {
    return TypeUtil.array(new SimpleTypeInformation<>(UncertainObject.class));
  }

  @Override
  protected Logging getLogger() {
    return FDBSCAN.LOG;
  }

  public static class Parameterizer<O extends UncertainObject<UOModel>> extends DBSCAN.Parameterizer<O> {

    protected double threshold;

    protected int sampleSize;

    public static final OptionID THRESHOLD_ID = new OptionID("fdbscan.threshold", "The minimum part of the sampled distances claiming epsilon-neighborhood.");

    public static final OptionID SAMPLE_SIZE_ID = new OptionID("fdbscan.sampleSize", "How many samples should be drawn per uncertain object. Choose in regard to your uncertainty model.");

    @Override
    protected void makeOptions(final Parameterization config) {
      super.makeOptions(config);
      final DoubleParameter thresholdP = new DoubleParameter(Parameterizer.THRESHOLD_ID, 0.5);
      if(config.grab(thresholdP)) {
        this.threshold = thresholdP.getValue();
      }
      final IntParameter sampleP = new IntParameter(Parameterizer.SAMPLE_SIZE_ID, 5);
      if(config.grab(sampleP)) {
        this.sampleSize = sampleP.getValue();
      }
    }

    @Override
    protected FDBSCAN<O> makeInstance() {
      return new FDBSCAN<>(this.distanceFunction, this.epsilon, this.threshold, this.minpts, this.sampleSize);
    }

  }
}
