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
package de.lmu.ifi.dbs.elki.logging;

import java.util.logging.Formatter;
import java.util.logging.LogRecord;

import de.lmu.ifi.dbs.elki.logging.progress.ProgressLogRecord;

/**
 * A formatter to simply retrieve the message of an LogRecord without printing
 * origin information. Usually, the formatter will try to ensure a newline at the end.
 * 
 * @author Arthur Zimek
 * @since 0.1
 */
public class MessageFormatter extends Formatter {
  /**
   * Constructor.
   */
  public MessageFormatter() {
    super();
  }

  /**
   * Retrieves the message as it is set in the given LogRecord.
   */
  @Override
  public String format(LogRecord record) {
    String msg = record.getMessage();
    if(msg.length() > 0) {
      if (record instanceof ProgressLogRecord) {
        return msg;
      }
      if(msg.endsWith(OutputStreamLogger.NEWLINE)) {
        return msg;
      }
    }
    return msg + OutputStreamLogger.NEWLINE;
  }
}
