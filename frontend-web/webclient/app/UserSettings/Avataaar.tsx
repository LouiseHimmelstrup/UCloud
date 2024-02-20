import {Client} from "@/Authentication/HttpClientInstance";
import Spinner from "@/LoadingIcon/LoadingIcon";
import {usePage} from "@/Navigation/Redux";
import PromiseKeeper from "@/PromiseKeeper";
import * as React from "react";
import {useDispatch} from "react-redux";
import {snackbarStore} from "@/Snackbar/SnackbarStore";
import {Box, Button, Flex, Label, Select} from "@/ui-components";
import {findAvatarQuery} from "@/Utilities/AvatarUtilities";
import {errorMessageOrDefault} from "@/UtilityFunctions";
import * as Options from "./AvatarOptions";
import {saveAvatar} from "./Redux";
import {avatarState} from "@/AvataaarLib/hook";
import {AvatarType, defaultAvatar} from "@/AvataaarLib";
import Avatar from "@/AvataaarLib/avatar";
import {SidebarTabId} from "@/ui-components/SidebarComponents";

function Modification(): React.JSX.Element {
    const [avatar, setAvatar] = React.useState(defaultAvatar);
    const [loading, setLoading] = React.useState(true);

    usePage("Edit Avatar", SidebarTabId.NONE);

    const dispatch = useDispatch();

    React.useEffect(() => {
        const promises = new PromiseKeeper();
        fetchAvatar(promises);
        return () => promises.cancelPromises();
    }, []);

    return (
        <Box overflow={"hidden"} height="100%" maxWidth="1200px" mx="auto">
            <Box>
                <Flex mx="auto">
                    <Box mr="auto" />
                    <Avatar
                        style={{height: "150px"}}
                        avatarStyle="Circle"
                        {...avatar}
                    />
                    <Box ml="auto" />
                </Flex>
                <Button
                    ml="auto"
                    mr="auto"
                    onClick={async () => {
                        dispatch(await saveAvatar(avatar));
                        avatarState.invalidateAndUpdate([Client.username!]);
                    }}
                    mt="5px"
                    mb="5px"
                    color="primaryMain"
                >
                    Update avatar
                </Button>
            </Box>
            {loading ? <Spinner /> : (
                <Box maxWidth="720px" overflowY="auto" maxHeight={"calc(100% - 197px - 32px)"} mx="auto" mb="16px">
                    <AvatarSelect
                        defaultValue={avatar.top}
                        update={top => setAvatar({...avatar, top})}
                        options={Options.Top}
                        title="Top"
                        disabled={false}
                    />
                    <AvatarSelect
                        defaultValue={avatar.hatColor}
                        update={hatColor => setAvatar({...avatar, hatColor})}
                        options={Options.HatColor}
                        title="Hat color"
                        disabled={!["Turban", "Hijab", "WinterHat1", "WinterHat2", "WinterHat3", "WinterHat4"]
                            .includes(avatar.top)}
                    />
                    <AvatarSelect
                        defaultValue={avatar.topAccessory}
                        update={topAccessory => setAvatar({...avatar, topAccessory})}
                        options={Options.TopAccessory}
                        title="Accessories"
                        disabled={avatar.top === "Eyepatch"}
                    />
                    <AvatarSelect
                        defaultValue={avatar.hairColor}
                        update={hairColor => setAvatar({...avatar, hairColor})}
                        options={Options.HairColor}
                        title="Hair color"
                        disabled={!avatar.top.includes("Hair") || ["LongHairFrida", "LongHairShavedSides"].includes(avatar.top)}
                    />
                    <AvatarSelect
                        defaultValue={avatar.facialHair}
                        update={facialHair => setAvatar({...avatar, facialHair})}
                        options={Options.FacialHair}
                        title="Facial Hair"
                        disabled={avatar.top === "Hijab"}
                    />
                    <AvatarSelect
                        defaultValue={avatar.facialHairColor}
                        update={facialHairColor => setAvatar({...avatar, facialHairColor})}
                        options={Options.FacialHairColor}
                        title="Facial Hair Color"
                        disabled={avatar.facialHair === "Blank"}
                    />
                    <AvatarSelect
                        defaultValue={avatar.clothes}
                        update={clothes => setAvatar({...avatar, clothes})}
                        options={Options.Clothes}
                        title="Clothes"
                        disabled={false}
                    />
                    <AvatarSelect
                        defaultValue={avatar.colorFabric}
                        title="Clothes Fabric"
                        options={Options.ColorFabric}
                        update={colorFabric => setAvatar({...avatar, colorFabric})}
                        disabled={avatar.clothes === "BlazerShirt" || avatar.clothes === "BlazerSweater"}
                    />
                    <AvatarSelect
                        defaultValue={avatar.clothesGraphic}
                        title="Graphic"
                        update={clothesGraphic => setAvatar({...avatar, clothesGraphic})}
                        options={Options.ClothesGraphic}
                        disabled={avatar.clothes !== "GraphicShirt"}
                    />
                    <AvatarSelect
                        defaultValue={avatar.eyes}
                        title="Eyes"
                        options={Options.Eyes}
                        update={eyes => setAvatar({...avatar, eyes})}
                        disabled={false}
                    />
                    <AvatarSelect
                        defaultValue={avatar.eyebrows}
                        title="Eyebrow"
                        options={Options.Eyebrows}
                        update={eyebrows => setAvatar({...avatar, eyebrows})}
                        disabled={false}
                    />
                    <AvatarSelect
                        defaultValue={avatar.mouthTypes}
                        title="Mouth type"
                        options={Options.MouthTypes}
                        update={mouthTypes => setAvatar({...avatar, mouthTypes})}
                        disabled={false}
                    />
                    <AvatarSelect
                        defaultValue={avatar.skinColors}
                        title={"Skin color"}
                        options={Options.SkinColors}
                        update={skinColors => setAvatar({...avatar, skinColors})}
                        disabled={false}
                    />
                </Box>)}
        </Box>
    );

    async function fetchAvatar(promises: PromiseKeeper): Promise<void> {
        try {
            const r = await promises.makeCancelable(Client.get<AvatarType>(findAvatarQuery, undefined)).promise;
            setAvatar(r.response);
        } catch (e) {
            if (!e.isCanceled)
                snackbarStore.addFailure(errorMessageOrDefault(e, "An error occurred fetching current Avatar"), false);
        } finally {
            setLoading(false);
        }
    }
}

interface AvatarSelect<T1, T2> {
    update: (value: T1) => void;
    defaultValue: T1;
    options: T2;
    title: string;
    disabled: boolean;
}

function AvatarSelect<T1 extends string, T2 extends Object>({
    update,
    options,
    title,
    disabled,
    defaultValue
}: AvatarSelect<T1, T2>): JSX.Element | null {
    if (disabled) return null;
    return (
        <Label mt="0.8em">{title}
            <Select
                defaultValue={defaultValue}
                onChange={e => update(e.target.value as T1)}
            >
                {Object.keys(options).map(it => <option key={it}>{it}</option>)}
            </Select>
        </Label>
    );
}

export default Modification;
