/**
 * Classes for computing histograms
 * <p>
 * This package contains two families of histograms. Static histograms have a
 * fixed initial number of bins. When encountering values outside of their
 * range, they will grow similar to an ArrayList by adding additional bins.
 * <p>
 * "Dynamic" histograms are more useful when you do not know the value range of
 * the data: they start by collecting a number of sample data, then use this to
 * estimate the initial histogram range. If they grow to twice their initial
 * size they will downsample, to keep the histogram size in the bounds n to
 * 2n-1, which effectively limits the memory use and histogram complexity.
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
package de.lmu.ifi.dbs.elki.utilities.datastructures.histogram;
