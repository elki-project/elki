/*
 * This file is part of ELKI:
 * Environment for Developing KDD-Applications Supported by Index-Structures
 *
 * Copyright (C) 2019
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
package de.lmu.ifi.dbs.elki.logging.statistics;

/**
 * Class to count events in a thread-safe counter.
 * 
 * @author Erich Schubert
 * @since 0.6.0
 */
public class UnsynchronizedLongCounter extends AbstractStatistic implements Counter {
  /**
   * The counter to use.
   */
  long counter = 0;

  /**
   * Constructor.
   * 
   * @param key Key to report.
   */
  public UnsynchronizedLongCounter(String key) {
    super(key);
  }

  @Override
  public long increment() {
    return ++counter;
  }

  @Override
  public long decrement() {
    return --counter;
  }

  @Override
  public long increment(long i) {
    counter += i;
    return counter;
  }

  @Override
  public long getValue() {
    return counter;
  }

  @Override
  public String formatValue() {
    return Long.toString(getValue());
  }
}
