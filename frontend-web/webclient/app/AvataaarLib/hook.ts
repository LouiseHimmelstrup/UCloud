import {AvatarType, defaultAvatar} from "@/UserSettings/Avataaar";
import {callAPI} from "@/Authentication/DataHook";
import {fetchBulkAvatars, FetchBulkAvatarsResponse} from "@/AvataaarLib/index";
import {useUState, UState} from "@/Utilities/UState";

class AvatarState extends UState<AvatarState> {
    private cache: Record<string, AvatarType> = {};

    updateCache(usernames: string[]): Promise<void> {
        const usernamesToUse = usernames.filter(it => !this.cache.hasOwnProperty(it));
        console.log("cache", this.cache);
        if (usernamesToUse.length === 0) return Promise.resolve();

        return this.run(async () => {
            const response = await callAPI<FetchBulkAvatarsResponse>(fetchBulkAvatars({usernames: usernamesToUse}));

            const newCache = response !== null ? {...this.cache, ...response.avatars} : this.cache;
            if (response !== null) {
                this.cache = newCache;
                for (const username of usernamesToUse) {
                    if (this.cache[username] == null) {
                        this.cache[username] = defaultAvatar;
                    }
                }
            }
        });
    }

    avatar(username: string): AvatarType {
        this.updateCache([username]);
        return this.cache[username] ?? defaultAvatar;
    }
}

export const avatarState = new AvatarState();

export function useAvatars(): AvatarState {
    return useUState(avatarState);
}
