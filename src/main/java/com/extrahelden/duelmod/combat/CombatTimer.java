package com.extrahelden.duelmod.combat;

/**
 * Simple tick-based combat timer.
 */
public class CombatTimer {
    private static final int MAX_TICKS = 500 * 20; // 500 Sekunden = 10.000 Ticks
    private int ticks;

    public CombatTimer(int ticks) {
        this.ticks = Math.min(ticks, MAX_TICKS);
    }

    /**
     * Add ticks to this timer.
     *
     * @param extraTicks ticks to add
     */
    public void addTicks(int extraTicks) {
        int before = this.ticks;
        this.ticks = Math.min(this.ticks + extraTicks, MAX_TICKS);
    }

    /**
     * Decrement the timer by one tick.
     *
     * @return {@code true} if timer is still active after ticking
     */
    public boolean tick() {
        if (ticks > 0) {
            ticks--;
        }
        return ticks > 0;
    }

    public boolean isActive() {
        return ticks > 0;
    }

    /**
     * Expose remaining ticks for debugging.
     */
    public int getTicks() {
        return ticks;
    }

    /**
     * Return remaining seconds (useful for logs).
     */
    public int getSeconds() {
        return ticks / 20;
    }
}
