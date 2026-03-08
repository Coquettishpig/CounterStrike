package src.counterstrike.Guns;

import src.counterstrike.Handler.Game;

public class GunCache
{
    private int ticks;
    private int rounds;
    private final Game game;
    private int ticks_left;
    private int yaw_direction;
    private float accuracy_yaw;
    private float accuracy_pitch;
    private boolean firstShot;
    
    public GunCache(final Game game, final int rounds) {
        this.ticks = 0;
        this.rounds = 0;
        this.ticks_left = 30;
        this.yaw_direction = 0;
        this.accuracy_yaw = 0.0f;
        this.accuracy_pitch = 0.0f;
        this.firstShot = true;
        this.game = game;
        this.rounds = rounds;
    }
    
    public Game getGame() {
        return this.game;
    }
    
    public float getAccuracyYaw() {
        return this.accuracy_yaw;
    }
    
    public float getAccuracyPitch() {
        return this.accuracy_pitch;
    }
    
    public int getYawDirection() {
        return this.yaw_direction;
    }
    
    public void setYawDirection(final int direction) {
        this.yaw_direction = direction;
    }
    
    public int getRounds() {
        return this.rounds;
    }
    
    public int getTicks() {
        return this.ticks;
    }
    
    public int getTicksLeft() {
        return this.ticks_left;
    }
    
    public void setTicksLeft(final int left) {
        this.ticks_left = left;
    }
    
    public boolean isFirstShot() {
        return this.firstShot;
    }
    
    public void setTicks(final int ticks) {
        this.ticks = ticks;
    }
    
    public void setRounds(final int rounds) {
        this.rounds = rounds;
    }
    
    public void setFirstShot(final boolean value) {
        this.firstShot = value;
    }
    
    public void setAccuracyYaw(final float yaw) {
        this.accuracy_yaw = yaw;
    }
    
    public void setAccuracyPitch(final float pitch) {
        this.accuracy_pitch = pitch;
    }
}
