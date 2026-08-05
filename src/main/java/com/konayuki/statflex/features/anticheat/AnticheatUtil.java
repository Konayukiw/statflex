package com.konayuki.statflex.features.anticheat;

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
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemBlock;
import net.minecraft.util.BlockPos;
import net.minecraft.util.MathHelper;

public final class AnticheatUtil {
    private static final Minecraft mc = Minecraft.getMinecraft();
    private static final Map<String, Field> fieldCache = new HashMap<String, Field>();
    private static final Set<String> missingFields = new HashSet<String>();
    public int fastTick;
    public int autoBlockTicks;
    public int ticksExisted;
    public int lastSneakTick;
    public int sneakTicks;
    public int noSlowTicks;
    public int aboveVoidTicks;
    public double speed;
    public double posX, posY, posZ;
    public double serverPosX = Double.NaN;
    public double serverPosY = Double.NaN;
    public double serverPosZ = Double.NaN;
    public boolean sneaking;
    public EntityPlayer player;

    public void update(EntityPlayer player) {
        int currentTicks = player.ticksExisted;
        posX = player.posX - player.prevPosX;
        posY = player.posY - player.prevPosY;
        posZ = player.posZ - player.prevPosZ;
        speed = Math.max(Math.abs(posX), Math.abs(posZ));

        if (speed >= 0.07D) {
            fastTick++;
            ticksExisted = currentTicks;
        } else {
            fastTick = 0;
        }

        if (Math.abs(posY) >= 0.1D) {
            aboveVoidTicks = currentTicks;
        }

        if (player.isSneaking()) {
            lastSneakTick = currentTicks;
        }

        if (player.isSwingInProgress && player.isBlocking()) {
            autoBlockTicks++;
        } else {
            autoBlockTicks = 0;
        }

        if (player.isSprinting() && player.isUsingItem()) {
            noSlowTicks++;
        } else {
            noSlowTicks = 0;
        }

        if (player.rotationPitch >= 70.0F
                && player.getHeldItem() != null
                && player.getHeldItem().getItem() instanceof ItemBlock) {
            if (player.swingProgressInt == 1) {
                if (!sneaking && player.isSneaking()) {
                    sneakTicks++;
                } else {
                    sneakTicks = 0;
                }
            }
        } else {
            sneakTicks = 0;
        }
    }

    public void sneak(EntityPlayer player) {
        sneaking = player.isSneaking();
    }

    public void syncPos(EntityPlayer player) {
        serverPosX = serverX(player);
        serverPosY = serverY(player);
        serverPosZ = serverZ(player);
    }

    public static boolean ready() {
        return mc.thePlayer != null && mc.theWorld != null;
    }

    public static long since(long first, long second) {
        return Math.abs(second - first);
    }

    public static Block block(BlockPos pos) {
        return mc.theWorld.getBlockState(pos).getBlock();
    }

    public static boolean overVoid(double posX, double posY, double posZ) {
        for (int i = (int) posY; i > -1; i--) {
            if (!(block(new BlockPos(posX, (double) i, posZ)) instanceof BlockAir)) {
                return false;
            }
        }

        return true;
    }

    public static boolean onLadder(Entity entity) {
        int posX = MathHelper.floor_double(entity.posX);
        int posY = MathHelper.floor_double(entity.posY - 0.2D);
        int posZ = MathHelper.floor_double(entity.posZ);
        Block block = block(new BlockPos(posX, posY, posZ));
        return block instanceof BlockLadder && !entity.onGround;
    }

    public static double toGround(Entity entity) {
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

    public static boolean isPlaceable(BlockPos pos) {
        Block block = block(pos);
        return block.isReplaceable(mc.theWorld, pos) || isFluid(block);
    }

    public static boolean isFluid(Block block) {
        Material material = block.getMaterial();
        return material == Material.water || material == Material.lava;
    }

    public static double serverX(Entity entity) {
        return scaled(entity, "serverPosX", "field_70118_ct");
    }

    public static double serverY(Entity entity) {
        return scaled(entity, "serverPosY", "field_70117_cu");
    }

    public static double serverZ(Entity entity) {
        return scaled(entity, "serverPosZ", "field_70116_cv");
    }

    private static double scaled(Entity entity, String mcpName, String srgName) {
        Integer value = intField(entity, mcpName, srgName);
        return value == null ? Double.NaN : value.intValue() / 32.0D;
    }

    private static Integer intField(Object target, String mcpName, String srgName) {
        Field field = field(target.getClass(), mcpName);
        if (field == null) {
            field = field(target.getClass(), srgName);
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

    private static Field field(Class<?> type, String name) {
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