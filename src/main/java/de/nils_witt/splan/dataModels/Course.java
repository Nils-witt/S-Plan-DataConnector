/*
 * Copyright (c) 2019. Nils Witt
 */

package de.nils_witt.splan.dataModels;

public class Course {
    private String grade;
    private String subject;
    private String group;

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
            System.out.println(parts[0]);
            grade = parts[0];
            if (subject != null) {
                group = parts[1].substring(subject.length());
                group = group.replaceAll("\\s", "");
            } else {
                group = parts[1];
            }
        } else {
            System.out.println("E:" + course);
            grade = course;
            group = course;
        }

    }
}