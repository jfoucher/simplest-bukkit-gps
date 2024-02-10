package fr.sixpixels.gps;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.util.Vector;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.PriorityQueue;
import java.util.Set;

public class Pathfinding {

    public static List<Location> findPath(Location start, Location goal) {
        Set<Location> closedSet = new HashSet<>();
        PriorityQueue<Node> openSet = new PriorityQueue<>();
        openSet.add(new Node(start, null, 0, heuristicCost(start, goal)));

        while (!openSet.isEmpty()) {
            Node current = openSet.poll();

            if (current.getLocation().distance(goal) < 2.0) {
                return reconstructPath(current);
            }

            closedSet.add(current.getLocation());

            for (Location neighbor : getNeighbors(current.getLocation())) {
                if (closedSet.contains(neighbor)) {
                    continue;
                }

                double tentativeGScore = current.getGScore() + current.getLocation().distance(neighbor);

                if (!containsLocation(openSet, neighbor) || tentativeGScore < getGScore(openSet, neighbor)) {
                    Node neighborNode = new Node(neighbor, current, tentativeGScore, heuristicCost(neighbor, goal));
                    openSet.add(neighborNode);
                }
            }

            if (current.getLocation().distance(goal) > 2* start.distance(goal)) {
                return Collections.emptyList();
            }
        }

        // No path found
        return Collections.emptyList();
    }

    private static double heuristicCost(Location a, Location b) {
        // Add a small cost for height difference
        return a.distance(b) + 20 * (a.getY() - b.getY()) * (a.getY() - b.getY());
    }

    private static List<Location> reconstructPath(Node node) {
        List<Location> path = new ArrayList<>();
        while (node != null) {
            path.add(node.getLocation());
            node = node.getParent();
        }
        Collections.reverse(path);
        return simplifyPath(path, 1.2);
    }

    private static List<Location> getNeighbors(Location location) {
        List<Location> neighbors = new ArrayList<>();
        int[][] directions = {{0, 0, 1}, {1,0, 0}, {0,0, -1}, {-1,0, 0}, {0, 1, 1}, {1,1, 0}, {0,1, -1}, {-1,1, 0}, {0, -1, 1}, {1,-1, 0}, {0,-1, -1}, {-1,-1, 0}};

        for (int[] direction : directions) {
            int offsetX = direction[0];
            int offsetY = direction[1];
            int offsetZ = direction[2];

            Location neighborLocation = location.clone().add(offsetX, offsetY, offsetZ);
            // Check if the neighbor block is passable (air or other walkable blocks)
            if (isWalkable(neighborLocation)) {
                neighbors.add(neighborLocation);
            }
        }

        return neighbors;
    }

    private static boolean isWalkable(Location location) {
        World world = location.getWorld();
        Location l = location.clone();
        Block block = world.getBlockAt(l);
        l.add(0, 1, 0);
        Block above = world.getBlockAt(l);
        l.add(0, 1, 0);
        Block above2 = world.getBlockAt(l);

        return block.getType().isSolid() && pass(above.getType()) && pass(above2.getType());
    }

    private static boolean pass(Material m) {
        return m.isAir() || m.name().toUpperCase().contains("WALL_SIGN");
    }

    private static boolean containsLocation(PriorityQueue<Node> nodes, Location location) {
        return nodes.stream().anyMatch(node -> node.getLocation().equals(location));
    }

    private static double getGScore(PriorityQueue<Node> nodes, Location location) {
        return nodes.stream()
                .filter(node -> node.getLocation().equals(location))
                .findFirst()
                .map(Node::getGScore)
                .orElse(Double.MAX_VALUE);
    }

    public static List<Location> simplifyPath(List<Location> path, double tolerance) {
        if (path == null || path.size() < 3) {
            return path;
        }

        // Find the point with the maximum distance
        double maxDistance = 0;

        int index = 0;
        int end = path.size() - 1;

        for (int i = 1; i < end; i++) {
            double distance = perpendicularDistance(path.get(i), path.get(0), path.get(end));

            if (distance > maxDistance) {
                maxDistance = distance;
                index = i;
            }
        }

        if (maxDistance <= 0.1) {
            index = end / 2;
        }


        // If the max distance is greater than the tolerance, recursively simplify both subpaths
        List<Location> simplifiedPath = new ArrayList<>();
        if (maxDistance > tolerance || path.get(0).distance(path.get(end)) > 15.0) {
            List<Location> subpath1 = simplifyPath(path.subList(0, index + 1), tolerance);
            List<Location> subpath2 = simplifyPath(path.subList(index, end + 1), tolerance);

            // Combine the simplified subpaths
            simplifiedPath.addAll(subpath1.subList(0, subpath1.size() - 1));
            simplifiedPath.addAll(subpath2);
        } else {
            // If the max distance is within tolerance, include only the start and end points
            simplifiedPath.add(path.get(0));
            // If the path length is bigger than 15, and a point in the middle

            simplifiedPath.add(path.get(end));
        }

        return simplifiedPath;
    }

    private static double perpendicularDistance(Location point, Location lineStart, Location lineEnd) {
        // Convert locations to vectors for easier calculations
        Vector lineVector = lineEnd.toVector().subtract(lineStart.toVector());
        Vector pointVector = point.toVector().subtract(lineStart.toVector());

        // Project point vector onto the line vector
        double projection = pointVector.dot(lineVector) / lineVector.lengthSquared();

        // Clamp the projection to the valid range [0, 1]
        projection = Math.max(0, Math.min(1, projection));

        // Calculate the closest point on the line to the point
        Vector closestPoint = lineStart.toVector().add(lineVector.multiply(projection));

        // Calculate the distance between the closest point and the given point

        return point.toVector().distance(closestPoint);
    }

    private static class Node implements Comparable<Node> {
        private final Location location;
        private final Node parent;
        private final double gScore;
        private final double hScore;

        public Node(Location location, Node parent, double gScore, double hScore) {
            this.location = location;
            this.parent = parent;
            this.gScore = gScore;
            this.hScore = hScore;
        }

        public Location getLocation() {
            return location;
        }

        public Node getParent() {
            return parent;
        }

        public double getGScore() {
            return gScore;
        }

        public double getHScore() {
            return hScore;
        }

        public double getFScore() {
            return gScore + hScore;
        }

        @Override
        public int compareTo(Node other) {
            return Double.compare(this.getFScore(), other.getFScore());
        }
    }
}