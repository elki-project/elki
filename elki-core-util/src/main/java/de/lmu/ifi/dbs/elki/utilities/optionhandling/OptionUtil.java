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
package de.lmu.ifi.dbs.elki.utilities.optionhandling;

import java.util.Collection;
import java.util.List;

import de.lmu.ifi.dbs.elki.utilities.ClassGenericsUtil;
import de.lmu.ifi.dbs.elki.utilities.documentation.Description;
import de.lmu.ifi.dbs.elki.utilities.documentation.Reference;
import de.lmu.ifi.dbs.elki.utilities.documentation.Title;
import de.lmu.ifi.dbs.elki.utilities.exceptions.ClassInstantiationException;
import de.lmu.ifi.dbs.elki.utilities.io.FormatUtil;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.constraints.ParameterConstraint;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.SerializedParameterization;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.TrackParameters;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.TrackedParameter;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.Parameter;

/**
 * Utility functions related to Option handling.
 * 
 * @author Erich Schubert
 * @since 0.2
 * 
 * @assoc - - - Parameter
 */
public final class OptionUtil {
  /**
   * Fake constructor. Use static method.
   */
  private OptionUtil() {
    // Do not instantiate.
  }

  /**
   * Format a list of options (and associated owning objects) for console help
   * output.
   * 
   * @param buf Serialization buffer
   * @param width Screen width
   * @param options List of options
   */
  public static void formatForConsole(StringBuilder buf, int width, Collection<TrackedParameter> options) {
    for(TrackedParameter pair : options) {
      Parameter<?> par = pair.getParameter();
      println(buf//
          .append(SerializedParameterization.OPTION_PREFIX).append(par.getOptionID().getName()) //
          .append(' ').append(par.getSyntax()).append(FormatUtil.NEWLINE), //
          width, getFullDescription(par));
    }
  }

  /**
   * Format a parameter description.
   * 
   * @param param Parameter
   * @return Parameter description
   */
  public static String getFullDescription(Parameter<?> param) {
    StringBuilder description = new StringBuilder(1000) //
        .append(param.getShortDescription()).append(FormatUtil.NEWLINE);
    param.describeValues(description);
    if(!FormatUtil.endsWith(description, FormatUtil.NEWLINE)) {
      description.append(FormatUtil.NEWLINE);
    }
    if(param.hasDefaultValue()) {
      description.append("Default: ").append(param.getDefaultValueAsString()).append(FormatUtil.NEWLINE);
    }
    List<? extends ParameterConstraint<?>> constraints = param.getConstraints();
    if(constraints != null && !constraints.isEmpty()) {
      description.append((constraints.size() == 1) ? "Constraint: " : "Constraints: ") //
          .append(constraints.get(0).getDescription(param.getOptionID().getName()));
      for(int i = 1; i < constraints.size(); i++) {
        description.append(", ").append(constraints.get(i).getDescription(param.getOptionID().getName()));
      }
      description.append(FormatUtil.NEWLINE);
    }
    return description.toString();
  }

  /**
   * Simple writing helper with no indentation.
   * 
   * @param buf Buffer to write to
   * @param width Width to use for linewraps
   * @param data Data to write.
   */
  private static void println(StringBuilder buf, int width, String data) {
    for(String line : FormatUtil.splitAtLastBlank(data, width)) {
      buf.append(line);
      if(!line.endsWith(FormatUtil.NEWLINE)) {
        buf.append(FormatUtil.NEWLINE);
      }
    }
  }

  /**
   * Format a description of a Parameterizable (including recursive options).
   * 
   * @param buf Buffer to append to.
   * @param pcls Parameterizable class to describe
   * @param width Width
   * @param indent Text indent
   * @return Formatted description
   */
  public static StringBuilder describeParameterizable(StringBuilder buf, Class<?> pcls, int width, String indent) throws ClassInstantiationException {
    println(buf, width, "Description for class " + pcls.getName());

    Title title = pcls.getAnnotation(Title.class);
    if(title != null && title.value() != null && !title.value().isEmpty()) {
      println(buf, width, title.value());
    }

    Description desc = pcls.getAnnotation(Description.class);
    if(desc != null && desc.value() != null && !desc.value().isEmpty()) {
      println(buf, width, desc.value());
    }

    for(Reference ref : pcls.getAnnotationsByType(Reference.class)) {
      if(!ref.prefix().isEmpty()) {
        println(buf, width, ref.prefix());
      }
      println(buf, width, ref.authors());
      println(buf, width, ref.title());
      println(buf, width, ref.booktitle());
      if(ref.url().length() > 0) {
        println(buf, width, ref.url());
      }
    }

    SerializedParameterization config = new SerializedParameterization();
    TrackParameters track = new TrackParameters(config);
    @SuppressWarnings("unused")
    Object p = ClassGenericsUtil.tryInstantiate(Object.class, pcls, track);
    Collection<TrackedParameter> options = track.getAllParameters();
    if(!options.isEmpty()) {
      OptionUtil.formatForConsole(buf, width, options);
    }
    return buf;
  }
}
