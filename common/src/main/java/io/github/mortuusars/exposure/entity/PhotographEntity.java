package io.github.mortuusars.exposure.entity;

import com.google.common.base.Preconditions;
import com.mojang.datafixers.util.Either;
import com.mojang.logging.LogUtils;
import io.github.mortuusars.exposure.Exposure;
import io.github.mortuusars.exposure.PlatformHelper;
import io.github.mortuusars.exposure.item.PhotographItem;
import net.minecraft.block.AbstractRedstoneGateBlock;
import net.minecraft.block.BlockState;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityDimensions;
import net.minecraft.entity.EntityPose;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.damage.DamageTypes;
import net.minecraft.entity.data.DataTracker;
import net.minecraft.entity.data.TrackedData;
import net.minecraft.entity.data.TrackedDataHandlerRegistry;
import net.minecraft.entity.decoration.AbstractDecorationEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.network.listener.ClientPlayPacketListener;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.s2c.play.EntitySpawnS2CPacket;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.sound.SoundEvent;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3i;
import net.minecraft.world.World;
import org.apache.commons.lang3.Validate;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class PhotographEntity extends AbstractDecorationEntity {
    protected static final TrackedData<ItemStack> DATA_ITEM = DataTracker.registerData(PhotographEntity.class, TrackedDataHandlerRegistry.ITEM_STACK);
    protected static final TrackedData<Boolean> DATA_GLOWING = DataTracker.registerData(PhotographEntity.class, TrackedDataHandlerRegistry.BOOLEAN);
    protected static final TrackedData<Integer> DATA_ROTATION = DataTracker.registerData(PhotographEntity.class, TrackedDataHandlerRegistry.INTEGER);

    @Nullable
    private Either<String, Identifier> idOrTexture;

    public PhotographEntity(EntityType<? extends PhotographEntity> entityType, World level) {
        super(entityType, level);
    }

    public PhotographEntity(World level, BlockPos pos, Direction facingDirection, ItemStack photographStack) {
        super(Exposure.EntityTypes.PHOTOGRAPH.get(), level, pos);
        setFacing(facingDirection);
        setItem(photographStack);
    }


    // Entity:


    @Override
    public boolean shouldRender(double distance) {
        double d = 16.0;
        d *= 64.0 * getRenderDistanceMultiplier();
        return distance < d * d;
    }

    protected void initDataTracker() {
        this.getDataTracker().startTracking(DATA_ITEM, ItemStack.EMPTY);
        this.getDataTracker().startTracking(DATA_GLOWING, false);
        this.getDataTracker().startTracking(DATA_ROTATION, 0);
    }

    public void onTrackedDataSet(TrackedData<?> key) {
        if (key.equals(DATA_ITEM)) {
            this.onItemChanged(this.getItem());
        }
    }

    @Override
    public void onSpawnPacket(@NotNull EntitySpawnS2CPacket packet) {
        super.onSpawnPacket(packet);
        this.setFacing(Direction.byId(packet.getEntityData()));
    }

    @Override
    public @NotNull Packet<ClientPlayPacketListener> createSpawnPacket() {
        return new EntitySpawnS2CPacket(this, this.facing.getId(), this.getDecorationBlockPos());
    }

    public void writeCustomDataToNbt(@NotNull NbtCompound tag) {
        super.writeCustomDataToNbt(tag);
        if (!this.getItem().isEmpty()) {
            tag.put("Item", this.getItem().writeNbt(new NbtCompound()));
            tag.putBoolean("Glowing", this.isGlowing());
            tag.putByte("ItemRotation", (byte)this.getRotation());
        }

        tag.putByte("Facing", (byte)this.facing.getId());
        tag.putBoolean("Invisible", this.isInvisible());
    }

    /**
     * (abstract) Protected helper method to read subclass entity data from NBT.
     */
    public void readCustomDataFromNbt(@NotNull NbtCompound tag) {
        super.readCustomDataFromNbt(tag);
        NbtCompound compoundtag = tag.getCompound("Item");
        if (!compoundtag.isEmpty()) {
            ItemStack itemstack = ItemStack.fromNbt(compoundtag);
            if (itemstack.isEmpty())
                LogUtils.getLogger().warn("Unable to load item from: {}", compoundtag);

            setItem(itemstack);
            setGlowing(tag.getBoolean("Glowing"));
            setRotation(tag.getByte("ItemRotation"));
        }

        this.setFacing(Direction.byId(tag.getByte("Facing")));
        this.setInvisible(tag.getBoolean("Invisible"));
    }


    // Properties:

    public @Nullable Either<String, Identifier> getIdOrTexture() {
        return idOrTexture;
    }

    @Override
    protected float getEyeHeight(@NotNull EntityPose pose, @NotNull EntityDimensions dimensions) {
        return 0f;
    }

    @Override
    public int getWidthPixels() {
        return 16;
    }

    @Override
    public int getHeightPixels() {
        return 16;
    }

    @Nullable
    @Override
    public ItemStack getPickBlockStack() {
        return getItem().copy();
    }

    @SuppressWarnings("deprecation")
    @Override
    public boolean canStayAttached() {
        if (!this.getWorld().isSpaceEmpty(this)) {
            return false;
        } else {
            BlockState blockstate = this.getWorld().getBlockState(this.attachmentPos.offset(this.facing.getOpposite()));
            return (blockstate.isSolid() || this.facing.getAxis().isHorizontal()
                    && AbstractRedstoneGateBlock.isRedstoneGate(blockstate))
                    && this.getWorld().getOtherEntities(this, this.getBoundingBox(), PREDICATE).isEmpty();
        }
    }

    @Override
    protected void updateAttachmentPosition() {
        //noinspection ConstantValue
        if (this.facing == null) {
            // When called from HangingEntity constructor direction is null
            return;
        }

        double value = 0.46875D;
        double d1 = (double)this.attachmentPos.getX() + 0.5D - (double)this.facing.getOffsetX() * value;
        double d2 = (double)this.attachmentPos.getY() + 0.5D - (double)this.facing.getOffsetY() * value;
        double d3 = (double)this.attachmentPos.getZ() + 0.5D - (double)this.facing.getOffsetZ() * value;
        this.setPos(d1, d2, d3);
        double d4 = this.getWidthPixels();
        double d5 = this.getHeightPixels();
        double d6 = this.getWidthPixels();
        Direction.Axis directionAxis = this.facing.getAxis();
        switch (directionAxis) {
            case X -> d4 = 1.0D;
            case Y -> d5 = 1.0D;
            case Z -> d6 = 1.0D;
        }

        d4 /= 32.0D;
        d5 /= 32.0D;
        d6 /= 32.0D;
        this.setBoundingBox(new Box(d1 - d4, d2 - d5, d3 - d6, d1 + d4, d2 + d5, d3 + d6));
    }


    // Interaction:

    @Override
    protected void setFacing(@NotNull Direction facingDirection) {
        Validate.notNull(facingDirection);
        this.facing = facingDirection;
        if (facingDirection.getAxis().isHorizontal()) {
            this.setPitch(0.0F);
            this.setYaw((float)(this.facing.getHorizontal() * 90));
        } else {
            this.setPitch((float)(-90 * facingDirection.getDirection().offset()));
            this.setYaw(0.0F);
        }

        this.prevPitch = this.getPitch();
        this.prevYaw = this.getYaw();
        this.updateAttachmentPosition();
    }

    public ItemStack getItem() {
        return this.getDataTracker().get(DATA_ITEM);
    }

    public void setItem(ItemStack photographStack) {
        Preconditions.checkState(photographStack.getItem() instanceof PhotographItem,  photographStack + " is not a PhotographItem");
        this.getDataTracker().set(DATA_ITEM, photographStack);
    }

    protected void onItemChanged(ItemStack itemStack) {
        if (!itemStack.isEmpty()) {
            itemStack.setHolder(this);
            if (itemStack.getItem() instanceof PhotographItem photographItem) {
                idOrTexture = photographItem.getIdOrTexture(itemStack);
            }
        }

        this.updateAttachmentPosition();
    }

    public boolean isGlowing() {
        return this.getDataTracker().get(DATA_GLOWING);
    }

    public void setGlowing(boolean glowing) {
        this.getDataTracker().set(DATA_GLOWING, glowing);
    }

    public int getRotation() {
        return this.getDataTracker().get(DATA_ROTATION);
    }

    public void setRotation(int rotation) {
        this.getDataTracker().set(DATA_ROTATION, rotation % 4);
    }

    @Override
    public @NotNull ActionResult interact(@NotNull PlayerEntity player, @NotNull Hand hand) {
        ItemStack itemInHand = player.getStackInHand(hand);
        if (!isInvisible() && canShear(itemInHand)) {
            if (!getWorld().isClient) {
                setInvisible(true);
                itemInHand.damage(1, player, (pl) -> pl.sendToolBreakStatus(hand));
                playSound(SoundEvents.ENTITY_SHEEP_SHEAR, 1f, getWorld().getRandom().nextFloat() * 0.2f + 0.9f);
            }

            return ActionResult.SUCCESS;
        }

        if (itemInHand.isOf(Items.GLOW_INK_SAC)) {
            setGlowing(true);
            itemInHand.decrement(1);
            if (!getWorld().isClient)
                playSoundIfNotSilent(SoundEvents.ITEM_GLOW_INK_SAC_USE);
            return ActionResult.SUCCESS;
        }

        if (!getWorld().isClient) {
            this.playSound(getRotateSound(), 1.0F, getWorld().getRandom().nextFloat() * 0.2f + 0.9f);
            this.setRotation(getRotation() + 1);
        }

        return ActionResult.SUCCESS;
    }

    public boolean canShear(ItemStack stack) {
        return PlatformHelper.canShear(stack);
    }

    @Override
    public boolean damage(@NotNull DamageSource damageSource, float amount) {
        if (this.isInvulnerableTo(damageSource))
            return false;

        if (!this.isRemoved() && !this.getWorld().isClient) {
            if (!getItem().isEmpty() && !damageSource.isOf(DamageTypes.EXPLOSION))
                this.onBreak(damageSource.getAttacker());

            this.kill();
            this.scheduleVelocityUpdate();
        }

        return true;
    }

    @Override
    public void onBreak(@Nullable Entity breaker) {
        this.playSound(this.getBreakSound(), 1.0F, getWorld().getRandom().nextFloat() * 0.3f + 0.6f);

        if ((breaker instanceof PlayerEntity player && player.isCreative()))
            return;

        ItemStack itemStack = getItem();
        dropStack(itemStack);
    }

    @Override
    public void tick() {
        super.tick();
        if (getWorld().isClient && isGlowing() && getWorld().getRandom().nextFloat() < 0.01f) {
            Box bb = getBoundingBox();
            Vec3i normal = getHorizontalFacing().getVector();
            getWorld().addParticle(ParticleTypes.END_ROD,
                    getPos().x + (getWorld().getRandom().nextFloat() * (bb.getXLength() * 0.75f) - bb.getXLength() * 0.75f / 2),
                    getPos().y + (getWorld().getRandom().nextFloat() * (bb.getYLength() * 0.75f) - bb.getYLength() * 0.75f / 2),
                    getPos().z + (getWorld().getRandom().nextFloat() * (bb.getZLength() * 0.75f) - bb.getZLength() * 0.75f / 2),
                    getWorld().getRandom().nextFloat() * 0.02f * normal.getX(),
                    getWorld().getRandom().nextFloat() * 0.02f * normal.getY(),
                    getWorld().getRandom().nextFloat() * 0.02f * normal.getZ());
        }
    }

    @Override
    public void onPlace() {
        this.playSound(this.getPlaceSound(), 1.0F, getWorld().getRandom().nextFloat() * 0.3f + 0.9f);
    }

    public SoundEvent getPlaceSound() {
        return Exposure.SoundEvents.PHOTOGRAPH_PLACE.get();
    }

    public SoundEvent getBreakSound() {
        return Exposure.SoundEvents.PHOTOGRAPH_BREAK.get();
    }

    public SoundEvent getRotateSound() {
        return Exposure.SoundEvents.PHOTOGRAPH_RUSTLE.get();
    }
}
