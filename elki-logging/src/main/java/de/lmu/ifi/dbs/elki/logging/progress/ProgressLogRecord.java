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
package de.lmu.ifi.dbs.elki.logging.progress;

import java.util.logging.Level;

import de.lmu.ifi.dbs.elki.logging.ELKILogRecord;

/**
 * Log record for progress messages.
 * 
 * @author Erich Schubert
 * @since 0.3
 * 
 * @has - - - Progress
 */
public class ProgressLogRecord extends ELKILogRecord {
  /**
   * Serial version
   */
  private static final long serialVersionUID = 1L;
  
  /**
   * Progress storage
   */
  private final Progress progress;

  /**
   * Constructor for progress log messages.
   * 
   * @param level Logging level
   * @param progress Progress to log
   */
  public ProgressLogRecord(Level level, Progress progress) {
    super(level, null);
    this.progress = progress;
    this.setMessage(progress.toString());
  }

  /**
   * Get the objects progress.
   * 
   * @return the progress
   */
  public Progress getProgress() {
    return progress;
  }
}