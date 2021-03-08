/*
 * Copyright (c) 2021. Nils Witt
 */

package de.nilswitt.splan.dataModels;

public class Course {
    private String grade;
    private String subject;
    private String group =  "";

    public String getGrade() {
        return grade;
    }

    public void setGrade(String grade) {
        this.grade = grade;
    }

    public String getSubject() {
        return subject;
    }

    public void setSubject(String subject) {
        this.subject = subject;
    }

    public String getGroup() {
        return group;
    }

    public void setGroup(String group) {
        this.group = group;
    }


    public void updateByCourseString(String course) {
        String[] parts = course.split("/ ");
        if (parts.length == 2) {
            grade = parts[0];
            if (subject != null) {
                group = parts[1].substring(subject.length());
                group = group.replaceAll("\\s", "");
            } else {
                group = parts[1];
            }
        } else {
            grade = course;
            group = course;
        }

    }
}
