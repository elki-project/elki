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
package de.lmu.ifi.dbs.elki.datasource.bundle;

import java.util.ArrayList;
import java.util.Arrays;

import de.lmu.ifi.dbs.elki.data.type.SimpleTypeInformation;

/**
 * Store the package metadata in an array list. While this is a trivial class,
 * it improves code readability and type safety.
 * 
 * @author Erich Schubert
 * @since 0.4.0
 * 
 * @composed - - - SimpleTypeInformation
 */
public class BundleMeta extends ArrayList<SimpleTypeInformation<?>> {
  /**
   * Serial version
   */
  private static final long serialVersionUID = 1L;

  /**
   * Constructor.
   */
  public BundleMeta() {
    super();
  }

  /**
   * Constructor.
   * 
   * @param initialCapacity
   */
  public BundleMeta(int initialCapacity) {
    super(initialCapacity);
  }

  /**
   * Constructor.
   * 
   * @param types
   */
  public BundleMeta(SimpleTypeInformation<?>... types) {
    super(Arrays.asList(types));
  }
}