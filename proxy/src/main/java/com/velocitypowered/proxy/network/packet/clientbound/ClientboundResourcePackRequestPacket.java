package com.velocitypowered.proxy.network.packet.clientbound;

import com.google.common.base.MoreObjects;
import com.velocitypowered.api.network.ProtocolVersion;
import com.velocitypowered.proxy.network.ProtocolUtils;
import com.velocitypowered.proxy.network.packet.Packet;
import com.velocitypowered.proxy.network.packet.PacketHandler;
import com.velocitypowered.proxy.network.packet.PacketReader;
import com.velocitypowered.proxy.network.packet.PacketWriter;
import io.netty.buffer.ByteBuf;
import java.util.Objects;

public class ClientboundResourcePackRequestPacket implements Packet {
  public static final PacketReader<ClientboundResourcePackRequestPacket> DECODER = (buf, version) -> {
    final String url = ProtocolUtils.readString(buf);
    final String hash = ProtocolUtils.readString(buf);
    return new ClientboundResourcePackRequestPacket(url, hash);
  };
  public static final PacketWriter<ClientboundResourcePackRequestPacket> ENCODER = PacketWriter.deprecatedEncode();

  private final String url;
  private final String hash;

  public ClientboundResourcePackRequestPacket(final String url, final String hash) {
    this.url = Objects.requireNonNull(url, "url");
    this.hash = Objects.requireNonNull(hash, "hash");
  }

  @Override
  public void encode(ByteBuf buf, ProtocolVersion protocolVersion) {
    ProtocolUtils.writeString(buf, url);
    ProtocolUtils.writeString(buf, hash);
  }

  @Override
  public boolean handle(PacketHandler handler) {
    return handler.handle(this);
  }

  public String getUrl() {
    return url;
  }

  public String getHash() {
    return hash;
  }

  @Override
  public String toString() {
    return MoreObjects.toStringHelper(this)
      .add("url", this.url)
      .add("hash", this.hash)
      .toString();
  }
}
