package buildaspell.client.model;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.neoforged.api.distmarker.Dist;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

public class BlackHoleModel {
    private static final Logger LOGGER = LoggerFactory.getLogger(BlackHoleModel.class);
    private static final String GEO_PATH = "/assets/buildaspell/geo/black_hole.geo.json";

    // Pre-baked vertex data: 8 floats per vertex (x, y, z, u, v, nx, ny, nz)
    private float[] vertexData;
    private int vertexCount;
    private float modelRadius;
    private boolean loaded;

    public BlackHoleModel() {
        load();
    }

    private void load() {
        try (InputStream is = BlackHoleModel.class.getResourceAsStream(GEO_PATH)) {
            if (is == null) {
                LOGGER.error("Black hole model not found at {}", GEO_PATH);
                createFallback();
                return;
            }

            JsonObject root = JsonParser.parseReader(new InputStreamReader(is, StandardCharsets.UTF_8)).getAsJsonObject();
            JsonArray geometries = root.getAsJsonArray("minecraft:geometry");
            JsonObject geometry = geometries.get(0).getAsJsonObject();
            JsonObject description = geometry.getAsJsonObject("description");

            float texWidth = description.get("texture_width").getAsFloat();
            float texHeight = description.get("texture_height").getAsFloat();

            JsonArray bones = geometry.getAsJsonArray("bones");
            JsonArray cubes = null;
            for (JsonElement boneEl : bones) {
                JsonObject bone = boneEl.getAsJsonObject();
                if (bone.has("cubes")) {
                    cubes = bone.getAsJsonArray("cubes");
                    break;
                }
            }

            if (cubes == null || cubes.isEmpty()) {
                LOGGER.error("No cubes found in black hole model");
                createFallback();
                return;
            }

            // Parse all cubes
            List<int[]> cubeList = new ArrayList<>(); // ox, oy, oz, w, h, d, uvX, uvY
            int minX = Integer.MAX_VALUE, minY = Integer.MAX_VALUE, minZ = Integer.MAX_VALUE;
            int maxX = Integer.MIN_VALUE, maxY = Integer.MIN_VALUE, maxZ = Integer.MIN_VALUE;

            for (JsonElement cubeEl : cubes) {
                JsonObject cube = cubeEl.getAsJsonObject();
                JsonArray origin = cube.getAsJsonArray("origin");
                JsonArray size = cube.getAsJsonArray("size");
                JsonArray uv = cube.getAsJsonArray("uv");

                int ox = origin.get(0).getAsInt();
                int oy = origin.get(1).getAsInt();
                int oz = origin.get(2).getAsInt();
                int w = size.get(0).getAsInt();
                int h = size.get(1).getAsInt();
                int d = size.get(2).getAsInt();
                int uvX = uv.get(0).getAsInt();
                int uvY = uv.get(1).getAsInt();

                cubeList.add(new int[]{ox, oy, oz, w, h, d, uvX, uvY});

                minX = Math.min(minX, ox);
                minY = Math.min(minY, oy);
                minZ = Math.min(minZ, oz);
                maxX = Math.max(maxX, ox + w);
                maxY = Math.max(maxY, oy + h);
                maxZ = Math.max(maxZ, oz + d);
            }

            modelRadius = Math.max(
                    Math.max(Math.abs(minX), Math.abs(maxX)),
                    Math.max(Math.abs(minZ), Math.abs(maxZ))
            );

            // Build occupancy grid for face culling
            int gridW = maxX - minX + 2;
            int gridH = maxY - minY + 2;
            int gridD = maxZ - minZ + 2;
            boolean[][][] occupied = new boolean[gridW][gridH][gridD];

            for (int[] c : cubeList) {
                for (int x = c[0]; x < c[0] + c[3]; x++) {
                    for (int y = c[1]; y < c[1] + c[4]; y++) {
                        for (int z = c[2]; z < c[2] + c[5]; z++) {
                            occupied[x - minX][y - minY][z - minZ] = true;
                        }
                    }
                }
            }

            // Bake vertex data with face culling
            List<float[]> vertices = new ArrayList<>();
            int culledFaces = 0;
            int totalFaces = 0;

            for (int[] c : cubeList) {
                int ox = c[0], oy = c[1], oz = c[2];
                int w = c[3], h = c[4], d = c[5];
                int uvX = c[6], uvY = c[7];

                // +X face
                totalFaces++;
                if (!isFullyOccluded(occupied, ox + w, oy, oz, 1, h, d, minX, minY, minZ, gridW, gridH, gridD)) {
                    addFace(vertices, ox + w, oy, oz, ox + w, oy + h, oz + d,
                            1, 0, 0, // normal
                            uvX, uvY, w, h, d, Face.POS_X, texWidth, texHeight);
                } else culledFaces++;

                // -X face
                totalFaces++;
                if (!isFullyOccluded(occupied, ox - 1, oy, oz, 1, h, d, minX, minY, minZ, gridW, gridH, gridD)) {
                    addFace(vertices, ox, oy, oz, ox, oy + h, oz + d,
                            -1, 0, 0,
                            uvX, uvY, w, h, d, Face.NEG_X, texWidth, texHeight);
                } else culledFaces++;

                // +Y face
                totalFaces++;
                if (!isFullyOccluded(occupied, ox, oy + h, oz, w, 1, d, minX, minY, minZ, gridW, gridH, gridD)) {
                    addFace(vertices, ox, oy + h, oz, ox + w, oy + h, oz + d,
                            0, 1, 0,
                            uvX, uvY, w, h, d, Face.POS_Y, texWidth, texHeight);
                } else culledFaces++;

                // -Y face
                totalFaces++;
                if (!isFullyOccluded(occupied, ox, oy - 1, oz, w, 1, d, minX, minY, minZ, gridW, gridH, gridD)) {
                    addFace(vertices, ox, oy, oz, ox + w, oy, oz + d,
                            0, -1, 0,
                            uvX, uvY, w, h, d, Face.NEG_Y, texWidth, texHeight);
                } else culledFaces++;

                // +Z face
                totalFaces++;
                if (!isFullyOccluded(occupied, ox, oy, oz + d, w, h, 1, minX, minY, minZ, gridW, gridH, gridD)) {
                    addFace(vertices, ox, oy, oz + d, ox + w, oy + h, oz + d,
                            0, 0, 1,
                            uvX, uvY, w, h, d, Face.POS_Z, texWidth, texHeight);
                } else culledFaces++;

                // -Z face
                totalFaces++;
                if (!isFullyOccluded(occupied, ox, oy, oz - 1, w, h, 1, minX, minY, minZ, gridW, gridH, gridD)) {
                    addFace(vertices, ox, oy, oz, ox + w, oy + h, oz,
                            0, 0, -1,
                            uvX, uvY, w, h, d, Face.NEG_Z, texWidth, texHeight);
                } else culledFaces++;
            }

            // Flatten into array
            vertexCount = vertices.size();
            vertexData = new float[vertexCount * 8];
            for (int i = 0; i < vertexCount; i++) {
                System.arraycopy(vertices.get(i), 0, vertexData, i * 8, 8);
            }

            loaded = true;
            int pctReduction = totalFaces > 0 ? (culledFaces * 100 / totalFaces) : 0;
            LOGGER.info("Black hole model loaded: {} cubes, {} vertices ({} faces culled / {} total = {}% reduction)",
                    cubeList.size(), vertexCount, culledFaces, totalFaces, pctReduction);

        } catch (Exception e) {
            LOGGER.error("Failed to load black hole model", e);
            createFallback();
        }
    }

