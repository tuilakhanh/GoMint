/*
 * Copyright (c) 2017, GoMint, BlackyPaw and geNAZt
 *
 * This code is licensed under the BSD license found in the
 * LICENSE file in the root directory of this source tree.
 */

package io.gomint.server.player;

import com.koloboke.collect.ObjCursor;
import io.gomint.entity.Entity;
import io.gomint.server.entity.EntityPlayer;
import io.gomint.server.network.packet.PacketDespawnEntity;
import io.gomint.server.util.collection.EntityHashSet;
import io.gomint.server.world.ChunkAdapter;
import io.gomint.server.world.CoordinateUtils;
import io.gomint.world.Chunk;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;

/**
 * @author geNAZt
 * @version 1.0
 */
@RequiredArgsConstructor
public class EntityVisibilityManager {

    private static final Logger LOGGER = LoggerFactory.getLogger( EntityVisibilityManager.class );
    private final EntityPlayer player;
    private EntityHashSet visible = EntityHashSet.withExpectedSize( 10 );

    public void updateAddedChunk( ChunkAdapter chunk ) {
        LOGGER.debug( "Checking chunk {}, {}", chunk.getX(), chunk.getZ() );

        Collection<Entity> collection = chunk.getEntities();
        if ( collection != null ) {
            for ( Entity entity : collection ) {
                if ( ( (io.gomint.server.entity.Entity) entity ).canSee( this.player ) ) {
                    addEntity( entity );
                }
            }
        }
    }

    public void updateEntity( Entity entity, Chunk chunk ) {
        if ( !( (io.gomint.server.entity.Entity) entity ).canSee( this.player ) ) {
            return;
        }

        int currentX = CoordinateUtils.fromBlockToChunk( (int) this.player.getPositionX() );
        int currentZ = CoordinateUtils.fromBlockToChunk( (int) this.player.getPositionZ() );

        if ( Math.abs( chunk.getX() - currentX ) > this.player.getViewDistance() ||
            Math.abs( chunk.getZ() - currentZ ) > this.player.getViewDistance() ) {
            removeEntity( entity );
        } else {
            addEntity( entity );
        }
    }

    public void updateRemoveChunk( ChunkAdapter chunk ) {
        // Check for removing entities
        Collection<Entity> collection = chunk.getEntities();
        if ( collection != null ) {
            for ( Entity entity : collection ) {
                removeEntity( entity );
            }
        }
    }

    public void removeEntity( Entity entity ) {
        if ( this.visible.remove( entity ) && !this.player.equals( entity ) ) {
            io.gomint.server.entity.Entity implEntity = (io.gomint.server.entity.Entity) entity;
            this.despawnEntity( implEntity );
        }
    }

    private void despawnEntity( io.gomint.server.entity.Entity entity ) {
        entity.detach( this.player );

        PacketDespawnEntity despawnEntity = new PacketDespawnEntity();
        despawnEntity.setEntityId( entity.getEntityId() );
        this.player.getConnection().addToSendQueue( despawnEntity );
    }

    public void addEntity( Entity entity ) {
        if ( !this.visible.contains( entity ) && !this.player.equals( entity ) ) {
            LOGGER.debug( "Showing entity {} for player", entity );
            io.gomint.server.entity.Entity implEntity = (io.gomint.server.entity.Entity) entity;

            implEntity.preSpawn( this.player.getConnection() );
            this.player.getConnection().addToSendQueue( implEntity.createSpawnPacket() );
            implEntity.postSpawn( this.player.getConnection() );

            implEntity.attach( this.player );
            this.visible.add( entity );
        }
    }

    public void clear() {
        ObjCursor<Entity> cursor = this.visible.cursor();
        while ( cursor.moveNext() ) {
            Entity curr = cursor.elem();
            if ( curr instanceof io.gomint.server.entity.Entity ) {
                this.despawnEntity( ( io.gomint.server.entity.Entity ) curr);
            }
        }

        this.visible.clear();
    }

    public boolean isVisible( Entity entity ) {
        return this.visible.contains( entity );
    }

}
