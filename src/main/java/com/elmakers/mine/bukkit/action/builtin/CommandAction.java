package com.elmakers.mine.bukkit.action.builtin;

import com.elmakers.mine.bukkit.action.BaseSpellAction;
import com.elmakers.mine.bukkit.api.action.CastContext;
import com.elmakers.mine.bukkit.api.magic.Mage;
import com.elmakers.mine.bukkit.api.magic.MageController;
import com.elmakers.mine.bukkit.api.spell.Spell;
import com.elmakers.mine.bukkit.api.spell.SpellResult;
import com.elmakers.mine.bukkit.spell.BaseSpell;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Entity;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.logging.Level;

/**
 * Can run any Bukkit command as a Spell.
 *
 * This includes running as Console, or opping/deopping the player if needed.
 *
 * This spell can also act as a targeting spell, running commands using the
 * target location or entity.
 *
 * The following parameters will all be substituted in the "command" string
 * if found:
 *
 * @_ - A spell, useful for command-line casting
 * @spell - name of spell being cast
 * @p - mage name
 * @uuid - mage uuid
 * @world, @x, @y, @z - mage location
 *
 * If targeting is used ("target: none" to disable), the following will also be escaped:
 *
 * @t - target mage name
 * @tuuid - target entity uuid
 * @tworld, @tx, @ty, @tz - target location
 */
public class CommandAction extends BaseSpellAction {
    public final static String[] PARAMETERS = {
            "command", "console", "op"
    };

    private List<String> commands = new ArrayList<String>();
    private boolean asConsole;
    private boolean opPlayer;

    @Override
    public void prepare(CastContext context, ConfigurationSection parameters) {
        super.prepare(context, parameters);
        asConsole = parameters.getBoolean("console", false);
        opPlayer = parameters.getBoolean("op", false);
    }

    @Override
    public void initialize(Spell spell, ConfigurationSection parameters) {
        super.initialize(spell, parameters);
        commands.clear();

        if (parameters.contains("command"))
        {
            String command = parameters.getString("command");
            if (command != null && command.length() > 0)
            {
                commands.add(command);
            }
        }
        else
        {
            commands.addAll(parameters.getStringList("commands"));
        }
    }

    @Override
    public SpellResult perform(CastContext context) {
        Mage mage = context.getMage();
        MageController controller = context.getController();
        CommandSender sender = asConsole ? Bukkit.getConsoleSender() : mage.getCommandSender();
        if (sender == null) {
            return SpellResult.FAIL;
        }
        boolean isOp = sender.isOp();
        if (opPlayer && !isOp) {
            sender.setOp(true);
        }
        for (String command : commands) {
            try {
                String converted = context.parameterize(command);
                controller.getPlugin().getServer().dispatchCommand(sender, converted);
            } catch (Exception ex) {
                controller.getLogger().log(Level.WARNING, "Error running command: " + command, ex);
            }
        }
        if (opPlayer && !isOp) {
            sender.setOp(false);
        }
        return SpellResult.CAST;
    }

    @Override
    public void getParameterNames(Spell spell, Collection<String> parameters)
    {
        super.getParameterNames(spell, parameters);
        parameters.addAll(Arrays.asList(PARAMETERS));
    }

    @Override
    public void getParameterOptions(Spell spell, String parameterKey, Collection<String> examples)
    {
        if (parameterKey.equals("command")) {
            examples.add("spawn");
            examples.add("clear");
        } else if (parameterKey.equals("op") || parameterKey.equals("console")) {
            examples.addAll(Arrays.asList(BaseSpell.EXAMPLE_BOOLEANS));
        } else {
            super.getParameterOptions(spell, parameterKey, examples);
        }
    }
}
