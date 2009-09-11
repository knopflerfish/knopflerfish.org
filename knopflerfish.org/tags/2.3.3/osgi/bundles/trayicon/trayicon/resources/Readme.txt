
Windows Tray Icon
-----------------

Written by Jan Struyf

jan.struyf@cs.kuleuven.ac.be
http://jeans.studentenweb.org/java/trayicon/trayicon.html

RUNNING

* Make sure you have Sun's JDK or JRE 1.1.8, 1.2.x, 1.3.x or 1.4.x
	* If you have another version and it doesn't work, please mail me
	* If you have Microsoft Visual J++, recompile the library using the J++ libs

* Download TrayIcon-xx.ZIP (You can unzip it using WinZip or whatever)
* Install it in a directory of your choice (C:\TrayIcon)
* Open a MS-Dos command prompt and say:
	* If you have Java/JDK
          	cd C:\TrayIcon
        	javac demo\awt\TestTrayIcon.java
          	java demo.awt.TestTrayIcon

	* If you have JRE
        	cd C:\TrayIcon
		    jre -cp . demo.awt.TestTrayIcon

* There is also a SWING demo in demo\swing\SwingTrayIcon.java
        	javac demo\swing\SwingTrayIcon.java
          	java demo.swing.SwingTrayIcon
          	
  The SWING version uses a Swing popup menu, which allows you to have icons in 
  the menu.

* Now the demo app should start.
* If something does not work, don't hesitate to mail me (jan.struyf@cs.kuleuven.ac.be).
	* I'll try to fix it asap :O)

* If you have build an application with TrayIcon (small or big, expensive or free does
  not matter), please let me know the URL.

* If you want to use TrayIcons in several apps then copy TrayIcon12.DLL to your
  WINDOWS/SYSTEM directory.
  
* The demo source in:
  demo\

* The TrayIcon main class in:
  com\jeans\trayicon\WindowsTrayIcon.java
  
* C++ Source code in:
  com\jeans\trayicon\c++\WindowsTrayIcon.cpp
  
* It's possible to include com\jeans\trayicon\* in a JAR file with your app

Quick usage:

// Initialisation
	public static void main(String[] args) {
		try {
			if (WindowsTrayIcon.isRunning("TestApp")) {
				// App already running, show error message and exit
			}
			WindowsTrayIcon.initTrayIcon("TestApp");
			...

// Termination
	...
	WindowsTrayIcon.cleanUp();
	System.exit(0);

// Create Tray Icon
	icon = new WindowsTrayIcon(image, 16, 16);
	icon.setToolTipText("SomeTooltip");
	icon.addActionListener(new SomeActionListener());
	icon.setVisible(true);
	...

See demo/awt/TestTrayIcon.java for more info..
Or contact me :O)


Good luck :O)

