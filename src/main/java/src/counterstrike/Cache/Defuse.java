package src.counterstrike.Cache;

public class Defuse
{
    private int time;
    private final int max;
    
    public Defuse(final int time) {
        this.time = time;
        this.max = time;
    }
    
    public void setTime(final int time) {
        this.time = time;
    }
    
    public int getTime() {
        return this.time;
    }
    
    public int getMax() {
        return this.max;
    }
}
