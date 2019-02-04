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
package de.lmu.ifi.dbs.elki.gui.icons;

import java.lang.ref.SoftReference;
import java.util.HashMap;
import java.util.Map;

import javax.swing.Icon;
import javax.swing.ImageIcon;

import de.lmu.ifi.dbs.elki.logging.LoggingUtil;

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

  public static final String DIALOG_ERROR = "dialog-error";

  public static final String DIALOG_INFORMATION = "dialog-information";

  public static final String DIALOG_WARNING = "dialog-warning";

  public static final String DOCUMENT_OPEN = "document-open";

  public static final String DOCUMENT_PROPERTIES = "document-properties";

  public static final String DOCUMENT_SAVE = "document-save";

  public static final String EDIT_CLEAR = "edit-clear";

  public static final String EDIT_REDO = "edit-redo";

  public static final String EDIT_UNDO = "edit-undo";

  public static final String EDIT_FIND = "edit-find";

  public static final String EMBLEM_IMPORTANT = "emblem-important";

  public static final String GO_BOTTOM = "go-bottom";

  public static final String GO_DOWN = "go-down";

  public static final String GO_FIRST = "go-first";

  public static final String GO_HOME = "go-home";

  public static final String GO_JUMP = "go-jump";

  public static final String GO_LAST = "go-last";

  public static final String GO_NEXT = "go-next";

  public static final String GO_PREVIOUS = "go-previous";

  public static final String GO_TOP = "go-top";

  public static final String GO_UP = "go-up";

  public static final String HELP_BROWSER = "help-browser";

  public static final String LIST_ADD = "list-add";

  public static final String LIST_REMOVE = "list-remove";

  public static final String PACKAGE = "package";

  public static final String PROCESS_STOP = "process-stop";

  public static final String SYSTEM_SEARCH = "system-search";

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
    if(imgURL != null) {
      Icon icon = new ImageIcon(imgURL);
      iconcache.put(name, new SoftReference<>(icon));
      return icon;
    }
    LoggingUtil.warning("Could not find stock icon: " + name);
    return null;
  }
}
