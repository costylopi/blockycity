package com.blockcity.world;

import com.blockcity.ModEntities;
import com.blockcity.entity.CarEntity;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.random.Random;

/**
 * Genereaza un oras mic, cu blocuri vanilla: grila de celule 24x24 = strada (8) + trotuar + cladire 12x12.
 * Terenul din zona este aplatizat la nivelul jucatorului.
 */
public final class CityGenerator {
    private static final int CELL = 24;
    private static final int MAX_HEIGHT = 46;
    private static final int FLAGS = Block.NOTIFY_LISTENERS;

    private static final BlockState ASPHALT = Blocks.GRAY_CONCRETE.getDefaultState();
    private static final BlockState LINE = Blocks.YELLOW_CONCRETE.getDefaultState();
    private static final BlockState SIDEWALK = Blocks.LIGHT_GRAY_CONCRETE.getDefaultState();
    private static final BlockState LOT = Blocks.SMOOTH_STONE.getDefaultState();
    private static final BlockState AIR = Blocks.AIR.getDefaultState();
    private static final BlockState STONE = Blocks.STONE.getDefaultState();
    private static final BlockState GLASS = Blocks.LIGHT_BLUE_STAINED_GLASS.getDefaultState();
    private static final BlockState LAMP = Blocks.SEA_LANTERN.getDefaultState();
    private static final BlockState POLE = Blocks.COBBLESTONE_WALL.getDefaultState();
    private static final BlockState[] WALLS = {
            Blocks.WHITE_CONCRETE.getDefaultState(), Blocks.LIGHT_GRAY_CONCRETE.getDefaultState(),
            Blocks.BROWN_CONCRETE.getDefaultState(), Blocks.RED_CONCRETE.getDefaultState(),
            Blocks.CYAN_CONCRETE.getDefaultState(), Blocks.ORANGE_CONCRETE.getDefaultState()
    };

    /** @return numarul de masini plasate */
    public static int generate(ServerWorld w, BlockPos origin, int n) {
        int span = n * CELL + 8;                // + o strada de inchidere
        int x0 = origin.getX() - span / 2, z0 = origin.getZ() - span / 2;
        int gy = origin.getY() - 1;
        BlockPos.Mutable m = new BlockPos.Mutable();

        for (int dx = 0; dx < span; dx++) {
            for (int dz = 0; dz < span; dz++) {
                int x = x0 + dx, z = z0 + dz;
                int lx = dx % CELL, lz = dz % CELL;
                boolean roadX = lx < 8, roadZ = lz < 8;
                BlockState top;
                if (roadX || roadZ) {
                    top = ASPHALT;
                    if (roadX && !roadZ && (lx == 3 || lx == 4) && lz % 6 < 4) top = LINE;
                    if (roadZ && !roadX && (lz == 3 || lz == 4) && lx % 6 < 4) top = LINE;
                } else if (lx < 10 || lz < 10 || lx >= 22 || lz >= 22) {
                    top = SIDEWALK;
                } else {
                    top = LOT;
                }
                // curata deasupra
                for (int y = gy + 1; y <= gy + MAX_HEIGHT; y++) {
                    m.set(x, y, z);
                    if (!w.getBlockState(m).isAir()) w.setBlockState(m, AIR, FLAGS);
                }
                m.set(x, gy, z);
                w.setBlockState(m, top, FLAGS);
                // umple golurile de sub strada (max 24 blocuri adancime)
                for (int y = gy - 1; y > gy - 24; y--) {
                    m.set(x, y, z);
                    BlockState s = w.getBlockState(m);
                    if (s.isSideSolidFullSquare(w, m, Direction.UP)) break;
                    w.setBlockState(m, STONE, FLAGS);
                }
            }
        }

        int cars = 0;
        for (int cx = 0; cx < n; cx++) {
            for (int cz = 0; cz < n; cz++) {
                int bx = x0 + cx * CELL, bz = z0 + cz * CELL;
                building(w, m, bx + 10, bz + 10, gy, Random.create(cx * 341873128712L ^ cz * 132897987541L ^ origin.asLong()));
                lamps(w, m, bx, bz, gy);
                CarEntity car = ModEntities.CAR.create(w);
                if (car != null) {
                    car.refreshPositionAndAngles(bx + 5.5, gy + 1, bz + 12.5, 180f, 0f);
                    w.spawnEntity(car);
                    cars++;
                }
            }
        }
        return cars;
    }

    private static void building(ServerWorld w, BlockPos.Mutable m, int x0, int z0, int gy, Random r) {
        int h = 10 + r.nextInt(32);
        BlockState wall = WALLS[r.nextInt(WALLS.length)];
        for (int y = 1; y <= h + 1; y++) {
            for (int i = 0; i < 12; i++) {
                for (int j = 0; j < 12; j++) {
                    boolean edge = i == 0 || j == 0 || i == 11 || j == 11;
                    boolean roof = y == h + 1;
                    if (!edge && !roof) {
                        // lumini interioare, ca sa nu apara monstri in cladiri
                        if (y % 4 == 3 && (i == 3 || i == 8) && (j == 3 || j == 8)) place(w, m, x0 + i, gy + y, z0 + j, LAMP);
                        continue;
                    }
                    BlockState s = wall;
                    if (edge && !roof && y >= 3 && (y % 4 == 2 || y % 4 == 3)) {
                        int idx = (i == 0 || i == 11) ? j : i;
                        if (idx % 3 != 0 && idx != 0 && idx != 11) s = GLASS;
                    }
                    // usa pe fata de nord (j == 0), centrata
                    if (j == 0 && (i == 5 || i == 6) && y <= 2) s = AIR;
                    place(w, m, x0 + i, gy + y, z0 + j, s);
                }
            }
        }
    }

    private static void lamps(ServerWorld w, BlockPos.Mutable m, int bx, int bz, int gy) {
        int[][] spots = {{8, 8}, {23, 8}, {8, 23}, {23, 23}};
        for (int[] s : spots) {
            for (int y = 1; y <= 4; y++) place(w, m, bx + s[0], gy + y, bz + s[1], POLE);
            place(w, m, bx + s[0], gy + 5, bz + s[1], LAMP);
        }
    }

    private static void place(ServerWorld w, BlockPos.Mutable m, int x, int y, int z, BlockState s) {
        m.set(x, y, z);
        w.setBlockState(m, s, FLAGS);
    }

    private CityGenerator() {}
}
