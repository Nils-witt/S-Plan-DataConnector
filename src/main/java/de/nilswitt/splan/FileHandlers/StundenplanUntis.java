/*
 * Copyright (c) 2021. Nils Witt
 */

package de.nilswitt.splan.FileHandlers;

import de.nilswitt.splan.connectors.ApiConnector;
import de.nilswitt.splan.dataModels.Course;
import de.nilswitt.splan.dataModels.Lesson;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;

public class StundenplanUntis {
    private static final Logger logger = LogManager.getLogger(StundenplanUntis.class);
    private final ApiConnector api;

    public StundenplanUntis(ApiConnector api) {
        this.api = api;
    }

    public void readDocument(String document) {
        BufferedReader reader;
        ArrayList<Lesson> lessons = new ArrayList<>();
        try {
            reader = new BufferedReader(new FileReader(document));
            String line = reader.readLine();
            while (line != null) {
                Lesson lesson = new Lesson();

                String[] lessonParts = line.split(";");
                String className = lessonParts[1].replaceAll("\"", "");
                String teacher = lessonParts[2].replaceAll("\"", "");
                String group = lessonParts[3].replaceAll("\"", "");
                String room = lessonParts[4].replaceAll("\"", "");
                int day = Integer.parseInt(lessonParts[5]);
                int lessonNumber = Integer.parseInt(lessonParts[6]);

                lesson.setDay(day);
                lesson.setLessonNumber(lessonNumber);
                lesson.setRoom(room);
                lesson.setTeacher(teacher);

                lesson.setCourse(new Course());
                lesson.getCourse().setGrade(className);
                String[] groupParts = group.split("-");
                if (groupParts.length == 2) {
                    lesson.getCourse().setSubject(groupParts[0]);

                    lesson.getCourse().setGroup(groupParts[1].substring(groupParts[1].length() - 1));
                    if (groupParts[1].contains("LK")) {
                        lesson.getCourse().setGroup("L" + lesson.getCourse().getGroup());
                    }
                }

                if (!className.equals("")) {
                    lessons.add(lesson);
                }
                line = reader.readLine();
            }
            reader.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        api.addLessons(lessons);
    }
}
