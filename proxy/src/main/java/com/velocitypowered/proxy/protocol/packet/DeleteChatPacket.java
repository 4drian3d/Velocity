/*
 * Copyright (C) 2024 Velocity Contributors
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

package com.velocitypowered.proxy.protocol.packet;

import com.velocitypowered.api.network.ProtocolVersion;
import com.velocitypowered.proxy.connection.MinecraftSessionHandler;
import com.velocitypowered.proxy.protocol.MinecraftPacket;
import com.velocitypowered.proxy.protocol.ProtocolUtils;
import io.netty.buffer.ByteBuf;
import org.jetbrains.annotations.Nullable;

@SuppressWarnings("checkstyle:MissingJavadocType")
public class DeleteChatPacket implements MinecraftPacket {
  private int id;
  private byte @Nullable [] signature;

  public DeleteChatPacket() {
  }

  public DeleteChatPacket(int id, byte @Nullable [] signature) {
    this.id = id;
    this.signature = signature;
  }

  @Override
  public void decode(
          final ByteBuf buf,
          final ProtocolUtils.Direction direction,
          final ProtocolVersion protocolVersion
  ) {
    this.id = ProtocolUtils.readVarInt(buf);
    if (protocolVersion.lessThan(ProtocolVersion.MINECRAFT_1_20) || id == -1) {
      this.signature = ProtocolUtils.readByteArray(buf);
    }
  }

  @Override
  public void encode(
          final ByteBuf buf,
          final ProtocolUtils.Direction direction,
          final ProtocolVersion protocolVersion
  ) {
    ProtocolUtils.writeVarInt(buf, this.id);
    // id == -1 = signature != null
    if (this.signature != null) {
      ProtocolUtils.writeByteArray(buf, this.signature);
    }
  }

  @Override
  public boolean handle(MinecraftSessionHandler handler) {
    return false;
  }
}
