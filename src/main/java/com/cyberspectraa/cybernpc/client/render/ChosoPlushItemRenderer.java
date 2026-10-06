package com.cyberspectraa.cybernpc.client.render;

import com.cyberspectraa.cybernpc.CyberNpc;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import org.joml.Matrix3f;
import org.joml.Matrix4f;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/**
 * Static plush renderer.
 *
 * The plush geometry is read from the baked doll mesh resource and submitted
 * directly to Minecraft's entity-translucent pipeline.  No HumanoidModel or
 * PlayerModel is involved, so there is no limb animation state to leak into
 * the item.  The mesh remains in normal item-model coordinates, allowing the
 * item JSON's GUI/ground/fixed/hand transforms to work normally.
 */
public final class ChosoPlushItemRenderer extends BlockEntityWithoutLevelRenderer {
    private static final Logger LOGGER = LoggerFactory.getLogger("CyberNpc/ChosoPlush");

    private static final ResourceLocation TEXTURE =
            new ResourceLocation(CyberNpc.MOD_ID, "textures/item/choso_plush.png");
    private static final ResourceLocation MESH_RESOURCE =
            new ResourceLocation(CyberNpc.MOD_ID, "models/item/choso_plush.obj");

    private volatile Mesh mesh;

    public ChosoPlushItemRenderer() {
        super(
                Minecraft.getInstance().getBlockEntityRenderDispatcher(),
                Minecraft.getInstance().getEntityModels()
        );
    }

    @Override
    public void renderByItem(
            ItemStack stack,
            ItemDisplayContext displayContext,
            PoseStack poseStack,
            MultiBufferSource buffer,
            int packedLight,
            int packedOverlay
    ) {
        Mesh current = mesh;
        if (current == null) {
            current = loadMesh();
            mesh = current;
        }
        if (current.vertices.length == 0) {
            return;
        }

        VertexConsumer consumer = buffer.getBuffer(RenderType.entityTranslucent(TEXTURE));
        PoseStack.Pose pose = poseStack.last();
        Matrix4f positionMatrix = pose.pose();
        Matrix3f normalMatrix = pose.normal();

        for (MeshVertex vertex : current.vertices) {
            consumer.vertex(positionMatrix, vertex.x, vertex.y, vertex.z)
                    .color(255, 255, 255, 255)
                    .uv(vertex.u, vertex.v)
                    .overlayCoords(packedOverlay)
                    .uv2(packedLight)
                    .normal(normalMatrix, vertex.nx, vertex.ny, vertex.nz)
                    .endVertex();
        }
    }

    private static Mesh loadMesh() {
        List<float[]> positions = new ArrayList<>();
        List<float[]> uvs = new ArrayList<>();
        List<float[]> normals = new ArrayList<>();
        List<MeshVertex> output = new ArrayList<>();

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(
                Minecraft.getInstance().getResourceManager()
                        .open(MESH_RESOURCE),
                StandardCharsets.UTF_8
        ))) {
            String line;
            while ((line = reader.readLine()) != null) {
                String trimmed = line.trim();
                if (trimmed.isEmpty() || trimmed.startsWith("#")) {
                    continue;
                }

                String[] tokens = trimmed.split("\\s+");
                switch (tokens[0]) {
                    case "v" -> positions.add(new float[]{
                            Float.parseFloat(tokens[1]),
                            Float.parseFloat(tokens[2]),
                            Float.parseFloat(tokens[3])
                    });
                    case "vt" -> uvs.add(new float[]{
                            Float.parseFloat(tokens[1]),
                            // The OBJ resource was authored for Forge's flip_v=true path.
                            // Entity textures use Minecraft's normal top-down V direction.
                            1.0F - Float.parseFloat(tokens[2])
                    });
                    case "vn" -> normals.add(new float[]{
                            Float.parseFloat(tokens[1]),
                            Float.parseFloat(tokens[2]),
                            Float.parseFloat(tokens[3])
                    });
                    case "f" -> appendFace(tokens, positions, uvs, normals, output);
                    default -> {
                        // Material/group/object declarations are intentionally ignored.
                    }
                }
            }
        } catch (Exception exception) {
            LOGGER.error("Failed to load Choso plush mesh {}", MESH_RESOURCE, exception);
            return Mesh.EMPTY;
        }

        return new Mesh(output.toArray(MeshVertex[]::new));
    }

    private static void appendFace(
            String[] tokens,
            List<float[]> positions,
            List<float[]> uvs,
            List<float[]> normals,
            List<MeshVertex> output
    ) throws IOException {
        if (tokens.length < 4) {
            return;
        }

        // The source mesh uses quads.  If a resource pack supplies a polygon
        // with more vertices, fan-triangulate it while preserving winding.
        int[][] refs = new int[tokens.length - 1][];
        for (int i = 1; i < tokens.length; i++) {
            String[] pieces = tokens[i].split("/");
            if (pieces.length < 3) {
                throw new IOException("Choso plush OBJ face is missing UV or normal data");
            }
            refs[i - 1] = new int[]{
                    parseObjIndex(pieces[0], positions.size()),
                    parseObjIndex(pieces[1], uvs.size()),
                    parseObjIndex(pieces[2], normals.size())
            };
        }

        if (refs.length == 4) {
            for (int[] ref : refs) {
                output.add(vertexOf(ref, positions, uvs, normals));
            }
            return;
        }

        // VertexConsumer quads require groups of four vertices.  Degenerate
        // the fourth point for triangle fans; the bundled plush never needs
        // this fallback, but it keeps the parser safe for overrides.
        for (int i = 1; i < refs.length - 1; i++) {
            MeshVertex a = vertexOf(refs[0], positions, uvs, normals);
            MeshVertex b = vertexOf(refs[i], positions, uvs, normals);
            MeshVertex c = vertexOf(refs[i + 1], positions, uvs, normals);
            output.add(a);
            output.add(b);
            output.add(c);
            output.add(c);
        }
    }

    private static MeshVertex vertexOf(
            int[] ref,
            List<float[]> positions,
            List<float[]> uvs,
            List<float[]> normals
    ) {
        float[] p = positions.get(ref[0]);
        float[] uv = uvs.get(ref[1]);
        float[] n = normals.get(ref[2]);
        return new MeshVertex(
                p[0], p[1], p[2],
                uv[0], uv[1],
                n[0], n[1], n[2]
        );
    }

    private static int parseObjIndex(String raw, int size) {
        int index = Integer.parseInt(raw);
        return index < 0 ? size + index : index - 1;
    }

    private record Mesh(MeshVertex[] vertices) {
        private static final Mesh EMPTY = new Mesh(new MeshVertex[0]);
    }

    private record MeshVertex(
            float x,
            float y,
            float z,
            float u,
            float v,
            float nx,
            float ny,
            float nz
    ) {
    }
}
