package io.gomint.server.command.vanilla;

import io.gomint.command.Command;
import io.gomint.command.CommandOutput;
import io.gomint.command.CommandSender;
import io.gomint.command.PlayerCommandSender;
import io.gomint.command.annotation.Description;
import io.gomint.command.annotation.Name;
import io.gomint.command.annotation.Overload;
import io.gomint.command.annotation.Parameter;
import io.gomint.command.annotation.Permission;
import io.gomint.command.validator.TargetValidator;
import io.gomint.entity.EntityPlayer;

import java.util.Map;

/**
 * @author lukeeey
 * @version 1.0
 */
@Name( "deop" )
@Description( "Revokes operator status from a player." )
@Permission( "gomint.command.deop" )
@Overload( {
    @Parameter( name = "player", validator = TargetValidator.class, optional = true )
} )
public class DeopCommand extends Command {

    @Override
    public CommandOutput execute( CommandSender sender, String alias, Map<String, Object> arguments ) {
        CommandOutput output = new CommandOutput();

        EntityPlayer target = (EntityPlayer) arguments.get( "player" );
        if ( target == null ) {
            if ( sender instanceof PlayerCommandSender ) {
                target = (EntityPlayer) sender;
            } else {
                return output.fail( "Please provide a player to deop" );
            }
        }

        if ( !target.isOp() ) {
            return output.fail( "Could not deop (already not op): " + target.getName() );
        }

        target.setOp( false );
        target.sendMessage( "You have been de-opped" );
        return output.success( "De-opped: " + target.getName() );
    }
}
