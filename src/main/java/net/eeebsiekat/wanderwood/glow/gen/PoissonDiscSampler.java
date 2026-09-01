package net.eeebsiekat.wanderwood.glow.gen;

import net.minecraft.util.RandomSource;
import net.minecraft.world.phys.Vec2;

import java.util.ArrayList;
import java.util.List;

public class PoissonDiscSampler {

    public static List<Vec2> generatePoints(double minRadius, double minX, double minZ, double maxX, double maxZ, int k, RandomSource random) {
        List<Vec2> points = new ArrayList<>();
        List<Vec2> spawnPoints = new ArrayList<>();

        double cellSize = minRadius / Math.sqrt(2);
        int gridWidth = (int) Math.ceil((maxX - minX) / cellSize);
        int gridHeight = (int) Math.ceil((maxZ - minZ) / cellSize);

        // Single flat 1D array to avoid creating millions of sub-array objects
        int[] grid = new int[gridWidth * gridHeight];

        Vec2 firstPoint = new Vec2(
                (float) (minX + random.nextDouble() * (maxX - minX)),
                (float) (minZ + random.nextDouble() * (maxZ - minZ))
        );

        points.add(firstPoint);
        spawnPoints.add(firstPoint);

        int firstGridX = (int) ((firstPoint.x - minX) / cellSize);
        int firstGridY = (int) ((firstPoint.y - minZ) / cellSize);
        grid[firstGridX + firstGridY * gridWidth] = points.size();

        while (!spawnPoints.isEmpty()) {
            int spawnIndex = random.nextInt(spawnPoints.size());
            Vec2 spawnCenter = spawnPoints.get(spawnIndex);
            boolean accepted = false;

            for (int i = 0; i < k; i++) {
                double angle = random.nextDouble() * Math.PI * 2;
                double dist = minRadius + random.nextDouble() * minRadius;
                Vec2 candidate = new Vec2(
                        (float) (spawnCenter.x + Math.cos(angle) * dist),
                        (float) (spawnCenter.y + Math.sin(angle) * dist)
                );

                if (candidate.x >= minX && candidate.x < maxX && candidate.y >= minZ && candidate.y < maxZ) {
                    int cellX = (int) ((candidate.x - minX) / cellSize);
                    int cellY = (int) ((candidate.y - minZ) / cellSize);

                    if (isValidPoint(candidate, minX, minZ, cellSize, minRadius, grid, points, gridWidth, gridHeight)) {
                        points.add(candidate);
                        spawnPoints.add(candidate);
                        grid[cellX + cellY * gridWidth] = points.size();
                        accepted = true;
                        break;
                    }
                }
            }

            if (!accepted) {
                spawnPoints.remove(spawnIndex);
            }
        }

        return points;
    }

    private static boolean isValidPoint(Vec2 candidate, double minX, double minZ, double cellSize, double minRadius, int[] grid, List<Vec2> points, int gWidth, int gHeight) {
        int cellX = (int) ((candidate.x - minX) / cellSize);
        int cellY = (int) ((candidate.y - minZ) / cellSize);

        int searchMinX = Math.max(0, cellX - 2);
        int searchMaxX = Math.min(gWidth - 1, cellX + 2);
        int searchMinY = Math.max(0, cellY - 2);
        int searchMaxY = Math.min(gHeight - 1, cellY + 2);

        for (int x = searchMinX; x <= searchMaxX; x++) {
            for (int y = searchMinY; y <= searchMaxY; y++) {
                int pointIndex = grid[x + y * gWidth] - 1;
                if (pointIndex >= 0) {
                    Vec2 neighbor = points.get(pointIndex);
                    double dx = candidate.x - neighbor.x;
                    double dy = candidate.y - neighbor.y;
                    if (dx * dx + dy * dy < minRadius * minRadius) {
                        return false;
                    }
                }
            }
        }
        return true;
    }
}