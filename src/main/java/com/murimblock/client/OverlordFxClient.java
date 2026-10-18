package com.murimblock.client;

import com.lowdragmc.photon.client.light.DynamicLight;
import com.lowdragmc.photon.client.light.PhotonLights;
import com.murimblock.network.OverlordFlashPayload;

/** The Overlord Spear's flashes on this client (martial/OverlordFx): a wide blood-red light and a hot gold one at its heart. */
public final class OverlordFxClient {
    private OverlordFxClient() {
    }

    public static void flash(OverlordFlashPayload p) {
        if (!PhotonLights.isEnabled()) return;
        boolean volumetric = PhotonLights.isVolumetricEnabled();
        PhotonLights.flash(new DynamicLight().point().at(p.x(), p.y(), p.z()).color(1.0F, 0.16F, 0.08F)
                .intensity(2.2F).range(p.range()).sourceRadius(p.range() * 0.12F).volumetric(volumetric ? 0.5F : 0), p.ticks());
        PhotonLights.flash(new DynamicLight().point().at(p.x(), p.y(), p.z()).color(1.0F, 0.72F, 0.3F)
                .intensity(1.6F).range(p.range() * 0.45F).sourceRadius(p.range() * 0.06F).volumetric(0), Math.max(3, p.ticks() / 2));
    }
}