    private enum Face {
        POS_X, NEG_X, POS_Y, NEG_Y, POS_Z, NEG_Z
    }

    private boolean isFullyOccluded(boolean[][][] grid, int ox, int oy, int oz,
                                     int w, int h, int d,
                                     int minX, int minY, int minZ,
                                     int gridW, int gridH, int gridD) {
        for (int x = ox; x < ox + w; x++) {
            for (int y = oy; y < oy + h; y++) {
                for (int z = oz; z < oz + d; z++) {
                    int gx = x - minX;
                    int gy = y - minY;
                    int gz = z - minZ;
                    if (gx < 0 || gy < 0 || gz < 0 || gx >= gridW || gy >= gridH || gz >= gridD) return false;
                    if (!grid[gx][gy][gz]) return false;
                }
            }
        }
        return true;
    }

    /**
     * Adds 4 vertices for a face quad using Bedrock box UV layout.
     * Vertex order: counter-clockwise when viewed from normal direction.
     */
    private void addFace(List<float[]> vertices,
                         float x0, float y0, float z0, float x1, float y1, float z1,
                         float nx, float ny, float nz,
                         int uvX, int uvY, int w, int h, int d,
                         Face face, float texW, float texH) {
        // Bedrock box UV layout:
        // Top row:    [u+D, v] Top (WxD),  [u+D+W, v] Bottom (WxD)
        // Bottom row: [u, v+D] Right/+X (DxH), [u+D, v+D] Front/-Z (WxH),
        //             [u+D+W, v+D] Left/-X (DxH), [u+2D+W, v+D] Back/+Z (WxH)
        float u0, v0, u1, v1;
        switch (face) {
            case POS_Y -> { // Top
                u0 = (uvX + d) / texW;
                v0 = uvY / texH;
                u1 = (uvX + d + w) / texW;
                v1 = (uvY + d) / texH;
                // +Y face: looking down, quad on XZ plane at y1
                vertices.add(new float[]{x0, y1, z1, u0, v1, nx, ny, nz});
                vertices.add(new float[]{x1, y1, z1, u1, v1, nx, ny, nz});
                vertices.add(new float[]{x1, y1, z0, u1, v0, nx, ny, nz});
                vertices.add(new float[]{x0, y1, z0, u0, v0, nx, ny, nz});
            }
            case NEG_Y -> { // Bottom
                u0 = (uvX + d + w) / texW;
                v0 = uvY / texH;
                u1 = (uvX + d + w + w) / texW;
                v1 = (uvY + d) / texH;
                // -Y face: looking up, quad on XZ plane at y0
                vertices.add(new float[]{x0, y0, z0, u0, v0, nx, ny, nz});
                vertices.add(new float[]{x1, y0, z0, u1, v0, nx, ny, nz});
                vertices.add(new float[]{x1, y0, z1, u1, v1, nx, ny, nz});
                vertices.add(new float[]{x0, y0, z1, u0, v1, nx, ny, nz});
            }
            case POS_X -> { // Right
                u0 = uvX / texW;
                v0 = (uvY + d) / texH;
                u1 = (uvX + d) / texW;
                v1 = (uvY + d + h) / texH;
                // +X face: looking left, quad on YZ plane at x1
                vertices.add(new float[]{x1, y0, z1, u0, v1, nx, ny, nz});
                vertices.add(new float[]{x1, y0, z0, u1, v1, nx, ny, nz});
                vertices.add(new float[]{x1, y1, z0, u1, v0, nx, ny, nz});
                vertices.add(new float[]{x1, y1, z1, u0, v0, nx, ny, nz});
            }
            case NEG_X -> { // Left
                u0 = (uvX + d + w) / texW;
                v0 = (uvY + d) / texH;
                u1 = (uvX + d + w + d) / texW;
                v1 = (uvY + d + h) / texH;
                // -X face: looking right, quad on YZ plane at x0
                vertices.add(new float[]{x0, y0, z0, u0, v1, nx, ny, nz});
                vertices.add(new float[]{x0, y0, z1, u1, v1, nx, ny, nz});
                vertices.add(new float[]{x0, y1, z1, u1, v0, nx, ny, nz});
                vertices.add(new float[]{x0, y1, z0, u0, v0, nx, ny, nz});
            }
            case NEG_Z -> { // Front (Bedrock -Z = front)
                u0 = (uvX + d) / texW;
                v0 = (uvY + d) / texH;
                u1 = (uvX + d + w) / texW;
                v1 = (uvY + d + h) / texH;
                // -Z face: quad on XY plane at z0
                vertices.add(new float[]{x0, y0, z0, u1, v1, nx, ny, nz});
                vertices.add(new float[]{x1, y0, z0, u0, v1, nx, ny, nz});
                vertices.add(new float[]{x1, y1, z0, u0, v0, nx, ny, nz});
                vertices.add(new float[]{x0, y1, z0, u1, v0, nx, ny, nz});
            }
            case POS_Z -> { // Back (Bedrock +Z = back)
                u0 = (uvX + d + w + d) / texW;
                v0 = (uvY + d) / texH;
                u1 = (uvX + d + w + d + w) / texW;
                v1 = (uvY + d + h) / texH;
                // +Z face: quad on XY plane at z1
                vertices.add(new float[]{x1, y0, z1, u1, v1, nx, ny, nz});
                vertices.add(new float[]{x0, y0, z1, u0, v1, nx, ny, nz});
                vertices.add(new float[]{x0, y1, z1, u0, v0, nx, ny, nz});
                vertices.add(new float[]{x1, y1, z1, u1, v0, nx, ny, nz});
            }
        }
    }

    private void createFallback() {
        // Minimal fallback: single cube
        vertexData = new float[0];
        vertexCount = 0;
        modelRadius = 1.0f;
        loaded = true;
    }

    public void render(VertexConsumer consumer, PoseStack.Pose pose) {
        for (int i = 0; i < vertexCount; i++) {
            int base = i * 8;
            consumer.addVertex(pose, vertexData[base], vertexData[base + 1], vertexData[base + 2])
                    .setColor(255, 255, 255, 255)
                    .setUv(vertexData[base + 3], vertexData[base + 4])
                    .setOverlay(OverlayTexture.NO_OVERLAY)
                    .setLight(0xF000F0)
                    .setNormal(pose, vertexData[base + 5], vertexData[base + 6], vertexData[base + 7]);
        }
    }

    public float getModelRadius() {
        return modelRadius;
    }

    public boolean isLoaded() {
        return loaded && vertexCount > 0;
    }
}
