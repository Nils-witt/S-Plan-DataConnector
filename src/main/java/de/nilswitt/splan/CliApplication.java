/*
 * Copyright (c) 2021. Nils Witt
 */

package de.nilswitt.splan;

import de.nilswitt.splan.FileHandlers.*;
import de.nilswitt.splan.connectors.ApiConnector;
import de.nilswitt.splan.connectors.ConfigConnector;
import de.nilswitt.splan.connectors.FileSystemConnector;
import de.nilswitt.splan.dataModels.Config;
import de.nilswitt.splan.exceptions.InvalidCredentialsException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class CliApplication {

    private final Logger logger = LogManager.getLogger(CliApplication.class);
    private Vertretungsplan vertretungsplan;
    private Stundenplan stundenplan;
    private Klausurplan klausurenplan;
    private VertretungsplanUntis vertretungsplanUntis;
    private StundenplanUntis stundenplanUntis;
    private CustomWatcher customWatcher;
    private Config config;
    private ApiConnector api;
    private Thread watcherThread;


    public CliApplication() {
    }

    public void initApplication() throws InvalidCredentialsException {
        //Creates the runtime directorys
        FileSystemConnector.createDataDirs();

        //loads the config
        config = ConfigConnector.loadConfig();
        //if no or corrupted config
        if (config == null) {
            try {
                //extract the template from the jar
                ConfigConnector.copyDefaultConfig();
                logger.info("Created default config.json");
            } catch (Exception e) {
                e.printStackTrace();
                logger.info("Failed to create default config.json");
            }
            return;
        }
        //validates the Api access token
        if (!ApiConnector.verifyBearer(config.getBearer(), config.getUrl())) {
            //Falls nicht config null setzen.
            logger.warn("Api token invalid");
            throw new InvalidCredentialsException();
        }

        api = new ApiConnector(config);

        vertretungsplan = new Vertretungsplan(api);
        stundenplan = new Stundenplan(api);
        klausurenplan = new Klausurplan(api);
        vertretungsplanUntis = new VertretungsplanUntis(api);
        stundenplanUntis = new StundenplanUntis(api);

        customWatcher = new CustomWatcher(vertretungsplan, vertretungsplanUntis, stundenplan, stundenplanUntis, klausurenplan, config);

        watcherThread = new Thread(customWatcher);
        logger.info("Done initialisation");
    }

    public CustomWatcher getCustomWatcher() {
        return customWatcher;
    }

    public Thread getWatcherThread() {
        return watcherThread;
    }

    /**
     * Replaces the old watcher thread with a new one
     */
    public void resetWatcherThread() {
        customWatcher = new CustomWatcher(vertretungsplan, vertretungsplanUntis, stundenplan, stundenplanUntis, klausurenplan, config);
        watcherThread = new Thread(customWatcher);
    }
}
