package com.konayuki.statflex.anticheat;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import net.minecraft.block.Block;
import net.minecraft.block.BlockAir;
import net.minecraft.block.BlockLadder;
import net.minecraft.block.material.Material;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.Entity;
import net.minecraft.util.BlockPos;
import net.minecraft.util.MathHelper;

final class AnticheatUtils {
    private static final Minecraft mc = Minecraft.getMinecraft();
    private static final Map<String, Field> fieldCache = new HashMap<String, Field>();
    private static final Set<String> missingFields = new HashSet<String>();

    private AnticheatUtils() {
    }

    static boolean nullCheck() {
        return mc.thePlayer != null && mc.theWorld != null;
    }

    static long timeBetween(long first, long second) {
        return Math.abs(second - first);
    }

    static Block getBlock(BlockPos pos) {
        return mc.theWorld.getBlockState(pos).getBlock();
    }

    static boolean overVoid(double posX, double posY, double posZ) {
        for (int i = (int) posY; i > -1; i--) {
            if (!(getBlock(new BlockPos(posX, (double) i, posZ)) instanceof BlockAir)) {
                return false;
            }
        }

        return true;
    }

    static boolean onLadder(Entity entity) {
        int posX = MathHelper.floor_double(entity.posX);
        int posY = MathHelper.floor_double(entity.posY - 0.2D);
        int posZ = MathHelper.floor_double(entity.posZ);
        Block block = getBlock(new BlockPos(posX, posY, posZ));
        return block instanceof BlockLadder && !entity.onGround;
    }

    static double distanceToGround(Entity entity) {
        if (entity.onGround) {
            return 0.0D;
        }

        double fallDistance = -1.0D;
        double y = entity.posY;
        if (entity.posY % 1.0D == 0.0D) {
            y--;
        }

        for (int i = (int) Math.floor(y); i > -1; i--) {
            if (!isPlaceable(new BlockPos(entity.posX, (double) i, entity.posZ))) {
                fallDistance = y - (double) i;
                break;
            }
        }

        return fallDistance - 1.0D;
    }

    static boolean isPlaceable(BlockPos pos) {
        Block block = getBlock(pos);
        return block.isReplaceable(mc.theWorld, pos) || isFluid(block);
    }

    static boolean isFluid(Block block) {
        Material material = block.getMaterial();
        return material == Material.water || material == Material.lava;
    }

    static double getServerPosX(Entity entity) {
        return getScaledServerPosition(entity, "serverPosX", "field_70118_ct");
    }

    static double getServerPosY(Entity entity) {
        return getScaledServerPosition(entity, "serverPosY", "field_70117_cu");
    }

    static double getServerPosZ(Entity entity) {
        return getScaledServerPosition(entity, "serverPosZ", "field_70116_cv");
    }

    static int getSprintingTicksLeft(Entity entity) {
        Integer value = getIntField(entity, "sprintingTicksLeft", "field_110158_av");
        return value == null ? 0 : value.intValue();
    }

    private static double getScaledServerPosition(Entity entity, String mcpName, String srgName) {
        Integer value = getIntField(entity, mcpName, srgName);
        return value == null ? Double.NaN : value.intValue() / 32.0D;
    }

    private static Integer getIntField(Object target, String mcpName, String srgName) {
        Field field = findField(target.getClass(), mcpName);
        if (field == null) {
            field = findField(target.getClass(), srgName);
        }

        if (field == null) {
            return null;
        }

        try {
            Object value = field.get(target);
            return value instanceof Number ? Integer.valueOf(((Number) value).intValue()) : null;
        } catch (IllegalAccessException ignored) {
            return null;
        }
    }

    private static Field findField(Class<?> type, String name) {
        String cacheKey = type.getName() + "#" + name;
        if (fieldCache.containsKey(cacheKey)) {
            return fieldCache.get(cacheKey);
        }

        if (missingFields.contains(cacheKey)) {
            return null;
        }

        Class<?> current = type;
        while (current != null) {
            try {
                Field field = current.getDeclaredField(name);
                field.setAccessible(true);
                fieldCache.put(cacheKey, field);
                return field;
            } catch (NoSuchFieldException ignored) {
                current = current.getSuperclass();
            }
        }

        missingFields.add(cacheKey);
        return null;
    }
}
