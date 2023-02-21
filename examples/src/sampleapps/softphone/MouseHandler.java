/*
 * MouseHandler.java
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

import java.awt.event.MouseEvent;
import java.awt.Point;

public class MouseHandler extends  javax.swing.event.MouseInputAdapter {
    
    private SkinSystem skinSystem;
	private Point lastPressPos = new Point();
	private boolean pressingComponent = false;
    
    /** Creates a new instance of MouseHandler */
    public MouseHandler(SkinSystem skinSys) {
    	this.skinSystem = skinSys;
    }
    
    public void mousePressed(MouseEvent e) {
		pressingComponent = false;
    	if (e.getButton() == MouseEvent.BUTTON1) {
			lastPressPos = e.getPoint();
	        
	        // TODO: change to hitTestPush or add handling to
	        // skin system to protect against using null images
	        SkinComponent hitComponent = skinSystem.hitTestComponentsByMode(e.getPoint(), "main");
	        if (hitComponent != null) {
				pressingComponent = true;
				JPhoneFrame frame = (JPhoneFrame)e.getSource();
				frame.pushSkinComponent(hitComponent);
	        }
    	}
    }
    
	public void mouseReleased(MouseEvent e) {
		JPhoneFrame frame = (JPhoneFrame)e.getSource();
		pressingComponent = false;
		frame.pushSkinComponent(null);
		
		SkinComponent hitComponent = skinSystem.hitTestComponentsByMode(e.getPoint(), "main");
		if (hitComponent != null) {
			//System.out.println("Hit Component: " + hitComponent.name + "!");
			pressingComponent = true;
			frame.handleComponentAction(hitComponent);
		}
	}
	
	public void mouseDragged(MouseEvent e) {
		// allow window dragging only if we're not clicking on a component
		if (!pressingComponent) {
			JPhoneFrame frame = (JPhoneFrame)e.getSource();
			Point p = frame.getLocation();
			p.x += e.getX() - lastPressPos.x;
			p.y += e.getY() - lastPressPos.y;
			frame.setLocation(p);
		}
	}
    
}
