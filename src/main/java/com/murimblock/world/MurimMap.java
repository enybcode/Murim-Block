package com.murimblock.world;

import java.util.List;

/**
 * The fixed layout of the Murim continent (docs/world/map.md): pure math, the same in every world. North is -Z.
 * Zones are cut in "warped" space: block coordinates bent by a two-scale noise, so every border is wavy. The guide's
 * coordinates are warped coordinates; {@link #unwarp} finds the block where one lies.
 */
public final class MurimMap {
    /** Where each biome is sure to be, in warped coordinates: used by the tests and the in-game tour. */
    public record Spot(String name, MurimBiome biome, int x, int z) {
    }

    public static final List<Spot> SPOTS = List.of(
            new Spot("frozen tundra", MurimBiome.FROZEN_TUNDRA, 0, -4500),
            new Spot("cold desert", MurimBiome.COLD_DESERT, -1000, -3600),
            new Spot("frozen steppe", MurimBiome.FROZEN_STEPPE, -500, -2500),
            new Spot("Qinghai plateau", MurimBiome.QINGHAI_PLATEAU, -3000, -800),
            new Spot("western snow peaks", MurimBiome.WESTERN_SNOW_PEAKS, -4850, 100),
            new Spot("Chang-An, loess plateau", MurimBiome.LOESS_PLATEAU, -1300, -700),
            new Spot("central plains", MurimBiome.CENTRAL_PLAINS, 1100, -1100),
            new Spot("Shandong hills", MurimBiome.SHANDONG_HILLS, 3000, -500),
            new Spot("sacred mountain", MurimBiome.SACRED_MOUNTAIN, 3400, -1100),
            new Spot("Sichuan red basin", MurimBiome.RED_BASIN, -2500, 1100),
            new Spot("bamboo mountains", MurimBiome.BAMBOO_MOUNTAINS, -600, 1100),
            new Spot("Wuhan, Hubei waterlands", MurimBiome.HUBEI_WATERLANDS, 950, 1100),
            new Spot("river delta", MurimBiome.RIVER_DELTA, 3000, 1600),
            new Spot("stone forest", MurimBiome.STONE_FOREST, -1800, 2900),
            new Spot("western gorges", MurimBiome.WESTERN_GORGES, -3800, 3500),
            new Spot("Unnam jungle", MurimBiome.UNNAM_JUNGLE, -1800, 4100),
            new Spot("Honam stone peaks", MurimBiome.STONE_PEAKS, 600, 2900),
            new Spot("rice terraces", MurimBiome.RICE_TERRACES, 1450, 3300),
            new Spot("subtropical forest", MurimBiome.SUBTROPICAL_FOREST, 1400, 4200),
            new Spot("southeast coast", MurimBiome.SOUTHEAST_COAST, 2900, 3000));

    private MurimMap() {
    }

    /** The biome of the map at this block column. */
    public static MurimBiome biome(double x, double z) {
        return biomeWarped(x + dwx(x, z), z + dwz(x, z));
    }

    /** The biome at a point given in warped (guide) coordinates. */
    public static MurimBiome biomeWarped(double wx, double wz) {
        double lr = landR(wx, wz);
        double coast = coastAt(wz);
        if (lr > 0 || wx > coast) {
            return MurimBiome.OCEAN;
        }
        if (wx > coast - 55 || lr > -.011 && wz > -2100 && wx > -1500) {
            return MurimBiome.BEACH;
        }
        if (wx < 0 && wz > -2100 && wz < 3500 && lr > -.16) {
            return wz > 2300 ? MurimBiome.WESTERN_GORGES : MurimBiome.WESTERN_SNOW_PEAKS;
        }
        if (wz < -2100) {
            return wz > -3000 ? MurimBiome.FROZEN_STEPPE : wz > -4200 ? MurimBiome.COLD_DESERT : MurimBiome.FROZEN_TUNDRA;
        }
        if (wz < 0) {
            if (wx < -2000) {
                return MurimBiome.QINGHAI_PLATEAU;
            }
            if (wx < 0) {
                return MurimBiome.LOESS_PLATEAU;
            }
            if (wx < 2300) {
                return MurimBiome.CENTRAL_PLAINS;
            }
            return Math.hypot(wx - 3400, wz + 1100) < 450 ? MurimBiome.SACRED_MOUNTAIN : MurimBiome.SHANDONG_HILLS;
        }
        if (wz < 2300) {
            if (wx < -200) {
                double ex = (wx + 2500) / 1750;
                double ez = (wz - 1100) / 760;
                return ex * ex + ez * ez < 1 ? MurimBiome.RED_BASIN : MurimBiome.BAMBOO_MOUNTAINS;
            }
            return wx < 2100 ? MurimBiome.HUBEI_WATERLANDS : MurimBiome.RIVER_DELTA;
        }
        if (wx < 0) {
            return wx < -3500 ? MurimBiome.WESTERN_GORGES : wz < 3500 ? MurimBiome.STONE_FOREST : MurimBiome.UNNAM_JUNGLE;
        }
        if (wx < 2500) {
            if (Math.hypot(wx - 600, wz - 2900) < 650) {
                return MurimBiome.STONE_PEAKS;
            }
            return wz > 3900 || wx > 1800 ? MurimBiome.SUBTROPICAL_FOREST : MurimBiome.RICE_TERRACES;
        }
        return MurimBiome.SOUTHEAST_COAST;
    }

