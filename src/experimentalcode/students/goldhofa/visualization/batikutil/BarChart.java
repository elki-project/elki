package experimentalcode.students.goldhofa.visualization.batikutil;

import org.apache.batik.util.SVGConstants;
import org.w3c.dom.Element;

import de.lmu.ifi.dbs.elki.utilities.FormatUtil;
import de.lmu.ifi.dbs.elki.visualization.svg.SVGPlot;

public class BarChart implements UIInterfaceElement {
  protected SVGPlot svgp;

  protected Element label = null;

  protected Element valueLabel = null;

  protected Element barchart;

  protected Element bar;

  protected Element chart;

  protected boolean horizontal;

  protected double width;
  
  protected double x = 0.0;
  protected double y = 0.0;

  protected double length;

  protected double fillLength;

  protected double size;

  protected double fill;

  public BarChart(SVGPlot svgp, double x, double y, double width, double height, boolean isHorizontal) {
    this.svgp   = svgp;
    this.x      = x;
    this.y      = y;
    build(width, height, isHorizontal);
  }
  
  public BarChart(SVGPlot svgp, double width, double height, boolean isHorizontal) {
    this.svgp   = svgp;
    build(width, height, isHorizontal);
  }
  
  protected void build(double width, double height, boolean isHorizontal) {
    horizontal = isHorizontal;
    barchart = svgp.svgElement(SVGConstants.SVG_G_TAG);

    bar = svgp.svgRect(x, y, width, height);
    //bar.setAttribute(SVGConstants.SVG_TRANSFORM_ATTRIBUTE, SVGConstants.SVG_TRANSLATE_VALUE+"("+x+","+y+")");
    bar.setAttribute(SVGConstants.SVG_FILL_ATTRIBUTE, "#a0a0a0");
    bar.setAttribute(SVGConstants.SVG_STROKE_ATTRIBUTE, "#a0a0a0");
    bar.setAttribute(SVGConstants.SVG_STROKE_WIDTH_ATTRIBUTE, ""+height*0.01);

    if(isHorizontal) {
      length = width;
      this.width = height;
      chart = svgp.svgRect(x+0.02*height, y+0.02*height, 0, height - 0.04*height);
    }
    else {
      length = height;
      this.width = width;
      chart = svgp.svgRect(x+0.02*width, y+0.02*width, width - 0.04*width, 0);
    }

    chart.setAttribute(SVGConstants.SVG_FILL_ATTRIBUTE, "#d4e4f1");
    chart.setAttribute(SVGConstants.SVG_STROKE_ATTRIBUTE, "#a0a0a0");
    chart.setAttribute(SVGConstants.SVG_STROKE_WIDTH_ATTRIBUTE, ""+height*0.01);

    barchart = svgp.svgElement(SVGConstants.SVG_G_TAG);
    barchart.appendChild(bar);
    barchart.appendChild(chart);
  }

  public void setSize(double size) {
    this.size = size;
  }

  public void setFill(double newfill) {
    this.fill = newfill;
    this.fillLength = (fill / size) * length - 0.04*width;
    this.chart.setAttribute(SVGConstants.SVG_WIDTH_ATTRIBUTE, "" + fillLength);
  }

  public void addLabel(String text) {
    if(horizontal) {
      label = svgp.svgText(x + length + 0.2*width, y + 0.75*width, text);
    }
    else {
      label = svgp.svgText(x, y + length + 0.25*width, text);
    }
    label.setAttribute(SVGConstants.SVG_STYLE_ATTRIBUTE, "font-size: "+0.75*width+"; font-weight: normal");
    barchart.appendChild(label);
  }

  public void showValues() {
    if(horizontal) {
      valueLabel = svgp.svgText(x + 0.2*width, y + 0.75*width, "" + FormatUtil.format(fill, FormatUtil.NF4));// + " / " + Math.round(size));
    }
    else {
      valueLabel = svgp.svgText(width + 0.25*width, y, "" + FormatUtil.format(fill, FormatUtil.NF4));// + " / " + Math.round(size));
    }
    valueLabel.setAttribute(SVGConstants.SVG_STYLE_ATTRIBUTE, "font-size: "+0.75*width+"; font-weight: bold");
    barchart.appendChild(valueLabel);
  }

  public Element asElement() {
    return barchart;
  }
}