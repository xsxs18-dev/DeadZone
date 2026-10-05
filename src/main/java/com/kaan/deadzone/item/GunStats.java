package com.kaan.deadzone.item;

/**
 * @param damage      Schaden pro Kugel (bei Schrotflinten pro Schrotkugel)
 * @param fireDelay   Ticks zwischen zwei Schüssen
 * @param spread      Streuung in Grad (halbiert beim Schleichen)
 * @param pellets     Kugeln pro Schuss
 * @param pierce      wie viele Gegner eine Kugel zusätzlich durchschlägt
 * @param explosion   Explosionsstärke beim Einschlag, 0 = keine
 * @param breaksBlocks ob die Explosion Blöcke zerstört
 */
public record GunStats(
		AmmoType ammo,
		float damage,
		int fireDelay,
		int magazine,
		int reloadTicks,
		float spread,
		int pellets,
		double range,
		boolean automatic,
		int pierce,
		float explosion,
		boolean breaksBlocks,
		GunSound sound
) {
	public static Builder of(AmmoType ammo, float damage, GunSound sound) {
		return new Builder(ammo, damage, sound);
	}

	public float shotsPerSecond() {
		return 20.0F / this.fireDelay;
	}

	public static class Builder {
		private final AmmoType ammo;
		private final float damage;
		private final GunSound sound;
		private int fireDelay = 10;
		private int magazine = 12;
		private int reloadTicks = 30;
		private float spread = 1.0F;
		private int pellets = 1;
		private double range = 48;
		private boolean automatic;
		private int pierce;
		private float explosion;
		private boolean breaksBlocks;

		Builder(AmmoType ammo, float damage, GunSound sound) {
			this.ammo = ammo;
			this.damage = damage;
			this.sound = sound;
		}

		public Builder delay(int ticks) {
			this.fireDelay = ticks;
			return this;
		}

		public Builder mag(int size, int reloadTicks) {
			this.magazine = size;
			this.reloadTicks = reloadTicks;
			return this;
		}

		public Builder spread(float degrees) {
			this.spread = degrees;
			return this;
		}

		public Builder pellets(int pellets) {
			this.pellets = pellets;
			return this;
		}

		public Builder range(double range) {
			this.range = range;
			return this;
		}

		public Builder auto() {
			this.automatic = true;
			return this;
		}

		public Builder pierce(int targets) {
			this.pierce = targets;
			return this;
		}

		public Builder explosion(float power, boolean breaksBlocks) {
			this.explosion = power;
			this.breaksBlocks = breaksBlocks;
			return this;
		}

		public GunStats build() {
			return new GunStats(ammo, damage, fireDelay, magazine, reloadTicks, spread, pellets, range, automatic, pierce, explosion, breaksBlocks, sound);
		}
	}
}
