package de.lmu.ifi.dbs.elki.utilities.optionhandling;

/*
 This file is part of ELKI:
 Environment for Developing KDD-Applications Supported by Index-Structures

 Copyright (C) 2012
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

import java.util.Collection;
import java.util.List;

import de.lmu.ifi.dbs.elki.logging.LoggingUtil;
import de.lmu.ifi.dbs.elki.utilities.ClassGenericsUtil;
import de.lmu.ifi.dbs.elki.utilities.FormatUtil;
import de.lmu.ifi.dbs.elki.utilities.documentation.DocumentationUtil;
import de.lmu.ifi.dbs.elki.utilities.documentation.Reference;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.SerializedParameterization;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.TrackParameters;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.Parameter;
import de.lmu.ifi.dbs.elki.utilities.pairs.Pair;

/**
 * Utility functions related to Option handling.
 * 
 * @apiviz.uses Parameter
 */
public final class OptionUtil {
  /**
   * Fake constructor. Use static method.
   */
  private OptionUtil() {
    // Do not instantiate.
  }

  /**
   * Returns a string representation of the specified list of options containing
   * the names of the options.
   * 
   * @param <O> Option type
   * @param options the list of options
   * @return the names of the options
   */
  public static <O extends Parameter<?, ?>> String optionsNamesToString(List<O> options) {
    StringBuffer buffer = new StringBuffer();
    buffer.append("[");
    for(int i = 0; i < options.size(); i++) {
      buffer.append(options.get(i).getName());
      if(i != options.size() - 1) {
        buffer.append(",");
      }
    }
    buffer.append("]");
    return buffer.toString();
  }

  /**
   * Returns a string representation of the specified list of options containing
   * the names of the options.
   * 
   * @param <O> Option type
   * @param options the list of options
   * @return the names of the options
   */
  public static <O extends Parameter<?, ?>> String optionsNamesToString(O[] options) {
    StringBuffer buffer = new StringBuffer();
    buffer.append("[");
    for(int i = 0; i < options.length; i++) {
      buffer.append(options[i].getName());
      if(i != options.length - 1) {
        buffer.append(",");
      }
    }
    buffer.append("]");
    return buffer.toString();
  }

  /**
   * Returns a string representation of the list of number parameters containing
   * the names and the values of the parameters.
   * 
   * @param <N> Parameter type
   * @param parameters the list of number parameters
   * @return the names and the values of the parameters
   */
  public static <N extends Parameter<?, ?>> String parameterNamesAndValuesToString(List<N> parameters) {
    StringBuffer buffer = new StringBuffer();
    buffer.append("[");
    for(int i = 0; i < parameters.size(); i++) {
      buffer.append(parameters.get(i).getName());
      buffer.append(":");
      buffer.append(parameters.get(i).getValueAsString());
      if(i != parameters.size() - 1) {
        buffer.append(", ");
      }
    }
    buffer.append("]");
    return buffer.toString();
  }

  /**
   * Format a list of options (and associated owning objects) for console help
   * output.
   * 
   * @param buf Serialization buffer
   * @param width Screen width
   * @param indent Indentation string
   * @param options List of options
   */
  public static void formatForConsole(StringBuffer buf, int width, String indent, Collection<Pair<Object, Parameter<?, ?>>> options) {
    for(Pair<Object, Parameter<?, ?>> pair : options) {
      String currentOption = pair.getSecond().getName();
      String syntax = pair.getSecond().getSyntax();
      String longDescription = pair.getSecond().getFullDescription();

      buf.append(SerializedParameterization.OPTION_PREFIX);
      buf.append(currentOption);
      buf.append(" ");
      buf.append(syntax);
      buf.append(FormatUtil.NEWLINE);
      println(buf, width, longDescription, indent);
    }
  }

  /**
   * Simple writing helper with no indentation.
   * 
   * @param buf Buffer to write to
   * @param width Width to use for linewraps
   * @param data Data to write.
   * @param indent Indentation
   */
  public static void println(StringBuffer buf, int width, String data, String indent) {
    for(String line : FormatUtil.splitAtLastBlank(data, width - indent.length())) {
      buf.append(indent);
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
  public static StringBuffer describeParameterizable(StringBuffer buf, Class<?> pcls, int width, String indent) {
    try {
      println(buf, width, "Description for class " + pcls.getName(), "");

      String title = DocumentationUtil.getTitle(pcls);
      if(title != null && title.length() > 0) {
        println(buf, width, title, "");
      }

      String desc = DocumentationUtil.getDescription(pcls);
      if(desc != null && desc.length() > 0) {
        println(buf, width, desc, "  ");
      }

      Reference ref = DocumentationUtil.getReference(pcls);
      if(ref != null) {
        if(ref.prefix().length() > 0) {
          println(buf, width, ref.prefix(), "");
        }
        println(buf, width, ref.authors() + ":", "");
        println(buf, width, ref.title(), "  ");
        println(buf, width, "in: " + ref.booktitle(), "");
        if(ref.url().length() > 0) {
          println(buf, width, "see also: " + ref.url(), "");
        }
      }

      SerializedParameterization config = new SerializedParameterization();
      TrackParameters track = new TrackParameters(config);
      @SuppressWarnings("unused")
      Object p = ClassGenericsUtil.tryInstantiate(Object.class, pcls, track);
      Collection<Pair<Object, Parameter<?, ?>>> options = track.getAllParameters();
      if(options.size() > 0) {
        OptionUtil.formatForConsole(buf, width, indent, options);
      }
      // TODO: report global constraints?
      return buf;
    }
    catch(Exception e) {
      LoggingUtil.exception("Error instantiating class to describe.", e.getCause());
      buf.append("No description available: ").append(e);
      return buf;
    }
  }
}