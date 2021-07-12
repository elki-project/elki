/*
 * This file is part of ELKI:
 * Environment for Developing KDD-Applications Supported by Index-Structures
 * 
 * Copyright (C) 2020
 * ELKI Development Team
 * 
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Affero General Public License for more details.
 * 
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package elki.clustering.hierarchical.betula.initialization;

import elki.clustering.hierarchical.betula.CFInterface;
import elki.data.NumberVector;

/**
 * Calculates the Euclidean distance on any of the betula clustering Features.
 *
 * @author Andreas Lang
 */
public class VarDist implements CFIDistance {

    @Override
    public double squaredDistance(NumberVector clusterCenter, CFInterface candidate) {
        final int d = clusterCenter.getDimensionality();
        assert (d == candidate.getDimensionality());
        double sum = candidate.SoD();
        for(int i = 0; i < d; i++) {
            double dx = candidate.centroid(i) - clusterCenter.doubleValue(i);
            sum += candidate.getWeight() * dx * dx;
        }
        return sum;
    }

    @Override
    public double squaredDistance(double[] clusterCenter, CFInterface candidate) {
        final int d = clusterCenter.length;
        assert (d == candidate.getDimensionality());
        double sum = candidate.SoD();
        for(int i = 0; i < d; i++) {
            double dx = candidate.centroid(i) - clusterCenter[i];
            sum += candidate.getWeight() * dx * dx;
        }
        return sum;
    }

    @Override
    public double squaredDistance(CFInterface clusterCenter, CFInterface candidate) {
        final int d = clusterCenter.getDimensionality();
        assert (d == candidate.getDimensionality());
        double sum = candidate.SoD();
        for(int i = 0; i < d; i++) {
            double dx = candidate.centroid(i) - clusterCenter.centroid(i);
            sum += candidate.getWeight() * dx * dx;
        }
        return sum;
    }

}
