package me.sentaihex.client.module.macros;

import me.sentaihex.client.module.ClientModule;
import me.sentaihex.client.util.InputSimulator;
import com.github.kwhat.jnativehook.keyboard.NativeKeyEvent;

/**
 * TNT Cart macro using a regular crossbow + flint and steel.
 * Since the crossbow has no flame enchant, flint and steel is used
 * to ignite the arrow before firing it at the cart.
 *
 * Sequence:
 *   1. Rail       -> right-click (place powered rail)
 *   2. Cart       -> right-click (place TNT minecart on rail)
 *   3. Flint      -> right-click (ignite the loaded arrow in crossbow)
 *   4. Crossbow   -> right-click (fire the now-burning arrow at the cart)
 */
public class FlintCrossbowTNTCartMacro extends ClientModule {

    private int slotRail     = NativeKeyEvent.VC_F12;
    private int slotCart     = NativeKeyEvent.VC_F9;
    private int slotFlint    = NativeKeyEvent.VC_6;
    private int slotCrossbow = NativeKeyEvent.VC_8;

    private int delay1    = 55;  // rail -> cart
    private int delay2    = 55;  // cart -> flint
    private int aimFlint  = 300; // pause so player can aim at the block to ignite
    private int delay3    = 55;  // flint -> crossbow
    private int aimBow    = 300; // pause so player can aim at the cart to shoot

    public FlintCrossbowTNTCartMacro() {
        super("TNT Cart (Flint + Crossbow)", "Macro", -1);
    }

    @Override
    public String getDisplayName() {
        return "Flint + Crossbow";
    }

    @Override
    public java.util.Map<String, String> getSlots() {
        java.util.Map<String, String> slots = new java.util.LinkedHashMap<>();
        slots.put("slot1", "Rail");
        slots.put("slot2", "Cart");
        slots.put("slot3", "Flint & Steel");
        slots.put("slot4", "Crossbow");
        return slots;
    }

    @Override public void onEnable()  { System.out.println("[SentaiHex] FlintCrossbowTNTCartMacro ON"); }
    @Override public void onDisable() { System.out.println("[SentaiHex] FlintCrossbowTNTCartMacro OFF"); }

    @Override
    public void execute() throws InterruptedException {
        // Step 1: place powered rail
        InputSimulator.pressKeyThenRightClick(InputSimulator.nativeToWinVK(slotRail));
        Thread.sleep(delay1);

        // Step 2: place TNT minecart on the rail
        InputSimulator.pressKeyThenRightClick(InputSimulator.nativeToWinVK(slotCart));
        Thread.sleep(delay2);

        // >>> Aim pause: player moves crosshair to the block to ignite
        Thread.sleep(aimFlint);

        // Step 3: right-click flint and steel to ignite the arrow loaded in crossbow
        InputSimulator.pressKeyThenRightClick(InputSimulator.nativeToWinVK(slotFlint));
        Thread.sleep(delay3);

        // >>> Aim pause: player moves crosshair up to aim at the cart
        Thread.sleep(aimBow);

        // Step 4: fire the burning arrow at the cart
        InputSimulator.pressKeyThenRightClick(InputSimulator.nativeToWinVK(slotCrossbow));
    }

    @Override
    public void setDelay(int ms) {
        int safe = Math.max(50, ms);
        setGlobalDelay(safe);
        this.delay1 = safe;
        this.delay2 = safe;
        this.delay3 = safe;
        // aimFlint and aimBow are intentionally NOT touched here —
        // they are personal aim speed settings, not timing delays.
    }

    // ── Getters / setters ─────────────────────────────────────────────────────
    public int  getSlotRail()          { return slotRail; }
    public void setSlotRail(int k)     { this.slotRail = k; }

    public int  getSlotCart()          { return slotCart; }
    public void setSlotCart(int k)     { this.slotCart = k; }

    public int  getSlotFlint()         { return slotFlint; }
    public void setSlotFlint(int k)    { this.slotFlint = k; }

    public int  getSlotCrossbow()      { return slotCrossbow; }
    public void setSlotCrossbow(int k) { this.slotCrossbow = k; }

    public int  getDelay1()            { return delay1; }
    public void setDelay1(int d)       { this.delay1 = Math.max(50, d); }

    public int  getDelay2()            { return delay2; }
    public void setDelay2(int d)       { this.delay2 = Math.max(50, d); }

    public int  getDelay3()            { return delay3; }
    public void setDelay3(int d)       { this.delay3 = Math.max(50, d); }

    public int  getAimFlint()          { return aimFlint; }
    /** How long to wait after placing cart so player can aim crosshair at block to ignite (ms). */
    public void setAimFlint(int ms)    { this.aimFlint = Math.max(0, ms); }

    public int  getAimBow()            { return aimBow; }
    /** How long to wait after igniting so player can aim crosshair at cart (ms). */
    public void setAimBow(int ms)      { this.aimBow = Math.max(0, ms); }
}