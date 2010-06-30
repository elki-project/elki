package de.lmu.ifi.dbs.elki.visualization.visualizers;

import de.lmu.ifi.dbs.elki.utilities.datastructures.AnyMap;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.Parameterizable;

/**
 * Defines the requirements for a visualizer. <br>
 * Note: Any implementation is supposed to provide a constructor without
 * parameters (default constructor) to be used for parameterization.
 * 
 * @author Remigius Wojdanowski
 */
public interface Visualizer extends Parameterizable {
  /**
   * Meta data key: Visualizer name for UI
   * 
   * Type: String
   */
  public final static String META_NAME = "name";
  
  /**
   * Meta data key: Level for visualizer ordering
   *
   * Returns an integer indicating the "temporal position" of this Visualizer.
   * It is intended to impose an ordering on the execution of Visualizers as a
   * Visualizer may depend on another Visualizer running earlier. <br>
   * Lower numbers should result in a earlier use of this Visualizer, while
   * higher numbers should result in a later use. If more Visualizers have the
   * same level, no ordering is guaranteed. <br>
   * Note that this value is only a recommendation, as it is totally up to the
   * framework to ignore it.
   * 
   * Type: Integer
   */
  public final static String META_LEVEL = "level";
  
  /**
   * Flag to control visibility. Type: Boolean
   */
  public final static String META_VISIBLE = "visible";

  /**
   * Flag to signal there is no thumbnail needed. Type: Boolean
   */
  public final static String META_NOTHUMB = "no-thumbnail";

  /**
   * Flag to signal the visualizer should not be exported. Type: Boolean
   */
  public final static String META_NOEXPORT = "no-export";

  /**
   * Flag to signal default visibility of a visualizer. Type: Boolean
   */
  public static final String META_VISIBLE_DEFAULT = "visible-default";

  /**
   * Flag to mark the visualizer as tool. Type: Boolean
   */
  public static final String META_TOOL = "tool";

  /**
   * Background layer
   */
  public final static int LEVEL_BACKGROUND = 0;

  /**
   * Data layer
   */
  public final static int LEVEL_DATA = 100;

  /**
   * Static plot layer
   */
  public final static int LEVEL_STATIC = 200;

  /**
   * Passive foreground layer
   */
  public final static int LEVEL_FOREGROUND = 300;

  /**
   * Active foreground layer (interactive elements)
   */
  public final static int LEVEL_INTERACTIVE = 1000;

  /**
   * Get visualization meta data, such as dimensions visualized.
   * 
   * @return AnyMap reference with meta data.
   */
  public AnyMap<String> getMetadata();
}