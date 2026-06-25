package com.ghosty.nomadscamps;

import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.WorldRenderer;
import net.minecraft.client.render.block.entity.BlockEntityRenderer;
import net.minecraft.client.render.block.entity.BlockEntityRendererFactory;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.Vec3i;

public class CampBlockEntityRenderer implements BlockEntityRenderer<CampBlockEntity> {

    // Constructor
    public CampBlockEntityRenderer(BlockEntityRendererFactory.Context ctx) { }

    @Override
    public void render(CampBlockEntity entity, float tickDelta, MatrixStack matrices, VertexConsumerProvider vertexConsumers, int light, int overlay) {
        VertexConsumer vertexConsumer = vertexConsumers.getBuffer(RenderLayer.getLines());

        if (entity.getStartVertex().equals(entity.getEndVertex()) && entity.getStartVertex().equals(Vec3i.ZERO))
            return;

        matrices.push();

        WorldRenderer.drawBox(
                matrices,
                vertexConsumer,
                entity.getEndVertex().getX(),
                entity.getEndVertex().getY(),
                entity.getEndVertex().getZ(),
                entity.getStartVertex().getX(),
                entity.getStartVertex().getY(),
                entity.getStartVertex().getZ(),
                0.9F,
                0.9F,
                0.9F,
                1.0F,
                0.5F,
                0.5F,
                0.5F
        );

        matrices.pop();
    }
}
