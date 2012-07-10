/*
 * Copyright (C) 2008-12  Bernhard Hobiger
 *
 * This file is part of HoDoKu.
 *
 * HoDoKu is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * HoDoKu is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with HoDoKu. If not, see <http://www.gnu.org/licenses/>.
 */

package sudoku;

import java.awt.Desktop;
import java.net.URI;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * A singleton wrapper for launching websites using an
 * external web browser. The launching process is delegated to
 * <code>java.awt.Desktop</code>.
 * 
 * @author hobiwan
 */
public class MyBrowserLauncher {
    /** The singleton instance. */
    private static MyBrowserLauncher instance = null;
    /** The url of the project web site. */
    private static String HTTP_BASE = "http://hodoku.sourceforge.net/";
    /** <code>true</code>, if Desktop class is supported and html files can be opened. */
    private boolean httpSupported = false;

    /**
     * Creates a new instance. checks wether the platform supports
     * opening URLs in external browsers.
     */
    private MyBrowserLauncher() {
        try {
            if (! Desktop.isDesktopSupported()) {
                httpSupported = false;
                return;
            }
            Desktop desktop = Desktop.getDesktop();
            if (desktop.isSupported(Desktop.Action.BROWSE)) {
                httpSupported = true;
            }
        } catch (Exception ex) {
            Logger.getLogger(MyBrowserLauncher.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    /**
     * Gets the singleton instance.
     * 
     * @return 
     */
    public static MyBrowserLauncher getInstance() {
        if (instance == null) {
            instance = new MyBrowserLauncher();
        }
        return instance;
    }

    /**
     * Displays the user manual.
     */
    public void launchUserManual() {
        String url = HTTP_BASE + "docs.php";
        browse(url);
    }

    /**
     * Displays the solving guide.
     */
    public void launchSolvingGuide() {
        String url = HTTP_BASE + "techniques.php";
        browse(url);
    }

    /**
     * Displays the project homepage.
     */
    public void launchHomePage() {
        String url = HTTP_BASE + "index.php";
        browse(url);
    }
    
    /**
     * Display the tracker
     */
    public void launchTracker() {
        String url = "http://sourceforge.net/p/hodoku/bugs-and-feature-requests/";
        browse(url);
    }
    
    /**
     * Display the help forum
     */
    public void launchForum() {
        String url = "http://sourceforge.net/p/hodoku/discussion/907403/";
        browse(url);
    }

    /**
     * Tries to open the web site given by <code>url</code>
     * with an external web browser.
     * 
     * @param url 
     */
    private void browse(String url) {
        if (! httpSupported) {
            // cant do anything on this platform.
            return;
        }
        try {
            URI uri = new URI(url);
            Desktop.getDesktop().browse(uri);
        } catch (Exception ex) {
            Logger.getLogger(MyBrowserLauncher.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
}
