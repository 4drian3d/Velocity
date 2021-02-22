package com.velocitypowered.proxy.network.packet;

import com.google.common.base.MoreObjects;
import com.velocitypowered.api.network.ProtocolVersion;
import io.netty.buffer.ByteBuf;
import java.util.function.LongFunction;

public abstract class AbstractStatusPingPacket implements Packet {
  protected static <P extends AbstractStatusPingPacket> PacketReader<P> decoder(final LongFunction<P> factory) {
    return (buf, version) -> {
      final long randomId = buf.readLong();
      return factory.apply(randomId);
    };
  }
  protected static <P extends AbstractStatusPingPacket> PacketWriter<P> encoder() {
    return (buf, packet, version) -> buf.writeLong(packet.getRandomId());
  }

  private final long randomId;

  protected AbstractStatusPingPacket(final long randomId) {
    this.randomId = randomId;
  }

  public long getRandomId() {
    return randomId;
  }

  @Override
  public String toString() {
    return MoreObjects.toStringHelper(this)
      .add("randomId", this.randomId)
      .toString();
  }
}
