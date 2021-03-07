/*
 * Copyright (c) 2021. Nils Witt
 */

package de.nilswitt.splan.FileHandlers;

import com.google.gson.Gson;
import de.nilswitt.splan.connectors.ApiConnector;
import de.nilswitt.splan.dataModels.Course;
import de.nilswitt.splan.dataModels.Lesson;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import java.util.ArrayList;

public class Stundenplan {
    private static final Logger logger = LogManager.getLogger(Stundenplan.class);
    private final ApiConnector api;
    private final Gson gson = new Gson();

    public Stundenplan(ApiConnector api) {
        this.api = api;
    }

    public void readDocument(Document document) {
        int length;

        try {
            //Laden der base node Unterelemente
            NodeList nl = document.getLastChild().getChildNodes();

            length = nl.getLength();
            ArrayList<Lesson> lessons = new ArrayList<>();

            for (int i = 0; i < length; i++) {
                //Ünerprüfen, dass das Element eine Node ist
                if (nl.item(i).getNodeType() == Node.ELEMENT_NODE) {
                    NodeList nodeList = nl.item(i).getChildNodes();
                    String grade = null;
                    for (int header = 0; header < nodeList.getLength(); header++) {
                        if (nodeList.item(header).getNodeType() == Node.ELEMENT_NODE) {
                            Element el = (Element) nodeList.item(header);

                            switch (el.getTagName()) {
                                case "haupt":
                                    //System.out.println("Haupt");
                                    //lessons = hauptToLessons(el, grade);
                                    lessons.addAll(hauptToLessons(el, grade));
                                    break;
                                case "kopf":
                                    grade = kopfToGrade(el);
                                    break;
                                default:
                                    logger.info(el.getTagName());
                            }
                        }
                    }
                }
            }
            api.addLessons(lessons);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    public ArrayList<Lesson> hauptToLessons(Element hauptDOM, String grade) {
        NodeList lessons = hauptDOM.getChildNodes();
        ArrayList<Lesson> lessonArrayList = new ArrayList<>();

        int lessonsLenght = lessons.getLength();

        for (int i = 0; i < lessonsLenght; i++) {
            if (lessons.item(i).getNodeType() == Node.ELEMENT_NODE) {
                int lessonNumber = 0;

                Element lesson = (Element) lessons.item(i);

                NodeList lessonInfos = lesson.getElementsByTagName("stunde");
                int lessonInfoLenght = lessonInfos.getLength();

                for (int lessonInfoCounter = 0; lessonInfoCounter < lessonInfoLenght; lessonInfoCounter++) {
                    if (lessons.item(i).getNodeType() == Node.ELEMENT_NODE) {
                        Element lessonInfo = (Element) lessonInfos.item(lessonInfoCounter);
                        lessonNumber = Integer.parseInt(lessonInfo.getTextContent());
                    }
                }

                NodeList lessonCourses = lesson.getChildNodes();
                int lessonCoursesLength = lessonCourses.getLength();

                for (int lessonCourseCounter = 0; lessonCourseCounter < lessonCoursesLength; lessonCourseCounter++) {
                    if (lessonCourses.item(lessonCourseCounter).getNodeType() == Node.ELEMENT_NODE) {
                        Lesson lessonModel = new Lesson();
                        lessonModel.setCourse(new Course());

                        Element lessonCourse = (Element) lessonCourses.item(lessonCourseCounter);

                        if (lessonCourse.getTagName().startsWith("tag")) {
                            try {
                                lessonModel.getCourse().setSubject(lessonCourse.getElementsByTagName("fach").item(0).getTextContent());
                                lessonModel.setLessonNumber(lessonNumber);
                                lessonModel.setDay(Integer.parseInt(lessonCourse.getTagName().substring(3)));
                                lessonModel.getCourse().setGrade(grade);
                                lessonModel.getCourse().setGroup(grade);

                                if (lessonCourse.getElementsByTagName("gruppe").getLength() > 0) {
                                    lessonModel.getCourse().setGroup(lessonCourse.getElementsByTagName("gruppe").item(0).getTextContent().substring(lessonModel.getCourse().getSubject().length()).replaceAll("\\s", ""));
                                }
                                if (lessonCourse.getElementsByTagName("lehrer").getLength() > 0) {
                                    String teacher = lessonCourse.getElementsByTagName("lehrer").item(0).getTextContent().substring(0, 3);
                                    lessonModel.setTeacher(teacher);
                                }
                                if (lessonCourse.getElementsByTagName("raum").getLength() > 0) {
                                    lessonModel.setRoom(lessonCourse.getElementsByTagName("raum").item(0).getTextContent());
                                }
                                if (!lessonModel.getCourse().getSubject().equals("") && lessonModel.getTeacher() != null) {
                                    lessonArrayList.add(lessonModel);
                                }

                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                        }
                    }
                }
            }
        }
        return lessonArrayList;
    }

    public String kopfToGrade(Element kopf) {
        String grade = null;
        NodeList elements = kopf.getElementsByTagName("titel");
        for (int i = 0; i < elements.getLength(); i++) {
            if (elements.item(i).getNodeType() == Node.ELEMENT_NODE) {
                grade = elements.item(i).getTextContent();
            }
        }
        return grade;
    }
}
