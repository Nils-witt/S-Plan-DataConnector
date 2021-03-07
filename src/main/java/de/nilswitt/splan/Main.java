/*
 * Copyright (c) 2021. Nils Witt
 */

package de.nilswitt.splan;

import de.nilswitt.splan.exceptions.InvalidCredentialsException;
import de.nilswitt.splan.gui.ConsoleGui;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class Main {
    private final static Logger logger = LogManager.getLogger(Main.class);

    /**
     * Main entrypoint into the application
     * @param args {String[]}
     */
    public static void main(String[] args) {
        boolean cli = false;
        try {
            //Check ENV value
            if (System.getenv("GUI").equals("false")) {
                System.out.println("CLI mode");
                cli = true;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        if (cli) {
            startCLI();
        } else {
            startGUI();
        }
    }

    /**
     * Starts the GUI
     */
    public static void startGUI() {
        ConsoleGui.launchGui();
    }

    /**
     * Starts the application without gui
     */
    public static void startCLI() {
        CliApplication cliApplication = new CliApplication();
        try {
            cliApplication.initApplication();
            cliApplication.resetWatcherThread();
            cliApplication.getWatcherThread().start();
        } catch (InvalidCredentialsException invalidCredentialsException) {
            logger.error("Exited(error)");
        }
    }

}
