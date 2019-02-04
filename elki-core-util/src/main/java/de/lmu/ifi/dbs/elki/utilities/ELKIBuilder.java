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

import de.lmu.ifi.dbs.elki.logging.Logging;
import de.lmu.ifi.dbs.elki.utilities.exceptions.AbortException;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.OptionID;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.ListParameterization;

/**
 * Builder utility class.
 *
 * This class delegates to {@link ListParameterization} and
 * {@link ClassGenericsUtil}, but may be easier to use in many cases.
 * 
 * You will often need to specify the type T, because of <i>nested</i>
 * generics. This is because of the way generics are implemented in Java, and
 * we cannot easily resolve this; you have to give the type explicitly.
 *
 * Example:
 * {@code
 * RStarTreeFactory<DoubleVector> indexfactory =
 *   new ELKIBuilder<RStarTreeFactory<DoubleVector>>(RStarTreeFactory.class)
 *       .with(AbstractPageFileFactory.Parameterizer.PAGE_SIZE_ID, 512)
 *       .with(RStarTreeFactory.Parameterizer.BULK_SPLIT_ID, SortTileRecursiveBulkSplit.class)
 *       .build();
 * }
 * with {@code new ELKIBuilder<RStarTreeFactory<DoubleVector>>} this code is
 * fine, whereas {@code new ELKIBuilder<>} will cause an unchecked cast warning,
 * because you are, in fact, casting a {@code RStarTreeFactory} (without
 * generics) into a {@code RStarTreeFactory<DoubleVector>} here.
 *
 * @author Erich Schubert
 * @since 0.7.5
 * 
 * @has - - - ClassGenericsUtil
 *
 * @param <T> Class to build.
 */
public final class ELKIBuilder<T> {
  /**
   * Class logger.
   */
  private static final Logging LOG = Logging.getLogger(ELKIBuilder.class);

  /**
   * Class to build.
   */
  private Class<? super T> clazz;

  /**
   * Parameter list.
   */
  private ListParameterization p = new ListParameterization();

  /**
   * Constructor.
   *
   * You will often need to specify the type T, because of <i>nested</i>
   * generics. This is because of the way generics are implemented in Java, and
   * we cannot easily resolve this; you have to give the type explicitly.
   *
   * Example:
   * {@code
   * RStarTreeFactory<DoubleVector> indexfactory =
   *   new ELKIBuilder<RStarTreeFactory<DoubleVector>>(RStarTreeFactory.class)
   *       .with(AbstractPageFileFactory.Parameterizer.PAGE_SIZE_ID, 512)
   *       .with(RStarTreeFactory.Parameterizer.BULK_SPLIT_ID, SortTileRecursiveBulkSplit.class)
   *       .build();
   * }
   * with {@code new ELKIBuilder<RStarTreeFactory<DoubleVector>>} this code is
   * fine, whereas {@code new ELKIBuilder<>} will cause an unchecked cast
   * warning, because you are, in fact, casting a {@code RStarTreeFactory}
   * (without generics) into a {@code RStarTreeFactory<DoubleVector>} here.
   *
   * @param clazz Class
   */
  public ELKIBuilder(Class<? super T> clazz) {
    this.clazz = clazz;
  }

  /**
   * Add an option to the builder.
   *
   * @param opt Option ID (usually found in the Parameterizer class)
   * @param value Value
   * @return The same builder
   */
  public ELKIBuilder<T> with(OptionID opt, Object value) {
    p.addParameter(opt, value);
    return this;
  }

  /**
   * Add a flag to the builder.
   *
   * @param opt Option ID (usually found in the Parameterizer class)
   * @return The same builder
   */
  public ELKIBuilder<T> with(OptionID opt) {
    p.addFlag(opt);
    return this;
  }

  /**
   * Add an option to the builder.
   *
   * @param opt Option ID (usually found in the Parameterizer class)
   * @param value Value
   * @return The same builder
   */
  public ELKIBuilder<T> with(String opt, Object value) {
    p.addParameter(opt, value);
    return this;
  }

  /**
   * Add a flag to the builder.
   *
   * @param opt Option ID (usually found in the Parameterizer class)
   * @return The same builder
   */
  public ELKIBuilder<T> with(String opt) {
    p.addFlag(opt);
    return this;
  }

  /**
   * Instantiate, consuming the parameter list.
   *
   * This will throw an {@link AbortException} if the parameters are incomplete,
   * or {@code build()} is called twice.
   * 
   * We lose some type safety here, for convenience. The type {@code <C>} is
   * meant to be {@code <T>} with generics added only, which is necessary
   * because generics are implemented by erasure in Java.
   *
   * @param <C> Output type
   * @return Instance
   */
  @SuppressWarnings("unchecked")
  public <C extends T> C build() {
    if(p == null) {
      throw new AbortException("build() may be called only once.");
    }
    final T obj = ClassGenericsUtil.parameterizeOrAbort(clazz, p);
    if(p.hasUnusedParameters()) {
      LOG.warning("Unused parameters: " + p.getRemainingParameters());
    }
    p = null; // Prevent build() from being called again.
    return (C) obj;
  }
}
