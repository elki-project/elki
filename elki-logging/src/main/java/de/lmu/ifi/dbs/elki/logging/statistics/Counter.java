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
 * Simple statistic by counting. For example: invocations of a method.
 * 
 * @author Erich Schubert
 * @since 0.6.0
 */
public interface Counter extends Statistic {
  /**
   * Increment the counter.
   * 
   * @return Current value.
   */
  long increment();

  /**
   * Decrement the counter.
   * 
   * @return Current value.
   */
  long decrement();

  /**
   * Get the current count.
   * 
   * @return Current count.
   */
  long getValue();

  /**
   * Increment the counter by i.
   * 
   * Note: the increment may be negative!
   * 
   * @param i increment.
   * @return Current count.
   */
  long increment(long i);
}
