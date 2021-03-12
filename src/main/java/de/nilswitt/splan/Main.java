/*
 * Copyright (c) 2021. Nils Witt
 */

package de.nilswitt.splan;

import com.google.gson.Gson;
import de.nilswitt.splan.exceptions.InvalidCredentialsException;
import de.nilswitt.splan.gui.ConsoleGui;
import okhttp3.OkHttpClient;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.logging.Level;

public class Main {
    private final static Logger logger = LogManager.getLogger(Main.class);
    public static Appearance appearance = Appearance.CONSOLE;
    public static boolean onlyFiles = false;
    public static String[] files = new String[]{};

    enum Appearance {
        CONSOLE,
        GUI
    }


    /**
     * Main entrypoint into the application
     *
     * @param args {String[]}
     */
    public static void main(String[] args) {
        java.util.logging.Logger.getLogger(OkHttpClient.class.getName()).setLevel(Level.FINE);
        Gson gson = new Gson();
        if (args.length > 0) {

            for (int i = 0; i < args.length; i++) {
                String arg = args[i];

                if (arg.startsWith("--")) {
                    String key = arg.substring(2);
                    String value = null;
                    if (args.length > (i + 1) && !args[i + 1].startsWith("--")) {
                        value = args[i + 1];
                    }
                    System.out.println(key.concat(";".concat(value)));

                    switch (key){
                        case ("appearance"):
                            if(value == null){
                                appearance = Appearance.CONSOLE;
                            }else if (value.equals("console")){
                                appearance = Appearance.CONSOLE;
                            }else if (value.equals("gui")){
                                appearance = Appearance.GUI;
                            }else{
                                System.out.println("Invalid Appearance parameter");
                            }
                             break;
                        case ("file"):
                            if(value != null){
                                onlyFiles = true;
                                files = value.replaceAll("\"","").split(",");
                            }
                            System.out.println("File: ".concat(value));
                            break;
                        default:
                            break;
                    }
                }
            }
        }
        if (appearance == Appearance.CONSOLE) {
            startCLI();
        } else  if (appearance == Appearance.GUI){
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
            cliApplication.getCustomWatcher().fileProcessor("GPU001.txt");
        } catch (InvalidCredentialsException invalidCredentialsException) {
            logger.error("Exited(error)");
        }
    }

}
