package me.sentaihex.client.module.macros;

import me.sentaihex.client.module.ClientModule;
import me.sentaihex.client.util.InputSimulator;
import com.github.kwhat.jnativehook.keyboard.NativeKeyEvent;

/**
 * TNT Cart macro using a Fire Bow.
 *
 * Sequence:
 *   1. Switch to bow slot -> hold RMB for chargeMs (200-300ms is enough to release an arrow)
 *   2. Release RMB (arrow fires)
 *   3. Immediately place rail + cart in one batch
 *
 * The arrow ignites the TNT cart on contact.
 */
public class FireBowTNTCartMacro extends ClientModule {

    private int slotFireBow = NativeKeyEvent.VC_7;
    private int slotRail    = NativeKeyEvent.VC_F12;
    private int slotCart    = NativeKeyEvent.VC_F9;

    // How long to hold RMB to charge the bow (ms).
    // 200-300ms is the minimum needed for the arrow to actually fire.
    private int chargeMs = 250;

    // Delay between bow release and placing rail (ms).
    // At < 5 block range: keep this LOW (50-80ms) so cart spawns while arrow is still mid-flight.
    // If cart spawns too late, arrow misses -> no explosion. Tune this value first.
    private int delayAfterShot = 60;

    public FireBowTNTCartMacro() {
        super("TNT Cart (Fire Bow)", "Macro", -1);
    }

    @Override public void onEnable()  { System.out.println("[SentaiHex] FireBowTNTCartMacro ON"); }
    @Override public void onDisable() { System.out.println("[SentaiHex] FireBowTNTCartMacro OFF"); }

    @Override
    public void execute() throws InterruptedException {
        // Close range (<=3 block): arrow arrives in ~20-40ms, far too fast to place cart after shooting.
        // Place rail + cart first so the entity already exists when the arrow hits.

        // Step 1: place rail
        InputSimulator.pressKeyThenRightClick(InputSimulator.nativeToWinVK(slotRail));
        Thread.sleep(50); // minimal gap so rail block registers before cart

        // Step 2: place TNT cart on the rail
        InputSimulator.pressKeyThenRightClick(InputSimulator.nativeToWinVK(slotCart));
        Thread.sleep(delayAfterShot); // let cart spawn packet reach server

        // Step 3: draw fire bow and shoot — arrow spawns while cart is already live
        InputSimulator.pressKeyThenChargeRelease(
                InputSimulator.nativeToWinVK(slotFireBow), chargeMs);
    }

    @Override
    public void setDelay(int ms) {
        int safe = Math.max(50, ms);
        setGlobalDelay(safe);
        this.delayAfterShot = safe;
    }

    // ── Getters / setters ─────────────────────────────────────────────────────
    public int  getSlotFireBow()       { return slotFireBow; }
    public void setSlotFireBow(int k)  { this.slotFireBow = k; }

    public int  getSlotRail()          { return slotRail; }
    public void setSlotRail(int k)     { this.slotRail = k; }

    public int  getSlotCart()          { return slotCart; }
    public void setSlotCart(int k)     { this.slotCart = k; }

    public int  getChargeMs()          { return chargeMs; }
    /** Min 200ms, max 1200ms (full charge). */
    public void setChargeMs(int ms)    { this.chargeMs = Math.max(200, Math.min(1200, ms)); }

    public int  getDelayAfterShot()          { return delayAfterShot; }
    public void setDelayAfterShot(int ms)    { this.delayAfterShot = Math.max(0, ms); }
}