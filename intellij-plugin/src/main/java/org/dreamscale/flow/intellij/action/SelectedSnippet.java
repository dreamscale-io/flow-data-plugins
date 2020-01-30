package org.dreamscale.flow.intellij.action;

public class SelectedSnippet {

    private String snippet;

    private String filePath;
    private int lineNumber;

    public String getSnippet() {
        return snippet;
    }

    public void setText(String snippet) {
        this.snippet = snippet;
    }

    public int getLineNumber() {
        return lineNumber;
    }

    public void setLineNumber(int lineNumber) {
        this.lineNumber = lineNumber;
    }

    public String getFilePath() {
        return filePath;
    }

    public void setFilePath(String filePath) {
        this.filePath = filePath;
    }
}
