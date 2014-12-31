package de.lmu.ifi.dbs.elki.data.uncertain;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

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
 * Util class to provide some methods used by
 * classes of the uncertain branch.
 * 
 * @author Alexander Koos
 *
 */
public class UncertainUtil {

  /**
   * 
   * Calculate a list of integer weight-values for
   * an uncertain object.
   * 
   * Those integer weights are used for sample
   * drawing later on.
   * 
   * @param size
   * @param totalProb
   * @param rand
   * @return
   */
  public static List<Integer> calculateRandomIntegerWeights(final int size, final int totalProb, final Random rand) {
    final List<Integer> result = new ArrayList<Integer>();
    double baseSum = 0.0;
    final int[] probDis = new int[size];
    for(int i = 0; i < size; i++) {
      probDis[i] = rand.nextInt(UOModel.DEFAULT_PROBABILITY_SEED) + 1;
      baseSum += probDis[i];
    }
    baseSum /= totalProb;
    for(int i = 0; i < size; i++) {
      result.add(Integer.valueOf( (int) ( probDis[i] / baseSum ) ));
    }
    return result;
  }
  
  /**
   * 
   * Calculates the weight that wins a particular random
   * draw and returns its index.
   * 
   * @param rand
   * @param weights
   * @param totalProb
   * @return
   */
  public static int drawIndexFromIntegerWeights(final Random rand, final List<Integer> weights, final int totalProb) {
    int i = 0;
    final int index = rand.nextInt(UOModel.PROBABILITY_SCALE);
    int sum = 0;
    do {
      sum += weights.get(i++);
    } while(sum < index && sum < totalProb);
    
    return --i;
  }
}
