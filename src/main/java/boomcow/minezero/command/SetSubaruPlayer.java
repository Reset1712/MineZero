package boomcow.minezero.command;

import boomcow.minezero.checkpoint.CheckpointData;
import net.minecraft.command.CommandBase;
import net.minecraft.command.CommandException;
import net.minecraft.command.ICommandSender;
import net.minecraft.command.WrongUsageException;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.WorldServer;

public class SetSubaruPlayer extends CommandBase {

    @Override
    public String getName() {
        return "setSubaruPlayer";
    }

    @Override
    public String getUsage(ICommandSender sender) {
        return "/setSubaruPlayer <player>";
    }

    @Override
    public int getRequiredPermissionLevel() {
        return 2; // OP Level 2
    }

    @Override
    public void execute(MinecraftServer server, ICommandSender sender, String[] args) throws CommandException {
        if (args.length < 1) {
            throw new WrongUsageException(getUsage(sender));
        }

        // getPlayer throws CommandException if the player is not found, handling the error automatically
        EntityPlayerMP target = getPlayer(server, sender, args[0]);
        WorldServer level = target.getServerWorld();

        CheckpointData data = CheckpointData.get(level);
        data.setAnchorPlayerUUID(target.getUniqueID());

        notifyCommandListener(sender, this, "Anchor player set to " + target.getName());
    }
}