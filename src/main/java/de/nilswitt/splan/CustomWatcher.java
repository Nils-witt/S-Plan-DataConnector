/*
 * Copyright (c) 2021. Nils Witt
 */

package de.nilswitt.splan;

import de.nilswitt.splan.FileHandlers.*;
import de.nilswitt.splan.connectors.ConfigConnector;
import de.nilswitt.splan.connectors.FileSystemConnector;
import de.nilswitt.splan.connectors.TrayNotification;
import de.nilswitt.splan.dataModels.Config;
import de.nilswitt.splan.dataModels.VertretungsLesson;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.w3c.dom.Document;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.*;
import java.util.ArrayList;

public class CustomWatcher implements Runnable {
    private static final Logger logger = LogManager.getLogger(CustomWatcher.class);
    private final Vertretungsplan vertretungsplan;
    private final Stundenplan stundenplan;
    private final Klausurplan klausurplan;
    private final Config config;
    private final Path watchPath;
    private final VertretungsplanUntis vertretungsplanUntis;
    private final StundenplanUntis stundenplanUntis;
    private boolean isStarted = false;
    private WatchService watchService;

    public CustomWatcher(Vertretungsplan vertretungsplan, VertretungsplanUntis vertretungsplanUntis, Stundenplan stundenplan, StundenplanUntis stundenplanUntis, Klausurplan klausurplan, Config config) {
        this.vertretungsplan = vertretungsplan;
        this.vertretungsplanUntis = vertretungsplanUntis;
        this.stundenplan = stundenplan;
        this.stundenplanUntis = stundenplanUntis;
        this.klausurplan = klausurplan;
        this.config = config;
        this.watchPath = Path.of(FileSystemConnector.getWorkingDir().concat("/data/watcher"));
    }

    @Override
    public synchronized void run() {
        logger.info("Watcher is starting");
        try {
            startWatcher();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void shutdown() throws IOException {
        logger.info("Watcher is stopping");
        watchService.close();
        isStarted = false;
    }

    public boolean isRunning() {
        return isStarted;
    }

    private void startWatcher() throws IOException, InterruptedException {
        watchService = this.watchPath.getFileSystem().newWatchService();

        this.watchPath.register(watchService,
                StandardWatchEventKinds.ENTRY_CREATE,
                StandardWatchEventKinds.ENTRY_MODIFY);
        logger.info("Watcher started");
        isStarted = true;
        WatchKey key;
        try {
            while (true) {
                key = watchService.take();
                for (WatchEvent<?> event : key.pollEvents()) {
                    logger.info(event.context().toString());
                    if (!event.context().toString().startsWith("~$")) {
                        fileProcessor(event.context().toString());
                    }

                }
                key.reset();
            }
        } catch (ClosedWatchServiceException ex) {
            //No need to handle
        } catch (Exception ex) {
            ex.printStackTrace();
            logger.fatal(ex);
        }
    }

    public void fileProcessor(String changed) {
        try {

            if (changed.endsWith(".xml")) {
                DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
                DocumentBuilder builder = factory.newDocumentBuilder();

                InputStream in = Files.newInputStream(Paths.get(this.watchPath.toString().concat("/").concat(changed)));
                Document document = builder.parse(in);

                String nodeName;
                //Laden der base XML node, anhand dieser kann der Inhaltstyp ermittelt werden
                nodeName = document.getLastChild().getNodeName();
                if (config.getTrayNotifications()) {
                    TrayNotification.display("Änderung erkannt", "Datei: ".concat(nodeName));
                }

                switch (nodeName) {
                    case "vp":
                        logger.info("Vplan");
                        vertretungsplan.readDocument(document);
                        break;
                    case "sp":
                        logger.info("Stundenplan");
                        //stundenplanFileReader(document, logger, config);
                        stundenplan.readDocument(document);
                        break;
                    case "dataroot":
                        logger.info("Klausuren");
                        klausurplan.readDocument(document);
                        logger.info("Done reading; pushing");
                        klausurplan.pushExams();
                        break;
                    default:
                        logger.info(nodeName);
                }

            } else if (changed.endsWith(".xlsx")) {
                logger.info("Excel: " + Paths.get(this.watchPath.toString().concat("/").concat(changed)));
                ArrayList<VertretungsLesson> vertretungsLessons = vertretungsplanUntis.readXslx(Paths.get(this.watchPath.toString().concat("/").concat(changed)).toString());
                vertretungsplanUntis.compareVplanLocalWithApi(vertretungsLessons);
            } else if (changed.toLowerCase().endsWith(".txt")) {
                logger.info("DIF: " + Paths.get(this.watchPath.toString().concat("/").concat(changed)));
                stundenplanUntis.readDocument(Paths.get(this.watchPath.toString().concat("/").concat(changed)).toString());
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
