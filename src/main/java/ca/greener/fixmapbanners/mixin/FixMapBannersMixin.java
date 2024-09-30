package ca.greener.fixmapbanners.mixin;
import java.util.Map;

import net.minecraft.item.map.MapState;
import ca.greener.fixmapbanners.FixMapBanners;
import net.minecraft.item.map.MapBannerMarker;
import net.minecraft.item.map.MapIcon;
import net.minecraft.world.WorldAccess;
import org.jetbrains.annotations.Nullable;
import net.minecraft.text.Text;
import net.minecraft.world.World;
import net.minecraft.registry.RegistryKey;
import net.minecraft.util.math.BlockPos;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Final;




@Mixin(MapState.class)
public abstract class FixMapBannersMixin {

	@Shadow public RegistryKey<World> dimension;
	@Shadow public abstract void markIconsDirty();
	@Shadow public abstract void removeIcon(String id);
	@Shadow public abstract boolean iconCountNotLessThan(int iconCount);
	
	@Shadow private int centerX;
	@Shadow private int centerZ;
	@Shadow private byte scale;
	@Shadow private boolean unlimitedTracking;
	@Shadow private int iconCount;
	@Shadow @Final Map<String, MapIcon> icons;
	@Shadow private @Final Map<String, MapBannerMarker> banners;

	/**
	 * 
	 * @param type
	 * @param world
	 * @param key
	 * @param x
	 * @param z
	 * @param rotation
	 * @param text
	 * @author greener
	 * @reason to fix a bug
	 */
	@Overwrite()
	private void addIcon(MapIcon.Type type, @Nullable WorldAccess world, String key, double x, double z, double rotation, @Nullable Text text) {
        MapIcon mapIcon2;
        MapIcon mapIcon;
        byte d;
		int k;
        int i = 1 << this.scale;
        float f = (float)(x - (double)this.centerX) / (float)i;
        float g = (float)(z - (double)this.centerZ) / (float)i;
        byte b = (byte)((double)(f * 2.0f));
        byte c = (byte)((double)(g * 2.0f));
        if (f >= -64.0f && g >= -64.0f && f < 64.0f && g < 64.0f) {
            d = (byte)((rotation += rotation < 0.0 ? -8.0 : 8.0) * 16.0 / 360.0);
            if (this.dimension == World.NETHER && world != null) {
                k = (int)(world.getLevelProperties().getTimeOfDay() / 10L);
                d = (byte)(k * k * 34187121 + k * 121 >> 15 & 0xF);
            }
        } else if (type == MapIcon.Type.PLAYER) {
            k = 320;
            if (Math.abs(f) < 320.0f && Math.abs(g) < 320.0f) {
                type = MapIcon.Type.PLAYER_OFF_MAP;
            } else if (this.unlimitedTracking) {
                type = MapIcon.Type.PLAYER_OFF_LIMITS;
            } else {
                this.removeIcon(key);
                return;
            }
			FixMapBanners.LOGGER.info("player at {} {} {} {}", f, g);
            d = 0;
            if (f < -64.0f) {
                b = -128;
            }
            if (g < -64.0f) {
                c = -128;
            }
            if (f >= 64.0f) {
                b = 127;
            }
            if (g >= 64.0f) {
                c = 127;
            }
        } else {
            this.removeIcon(key);
            return;
        }
        if (!(mapIcon = new MapIcon(type, b, c, d, text)).equals(mapIcon2 = this.icons.put(key, mapIcon))) {
            if (mapIcon2 != null && mapIcon2.type().shouldUseIconCountLimit()) {
                --this.iconCount;
            }
            if (type.shouldUseIconCountLimit()) {
                ++this.iconCount;
            }
            this.markIconsDirty();
        }
    }



	/**
	 * 
	 * @param world
	 * @param pos
	 * @return
	 * @author greener
	 * @reason to fix a bug
	*/
	@Overwrite()
	public boolean addBanner(WorldAccess world, BlockPos pos) {
		double d = (double)pos.getX();
		double e = (double)pos.getZ();
		int i = 1 << this.scale;
		double f = (d - (double)this.centerX) / (double)i;
		double g = (e - (double)this.centerZ) / (double)i;
		FixMapBanners.LOGGER.info("adding a banner {} {} {} {}", d, e, f, g);
		if (f >= -64.0 && g >= -64.0 && f < 64.0 && g < 64.0) {
			MapBannerMarker mapBannerMarker = MapBannerMarker.fromWorldBlock(world, pos);
			if (mapBannerMarker == null) {
				return false;
			}

			if (this.banners.remove(mapBannerMarker.getKey(), mapBannerMarker)) {
				this.removeIcon(mapBannerMarker.getKey());
				return true;
			}

			if (!this.iconCountNotLessThan(256)) {
				this.banners.put(mapBannerMarker.getKey(), mapBannerMarker);
				this.addIcon(mapBannerMarker.getIconType(), world, mapBannerMarker.getKey(), d, e, 180.0, mapBannerMarker.getName());
				return true;
			}
		}

		return false;
	}

}
