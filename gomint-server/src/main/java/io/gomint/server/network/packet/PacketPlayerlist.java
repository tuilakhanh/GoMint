package io.gomint.server.network.packet;

import io.gomint.jraknet.PacketBuffer;
import io.gomint.player.DeviceInfo;
import io.gomint.player.PlayerSkin;
import io.gomint.server.entity.EntityPlayer;
import io.gomint.server.entity.passive.EntityHuman;
import io.gomint.server.network.Protocol;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.List;
import java.util.UUID;

/**
 * @author geNAZt
 * @version 1.0
 */
@Data
public class PacketPlayerlist extends Packet {

    private byte mode;
    private List<Entry> entries;

    public PacketPlayerlist() {
        super( Protocol.PACKET_PLAYER_LIST );
    }

    @Override
    public void serialize( PacketBuffer buffer, int protocolID ) {
        buffer.writeByte( this.mode );
        buffer.writeUnsignedVarInt( this.entries.size() );

        if ( this.mode == 0 ) {
            for ( Entry entry : this.entries ) {
                buffer.writeUUID( entry.getUuid() );
                buffer.writeSignedVarLong( entry.getEntityId() );
                buffer.writeString( entry.getName() );

                // xbox user id
                buffer.writeString( entry.xboxId );

                // Player device UUID
                buffer.writeString( entry.deviceInfo.getDeviceId() );

                // Player device ID
                buffer.writeLInt( entry.deviceInfo.getOs().getId() );

                buffer.writeString( entry.getSkin().getName() );

                // Raw skin data
                buffer.writeUnsignedVarInt( entry.getSkin().getRawData().length );
                buffer.writeBytes( entry.getSkin().getRawData() );

                // Cape data
                if ( entry.skin.getCapeData() != null ) {
                    buffer.writeUnsignedVarInt( entry.skin.getCapeData().length );
                    buffer.writeBytes( entry.skin.getCapeData() );
                } else {
                    buffer.writeUnsignedVarInt( 0 );
                }

                // Geometry name
                buffer.writeString( entry.skin.getGeometryName() );

                // Geometry data
                buffer.writeUnsignedVarInt( entry.skin.getGeometryData().length() );
                buffer.writeBytes( entry.skin.getGeometryData().getBytes() );

                // Is teacher
                buffer.writeBoolean( false );

                // Is host
                buffer.writeBoolean( false );
            }
        } else {
            for ( Entry entry : this.entries ) {
                buffer.writeUUID( entry.getUuid() );
            }
        }

        if ( mode == 0 ) {
            for (int i = 0; i < entries.size(); i++) {
                // Is skin trusted
                buffer.writeBoolean( true );
            }
        }
    }

    @Override
    public void deserialize( PacketBuffer buffer, int protocolID ) {

    }

    @Data
    @AllArgsConstructor
    public static class Entry {
        private final UUID uuid;
        private long entityId = 0;
        private String name = "";
        private DeviceInfo deviceInfo;
        private String xboxId = "";
        private PlayerSkin skin;

        public Entry( EntityHuman human ) {
            this( human.getUUID(), human.getEntityId(), human.getPlayerListName(), human.getDeviceInfo() , human.getXboxID(), human.getSkin() );
        }
    }

}
