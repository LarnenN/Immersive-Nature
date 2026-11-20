package com.immersive_nature.snow;

/** Snow settings, filled from the "snow" section of the Immersive Nature config (PlatformConfigs). */
public final class SnowCommonConfig {

	public static boolean snowGravity = true;
	public static boolean snowMakingIce = true;
	public static boolean snowAlwaysReplaceable = true;
	public static boolean thinnerBoundingBox = true;
	public static boolean snowNeverMelt = false;
	public static int snowSpawnMaxLightLevel = 9;
	public static int snowPersistMaxLightLevel = 11;
	public static boolean snowReduceFallDamage = true;
	public static boolean sustainGrassIfLayerMoreThanOne = true;
	public static boolean sneakSnowball = true;
	public static boolean fancySnowOnUpperSlab = true;
	public static int mobSpawningMaxLayers = 8;
	public static boolean placeSnowOnBlock = true;
	public static boolean placeSnowOnBlockNaturally = true;
	public static boolean replaceWorldFeature = true;
	public static boolean retainOriginalBlocks = false;
	public static boolean snowAccumulationDuringSnowfall = false;
	public static boolean snowAccumulationDuringSnowstorm = true;
	public static int snowAccumulationMaxLayers = 8;
	public static boolean snowAndIceMeltInWarmBiomes = false;
	public static boolean snowNaturalMelt = true;

	public static boolean accumulationWinterOnly = false;
	public static boolean debugSpawningCommand = false;
	public static int weatherTickSlowness = 16;
	public static boolean preventWorldgenLogSpam = true;
	public static boolean skipSnowLoggingMushroom = true;

	public static boolean canPlaceSnowInBlock() {
		return placeSnowOnBlock && !retainOriginalBlocks;
	}

}
