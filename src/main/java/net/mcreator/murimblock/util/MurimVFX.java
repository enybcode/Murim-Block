package net.mcreator.murimblock.util;

import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;

public final class MurimVFX {
	private static final double TAU = Math.PI * 2.0;

	private MurimVFX() {
	}

	public static void auraIgnition(ServerLevel level, Entity entity) {
		double x = entity.getX();
		double y = entity.getY() + 0.2;
		double z = entity.getZ();
		ring(level, ParticleTypes.SOUL_FIRE_FLAME, x, y + 0.15, z, 0.75, 22, 0.03);
		ring(level, ParticleTypes.END_ROD, x, y + 0.55, z, 0.55, 12, 0.01);
		level.sendParticles(ParticleTypes.FLASH, x, y + 1.0, z, 1, 0, 0, 0, 0);
	}

	public static void auraCollapse(ServerLevel level, Entity entity) {
		double x = entity.getX();
		double y = entity.getY() + 0.75;
		double z = entity.getZ();
		ring(level, ParticleTypes.SOUL, x, y, z, 0.5, 14, -0.01);
		level.sendParticles(ParticleTypes.SMOKE, x, y, z, 10, 0.25, 0.5, 0.25, 0.02);
	}

	public static void qiAuraTick(ServerLevel level, Entity entity) {
		if (entity.tickCount % 2 != 0) {
			return;
		}
		double baseX = entity.getX();
		double baseY = entity.getY();
		double baseZ = entity.getZ();
		double phase = entity.tickCount * 0.22;

		for (int i = 0; i < 6; i++) {
			double height = 0.15 + i * 0.32;
			double radius = 0.42 + Math.sin(phase + i * 0.65) * 0.12;
			double angle = phase + i * 1.35;
			double x = baseX + Math.cos(angle) * radius;
			double z = baseZ + Math.sin(angle) * radius;
			spawn(level, ParticleTypes.SOUL_FIRE_FLAME, x, baseY + height, z, 0, 0.035, 0, 0.02);
			if (i % 2 == 0) {
				spawn(level, ParticleTypes.END_ROD, x, baseY + height + 0.1, z, 0, 0.018, 0, 0.004);
			}
		}

		if (entity.tickCount % 8 == 0) {
			level.sendParticles(ParticleTypes.SOUL, baseX, baseY + 0.9, baseZ, 5, 0.35, 0.65, 0.35, 0.015);
		}
	}

	public static void breakthroughAscension(ServerLevel level, Entity entity) {
		double x = entity.getX();
		double y = entity.getY();
		double z = entity.getZ();

		level.sendParticles(ParticleTypes.FLASH, x, y + 1.1, z, 2, 0.1, 0.1, 0.1, 0);
		for (int step = 0; step < 6; step++) {
			ring(level, ParticleTypes.SOUL_FIRE_FLAME, x, y + 0.25 + step * 0.35, z, 0.55 + step * 0.18, 28, 0.035 + step * 0.004);
			ring(level, ParticleTypes.END_ROD, x, y + 0.35 + step * 0.35, z, 0.35 + step * 0.11, 16, 0.02);
		}
		level.sendParticles(ParticleTypes.SONIC_BOOM, x, y + 1.0, z, 1, 0, 0, 0, 0);
		level.sendParticles(ParticleTypes.CLOUD, x, y + 0.05, z, 45, 1.1, 0.15, 1.1, 0.04);
		level.sendParticles(ParticleTypes.SOUL, x, y + 1.1, z, 90, 0.85, 1.8, 0.85, 0.08);
	}

	public static void qiAbsorbOnKill(ServerLevel level, Entity victim, Entity cultivator, boolean strong) {
		double startX = victim.getX();
		double startY = victim.getY() + victim.getBbHeight() * 0.55;
		double startZ = victim.getZ();
		double endX = cultivator.getX();
		double endY = cultivator.getY() + cultivator.getBbHeight() * 0.8;
		double endZ = cultivator.getZ();
		int points = strong ? 18 : 10;

		for (int i = 0; i <= points; i++) {
			double t = i / (double) points;
			double arc = Math.sin(t * Math.PI) * (strong ? 0.9 : 0.45);
			double x = lerp(startX, endX, t);
			double y = lerp(startY, endY, t) + arc;
			double z = lerp(startZ, endZ, t);
			spawn(level, strong ? ParticleTypes.SOUL_FIRE_FLAME : ParticleTypes.SOUL, x, y, z, 0, 0.02, 0, 0.015);
			if (strong && i % 3 == 0) {
				spawn(level, ParticleTypes.END_ROD, x, y, z, 0, 0.01, 0, 0.004);
			}
		}

		if (strong) {
			ring(level, ParticleTypes.SOUL_FIRE_FLAME, endX, cultivator.getY() + 0.2, endZ, 0.65, 18, 0.025);
		} else {
			level.sendParticles(ParticleTypes.SOUL, endX, endY, endZ, 8, 0.2, 0.35, 0.2, 0.02);
		}
	}

