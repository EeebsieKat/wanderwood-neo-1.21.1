package net.eeebsiekat.wanderwood.glow.gen;

import net.minecraft.world.phys.Vec2;
import java.util.*;

public class DelaunayTriangulation {

    public record Edge2D(Vec2 p1, Vec2 p2) {
        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof Edge2D edge)) return false;
            return (p1.equals(edge.p1) && p2.equals(edge.p2)) || (p1.equals(edge.p2) && p2.equals(edge.p1));
        }

        @Override
        public int hashCode() {
            return p1.hashCode() + p2.hashCode();
        }
    }

    public static Set<Edge2D> triangulate(List<Vec2> points) {
        if (points.size() < 3) return Collections.emptySet();

        // Calculate Super-Triangle
        float minX = Float.MAX_VALUE, minY = Float.MAX_VALUE, maxX = -Float.MAX_VALUE, maxY = -Float.MAX_VALUE;
        for (Vec2 p : points) {
            minX = Math.min(minX, p.x); minY = Math.min(minY, p.y);
            maxX = Math.max(maxX, p.x); maxY = Math.max(maxY, p.y);
        }

        float dx = (maxX - minX) * 10;
        float dy = (maxY - minY) * 10;

        Vec2 st1 = new Vec2(minX - dx, minY - dy * 3);
        Vec2 st2 = new Vec2(minX - dx, maxY + dy);
        Vec2 st3 = new Vec2(maxX + dx * 3, maxY + dy);

        List<Triangle> triangles = new ArrayList<>();
        triangles.add(new Triangle(st1, st2, st3));

        for (Vec2 point : points) {
            List<Triangle> badTriangles = new ArrayList<>();
            for (Triangle tri : triangles) {
                if (tri.containsInCircumcircle(point)) {
                    badTriangles.add(tri);
                }
            }

            List<Edge2D> polygon = new ArrayList<>();
            for (Triangle tri : badTriangles) {
                for (Edge2D edge : tri.edges()) {
                    boolean shared = false;
                    for (Triangle other : badTriangles) {
                        if (tri != other && other.hasEdge(edge)) {
                            shared = true;
                            break;
                        }
                    }
                    if (!shared) polygon.add(edge);
                }
            }

            triangles.removeAll(badTriangles);

            for (Edge2D edge : polygon) {
                triangles.add(new Triangle(edge.p1, edge.p2, point));
            }
        }

        // Filter out super-triangle connections
        Set<Edge2D> edges = new HashSet<>();
        for (Triangle tri : triangles) {
            if (!tri.hasVertex(st1) && !tri.hasVertex(st2) && !tri.hasVertex(st3)) {
                edges.addAll(Arrays.asList(tri.edges()));
            }
        }

        return edges;
    }

    private static class Triangle {
        final Vec2 a, b, c;

        Triangle(Vec2 a, Vec2 b, Vec2 c) {
            this.a = a; this.b = b; this.c = c;
        }

        Edge2D[] edges() {
            return new Edge2D[]{ new Edge2D(a, b), new Edge2D(b, c), new Edge2D(c, a) };
        }

        boolean hasVertex(Vec2 v) {
            return a.equals(v) || b.equals(v) || c.equals(v);
        }

        boolean hasEdge(Edge2D e) {
            return (a.equals(e.p1) && b.equals(e.p2)) || (b.equals(e.p1) && a.equals(e.p2)) ||
                    (b.equals(e.p1) && c.equals(e.p2)) || (c.equals(e.p1) && b.equals(e.p2)) ||
                    (c.equals(e.p1) && a.equals(e.p2)) || (a.equals(e.p1) && c.equals(e.p2));
        }

        boolean containsInCircumcircle(Vec2 p) {
            double ab = a.x * a.x + a.y * a.y;
            double cd = b.x * b.x + b.y * b.y;
            double ef = c.x * c.x + c.y * c.y;

            double circumX = (ab * (c.y - b.y) + cd * (a.y - c.y) + ef * (b.y - a.y)) /
                    (2 * (a.x * (c.y - b.y) + b.x * (a.y - c.y) + c.x * (b.y - a.y)));
            double circumY = (ab * (b.x - c.x) + cd * (c.x - a.x) + ef * (a.x - b.x)) /
                    (2 * (a.x * (c.y - b.y) + b.x * (a.y - c.y) + c.x * (b.y - a.y)));

            double radiusSq = Math.pow(a.x - circumX, 2) + Math.pow(a.y - circumY, 2);
            double distSq = Math.pow(p.x - circumX, 2) + Math.pow(p.y - circumY, 2);

            return distSq <= radiusSq;
        }
    }
}