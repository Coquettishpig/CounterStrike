package src.counterstrike.Guns;

public class GunReload
{
    private int time;
    private double left;
    
    public GunReload(final int time) {
        this.time = time;
        this.left = time;
    }
    
    public int getMaxTime() {
        return this.time;
    }
    
    public double getLeft() {
        return this.left;
    }
    
    public void setLeft(final int left) {
        this.left = left;
    }
}
