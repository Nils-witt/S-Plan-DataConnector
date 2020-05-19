/*
 * Copyright (c) 2019. Nils Witt
 */

package de.nils_witt.splan;

import com.google.gson.Gson;
import de.nils_witt.splan.dataModels.*;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import java.sql.Timestamp;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.logging.Logger;

public class Klausurplan {
    private Api api;
    private Logger logger;
    private Gson gson = new Gson();
    ArrayList<VertretungsLesson> vertretungsLessons = new ArrayList<>();
    ArrayList<String> replacementLessonIds = new ArrayList<>();
    ArrayList<String> lessonsOnServer = new ArrayList<>();
    ArrayList<Klausur> exams = new ArrayList<>();

    public Klausurplan(Logger logger, Api api) {
        this.logger = logger;
        this.api = api;
    }


    public void readDocument(Document document){
        int length;


        replacementLessonIds = new ArrayList<>();
        vertretungsLessons = new ArrayList<>();
        lessonsOnServer.clear();
        for (VertretungsLesson vLesson : api.getReplacementLessonByFilter("Klausuraufsicht")) {
            lessonsOnServer.add(vLesson.getVertretungsID());
        }
        //System.out.println(gson.toJson(lessonsOnServer));
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
                        LocalDate date = new Timestamp(dateInt).toLocalDateTime().toLocalDate();;
                        klausur.setDate(date.toString());

                        String stufe = el.getElementsByTagName("stufe").item(0).getTextContent();
                        klausur.setGrade(stufe);
                        String room = el.getElementsByTagName("raum").item(0).getTextContent();
                        klausur.setRoom(room);
                        String teacher = null;
                        teacher = el.getElementsByTagName("lehrer").item(0).getTextContent();
                        String[] parts = teacher.split(" ");
                        if(parts.length == 2){
                            klausur.setTeacher(parts[0]);
                            try {
                                klausur.setStudents(Integer.parseInt(parts[1]));
                            }catch (Exception e){
                                klausur.setStudents(1);
                            }

                        }

                        String kurs = el.getElementsByTagName("kurs").item(0).getTextContent();
                        parts = kurs.split("-");
                        if(parts.length == 2){
                            klausur.setGroup(parts[1]);
                            klausur.setSubject(parts[0]);
                        }
                        if(el.getElementsByTagName("anzeigen").getLength() == 1){
                            klausur.setDisplay(1);
                        }else {
                            klausur.setDisplay(0);
                        }

                        String fromTo = el.getElementsByTagName("stunde").item(0).getTextContent();
                        try {
                            //7:50-9:20
                            parts = fromTo.split("-");

                            if(parts.length == 2){
                                String[] from = parts[0].split(":");
                                if(from.length != 2){
                                    from = parts[0].split("\\.");
                                }

                                if(from.length == 2){
                                    klausur.setFrom(from[0].concat(":").concat(from[1]));
                                }

                                String[] to = parts[1].split(":");
                                if(to.length != 2){
                                    to = parts[1].split("\\.");
                                }

                                if(to.length == 2){
                                    klausur.setTo(to[0].concat(":").concat(to[1]));
                                }
                            }
                            exams.add(klausur);
                        } catch (Exception e){
                            e.printStackTrace();
                        }
                        //createKLVert(el);
                    } catch (Exception e){
                        //e.printStackTrace();
                        System.out.println("Error reading Element");
                    }
                }
            }
        } catch (Exception e){
            e.printStackTrace();
        }

        //System.out.println(gson.toJson(vertretungsLessons));
        api.addVertretungen(vertretungsLessons);
        //System.out.println(gson.toJson(lessonsOnServer));
    }


    private void createKLVert(Element el){
        NodeList childs = el.getChildNodes();

        for (int i = 0; i < childs.getLength(); i++) {

            if(childs.item(i).getNodeType() == Node.ELEMENT_NODE){
                Element element = (Element) childs.item(i);

                if(!(element.getTextContent().equals("") || element.getTextContent().equals("/"))){
                    Integer lesson = 0;
                    switch (element.getTagName()){
                        case "eins":
                            lesson = 1;
                            break;
                        case "zwei":
                            lesson = 2;
                            break;
                        case "drei":
                            lesson = 3;
                            break;
                        case "vier":
                            lesson = 4;
                            break;
                        case "fünf":
                            lesson = 5;
                            break;
                        case "sechs":
                            lesson = 6;
                            break;
                        case "sieben":
                            lesson = 7;
                            break;
                        default:
                            //System.out.println(element.getTagName());
                    }
                    if(lesson != 0){
                        String datum = el.getElementsByTagName("datum").item(0).getTextContent();
                        long dateInt = (long) (Integer.parseInt(datum) - 25569) * 86400000;
                        LocalDate date = new Timestamp(dateInt).toLocalDateTime().toLocalDate();
                        LessonRequest lessonRequest = new LessonRequest();
                        lessonRequest.setLesson(String.valueOf(lesson));
                        lessonRequest.setTeacher(element.getTextContent());
                        lessonRequest.setWeekday(String.valueOf(date.getDayOfWeek().getValue()));

                        Lesson[] lessons = api.getLessonByTeacherDayLesson(lessonRequest);
                        if(lessons.length > 0){
                            try {
                                VertretungsLesson vertretungsLesson = new VertretungsLesson();
                                vertretungsLesson.setChangedTeacher("---");
                                vertretungsLesson.setChangedRoom("---");
                                vertretungsLesson.setChangedSubject("---");
                                vertretungsLesson.setDate(date.toString());
                                vertretungsLesson.setGrade(lessons[0].getGrade());
                                vertretungsLesson.setGroup(lessons[0].getGroup());
                                vertretungsLesson.setLesson(String.valueOf(lessons[0].getLesson()));
                                vertretungsLesson.setSubject(lessons[0].getSubject());
                                vertretungsLesson.setInfo("Klausuraufsicht");
                                vertretungsLesson.genVertretungsID();
                                if(!replacementLessonIds.contains(vertretungsLesson.getVertretungsID())){
                                    replacementLessonIds.add(vertretungsLesson.getVertretungsID());

                                    VertretungsLesson[] matchingReplacementLessons = api.getReplacementLessonById(vertretungsLesson.getVertretungsID());
                                    if(matchingReplacementLessons.length == 0){
                                        vertretungsLessons.add(vertretungsLesson);
                                    }
                                    if(lessonsOnServer.contains(vertretungsLesson.getVertretungsID())){
                                        lessonsOnServer.remove(vertretungsLesson.getVertretungsID());
                                    }
                                }
                            }catch (Exception e){
                                e.printStackTrace();
                            }
                        }

                    }
                }
            }
        }
    }

    public void pushExams(){
        api.addExams(exams);
    }
}