    /**
     * Roughly how many blocks this column lies inside the land (negative out at sea): it shapes the continentalness,
     * so the terrain and the biomes see the same coast.
     */
    public static double landDistance(double x, double z) {
        double wx = x + dwx(x, z);
        double wz = z + dwz(x, z);
        return Math.min(-landR(wx, wz) * 5000, coastAt(wz) - wx);
    }

    /** The warped coordinates of a block column. */
    public static double[] warp(double x, double z) {
        return new double[] {x + dwx(x, z), z + dwz(x, z)};
    }

    /** The block column whose warped coordinates are these (the warp is gentle enough for a fixed-point search). */
    public static double[] unwarp(double wx, double wz) {
        double x = wx;
        double z = wz;
        for (int i = 0; i < 16; i++) {
            double nx = wx - dwx(x, z);
            z = wz - dwz(x, z);
            x = nx;
        }
        return new double[] {x, z};
    }

    // Two-scale warp: long bends of a few thousand blocks, plus smaller wiggles.
    private static double dwx(double x, double z) {
        return 620 * fbm(x / 3600, z / 3600, 11) + 230 * fbm(x / 950, z / 950, 29);
    }

    private static double dwz(double x, double z) {
        return 620 * fbm(x / 3600, z / 3600, 23) + 230 * fbm(x / 950, z / 950, 37);
    }

    /** The landmass, a rounded blob about 10 000 blocks wide with bays and capes: below 0 is land. */
    static double landR(double wx, double wz) {
        double p = 2.3;
        double r = Math.pow(Math.pow(Math.abs(wx) / 5000, p) + Math.pow(Math.abs(wz) / 5000, p), 1 / p);
        return r - (.98 + .2 * fbm(wx / 2600, wz / 2600, 5) + .06 * fbm(wx / 700, wz / 700, 6));
    }

    /** The east coast, from about X 4600 in the north to 3700 in the south. */
    static double coastAt(double wz) {
        double t = Math.max(0, Math.min(1, (wz + 2100) / 7100));
        return 4600 - 900 * t * t * (3 - 2 * t) + 150 * fbm(0.37, wz / 500, 7);
    }

    // Value noise and its three-octave sum, the same as the mockup's.
    static double fbm(double x, double z, int s) {
        return (noise(x, z, s) - .5) + (noise(x * 2.03, z * 2.03, s + 7) - .5) * .5
                + (noise(x * 4.1, z * 4.1, s + 13) - .5) * .25;
    }

    private static double noise(double x, double z, int s) {
        int ix = (int) Math.floor(x);
        int iz = (int) Math.floor(z);
        double fx = x - ix;
        double fz = z - iz;
        double u = fx * fx * (3 - 2 * fx);
        double v = fz * fz * (3 - 2 * fz);
        double a = hash(ix, iz, s);
        double b = hash(ix + 1, iz, s);
        double c = hash(ix, iz + 1, s);
        double d = hash(ix + 1, iz + 1, s);
        return a + (b - a) * u + (c - a) * v + (a - b - c + d) * u * v;
    }

    private static double hash(int ix, int iz, int s) {
        int h = ix * 374761393 ^ iz * 668265263 ^ s * 1274126177;
        h = (h ^ h >>> 13) * 1274126177;
        h ^= h >>> 16;
        return (h & 0xffffffffL) / 4294967296.0;
    }
}
