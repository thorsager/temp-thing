/*
 * SkinSystem.java
 * 
 * Copyright (c) 2007 Avaya Inc. All rights reserved.
 * 
 * USE OR INSTALLATION OF THIS SAMPLE DEMONSTRATION SOFTWARE INDICATES THE END
 * USERS ACCEPTANCE OF THE GENERAL LICENSE TERMS AVAILABLE ON THE AVAYA WEBSITE
 * AT http://support.avaya.com/LicenseInfo/ (GENERAL LICENSE TERMS). DO NOT USE
 * THE SOFTWARE IF YOU DO NOT WISH TO BE BOUND BY THE GENERAL LICENSE TERMS. IN
 * ADDITION TO THE GENERAL LICENSE TERMS, THE FOLLOWING ADDITIONAL TERMS AND
 * RESTRICTIONS WILL TAKE PRECEDENCE AND APPLY TO THIS DEMONSTRATION SOFTWARE.
 * 
 * THIS DEMONSTRATION SOFTWARE IS PROVIDED FOR THE SOLE PURPOSE OF DEMONSTRATING
 * HOW TO USE THE SOFTWARE DEVELOPMENT KIT AND MAY NOT BE USED IN A LIVE OR
 * PRODUCTION ENVIRONMENT. THIS DEMONSTRATION SOFTWARE IS PROVIDED ON AN AS IS
 * BASIS, WITHOUT ANY WARRANTIES OR REPRESENTATIONS EXPRESS, IMPLIED, OR
 * STATUTORY, INCLUDING WITHOUT LIMITATION, WARRANTIES OF QUALITY, PERFORMANCE,
 * INFRINGEMENT, MERCHANTABILITY, OR FITNESS FOR A PARTICULAR PURPOSE.
 * 
 * EXCEPT FOR PERSONAL INJURY CLAIMS, WILLFUL MISCONDUCT AND END USERS VIOLATION
 * OF AVAYA OR ITS SUPPLIERS INTELLECTUAL PROPERTY RIGHTS, INCLUDING THROUGH A
 * BREACH OF THE SOFTWARE LICENSE, NEITHER AVAYA, ITS SUPPLIERS NOR END USER
 * SHALL BE LIABLE FOR (i) ANY INCIDENTAL, SPECIAL, STATUTORY, INDIRECT OR
 * CONSEQUENTIAL DAMAGES, OR FOR ANY LOSS OF PROFITS, REVENUE, OR DATA, TOLL
 * FRAUD, OR COST OF COVER AND (ii) DIRECT DAMAGES ARISING UNDER THIS AGREEMENT
 * IN EXCESS OF FIFTY DOLLARS (U.S. $50.00).
 * 
 * To the extent there is a conflict between the General License Terms, your
 * Customer Sales Agreement and the terms and restrictions set forth herein, the
 * terms and restrictions set forth herein shall prevail solely for this Utility
 * Demonstration Software.
 */

package sampleapps.softphone;

import java.util.ArrayList;

import javax.xml.parsers.*;
import org.w3c.dom.*;

import java.awt.Graphics;
import java.awt.image.ImageObserver;
import java.awt.Toolkit;
import java.awt.Point;

public class SkinSystem {
	
	//private final Logger log = Logger.getLogger(getClass().getName());
	public ArrayList<SkinBitmap> bitmaps = new ArrayList<SkinBitmap>();
	public ArrayList<SkinComponent> components = new ArrayList<SkinComponent>();
	
