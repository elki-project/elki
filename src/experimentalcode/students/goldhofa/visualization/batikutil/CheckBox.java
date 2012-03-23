package experimentalcode.students.goldhofa.visualization.batikutil;

import javax.swing.event.EventListenerList;

import org.apache.batik.util.SVGConstants;
import org.w3c.dom.Element;
import org.w3c.dom.events.Event;
import org.w3c.dom.events.EventListener;
import org.w3c.dom.events.EventTarget;

import de.lmu.ifi.dbs.elki.visualization.svg.SVGPlot;
import de.lmu.ifi.dbs.elki.visualization.svg.SVGUtil;
import experimentalcode.students.goldhofa.CCConstants;

public class CheckBox implements UIInterfaceElement, EventListener {
  protected boolean checked;

  protected EventListenerList listenerList = new EventListenerList();

  protected Element checkbox;

  protected Element checkmark;

  public CheckBox(SVGPlot svgp, boolean checked) {
    this.checked = checked;
    createCheckBox(svgp);
  }

  public CheckBox(SVGPlot svgp, boolean checked, String labelText) {
    this.checked = checked;
    createCheckBox(svgp);

    // create Label
    Element label = svgp.svgText(20, 12, labelText);
    SVGUtil.addCSSClass(label, CCConstants.CSS_TEXT_BOLD);
    checkbox.appendChild(label);
  }

  protected void createCheckBox(SVGPlot svgp) {
    SVGUI.setupCSS(svgp);

    // create check
    checkmark = SVGUI.svgCheckmark(svgp);
    if(!checked) {
      SVGUtil.addCSSClass(checkmark, CCConstants.CSS_DISPLAY_NONE);
    }

    // create box
    Element checkbox_box = SVGUtil.svgRect(svgp.getDocument(), 0, 2, 11, 11);
    checkbox_box.setAttribute(SVGConstants.SVG_FILL_ATTRIBUTE, "#d4e4f1");
    checkbox_box.setAttribute(SVGConstants.SVG_STROKE_ATTRIBUTE, "#a0a0a0");
    checkbox_box.setAttribute(SVGConstants.SVG_STROKE_WIDTH_ATTRIBUTE, "0.5");

    // create checkbox
    checkbox = SVGUI.group(svgp, checkbox_box, checkmark);

    // add click event
    EventTarget targ = (EventTarget) checkbox;
    targ.addEventListener(SVGConstants.SVG_CLICK_EVENT_TYPE, this, false);
  }

  public Element asElement() {
    return checkbox;
  }

  public void addCheckBoxListener(CheckBoxListener listener) {
    listenerList.add(CheckBoxListener.class, listener);
  }

  public void removeCheckBoxListener(CheckBoxListener listener) {
    listenerList.remove(CheckBoxListener.class, listener);
  }

  public void handleEvent(Event evt) {
    if(checked) {
      uncheck();
    }
    else {
      check();
    }
  }

  protected void check() {
    SVGUtil.removeCSSClass(checkmark, CCConstants.CSS_DISPLAY_NONE);
    checked = true;
    fireSwitchEvent(new SwitchEvent(this, checked));
  }

  protected void uncheck() {
    SVGUtil.addCSSClass(checkmark, CCConstants.CSS_DISPLAY_NONE);
    checked = false;
    fireSwitchEvent(new SwitchEvent(this, checked));
  }

  protected void fireSwitchEvent(SwitchEvent evt) {
    Object[] listeners = listenerList.getListenerList();
    for(int i = 0; i < listeners.length; i += 2) {
      if(listeners[i] == CheckBoxListener.class) {
        ((CheckBoxListener) listeners[i + 1]).switched(evt);
      }
    }
  }
}