/*
 * Copyright (c) 2017, GoMint, BlackyPaw and geNAZt
 *
 * This code is licensed under the BSD license found in the
 * LICENSE file in the root directory of this source tree.
 */

package io.gomint.server.entity.projectile;

import io.gomint.entity.EntityLiving;
import io.gomint.event.entity.projectile.ProjectileHitBlocksEvent;
import io.gomint.event.player.PlayerPickupItemEvent;
import io.gomint.inventory.item.ItemArrow;
import io.gomint.math.Location;
import io.gomint.math.MathUtils;
import io.gomint.math.Vector;
import io.gomint.server.entity.Entity;
import io.gomint.server.entity.EntityPlayer;
import io.gomint.server.entity.EntityType;
import io.gomint.server.registry.RegisterInfo;
import io.gomint.server.util.Values;
import io.gomint.server.world.WorldAdapter;
import io.gomint.util.random.FastRandom;
import io.gomint.world.block.Block;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.TimeUnit;

/**
 * @author geNAZt
 * @version 1.0
 */
@RegisterInfo( sId = "minecraft:arrow" )
public class EntityArrow extends EntityProjectile implements io.gomint.entity.projectile.EntityArrow {

    private boolean firedHitEvent = false;
    private boolean isReset = false;

    private boolean canBePickedup;
    private boolean critical;
    private float lastUpdateDT;

    private int powerModifier;
    private int punchModifier;
    private int flameModifier;

    /**
     * Create entity for API
     */
    public EntityArrow() {
        super( null, EntityType.ARROW, null );
        this.setSize( 0.5f, 0.5f );
    }

    /**
     * Construct a new Entity
     *
     * @param player        which spawned this hook
     * @param world         The world in which this entity is in
     * @param force         with which the player shoot the bow
     * @param powerModifier modifier for damage of this arrow
     * @param punchModifier modifier for knockback of this arrow
     * @param flameModifier modifier for burning arrows
     */
    public EntityArrow( EntityPlayer player, WorldAdapter world, float force, int powerModifier, int punchModifier, int flameModifier ) {
        super( player, EntityType.ARROW, world );
        this.setSize( 0.5f, 0.5f );

        this.powerModifier = powerModifier;
        this.punchModifier = punchModifier;
        this.flameModifier = flameModifier;

        this.critical = force == 1.0f;
        float applyForce = force * 2;

        Location position = this.setPositionFromShooter();

        // Calculate motion
        Vector motion = new Vector(
            (float) ( -Math.sin( position.getYaw() / 180.0F * Math.PI ) * Math.cos( position.getPitch() / 180.0F * (float) Math.PI ) * 0.4f ),
            (float) ( -Math.sin( position.getPitch() / 180.0F * (float) Math.PI ) * 0.4f ),
            (float) ( Math.cos( position.getYaw() / 180.0F * Math.PI ) * Math.cos( position.getPitch() / 180.0F * (float) Math.PI ) * 0.4f )
        );

        float distanceTravel = (float) Math.sqrt( MathUtils.square( motion.getX() ) + MathUtils.square( motion.getY() ) + MathUtils.square( motion.getZ() ) );
        motion.setX( (float) ( ( ( motion.getX() / distanceTravel ) + ( FastRandom.current().nextDouble() * ( FastRandom.current().nextBoolean() ? -1 : 1 ) * 0.0075f ) ) * ( applyForce * 1.5f ) ) );
        motion.setY( (float) ( ( ( motion.getY() / distanceTravel ) + ( FastRandom.current().nextDouble() * ( FastRandom.current().nextBoolean() ? -1 : 1 ) * 0.0075f ) ) * ( applyForce * 1.5f ) ) );
        motion.setZ( (float) ( ( ( motion.getZ() / distanceTravel ) + ( FastRandom.current().nextDouble() * ( FastRandom.current().nextBoolean() ? -1 : 1 ) * 0.0075f ) ) * ( applyForce * 1.5f ) ) );
        this.setVelocity( motion );

        // Calculate correct yaw / pitch
        double motionDistance = MathUtils.square( motion.getX() ) + MathUtils.square( motion.getZ() );
        float motionForce = (float) Math.sqrt( motionDistance );

        float yaw = (float) ( Math.atan2( motion.getX(), motion.getZ() ) * 180.0D / Math.PI );
        float pitch = (float) ( Math.atan2( motion.getY(), (double) motionForce ) * 180.0D / Math.PI );

        this.setYaw( yaw );
        this.setHeadYaw( yaw );
        this.setPitch( pitch );

        // Set owning entity
        this.metadataContainer.putLong( 5, player.getEntityId() );
    }

