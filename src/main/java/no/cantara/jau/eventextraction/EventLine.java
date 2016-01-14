package no.cantara.jau.eventextraction;

import com.fasterxml.jackson.annotation.JsonFilter;

@JsonFilter("line")
public class EventLine {
    final int number;
    final String line;
    private String tag;
    private String groupName;
    private String fileName;

    EventLine(int number, String line) {
        this.number = number;
        this.line = line;
    }
    public int getNumber() {
        return number;
    }
    public String getLine() {
        return line;
    }
    public String getTag() {
        return tag;
    }

    public String getGroupName() {
        return groupName;
    }

    public String getFileName() {
        return fileName;
    }

    public void setTag(String type) {
        this.tag = type;
    }
    public void setFileName(String fileName) {
        this.fileName = fileName;
    }
    public void setGroupName(String groupName) {
        this.groupName = groupName;
    }

    @Override
    public String toString() {
        return tag + "={" + number + ":\t" + line + "}";
    }
}