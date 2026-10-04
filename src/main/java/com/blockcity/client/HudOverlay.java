package com.blockcity.client;

import com.blockcity.BlockCityConfig;
import com.blockcity.BlockCityMod;
import com.blockcity.entity.CarEntity;
import com.blockcity.entity.PoliceEntity;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.block.BlockState;
import net.minecraft.block.MapColor;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.client.texture.NativeImageBackedTexture;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.Heightmap;

/** Minimap rotitor in stil GTA (colt stanga-jos), stele wanted (dreapta-sus) si vitezometru. */
public final class HudOverlay {
    private static final int TEX = 96; // rezolutia texturii minimapului (pixeli)
    private static final Identifier TEX_ID = BlockCityMod.id("minimap_dynamic");
    private static NativeImageBackedTexture texture;
    private static int tickCounter;

    public static void init() {
        ClientTickEvents.END_CLIENT_TICK.register(HudOverlay::tick);
        HudRenderCallback.EVENT.register(HudOverlay::render);
    }

    // ---------- actualizare textura (la fiecare 4 tick-uri) ----------
    private static void tick(MinecraftClient mc) {
        BlockCityConfig cfg = BlockCityConfig.get();
        if (!cfg.minimapEnabled || mc.world == null || mc.player == null) return;
        if (++tickCounter % 4 != 0) return;
        if (texture == null) {
            texture = new NativeImageBackedTexture(TEX, TEX, true);
            mc.getTextureManager().registerTexture(TEX_ID, texture);
        }
        NativeImage img = texture.getImage();
        if (img == null) return;

        ClientWorld world = mc.world;
        PlayerEntity p = mc.player;
        double yaw = Math.toRadians(p.getYaw());
        double fx = -Math.sin(yaw), fz = Math.cos(yaw);   // inainte
        double rx = -Math.cos(yaw), rz = -Math.sin(yaw);  // dreapta
        double zoom = cfg.minimapBlocksPerPixel;
        double radius = TEX / 2.0;
        BlockPos.Mutable pos = new BlockPos.Mutable();

        for (int py = 0; py < TEX; py++) {
            for (int px = 0; px < TEX; px++) {
                double sx = px - TEX / 2.0 + 0.5, sy = py - TEX / 2.0 + 0.5;
                if (sx * sx + sy * sy > radius * radius) { img.setColor(px, py, 0); continue; }
                double wx = p.getX() + (rx * sx + fx * -sy) * zoom;
                double wz = p.getZ() + (rz * sx + fz * -sy) * zoom;
                int bx = MathHelper.floor(wx), bz = MathHelper.floor(wz);
                if (!world.getChunkManager().isChunkLoaded(bx >> 4, bz >> 4)) { img.setColor(px, py, 0xFF202020); continue; }
                int topY = world.getTopY(Heightmap.Type.WORLD_SURFACE, bx, bz) - 1;
                pos.set(bx, topY, bz);
                BlockState st = world.getBlockState(pos);
                MapColor mc2 = st.getMapColor(world, pos);
                int rgb = mc2 == MapColor.CLEAR ? 0x202020 : mc2.color;
                int r = (rgb >> 16) & 0xFF, g = (rgb >> 8) & 0xFF, b = rgb & 0xFF;
                img.setColor(px, py, 0xFF000000 | (b << 16) | (g << 8) | r); // ABGR
            }
        }
        texture.upload();
    }

    // ---------- desenare ----------
    private static void render(DrawContext ctx, RenderTickCounter tickCounter) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player == null || mc.world == null || mc.options.hudHidden) return;
        BlockCityConfig cfg = BlockCityConfig.get();

        // --- wanted ---
        int stars = ClientState.stars;
        if (cfg.wantedEnabled && stars > 0) {
            boolean flash = (mc.world.getTime() / 10) % 2 == 0;
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < 5; i++) sb.append(i < stars ? "\u2605" : "\u2606");
            int color = flash ? 0xFFFFD700 : 0xFFFFFFFF;
            int w = mc.textRenderer.getWidth(sb.toString());
            ctx.drawText(mc.textRenderer, sb.toString(), ctx.getScaledWindowWidth() - w - 10, 10, color, true);
        }

        // --- minimap ---
        int size = cfg.minimapSize;
        int x = 10, y = ctx.getScaledWindowHeight() - size - 10;
        if (cfg.minimapEnabled && texture != null) {
            ctx.fill(x - 2, y - 2, x + size + 2, y + size + 2, 0xAA000000);
            ctx.drawTexture(TEX_ID, x, y, size, size, 0f, 0f, TEX, TEX, TEX, TEX);

            PlayerEntity p = mc.player;
            double yaw = Math.toRadians(p.getYaw());
            double fx = -Math.sin(yaw), fz = Math.cos(yaw), rx = -Math.cos(yaw), rz = -Math.sin(yaw);
            double scale = size / (TEX * cfg.minimapBlocksPerPixel); // pixeli ecran pe bloc
            int cx = x + size / 2, cy = y + size / 2;
            double maxR = size / 2.0 - 3;

            boolean blink = (mc.world.getTime() / 6) % 2 == 0;
            for (PoliceEntity cop : mc.world.getEntitiesByClass(PoliceEntity.class, p.getBoundingBox().expand(TEX), e -> true)) {
                dot(ctx, cx, cy, maxR, scale, cop.getX() - p.getX(), cop.getZ() - p.getZ(), fx, fz, rx, rz, blink ? 0xFFFF2020 : 0xFF2060FF);
            }
            for (CarEntity car : mc.world.getEntitiesByClass(CarEntity.class, p.getBoundingBox().expand(TEX), e -> e != p.getVehicle())) {
                dot(ctx, cx, cy, maxR, scale, car.getX() - p.getX(), car.getZ() - p.getZ(), fx, fz, rx, rz, 0xFFFFE000);
            }
            // jucator (centru) si nord
            ctx.fill(cx - 2, cy - 2, cx + 2, cy + 2, 0xFFFFFFFF);
            ctx.fill(cx - 1, cy - 4, cx + 1, cy - 2, 0xFFFFFFFF); // varf = directia de privire
            double nx = Math.sin(yaw), ny = Math.cos(yaw); // directia nordului pe ecran
            ctx.drawText(mc.textRenderer, "N", (int) (cx + nx * (size / 2.0 - 8)) - 2, (int) (cy + ny * (size / 2.0 - 8)) - 4, 0xFFFF5555, true);
        }

        // --- vitezometru ---
        if (mc.player.getVehicle() instanceof CarEntity car) {
            int kmh = (int) Math.round(Math.abs(car.getSpeedBlocksPerTick()) * 72.0);
            ctx.drawText(mc.textRenderer, kmh + " km/h", x + size + 10, y + size - 24, 0xFFFFFFFF, true);
            ctx.drawText(mc.textRenderer, "HP " + Math.round(car.getCarHealth()), x + size + 10, y + size - 12, 0xFF55FF55, true);
        }
    }

    private static void dot(DrawContext ctx, int cx, int cy, double maxR, double scale,
                            double dx, double dz, double fx, double fz, double rx, double rz, int color) {
        double sx = (dx * rx + dz * rz) * scale;
        double sy = -(dx * fx + dz * fz) * scale;
        double len = Math.sqrt(sx * sx + sy * sy);
        if (len > maxR) { sx *= maxR / len; sy *= maxR / len; }
        int px = (int) Math.round(cx + sx), py = (int) Math.round(cy + sy);
        ctx.fill(px - 1, py - 1, px + 2, py + 2, color);
    }

    private HudOverlay() {}
}
