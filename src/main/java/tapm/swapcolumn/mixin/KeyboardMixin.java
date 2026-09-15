package tapm.swapcolumn.mixin;

import tapm.swapcolumn.client.SwapColumnClient;
import tapm.swapcolumn.client.SwapColumnState;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.KeyboardHandler;
import net.minecraft.client.Minecraft;
import net.minecraft.client.input.KeyEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(KeyboardHandler.class)
public class KeyboardMixin {

    @Inject(method = "keyPress", at = @At("HEAD"), cancellable = true)
    private void swapcolumn$onKeyPress(long window, int action, KeyEvent event,
                                    CallbackInfo ci) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.gui.screen() != null) return;

        if (SwapColumnState.isActive()) {
            if (swapcolumn$handleActiveKey(mc, action, event)) ci.cancel();
        } else if (action == InputConstants.PRESS) {
            if (swapcolumn$handleIdlePress(mc, event)) ci.cancel();
        }
    }

    // Returns false for irrelevant keys so they pass through normally (movement, chat, etc.)
    // Returns true for any swap-related key, consuming the event.
    @Unique
    private boolean swapcolumn$handleActiveKey(Minecraft mc, int action, KeyEvent event) {
        KeyMapping swapKey = SwapColumnClient.swapKeyMapping;
        boolean isSwapKey  = swapKey != null && swapKey.matches(event);
        int hotbarIdx      = swapcolumn$hotbarIndex(mc, event);

        if (!isSwapKey && hotbarIdx < 0) return false;

        switch (action) {
            case InputConstants.PRESS   -> swapcolumn$pressWhileActive(isSwapKey, hotbarIdx);
            case InputConstants.RELEASE -> swapcolumn$releaseWhileActive(isSwapKey, hotbarIdx);
            // REPEAT is consumed silently
        }
        return true;
    }

    @Unique
    private boolean swapcolumn$handleIdlePress(Minecraft mc, KeyEvent event) {
        KeyMapping swapKey = SwapColumnClient.swapKeyMapping;
        int hotbarIdx      = swapcolumn$hotbarIndex(mc, event);

        // Mode 2: custom keybind - open menu for current hotbar slot
        if (swapKey != null && swapKey.matches(event)) {
            return SwapColumnState.openMenu(mc.player.getInventory().getSelectedSlot(), true);
        }

        // Mode 1: pressed the number key of the already-selected slot
        if (hotbarIdx >= 0 && hotbarIdx == mc.player.getInventory().getSelectedSlot()) {
            return SwapColumnState.openMenu(hotbarIdx, false);
        }

        return false;
    }

    @Unique
    private void swapcolumn$pressWhileActive(boolean isSwapKey, int hotbarIdx) {
        if (SwapColumnState.isOpenedWithKeybind()) {
            if (isSwapKey) {
                SwapColumnState.close(); // keybind cancels in keybind-mode
            } else {
                swapcolumn$keybindMenuInput(hotbarIdx);
            }
        } else {
            swapcolumn$numberMenuInput(hotbarIdx);
        }
    }

    // Number mode: key 1 = V=0 (cancel), keys 2–4 = V=1–3
    @Unique
    private void swapcolumn$numberMenuInput(int hotbarIdx) {
        if (hotbarIdx < 0) return; // swap key pressed in number-mode — consume silently
        if (hotbarIdx == 0) {
            SwapColumnState.close(); // V=0 = current hotbar slot = cancel
        } else if (hotbarIdx <= 3 && hotbarIdx <= SwapColumnState.getMenuHeight()) {
            SwapColumnState.executeSwap(hotbarIdx);
        } else {
            SwapColumnState.close();
        }
    }

    // Keybind mode: keys 1–3 map to V=1–3. Cancelling via swap key is handled before this call.
    @Unique
    private void swapcolumn$keybindMenuInput(int hotbarIdx) {
        int v = hotbarIdx + 1; // key 1 V=1, key 2 V=2, key 3 V=3
        if (v <= 3 && v <= SwapColumnState.getMenuHeight()) {
            SwapColumnState.executeSwap(v);
        } else {
            SwapColumnState.close();
        }
    }

    @Unique
    private void swapcolumn$releaseWhileActive(boolean isSwapKey, int hotbarIdx) {
        SwapColumnState.Phase phase = SwapColumnState.getPhase();
        boolean isOpeningKey     = swapcolumn$isOpeningKey(isSwapKey, hotbarIdx);

        if (phase == SwapColumnState.Phase.KEY_HELD && isOpeningKey) {
            SwapColumnState.transitionToMenuWaiting();
        } else if (phase == SwapColumnState.Phase.SCROLL_MODE && isOpeningKey) {
            SwapColumnState.executeSwap(SwapColumnState.getScrollIndex());
        }
    }

    @Unique
    private boolean swapcolumn$isOpeningKey(boolean isSwapKey, int hotbarIdx) {
        return SwapColumnState.isOpenedWithKeybind()
                ? isSwapKey
                : hotbarIdx == SwapColumnState.getHotbarSlot();
    }

    @Unique
    private int swapcolumn$hotbarIndex(Minecraft mc, KeyEvent event) {
        KeyMapping[] slots = mc.options.keyHotbarSlots;
        for (int i = 0; i < slots.length; i++) {
            if (slots[i].matches(event)) return i;
        }
        return -1;
    }
}