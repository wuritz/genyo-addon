package com.genyo.render;

import net.minecraft.client.render.entity.state.PlayerEntityRenderState;

/** A frozen, non-mutating copy of a PlayerEntityRenderState, captured at logout. */
public class FrozenPlayerRenderState extends PlayerEntityRenderState {

    public static FrozenPlayerRenderState copyOf(PlayerEntityRenderState src) {
        FrozenPlayerRenderState f = new FrozenPlayerRenderState();

        // EntityRenderState
        f.entityType = src.entityType;
        f.x = src.x; f.y = src.y; f.z = src.z;
        f.age = src.age;
        f.width = src.width; f.height = src.height;
        f.standingEyeHeight = src.standingEyeHeight;
        f.invisible = src.invisible;
        f.sneaking = src.sneaking;
        f.onFire = src.onFire;
        f.light = src.light;
        f.outlineColor = src.outlineColor;

        // LivingEntityRenderState
        f.bodyYaw = src.bodyYaw;
        f.relativeHeadYaw = src.relativeHeadYaw;
        f.pitch = src.pitch;
        f.deathTime = src.deathTime;
        f.limbSwingAnimationProgress = src.limbSwingAnimationProgress;
        f.limbSwingAmplitude = src.limbSwingAmplitude;
        f.baseScale = src.baseScale;
        f.ageScale = src.ageScale;
        f.flipUpsideDown = src.flipUpsideDown;
        f.shaking = src.shaking;
        f.baby = src.baby;
        f.touchingWater = src.touchingWater;
        f.usingRiptide = src.usingRiptide;
        f.hurt = src.hurt;
        f.pose = src.pose;

        // ArmedEntityRenderState
        f.mainArm = src.mainArm;
        f.rightArmPose = src.rightArmPose;
        f.leftArmPose = src.leftArmPose;
        f.rightHandItem = src.rightHandItem.copy();
        f.leftHandItem = src.leftHandItem.copy();
        f.swingAnimationType = src.swingAnimationType;
        f.handSwingProgress = src.handSwingProgress; // frozen value; won't animate further, which is intended

        // BipedEntityRenderState
        f.leaningPitch = src.leaningPitch;
        f.limbAmplitudeInverse = src.limbAmplitudeInverse;
        f.crossbowPullTime = src.crossbowPullTime;
        f.itemUseTime = src.itemUseTime;
        f.preferredArm = src.preferredArm;
        f.activeHand = src.activeHand;
        f.isInSneakingPose = src.isInSneakingPose;
        f.isGliding = src.isGliding;
        f.isSwimming = src.isSwimming;
        f.hasVehicle = src.hasVehicle;
        f.isUsingItem = src.isUsingItem;
        f.equippedHeadStack = src.equippedHeadStack.copy();
        f.equippedChestStack = src.equippedChestStack.copy();
        f.equippedLegsStack = src.equippedLegsStack.copy();
        f.equippedFeetStack = src.equippedFeetStack.copy();

        // PlayerEntityRenderState
        f.skinTextures = src.skinTextures;
        f.spectator = src.spectator;
        f.hatVisible = src.hatVisible;
        f.jacketVisible = src.jacketVisible;
        f.leftPantsLegVisible = src.leftPantsLegVisible;
        f.rightPantsLegVisible = src.rightPantsLegVisible;
        f.leftSleeveVisible = src.leftSleeveVisible;
        f.rightSleeveVisible = src.rightSleeveVisible;

        return f;
    }
}
