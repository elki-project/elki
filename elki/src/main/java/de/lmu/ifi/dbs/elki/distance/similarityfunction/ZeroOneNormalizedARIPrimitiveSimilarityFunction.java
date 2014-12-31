package de.lmu.ifi.dbs.elki.distance.similarityfunction;

import de.lmu.ifi.dbs.elki.algorithm.clustering.PWCClusteringAlgorithm;
import de.lmu.ifi.dbs.elki.data.Clustering;
import de.lmu.ifi.dbs.elki.data.model.Model;
import de.lmu.ifi.dbs.elki.data.type.SimpleTypeInformation;
import de.lmu.ifi.dbs.elki.evaluation.clustering.ClusterContingencyTable;
import de.lmu.ifi.dbs.elki.evaluation.clustering.PairCounting;
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

/**
 *
 * Class for use in {@link PWCClusteringAlgorithm}
 * for the metaclustering.
 *
 * @author Alexander Koos
 *
 */
public class ZeroOneNormalizedARIPrimitiveSimilarityFunction extends PWCPrimitiveSimilarityFunction implements NormalizedPrimitiveSimilarityFunction<Clustering<Model>> {

  @Override
  public SimpleTypeInformation<? super Clustering<Model>> getInputTypeRestriction() {
    return new SimpleTypeInformation<>(Clustering.class);
  }

  @Override
  public double getMetricScale(final PairCounting pairCounting) {
    return ( pairCounting.adjustedRandIndex() + 1 ) / 2;
  }

  @Override
  public double similarity(final Clustering<Model> o1, final Clustering<Model> o2) {
    final ClusterContingencyTable cct = new ClusterContingencyTable(false, true);
    cct.process(o1, o2);

    return ( ( cct.getPaircount().adjustedRandIndex() + 1 ) / 2);
  }

}
