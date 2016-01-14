package no.cantara.jau.eventextraction;

public class EventLine {
    final int number;
    final String line;
    private String tag;

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
    public void setTagName(String type) {
        this.tag = type;
    }
    @Override
    public String toString() {
        return tag + "={" + number + ":\t" + line + "}";
    }
}