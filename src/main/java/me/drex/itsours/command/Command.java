package me.drex.itsours.command;

import com.mojang.authlib.GameProfile;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import me.drex.itsours.ItsOursMod;
import me.drex.itsours.claim.AbstractClaim;
import me.drex.itsours.claim.Subzone;
import me.drex.itsours.claim.permission.util.Group;
import me.drex.itsours.claim.permission.util.Permission;
import me.drex.itsours.claim.permission.util.node.AbstractNode;
import net.minecraft.command.CommandSource;
import net.minecraft.command.argument.GameProfileArgumentType;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.LiteralText;
import net.minecraft.util.math.BlockPos;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicReference;

import static com.mojang.brigadier.arguments.StringArgumentType.word;
import static net.minecraft.server.command.CommandManager.argument;

public abstract class Command {

    public final SuggestionProvider<ServerCommandSource> ROLE_PROVIDER = (source, builder) -> {
        List<String> names = new ArrayList<>();
        ItsOursMod.INSTANCE.getRoleManager().forEach((roleID, role) -> names.add(roleID));
        return CommandSource.suggestMatching(names, builder);
    };
    public final SuggestionProvider<ServerCommandSource> CLAIM_PROVIDER = (source, builder) -> {
        UUID uuid = source.getSource().getPlayer().getUuid();
        ServerPlayerEntity player = source.getSource().getPlayer();
        List<String> names = new ArrayList<>();
        AbstractClaim current = ItsOursMod.INSTANCE.getClaimList().get(player.getServerWorld(), player.getBlockPos());
        if (current != null) names.add(current.getName());
        if (uuid != null) {
            for (AbstractClaim claim : ItsOursMod.INSTANCE.getClaimList().get(uuid)) {
                names.add(claim.getName());
                addSubzones(claim, builder.getRemaining(), names);
            }
        }
        return CommandSource.suggestMatching(names, builder);
    };
    //TODO: Validate permissions
    public final SuggestionProvider<ServerCommandSource> PERMISSION_PROVIDER = (source, builder) -> {
        List<String> permissions = new ArrayList<>();
        for (Permission permission : Permission.permissions) {
            permissions.add(permission.id);
            if (builder.getRemaining().startsWith(permission.id)) {
                addNodes(permission.id, permission.groups, 0, builder.getRemaining(), permissions);
            }
        }
        return CommandSource.suggestMatching(permissions, builder);
    };

    //TODO: Look at this again, maybe there is a better approach to this
    public static GameProfile getGameProfile(CommandContext<ServerCommandSource> ctx, String name) throws CommandSyntaxException {
        AtomicReference<String> exception = new AtomicReference<>();
        CompletableFuture<GameProfile> completableFuture = CompletableFuture.supplyAsync(() -> {
            try {
                Collection<GameProfile> profiles = GameProfileArgumentType.getProfileArgument(ctx, name);
                if (profiles.size() > 1) {
                    exception.set("Only one selection is allowed!");
                } else if (profiles.isEmpty()) {
                    exception.set("At least one selection is required!");
                }
                return profiles.iterator().next();
            } catch (CommandSyntaxException e) {
                exception.set(e.getRawMessage().getString());
            }
            return null;
        });
        GameProfile profile = null;
        try {
            profile = completableFuture.get();
        } catch (InterruptedException | ExecutionException e) {
            ItsOursMod.LOGGER.error("Unable to retrieve GameProfile: ", e);
        }
        if (exception.get() != null) throw new SimpleCommandExceptionType(new LiteralText(exception.get())).create();
        return profile;
    }

    private void addSubzones(AbstractClaim claim, String input, List<String> names) {
        if (input.startsWith(claim.getFullName())) {
            for (Subzone subzone : claim.getSubzones()) {
                names.add(subzone.getFullName());
                addSubzones(subzone, input, names);
            }
        }
    }

    private void addNodes(String parent, Group[] groups, int i, String input, List<String> permissions) {
        for (AbstractNode node : groups[i].list) {
            String s = parent + "." + node.getID();
            permissions.add(s);
            if (input.startsWith(s) && i + 1 < groups.length) addNodes(s, groups, i + 1, input, permissions);
        }
    }

    AbstractClaim getAndValidateClaim(ServerWorld world, BlockPos pos) throws CommandSyntaxException {
        AbstractClaim claim = ItsOursMod.INSTANCE.getClaimList().get(world, pos);
        if (claim == null)
            throw new SimpleCommandExceptionType(new LiteralText("Couldn't find a claim at your position!")).create();
        return claim;
    }

    boolean hasPermission(ServerCommandSource src, String permission) {
        return ItsOursMod.INSTANCE.getPermissionHandler().hasPermission(src, permission, 2);
    }

    public AbstractClaim getClaim(CommandContext<ServerCommandSource> ctx) throws CommandSyntaxException {
        String name = StringArgumentType.getString(ctx, "claim");
        AbstractClaim claim = ItsOursMod.INSTANCE.getClaimList().get(name);
        if (claim != null) return claim;
        throw new SimpleCommandExceptionType(new LiteralText("Couldn't find a claim with that name")).create();
    }

    public String getPermission(CommandContext<ServerCommandSource> ctx) throws CommandSyntaxException {
        String permission = StringArgumentType.getString(ctx, "perm");
        if (!Permission.isValid(permission)) throw new SimpleCommandExceptionType(new LiteralText("Invalid permission")).create();
        return permission;
    }

    public RequiredArgumentBuilder<ServerCommandSource, String> claimArgument() {
        return argument("claim", word()).suggests(CLAIM_PROVIDER);
    }

    public RequiredArgumentBuilder<ServerCommandSource, String> roleArgument() {
        return argument("name", word()).suggests(ROLE_PROVIDER);
    }

    public RequiredArgumentBuilder<ServerCommandSource, String> permissionArgument() {
        return argument("perm", word()).suggests(PERMISSION_PROVIDER);
    }


}
