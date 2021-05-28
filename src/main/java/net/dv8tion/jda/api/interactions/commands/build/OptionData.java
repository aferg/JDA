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

package net.dv8tion.jda.api.interactions.commands.build;

import net.dv8tion.jda.api.interactions.commands.Command;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.utils.data.DataArray;
import net.dv8tion.jda.api.utils.data.DataObject;
import net.dv8tion.jda.api.utils.data.SerializableData;
import net.dv8tion.jda.internal.utils.Checks;

import javax.annotation.Nonnull;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Builder for a Slash-Command option.
 */
public class OptionData implements SerializableData
{
    private final OptionType type;
    private final String name, description;
    private boolean isRequired;
    private Map<String, Object> choices;

    /**
     * Create an option builder.
     * <br>This option is not {@link #isRequired() required} by default.
     *
     * @param type
     *        The {@link OptionType}
     * @param name
     *        The option name, 1-32 lowercase alphanumeric characters
     * @param description
     *        The option description, 1-100 characters
     *
     * @throws IllegalArgumentException
     *         If any of the following requirements are not met
     *         <ul>
     *             <li>The name must be lowercase alphanumeric (with dash), 1-32 characters long</li>
     *             <li>The description must be 1-100 characters long</li>
     *             <li>The type must not be null</li>
     *         </ul>
     */
    public OptionData(@Nonnull OptionType type, @Nonnull String name, @Nonnull String description)
    {
        Checks.notNull(type, "Type");
        Checks.notEmpty(name, "Name");
        Checks.notEmpty(description, "Description");
        Checks.notLonger(name, 32, "Name");
        Checks.notLonger(description, 100, "Description");
        Checks.matches(name, Checks.ALPHANUMERIC_WITH_DASH, "Name");
        Checks.isLowercase(name, "Name");
        this.type = type;
        this.name = name;
        this.description = description;
        if (type.canSupportChoices())
            choices = new LinkedHashMap<>();
    }

    /**
     * The {@link OptionType} for this option
     *
     * @return The {@link OptionType}
     */
    @Nonnull
    public OptionType getType()
    {
        return type;
    }

    /**
     * The name for this option
     *
     * @return The name
     */
    @Nonnull
    public String getName()
    {
        return name;
    }

    /**
     * The description for this option
     *
     * @return The description
     */
    @Nonnull
    public String getDescription()
    {
        return description;
    }

    /**
     * Whether this option is required.
     * <br>This can be configured with {@link #setRequired(boolean)}.
     *
     * <p>Required options must always be set by the command invocation.
     *
     * @return True, if this option is required
     */
    public boolean isRequired()
    {
        return isRequired;
    }

    /**
     * The choices for this option.
     * <br>This is empty by default and can only be configured for specific option types.
     *
     * @return Immutable list of {@link net.dv8tion.jda.api.interactions.commands.Command.Choice Choices}
     *
     * @see #addChoice(String, int)
     * @see #addChoice(String, String)
     */
    @Nonnull
    public List<Command.Choice> getChoices()
    {
        if (choices == null || choices.isEmpty())
            return Collections.emptyList();
        return choices.entrySet().stream()
                .map(entry ->
                {
                    if (entry.getValue() instanceof String)
                        return new Command.Choice(entry.getKey(), entry.getValue().toString());
                    return new Command.Choice(entry.getKey(), ((Number) entry.getValue()).longValue());
                })
                .collect(Collectors.toList());
    }

    /**
     * Configure whether the user must set this option.
     * <br>Required options must always be filled out when using the command.
     *
     * @param  required
     *         True, if this option is required
     *
     * @return The OptionData instance, for chaining
     */
    @Nonnull
    public OptionData setRequired(boolean required)
    {
        this.isRequired = required;
        return this;
    }

    /**
     * Add a predefined choice for this option.
     * <br>The user can only provide one of the choices and cannot specify any other value.
     *
     * @param  name
     *         The name used in the client
     * @param  value
     *         The value received in {@link net.dv8tion.jda.api.interactions.commands.OptionMapping OptionMapping}
     *
     * @throws IllegalArgumentException
     *         If the name is null, empty, or longer than 100 characters.
     *         Also thrown if this is not an option of type {@link OptionType#INTEGER} or more than 25 choices are provided.
     *
     * @return The OptionData instance, for chaining
     */
    @Nonnull
    public OptionData addChoice(@Nonnull String name, int value)
    {
        Checks.notEmpty(name, "Name");
        Checks.notLonger(name, 100, "Name");
        Checks.check(choices.size() < 25, "Cannot have more than 25 choices for an option!");
        if (type != OptionType.INTEGER)
            throw new IllegalArgumentException("Cannot add int choice for OptionType." + type);
        choices.put(name, value);
        return this;
    }

    /**
     * Add a predefined choice for this option.
     * <br>The user can only provide one of the choices and cannot specify any other value.
     *
     * @param  name
     *         The name used in the client
     * @param  value
     *         The value received in {@link net.dv8tion.jda.api.interactions.commands.OptionMapping OptionMapping}
     *
     * @throws IllegalArgumentException
     *         If the name or value is null, empty, or longer than 100 characters.
     *         Also thrown if this is not an option of type {@link OptionType#STRING} or more than 25 choices are provided.
     *
     * @return The OptionData instance, for chaining
     */
    @Nonnull
    public OptionData addChoice(@Nonnull String name, @Nonnull String value)
    {
        Checks.notEmpty(name, "Name");
        Checks.notEmpty(value, "Value");
        Checks.notLonger(name, 100, "Name");
        Checks.notLonger(value, 100, "Value");
        Checks.check(choices.size() < 25, "Cannot have more than 25 choices for an option!");
        if (type != OptionType.STRING)
            throw new IllegalArgumentException("Cannot add string choice for OptionType." + type);
        choices.put(name, value);
        return this;
    }

    @Nonnull
    @Override
    public DataObject toData()
    {
        DataObject json = DataObject.empty()
                .put("type", type.getKey())
                .put("name", name)
                .put("description", description);
        if (type != OptionType.SUB_COMMAND && type != OptionType.SUB_COMMAND_GROUP)
            json.put("required", isRequired);
        if (choices != null && !choices.isEmpty())
        {
            json.put("choices", DataArray.fromCollection(choices.entrySet()
                    .stream()
                    .map(entry -> DataObject.empty().put("name", entry.getKey()).put("value", entry.getValue()))
                    .collect(Collectors.toList())));
        }
        return json;
    }

    /**
     * Parses the provided serialization back into an OptionData instance.
     * <br>This is the reverse function for {@link #toData()}.
     *
     * @param  json
     *         The serialized {@link DataObject} representing the option
     *
     * @throws net.dv8tion.jda.api.exceptions.ParsingException
     *         If the serialized object is missing required fields
     * @throws IllegalArgumentException
     *         If any of the values are failing the respective checks such as length
     *
     * @return The parsed OptionData instance, which can be further configured through setters
     */
    @Nonnull
    public static OptionData load(@Nonnull DataObject json)
    {
        String name = json.getString("name");
        String description = json.getString("description");
        OptionType type = OptionType.fromKey(json.getInt("type"));
        OptionData option = new OptionData(type, name, description);
        option.setRequired(json.getBoolean("required"));
        json.optArray("choices").ifPresent(choices1 ->
                choices1.stream(DataArray::getObject).forEach(o ->
                {
                    Object value = o.get("value");
                    if (value instanceof Number)
                        option.addChoice(o.getString("name"), ((Number) value).intValue());
                    else
                        option.addChoice(o.getString("name"), value.toString());
                })
        );
        return option;
    }
}