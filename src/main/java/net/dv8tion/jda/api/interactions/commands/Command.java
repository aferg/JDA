/*
 * Copyright 2015 Austin Keener, Michael Ritter, Florian Spieß, and the JDA contributors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package net.dv8tion.jda.api.interactions.commands;

import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.ISnowflake;
import net.dv8tion.jda.api.requests.RestAction;
import net.dv8tion.jda.api.requests.restaction.CommandEditAction;
import net.dv8tion.jda.api.utils.data.DataArray;
import net.dv8tion.jda.api.utils.data.DataObject;
import net.dv8tion.jda.api.utils.data.DataType;
import net.dv8tion.jda.internal.JDAImpl;
import net.dv8tion.jda.internal.requests.RestActionImpl;
import net.dv8tion.jda.internal.requests.Route;
import net.dv8tion.jda.internal.requests.restaction.CommandEditActionImpl;

import javax.annotation.CheckReturnValue;
import javax.annotation.Nonnull;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Represents a Discord slash-command.
 * <br>This can be used to edit or delete the command.
 *
 * @see Guild#retrieveCommandById(String)
 * @see Guild#retrieveCommands()
 */
public class Command implements ISnowflake
{
    private final JDAImpl api;
    private final Guild guild;
    private final String name, description;
    private final List<Option> options;
    private final long id, guildId;

    public Command(JDAImpl api, Guild guild, DataObject json)
    {
        this.api = api;
        this.guild = guild;
        this.name = json.getString("name");
        this.description = json.getString("description");
        this.id = json.getUnsignedLong("id");
        this.guildId = guild != null ? guild.getIdLong() : 0L;
        this.options = parseOptions(json);
    }

    protected static List<Option> parseOptions(DataObject json)
    {
        return json.optArray("options").map(arr ->
            arr.stream(DataArray::getObject)
               .map(Option::new)
               .collect(Collectors.toList())
        ).orElse(Collections.emptyList());
    }

    /**
     * Delete this command.
     * <br>If this is a global command it may take up to 1 hour to vanish from all clients.
     *
     * @return {@link RestAction}
     */
    @Nonnull
    @CheckReturnValue
    public RestAction<Void> delete()
    {
        Route.CompiledRoute route;
        if (guildId != 0L)
            route = Route.Interactions.DELETE_GUILD_COMMAND.compile(Long.toUnsignedString(guildId), getId());
        else
            route = Route.Interactions.DELETE_COMMAND.compile(getId());
        return new RestActionImpl<>(api, route);
    }

    /**
     * Edit this command.
     * <br>This can be used to change the command attributes such as name or description.
     *
     * @return {@link CommandEditAction}
     */
    @Nonnull
    @CheckReturnValue
    public CommandEditAction editCommand()
    {
        return guild == null ? new CommandEditActionImpl(api, getId()) : new CommandEditActionImpl(guild, getId());
    }

    /**
     * Returns the {@link net.dv8tion.jda.api.JDA JDA} instance of this Command
     *
     * @return the corresponding JDA instance
     */
    @Nonnull
    public JDA getJDA()
    {
        return api;
    }

    /**
     * The name of this command.
     *
     * @return The name
     */
    @Nonnull
    public String getName()
    {
        return name;
    }

    /**
     * The description of this command.
     *
     * @return The description
     */
    @Nonnull
    public String getDescription()
    {
        return description;
    }

    // TODO: This should be split to getSubcommands etc

    /**
     * The {@link Option Options} of this command.
     * <br>If this command uses subcommands, then the provided list will be the list of subcommands instead.
     * Each subcommand has its own list of options via {@link Option#getOptions()}.
     * If this command uses subcommand groups, this will return the groups instead and {@link Option#getOptions()} the respective subcommands within that group.
     *
     * @return Immutable list of command options
     */
    @Nonnull
    public List<Option> getOptions()
    {
        return options;
    }

    @Override
    public long getIdLong()
    {
        return id;
    }

    @Override
    public String toString()
    {
        return "C:" + getName() + "(" + getId() + ")";
    }

    @Override
    public boolean equals(Object obj)
    {
        if (obj == this)
            return true;
        if (!(obj instanceof Command))
            return false;
        return id == ((Command) obj).id;
    }

    @Override
    public int hashCode()
    {
        return Long.hashCode(id);
    }

    /**
     * Predefined choice used for options.
     */
    public static class Choice
    {
        private final String name;
        private final long intValue;
        private final String stringValue;

        public Choice(String name, long value)
        {
            this.name = name;
            this.intValue = value;
            this.stringValue = Long.toString(value);
        }

        public Choice(String name, String value)
        {
            this.name = name;
            this.intValue = 0;
            this.stringValue = value;
        }

        public Choice(DataObject json)
        {
            this.name = json.getString("name");
            if (json.isType("value", DataType.INT))
            {
                this.intValue = json.getLong("value");
                this.stringValue = Long.toString(intValue); // does this make sense?
            }
            else
            {
                this.intValue = 0;
                this.stringValue = json.getString("value");
            }
        }

        /**
         * The readable name of this choice.
         * <br>This is shown to the user in the official client.
         *
         * @return The choice name
         */
        @Nonnull
        public String getName()
        {
            return name;
        }

        /**
         * The value of this choice.
         *
         * @return The long value
         */
        public long getAsLong()
        {
            return intValue;
        }

        /**
         * The value of this choice.
         *
         * @return The String value
         */
        @Nonnull
        public String getAsString()
        {
            return stringValue;
        }
    }

    /**
     * An Option for a command.
     * <br>Options can also represent subcommands and subcommand groups.
     *
     * <p>If this is a subcommand, the {@link #getOptions()} will return the options for that subcommand.
     * For subcommand groups it will return the subcommands of that group.
     */
    public static class Option
    {
        private final String name, description;
        private final int type;
        private final List<Option> options;
        private final List<Choice> choices;

        public Option(DataObject json)
        {
            this.name = json.getString("name");
            this.description = json.getString("description");
            this.type = json.getInt("type");

            this.options = parseOptions(json);
            this.choices = json.optArray("choices")
                .map(it -> it.stream(DataArray::getObject).map(Choice::new).collect(Collectors.toList()))
                .orElse(Collections.emptyList());
        }

        /**
         * The name of this option, subcommand, or subcommand group.
         *
         * @return The name
         */
        @Nonnull
        public String getName()
        {
            return name;
        }

        /**
         * The description of this option, subcommand, or subcommand group.
         *
         * @return The description
         */
        @Nonnull
        public String getDescription()
        {
            return description;
        }

        /**
         * The raw option type.
         *
         * @return The type
         */
        public int getTypeRaw()
        {
            return type;
        }

        /**
         * The {@link OptionType}.
         *
         * @return The type
         */
        @Nonnull
        public OptionType getType()
        {
            return OptionType.fromKey(type);
        }

        /**
         * The list of predefined {@link Choice Choices} for this option.
         *
         * @return Immutable list of choices
         */
        @Nonnull
        public List<Choice> getChoices()
        {
            return choices;
        }

        /**
         * The options for this subcommand, or the subcommands whtin this group.
         *
         * @return Immutable list of Options
         */
        @Nonnull
        public List<Option> getOptions()
        {
            return options;
        }
    }
}