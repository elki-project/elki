package de.lmu.ifi.dbs.elki.utilities.exceptions;
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

/**
 * Interface to collect exception messages that are used in several cases.
 * 
 * @author Arthur Zimek
 */
public interface ExceptionMessages {
  /**
   * Message when the user requested a help message.
   */
  public static final String USER_REQUESTED_HELP = "Aborted: User requested help message.";
  /**
   * Messages in case a database is unexpectedly empty.
   */
  public static final String DATABASE_EMPTY = "database empty: must contain elements";
  /**
   * Message when a new label was discovered in a database, that did not exist before.
   */
  public static final String INCONSISTENT_STATE_NEW_LABEL = "inconsistent state of database - found new label";
  /**
   * Message when an empty clustering is encountered.
   */
  public static final String CLUSTERING_EMPTY = "Clustering doesn't contain any cluster.";
  /**
   * Message when a distance doesn't support undefined values.
   */
  public static final String UNSUPPORTED_UNDEFINED_DISTANCE = "Undefinded distance not supported!";
  /**
   * Generic "unsupported" message
   */
  public static final String UNSUPPORTED = "Unsupported.";
  /**
   * Generic "not yet supported" message
   */
  public static final String UNSUPPORTED_NOT_YET = "Not yet supported.";
  /**
   * "remove unsupported" message for iterators
   */
  public static final String UNSUPPORTED_REMOVE = "remove() unsupported";
  /**
   * File not found. 404.
   */
  public static final String FILE_NOT_FOUND = "File not found";
  /**
   * File already exists, will not overwrite.
   */
  public static final String FILE_EXISTS = "File already exists";
}
