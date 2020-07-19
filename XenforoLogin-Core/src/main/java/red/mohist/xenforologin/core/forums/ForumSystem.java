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

package red.mohist.xenforologin.core.forums;

import red.mohist.xenforologin.core.enums.ResultType;
import red.mohist.xenforologin.core.modules.AbstractPlayer;

import javax.annotation.Nonnull;

public interface ForumSystem {

    @Nonnull
    ResultType register(AbstractPlayer player, String password, String email);

    @Nonnull
    ResultType login(AbstractPlayer player, String password);

    @SuppressWarnings("unused")
    @Nonnull
    ResultType join(AbstractPlayer player);

    @Nonnull
    ResultType join(String name);

}
