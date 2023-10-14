/*
 * Copyright (C) 2023 Velocity Contributors
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package com.velocitypowered.proxy.util;

import java.util.List;
import java.util.regex.Pattern;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.config.Configuration;
import org.apache.logging.log4j.core.config.plugins.Plugin;
import org.apache.logging.log4j.core.layout.PatternLayout;
import org.apache.logging.log4j.core.pattern.ConverterKeys;
import org.apache.logging.log4j.core.pattern.LogEventPatternConverter;
import org.apache.logging.log4j.core.pattern.PatternConverter;
import org.apache.logging.log4j.core.pattern.PatternFormatter;
import org.apache.logging.log4j.core.pattern.PatternParser;

/**
 * Strip Format Converter.
 */
@Plugin(name = "stripAnsi", category = PatternConverter.CATEGORY)
@ConverterKeys("stripAnsi")
public class StripAnsiConverter extends LogEventPatternConverter {
  private static final Pattern ANSI_PATTERN = Pattern.compile("\u001B\\[[;\\d]*m");
  private final List<PatternFormatter> formatters;

  /**
   * Constructs an instance of LoggingEventPatternConverter.
   */
  protected StripAnsiConverter(List<PatternFormatter> formatters) {
    super("stripAnsi", null);
    this.formatters = formatters;
  }

  @Override
  public void format(final LogEvent event, final StringBuilder toAppendTo) {
    int start = toAppendTo.length();
    for (final PatternFormatter formatter : formatters) {
      formatter.format(event, toAppendTo);
    }
    String content = toAppendTo.substring(start);
    content = ANSI_PATTERN.matcher(content).replaceAll("");

    toAppendTo.setLength(start);
    toAppendTo.append(content);
  }

  /**
   * a.
   *
   * @param config  config
   * @param options options
   * @return idk
   */
  public static StripAnsiConverter newInstance(Configuration config, String[] options) {
    if (options.length != 1) {
      LOGGER.error("Incorrect number of options on stripFormat. Expected 1 received "
              + options.length);
      return null;
    }
    PatternParser parser = PatternLayout.createPatternParser(config);
    List<PatternFormatter> formatters = parser.parse(options[0]);
    return new StripAnsiConverter(formatters);
  }
}
