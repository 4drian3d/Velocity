package com.velocitypowered.proxy.network.packet.serverbound;

import com.velocitypowered.proxy.network.packet.AbstractKeepAlivePacket;
import com.velocitypowered.proxy.network.packet.Packet;
import com.velocitypowered.proxy.network.packet.PacketHandler;
import com.velocitypowered.proxy.network.packet.PacketReader;
import com.velocitypowered.proxy.network.packet.PacketWriter;

public class ServerboundKeepAlivePacket extends AbstractKeepAlivePacket implements Packet {
  public static final PacketReader<ServerboundKeepAlivePacket> DECODER = decoder(ServerboundKeepAlivePacket::new);
  public static final PacketWriter<ServerboundKeepAlivePacket> ENCODER = encoder();

  public ServerboundKeepAlivePacket(final long randomId) {
    super(randomId);
  }

  @Override
  public boolean handle(PacketHandler handler) {
    return handler.handle(this);
  }
}
