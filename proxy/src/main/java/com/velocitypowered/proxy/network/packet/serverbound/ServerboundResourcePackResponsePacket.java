package com.velocitypowered.proxy.network.packet.serverbound;

import com.google.common.base.MoreObjects;
import com.velocitypowered.api.event.player.PlayerResourcePackStatusEvent.Status;
import com.velocitypowered.api.network.ProtocolVersion;
import com.velocitypowered.proxy.network.ProtocolUtils;
import com.velocitypowered.proxy.network.packet.Packet;
import com.velocitypowered.proxy.network.packet.PacketHandler;
import com.velocitypowered.proxy.network.packet.PacketReader;
import com.velocitypowered.proxy.network.packet.PacketWriter;
import io.netty.buffer.ByteBuf;
import org.checkerframework.checker.nullness.qual.Nullable;

public class ServerboundResourcePackResponsePacket implements Packet {
  public static final PacketReader<ServerboundResourcePackResponsePacket> DECODER = (buf, version) -> {
    final String hash;
    if (version.lte(ProtocolVersion.MINECRAFT_1_9_4)) {
      hash = ProtocolUtils.readString(buf);
    } else {
      hash = null;
    }
    final Status status = Status.values()[ProtocolUtils.readVarInt(buf)];
    return new ServerboundResourcePackResponsePacket(hash, status);
  };
  public static final PacketWriter<ServerboundResourcePackResponsePacket> ENCODER = PacketWriter.deprecatedEncode();

  private final @Nullable String hash;
  private final Status status;

  public ServerboundResourcePackResponsePacket(final @Nullable String hash, final Status status) {
    this.hash = hash;
    this.status = status;
  }

  @Override
  public void encode(ByteBuf buf, ProtocolVersion protocolVersion) {
    if (protocolVersion.lte(ProtocolVersion.MINECRAFT_1_9_4)) {
      ProtocolUtils.writeString(buf, hash);
    }
    ProtocolUtils.writeVarInt(buf, status.ordinal());
  }

  @Override
  public boolean handle(PacketHandler handler) {
    return handler.handle(this);
  }

  public Status getStatus() {
    return status;
  }

  @Override
  public String toString() {
    return MoreObjects.toStringHelper(this)
      .add("hash", this.hash)
      .add("status", this.status)
      .toString();
  }
}
