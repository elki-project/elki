/**
 * ELKI framework "Environment for Developing KDD-Applications Supported by
 * Index-Structures".
 * <p>
 * {@link de.lmu.ifi.dbs.elki.KDDTask} is the basic work-flow for unsupervised
 * knowledge discovery. It will setup a
 * {@link de.lmu.ifi.dbs.elki.datasource.DatabaseConnection DatabaseConnection},
 * run an {@link de.lmu.ifi.dbs.elki.algorithm.Algorithm Algorithm} on it and
 * pass the result to a {@link de.lmu.ifi.dbs.elki.result.ResultHandler
 * ResultHandler}.
 *
 * @opt hide ^experimentalcode\.
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
package de.lmu.ifi.dbs.elki;
