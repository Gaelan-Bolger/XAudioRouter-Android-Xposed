package xposed.audiorouter;

public class Rule {

    private String packageName;
    private int stream = -1;

    public Rule(String packageName) {
        this.packageName = packageName;
    }

    public String getPackageName() {
        return packageName;
    }

    public void setStream(int stream) {
        this.stream = stream;
    }

    public int getStream() {
        return stream;
    }
}