	public void loadSkin(String uri)
	{
		try {
			ClassLoader cl = this.getClass().getClassLoader();			
			DocumentBuilderFactory docFactory = DocumentBuilderFactory.newInstance();
			Document doc = docFactory.newDocumentBuilder().parse(uri);
			int i;
			int x;
			
			bitmaps.clear();
			components.clear();
				
			// get the base path
			NodeList bmpPathList = doc.getElementsByTagName("bitmaps");
			Element bmpElem = (Element)bmpPathList.item(0);
			String baseImgPath = bmpElem.getAttribute("basepath");			
			
			// get the bitmaps
			Toolkit toolkit = Toolkit.getDefaultToolkit();
			NodeList list = doc.getElementsByTagName("bitmap");
			for (i=0; i < list.getLength(); i++) {
				Element elem = (Element)list.item(i);
				SkinBitmap bmp = new SkinBitmap();
				bmp.name = elem.getAttribute("name");
				bmp.file = elem.getAttribute("file");
				// trans color
				
				// get the image file
				bmp.bitmap = toolkit.getImage(cl.getResource(baseImgPath + bmp.file));
				
				bitmaps.add(bmp);
			}
			
			// get the components
			NodeList componentList = doc.getElementsByTagName("component");
			for (i=0; i < componentList.getLength(); i++) {
				Element elem = (Element)componentList.item(i);
				
				SkinComponent component = new SkinComponent();
				component.name = elem.getAttribute("name");
				component.inMode = elem.getAttribute("mode");
				
				if (elem.getAttribute("ignoreinput").equalsIgnoreCase("true"))
					component.ignoreInput = true;
				
				NodeList imgList = elem.getChildNodes();
				for (x=0; x < imgList.getLength(); x++) {
					Node imgNode = imgList.item(x);
					
					// make sure we're dealing with an element node
					if (imgNode.getNodeType() == Node.ELEMENT_NODE) {
						Element imgElem = (Element)imgNode;
						//System.out.println(x + ". [" + imgNode.getNodeName() + "] " + imgNode);
						
						String imgType = imgElem.getAttribute("type");
					
						SkinImage img;
						if (imgType.equalsIgnoreCase("push")) {
							img = component.push;
						} else if (imgType.equalsIgnoreCase("hover")) {
							img = component.hover;
						} else {
							img = component.norm;
						}

						img.active = true;

						// get the source info
						int src_width = -1;
						int src_height = -1;
						NodeList subList = imgElem.getElementsByTagName("source");
						if (subList.getLength() > 0) {
							Element srcElem = (Element)subList.item(0);
							img.bitmapName = srcElem.getAttribute("name");
							img.src_x = Integer.parseInt(srcElem.getAttribute("x"));
							img.src_y = Integer.parseInt(srcElem.getAttribute("y"));
							//src_width = Integer.parseInt(srcElem.getAttribute("width"));
							//src_height = Integer.parseInt(srcElem.getAttribute("height"));
							
							if (srcElem.getAttribute("width").equals("")) {
								src_width = -1;
							} else {
								src_width = Integer.parseInt(srcElem.getAttribute("width"));;
							}

							if (srcElem.getAttribute("height").equals("")) {
								src_height = -1;
							} else {
								src_height = Integer.parseInt(srcElem.getAttribute("height"));
							}
							
							if (src_width != -1)
								img.src_x2 = img.src_x + src_width;
							else
								img.src_x2 = img.src_x + component.norm.srcWidth();
								
							if (src_height != -1)
								img.src_y2 = img.src_y + src_height;
							else
								img.src_y2 = img.src_y + component.norm.srcHeight();
								
						} else {
							continue;
						}
						
						// get the draw rect info
						subList = imgElem.getElementsByTagName("rect");
						if (subList.getLength() > 0) {
							Element drawElem = (Element)subList.item(0);
							
							img.x = Integer.parseInt(drawElem.getAttribute("x"));
							img.y = Integer.parseInt(drawElem.getAttribute("y"));
							
							if (drawElem.getAttribute("width").equals("")) {
								img.x2 = img.x + src_width;
							} else {
								img.x2 = img.x + Integer.parseInt(drawElem.getAttribute("width"));;
							}
							
							if (drawElem.getAttribute("height").equals("")) {
								img.y2 = img.y + src_height;
							} else {
								img.y2 = img.y + Integer.parseInt(drawElem.getAttribute("height"));
							}
							
						} else {
							// use the norm state info
							if (imgType.equalsIgnoreCase("norm")) {
								img.x = 0;
								img.y = 0;
								img.x2 = src_width;
								img.y2 = src_height;
							} else {
								SkinImage normImg = component.norm;
								img.x = normImg.x;
								img.y = normImg.y;
								img.x2 = normImg.x2;
								img.y2 = normImg.y2;
							}
						}
					}					
				}
				
				components.add(component);
			}
		}
		catch (Exception e) {
			System.out.println(e);
			e.printStackTrace();
		}
	}
	
