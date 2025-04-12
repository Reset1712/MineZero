package boomcow.minezero.event;

import boomcow.minezero.checkpoint.WorldData;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.portal.PortalShape;
import net.minecraftforge.event.level.BlockEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.lang.reflect.Field;

@Mod.EventBusSubscriber
public class PortalChangeHandler {



    @SubscribeEvent
    public static void onPortalCreate(BlockEvent.PortalSpawnEvent event) {
        if (!(event.getLevel() instanceof ServerLevel level)) return;

        PortalShape shape = event.getPortalSize();
        int width = getPrivateIntField(shape, "width");
        int height = getPrivateIntField(shape, "height");
        BlockPos bottomLeft = getPrivateBlockPosField(shape, "bottomLeft");
        Direction.Axis axis = getPrivateAxisField(shape);
        Direction right = axis == Direction.Axis.X ? Direction.EAST : Direction.SOUTH;



        for (int w = 0; w < width; w++) {
            for (int h = 0; h < height; h++) {
                BlockPos pos = bottomLeft.relative(right, w).above(h).immutable();
                WorldData.createdPortals.add(pos);
                WorldData.destroyedPortals.remove(pos);
                WorldData.blockDimensionIndices.put(pos, WorldData.getDimensionIndex(level.dimension()));
            }
        }
    }
    private static int getPrivateIntField(PortalShape shape, String fieldName) {
        try {
            Field field = PortalShape.class.getDeclaredField(fieldName);
            field.setAccessible(true);
            return field.getInt(shape);
        } catch (Exception e) {
            throw new RuntimeException("Failed to access field: " + fieldName, e);
        }
    }

    private static BlockPos getPrivateBlockPosField(PortalShape shape, String fieldName) {
        try {
            Field field = PortalShape.class.getDeclaredField(fieldName);
            field.setAccessible(true);
            return (BlockPos) field.get(shape);
        } catch (Exception e) {
            throw new RuntimeException("Failed to access field: " + fieldName, e);
        }
    }

    private static Direction.Axis getPrivateAxisField(PortalShape shape) {
        try {
            Field field = PortalShape.class.getDeclaredField("axis");
            field.setAccessible(true);
            return (Direction.Axis) field.get(shape);
        } catch (Exception e) {
            throw new RuntimeException("Failed to access axis", e);
        }
    }

}