    @Override
    public boolean isCritical() {
        return this.critical;
    }

    @Override
    protected void applyCustomKnockback( Entity hitEntity ) {
        if ( this.punchModifier > 0 ) {
            float sqrtMotion = (float) Math.sqrt( this.getMotionX() * this.getMotionX() + this.getMotionZ() * this.getMotionZ() );
            if ( sqrtMotion > 0.0F ) {
                Vector toAdd = new Vector(
                    this.getMotionX() * this.punchModifier * 0.6f / sqrtMotion,
                    0.1f,
                    this.getMotionZ() * this.punchModifier * 0.6f / sqrtMotion
                );

                hitEntity.setVelocity( hitEntity.getVelocity().add( toAdd ) );
            }
        }
    }

    @Override
    protected void applyCustomDamageEffects( Entity hitEntity ) {
        if ( this.flameModifier > 0 && hitEntity instanceof EntityLiving ) {
            ( (EntityLiving) hitEntity ).setBurning( 5, TimeUnit.SECONDS );
        }
    }

    @Override
    public float getDamage() {
        if ( this.powerModifier > 0 ) {
            return 2 + ( this.powerModifier * 0.5f + 0.5f );
        }

        return 2;
    }

    @Override
    public void update( long currentTimeMS, float dT ) {
        super.update( currentTimeMS, dT );

        // Arrows which hit are gone
        if ( this.hitEntity != null ) {
            this.despawn();
        }

        this.lastUpdateDT += dT;
        if ( Values.CLIENT_TICK_RATE - this.lastUpdateDT < MathUtils.EPSILON ) {
            if ( this.isCollided && !this.canBePickedup && !this.firedHitEvent ) { // this.canBePickedup indicates if a event got cancelled
                // Remap
                Set<Block> blocks = new HashSet<>( this.collidedWith );
                ProjectileHitBlocksEvent hitBlockEvent = new ProjectileHitBlocksEvent( blocks, this );
                this.world.getServer().getPluginManager().callEvent( hitBlockEvent );
                if ( !hitBlockEvent.isCancelled() ) {
                    this.canBePickedup = true;
                }
            }

            if ( this.canBePickedup && !this.isReset && this.getVelocity().length() < 0.0025 ) {
                this.setVelocity( Vector.ZERO );
                this.isReset = true;
            }

            // Despawn after 1200 ticks ( 1 minute )
            if ( this.age >= 1200 ) {
                this.despawn();
            }

            this.lastUpdateDT = 0;
        }
    }

    @Override
    public void onCollideWithPlayer( EntityPlayer player ) {
        if ( this.canBePickedup && !this.isDead() ) {
            ItemArrow arrow = ItemArrow.create( 1 );

            // Check if we have place in out inventory to store this item
            if ( !player.getInventory().hasPlaceFor( arrow ) ) {
                return;
            }

            PlayerPickupItemEvent pickupItemEvent = new PlayerPickupItemEvent( player, this, arrow );
            player.getWorld().getServer().getPluginManager().callEvent( pickupItemEvent );
            if ( pickupItemEvent.isCancelled() ) {
                return;
            }

            player.getInventory().addItem( arrow );
            this.despawn();
        }
    }

}
