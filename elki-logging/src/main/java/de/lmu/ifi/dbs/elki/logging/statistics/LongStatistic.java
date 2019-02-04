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
 * Trivial long-valued statistic.
 * 
 * @author Erich Schubert
 * @since 0.6.0
 */
public class LongStatistic extends AbstractStatistic {
  /**
   * Statistic value.
   */
  long value;

  /**
   * Constructor.
   * 
   * @param key Key
   */
  public LongStatistic(String key) {
    super(key);
  }

  /**
   * Constructor.
   * 
   * @param key Key
   * @param value Value
   */
  public LongStatistic(String key, long value) {
    super(key);
    this.value = value;
  }

  /**
   * Set the statistics.
   * 
   * @param value New value
   * @return {@code this} for chaining
   */
  public LongStatistic setLong(long value) {
    this.value = value;
    return this;
  }

  /**
   * Increment counter.
   * 
   * @param inc Increment
   * @return {@code this} for chaining
   */
  public LongStatistic increment(long inc) {
    this.value += inc;
    return this;
  }

  @Override
  public String formatValue() {
    return Long.toString(value);
  }
}
