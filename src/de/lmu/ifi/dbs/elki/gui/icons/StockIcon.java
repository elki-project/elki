package de.lmu.ifi.dbs.elki.gui.icons;

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
 * 
 * @apiviz.landmark
 */
public class StockIcon {
  public final static String DIALOG_ERROR = "dialog-error";

  public final static String DIALOG_INFORMATION = "dialog-information";

  public final static String DIALOG_WARNING = "dialog-warning";

  public final static String DOCUMENT_OPEN = "document-open";

  public final static String DOCUMENT_PROPERTIES = "document-properties";

  public final static String DOCUMENT_SAVE = "document-save";

  public final static String EDIT_CLEAR = "edit-clear";

  public final static String EDIT_REDO = "edit-redo";

  public final static String EDIT_UNDO = "edit-undo";

  public final static String EMBLEM_IMPORTANT = "emblem-important";

  public final static String GO_BOTTOM = "go-bottom";

  public final static String GO_DOWN = "go-down";

  public final static String GO_FIRST = "go-first";

  public final static String GO_HOME = "go-home";

  public final static String GO_JUMP = "go-jump";

  public final static String GO_LAST = "go-last";

  public final static String GO_NEXT = "go-next";

  public final static String GO_PREVIOUS = "go-previous";

  public final static String GO_TOP = "go-top";

  public final static String GO_UP = "go-up";

  public final static String HELP_BROWSER = "help-browser";

  public final static String LIST_ADD = "list-add";

  public final static String LIST_REMOVE = "list-remove";

  public final static String PROCESS_STOP = "process-stop";

  private final static Map<String, SoftReference<Icon>> iconcache = new HashMap<String, SoftReference<Icon>>();

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
      iconcache.put(name, new SoftReference<Icon>(icon));
      return icon;
    }
    else {
      LoggingUtil.warning("Could not find stock icon: " + name);
      return null;
    }
  }
}