package com.holybuckets.foundation.AAA;

import com.holybuckets.foundation.HBUtil;
import net.minecraft.core.BlockPos;

import java.util.ArrayList;
import java.util.List;

import static com.holybuckets.foundation.HBUtil.Fast3DArray;
import static com.holybuckets.foundation.HBUtil.TripleInt;
import static com.holybuckets.foundation.HBUtil.ShapeUtil;

public class ShapeGen {

    public static List<BlockPos> hollowCube(BlockPos source, int dim) {
        List<BlockPos> initShape = cuboid(source, dim, dim, dim);

        List<BlockPos> finalShape = new ArrayList<>(initShape.size());
        //add only the outer layer of the cube
        for (BlockPos pos : initShape) {
            if (pos.getX() == source.getX() || pos.getX() == source.getX() + dim - 1 ||
                pos.getY() == source.getY() || pos.getY() == source.getY() + dim - 1 ||
                pos.getZ() == source.getZ() || pos.getZ() == source.getZ() + dim - 1) {
                finalShape.add(pos);
            }
        }
        return finalShape;
    }

    public static List<BlockPos> cuboid(BlockPos source, int x, int y, int z) {
            Fast3DArray cube = ShapeUtil.getCube(x,y,z);
            List<BlockPos> positions = new ArrayList<>(cube.size);
            for (int i = 0; i < cube.size; i++) {
                positions.add(new BlockPos(source.getX() + cube.getX(i),
                        source.getY() + cube.getY(i),
                        source.getZ() + cube.getZ(i)));
            }
            return positions;
    }


}
