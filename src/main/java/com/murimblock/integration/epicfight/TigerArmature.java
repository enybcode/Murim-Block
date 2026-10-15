package com.murimblock.integration.epicfight;

import java.util.Map;
import yesman.epicfight.api.animation.Joint;
import yesman.epicfight.api.model.Armature;

/**
 * Joints of assets/murimblock/animmodels/entity/tiger.json (15 joints, root "Root"), from scripts/animation/tiger-epicfight.cjs.
 * Only the joints the attacks strike with are named here.
 */
public final class TigerArmature extends Armature {
    public final Joint body;
    public final Joint head;
    public final Joint frontFootRight;
    public final Joint frontFootLeft;

    public TigerArmature(String name, int jointNumber, Joint rootJoint, Map<String, Joint> jointMap) {
        super(name, jointNumber, rootJoint, jointMap);
        body = getOrLogException(jointMap, "body");
        head = getOrLogException(jointMap, "head");
        frontFootRight = getOrLogException(jointMap, "front_foot_r");
        frontFootLeft = getOrLogException(jointMap, "front_foot_l");
    }
}
