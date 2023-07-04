import * as React from "react";
import {emptyPage} from "@/DefaultObjects";
import {joinToString} from "@/UtilityFunctions";
import {useLocation, useNavigate} from "react-router";
import {useEffect} from "react";
import {buildQueryString, getQueryParamOrElse} from "@/Utilities/URIUtilities";
import {useCloudAPI, useCloudCommand} from "@/Authentication/DataHook";
import * as UCloud from "@/UCloud";
import {GridCardGroup} from "@/ui-components/Grid";
import {AppCard, ApplicationCardType} from "@/Applications/Card";
import * as Pagination from "@/Pagination";
import {Input} from "@/ui-components";
import AppRoutes from "@/Routes";
import {Link} from "react-router-dom";
import * as Pages from "./Pages";

interface SearchQuery {
    tags: string[];
    query: string;
    showAllVersions: boolean;
    page: number;
    itemsPerPage: number;
}

function readQuery(queryParams: string): SearchQuery {
    const tags: string[] = [];
    const tagsQuery = getQueryParamOrElse(queryParams, "tags", "");
    tagsQuery.split(",").forEach(it => {
        if (it !== "") {
            tags.push(it);
        }
    });
    const showAllVersions = getQueryParamOrElse(queryParams, "showAllVersions", "false") === "true";
    const query = getQueryParamOrElse(queryParams, "query", "");
    let itemsPerPage = parseInt(getQueryParamOrElse(queryParams, "itemsPerPage", "25"), 10);
    let page = parseInt(getQueryParamOrElse(queryParams, "page", "0"), 10);
    if (isNaN(itemsPerPage) || itemsPerPage <= 0) itemsPerPage = 25;
    if (isNaN(page) || page < 0) page = 0;

    return {query, tags, showAllVersions, itemsPerPage, page};
}

export const SearchResults: React.FunctionComponent<{entriesPerPage: number}> = ({entriesPerPage}) => {
    const navigate = useNavigate();
    const location = useLocation();
    const [, invokeCommand] = useCloudCommand();
    const [results, fetchResults] = useCloudAPI<UCloud.Page<UCloud.compute.ApplicationSummaryWithFavorite>>(
        {noop: true},
        emptyPage
    );

    const queryParams = location.search;
    const parsedQuery = readQuery(queryParams);

    useEffect(() => {
        fetchResults(
            UCloud.compute.apps.searchApps({
                query: new URLSearchParams(queryParams).get("q") ?? "",
                itemsPerPage: 100,
                page: parsedQuery.page,
            })
        );
    }, [queryParams]);

    const toggleFavorite = React.useCallback(async (appName: string, appVersion: string) => {
        await invokeCommand(UCloud.compute.apps.toggleFavorite({appName, appVersion}));
        fetchResults(
            UCloud.compute.apps.searchApps({
                query: new URLSearchParams(queryParams).get("q") ?? "",
                itemsPerPage: 100,
                page: 0
            })
        );
    }, [fetch]);

    return <>
        <Input
            my="8px"
            width="400px"
            mx="auto"
            defaultValue={new URLSearchParams(queryParams).get("q") ?? ""}
            placeholder="Application name..."
            onKeyUp={e => {
                if (e.key === "Enter") {
                    navigate(AppRoutes.apps.search((e.target as unknown as {value: string}).value));
                }
            }}
        />

        <Pagination.List
            loading={results.loading}
            page={results.data}
            pageRenderer={page => (
                <GridCardGroup minmax={322}>
                    {page.items.map(app => (
                        <Link key={`${app.metadata.name}${app.metadata.version}`} to={Pages.run(app.metadata.name, app.metadata.version)}>
                            <AppCard
                                app={app}
                                type={ApplicationCardType.WIDE}
                                onFavorite={toggleFavorite}
                                isFavorite={app.favorite}
                                tags={app.tags}
                            />
                        </Link>))
                    }
                </GridCardGroup>
            )}
            onPageChanged={newPage => {
                navigate(buildQueryString("/applications/search", {
                    q: new URLSearchParams(queryParams).get("q") ?? "",
                    tags: joinToString(parsedQuery.tags),
                    showAllVersions: parsedQuery.showAllVersions.toString(),
                    page: newPage.toString(),
                    itemsPerPage: parsedQuery.itemsPerPage.toString(),
                }));
            }}
        />
    </>;
};
