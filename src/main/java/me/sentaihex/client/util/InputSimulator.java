package me.sentaihex.client.util;

import com.sun.jna.platform.win32.User32;
import com.sun.jna.platform.win32.WinDef;
import com.sun.jna.platform.win32.WinUser;
import com.github.kwhat.jnativehook.keyboard.NativeKeyEvent;

public class InputSimulator {

    private static final int MOUSEEVENTF_LEFTDOWN  = 0x0002;
    private static final int MOUSEEVENTF_LEFTUP    = 0x0004;
    private static final int MOUSEEVENTF_RIGHTDOWN = 0x0008;
    private static final int MOUSEEVENTF_RIGHTUP   = 0x0010;

    // Gửi key + mouse trong một SendInput call duy nhất
    // → đảm bảo thứ tự tuyệt đối, không bị Windows input queue xáo trộn
    public static void pressKeyThenRightClick(int winVK) {
        // toArray() cấp phát contiguous memory — bắt buộc với JNA struct array
        WinUser.INPUT[] inputs = (WinUser.INPUT[]) new WinUser.INPUT().toArray(4);

        inputs[0].type = new WinDef.DWORD(WinUser.INPUT.INPUT_KEYBOARD);
        inputs[0].input.setType("ki");
        inputs[0].input.ki.wVk = new WinDef.WORD(winVK);
        inputs[0].input.ki.dwFlags = new WinDef.DWORD(0);

        inputs[1].type = new WinDef.DWORD(WinUser.INPUT.INPUT_KEYBOARD);
        inputs[1].input.setType("ki");
        inputs[1].input.ki.wVk = new WinDef.WORD(winVK);
        inputs[1].input.ki.dwFlags = new WinDef.DWORD(WinUser.KEYBDINPUT.KEYEVENTF_KEYUP);

        inputs[2].type = new WinDef.DWORD(WinUser.INPUT.INPUT_MOUSE);
        inputs[2].input.setType("mi");
        inputs[2].input.mi.dwFlags = new WinDef.DWORD(MOUSEEVENTF_RIGHTDOWN);

        inputs[3].type = new WinDef.DWORD(WinUser.INPUT.INPUT_MOUSE);
        inputs[3].input.setType("mi");
        inputs[3].input.mi.dwFlags = new WinDef.DWORD(MOUSEEVENTF_RIGHTUP);

        User32.INSTANCE.SendInput(new WinDef.DWORD(4), inputs, inputs[0].size());
    }

    public static void pressKeyThenLeftClick(int winVK) {
        WinUser.INPUT[] inputs = (WinUser.INPUT[]) new WinUser.INPUT().toArray(4);

        inputs[0].type = new WinDef.DWORD(WinUser.INPUT.INPUT_KEYBOARD);
        inputs[0].input.setType("ki");
        inputs[0].input.ki.wVk = new WinDef.WORD(winVK);
        inputs[0].input.ki.dwFlags = new WinDef.DWORD(0);

        inputs[1].type = new WinDef.DWORD(WinUser.INPUT.INPUT_KEYBOARD);
        inputs[1].input.setType("ki");
        inputs[1].input.ki.wVk = new WinDef.WORD(winVK);
        inputs[1].input.ki.dwFlags = new WinDef.DWORD(WinUser.KEYBDINPUT.KEYEVENTF_KEYUP);

        inputs[2].type = new WinDef.DWORD(WinUser.INPUT.INPUT_MOUSE);
        inputs[2].input.setType("mi");
        inputs[2].input.mi.dwFlags = new WinDef.DWORD(MOUSEEVENTF_LEFTDOWN);

        inputs[3].type = new WinDef.DWORD(WinUser.INPUT.INPUT_MOUSE);
        inputs[3].input.setType("mi");
        inputs[3].input.mi.dwFlags = new WinDef.DWORD(MOUSEEVENTF_LEFTUP);

        User32.INSTANCE.SendInput(new WinDef.DWORD(4), inputs, inputs[0].size());
    }

    // Các method cũ giữ lại để tương thích
    public static void pressKey(int winVK) {
        keyDown(winVK);
        keyUp(winVK);
    }

    public static void keyDown(int winVK) {
        WinUser.INPUT input = new WinUser.INPUT();
        input.type = new WinDef.DWORD(WinUser.INPUT.INPUT_KEYBOARD);
        input.input.setType("ki");
        input.input.ki.wVk = new WinDef.WORD(winVK);
        input.input.ki.dwFlags = new WinDef.DWORD(0);
        User32.INSTANCE.SendInput(new WinDef.DWORD(1), new WinUser.INPUT[]{input}, input.size());
    }

    public static void keyUp(int winVK) {
        WinUser.INPUT input = new WinUser.INPUT();
        input.type = new WinDef.DWORD(WinUser.INPUT.INPUT_KEYBOARD);
        input.input.setType("ki");
        input.input.ki.wVk = new WinDef.WORD(winVK);
        input.input.ki.dwFlags = new WinDef.DWORD(WinUser.KEYBDINPUT.KEYEVENTF_KEYUP);
        User32.INSTANCE.SendInput(new WinDef.DWORD(1), new WinUser.INPUT[]{input}, input.size());
    }

