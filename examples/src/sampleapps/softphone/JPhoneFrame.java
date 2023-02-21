/*
 * JPhoneFrame.java
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

import java.awt.Graphics;
import java.awt.Image;
import java.awt.image.ImageObserver;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;
import ch.ecma.csta.binding.*;

import sampleapps.station.ButtonTypes;

@SuppressWarnings({"serial", "unused"})
public class JPhoneFrame extends javax.swing.JFrame {
	
	SkinSystem skinSystem;
	SkinComponent pushedComponent;
	int paintCount;
	private ButtonList buttonList;
	
	private String currentDisplayText = new String();
	private boolean onHook;
	private ActionListener actionListener;
    
    /** Creates a new instance of JPhoneFrame */    
    public JPhoneFrame(String title, SkinSystem skinSys) {
        super(title);
		this.skinSystem = skinSys;
		this.onHook = true;
    }
    
    public void paint(Graphics g)
    {
        super.paint(g);
        
        // for debugging
        //paintCount++;
        //System.out.println("painting! count: " + paintCount);
        
        // TODO: use an off-screen graphics context to draw more uniformly
        
        skinSystem.drawMode("main", g, this);
        
        drawDisplayText(currentDisplayText);
		drawButtonText();
        setHookState(onHook);
    }
    
    public boolean imageUpdate(Image img, int infoflags, int x, int y, int width, int height) {
    	/* Since Graphics.draw() doesn't necessarily load the image
    	 * all at once, we need to attach an image observer (ourselves).
    	 * By default the frame will repaint on *every* notification,
    	 * which is bad because imageUpdate() may get called several
    	 * times throughout the loading of each image. So we'll override
    	 * it and repaint only when the image loading is actually done.
    	 */    	
    	 
    	/* TODO: look into a better way to encapsulate this into the
    	 * skin system so we don't have to worry about it at all.
    	 * (i.e. like the C++ version - handles image loading for the 
    	 * frame, loading only whe needed).
    	 */
    	
    	if ((infoflags & ImageObserver.ALLBITS) != 0) {
    		paint(getGraphics());    		
    	}
    	return true;
    }
    
    public void pushSkinComponent(SkinComponent component) {
    	Graphics g = getGraphics();
    	
    	if (pushedComponent != null) {
    		skinSystem.pushSkinComponent(g, this, pushedComponent, false);
    		
			if (pushedComponent.name.startsWith("feat"))
				drawButtonText();
    		
    		pushedComponent = null;
    	}
    	
    	if (component != null) {
    		skinSystem.pushSkinComponent(g, this, component, true);
    		pushedComponent = component;
    		
			if (component.name.startsWith("feat"))
				drawButtonText();
    	}
    }
    
    public void handleComponentAction(SkinComponent component) {
		if ((actionListener != null) && (component != null)) {
			actionListener.actionPerformed(new ActionEvent(this, ActionEvent.ACTION_PERFORMED, component.name));
		}
    }
    
    public void drawDisplayText(String display) {
		Graphics g = getGraphics();
		if(g == null)
		{
		    System.out.println("drawDisplayText:  Graphics not available yet");
		    return;
		}
		SkinComponent displayBack = skinSystem.findComponent("back.display");
		skinSystem.drawImage(displayBack.norm, g, this);
			
		currentDisplayText = display;
		
		g.drawString(display, displayBack.norm.x + 6, displayBack.norm.y + 16);
    }
    
	public void drawButtonText() {
		if (buttonList == null)
			return;
			
		Graphics g = getGraphics();
		if(g == null)
		{
		    System.out.println("drawButtonText:  Graphics not available yet");
		    return;
		}
		
		for (int i=0; i < buttonList.getButtonItemCount(); i++) {
			ButtonItem button = buttonList.getButtonItem(i);
	
			int buttonNum = Integer.parseInt(button.getButton());
			int module = (buttonNum & 0x700) >> 8;
			buttonNum = buttonNum & 0xFF;
	
			String label = "";
	
			try {
				label = ButtonTypes.getButtonTypes(Integer.parseInt(button.getButtonFunction())).getName();
			} catch (Exception e) {
				label = "Unknown";
			}
			
			SkinComponent buttonBack = skinSystem.findComponent("feat." + buttonNum);
			if (buttonBack != null) {
				//skinSystem.drawImage(buttonBack.norm, g, this);
				g.drawString(label, buttonBack.norm.x + 6, buttonBack.norm.y2 - 3);
			}
		}
    }
    
    public void setButtonList(ButtonList list) {
    	buttonList = list;
		paint(getGraphics());
    }
    
    public boolean getHookState() { 
    	return onHook; 
    }
    
    public void setHookState(boolean isOnHook) {
    	// swap the image	
    	onHook = isOnHook;
    	
	Graphics g = getGraphics();
	if(g == null)
	{
	    System.out.println("setHookState:  Graphics not available yet");
	    return;
	}
    	
    	SkinImage img;
    	if (onHook)
    		img = skinSystem.findComponent("handset").norm;
    	else
    		img = skinSystem.findComponent("handset.offhook").norm;
    		
    	skinSystem.drawImage(img, g, this);  	
    }
    
    public void setActionListener(ActionListener listener) {
    	actionListener = listener;
    }
    
    public void updateLamp(int lamp, boolean greenLamp, int state) {
    	Graphics g = getGraphics();
	if(g == null)
	{
	    System.out.println("updateLamp:  Graphics not available yet");
	    return;
	}
		// TODO: persist the lamp state between paints
    	
    	// first we need to find the component associated with this lamp
    	String findString = "lamp.";
    	
    	if (greenLamp)
    		findString += "green.";
    	else
    		findString += "red.";
    		
    	findString += lamp;
    	
    	SkinComponent base = skinSystem.findComponent(findString);
    	if (base != null) {
    		if(state != 2)
    		{
				SkinComponent drawLamp;
		    	if (greenLamp)
					drawLamp = skinSystem.findComponent("lamp.green");
		    	else
					drawLamp = skinSystem.findComponent("lamp.red");
					
				if (drawLamp != null) {
					skinSystem.drawImage(drawLamp.norm, g, this, base.norm.x, base.norm.y);	
				}
    		}
    		else
    		{
    			skinSystem.drawImage(base.norm, g, this);
    	
    		}
    	}
    }
}
