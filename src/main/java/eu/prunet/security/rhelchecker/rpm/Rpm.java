package eu.prunet.security.rhelchecker.rpm;

public class Rpm {
    private String packageName;
    private long epoch;
    private String arch;
    private String version;
    private String release;
    private String sigid;

    public Rpm(String line) {
        int idx = line.indexOf('<');
        String[] s = line.substring(0, idx).split(" ");
        packageName = s[0];
        epoch = Long.parseLong(s[1]);
        version = s[2];
        release = s[3];
        arch = s[4];
        idx = line.indexOf("Key ID ", idx);
        if (idx != -1) sigid = line.substring(idx + 7 , line.length() - 1);
    }

    public String getPackageName() {
        return packageName;
    }

    public void setPackageName(String packageName) {
        this.packageName = packageName;
    }

    public long getEpoch() {
        return epoch;
    }

    public void setEpoch(long epoch) {
        this.epoch = epoch;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public String getRelease() {
        return release;
    }

    public void setRelease(String release) {
        this.release = release;
    }

    public String getSigid() {
        return sigid;
    }

    public String getArch() {
        return arch;
    }

    public void setArch(String arch) {
        this.arch = arch;
    }

    public void setSigid(String sigid) {
        this.sigid = sigid;
    }


}
