package de.lmu.ifi.dbs.elki.data.uncertain;

import java.util.Random;

/**
 * Utility class to provide some methods used by classes of the uncertain
 * branch.
 * 
 * @author Alexander Koos
 */
public class UncertainUtil {
  /**
   * Calculate a list of integer weight-values for an uncertain object.
   * 
   * Those integer weights are used for sample drawing later on.
   * 
   * @param size
   * @param totalProb
   * @param rand
   * @return
   */
  public static int[] calculateRandomIntegerWeights(final int size, final int totalProb, final Random rand) {
    final int[] result = new int[size];
    double baseSum = 0.0;
    final int[] probDis = new int[size];
    for(int i = 0; i < size; i++) {
      probDis[i] = rand.nextInt(UOModel.DEFAULT_PROBABILITY_SEED) + 1;
      baseSum += probDis[i];
    }
    baseSum /= totalProb;
    for(int i = 0; i < size; i++) {
      probDis[i] /= baseSum;
    }
    return result;
  }

  /**
   * 
   * Calculates the weight that wins a particular random draw and returns its
   * index.
   * 
   * @param rand
   * @param weights
   * @param totalProb
   * @return
   */
  public static int drawIndexFromIntegerWeights(final Random rand, final int[] weights, final int totalProb) {
    final int index = rand.nextInt(UOModel.PROBABILITY_SCALE);
    for(int i = 0, sum = 0;; i++) {
      sum += weights[i];
      if(sum >= index || sum >= totalProb) {
        return i;
      }
    }
  }
}
