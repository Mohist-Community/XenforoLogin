/*
 * Copyright 2020 Mohist-Community
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package red.mohist.xenforologin.sponge.listeners;

import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.event.Listener;
import org.spongepowered.api.event.filter.cause.First;
import org.spongepowered.api.event.message.MessageChannelEvent.Chat;
import red.mohist.xenforologin.core.XenforoLoginCore;
import red.mohist.xenforologin.core.utils.Config;
import red.mohist.xenforologin.core.utils.Helper;
import red.mohist.xenforologin.sponge.implementation.SpongePlayer;
import red.mohist.xenforologin.sponge.interfaces.SpongeAPIListener;

public class ChatListener implements SpongeAPIListener {

    @Listener
    public void onChatEvent(Chat event, @First Player spongePlayer) {
        SpongePlayer player = new SpongePlayer(spongePlayer);
        if (!XenforoLoginCore.instance.needCancelled(player)) {
            if (Config.getBoolean("secure.cancel_chat_after_login", false)) {
                player.sendMessage(Helper.langFile("logged_in"));
                event.setCancelled(true);
            }
            return;
        }
        event.setCancelled(true);
        XenforoLoginCore.instance.onChat(player, event.getRawMessage().toPlainSingle());
    }

    @Override
    public void eventClass() {
        Chat.class.getName();
    }
}
