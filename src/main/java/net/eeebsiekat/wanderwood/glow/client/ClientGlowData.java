package net.eeebsiekat.wanderwood.glow.client;

import net.eeebsiekat.wanderwood.glow.network.GlowLine;
import net.eeebsiekat.wanderwood.glow.network.GlowNode;

import java.util.ArrayList;
import java.util.List;

public class ClientGlowData {
    private static List<GlowNode> nodes = new ArrayList<>();
    private static List<GlowLine> lines = new ArrayList<>();

    public static void setNetwork(List<GlowNode> newNodes, List<GlowLine> newLines) {
        nodes = newNodes;
        lines = newLines;
    }

    public static List<GlowNode> getNodes() { return nodes; }
    public static List<GlowLine> getLines() { return lines; }
}