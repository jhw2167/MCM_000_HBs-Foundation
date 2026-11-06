package com.holybuckets.foundation.AAA;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.levelgen.structure.BoundingBox;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static com.holybuckets.foundation.HBUtil.Fast3DArray;

/**
 * Shape generation. The List<BlockPos> methods are centered on source in x/z with their base at source y,
 * built from the Fast3DArray primitives below (formerly HBUtil.ShapeUtil).
 */
public class ShapeGen {

    /**
     * Returns the outer shell of a cube, one block thick
     * @param source - center of the base
     * @param dim - side length
     * @return list of BlockPos representing the hollow cube
     */
    public static List<BlockPos> hollowCube(BlockPos source, int dim) {
        List<BlockPos> initShape = cuboid(source, dim, dim, dim);
        BoundingBox box = BoundingBox.encapsulatingPositions(initShape).orElseThrow();

        List<BlockPos> finalShape = new ArrayList<>(initShape.size());
        //add only the outer layer of the cube
        for (BlockPos pos : initShape) {
            if (pos.getX() == box.minX() || pos.getX() == box.maxX() ||
                pos.getY() == box.minY() || pos.getY() == box.maxY() ||
                pos.getZ() == box.minZ() || pos.getZ() == box.maxZ()) {
                finalShape.add(pos);
            }
        }
        return finalShape;
    }

    /**
     * Returns shape centered at source
     * @param source
     * @param x - size in x direction
     * @param y - size in y direction
     * @param z - size in z direction
     * @return list of BlockPos representing the cuboid
     */
    public static List<BlockPos> cuboid(BlockPos source, int x, int y, int z) {
            Fast3DArray cube = getCube(x,z,y);
            List<BlockPos> positions = new ArrayList<>(cube.size);
            for (int i = 0; i < cube.size; i++) {
                positions.add(new BlockPos(source.getX() + cube.getX(i),
                        source.getY() + cube.getY(i),
                        source.getZ() + cube.getZ(i)));
            }
            return positions;
    }

    /**
     * Returns a flat square centered at source
     * @param source
     * @param length - size in x direction
     * @param width - size in z direction
     * @return list of BlockPos representing the square
     */
    public static List<BlockPos> square(BlockPos source, int length, int width) {
        return offsetAll(source, getSquare(length, width));
    }

    /**
     * Returns a flat circle centered at source
     * @param source
     * @param diameter - width in blocks, odd values are exact
     * @return list of BlockPos representing the circle
     */
    public static List<BlockPos> circle(BlockPos source, int diameter) {
        return offsetAll(source, getCircle((diameter + 1) / 2));
    }

    /**
     * Returns a solid sphere stacked from circles, centered at source in x/z with its base at source y
     * @param source - center of the base
     * @param diameter - width and height in blocks
     * @return list of BlockPos representing the sphere
     */
    public static List<BlockPos> sphere(BlockPos source, int diameter) {
        List<BlockPos> positions = new ArrayList<>();
        double r = diameter / 2.0;
        int c = diameter / 2;
        for (int y = 0; y < diameter; y++) {
            double layerRadius = Math.sqrt(r * r - (y - c) * (y - c));
            positions.addAll(circle(source.above(y), 2 * (int) layerRadius + 1));
        }
        return positions;
    }

    /**
     * Returns the top half of a sphere, flat side down at source y
     * @param source - center of the base
     * @param diameter - width in blocks
     * @return list of BlockPos representing the dome
     */
    public static List<BlockPos> halfSphere(BlockPos source, int diameter) {
        List<BlockPos> positions = new ArrayList<>();
        for (BlockPos pos : sphere(source.below(diameter / 2), diameter)) {
            if (pos.getY() >= source.getY()) positions.add(pos);
        }
        return positions;
    }

    /**
     * Returns the outer shell of a sphere, one block thick
     * @param source - center of the base
     * @param diameter - width and height in blocks
     * @return list of BlockPos representing the hollow sphere
     */
    public static List<BlockPos> hollowSphere(BlockPos source, int diameter) {
        List<BlockPos> solid = sphere(source, diameter);
        Set<BlockPos> lookup = new HashSet<>(solid);
        List<BlockPos> shell = new ArrayList<>();
        for (BlockPos pos : solid) {
            for (Direction dir : Direction.values()) {
                if (!lookup.contains(pos.relative(dir))) {
                    shell.add(pos);
                    break;
                }
            }
        }
        return shell;
    }

    private static List<BlockPos> offsetAll(BlockPos source, Fast3DArray shape) {
        List<BlockPos> positions = new ArrayList<>(shape.size);
        for (int i = 0; i < shape.size; i++) {
            positions.add(source.offset(shape.getX(i), shape.getY(i), shape.getZ(i)));
        }
        return positions;
    }


    //** Fast3DArray primitives


