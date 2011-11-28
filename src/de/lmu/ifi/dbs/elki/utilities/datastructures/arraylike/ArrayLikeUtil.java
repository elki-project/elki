package de.lmu.ifi.dbs.elki.utilities.datastructures.arraylike;

/*
 This file is part of ELKI:
 Environment for Developing KDD-Applications Supported by Index-Structures

 Copyright (C) 2011
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

import java.util.List;

import de.lmu.ifi.dbs.elki.data.FeatureVector;
import de.lmu.ifi.dbs.elki.data.NumberVector;

/**
 * Utility class that allows plug-in use of various "array-like" types such as
 * lists in APIs that can take any kind of array to safe the cost of
 * reorganizing the objects into a real array.
 * 
 * @author Erich Schubert
 */
public final class ArrayLikeUtil {
  /**
   * Static instance for lists
   */
  private static final ListArrayAdapter<Object> LISTADAPTER = new ListArrayAdapter<Object>();

  /**
   * Static instance for lists of numbers
   */
  private static final NumberListArrayAdapter<Number> NUMBERLISTADAPTER = new NumberListArrayAdapter<Number>();

  /**
   * Static instance
   */
  private final static IdentityArrayAdapter<?> IDENTITYADAPTER = new IdentityArrayAdapter<Object>();

  /**
   * Static instance
   */
  private static final FeatureVectorAdapter<?> FEATUREVECTORADAPTER = new FeatureVectorAdapter<Number>();

  /**
   * Use a number vector in the array API.
   */
  private static final NumberVectorAdapter<?> NUMBERVECTORADAPTER = new NumberVectorAdapter<Double>();

  /**
   * Use a double array in the array API.
   */
  public static final NumberArrayAdapter<Double, double[]> DOUBLEARRAYADAPTER = new DoubleArrayAdapter();

  /**
   * USe a Trove double list as array.
   */
  public static final TDoubleListAdapter TDOUBLELISTADAPTER = new TDoubleListAdapter();
  
  /**
   * Cast the static instance.
   * 
   * @param dummy Dummy variable, for type inference
   * @return Static instance
   */
  @SuppressWarnings("unchecked")
  public static <T> ArrayAdapter<T, List<? extends T>> listAdapter(List<? extends T> dummy) {
    return (ListArrayAdapter<T>) LISTADAPTER;
  }

  /**
   * Cast the static instance.
   * 
   * @param dummy Dummy variable, for type inference
   * @return Static instance
   */
  @SuppressWarnings("unchecked")
  public static <T extends Number> NumberArrayAdapter<T, List<? extends T>> numberListAdapter(List<? extends T> dummy) {
    return (NumberListArrayAdapter<T>) NUMBERLISTADAPTER;
  }

  /**
   * Get the static instance.
   * 
   * @param dummy Dummy object for type inference
   * @return Static instance
   */
  @SuppressWarnings("unchecked")
  public static <T> IdentityArrayAdapter<T> identityAdapter(T dummy) {
    return (IdentityArrayAdapter<T>) IDENTITYADAPTER;
  }

  /**
   * Get the static instance.
   * 
   * @param prototype Prototype value, for type inference
   * @return Instance
   */
  @SuppressWarnings("unchecked")
  public static <F> FeatureVectorAdapter<F> featureVectorAdapter(FeatureVector<?, F> prototype) {
    return (FeatureVectorAdapter<F>) FEATUREVECTORADAPTER;
  }

  /**
   * Get the static instance.
   * 
   * @param prototype Prototype value, for type inference
   * @return Instance
   */
  @SuppressWarnings("unchecked")
  public static <N extends Number> NumberVectorAdapter<N> numberVectorAdapter(NumberVector<?, N> prototype) {
    return (NumberVectorAdapter<N>) NUMBERVECTORADAPTER;
  }

  /**
   * Get the adapter for double arrays.
   * 
   * @return double array adapter
   */
  public static NumberArrayAdapter<Double, double[]> doubleArrayAdapter() {
    return DOUBLEARRAYADAPTER;
  }
}