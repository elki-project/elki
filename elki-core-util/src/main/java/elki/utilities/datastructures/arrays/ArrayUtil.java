/*
 * This file is part of ELKI:
 * Environment for Developing KDD-Applications Supported by Index-Structures
 * 
 * Copyright (C) 2022
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
package elki.utilities.datastructures.arrays;

import java.util.ArrayList;
import java.util.Random;

/**
 * Utility functions for manipulating arrays.
 *
 * @author Erich Schubert
 * @since 0.8.0
 */
public class ArrayUtil {
  /**
   * Swap two values in an object array.
   * 
   * @param data Data
   * @param i First position
   * @param j Second position
   * @param <T> Object type
   */
  public static <T> void swap(ArrayList<T> data, int i, int j) {
    T tmp = data.get(i);
    data.set(i, data.get(j));
    data.set(j, tmp);
  }

  /**
   * Swap two values in an object array.
   * 
   * @param data Data
   * @param i First position
   * @param j Second position
   * @param <T> Object type
   */
  public static <T> void swap(T[] data, int i, int j) {
    T tmp = data[i];
    data[i] = data[j];
    data[j] = tmp;
  }

  /**
   * Swap two values in an integer array.
   * 
   * @param data Data
   * @param i First position
   * @param j Second position
   */
  public static void swap(int[] data, int i, int j) {
    int tmp = data[i];
    data[i] = data[j];
    data[j] = tmp;
  }

  /**
   * Swap two values in a short array.
   * 
   * @param data Data
   * @param i First position
   * @param j Second position
   */
  public static void swap(short[] data, int i, int j) {
    short tmp = data[i];
    data[i] = data[j];
    data[j] = tmp;
  }

  /**
   * Swap two values in a byte array.
   * 
   * @param data Data
   * @param i First position
   * @param j Second position
   */
  public static void swap(byte[] data, int i, int j) {
    byte tmp = data[i];
    data[i] = data[j];
    data[j] = tmp;
  }

  /**
   * Swap two values in a double array.
   * 
   * @param data Data
   * @param i First position
   * @param j Second position
   */
  public static void swap(double[] data, int i, int j) {
    double tmp = data[i];
    data[i] = data[j];
    data[j] = tmp;
  }

  /**
   * Swap two values in a float array.
   * 
   * @param data Data
   * @param i First position
   * @param j Second position
   */
  public static void swap(float[] data, int i, int j) {
    float tmp = data[i];
    data[i] = data[j];
    data[j] = tmp;
  }

  /**
   * Random shuffle (part) of an array.
   * 
   * @param data Input array
   * @param random Random generator
   * @param limit Number of elements to randomize
   */
  public static void randomShuffle(int[] data, Random random, final int limit) {
    for(int i = 0; i < limit; i++) {
      swap(data, i, i + random.nextInt(data.length - i));
    }
  }

  /**
   * Random shuffle (part) of an array.
   * 
   * @param data Input array
   * @param random Random generator
   * @param limit Number of elements to randomize
   */
  public static void randomShuffle(short[] data, Random random, final int limit) {
    for(int i = 0; i < limit; i++) {
      swap(data, i, i + random.nextInt(data.length - i));
    }
  }

  /**
   * Random shuffle (part) of an array.
   * 
   * @param data Input array
   * @param random Random generator
   * @param limit Number of elements to randomize
   */
  public static void randomShuffle(byte[] data, Random random, final int limit) {
    for(int i = 0; i < limit; i++) {
      swap(data, i, i + random.nextInt(data.length - i));
    }
  }

  /**
   * Random shuffle (part) of an array.
   * 
   * @param data Input array
   * @param random Random generator
   * @param limit Number of elements to randomize
   */
  public static void randomShuffle(double[] data, Random random, final int limit) {
    for(int i = 0; i < limit; i++) {
      swap(data, i, i + random.nextInt(data.length - i));
    }
  }

  /**
   * Random shuffle (part) of an array.
   * 
   * @param data Input array
   * @param random Random generator
   * @param limit Number of elements to randomize
   */
  public static void randomShuffle(float[] data, Random random, final int limit) {
    for(int i = 0; i < limit; i++) {
      swap(data, i, i + random.nextInt(data.length - i));
    }
  }

  /**
   * Random shuffle (part) of an array.
   * 
   * @param data Input array
   * @param random Random generator
   * @param limit Number of elements to randomize
   * @param <T> Array object type
   */
  public static <T> void randomShuffle(T[] data, Random random, final int limit) {
    for(int i = 0; i < limit; i++) {
      swap(data, i, i + random.nextInt(data.length - i));
    }
  }

  /**
   * Random shuffle (part) of an array.
   * 
   * @param data Input array
   * @param random Random generator
   * @param limit Number of elements to randomize
   * @param <T> Array object type
   */
  public static <T> void randomShuffle(ArrayList<T> data, Random random, final int limit) {
    for(int i = 0; i < limit; i++) {
      swap(data, i, i + random.nextInt(data.size() - i));
    }
  }
}
