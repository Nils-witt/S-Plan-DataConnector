/*
 * Copyright (c) 2021. Nils Witt
 */

package de.nilswitt.splan.FileHandlers;

import com.google.gson.Gson;
import de.nilswitt.splan.Utils;
import de.nilswitt.splan.connectors.ApiConnector;
import de.nilswitt.splan.connectors.ConfigConnector;
import de.nilswitt.splan.dataModels.Aufsicht;
import de.nilswitt.splan.dataModels.Course;
import de.nilswitt.splan.dataModels.Lesson;
import de.nilswitt.splan.dataModels.VertretungsLesson;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import java.util.ArrayList;
import java.util.List;

public class Vertretungsplan {
    private static final Logger logger = LogManager.getLogger(ConfigConnector.class);
    private final Utils utils = new Utils();
    private final List<VertretungsLesson> lessons = new ArrayList<>();
    private final List<Aufsicht> aufsichten = new ArrayList<>();
    private final ApiConnector api;
    private final Gson gson = new Gson();

    public Vertretungsplan(ApiConnector api) {
        this.api = api;
    }

    public void readDocument(Document document) {
        Lesson[] lessonsApi = api.getLessons();

        lessons.clear();
        String currentDate = "";
        int length;

        try {
            //Laden der base node Unterelemente
            NodeList nl = document.getLastChild().getChildNodes();

            length = nl.getLength();

            for (int i = 0; i < length; i++) {
                //Ünerprüfen, dass das Element eine Node ist
                if (nl.item(i).getNodeType() == Node.ELEMENT_NODE) {
                    Element el = (Element) nl.item(i);
                    /*
                    Jeder Tag hat drei Nodes Kopf, Haupt und Aufsichen. Der Kopf bestimmt das Datum für die Folgenden Haput und Aufsichent Teile.
                    Die Teile Haupt und Aufsichen haben Unterelement, die die jeweiligen Events beschreiben (ein Event pro Vertretung / Aufsicht)
                     */
                    //Typ der Node bestimmen
                    switch (el.getTagName()) {
                        case "kopf":
                            //Das Datum der XML in das Format yyyy-mm-dd konvertieren, wie es von der Api erfordert wird, mithilfe der Utils Klasse.
                            currentDate = utils.convertDate(el.getElementsByTagName("titel").item(0).getTextContent());
                            break;
                        case "haupt":
                            //Laden aller Unterelement = Vertretungen
                            NodeList aktionen = el.getChildNodes();
                            for (int k = 0; k < aktionen.getLength(); k++) {
                                if (aktionen.item(k).getNodeType() == Node.ELEMENT_NODE) {
                                    //Neues Object laden und Daten aus der Node übertragen
                                    VertretungsLesson lesson = new VertretungsLesson();

                                    Element aktion = (Element) aktionen.item(k);

                                    lesson.setLessonNumber(Integer.parseInt(aktion.getElementsByTagName("stunde").item(0).getTextContent()));
                                    lesson.setInfo(aktion.getElementsByTagName("info").item(0).getTextContent());
                                    lesson.setSubject(aktion.getElementsByTagName("vfach").item(0).getTextContent());
                                    String changedTeacher = aktion.getElementsByTagName("vlehrer").item(0).getTextContent();
                                    if ("(".concat(aktion.getElementsByTagName("lehrer").item(0).getTextContent()).concat(")").equals(changedTeacher)) {
                                        lesson.setTeacher("---");
                                    } else {
                                        lesson.setTeacher(changedTeacher);
                                    }

                                    lesson.setRoom(aktion.getElementsByTagName("vraum").item(0).getTextContent());
                                    //Setzen das Datums, das im Kopf ausgelesen wurde
                                    lesson.setDate(currentDate);
                                    //Seperates Splitten der Kursbezeichnung in Stufe, Fach und Gruppe
                                    Course course = new Course();
                                    course.setSubject(aktion.getElementsByTagName("fach").item(0).getTextContent());
                                    course.updateByCourseString(aktion.getElementsByTagName("klasse").item(0).getTextContent());
                                    lesson.setCourse(course);
                                    //Vertretung dem Array aller Vertretungen hinzufügen
                                    for (Lesson apiLesson : lessonsApi) {
                                        if (lesson.getCourse().getGrade().equals(apiLesson.getCourse().getGrade())) {
                                            if (lesson.getCourse().getSubject().equals(apiLesson.getCourse().getSubject())) {
                                                if (lesson.getCourse().getGroup().equals(apiLesson.getCourse().getGroup())) {
                                                    if (lesson.getLessonNumber() == apiLesson.getLessonNumber()) {
                                                        if (lesson.getWeekday() == apiLesson.getDay()) {
                                                            logger.info("Found:" + apiLesson.getId());
                                                            lesson.setLessonId(apiLesson.getId());
                                                            break;
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }

                                    lesson.genReplacementId();
                                    lessons.add(lesson);
                                }
                            }
                            break;
                        case "aufsichten":
                            NodeList nodeAufsichtenChilds = el.getChildNodes();
                            for (int k = 0; k < nodeAufsichtenChilds.getLength(); k++) {
                                if (nodeAufsichtenChilds.item(k).getNodeType() == Node.ELEMENT_NODE) {

                                    Element elementAufsicht = (Element) nodeAufsichtenChilds.item(k);
                                    String aufsichtInfo = elementAufsicht.getElementsByTagName("aufsichtinfo").item(0).getTextContent();

                                    String[] parts = aufsichtInfo.split(" - ");
                                    String[] parts2 = parts[1].split(" {2}--> {2}");

                                    Aufsicht aufsicht = new Aufsicht();

                                    aufsicht.setLocation(parts2[0]);
                                    aufsicht.setTeacher(parts2[1]);
                                    aufsicht.setTime(parts[0]);

                                    aufsichten.add(aufsicht);
                                }
                            }
                            break;
                    }
                }
            }
            //Vertretungen Array als Json String an die Vergleichs Methode übergeben
            compareVplanLocalWithApi(lessons);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void compareVplanLocalWithApi(List<VertretungsLesson> lessons) {
        ArrayList<String> localDates = new ArrayList<>();

        ArrayList<VertretungsLesson> lessonsServer = new ArrayList<>();
        ArrayList<String> lessonsServerIds = new ArrayList<>();
        ArrayList<String> lessonsLocalIds = new ArrayList<>();

        ArrayList<String> removedLessonsIds = new ArrayList<>();
        ArrayList<VertretungsLesson> updatedLessons = new ArrayList<>();

        //Get from all lesson the dates and add them to unique list
        for (VertretungsLesson lesson : lessons) {
            if (!localDates.contains(lesson.getDate())) {
                localDates.add(lesson.getDate());
            }

            lessonsLocalIds.add(lesson.getReplacementId());
        }

        //Load vertretungen for each day
        for (String date : localDates) {

            VertretungsLesson[] vertretungsLesson = api.getVertretungenByDate(date);
            for (VertretungsLesson lesson : vertretungsLesson) {
                lesson.genReplacementId();
                lessonsServerIds.add(lesson.getReplacementId());
                lessonsServer.add(lesson);
            }
        }


        for (VertretungsLesson vertretungsLesson : lessonsServer) {
            //System.out.println(vertretungsLesson.getReplacementId());
            if (!lessonsLocalIds.contains(vertretungsLesson.getReplacementId())) {
                if (!vertretungsLesson.getInfo().equals("Klausuraufsicht")) {
                    //System.out.println("removed: ".concat(vertretungsLesson.getReplacementId()));
                    removedLessonsIds.add(vertretungsLesson.getReplacementId());
                }
            } else {
                int pos = lessonsLocalIds.indexOf(vertretungsLesson.getReplacementId());
                VertretungsLesson localLesson = lessons.get(pos);
                //System.out.println(gson.toJson(localLesson));
                //System.out.println(gson.toJson(vertretungsLesson));
                //TODO add validation of teacher
                if (!(vertretungsLesson.getRoom().equals(localLesson.getRoom()) && vertretungsLesson.getInfo().equals(localLesson.getInfo()) && vertretungsLesson.getSubject().equals(localLesson.getSubject()))) {
                    //System.out.println("updated: ".concat(vertretungsLesson.getReplacementId()));
                    updatedLessons.add(localLesson);
                }
            }
        }
        ArrayList<VertretungsLesson> addedLessons = new ArrayList<>();
        for (VertretungsLesson lesson : lessons) {
            if (!lessonsServerIds.contains(lesson.getReplacementId())) {
                //System.out.println("added: ".concat(lesson.getReplacementId()).concat(",pos: ").concat(String.valueOf(lessonsLocalIds.indexOf(lesson.getReplacementId()))));
                addedLessons.add(lesson);
                //System.out.println(lesson.getReplacementId());
            }

        }

        logger.info("removed:".concat(gson.toJson(removedLessonsIds)));
        logger.info("added:".concat(gson.toJson(addedLessons)));
        logger.info("updated:".concat(gson.toJson(updatedLessons)));


        for (String lessonId : removedLessonsIds) {
            api.deleteVertretung(lessonId);
        }
        /*
        for (VertretungsLesson lesson : updatedLessons) {
           // api.addVertretungen(lesson);
        }
        */
        api.addVertretungen(addedLessons);
        api.addVertretungen(updatedLessons);

    }
}
