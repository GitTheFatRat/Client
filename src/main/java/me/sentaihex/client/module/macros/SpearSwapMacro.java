package me.sentaihex.client.module.macros;

import me.sentaihex.client.module.ClientModule;
import me.sentaihex.client.util.InputSimulator;
import com.github.kwhat.jnativehook.keyboard.NativeKeyEvent;

/**
 * Spear Swap Macro
 *
 * Sequence:
 *   1. Switch to prev slot + left click (attack/trigger action)
 *   2. Wait delay1
 *   3. Switch to spear slot (kích hoạt hiệu ứng enchant từ spear)
 *   4. Wait delay2 (rất ngắn để lấy hiệu ứng)
 *   5. Switch back to prev slot thật nhanh
 *
 * Purpose: left click ở prev slot để trigger action, swap nhanh sang spear
 * để kích hoạt enchant effect, rồi về lại prev slot ngay lập tức.
 */
public class SpearSwapMacro extends ClientModule {

    private int slotPrev  = NativeKeyEvent.VC_1; // slot trước (slot sẽ quay về)
    private int slotSpear = NativeKeyEvent.VC_2; // slot chứa spear

    private int delay1 = 55; // left click → switch spear
    private int delay2 = 30; // thời gian ở spear trước khi swap về (ngắn, trước khi cooldown bar kịp render)

    public SpearSwapMacro() {
        super("Spear Swap", "Macro", -1);
    }

    @Override
    public String getDisplayName() {
        return "Spear Swap";
    }

    @Override
    public java.util.Map<String, String> getSlots() {
        java.util.Map<String, String> slots = new java.util.LinkedHashMap<>();
        slots.put("slot1", "Prev Slot");
        slots.put("slot2", "Spear");
        return slots;
    }

    @Override
    public void onEnable()  { System.out.println("[SentaiHex] SpearSwapMacro ON"); }
    @Override
    public void onDisable() { System.out.println("[SentaiHex] SpearSwapMacro OFF"); }

    @Override
    public void execute() throws InterruptedException {
        // Bước 1: switch sang prev slot (ví dụ: 1)
        InputSimulator.pressKey(InputSimulator.nativeToWinVK(slotPrev));

        // Bước 2: left click 1 lần tại prev slot
        InputSimulator.leftClick();
        Thread.sleep(delay1);

        // Bước 3: switch sang spear (không click) → kích hoạt enchant effect
        InputSimulator.pressKey(InputSimulator.nativeToWinVK(slotSpear));
        Thread.sleep(delay2);

        // Bước 4: swap về prev slot ngay trước khi cooldown bar kịp render
        InputSimulator.pressKey(InputSimulator.nativeToWinVK(slotPrev));
    }

    @Override
    public void setDelay(int ms) {
        int safe = Math.max(50, ms);
        setGlobalDelay(safe);
        this.delay1 = safe;
        // delay2 giữ nguyên — người dùng tự chỉnh riêng để swap về thật nhanh
    }

    // ── Getters / Setters ─────────────────────────────────────────────────────

    public int  getSlotPrev()       { return slotPrev; }
    public void setSlotPrev(int k)  { this.slotPrev = k; }

    public int  getSlotSpear()      { return slotSpear; }
    public void setSlotSpear(int k) { this.slotSpear = k; }

    public int  getDelay1()         { return delay1; }
    public void setDelay1(int d)    { this.delay1 = Math.max(50, d); }

    public int  getDelay2()         { return delay2; }
    public void setDelay2(int d)    { this.delay2 = Math.max(1, d); } // cho phép rất ngắn
}