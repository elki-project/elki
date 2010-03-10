package de.lmu.ifi.dbs.elki.utilities.optionhandling;

import java.util.Collection;
import java.util.List;

import de.lmu.ifi.dbs.elki.algorithm.Algorithm;
import de.lmu.ifi.dbs.elki.logging.LoggingUtil;
import de.lmu.ifi.dbs.elki.utilities.ClassGenericsUtil;
import de.lmu.ifi.dbs.elki.utilities.Description;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.Parameterizable;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.SerializedParameterization;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.TrackParameters;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.NumberParameter;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.Parameter;
import de.lmu.ifi.dbs.elki.utilities.output.FormatUtil;
import de.lmu.ifi.dbs.elki.utilities.pairs.Pair;

/**
 * Utility functions related to Option handling.
 * 
 */
public final class OptionUtil {
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
  public static <N extends NumberParameter<?>> String parameterNamesAndValuesToString(List<N> parameters) {
    StringBuffer buffer = new StringBuffer();
    buffer.append("[");
    for(int i = 0; i < parameters.size(); i++) {
      buffer.append(parameters.get(i).getName());
      buffer.append(":");
      buffer.append(parameters.get(i).getValue().doubleValue());
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
      for(String line : FormatUtil.splitAtLastBlank(longDescription, width - indent.length())) {
        buf.append(indent);
        buf.append(line);
        if(!line.endsWith(FormatUtil.NEWLINE)) {
          buf.append(FormatUtil.NEWLINE);
        }
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
      buf.append("Description for class ");
      buf.append(pcls.getName());
      buf.append(":\n");
      
      SerializedParameterization config = new SerializedParameterization();
      TrackParameters track = new TrackParameters(config);
      Object p = ClassGenericsUtil.tryInstanciate(Object.class, pcls, track);

      if(p instanceof Algorithm<?, ?>) {
        Algorithm<?, ?> a = (Algorithm<?, ?>) p;
        buf.append(a.getDescription().toString());
        buf.append("\n");
      }
      if(p instanceof Parameterizable) {
        Description desc = ((Parameterizable) p).getDescription();
        if(desc != null) {
          buf.append(desc.toString());
        }
      }
      Collection<Pair<Object, Parameter<?, ?>>> options = track.getAllParameters();
      if (options.size() > 0) {
        OptionUtil.formatForConsole(buf, width, indent, options);
      }
      // TODO: report constraints?
      return buf;
    }
    catch(Exception e) {
      LoggingUtil.exception("Error instantiating class to describe.", e.getCause());
      buf.append("No description available: " + e);
      return buf;
    }
  }
}