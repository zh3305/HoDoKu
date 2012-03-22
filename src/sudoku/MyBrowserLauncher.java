/*
 * Copyright (C) 2008-11  Bernhard Hobiger
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

import browserlauncher.edu.stanford.ejalbert.BrowserLauncher;
import browserlauncher.edu.stanford.ejalbert.exception.BrowserLaunchingInitializingException;
import browserlauncher.edu.stanford.ejalbert.exception.UnsupportedOperatingSystemException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author hobiwan
 */
public class MyBrowserLauncher {
    private static MyBrowserLauncher instance = null;
    private static String HTTP_BASE = "http://hodoku.sourceforge.net/";
    //private static String HTTP_BASE = "http://localhost/";
    //private String language;
    private BrowserLauncher launcher;

    private MyBrowserLauncher() {
        //language = Locale.getDefault().getLanguage();
        try {
            launcher = new BrowserLauncher();
        } catch (BrowserLaunchingInitializingException ex) {
            Logger.getLogger(MyBrowserLauncher.class.getName()).log(Level.SEVERE, null, ex);
        } catch (UnsupportedOperatingSystemException ex) {
            Logger.getLogger(MyBrowserLauncher.class.getName()).log(Level.SEVERE, null, ex);
        }
        //System.out.println("MyBrowserLauncher: " + language + "/" + launcher);
    }

    public static MyBrowserLauncher getInstance() {
        if (instance == null) {
            instance = new MyBrowserLauncher();
        }
        return instance;
    }

    public void launchUserManual() {
        String url = HTTP_BASE + "docs.php";
        launcher.openURLinBrowser(url);
    }

    public void launchSolvingGuide() {
        String url = HTTP_BASE + "techniques.php";
        launcher.openURLinBrowser(url);
    }

    public void launchHomePage() {
        String url = HTTP_BASE + "index.php";
        launcher.openURLinBrowser(url);
    }
}
