package no.cantara.jau.eventextraction;

public class NumberedLine {
    final int number;
    final String line;

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
    @Override
    public String toString() {
        return number + ":\t" + line;
    }
}