	public SkinBitmap getBitmap(String name) {
		for (int i=0; i < bitmaps.size(); i++) {
			SkinBitmap bmp = bitmaps.get(i);
			if (bmp.name.equalsIgnoreCase(name))
				return bmp;
		}
		return null;
	}
	
	public boolean drawImage(SkinImage img, Graphics g, ImageObserver observer) {
		return drawImage(img, g, observer, img.x, img.y, img.width(), img.height(), img.src_x, img.src_y, img.src_x2, img.src_y2);
	}
	
	public boolean drawImage(SkinImage img, Graphics g, ImageObserver observer, int x, int y) {
		return drawImage(img, g, observer, x, y, img.width(), img.height(), img.src_x, img.src_y, img.src_x2, img.src_y2);
	}
	
	public boolean drawImage(SkinImage img, Graphics g, ImageObserver observer, int x, int y, int cx, int cy, int src_x, int src_y, int src_x2, int src_y2) {
		SkinBitmap bmp = getBitmap(img.bitmapName);
		if (bmp != null) {
			// do trans test
			
			int x2 = x + cx;
			int y2 = y + cy;
			return g.drawImage(bmp.bitmap, x, y, x2, y2, src_x, src_y, src_x2, src_y2, observer);
		}
		
		return false;
	}
	
	public boolean pushSkinComponent(Graphics g, ImageObserver observer, SkinComponent component, boolean pushed) {
		
		// TODO: update to use exceptions
		if (component == null)
			return false;
			
		if (!component.push.active)
			return false;
			
		SkinImage img;
		if (pushed)
			img = component.push;
		else
			img = component.norm;
			
		return drawImage(img, g, observer);
	}
	
	public boolean hoverSkinComponent(Graphics g, ImageObserver observer, SkinComponent component, boolean hovering) {
		
		// TODO: update to use exceptions
		if (component == null)
			return false;
		
		SkinImage img;
		if (hovering)
			img = component.hover;
		else
			img = component.norm;
		
		return drawImage(img, g, observer);
	}
	
	public SkinComponent findComponent(String name) {
		for (int i=0; i < components.size(); i++) {
			SkinComponent component = components.get(i);
			if (component.name.equalsIgnoreCase(name))
				return component;
		}
		
		return null;
	}
	
	public void drawMode(String mode, Graphics g, ImageObserver observer) {
		for (int i=0; i < components.size(); i++) {
			SkinComponent component = components.get(i);
			if ((component.norm.active) && component.isInMode(mode)) {
				drawImage(component.norm, g, observer);		
			}
		}
	}
	
	public Point getOuterBounds() {
		Point pt = new Point();
		
		for (int i=0; i < components.size(); i++) {
			SkinComponent component = components.get(i);
			
			if (component.norm.x2 > pt.x)
				pt.x = component.norm.x2;
				
			if (component.norm.y2 > pt.y)
				pt.y = component.norm.y2;
				
			if (component.push.x2 > pt.x)
				pt.x = component.push.x2;
	
			if (component.push.y2 > pt.y)
				pt.y = component.push.y2;
				
			if (component.hover.x2 > pt.x)
				pt.x = component.hover.x2;
	
			if (component.hover.y2 > pt.y)
				pt.y = component.hover.y2;				
		}
		
		return pt;
	}
	
	public SkinComponent hitTestComponentsByMode(Point point, String mode) {
		
		for (int i=0; i < components.size(); i++) {
			SkinComponent component = components.get(i);
			if ((point.x >= component.norm.x) && (point.x <= component.norm.x2) && (component.isInMode(mode)) && (!component.ignoreInput)) {
				if ((point.y >= component.norm.y) && (point.y <= component.norm.y2)) {
						return component;
				}
			}
		}
		
		return null;
	}
	
	public String toString() {
		String output = new String();		
		int i;
		
		output += "BITMAPS:\n";
		output += "--------\n";
		for (i=0; i < bitmaps.size(); i++) {
			output += bitmaps.get(i) + "\n";
		}
		
		output += "\n\n";
		output += "COMPONENTS:\n";
		output += "-----------\n";
		for (i=0; i < components.size(); i++) {
			output += components.get(i) + "\n";
		}
		
		return output;
	}
}
