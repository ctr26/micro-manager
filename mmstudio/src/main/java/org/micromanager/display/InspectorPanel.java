///////////////////////////////////////////////////////////////////////////////
//PROJECT:       Micro-Manager
//SUBSYSTEM:     Display implementation
//-----------------------------------------------------------------------------
//
// AUTHOR:       Chris Weisiger, 2015
//
// COPYRIGHT:    University of California, San Francisco, 2015
//
// LICENSE:      This file is distributed under the BSD license.
//               License text is included with the source distribution.
//
//               This file is distributed in the hope that it will be useful,
//               but WITHOUT ANY WARRANTY; without even the implied warranty
//               of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
//
//               IN NO EVENT SHALL THE COPYRIGHT OWNER OR
//               CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT,
//               INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES.

package org.micromanager.display;

import javax.swing.JPopupMenu;
import javax.swing.JPanel;

/**
 * An InspectorPanel is a single component in the inspector window. 
 */
public abstract class InspectorPanel extends JPanel {
   /**
    * Provide a menu used for miscellaneous controls. This menu will be
    * attached to a "gear button" visible in the title bar, when this
    * panel is currently open. May be null, in which case no gear button will
    * be shown. This method is called when the panel is first added to the
    * inspector to determine if the button should be used; then when the
    * button is clicked, the method is called again, in case you need to
    * re-generate your menu based on context (e.g. changes in display
    * settings).
    * @return GearMenu
    */
   public JPopupMenu getGearMenu() {
      return null;
   }

   /**
    * Set the Inspector instance. This is only needed if your class needs to
    * be able to invoke the methods exposed in the Inspector interface.
    * @param inspector An implementation of the Inspector interface.
    */
   public void setInspector(Inspector inspector) {}

   /**
    * This enum defines what level of detail this InspectorPanel needs about
    * the display in order to operate successfully.
    */
   public enum DisplayRequirement {
      DATA_VIEWER, DISPLAY_WINDOW
   }

   /**
    * Return the level of detail this InspectorPanel needs of its current
    * display in order to be able to perform its tasks.
    * If a panel with a DISPLAY_WINDOW level of DisplayRequirement is added to
    * the inspector window, and the inspector only has a DataViewer, then the
    * panel will be hidden. Thus, depending on whether you only require the
    * methods exposed in DataViewer or you need the "complete" API in
    * DisplayWindow, and depending on the context in which the inspector
    * window is operating, your panel may not always appear.
    * @return DISPLAY_WINDOW for panels that need access to the DisplayWindow
    *         API, DATA_VIEWER for panels that don't.
    */
   public DisplayRequirement getDisplayRequirement() {
      return DisplayRequirement.DATA_VIEWER;
   }

   /**
    * Receive a new DataViewer. For panels whose contents change in response
    * to the DataViewer that is currently "active", this method must be
    * overridden to update those contents. This method will only ever be
    * called if getDisplayRequirement() returns DisplayRequirement.DATA_VIEWER.
    * @param viewer The newly-active DataViewer. This may be null, in which
    *        case no DataViewer is available.
    */
   public void setDataViewer(DataViewer viewer) {}

   /**
    * Receive a new DisplayWindow. For panels whose contents change in response
    * to the DisplayWindow that is currently "active", this method must be
    * overridden to update those contents. This method will only ever be
    * called if getDisplayRequirement() returns
    * DisplayRequirement.DISPLAY_WINDOW.
    * @param display The newly-active DisplayWindow. This may be null, in which
    *        case no DisplayWindow is available.
    */
   public void setDisplayWindow(DisplayWindow display) {}

   /**
    * Release resources and unregister for events, because the Inspector that
    * this panel is a part of is about to be destroyed. For example, if your
    * InspectorPanel is registered to a DisplayWindow's event system, then it
    * needs to unregister in this method.
    */
   public abstract void cleanup();
}
