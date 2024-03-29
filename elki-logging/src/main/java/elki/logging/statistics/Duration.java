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
package elki.logging.statistics;

/**
 * Class that tracks the duration of a task.
 * 
 * TODO: add support for different time units?
 * 
 * @author Erich Schubert
 * @since 0.6.0
 */
public interface Duration extends Statistic {
  /**
   * Start the timer.
   * 
   * @return {@code this} for call chaining
   */
  Duration begin();

  /**
   * Finish the timer.
   * 
   * @return {@code this} for call chaining
   */
  Duration end();

  /**
   * Get the begin of the interval.
   * 
   * @return Begin
   */
  long getBegin();

  /**
   * Get the end of the interval.
   * 
   * @return End
   */
  long getEnd();
  
  /**
   * Get the duration of the interval.
   * 
   * @return Duration
   */
  long getDuration();
}
