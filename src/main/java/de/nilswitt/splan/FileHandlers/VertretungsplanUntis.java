/*
 * Copyright (c) 2021. Nils Witt
 */

package de.nilswitt.splan.FileHandlers;

import com.google.gson.Gson;
import de.nilswitt.splan.connectors.ApiConnector;
import de.nilswitt.splan.dataModels.Course;
import de.nilswitt.splan.dataModels.Lesson;
import de.nilswitt.splan.dataModels.VertretungsLesson;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.xssf.usermodel.XSSFCell;
import org.apache.poi.xssf.usermodel.XSSFRow;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;

public class VertretungsplanUntis {
    private final Logger logger = LogManager.getLogger(VertretungsplanUntis.class);
    private final ArrayList<VertretungsLesson> lessons = new ArrayList<>();
    private final ApiConnector api;
    private final Gson gson = new Gson();

    public VertretungsplanUntis(ApiConnector api) {

        this.api = api;
    }

    public ArrayList<VertretungsLesson> readXslx(String fileLocation) throws IOException {
        Lesson[] lessonsApi = api.getLessons();
        this.logger.info(fileLocation);
        this.logger.info("Starting XSLX read");
        Iterator rows;
        InputStream excelFileToRead;
        XSSFWorkbook wb;
        XSSFSheet sheet;
        XSSFRow row;
        XSSFCell cell;

        try {
            excelFileToRead = new FileInputStream(fileLocation);
        } catch (Exception e) {
            this.logger.warn("Error while opening File", e);
            return null;
        }

        logger.info("opening File");
        wb = new XSSFWorkbook(excelFileToRead);
        logger.info("opening Sheet");
        sheet = wb.getSheetAt(0);
        rows = sheet.rowIterator();
        lessons.clear();
        while (rows.hasNext()) {
            row = (XSSFRow) rows.next();
            cell = row.getCell(0);
            if (cell != null && cell.getCellType() == CellType.NUMERIC) {
                String eventType = row.getCell(1).getStringCellValue();
                if (!eventType.equals("Pausenaufsicht")) {
                    try {

                        String[] lessonNumbers = new String[]{};
                        if (row.getCell(3).getCellType() == CellType.NUMERIC) {
                            lessonNumbers = new String[]{String.valueOf((int) row.getCell(3).getNumericCellValue())};
                        } else if (row.getCell(3).getCellType() == CellType.STRING) {
                            lessonNumbers = row.getCell(3).getStringCellValue().split(" - ");
                        }

                        String date = formatDate(row.getCell(2).getStringCellValue());
                        String[] teachers = row.getCell(5).getStringCellValue().replaceAll("</s>", "").replaceAll("<s>", "").split("→");
                        String[] rooms = new String[2];
                        try {
                            rooms = row.getCell(6).getStringCellValue().replaceAll("</s>", "").replaceAll("<s>", "").split("→");
                        } catch (Exception e) {

                            e.printStackTrace();
                        }
                        String[] subjectGroup = row.getCell(7).getStringCellValue().replaceAll("</s>", "").replaceAll("<s>", "").split("-");
                        if (subjectGroup.length == 1)
                            subjectGroup = row.getCell(7).getStringCellValue().replaceAll("</s>", "").replaceAll("<s>", "").split(" ");
                        String grade = row.getCell(4).getStringCellValue().replaceAll("</s>", "").replaceAll("<s>", "");

                        VertretungsLesson lesson = new VertretungsLesson();

                        if (row.getCell(10) != null) {
                            lesson.setInfo(row.getCell(10).getStringCellValue());
                        } else {
                            lesson.setInfo("");
                        }

                        if (eventType.equals("Raum-Vtr.")) {
                            lesson.setTeacher(teachers[0]);
                            lesson.setSubject(subjectGroup[0]);
                        } else if (teachers.length == 1) {
                            lesson.setTeacher("---");
                            lesson.setSubject("---");
                        } else {
                            lesson.setTeacher(teachers[1]);
                            lesson.setSubject(subjectGroup[0]);
                        }
                        if (rooms.length == 1) {
                            lesson.setRoom("---");
                        } else {
                            lesson.setRoom(rooms[1]);
                        }

                        Course course = new Course();
                        course.setSubject(subjectGroup[0]);
                        course.setGrade(grade);
                        String group = "";

                        if (subjectGroup.length > 1) {
                            group = subjectGroup[1];
                        } else {
                            logger.fatal(gson.toJson(subjectGroup));
                        }

                        if (group.startsWith("GK")) {
                            group = group.substring(2);
                        } else if (group.startsWith("LK")) {
                            group = "L".concat(group.substring(2));
                        }
                        course.setGroup(group);
                        lesson.setCourse(course);

                        if (lesson.getCourse().getSubject().equals("EK")) {
                            lesson.getCourse().setSubject("GO");
                        }
                        lesson.getCourse().setGroup(lesson.getCourse().getGroup().replace("G", ""));

                        lesson.setDateUntis(date);

                        if (lessonNumbers.length == 1) {
                            lesson.setLessonNumber(Integer.parseInt(lessonNumbers[0]));
                            compareLessons(lessonsApi, lesson);
                            if (lesson.getLessonId() == 0) {
                                logger.warn("No lesson Found; for: " + gson.toJson(lesson));
                            }
                            lesson.genReplacementId();
                            if (lesson.getLessonId() != 0) lessons.add(new VertretungsLesson(lesson));
                        } else {
                            for (String lessonNumber : lessonNumbers) {
                                int lessonNum = Integer.parseInt(lessonNumber);
                                lesson.setLessonNumber(lessonNum);
                                compareLessons(lessonsApi, lesson);
                                lesson.genReplacementId();
                                if (lesson.getLessonId() != 0) lessons.add(new VertretungsLesson(lesson));
                            }
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }
        }
        return lessons;
    }

    private void compareLessons(Lesson[] lessonsApi, VertretungsLesson lesson) {
        for (Lesson apiLesson : lessonsApi) {
            if (lesson.getCourse().getGrade().equals(apiLesson.getCourse().getGrade())) {
                if (lesson.getCourse().getSubject().equals(apiLesson.getCourse().getSubject())) {
                    if (lesson.getCourse().getGroup().equals(apiLesson.getCourse().getGroup())) {
                        if (lesson.getLessonNumber() == apiLesson.getLessonNumber()) {
                            if (lesson.getWeekday() == apiLesson.getDay()) {
                                logger.info("Found(FIN):" + apiLesson.getId());
                                lesson.setLessonId(apiLesson.getId());
                                break;
                            }
                        }
                    }
                }
            }
        }
    }


    private String formatDate(String dateIn) throws ParseException {
        SimpleDateFormat formatIn = new SimpleDateFormat("dd.MM");
        SimpleDateFormat formatOutL = new SimpleDateFormat("MM-dd");
        SimpleDateFormat formatOutY = new SimpleDateFormat("yyyy");
        Date currentDate = new Date();
        Date date = formatIn.parse(dateIn);
        return formatOutY.format(currentDate).concat("-").concat(formatOutL.format(date));
    }

    public void compareVplanLocalWithApi(List<VertretungsLesson> lessons) {
        ArrayList<String> localDates = new ArrayList<>();

        ArrayList<VertretungsLesson> lessonsServer = new ArrayList<>();
        ArrayList<String> lessonsServerIds = new ArrayList<>();
        ArrayList<String> lessonsLocalIds = new ArrayList<>();

        ArrayList<String> removedLessonsIds = new ArrayList<>();
        ArrayList<VertretungsLesson> updatedLessons = new ArrayList<>();

        //Get from all lesson the localDates and add them to unique list
        for (VertretungsLesson lesson : lessons) {
            if (!localDates.contains(lesson.getDate())) {
                localDates.add(lesson.getDate());
            }

            lessonsLocalIds.add(lesson.getReplacementId());
        }

        for (String date : localDates) {
            VertretungsLesson[] vertretungsLesson = api.getVertretungenByDate(date);
            for (VertretungsLesson lesson : vertretungsLesson) {
                lesson.genReplacementId();
                lessonsServerIds.add(lesson.getReplacementId());
                lessonsServer.add(lesson);
            }
        }


        for (VertretungsLesson vertretungsLesson : lessonsServer) {
            if (!lessonsLocalIds.contains(vertretungsLesson.getReplacementId())) {
                removedLessonsIds.add(vertretungsLesson.getReplacementId());
            } else {
                int pos = lessonsLocalIds.indexOf(vertretungsLesson.getReplacementId());
                VertretungsLesson localLesson = lessons.get(pos);
                if (!(vertretungsLesson.getRoom().equals(localLesson.getRoom()) && vertretungsLesson.getInfo().equals(localLesson.getInfo()) && vertretungsLesson.getSubject().equals(localLesson.getSubject()))) {
                    updatedLessons.add(localLesson);
                }
            }
        }

        ArrayList<VertretungsLesson> addedLessons = new ArrayList<>();
        for (VertretungsLesson lesson : lessons) {
            if (!lessonsServerIds.contains(lesson.getReplacementId())) {
                addedLessons.add(lesson);
            }
        }

        logger.info("removed:".concat(gson.toJson(removedLessonsIds)));
        logger.info("added:".concat(gson.toJson(addedLessons)));
        logger.info("updated:".concat(gson.toJson(updatedLessons)));

        for (String lessonId : removedLessonsIds) {
            api.deleteVertretung(lessonId);
        }

        api.addVertretungen(addedLessons);
        api.addVertretungen(updatedLessons);
    }
}
