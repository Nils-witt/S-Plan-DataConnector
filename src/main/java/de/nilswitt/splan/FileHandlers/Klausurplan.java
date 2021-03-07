/*
 * Copyright (c) 2021. Nils Witt
 */

package de.nilswitt.splan.FileHandlers;

import de.nilswitt.splan.connectors.ApiConnector;
import de.nilswitt.splan.dataModels.Klausur;
import de.nilswitt.splan.dataModels.VertretungsLesson;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import java.sql.Timestamp;
import java.time.LocalDate;
import java.util.ArrayList;

public class Klausurplan {
    private static final Logger logger = LogManager.getLogger(Klausurplan.class);
    private final ApiConnector api;
    private final ArrayList<String> lessonsOnServer = new ArrayList<>();
    private final ArrayList<Klausur> exams = new ArrayList<>();
    private ArrayList<VertretungsLesson> vertretungsLessons = new ArrayList<>();
    private ArrayList<String> replacementLessonIds = new ArrayList<>();

    public Klausurplan(ApiConnector api) {
        this.api = api;
    }


    public void readDocument(Document document) {
        int length;


        replacementLessonIds = new ArrayList<>();
        vertretungsLessons = new ArrayList<>();
        lessonsOnServer.clear();
        for (VertretungsLesson vLesson : api.getReplacementLessonByFilter("Klausuraufsicht")) {
            lessonsOnServer.add(vLesson.getReplacementId());
        }
        try {
            //Laden der base node Unterelemente
            NodeList nl = document.getLastChild().getChildNodes();

            length = nl.getLength();

            for (int i = 0; i < length; i++) {
                //Ünerprüfen, dass das Element eine Node ist
                if (nl.item(i).getNodeType() == Node.ELEMENT_NODE) {
                    try {
                        Element el = (Element) nl.item(i);
                        Klausur klausur = new Klausur();

                        String datum = el.getElementsByTagName("datum").item(0).getTextContent();
                        long dateInt = (long) (Integer.parseInt(datum) - 25569) * 86400000;
                        LocalDate date = new Timestamp(dateInt).toLocalDateTime().toLocalDate();
                        klausur.setDate(date.toString());

                        String stufe = el.getElementsByTagName("stufe").item(0).getTextContent();
                        klausur.setGrade(stufe);
                        String room = el.getElementsByTagName("raum").item(0).getTextContent();
                        klausur.setRoom(room);
                        String teacher = el.getElementsByTagName("lehrer").item(0).getTextContent();
                        String[] parts = teacher.split(" ");
                        if (parts.length == 2) {
                            klausur.setTeacher(parts[0]);
                            try {
                                klausur.setStudents(Integer.parseInt(parts[1]));
                            } catch (Exception e) {
                                klausur.setStudents(1);
                            }
                        }

                        String kurs = el.getElementsByTagName("kurs").item(0).getTextContent();
                        parts = kurs.split("-");
                        if (parts.length == 2) {
                            klausur.setGroup(parts[1]);
                            klausur.setSubject(parts[0]);
                        }
                        if (el.getElementsByTagName("anzeigen").getLength() == 1) {
                            klausur.setDisplay(1);
                        } else {
                            klausur.setDisplay(0);
                        }

                        String fromTo = el.getElementsByTagName("stunde").item(0).getTextContent();
                        try {
                            //7:50-9:20
                            parts = fromTo.split("-");

                            if (parts.length == 2) {
                                String[] from = parts[0].split(":");
                                if (from.length != 2) {
                                    from = parts[0].split("\\.");
                                }

                                if (from.length == 2) {
                                    klausur.setFrom(from[0].concat(":").concat(from[1]));
                                }

                                String[] to = parts[1].split(":");
                                if (to.length != 2) {
                                    to = parts[1].split("\\.");
                                }

                                if (to.length == 2) {
                                    klausur.setTo(to[0].concat(":").concat(to[1]));
                                }
                            }
                            exams.add(klausur);
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    } catch (Exception e) {
                        logger.warn("Error reading Element");
                    }
                }
            }
        } catch (Exception e) {
            logger.fatal(e);
            e.printStackTrace();
        }

        api.addVertretungen(vertretungsLessons);
    }

    public void pushExams() {
        api.addExams(exams);
    }
}
