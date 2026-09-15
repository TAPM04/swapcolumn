package tapm.swapcolumn.client;

import com.mojang.blaze3d.platform.InputConstants;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.minecraft.client.KeyMapping;
import net.minecraft.resources.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class SwapColumnClient implements ClientModInitializer {

    public static final String MOD_ID = "swapcolumn";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    public static KeyMapping swapKeyMapping;

    @Override
    public void onInitializeClient() {

        LOGGER.info("SwapColumn client initializing...");

        KeyMapping.Category swapcolumnCategory =
                KeyMapping.Category.register(Identifier.fromNamespaceAndPath("swapcolumn", "category"));

        swapKeyMapping = new KeyMapping(
                "key.swapcolumn.swap_column",      // translation key for the keybind name
                InputConstants.Type.KEYBOARD,
                InputConstants.UNKNOWN.getValue(),        // unbound by default
                swapcolumnCategory
        );

        KeyMappingHelper.registerKeyMapping(swapKeyMapping);

        SwapColumnConfig.get();

        // Close the Menu if another pops up
        ClientTickEvents.END_CLIENT_TICK.register(mc -> {
            if (SwapColumnState.isActive() && mc.gui.screen() != null) {
                SwapColumnState.close();
            }
        });

        LOGGER.info("SwapColumn client initialized!");
    }
}