    /**
     * Returns a 3D array of all vertices within a circle of radius r.
     * The y value is always 0. If the provided radius is odd, it will be incremented by 1.  Returns single block at
     * the center for radius of 0 or 1 Returns 3x3 square for radius of 2 or 3.
     * @param radius
     * @return null if radius is less than 0. returns 3x3 square if radius is 2 or 3. Otherwise
     * returns all integer solutions where x^2 + z^2 = r^2 or less
     */
    public static Fast3DArray getCircle(int radius)
    {
        if( radius <= 0 )
            return null;

        Fast3DArray circle = new Fast3DArray(4 * (radius+1) * (radius+1));
        circle.add(0, 0, 0);
        if( radius <= 1 ) {
            return circle;
        }

        //if radius is 2, we just return 3x3 square
        if( radius == 2 )
        {
            for (int x = -1; x <= 1; x++) {
                for (int z = -1; z <= 1; z++) {
                    circle.add(x, 0, z);
                }
            }
            return circle;
        }


        radius -= 1;
        double softness = 0.5;
        double radiusSquared = Math.ceil(radius + softness) * (radius+softness);
        for (int x = -radius; x <= radius; x++)
        {
            //Instead, we want all integer solutions where x^2 + y^2 <= r^2
            for (int z = -radius; z <= radius; z++) {
                if (x * x + z * z <= radiusSquared) {
                    circle.add(x, 0, z);
                }
            }
        }

        return circle;
    }

    /**
     * Returns a 3D array of all vertices within a sphere of radius r.
     * If height < radius*2, the sphere is compressed vertically.
     * If height > radius*2, the sphere is stretched vertically.
     * @param radius Sphere radius
     * @param height Total height of the sphere
     * @return Fast3DArray containing all points, null if invalid params
     */
    public static Fast3DArray getSphere(int radius, int height) 
    {
        if (radius <= 0 || height <= 0) {
            return null;
        }

        // Special case: height < 2 returns a circle
        if (height < 2) {
            return getCircle(radius);
        }

        // Special case: tiny radius
        if (radius < 4) {
            Fast3DArray sphere = new Fast3DArray(height * 9); // Approximate size
            Fast3DArray circle = getCircle(1);
            
            for (int y = 0; y < height; y++) {
                for (int i = 0; i < circle.size; i++) {
                    sphere.add(circle.getX(i), y, circle.getZ(i));
                }
            }
            return sphere;
        }

        Fast3DArray sphere = new Fast3DArray((int) (height * radius * radius * Math.PI*2));
        
        // Generate layers of circles with decreasing radii
        int halfHeight = (int) Math.floor(height / 2);
        double[] radii = new double[halfHeight + 1];
        double deltaRadiusPerHt =  ( (double) radius / height);
        for (int y = 0; y <= halfHeight; y++) {
            if( y == 0)
                radii[y] = radius;
            else
                radii[y] = (radii[y-1] - deltaRadiusPerHt) - 0.01;
        }
        int yAdj = 0;
        for (int y = halfHeight*-1; y <= halfHeight; y++)
        {
            if(height % 2 == 0 && y == 0) {
                yAdj = -1;
                continue;
            }


            Fast3DArray layer = getCircle( (int) Math.ceil(radii[Math.abs(y)]) );
            for (int i = 0; i < layer.size; i++) {
                sphere.add(layer.getX(i), y+ yAdj, layer.getZ(i));
            }
        }

        return sphere;
    }


    /**
     * Returns a 3D array of all vertices within a 2D square of provided length and width
     * The y parameter is always 0.
     * @param length
     * @param width
     * @return
     */
    public static Fast3DArray getSquare(int length, int width)
    {
        if( length <= 0 || width <= 0)
            return null;

        Fast3DArray square = new Fast3DArray(length * width + 1);

       int modL = ( length % 2 == 0) ? 1 : 0;
       int modW = ( width % 2 == 0) ? 1 : 0;

        int xStart = -length / 2 + modL;
        int zStart = -width / 2 + modW;

//consider edge cases for l/w = 1 or 2
        if( length == 1 && width == 1)
        {
            square.add(0, 0, 0);
            return square;
        }

        if( length == 1)
        {
            for( int z = zStart; z <= width / 2; z++)
            {
                square.add(0, 0, z);
            }
            return square;
        }

        if( width == 1)
        {
            for( int x = xStart; x <= length / 2; x++)
            {
                square.add(x, 0, 0);
            }
            return square;
        }

        if( length == 2 && width == 2)
        {
            square.add(-1, 0, -1);
            square.add(-1, 0, 1);
            square.add(1, 0, -1);
            square.add(1, 0, 1);
            return square;
        }

        if( length == 2)
        {
            for( int z = zStart; z <= width / 2; z++)
            {
                square.add(-1, 0, z);
                square.add(1, 0, z);
            }
            return square;
        }

        if( width == 2)
        {
            for( int x = xStart; x <= length / 2; x++)
            {
                square.add(x, 0, -1);
                square.add(x, 0, 1);
            }
            return square;
        }

        for( int x = xStart; x <= length / 2; x++)
        {
            for( int z = zStart; z <= width / 2; z++)
            {
                square.add(x, 0, z);
            }
        }
        //if l==3, xStart = -1, l/2 = 1, that works
        //if l==4, xStart = -2, l/2 = 2, thats 5 verticies, we need 4



        return square;
    }


    public static Fast3DArray getCube(int length, int width, int height)
    {
        if( height <= 0 || length <= 0 || width <= 0)
            return null;

        Fast3DArray cube = new Fast3DArray(height * length * width);

        for (int y = 0; y < height; y++)
        {
            Fast3DArray plane = getSquare(length, width);
            for( int i = 0; i < plane.size; i++)
            {
                cube.add(plane.getX(i), y, plane.getZ(i));
            }
        }

        return cube;
    }

}
