strictSprint


        boolean strictSprint = !(mc.player.isPartlyTouchingWater())
            && !mc.player.hasBlindnessEffect()
            && mc.player.hasVehicle() ? (mc.player.getVehicle().canSprintAsVehicle() && mc.player.getVehicle().isLogicalSideForUpdatingMovement()) : mc.player.getHungerManager().canSprint()
            && (!mc.player.horizontalCollision || mc.player.collidedSoftly);