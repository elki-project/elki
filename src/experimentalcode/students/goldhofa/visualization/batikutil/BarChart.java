package experimentalcode.students.goldhofa.visualization.batikutil;

import org.apache.batik.util.SVGConstants;
import org.w3c.dom.Element;

import de.lmu.ifi.dbs.elki.visualization.svg.SVGPlot;
import de.lmu.ifi.dbs.elki.visualization.svg.SVGUtil;
import experimentalcode.students.goldhofa.CCConstants;

public class BarChart implements UIInterfaceElement {
  
  protected SVGPlot svgp;
  
  protected Element label = null;
  protected Element valueLabel = null;
  
  protected Element barchart;
  protected Element bar;

  protected Element chart;
  protected boolean horizontal;
  
  protected double width;
  protected double length;
  protected double fillLength;
  
  protected double size;
  protected double fill;

  
  public BarChart(SVGPlot svgp, double width, double height, boolean isHorizontal) {
    this.svgp = svgp;
    horizontal = isHorizontal;
    barchart = svgp.svgElement(SVGConstants.SVG_G_TAG);
    
    bar = svgp.svgRect(0.0, 0.0, width, height);
    bar.setAttribute(SVGConstants.SVG_FILL_ATTRIBUTE, "#a0a0a0");
    bar.setAttribute(SVGConstants.SVG_STROKE_ATTRIBUTE, "#a0a0a0");
    bar.setAttribute(SVGConstants.SVG_STROKE_WIDTH_ATTRIBUTE, "0.5");
 
    if (isHorizontal) {
      length  = width;
      this.width = height;
      chart   = svgp.svgRect(1, 1, 0, height-2);
    } else {
      length = height;
      this.width = width;
      chart   = svgp.svgRect(1, 1, width-2, 0);
    }   

    chart.setAttribute(SVGConstants.SVG_FILL_ATTRIBUTE, "#d4e4f1");
    chart.setAttribute(SVGConstants.SVG_STROKE_ATTRIBUTE, "#a0a0a0");
    chart.setAttribute(SVGConstants.SVG_STROKE_WIDTH_ATTRIBUTE, "0.5");
    
    barchart = SVGUI.group(svgp, bar, chart);
  }
  
  public void setSize(double size) {
    this.size = size;
  }
  
  public void setFill(double newfill) {
    this.fill = newfill;
    this.fillLength = (fill/size)*length;
    this.chart.setAttribute(SVGConstants.SVG_WIDTH_ATTRIBUTE, ""+fillLength);
  }
  
  public void addLabel(String text) {
    if (horizontal) {
      label = svgp.svgText(length+5, width/2 + 5, text);
    } else {
      label = svgp.svgText(0, length+5, text);
    }
    
    SVGUtil.addCSSClass(label,  CCConstants.CSS_TEXT);
    barchart.appendChild(label);
  }
  
  public void showValues() {
    
    if (horizontal) {
      
      valueLabel = svgp.svgText(2, width/2 + 5, ""+Math.round(fill)+" / "+Math.round(size));
      
    } else {
      
      valueLabel = svgp.svgText(width + 5, 10, ""+Math.round(fill)+" / "+Math.round(size));
    }
    
    SVGUtil.addCSSClass(valueLabel,  CCConstants.CSS_TEXT);
    barchart.appendChild(valueLabel);
  }
  
  public Element asElement() {
    return barchart;
  }

}