	public static void pillRefinement(ServerLevel level, Entity entity) {
		double x = entity.getX();
		double y = entity.getY() + 0.75;
		double z = entity.getZ();
		level.sendParticles(ParticleTypes.HAPPY_VILLAGER, x, y, z, 12, 0.35, 0.45, 0.35, 0.02);
		level.sendParticles(ParticleTypes.END_ROD, x, y + 0.2, z, 10, 0.22, 0.35, 0.22, 0.01);
		ring(level, ParticleTypes.SOUL, x, entity.getY() + 0.15, z, 0.45, 12, 0.015);
	}

	public static void swordArc(ServerLevel level, Entity entity, double range, int points) {
		Vec3 look = entity.getLookAngle().normalize();
		Vec3 side = new Vec3(-look.z, 0, look.x).normalize();
		Vec3 center = entity.position().add(0, entity.getBbHeight() * 0.65, 0).add(look.scale(range * 0.55));

		for (int i = 0; i <= points; i++) {
			double t = i / (double) points;
			double sweep = (t - 0.5) * 2.2;
			Vec3 pos = center.add(side.scale(Math.sin(sweep) * range * 0.75)).add(look.scale(Math.cos(sweep) * range * 0.25));
			double lift = Math.sin(t * Math.PI) * 0.55;
			spawn(level, ParticleTypes.SWEEP_ATTACK, pos.x, pos.y + lift, pos.z, 0, 0, 0, 0);
			if (i % 2 == 0) {
				spawn(level, ParticleTypes.END_ROD, pos.x, pos.y + lift, pos.z, 0, 0.01, 0, 0.004);
			}
		}
	}

	public static void impactBurst(ServerLevel level, Entity target) {
		double x = target.getX();
		double y = target.getY() + target.getBbHeight() * 0.5;
		double z = target.getZ();
		level.sendParticles(ParticleTypes.CRIT, x, y, z, 16, 0.35, 0.45, 0.35, 0.12);
		level.sendParticles(ParticleTypes.SOUL_FIRE_FLAME, x, y, z, 8, 0.2, 0.3, 0.2, 0.05);
		level.sendParticles(ParticleTypes.FLASH, x, y, z, 1, 0, 0, 0, 0);
	}

	public static void demonicGuardPulse(ServerLevel level, Entity entity) {
		double x = entity.getX();
		double y = entity.getY() + 0.8;
		double z = entity.getZ();
		ring(level, ParticleTypes.SMOKE, x, y, z, 0.85, 24, 0.015);
		ring(level, ParticleTypes.SOUL_FIRE_FLAME, x, y, z, 0.55, 18, 0.025);
		level.sendParticles(ParticleTypes.LARGE_SMOKE, x, y, z, 8, 0.35, 0.35, 0.35, 0.02);
	}

	private static void ring(ServerLevel level, ParticleOptions particle, double x, double y, double z, double radius, int points, double speed) {
		for (int i = 0; i < points; i++) {
			double angle = TAU * i / points;
			double px = x + Math.cos(angle) * radius;
			double pz = z + Math.sin(angle) * radius;
			double vx = Math.cos(angle) * speed;
			double vz = Math.sin(angle) * speed;
			spawn(level, particle, px, y, pz, vx, 0.015, vz, Math.abs(speed));
		}
	}

	private static void spawn(ServerLevel level, ParticleOptions particle, double x, double y, double z, double dx, double dy, double dz, double speed) {
		level.sendParticles(particle, x, y, z, 1, dx, dy, dz, speed);
	}

	private static double lerp(double a, double b, double t) {
		return a + (b - a) * t;
	}
}
