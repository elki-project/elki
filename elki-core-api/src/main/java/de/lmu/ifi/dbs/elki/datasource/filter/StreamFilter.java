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
package de.lmu.ifi.dbs.elki.datasource.filter;

import de.lmu.ifi.dbs.elki.datasource.bundle.BundleStreamSource;

/**
 * Streaming filters are often more efficient (less memory use) as they do not
 * keep a reference to earlier data.
 * 
 * @author Erich Schubert
 * @since 0.5.0
 * 
 * @opt nodefillcolor LemonChiffon
 *
 * @assoc - filters - BundleStreamSource
 */
public interface StreamFilter extends ObjectFilter, BundleStreamSource {
  /**
   * Connect to the previous stream.
   * 
   * @param source Stream source
   */
  BundleStreamSource init(BundleStreamSource source);
}
