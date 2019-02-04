/**
 * Logging facility for controlling logging behavior of the complete framework.
 * <h3>Logging in ELKI</h3>
 * Logging in ELKI is closely following the {@link java.util.logging.Logger}
 * approach.
 * <p>
 * However, system-wide configuration of logging does not seem appropriate,
 * therefore ELKI uses a configuration file named
 * 
 * <pre>
 * logging - cli.properties
 * </pre>
 * 
 * living in the package {@link de.lmu.ifi.dbs.elki.logging} (or an
 * appropriately named directory) for command line interface based operation.
 * <p>
 * Logging levels can be configured on a per-class or per-package level using,
 * e.g.:
 * 
 * <pre>
 * de.lmu.ifi.dbs.elki.index.level = FINE
 * </pre>
 * 
 * to set the logging level for the index structure package to FINE.
 * <h3>Logging for Developers:</h3>
 * Developers working in ELKI are encouraged to use the following setup to make
 * configurable logging:
 * <ol>
 * <li>Introduce one or multiple static final debug flags in their classes:<br>
 * <code>protected static final boolean debug = true || {@link de.lmu.ifi.dbs.elki.logging.LoggingConfiguration#DEBUG LoggingConfiguration.DEBUG};</code>
 * <br>
 * After development, it should be changed to
 * <code>false || {@link de.lmu.ifi.dbs.elki.logging.LoggingConfiguration#DEBUG LoggingConfiguration.DEBUG}</code>.
 * </li>
 * <li>
 * If the class contains 'frequent' logging code, acquire a static Logger
 * reference:<br>
 * <code>protected static final {@link de.lmu.ifi.dbs.elki.logging.Logging Logging} logger = {@link de.lmu.ifi.dbs.elki.logging.Logging#getLogger Logging.getLogger}(Example.class);</code>
 * </li>
 * <li>
 * Wrap logging statements in appropriate level checks:<br>
 * <code>if ({@link de.lmu.ifi.dbs.elki.logging.Logging#isVerbose logger.isVerbose()}) {
 *   // compute logging message
 *   {@link de.lmu.ifi.dbs.elki.logging.Logging#verbose logger.verbose}(expensive + message + construction);
 * }</code>
 * </li>
 * <li>
 * For infrequent logging, the following static convenience function is
 * appropriate:
 * <code>{@link de.lmu.ifi.dbs.elki.logging.LoggingUtil#exception LoggingUtil.exception}("Out of memory in algorithm.", exception);</code>
 * <br>
 * This function is expensive (it acquires a stack trace to obtain class and
 * method references, retrieves a logger reference etc.) and thus should only be
 * used for 'rare' logging events.
 * </li>
 * <li>
 * In cases where many tests would occur, also consider using:
 * 
 * <pre>
 * final boolean verbose = {@link de.lmu.ifi.dbs.elki.logging.Logging#isVerbose logger.isVerbose}();
 * // ... for, while, anything expensive
 * if (verbose) {
 *   {@link de.lmu.ifi.dbs.elki.logging.Logging#verbose logger.verbose}(...);
 * }
 * </pre>
 * 
 * </li>
 * </ol>
 */
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