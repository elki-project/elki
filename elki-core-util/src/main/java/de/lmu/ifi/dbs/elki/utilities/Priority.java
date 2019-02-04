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
package de.lmu.ifi.dbs.elki.utilities;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * This annotation is used for sorting entries in the UIs.
 * 
 * @author Erich Schubert
 * @since 0.7.5
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ ElementType.TYPE })
public @interface Priority {
  /**
   * Priority of this class
   * 
   * @return Priority
   */
  int value();

  /**
   * Supplementary priority.
   */
  int SUPPLEMENTARY = -100;

  /**
   * Default priority.
   */
  int DEFAULT = 0;

  /**
   * Important implementations.
   */
  int IMPORTANT = 100;

  /**
   * Recommended implementations.
   */
  int RECOMMENDED = 200;

  /**
   * Users are suggested to use this, so their own extensions show up at the
   * top.
   */
  int USER = 1000;
}
