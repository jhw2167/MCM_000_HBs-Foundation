package com.holybuckets.foundation.core;

import io.netty.util.collection.IntObjectHashMap;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import org.joml.Vector3f;

import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Set;


public class WoolColorHelper {
    private static final IntObjectHashMap<DustParticleOptions> WOOL_DUST_CACHE = new IntObjectHashMap<>();

    public static void addDustColorFromWool(Block wool, int id)
    {
        DyeColor color = DyeColor.WHITE;
        for(DyeColor dyeColor : DyeColor.values()) {
            if(dyeColor.getMapColor() == wool.defaultMapColor()) {
                color = dyeColor;
                break;
            }
        }
        float[] rgb = color.getTextureDiffuseColors();
        WOOL_DUST_CACHE.put(id, new DustParticleOptions(
            new Vector3f(rgb[0], rgb[1], rgb[2]),
            1.0f
        ));
    }

    public static DustParticleOptions getDust(int id) {
        return WOOL_DUST_CACHE.get(id);
    }

    public static int getIntColor(int id) {
        Vector3f v = getDust(id).getColor();
        //(red << 16) | (green << 8) | blue;
        return ((int)(v.x * 255) << 16) | ((int)(v.y * 255) << 8) | (int)(v.z * 255);
    }

    public static float[] getWoolColorRGB(int colorId) {
        Vector3f v = getDust(colorId).getColor();
        return new float[] {v.x, v.y, v.z};
    }

    public static void initWoolColors() {
        Set<Block> woolIds = new LinkedHashSet<>();
        woolIds.add(Blocks.RED_WOOL);
        woolIds.add(Blocks.ORANGE_WOOL);
        woolIds.add(Blocks.YELLOW_WOOL);
        woolIds.add(Blocks.LIME_WOOL);
        woolIds.add(Blocks.GREEN_WOOL);
        woolIds.add(Blocks.CYAN_WOOL);
        woolIds.add(Blocks.LIGHT_BLUE_WOOL);
        woolIds.add(Blocks.BLUE_WOOL);
        woolIds.add(Blocks.PURPLE_WOOL);
        woolIds.add(Blocks.MAGENTA_WOOL);
        woolIds.add(Blocks.PINK_WOOL);
        woolIds.add(Blocks.WHITE_WOOL);
        woolIds.add(Blocks.LIGHT_GRAY_WOOL);
        woolIds.add(Blocks.GRAY_WOOL);
        woolIds.add(Blocks.BROWN_WOOL);
        woolIds.add(Blocks.BLACK_WOOL);

        int i = 0;
        for (Block wool : woolIds) {
            WoolColorHelper.addDustColorFromWool(wool, i++);
        }

    }
}
