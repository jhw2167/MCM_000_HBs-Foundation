package com.holybuckets.foundation.mixin;

import com.holybuckets.foundation.item.FoundationCreativeTab;
import net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Collection;
import java.util.List;

/**
 * Pads Foundation's creative tab with blank slots so each FoundationCreativeTab group starts on its own row.
 * Only the client's displayed list changes; the tab's real contents and the search tab are untouched.
 */
@Mixin(CreativeModeInventoryScreen.class)
public abstract class CreativeModeInventoryScreenMixin {

    @Shadow private static CreativeModeTab selectedTab;
    @Shadow private float scrollOffs;

    @Inject(method = "selectTab", at = @At("RETURN"))
    private void hbs$onSelectTab(CreativeModeTab tab, CallbackInfo ci) {
        hbs$applyRowGaps();
    }

    @Inject(method = "refreshCurrentTabContents", at = @At("RETURN"))
    private void hbs$onRefreshCurrentTabContents(Collection<ItemStack> items, CallbackInfo ci) {
        hbs$applyRowGaps();
    }

    @Unique
    private void hbs$applyRowGaps() {
        if (!FoundationCreativeTab.isFoundationTab(selectedTab)) return;

        CreativeModeInventoryScreen.ItemPickerMenu menu = ((CreativeModeInventoryScreen) (Object) this).getMenu();
        List<ItemStack> padded = FoundationCreativeTab.withRowGaps(List.copyOf(menu.items));
        menu.items.clear();
        menu.items.addAll(padded);
        menu.scrollTo(this.scrollOffs);
    }

}
