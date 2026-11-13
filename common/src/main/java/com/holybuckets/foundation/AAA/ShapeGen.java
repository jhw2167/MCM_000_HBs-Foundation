package com.holybuckets.foundation.AAA;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.levelgen.structure.BoundingBox;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import static com.holybuckets.foundation.HBUtil.Fast3DArray;

/**
 * Shape positions relative to a source block. A shape n wide spans -(n-1)/2 to n/2 on that axis, so odd sizes
 * center on the source and even sizes center on the seam half a block toward +x/+z, with two center columns.
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
     * @param diameter - width in blocks, even values sit half a block toward +x/+z
     * @return list of BlockPos representing the circle
     */
    public static List<BlockPos> circle(BlockPos source, int diameter) {
        if (diameter % 2 == 0) return ellipse(source, diameter, diameter);
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
        double c = (diameter - 1) / 2.0;
        for (int y = 0; y < diameter; y++) {
            double layerRadius = Math.sqrt(Math.max(0, r * r - (y - c) * (y - c)));
            // Layer widths share the diameter's parity so every layer stays centered
            int width = (diameter % 2 == 1) ? 2 * (int) layerRadius + 1 : Math.max(2, 2 * (int) Math.round(layerRadius));
            positions.addAll(circle(source.above(y), width));
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
        return shell(sphere(source, diameter));
    }

    /**
     * Returns a flat ellipse centered at source
     * @param source
     * @param xSize - width in x direction
     * @param zSize - width in z direction
     * @return list of BlockPos representing the ellipse
     */
    public static List<BlockPos> ellipse(BlockPos source, int xSize, int zSize) {
        double cx = ((xSize / 2) - (xSize - 1) / 2) / 2.0;
        double cz = ((zSize / 2) - (zSize - 1) / 2) / 2.0;
        double rx = xSize / 2.0;
        double rz = zSize / 2.0;
        List<BlockPos> positions = new ArrayList<>();
        for (int x = -(xSize - 1) / 2; x <= xSize / 2; x++) {
            for (int z = -(zSize - 1) / 2; z <= zSize / 2; z++) {
                double dx = (x - cx) / rx;
                double dz = (z - cz) / rz;
                if (dx * dx + dz * dz <= 1.0) positions.add(source.offset(x, 0, z));
            }
        }
        return positions;
    }

    /**
     * Returns a flat isosceles triangle with its base along x at the low z edge and its point at the high z edge.
     * Even bases end in a two block point so both sides stay mirrored.
     * @param source
     * @param base - width of the base in x direction
     * @param depth - length from base to point in z direction
     * @return list of BlockPos representing the triangle
     */
    public static List<BlockPos> triangle(BlockPos source, int base, int depth) {
        return trapezoid(source, base, pointWidth(base), depth);
    }

    /**
     * Returns a flat isosceles trapezoid with its base along x at the low z edge and its top at the high z edge
     * @param source
     * @param base - width of the base in x direction
     * @param top - width of the top in x direction, nudged to match the base's parity
     * @param depth - length from base to top in z direction
     * @return list of BlockPos representing the trapezoid
     */
    public static List<BlockPos> trapezoid(BlockPos source, int base, int top, int depth) {
        top = matchParity(base, top);
        int zStart = -(depth - 1) / 2;
        List<BlockPos> positions = new ArrayList<>();
        for (int k = 0; k < depth; k++) {
            int[] bounds = rowBounds(base, top, depth, k);
            for (int x = bounds[0]; x <= bounds[1]; x++) {
                positions.add(source.offset(x, 0, zStart + k));
            }
        }
        return positions;
    }

    /**
     * Returns a flat right triangle with the right angle at the low x, low z corner
     * @param source
     * @param base - width of the base in x direction
     * @param depth - length of the upright side in z direction
     * @return list of BlockPos representing the triangle
     */
    public static List<BlockPos> rightTriangle(BlockPos source, int base, int depth) {
        int xStart = -(base - 1) / 2;
        int zStart = -(depth - 1) / 2;
        List<BlockPos> positions = new ArrayList<>();
        for (int k = 0; k < depth; k++) {
            double t = (depth == 1) ? 0 : (double) k / (depth - 1);
            int width = (int) Math.round((base - 1) * (1.0 - t)) + 1;
            for (int x = 0; x < width; x++) {
                positions.add(source.offset(xStart + x, 0, zStart + k));
            }
        }
        return positions;
    }

    /**
     * Returns the half of a flat circle on the high z side, flat edge along x through source
     * @param source
     * @param diameter - width in blocks
     * @return list of BlockPos representing the semicircle
     */
    public static List<BlockPos> semicircle(BlockPos source, int diameter) {
        List<BlockPos> positions = new ArrayList<>();
        for (BlockPos pos : circle(source, diameter)) {
            if (pos.getZ() >= source.getZ() + (diameter + 1) % 2) positions.add(pos);
        }
        return positions;
    }

    /**
     * Returns a solid cylinder with its base centered at source
     * @param source - center of the base
     * @param diameter - width in blocks
     * @param height - height in blocks
     * @return list of BlockPos representing the cylinder
     */
    public static List<BlockPos> cylinder(BlockPos source, int diameter, int height) {
        return extrude(circle(source, diameter), height);
    }

    /**
     * Returns a solid triangular prism, the triangle from triangle() stacked upward
     * @param source - center of the base
     * @param base - width of the triangle base in x direction
     * @param depth - length of the triangle in z direction
     * @param height - height in blocks
     * @return list of BlockPos representing the prism
     */
    public static List<BlockPos> prism(BlockPos source, int base, int depth, int height) {
        return extrude(triangle(source, base, depth), height);
    }

    /**
     * Returns a solid cone with its base centered at source and its point at the top
     * @param source - center of the base
     * @param diameter - width of the base in blocks
     * @param height - height in blocks
     * @return list of BlockPos representing the cone
     */
    public static List<BlockPos> cone(BlockPos source, int diameter, int height) {
        return frustum(source, diameter, 1, height);
    }

    /**
     * Returns a solid truncated cone, circles shrinking from the bottom diameter to the top diameter
     * @param source - center of the base
     * @param bottom - width of the base in blocks
     * @param top - width of the top in blocks
     * @param height - height in blocks
     * @return list of BlockPos representing the frustum
     */
    public static List<BlockPos> frustum(BlockPos source, int bottom, int top, int height) {
        List<BlockPos> positions = new ArrayList<>();
        for (int y = 0; y < height; y++) {
            positions.addAll(circle(source.above(y), frustumWidth(bottom, top, height, y)));
        }
        return positions;
    }

    /**
     * Stacks a flat shape upward
     * @param plane - shape at the base y level
     * @param height - number of layers
     * @return list of BlockPos representing the extruded shape
     */
    public static List<BlockPos> extrude(List<BlockPos> plane, int height) {
        List<BlockPos> positions = new ArrayList<>(plane.size() * height);
        for (int y = 0; y < height; y++) {
            for (BlockPos pos : plane) positions.add(pos.above(y));
        }
        return positions;
    }

    /**
     * Returns the blocks of a solid shape that have at least one exposed face
     * @param solid - any filled shape
     * @return list of BlockPos representing the one block thick shell
     */
    public static List<BlockPos> shell(List<BlockPos> solid) {
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

    /**
     * Returns the edge of a flat shape, blocks with an exposed side in the x/z plane
     * @param plane - any flat filled shape
     * @return list of BlockPos representing the outline
     */
    public static List<BlockPos> outline(List<BlockPos> plane) {
        Set<BlockPos> lookup = new HashSet<>(plane);
        List<BlockPos> edge = new ArrayList<>();
        for (BlockPos pos : plane) {
            for (Direction dir : Direction.Plane.HORIZONTAL) {
                if (!lookup.contains(pos.relative(dir))) {
                    edge.add(pos);
                    break;
                }
            }
        }
        return edge;
    }

    /**
     * Returns a straight line of blocks between two points, both ends included.
     * Halves round away from zero so mirrored lines stay mirrored.
     * @param from
     * @param to
     * @return list of BlockPos representing the line
     */
    public static List<BlockPos> line(BlockPos from, BlockPos to) {
        int dx = to.getX() - from.getX();
        int dy = to.getY() - from.getY();
        int dz = to.getZ() - from.getZ();
        int steps = Math.max(Math.abs(dx), Math.max(Math.abs(dy), Math.abs(dz)));
        List<BlockPos> positions = new ArrayList<>(steps + 1);
        for (int i = 0; i <= steps; i++) {
            double t = (steps == 0) ? 0 : (double) i / steps;
            positions.add(from.offset(roundHalfAway(dx * t), roundHalfAway(dy * t), roundHalfAway(dz * t)));
        }
        return positions;
    }

    /**
     * Returns three rings through the six poles of a sphere, one per axis plane. Even diameters have no single
     * middle layer, so each ring is two blocks thick across the seam.
     * @param source - center of the base
     * @param diameter - width and height in blocks
     * @return list of BlockPos representing the sphere frame
     */
    public static List<BlockPos> sphereFrame(BlockPos source, int diameter) {
        BlockPos center = source.above((diameter - 1) / 2);
        Set<BlockPos> frame = new LinkedHashSet<>();
        for (BlockPos p : outline(circle(BlockPos.ZERO, diameter))) {
            for (int o : centerOffsets(diameter)) {
                frame.add(center.offset(p.getX(), o, p.getZ()));
                frame.add(center.offset(p.getX(), p.getZ(), o));
                frame.add(center.offset(o, p.getZ(), p.getX()));
            }
        }
        return new ArrayList<>(frame);
    }

    /**
     * Returns the base ring of a half sphere and the two half rings over its top, matching halfSphere's layers
     * @param source - center of the base
     * @param diameter - width in blocks
     * @return list of BlockPos representing the half sphere frame
     */
    public static List<BlockPos> halfSphereFrame(BlockPos source, int diameter) {
        List<BlockPos> positions = new ArrayList<>();
        for (BlockPos pos : sphereFrame(source.below(diameter / 2), diameter)) {
            if (pos.getY() >= source.getY()) positions.add(pos);
        }
        return positions;
    }

    /**
     * Returns the bottom and top rings of a truncated cone joined by sloped edges, a top of 1 gives a cone.
     * Even diameters run each edge down both center columns.
     * @param source - center of the base
     * @param bottom - width of the base in blocks
     * @param top - width of the top in blocks, nudged to match the bottom's parity
     * @param height - height in blocks
     * @return list of BlockPos representing the frustum frame
     */
    public static List<BlockPos> frustumFrame(BlockPos source, int bottom, int top, int height) {
        top = matchParity(bottom, top);
        BlockPos topCenter = source.above(height - 1);
        Set<BlockPos> frame = new LinkedHashSet<>(outline(circle(source, bottom)));
        frame.addAll(outline(circle(topCenter, top)));

        int lo = -(bottom - 1) / 2;
        int hi = bottom / 2;
        int topLo = -(top - 1) / 2;
        int topHi = top / 2;
        for (int o : centerOffsets(bottom)) {
            frame.addAll(line(source.offset(lo, 0, o), topCenter.offset(topLo, 0, o)));
            frame.addAll(line(source.offset(hi, 0, o), topCenter.offset(topHi, 0, o)));
            frame.addAll(line(source.offset(o, 0, lo), topCenter.offset(o, 0, topLo)));
            frame.addAll(line(source.offset(o, 0, hi), topCenter.offset(o, 0, topHi)));
        }
        return new ArrayList<>(frame);
    }

    /**
     * Returns the twelve edges of a cuboid
     * @param source - center of the base
     * @param x - size in x direction
     * @param y - size in y direction
     * @param z - size in z direction
     * @return list of BlockPos representing the cuboid frame
     */
    public static List<BlockPos> cuboidFrame(BlockPos source, int x, int y, int z) {
        List<BlockPos> solid = cuboid(source, x, y, z);
        BoundingBox box = BoundingBox.encapsulatingPositions(solid).orElseThrow();
        List<BlockPos> frame = new ArrayList<>();
        for (BlockPos pos : solid) {
            int onBounds = 0;
            if (pos.getX() == box.minX() || pos.getX() == box.maxX()) onBounds++;
            if (pos.getY() == box.minY() || pos.getY() == box.maxY()) onBounds++;
            if (pos.getZ() == box.minZ() || pos.getZ() == box.maxZ()) onBounds++;
            if (onBounds >= 2) frame.add(pos);
        }
        return frame;
    }

    /**
     * Returns the top and bottom rings of a cylinder joined by vertical edges, doubled across the seam for even diameters
     * @param source - center of the base
     * @param diameter - width in blocks
     * @param height - height in blocks
     * @return list of BlockPos representing the cylinder frame
     */
    public static List<BlockPos> cylinderFrame(BlockPos source, int diameter, int height) {
        Set<BlockPos> frame = new LinkedHashSet<>();
        List<BlockPos> ring = outline(circle(source, diameter));
        frame.addAll(ring);
        for (BlockPos pos : ring) frame.add(pos.above(height - 1));

        int lo = -(diameter - 1) / 2;
        int hi = diameter / 2;
        for (int o : centerOffsets(diameter)) {
            BlockPos[] sides = {source.offset(lo, 0, o), source.offset(hi, 0, o), source.offset(o, 0, lo), source.offset(o, 0, hi)};
            for (BlockPos side : sides) frame.addAll(line(side, side.above(height - 1)));
        }
        return new ArrayList<>(frame);
    }

    /**
     * Returns the edges of a triangular prism: the triangle's outline at the bottom and top joined by vertical
     * edges at its corners. Using the solid triangle's outline keeps the frame on the same blocks as prism().
     * @param source - center of the base
     * @param base - width of the triangle base in x direction
     * @param depth - length of the triangle in z direction
     * @param height - height in blocks
     * @return list of BlockPos representing the prism frame
     */
    public static List<BlockPos> prismFrame(BlockPos source, int base, int depth, int height) {
        List<BlockPos> edge = outline(triangle(source, base, depth));
        Set<BlockPos> frame = new LinkedHashSet<>(edge);
        for (BlockPos pos : edge) frame.add(pos.above(height - 1));

        int zStart = -(depth - 1) / 2;
        int[] first = rowBounds(base, pointWidth(base), depth, 0);
        int[] last = rowBounds(base, pointWidth(base), depth, depth - 1);
        BlockPos[] corners = {
            source.offset(first[0], 0, zStart), source.offset(first[1], 0, zStart),
            source.offset(last[0], 0, zStart + depth - 1), source.offset(last[1], 0, zStart + depth - 1)
        };
        for (BlockPos corner : corners) frame.addAll(line(corner, corner.above(height - 1)));
        return new ArrayList<>(frame);
    }

    //** Even size helpers

    // Bounds of row k of a trapezoid, the low bound mirrors the high one about the center so both sides round alike
    private static int[] rowBounds(int base, int top, int depth, int k) {
        double cx = ((base / 2) - (base - 1) / 2) / 2.0;
        double halfBase = (base - 1) / 2.0;
        double halfTop = (top - 1) / 2.0;
        double t = (depth == 1) ? 0 : (double) k / (depth - 1);
        double half = halfBase + (halfTop - halfBase) * t;
        int hi = (int) Math.floor(cx + half + 0.5);
        return new int[]{(int) Math.round(2 * cx) - hi, hi};
    }

    // A pointed shape ends in one block for odd widths and two for even widths
    private static int pointWidth(int width) {
        return 2 - width % 2;
    }

    // Narrower widths must share the parity of the wider one or they sit half a block off center
    private static int matchParity(int width, int narrower) {
        return ((width - narrower) % 2 == 0) ? narrower : narrower + 1;
    }

    // The columns through the middle of a shape, one for odd sizes and two for even sizes
    private static int[] centerOffsets(int size) {
        return (size % 2 == 0) ? new int[]{0, 1} : new int[]{0};
    }

    private static int roundHalfAway(double value) {
        return (int) (Math.signum(value) * Math.floor(Math.abs(value) + 0.5));
    }

    // Widths keep the bottom's parity so each layer stays centered on the same column
    private static int frustumWidth(int bottom, int top, int height, int y) {
        double t = (height == 1) ? 0 : (double) y / (height - 1);
        double width = bottom + (top - bottom) * t;
        int parity = bottom % 2;
        int w = 2 * (int) Math.round((width - parity) / 2.0) + parity;
        return Math.max(w, 2 - parity);
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
     *
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

//consider edge cases for l/w = 1
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
