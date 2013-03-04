package de.lmu.ifi.dbs.elki.logging.statistics;

/*
 This file is part of ELKI:
 Environment for Developing KDD-Applications Supported by Index-Structures

 Copyright (C) 2013
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
 * Class that tracks the runtime of a task with {@code System.nanoTime()}
 * 
 * @author Erich Schubert
 */
public class NanoDuration extends AbstractStatistic implements Duration {
  /**
   * Tracking variables.
   */
  long begin = -1, end = -2;

  /**
   * Constructor.
   * 
   * @param key Key
   */
  public NanoDuration(String key) {
    super(key);
  }

  @Override
  public void begin() {
    begin = System.nanoTime();
  }

  @Override
  public void end() {
    end = System.nanoTime();
  }

  @Override
  public long getBegin() {
    return begin;
  }

  @Override
  public long getEnd() {
    return end;
  }

  @Override
  public long getDuration() {
    return (end - begin);
  }

  @Override
  public String formatValue() {
    return getDuration() + " ns";
  }
}
