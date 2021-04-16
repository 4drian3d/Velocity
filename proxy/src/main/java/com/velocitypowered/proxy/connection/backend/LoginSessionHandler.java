/*
 * Copyright (C) 2018 Velocity Contributors
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

package com.velocitypowered.proxy.connection.backend;

import com.velocitypowered.api.util.GameProfile;
import com.velocitypowered.proxy.VelocityServer;
import com.velocitypowered.proxy.config.PlayerInfoForwarding;
import com.velocitypowered.proxy.config.VelocityConfiguration;
import com.velocitypowered.proxy.connection.MinecraftConnection;
import com.velocitypowered.proxy.connection.MinecraftSessionHandler;
import com.velocitypowered.proxy.connection.VelocityConstants;
import com.velocitypowered.proxy.connection.util.ConnectionRequestResults;
import com.velocitypowered.proxy.connection.util.ConnectionRequestResults.Impl;
import com.velocitypowered.proxy.network.ProtocolUtils;
import com.velocitypowered.proxy.network.StateRegistry;
import com.velocitypowered.proxy.network.packet.clientbound.ClientboundDisconnectPacket;
import com.velocitypowered.proxy.network.packet.clientbound.ClientboundEncryptionRequestPacket;
import com.velocitypowered.proxy.network.packet.clientbound.ClientboundLoginPluginMessagePacket;
import com.velocitypowered.proxy.network.packet.clientbound.ClientboundServerLoginSuccessPacket;
import com.velocitypowered.proxy.network.packet.clientbound.ClientboundSetCompressionPacket;
import com.velocitypowered.proxy.network.packet.serverbound.ServerboundLoginPluginResponsePacket;
import com.velocitypowered.proxy.util.except.QuietRuntimeException;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.concurrent.CompletableFuture;
import javax.crypto.Mac;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;

public class LoginSessionHandler implements MinecraftSessionHandler {

  private static final TextComponent MODERN_IP_FORWARDING_FAILURE = Component
      .text("Your server did not send a forwarding request to the proxy. Is it set up correctly?");

  private final VelocityServer server;
  private final VelocityServerConnection serverConn;
  private final CompletableFuture<Impl> resultFuture;
  private boolean informationForwarded;

  LoginSessionHandler(VelocityServer server, VelocityServerConnection serverConn,
      CompletableFuture<Impl> resultFuture) {
    this.server = server;
    this.serverConn = serverConn;
    this.resultFuture = resultFuture;
  }

  @Override
  public boolean handle(ClientboundEncryptionRequestPacket packet) {
    throw new IllegalStateException("Backend server is online-mode!");
  }

  @Override
  public boolean handle(ClientboundLoginPluginMessagePacket packet) {
    MinecraftConnection mc = serverConn.ensureConnected();
    VelocityConfiguration configuration = server.getConfiguration();
    if (configuration.getPlayerInfoForwardingMode() == PlayerInfoForwarding.MODERN && packet
        .getChannel().equals(VelocityConstants.VELOCITY_IP_FORWARDING_CHANNEL)) {
      ByteBuf forwardingData = createForwardingData(configuration.getForwardingSecret(),
          cleanRemoteAddress(serverConn.getPlayer().getRemoteAddress()),
          serverConn.getPlayer().getGameProfile());
      ServerboundLoginPluginResponsePacket response = new ServerboundLoginPluginResponsePacket(
          packet.getId(), true, forwardingData);
      mc.write(response);
      informationForwarded = true;
    } else {
      // Don't understand
      mc.write(new ServerboundLoginPluginResponsePacket(packet.getId(), false,
          Unpooled.EMPTY_BUFFER));
    }
    return true;
  }

  @Override
  public boolean handle(ClientboundDisconnectPacket packet) {
    resultFuture.complete(ConnectionRequestResults.forDisconnect(packet, serverConn.getServer()));
    serverConn.disconnect();
    return true;
  }

  @Override
  public boolean handle(ClientboundSetCompressionPacket packet) {
    serverConn.ensureConnected().setCompressionThreshold(packet.getThreshold());
    return true;
  }

  @Override
  public boolean handle(ClientboundServerLoginSuccessPacket packet) {
    if (server.getConfiguration().getPlayerInfoForwardingMode() == PlayerInfoForwarding.MODERN
        && !informationForwarded) {
      resultFuture.complete(ConnectionRequestResults.forDisconnect(MODERN_IP_FORWARDING_FAILURE,
          serverConn.getServer()));
      serverConn.disconnect();
      return true;
    }

    // The player has been logged on to the backend server, but we're not done yet. There could be
    // other problems that could arise before we get a JoinGame packet from the server.

    // Move into the PLAY phase.
    MinecraftConnection smc = serverConn.ensureConnected();
    smc.setState(StateRegistry.PLAY);

    // Switch to the transition handler.
    smc.setSessionHandler(new TransitionSessionHandler(server, serverConn, resultFuture));
    return true;
  }

  @Override
  public void exception(Throwable throwable) {
    resultFuture.completeExceptionally(throwable);
  }

  @Override
  public void disconnected() {
    if (server.getConfiguration().getPlayerInfoForwardingMode() == PlayerInfoForwarding.LEGACY) {
      resultFuture.completeExceptionally(
          new QuietRuntimeException("The connection to the remote server was unexpectedly closed.\n"
              + "This is usually because the remote server does not have BungeeCord IP forwarding "
              + "correctly enabled.\nSee "
              + "https://docs.velocitypowered.com/en/latest/users/player-info-forwarding.html "
              + "for instructions on how to configure player info forwarding correctly.")
      );
    } else {
      resultFuture.completeExceptionally(
          new QuietRuntimeException("The connection to the remote server was unexpectedly closed.")
      );
    }
  }

  private static String cleanRemoteAddress(SocketAddress address) {
    if (address instanceof InetSocketAddress) {
      String addressString = ((InetSocketAddress) address).getAddress().getHostAddress();
      int ipv6ScopeIdx = addressString.indexOf('%');
      if (ipv6ScopeIdx == -1) {
        return addressString;
      } else {
        return addressString.substring(0, ipv6ScopeIdx);
      }
    } else {
      // Fake it
      return "127.0.0.1";
    }
  }

  private static ByteBuf createForwardingData(byte[] hmacSecret, String address,
      GameProfile profile) {
    ByteBuf forwarded = Unpooled.buffer(2048);
    try {
      ProtocolUtils.writeVarInt(forwarded, VelocityConstants.FORWARDING_VERSION);
      ProtocolUtils.writeString(forwarded, address);
      ProtocolUtils.writeUuid(forwarded, profile.getId());
      ProtocolUtils.writeString(forwarded, profile.getName());
      ProtocolUtils.writeProperties(forwarded, profile.getProperties());

      SecretKey key = new SecretKeySpec(hmacSecret, "HmacSHA256");
      Mac mac = Mac.getInstance("HmacSHA256");
      mac.init(key);
      mac.update(forwarded.array(), forwarded.arrayOffset(), forwarded.readableBytes());
      byte[] sig = mac.doFinal();

      return Unpooled.wrappedBuffer(Unpooled.wrappedBuffer(sig), forwarded);
    } catch (InvalidKeyException e) {
      forwarded.release();
      throw new RuntimeException("Unable to authenticate data", e);
    } catch (NoSuchAlgorithmException e) {
      // Should never happen
      forwarded.release();
      throw new AssertionError(e);
    }
  }
}
