package tapm.swapcolumn.mixin;

import tapm.swapcolumn.client.SwapColumnRenderer;
import tapm.swapcolumn.client.SwapColumnState;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.mojang.renderpearl.api.pipeline.RenderPipeline;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.Hud;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Hud.class)
public class InGameHudMixin {

    // Hook to render
    @Inject(method = "extractItemHotbar", at = @At("TAIL"))
    private void swapcolumn$afterHotbar(GuiGraphicsExtractor gfx, DeltaTracker dt,
                                     CallbackInfo ci) {
        if (SwapColumnState.isActive()) {
            SwapColumnRenderer.render(gfx);
        }
    }

    @WrapOperation(
            method = "extractItemHotbar",
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/client/gui/GuiGraphicsExtractor;blitSprite(Lcom/mojang/renderpearl/api/pipeline/RenderPipeline;Lnet/minecraft/resources/Identifier;IIII)V",
                    ordinal = 0)
    )
    private void swapcolumn$wrapHotbarBackground(
            GuiGraphicsExtractor gfx,
            RenderPipeline pipeline,
            Identifier sprite,
            int x, int y, int w, int h,
            Operation<Void> original) {

        if (SwapColumnState.isActive() && !SwapColumnState.isOpenedWithKeybind()) {
            int hSlot = SwapColumnState.getHotbarSlot();
            int slotStart = 1 + hSlot * 20;

            // 1. Left part of hotbar (before active slot)
            if (slotStart > 0) {
                gfx.blitSprite(pipeline, sprite, 182, 22, 0, 0, x, y, slotStart, 22, -1);
            }

            // 2. Active slot: replaced with slice 1 (pixels 1-20, shows "1" in Dandelion)
            gfx.blitSprite(pipeline, sprite, 182, 22, 1, 0, x + slotStart, y, 20, 22, -1);

            // 3. Right part of hotbar (after active slot)
            int rightStart = slotStart + 20;
            int rightWidth = 182 - rightStart;
            if (rightWidth > 0) {
                gfx.blitSprite(pipeline, sprite, 182, 22, rightStart, 0, x + rightStart, y, rightWidth, 22, -1);
            }
        } else {
            original.call(gfx, pipeline, sprite, x, y, w, h);
        }
    }

    // Suppress Hotbar Selection Texture if needed
    @WrapOperation(
            method = "extractItemHotbar",
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/client/gui/GuiGraphicsExtractor;blitSprite(Lcom/mojang/renderpearl/api/pipeline/RenderPipeline;Lnet/minecraft/resources/Identifier;IIII)V",
                    ordinal = 1)
    )
    private void swapcolumn$wrapSelection(
            GuiGraphicsExtractor gfx,
            RenderPipeline pipeline,
            Identifier sprite,
            int x, int y, int w, int h,
            Operation<Void> original) {

        if (SwapColumnState.isActive()) {
            return;
        } else {
            original.call(gfx, pipeline, sprite, x, y, w, h);
        }
    }

    // Suppress specific item rendering we can replace
    @WrapOperation(
            method = "extractItemHotbar",
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/client/gui/Hud;extractSlot(Lnet/minecraft/client/gui/GuiGraphicsExtractor;II Lnet/minecraft/client/DeltaTracker;Lnet/minecraft/world/entity/player/Player;Lnet/minecraft/world/item/ItemStack;I)V",
                    ordinal = -1)   // -1 = alle Aufrufe (wir filtern selbst)
    )
    private void swapcolumn$wrapActiveSlot(
            Hud hud,
            GuiGraphicsExtractor graphics,
            int x,
            int y,
            DeltaTracker deltaTracker,
            Player player,
            ItemStack itemStack,
            int seed,
            Operation<Void> original
    )
    {
        if (SwapColumnState.isActive() && player.getInventory().getSelectedItem() == itemStack) {
            return;
        } else {
            original.call(hud, graphics, x, y, deltaTracker, player, itemStack, seed);
        }
    }
}