    public static void rightClick() {
        sendMouseEvent(MOUSEEVENTF_RIGHTDOWN);
        sendMouseEvent(MOUSEEVENTF_RIGHTUP);
    }

    public static void leftClick() {
        sendMouseEvent(MOUSEEVENTF_LEFTDOWN);
        sendMouseEvent(MOUSEEVENTF_LEFTUP);
    }

    private static void sendMouseEvent(int flags) {
        WinUser.INPUT input = new WinUser.INPUT();
        input.type = new WinDef.DWORD(WinUser.INPUT.INPUT_MOUSE);
        input.input.setType("mi");
        input.input.mi.dwFlags = new WinDef.DWORD(flags);
        User32.INSTANCE.SendInput(new WinDef.DWORD(1), new WinUser.INPUT[]{input}, input.size());
    }

    public static int nativeToWinVK(int vcCode) {
        return switch (vcCode) {
            case NativeKeyEvent.VC_0 -> 0x30;
            case NativeKeyEvent.VC_1 -> 0x31;
            case NativeKeyEvent.VC_2 -> 0x32;
            case NativeKeyEvent.VC_3 -> 0x33;
            case NativeKeyEvent.VC_4 -> 0x34;
            case NativeKeyEvent.VC_5 -> 0x35;
            case NativeKeyEvent.VC_6 -> 0x36;
            case NativeKeyEvent.VC_7 -> 0x37;
            case NativeKeyEvent.VC_8 -> 0x38;
            case NativeKeyEvent.VC_9 -> 0x39;
            case NativeKeyEvent.VC_A -> 0x41;
            case NativeKeyEvent.VC_B -> 0x42;
            case NativeKeyEvent.VC_C -> 0x43;
            case NativeKeyEvent.VC_D -> 0x44;
            case NativeKeyEvent.VC_E -> 0x45;
            case NativeKeyEvent.VC_F -> 0x46;
            case NativeKeyEvent.VC_G -> 0x47;
            case NativeKeyEvent.VC_H -> 0x48;
            case NativeKeyEvent.VC_I -> 0x49;
            case NativeKeyEvent.VC_J -> 0x4A;
            case NativeKeyEvent.VC_K -> 0x4B;
            case NativeKeyEvent.VC_L -> 0x4C;
            case NativeKeyEvent.VC_M -> 0x4D;
            case NativeKeyEvent.VC_N -> 0x4E;
            case NativeKeyEvent.VC_O -> 0x4F;
            case NativeKeyEvent.VC_P -> 0x50;
            case NativeKeyEvent.VC_Q -> 0x51;
            case NativeKeyEvent.VC_R -> 0x52;
            case NativeKeyEvent.VC_S -> 0x53;
            case NativeKeyEvent.VC_T -> 0x54;
            case NativeKeyEvent.VC_U -> 0x55;
            case NativeKeyEvent.VC_V -> 0x56;
            case NativeKeyEvent.VC_W -> 0x57;
            case NativeKeyEvent.VC_X -> 0x58;
            case NativeKeyEvent.VC_Y -> 0x59;
            case NativeKeyEvent.VC_Z -> 0x5A;
            case NativeKeyEvent.VC_F1  -> 0x70;
            case NativeKeyEvent.VC_F2  -> 0x71;
            case NativeKeyEvent.VC_F3  -> 0x72;
            case NativeKeyEvent.VC_F4  -> 0x73;
            case NativeKeyEvent.VC_F5  -> 0x74;
            case NativeKeyEvent.VC_F6  -> 0x75;
            case NativeKeyEvent.VC_F7  -> 0x76;
            case NativeKeyEvent.VC_F8  -> 0x77;
            case NativeKeyEvent.VC_F9  -> 0x78;
            case NativeKeyEvent.VC_F10 -> 0x79;
            case NativeKeyEvent.VC_F11 -> 0x7A;
            case NativeKeyEvent.VC_F12 -> 0x7B;
            case NativeKeyEvent.VC_ESCAPE    -> 0x1B;
            case NativeKeyEvent.VC_TAB       -> 0x09;
            case NativeKeyEvent.VC_ENTER     -> 0x0D;
            case NativeKeyEvent.VC_BACKSPACE -> 0x08;
            case NativeKeyEvent.VC_SPACE     -> 0x20;
            case NativeKeyEvent.VC_INSERT    -> 0x2D;
            case NativeKeyEvent.VC_DELETE    -> 0x2E;
            case NativeKeyEvent.VC_HOME      -> 0x24;
            case NativeKeyEvent.VC_END       -> 0x23;
            case NativeKeyEvent.VC_PAGE_UP   -> 0x21;
            case NativeKeyEvent.VC_PAGE_DOWN -> 0x22;
            case NativeKeyEvent.VC_LEFT      -> 0x25;
            case NativeKeyEvent.VC_UP        -> 0x26;
            case NativeKeyEvent.VC_RIGHT     -> 0x27;
            case NativeKeyEvent.VC_DOWN      -> 0x28;
            case NativeKeyEvent.VC_SHIFT   -> 0x10;
            case NativeKeyEvent.VC_CONTROL -> 0x11;
            case NativeKeyEvent.VC_ALT     -> 0x12;
            default -> vcCode;
        };
    }
}