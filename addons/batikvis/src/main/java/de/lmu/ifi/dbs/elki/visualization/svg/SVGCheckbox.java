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
package de.lmu.ifi.dbs.elki.visualization.svg;

import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.EventListenerList;

import org.apache.batik.util.SVGConstants;
import org.w3c.dom.Element;
import org.w3c.dom.events.Event;
import org.w3c.dom.events.EventListener;
import org.w3c.dom.events.EventTarget;

/**
 * SVG checkbox.
 * 
 * @author Sascha Goldhofer
 * @since 0.5.0
 */
public class SVGCheckbox {
  /**
   * Status flag
   */
  protected boolean checked;

  /**
   * Event listeners
   */
  protected EventListenerList listenerList = new EventListenerList();

  /**
   * Checkbox label
   */
  protected String label = null;

  /**
   * Constructor, without label
   * 
   * @param checked Checked status
   */
  public SVGCheckbox(boolean checked) {
    this(checked, null);
  }

  /**
   * Constructor, with label
   * 
   * @param checked Checked status
   * @param label Label
   */
  public SVGCheckbox(boolean checked, String label) {
    this.checked = checked;
    this.label = label;
  }

  /**
   * Render the SVG checkbox to a plot
   * 
   * @param svgp Plot to draw to
   * @param x X offset
   * @param y Y offset
   * @param size Size factor
   * @return Container element
   */
  public Element renderCheckBox(SVGPlot svgp, double x, double y, double size) {
    // create check
    final Element checkmark = SVGEffects.makeCheckmark(svgp);
    checkmark.setAttribute(SVGConstants.SVG_TRANSFORM_ATTRIBUTE, "scale(" + (size / 12) + ") translate(" + x + " " + y + ")");
    if(!checked) {
      checkmark.setAttribute(SVGConstants.SVG_STYLE_ATTRIBUTE, SVGConstants.CSS_DISPLAY_PROPERTY + ":" + SVGConstants.CSS_NONE_VALUE);
    }

    // create box
    Element checkbox_box = SVGUtil.svgRect(svgp.getDocument(), x, y, size, size);
    checkbox_box.setAttribute(SVGConstants.SVG_FILL_ATTRIBUTE, "#d4e4f1");
    checkbox_box.setAttribute(SVGConstants.SVG_STROKE_ATTRIBUTE, "#a0a0a0");
    checkbox_box.setAttribute(SVGConstants.SVG_STROKE_WIDTH_ATTRIBUTE, "0.5");

    // create checkbox
    final Element checkbox = svgp.svgElement(SVGConstants.SVG_G_TAG);
    checkbox.appendChild(checkbox_box);
    checkbox.appendChild(checkmark);

    // create Label
    if(label != null) {
      Element labele = svgp.svgText(x + 2 * size, y + size, label);
      // TODO: font size!
      checkbox.appendChild(labele);
    }

    // add click event listener
    EventTarget targ = (EventTarget) checkbox;
    targ.addEventListener(SVGConstants.SVG_CLICK_EVENT_TYPE, new EventListener() {
      @Override
      public void handleEvent(Event evt) {
        if(checked ^= true) {
          checkmark.removeAttribute(SVGConstants.SVG_STYLE_ATTRIBUTE);
        }
        else {
          checkmark.setAttribute(SVGConstants.SVG_STYLE_ATTRIBUTE, SVGConstants.CSS_DISPLAY_PROPERTY + ":" + SVGConstants.CSS_NONE_VALUE);
        }
        fireSwitchEvent(new ChangeEvent(SVGCheckbox.this));
      }
    }, false);
    return checkbox;
  }

  /**
   * Register a listener for this checkbox.
   * 
   * @param listener Listener to add
   */
  public void addCheckBoxListener(ChangeListener listener) {
    listenerList.add(ChangeListener.class, listener);
  }

  /**
   * Remove a listener for this checkbox.
   * 
   * @param listener Listener to remove
   */
  public void removeCheckBoxListener(ChangeListener listener) {
    listenerList.remove(ChangeListener.class, listener);
  }

  /**
   * Return the checkmark status.
   * 
   * @return true, when checked
   */
  public boolean isChecked() {
    return checked;
  }

  /**
   * Fire the event to listeners
   * 
   * @param evt Event to fire
   */
  protected void fireSwitchEvent(ChangeEvent evt) {
    Object[] listeners = listenerList.getListenerList();
    for(int i = 1; i < listeners.length; i += 2) {
      if(listeners[i - 1] == ChangeListener.class) {
        ((ChangeListener) listeners[i]).stateChanged(evt);
      }
    }
  }
}