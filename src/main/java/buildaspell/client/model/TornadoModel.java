package buildaspell.client.model;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.neoforged.api.distmarker.Dist;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class TornadoModel {
    private static final Logger LOGGER = LoggerFactory.getLogger(TornadoModel.class);
    private static final String GEO_PATH = "/assets/buildaspell/geo/tornado.geo.json";
    private static final String ANIM_PATH = "/assets/buildaspell/anim/tornado.anim.json";

    private BoneData[] bones;
    private float modelRadius;
    private float modelHeight;
    private boolean loaded;

    // Animation length in seconds
    private float animationLength = 3.0f;

    public TornadoModel() {
        load();
    }

    private static class BoneData {
        String name;
        float[] vertexData; // 8 floats per vertex: x, y, z, u, v, nx, ny, nz
        int vertexCount;

        // Animation: Y-axis rotation
        float rotationDuration; // seconds for full 360 deg
        float rotationDirection; // +1 or -1

        // Animation: Y-axis position bob (3-keyframe triangle: 0 -> peak -> 0)
        float bobPeakTime;  // time of peak Y offset (seconds)
        float bobEndTime;   // time of return to 0 (seconds)
        float bobPeakY;     // max Y offset
    }

    private void load() {
        try {
            loadGeometry();
            loadAnimation();
            loaded = true;
        } catch (Exception e) {
            LOGGER.error("Failed to load tornado model", e);
            bones = new BoneData[0];
            modelRadius = 1.0f;
            modelHeight = 1.0f;
            loaded = true;
        }
    }

    private void loadGeometry() throws Exception {
        try (InputStream is = TornadoModel.class.getResourceAsStream(GEO_PATH)) {
            if (is == null) {
                throw new RuntimeException("Tornado model not found at " + GEO_PATH);
            }

            JsonObject root = JsonParser.parseReader(new InputStreamReader(is, StandardCharsets.UTF_8)).getAsJsonObject();
            JsonArray geometries = root.getAsJsonArray("minecraft:geometry");
            JsonObject geometry = geometries.get(0).getAsJsonObject();
            JsonObject description = geometry.getAsJsonObject("description");

            float texWidth = description.get("texture_width").getAsFloat();
            float texHeight = description.get("texture_height").getAsFloat();

            JsonArray jsonBones = geometry.getAsJsonArray("bones");

            // Parse bones with cubes into a map
            Map<String, BoneData> boneMap = new LinkedHashMap<>();
            float maxRadius = 0;
            float maxHeight = 0;

            for (JsonElement boneEl : jsonBones) {
                JsonObject bone = boneEl.getAsJsonObject();
                String name = bone.get("name").getAsString();
                if (!bone.has("cubes")) continue;

                JsonArray cubes = bone.getAsJsonArray("cubes");
                if (cubes.isEmpty()) continue;

                BoneData bd = new BoneData();
                bd.name = name;
                bakeBone(bd, cubes, texWidth, texHeight);

                if (bd.vertexCount > 0) {
                    boneMap.put(name, bd);

                    // Track bounds from vertex data for scaling
                    for (int i = 0; i < bd.vertexCount; i++) {
                        float x = Math.abs(bd.vertexData[i * 8]);
                        float y = bd.vertexData[i * 8 + 1];
                        float z = Math.abs(bd.vertexData[i * 8 + 2]);
                        maxRadius = Math.max(maxRadius, Math.max(x, z));
                        maxHeight = Math.max(maxHeight, y);
                    }
                }
            }

            bones = boneMap.values().toArray(new BoneData[0]);
            modelRadius = maxRadius;
            modelHeight = maxHeight;

            int totalVerts = 0;
            for (BoneData b : bones) totalVerts += b.vertexCount;
            LOGGER.info("Tornado model loaded: {} bones, {} total vertices", bones.length, totalVerts);
        }
    }

    private void bakeBone(BoneData bd, JsonArray cubes, float texWidth, float texHeight) {
        List<int[]> cubeList = new ArrayList<>();
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

        // Bake faces with culling
        List<float[]> vertices = new ArrayList<>();
        for (int[] c : cubeList) {
            int ox = c[0], oy = c[1], oz = c[2];
            int w = c[3], h = c[4], d = c[5];
            int uvX = c[6], uvY = c[7];

            // +X
            if (!isOccluded(occupied, ox + w, oy, oz, 1, h, d, minX, minY, minZ, gridW, gridH, gridD))
                addFace(vertices, ox + w, oy, oz, ox + w, oy + h, oz + d, 1, 0, 0, uvX, uvY, w, h, d, 0, texWidth, texHeight);
            // -X
            if (!isOccluded(occupied, ox - 1, oy, oz, 1, h, d, minX, minY, minZ, gridW, gridH, gridD))
                addFace(vertices, ox, oy, oz, ox, oy + h, oz + d, -1, 0, 0, uvX, uvY, w, h, d, 1, texWidth, texHeight);
            // +Y
            if (!isOccluded(occupied, ox, oy + h, oz, w, 1, d, minX, minY, minZ, gridW, gridH, gridD))
                addFace(vertices, ox, oy + h, oz, ox + w, oy + h, oz + d, 0, 1, 0, uvX, uvY, w, h, d, 2, texWidth, texHeight);
            // -Y
            if (!isOccluded(occupied, ox, oy - 1, oz, w, 1, d, minX, minY, minZ, gridW, gridH, gridD))
                addFace(vertices, ox, oy, oz, ox + w, oy, oz + d, 0, -1, 0, uvX, uvY, w, h, d, 3, texWidth, texHeight);
            // +Z
            if (!isOccluded(occupied, ox, oy, oz + d, w, h, 1, minX, minY, minZ, gridW, gridH, gridD))
                addFace(vertices, ox, oy, oz + d, ox + w, oy + h, oz + d, 0, 0, 1, uvX, uvY, w, h, d, 4, texWidth, texHeight);
            // -Z
            if (!isOccluded(occupied, ox, oy, oz - 1, w, h, 1, minX, minY, minZ, gridW, gridH, gridD))
                addFace(vertices, ox, oy, oz, ox + w, oy + h, oz, 0, 0, -1, uvX, uvY, w, h, d, 5, texWidth, texHeight);
        }

        bd.vertexCount = vertices.size();
        bd.vertexData = new float[bd.vertexCount * 8];
        for (int i = 0; i < bd.vertexCount; i++) {
            System.arraycopy(vertices.get(i), 0, bd.vertexData, i * 8, 8);
        }
    }

    private boolean isOccluded(boolean[][][] grid, int ox, int oy, int oz,
                                int w, int h, int d,
                                int minX, int minY, int minZ,
                                int gridW, int gridH, int gridD) {
        for (int x = ox; x < ox + w; x++) {
            for (int y = oy; y < oy + h; y++) {
                for (int z = oz; z < oz + d; z++) {
                    int gx = x - minX, gy = y - minY, gz = z - minZ;
                    if (gx < 0 || gy < 0 || gz < 0 || gx >= gridW || gy >= gridH || gz >= gridD) return false;
                    if (!grid[gx][gy][gz]) return false;
                }
            }
        }
        return true;
    }

    // Face indices: 0=+X, 1=-X, 2=+Y, 3=-Y, 4=+Z, 5=-Z
    private void addFace(List<float[]> vertices,
                         float x0, float y0, float z0, float x1, float y1, float z1,
                         float nx, float ny, float nz,
                         int uvX, int uvY, int w, int h, int d,
                         int faceIdx, float texW, float texH) {
        float u0, v0, u1, v1;
        switch (faceIdx) {
            case 0 -> { // +X (Right)
                u0 = uvX / texW; v0 = (uvY + d) / texH;
                u1 = (uvX + d) / texW; v1 = (uvY + d + h) / texH;
                vertices.add(new float[]{x1, y0, z1, u0, v1, nx, ny, nz});
                vertices.add(new float[]{x1, y0, z0, u1, v1, nx, ny, nz});
                vertices.add(new float[]{x1, y1, z0, u1, v0, nx, ny, nz});
                vertices.add(new float[]{x1, y1, z1, u0, v0, nx, ny, nz});
            }
            case 1 -> { // -X (Left)
                u0 = (uvX + d + w) / texW; v0 = (uvY + d) / texH;
                u1 = (uvX + d + w + d) / texW; v1 = (uvY + d + h) / texH;
                vertices.add(new float[]{x0, y0, z0, u0, v1, nx, ny, nz});
                vertices.add(new float[]{x0, y0, z1, u1, v1, nx, ny, nz});
                vertices.add(new float[]{x0, y1, z1, u1, v0, nx, ny, nz});
                vertices.add(new float[]{x0, y1, z0, u0, v0, nx, ny, nz});
            }
            case 2 -> { // +Y (Top)
                u0 = (uvX + d) / texW; v0 = uvY / texH;
                u1 = (uvX + d + w) / texW; v1 = (uvY + d) / texH;
                vertices.add(new float[]{x0, y1, z1, u0, v1, nx, ny, nz});
                vertices.add(new float[]{x1, y1, z1, u1, v1, nx, ny, nz});
                vertices.add(new float[]{x1, y1, z0, u1, v0, nx, ny, nz});
                vertices.add(new float[]{x0, y1, z0, u0, v0, nx, ny, nz});
            }
            case 3 -> { // -Y (Bottom)
                u0 = (uvX + d + w) / texW; v0 = uvY / texH;
                u1 = (uvX + d + w + w) / texW; v1 = (uvY + d) / texH;
                vertices.add(new float[]{x0, y0, z0, u0, v0, nx, ny, nz});
                vertices.add(new float[]{x1, y0, z0, u1, v0, nx, ny, nz});
                vertices.add(new float[]{x1, y0, z1, u1, v1, nx, ny, nz});
                vertices.add(new float[]{x0, y0, z1, u0, v1, nx, ny, nz});
            }
            case 4 -> { // +Z (Back)
                u0 = (uvX + d + w + d) / texW; v0 = (uvY + d) / texH;
                u1 = (uvX + d + w + d + w) / texW; v1 = (uvY + d + h) / texH;
                vertices.add(new float[]{x1, y0, z1, u1, v1, nx, ny, nz});
                vertices.add(new float[]{x0, y0, z1, u0, v1, nx, ny, nz});
                vertices.add(new float[]{x0, y1, z1, u0, v0, nx, ny, nz});
                vertices.add(new float[]{x1, y1, z1, u1, v0, nx, ny, nz});
            }
            case 5 -> { // -Z (Front)
                u0 = (uvX + d) / texW; v0 = (uvY + d) / texH;
                u1 = (uvX + d + w) / texW; v1 = (uvY + d + h) / texH;
                vertices.add(new float[]{x0, y0, z0, u1, v1, nx, ny, nz});
                vertices.add(new float[]{x1, y0, z0, u0, v1, nx, ny, nz});
                vertices.add(new float[]{x1, y1, z0, u0, v0, nx, ny, nz});
                vertices.add(new float[]{x0, y1, z0, u1, v0, nx, ny, nz});
            }
        }
    }

    private void loadAnimation() throws Exception {
        try (InputStream is = TornadoModel.class.getResourceAsStream(ANIM_PATH)) {
            if (is == null) {
                LOGGER.warn("Tornado animation not found at {}, using defaults", ANIM_PATH);
                applyDefaultAnimation();
                return;
            }

            JsonObject root = JsonParser.parseReader(new InputStreamReader(is, StandardCharsets.UTF_8)).getAsJsonObject();
            JsonObject animations = root.getAsJsonObject("animations");
            JsonObject spin = animations.getAsJsonObject("spin");

            animationLength = spin.get("animation_length").getAsFloat();
            JsonObject animBones = spin.getAsJsonObject("bones");

            for (BoneData bd : bones) {
                if (!animBones.has(bd.name)) continue;
                JsonObject boneAnim = animBones.getAsJsonObject(bd.name);

                // Parse rotation
                if (boneAnim.has("rotation")) {
                    JsonObject rot = boneAnim.getAsJsonObject("rotation");
                    // Find the end keyframe (non-zero time)
                    for (Map.Entry<String, JsonElement> entry : rot.entrySet()) {
                        float time = Float.parseFloat(entry.getKey());
                        if (time > 0) {
                            JsonArray vec = entry.getValue().getAsJsonObject().getAsJsonArray("vector");
                            float yRot = vec.get(1).getAsFloat();
                            bd.rotationDuration = time;
                            bd.rotationDirection = Math.signum(yRot);
                            break;
                        }
                    }
                }

                // Parse position bob (for debris bones)
                if (boneAnim.has("position")) {
                    JsonObject pos = boneAnim.getAsJsonObject("position");
                    List<float[]> keyframes = new ArrayList<>(); // time, y
                    for (Map.Entry<String, JsonElement> entry : pos.entrySet()) {
                        float time = Float.parseFloat(entry.getKey());
                        JsonArray vec = entry.getValue().getAsJsonObject().getAsJsonArray("vector");
                        float y = vec.get(1).getAsFloat();
                        keyframes.add(new float[]{time, y});
                    }
                    // Expect 3 keyframes: 0->peak->0
                    if (keyframes.size() >= 3) {
                        bd.bobPeakTime = keyframes.get(1)[0];
                        bd.bobPeakY = keyframes.get(1)[1];
                        bd.bobEndTime = keyframes.get(2)[0];
                    }
                }
            }
        }
    }

    private void applyDefaultAnimation() {
        for (BoneData bd : bones) {
            if (bd.name.equals("funnel")) {
                bd.rotationDuration = 1.5f;
                bd.rotationDirection = 1;
            }
        }
    }

    /**
     * Renders the tornado model with animation applied.
     * @param timeSeconds current animation time in seconds (will be wrapped to animation length)
     */
    public void render(PoseStack poseStack, com.mojang.blaze3d.vertex.VertexConsumer consumer,
                       PoseStack.Pose basePose, float timeSeconds) {
        // Not used directly - use renderBone instead via the renderer
    }

    public int getBoneCount() {
        return bones != null ? bones.length : 0;
    }

    public String getBoneName(int index) {
        return bones[index].name;
    }

    /**
     * Calculates the Y rotation in degrees for a bone at the given time.
     */
    public float getBoneRotation(int index, float timeSeconds) {
        BoneData bd = bones[index];
        if (bd.rotationDuration <= 0) return 0;
        // Linear rotation, looping
        float t = (timeSeconds % bd.rotationDuration) / bd.rotationDuration;
        return t * 360.0f * bd.rotationDirection;
    }

    /**
     * Calculates the Y position offset for a bone at the given time.
     */
    public float getBoneBobOffset(int index, float timeSeconds) {
        BoneData bd = bones[index];
        if (bd.bobEndTime <= 0) return 0;
        // Triangle wave: 0 -> peak -> 0, looping over bobEndTime
        float t = timeSeconds % bd.bobEndTime;
        if (t <= bd.bobPeakTime) {
            return bd.bobPeakY * (t / bd.bobPeakTime);
        } else {
            float remaining = bd.bobEndTime - bd.bobPeakTime;
            return bd.bobPeakY * (1.0f - (t - bd.bobPeakTime) / remaining);
        }
    }

    /**
     * Renders a single bone's geometry.
     */
    public void renderBone(VertexConsumer consumer, PoseStack.Pose pose, int boneIndex) {
        BoneData bd = bones[boneIndex];
        for (int i = 0; i < bd.vertexCount; i++) {
            int base = i * 8;
            consumer.addVertex(pose, bd.vertexData[base], bd.vertexData[base + 1], bd.vertexData[base + 2])
                    .setColor(255, 255, 255, 190)
                    .setUv(bd.vertexData[base + 3], bd.vertexData[base + 4])
                    .setOverlay(OverlayTexture.NO_OVERLAY)
                    .setLight(0xF000F0)
                    .setNormal(pose, bd.vertexData[base + 5], bd.vertexData[base + 6], bd.vertexData[base + 7]);
        }
    }

    public float getModelRadius() { return modelRadius; }
    public float getModelHeight() { return modelHeight; }
    public boolean isLoaded() { return loaded && bones != null && bones.length > 0; }
}