/**
 * WindowsTrayIcon
 *
 * Written by Jan Struyf
 *
 *  jan.struyf@cs.kuleuven.ac.be
 *  http://jeans.studentenweb.org/java/trayicon/trayicon.html
 *
 * Changelog
 *
 * Version 1.7.9 (02/03/04)
 *  * Fix for working with Java Web Start
 *  * Swing menu is now set Always-On-Top
 *  * Fix in balloon message that caused VM to crash
 *  * Fixed bug in Balloon message specific to Windows 2000
 * 
 * Version 1.7.8 (11/2/03)
 *  * Unicode support also for 95/98/Me
 *  * Exception added for balloon message
 *
 * Version 1.7.7 (10/23/03)
 *  * Unicode support for native components
 *
 * Version 1.7.6 (08/17/03)
 *  * Support for Balloon Messages
 *  * Fix for Swing menu (menu is removed if user clicks on desktop area)
 *
 * Version 1.7.5 (02/20/03)
 *  * Support for MouseListeners (icon can respond to double clicks)
 *  * TrayIcons are not lost anymore when Explorere crashes ;-)
 *  * JAWT is only used if Swing Menu is used (call initJAWT()!)
 * 
 * Version 1.7.4 (01/08/03)
 *  * setAlwaysOnTop implemented for Swing menu
 *  * SwingTrayIcon demo updated with nice Swing menu
 *
 * Version 1.7.3 (01/03/03)
 *  * Bold and disabled AWT menu item support 
 *  * Fixed bug in keepAlive()  
 *
 * Version 1.7.2 (12/17/02)
 *  * Transparency bug fix for Windows ME 
 *  * Fixed naming problem with setWindowsMessageCallback (introduced in 1.7.1) 
 *
 * Version 1.7.1 (11/01/02)
 *  * Works with Java 1.4 
 *  * Bug fix for setCheck(boolean selected) 
 *  * Dependency on jvm.lib removed (thanks to Justin Chapweske) 
 *  *Includes some support for using a Swing popup menu 
 * 
 * Version pre1.6c (07/16/01)
 *	* Fixed minor compilation warnings reported by the more strict VC++ 6.0
 *
 * Version pre1.6b (12/16/00)
 *	* Fixed memory leak for 'animating icons'
 *      * ReleaseDC -> DeleteDC
 *
 * Version pre1.6 (09/02/00)
 *	* Support for old JDK/JRE 1.1.x
 *	* TrayIcon 1.6 will support Microsoft Visual J++
 *
 * Version 1.5 (07/03/00)
 *	* Tray icon popup menu support
 *	* Added code for sendWindowsMessage()
 *
 * Version 1.4 (06/29/00)
 *	* Added DllMain function to clean init code
 *	* Removed redundant LoadLibrary/FreeLibrary calls
 *	* Added code for isRunning()
 *
 * Version 1.3 (06/09/00)
 *	* Trail bug fix for NT (no public release)
 *		(Patch from 'Daniel Hope <danielh@inform.co.nz>')
 *
 * Version 1.2 (05/03/00)
 *	* Message handler for first icon fixed
 *	* WM_RBUTTONDOWN message handler fixed
 *	* Classes are now unregistered on exit
 *		(Patch from 'Daniel Rejment <daniel@rejment.com>')
 *
 * Version 1.0 (06/29/99)
 *	* Initial release
 *
 * Please mail me if you
 *	- 've found bugs
 *	- like this program
 *	- don't like a particular feature
 *	- would like something to be modified
 *
 * To compile:
 *	- Use the MDP project file in the VC++ IDE
 *	- Use Makefile.vc as in:
 *		VCVARS32
 *		nmake /f makefile.vc
 *
 * I always give it my best shot to make a program useful and solid, but
 * remeber that there is absolutely no warranty for using this program as
 * stated in the following terms:
 *
 * THERE IS NO WARRANTY FOR THIS PROGRAM, TO THE EXTENT PERMITTED BY APPLICABLE
 * LAW. THE COPYRIGHT HOLDER AND/OR OTHER PARTIES WHO MAY HAVE MODIFIED THE
 * PROGRAM, PROVIDE THE PROGRAM "AS IS" WITHOUT WARRANTY OF ANY KIND, EITHER
 * EXPRESSED OR IMPLIED, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES
 * OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE.  THE ENTIRE RISK AS
 * TO THE QUALITY AND PERFORMANCE OF THE PROGRAM IS WITH YOU.  SHOULD THE
 * PROGRAM PROVE DEFECTIVE, YOU ASSUME THE COST OF ALL NECESSARY SERVICING,
 * REPAIR OR CORRECTION.
 *
 * IN NO EVENT UNLESS REQUIRED BY APPLICABLE LAW WILL ANY COPYRIGHT HOLDER,
 * OR ANY OTHER PARTY WHO MAY MODIFY AND/OR REDISTRIBUTE THE PROGRAM,
 * BE LIABLE TO YOU FOR DAMAGES, INCLUDING ANY GENERAL, SPECIAL, INCIDENTAL OR
 * CONSEQUENTIAL DAMAGES ARISING OUT OF THE USE OR INABILITY TO USE THE
 * PROGRAM (INCLUDING BUT NOT LIMITED TO LOSS OF DATA OR DATA BEING RENDERED
 * INACCURATE OR LOSSES SUSTAINED BY YOU OR THIRD PARTIES OR A FAILURE OF THE
 * PROGRAM TO OPERATE WITH ANY OTHER PROGRAMS), EVEN IF SUCH HOLDER OR OTHER
 * PARTY HAS BEEN ADVISED OF THE POSSIBILITY OF SUCH DAMAGES.
 *
 * May the Force be with you... Just compile it & use it!
 */
