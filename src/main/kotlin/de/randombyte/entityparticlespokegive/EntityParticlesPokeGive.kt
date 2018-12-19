package de.randombyte.entityparticlespokegive

import com.google.inject.Inject
import com.pixelmonmod.pixelmon.Pixelmon
import com.pixelmonmod.pixelmon.api.events.PixelmonReceivedEvent
import de.randombyte.entityparticles.data.ParticleData
import de.randombyte.entityparticlespokegive.EntityParticlesPokeGive.Companion.ID
import de.randombyte.kosp.extensions.executeAsConsole
import de.randombyte.kosp.extensions.toText
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import org.bstats.sponge.Metrics
import org.slf4j.Logger
import org.spongepowered.api.Sponge
import org.spongepowered.api.command.CommandResult
import org.spongepowered.api.command.args.GenericArguments.*
import org.spongepowered.api.command.spec.CommandSpec
import org.spongepowered.api.config.ConfigDir
import org.spongepowered.api.data.DataHolder
import org.spongepowered.api.entity.living.player.Player
import org.spongepowered.api.event.Listener
import org.spongepowered.api.event.game.state.GameInitializationEvent
import org.spongepowered.api.event.network.ClientConnectionEvent
import org.spongepowered.api.plugin.Dependency
import org.spongepowered.api.plugin.Plugin
import org.spongepowered.api.plugin.PluginContainer
import java.nio.file.Path
import java.util.*

@Plugin(id = ID,
        name = EntityParticlesPokeGive.NAME,
        version = EntityParticlesPokeGive.VERSION,
        dependencies = [Dependency(id = "pixelmon")],
        authors = [(EntityParticlesPokeGive.AUTHOR)])
class EntityParticlesPokeGive @Inject constructor(
        private val logger: Logger,
        @ConfigDir(sharedRoot = false) configPath: Path,
        private val pluginContainer: PluginContainer,
        private val bStats: Metrics
) {
    internal companion object {
        const val ID = "entity-particles-poke-give"
        const val NAME = "EntityParticlesPokeGive"
        const val VERSION = "1.0.0"
        const val AUTHOR = "RandomByte"

        const val ROOT_PERMISSION = ID

        const val PLAYER_ARG = "player"
        const val POKEMON_ARG = "pokemon"
        const val PARTICLE_ARG = "particle"
        const val ARGS_ARG = "args"
    }

    // <player, particle>
    private val markedPlayers = mutableMapOf<UUID, String>()

    @Listener
    fun onInit(event: GameInitializationEvent) {
        registerCommands()

        Pixelmon.EVENT_BUS.register(this)

        logger.info("$NAME loaded: $VERSION")
    }

    @Listener
    fun onPlayerDisconnect(event: ClientConnectionEvent.Disconnect) {
        markedPlayers -= event.targetEntity.uniqueId
    }

    @SubscribeEvent
    fun onPixelmonReceivedEvent(event: PixelmonReceivedEvent) {
        val particleString = markedPlayers.remove(event.player.uniqueID)?: return
        (event.pokemon as DataHolder).tryOffer(ParticleData(particleString, true))
    }

    private fun registerCommands() {
        with (Sponge.getCommandManager()) { getOwnedBy(this@EntityParticlesPokeGive).forEach { removeMapping(it) } }

        Sponge.getCommandManager().register(this, CommandSpec.builder()
                    .permission(ROOT_PERMISSION)
                    .arguments(
                            player(PLAYER_ARG.toText()),
                            string(POKEMON_ARG.toText()),
                            string(PARTICLE_ARG.toText()),
                            optional(remainingRawJoinedStrings(ARGS_ARG.toText())))
                    .executor { src, args ->
                        val target = args.getOne<Player>(PLAYER_ARG).get()
                        val pokemonString = args.getOne<String>(POKEMON_ARG).get()
                        val particleString = args.getOne<String>(PARTICLE_ARG).get()
                        val additionalArgs = args.getOne<String>(ARGS_ARG).orElse("")

                        markedPlayers += (target.uniqueId to particleString)
                        "pokegive ${target.name} $pokemonString $additionalArgs".trim().executeAsConsole()
                        markedPlayers -= target.uniqueId

                        return@executor CommandResult.success()
                    }
                .build(), "entityparticlespokegive", "eppg")
    }
}