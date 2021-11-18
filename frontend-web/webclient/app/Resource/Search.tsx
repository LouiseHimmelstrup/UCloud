import {useHistory} from "react-router";
import {useSearch, useSearchPlaceholder} from "@/DefaultObjects";
import {useCallback} from "react";
import {buildQueryString} from "@/Utilities/URIUtilities";

export interface ReducedApiInterface {
    routingNamespace: string;
    titlePlural: string;
}

export function useResourceSearch(api: ReducedApiInterface) {
    const history = useHistory();
    const onSearch = useCallback((q) => {
        if (q === "") {
            history.push(`/${api.routingNamespace}`);
        } else {
            history.push(buildQueryString(`/${api.routingNamespace}/search`, {q}));
        }
    }, [api]);

    useSearch(onSearch);
    const searchPlaceholder = `Search ${api.titlePlural.toLowerCase()}...`;
    useSearchPlaceholder(searchPlaceholder);
}
