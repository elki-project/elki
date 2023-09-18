/*
 * This file is part of ELKI:
 * Environment for Developing KDD-Applications Supported by Index-Structures
 *
 * Copyright (C) 2022
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
package elki.gui.icons;

import java.lang.ref.SoftReference;
import java.util.HashMap;
import java.util.Map;

import javax.swing.Icon;
import javax.swing.ImageIcon;

import elki.logging.LoggingUtil;

/**
 * Stock icon library for use in the GUI.
 * 
 * @author Erich Schubert
 * @since 0.4.0
 * @opt nodefillcolor LemonChiffon
 */
public final class StockIcon {
  /**
   * Private constructor. Static methods only.
   */
  private StockIcon() {
    // Do not use.
  }

  /** Dialog error icon */
  public static final String DIALOG_ERROR = "dialog-error";

  /** Dialog information icon */
  public static final String DIALOG_INFORMATION = "dialog-information";

  /** Dialog warning icon */
  public static final String DIALOG_WARNING = "dialog-warning";

  /** Document open icon */
  public static final String DOCUMENT_OPEN = "document-open";

  /** Document properties icon */
  public static final String DOCUMENT_PROPERTIES = "document-properties";

  /** Document save icon */
  public static final String DOCUMENT_SAVE = "document-save";

  /** Edit clear operation icon */
  public static final String EDIT_CLEAR = "edit-clear";

  /** Edit redo operation icon */
  public static final String EDIT_REDO = "edit-redo";

  /** Edit undo operation icon */
  public static final String EDIT_UNDO = "edit-undo";

  /** Edit find icon */
  public static final String EDIT_FIND = "edit-find";

  /** Emblem to show importance */
  public static final String EMBLEM_IMPORTANT = "emblem-important";

  /** Go to bottom */
  public static final String GO_BOTTOM = "go-bottom";

  /** Go down */
  public static final String GO_DOWN = "go-down";

  /** Go to first */
  public static final String GO_FIRST = "go-first";

  /** Go home */
  public static final String GO_HOME = "go-home";

  /** Jump */
  public static final String GO_JUMP = "go-jump";

  /** Go to last */
  public static final String GO_LAST = "go-last";

  /** Go to next */
  public static final String GO_NEXT = "go-next";

  /** Go to previous */
  public static final String GO_PREVIOUS = "go-previous";

  /** Go to top */
  public static final String GO_TOP = "go-top";

  /** Go up */
  public static final String GO_UP = "go-up";

  /** Help browser */
  public static final String HELP_BROWSER = "help-browser";

  /** Add to list */
  public static final String LIST_ADD = "list-add";

  /** Remove from list */
  public static final String LIST_REMOVE = "list-remove";

  /** Package icon */
  public static final String PACKAGE = "package";

  /** Stop the process */
  public static final String PROCESS_STOP = "process-stop";

  /** Search */
  public static final String SYSTEM_SEARCH = "system-search";

  /** Map from strings to icons */
  private static final Map<String, SoftReference<Icon>> iconcache = new HashMap<>();

  /**
   * Get a particular stock icon.
   * 
   * @param name Icon name
   * @return Icon
   */
  public static Icon getStockIcon(String name) {
    SoftReference<Icon> ref = iconcache.get(name);
    if(ref != null) {
      Icon icon = ref.get();
      if(icon != null) {
        return icon;
      }
    }
    java.net.URL imgURL = StockIcon.class.getResource(name + ".png");
    if(imgURL == null) {
      LoggingUtil.warning("Could not find stock icon: " + name);
      return null;
    }
    Icon icon = new ImageIcon(imgURL);
    iconcache.put(name, new SoftReference<>(icon));
    return icon;
  }
}
