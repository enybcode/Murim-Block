package com.murimblock.cultivation;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;

public record CultivationData(CultivationRealm realm, CultivationStage stage) {
    public static final Codec<CultivationData> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.STRING.xmap(
                    name -> CultivationRealm.bySerializedName(name).orElse(CultivationRealm.QI_SENSING),
                    CultivationRealm::serializedName
            ).fieldOf("realm").forGetter(CultivationData::realm),
            Codec.STRING.xmap(
                    name -> CultivationStage.bySerializedName(name).orElse(CultivationStage.EARLY),
                    CultivationStage::serializedName
            ).fieldOf("stage").forGetter(CultivationData::stage)
    ).apply(instance, CultivationData::new));

    public static final StreamCodec<RegistryFriendlyByteBuf, CultivationData> STREAM_CODEC = StreamCodec.of(
            (buffer, data) -> {
                buffer.writeUtf(data.realm.serializedName());
                buffer.writeUtf(data.stage.serializedName());
            },
            buffer -> new CultivationData(
                    CultivationRealm.bySerializedName(buffer.readUtf()).orElse(CultivationRealm.QI_SENSING),
                    CultivationStage.bySerializedName(buffer.readUtf()).orElse(CultivationStage.EARLY)
            )
    );

    public CultivationData {
        if (realm == null) {
            realm = CultivationRealm.QI_SENSING;
        }
        if (stage == null) {
            stage = CultivationStage.EARLY;
        }
    }

    public static CultivationData initial() {
        return new CultivationData(CultivationRealm.QI_SENSING, CultivationStage.EARLY);
    }

    public String displayName() {
        return realm.displayName() + " - " + stage.displayName();
    }
}
