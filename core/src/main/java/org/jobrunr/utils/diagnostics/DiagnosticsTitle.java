package org.jobrunr.utils.diagnostics;

public class DiagnosticsTitle implements DiagnosticsItem {

    private final int level;
    private final String title;

    public DiagnosticsTitle(String title) {
        this(0, title);
    }

    public DiagnosticsTitle(int level, String title) {
        this.level = level;
        this.title = title;
    }

    public DiagnosticsTitle(int shiftLevel, DiagnosticsTitle title) {
        this.level = shiftLevel + title.level;
        this.title = title.title;
    }

    @Override
    public String toMarkdown() {
        return new String(new char[level + 2]).replace("\0", "#") + " " + title + "\n";
    }
}
