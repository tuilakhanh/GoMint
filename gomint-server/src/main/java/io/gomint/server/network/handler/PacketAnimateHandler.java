package io.gomint.server.network.handler;

import com.koloboke.collect.ObjCursor;
import io.gomint.entity.Entity;
import io.gomint.event.player.PlayerAnimationEvent;
import io.gomint.server.entity.EntityPlayer;
import io.gomint.server.network.PlayerConnection;
import io.gomint.server.network.packet.PacketAnimate;


/**
 * @author geNAZt
 * @version 1.0
 */
public class PacketAnimateHandler implements PacketHandler<PacketAnimate> {

    @Override
    public void handle( PacketAnimate packet, long currentTimeMillis, PlayerConnection connection ) {
        ObjCursor<Entity> entityObjCursor = connection.getEntity().getAttachedEntities().cursor();

        switch ( packet.getPlayerAnimation() ) {

            case SWING:

                PlayerAnimationEvent playerAnimationEvent = new PlayerAnimationEvent( connection.getEntity(), PlayerAnimationEvent.Animation.SWING );
                connection.getServer().getPluginManager().callEvent( playerAnimationEvent );

                if( !playerAnimationEvent.isCancelled() ) {
                    while ( entityObjCursor.moveNext() ) {
                        Entity entity = entityObjCursor.elem();
                        if ( entity instanceof EntityPlayer ) {
                            ( (EntityPlayer) entity ).getConnection().addToSendQueue( packet );
                        }
                    }
                }
            break;
        }
    }

}
