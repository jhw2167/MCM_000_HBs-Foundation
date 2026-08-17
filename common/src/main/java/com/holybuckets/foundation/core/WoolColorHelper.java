package com.holybuckets.foundation.core;

import io.netty.util.collection.IntObjectHashMap;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Set;


public class WoolColorHelper {
    private static final IntObjectHashMap<DustParticleOptions> WOOL_DUST_CACHE = new IntObjectHashMap<>();
    private static final IntObjectHashMap<Integer> INT_COLOR_CACHE = new IntObjectHashMap<>();
    private static final HashSet<Block> woolBlocks = new LinkedHashSet<>();

    public static void addDustColorFromWool(Block wool, int id)
    {
        DyeColor color = DyeColor.WHITE;
        for(DyeColor dyeColor : DyeColor.values()) {
            if(dyeColor.getMapColor() == wool.defaultMapColor()) {
                color = dyeColor;
                break;
            }
        }
        //float[] rgb = color.getTextureDiffuseColors();
        Integer colorInt = color.getTextureDiffuseColor();
        WOOL_DUST_CACHE.put(id, new DustParticleOptions(
                new Vector3f(
                        ((colorInt >> 16) & 0xFF) / 255.0f,
                        ((colorInt >> 8) & 0xFF) / 255.0f,
                        (colorInt & 0xFF) / 255.0f
                ),
            1.0f
        ));
        INT_COLOR_CACHE.put(id, colorInt);
    }

    public static DustParticleOptions getDust(int id) {
        return WOOL_DUST_CACHE.get(id);
    }

    public static float[] getWoolColorRGB(int colorId) {
        Vector3f v = getDust(colorId).getColor();
        return new float[] {v.x, v.y, v.z};
    }

    public static int getWoolColorRGBInt(int colorId) {
        return INT_COLOR_CACHE.getOrDefault(colorId, DyeColor.WHITE.getTextureDiffuseColor());
    }

    public static void initWoolColors() {
        woolBlocks.add(Blocks.RED_WOOL);
        woolBlocks.add(Blocks.ORANGE_WOOL);
        woolBlocks.add(Blocks.YELLOW_WOOL);
        woolBlocks.add(Blocks.LIME_WOOL);
        woolBlocks.add(Blocks.GREEN_WOOL);
        woolBlocks.add(Blocks.CYAN_WOOL);
        woolBlocks.add(Blocks.LIGHT_BLUE_WOOL);
        woolBlocks.add(Blocks.BLUE_WOOL);
        woolBlocks.add(Blocks.PURPLE_WOOL);
        woolBlocks.add(Blocks.MAGENTA_WOOL);
        woolBlocks.add(Blocks.PINK_WOOL);
        woolBlocks.add(Blocks.WHITE_WOOL);
        woolBlocks.add(Blocks.LIGHT_GRAY_WOOL);
        woolBlocks.add(Blocks.GRAY_WOOL);
        woolBlocks.add(Blocks.BROWN_WOOL);
        woolBlocks.add(Blocks.BLACK_WOOL);

        int i = 0;
        for (Block wool : woolBlocks) {
            WoolColorHelper.addDustColorFromWool(wool, i++);
        }

    }

    //get woolBlocks
    public static Set<Block> getWoolBlocks() {
        return woolBlocks;
    }

}
//end class
