import * as React from "react";
import {useCallback, useEffect, useState} from "react";
import {useCloudCommand} from "@/Authentication/DataHook";
import {Icon} from "@/ui-components";
import {useDispatch} from "react-redux";
import {toggleAppFavorite} from "./Redux/Actions";
import {ApplicationWithFavoriteAndTags} from "@/Applications/AppStoreApi";
import * as AppStore from "@/Applications/AppStoreApi";

export const FavoriteToggle: React.FunctionComponent<{
    application: ApplicationWithFavoriteAndTags
}> = ({application}) => {
    const [loading, invokeCommand] = useCloudCommand();
    const [favorite, setFavorite] = useState(application.favorite);
    const dispatch = useDispatch();
    useEffect(() => {
        setFavorite(application.favorite);
    }, [application]);

    const toggle = useCallback(async () => {
        if (!loading) {
            setFavorite(!favorite);
            dispatch(toggleAppFavorite(application, !favorite));
            invokeCommand(AppStore.toggleStar({
                name: application.metadata.name
            }));
        }
    }, [loading, favorite]);

    return <Icon ml="4px" name={favorite ? "starFilled" : "starEmpty"} mb="2px" color={"primaryMain"} onClick={toggle}/>
}
