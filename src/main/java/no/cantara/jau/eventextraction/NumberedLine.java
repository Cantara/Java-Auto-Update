package no.cantara.jau.eventextraction;

public class NumberedLine {
    final int number;
    final String line;
    private String type;

    NumberedLine(int number, String line) {
        this.number = number;
        this.line = line;
    }
    public int getNumber() {
        return number;
    }
    public String getLine() {
        return line;
    }
    public String getType() {
        return type;
    }
    public void setTagName(String type) {
        this.type = type;
    }
    @Override
    public String toString() {
        return type + " " + number + ":\t" + line;
    }
}