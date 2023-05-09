import {emptyPage} from "@/DefaultObjects";
import {MainContainer} from "@/MainContainer/MainContainer";
import * as React from "react";
import {useCallback, useEffect, useState} from "react";
import {Absolute, Box, Divider, Flex, Link, Relative} from "@/ui-components";
import Grid from "@/ui-components/Grid";
import * as Heading from "@/ui-components/Heading";
import {Spacer} from "@/ui-components/Spacer";
import {AppCard, ApplicationCardType, FavoriteApp} from "./Card";
import * as Pages from "./Pages";
import {SidebarPages, useSidebarPage} from "@/ui-components/SidebarPagesEnum";
import {useTitle} from "@/Navigation/Redux/StatusActions";
import {useRefreshFunction} from "@/Navigation/Redux/HeaderActions";
import {useCloudAPI, useCloudCommand} from "@/Authentication/DataHook";
import * as UCloud from "@/UCloud";
import {compute} from "@/UCloud";
import ApplicationSummaryWithFavorite = compute.ApplicationSummaryWithFavorite;
import AppStoreOverview = compute.AppStoreOverview;
import {ReducedApiInterface, useResourceSearch} from "@/Resource/Search";
import {injectStyle, injectStyleSimple} from "@/Unstyled";
import {useDispatch, useSelector} from "react-redux";
import {toggleAppFavorite} from "./Redux/Actions";

export const ApiLike: ReducedApiInterface = {
    routingNamespace: "applications",
    titlePlural: "Applications"
};

export const ShowAllTagItem: React.FunctionComponent<{tag?: string; children: React.ReactNode;}> = props => (
    <Link to={props.tag ? Pages.browseByTag(props.tag) : Pages.browse()}>{props.children}</Link>
);

function favoriteStatusKey(metadata: compute.ApplicationMetadata): string {
    return `${metadata.name}/${metadata.version}`;
}

const ScrollButtonClass = injectStyle("scroll-button", k => `
    ${k} {
        background-color: var(--blue);
        color: var(--white);
        width: 32px;
        height: 32px;
        border-radius: 16px;
        cursor: pointer;
        user-select: none;
        font-weight: 800;
        font-size: 18px;
        padding-left: 12px;
        padding-top: 1px;
    }

    ${k}[data-is-left="true"] {
        padding-left: 10px;
    }
`);

function ScrollButton({disabled, text, onClick}: {disabled: boolean; text: string; onClick(): void}): JSX.Element {
    return <div onClick={onClick} data-is-left={text === "⟨"} className={ScrollButtonClass} data-disabled={disabled}>
        {text}
    </div>
}

type FavoriteStatus = Record<string, {override: boolean, app: ApplicationSummaryWithFavorite}>;

const ApplicationsOverview: React.FunctionComponent = () => {
    const [sections, fetchOverview] = useCloudAPI<AppStoreOverview>(
        {noop: true},
        {sections: []}
    );

    const [refreshId, setRefreshId] = useState<number>(0);

    useEffect(() => {
        fetchOverview(UCloud.compute.apps.appStoreOverview());
    }, [refreshId]);

    useResourceSearch(ApiLike);

    const dispatch = useDispatch();

    useTitle("Applications");
    useSidebarPage(SidebarPages.AppStore);
    const refresh = () => {
        setRefreshId(refreshId + 1);
    };
    useRefreshFunction(refresh);

    const [, invokeCommand] = useCloudCommand();
    const favoriteStatus = React.useRef<FavoriteStatus>({});

    const onFavorite = useCallback(async (app: ApplicationSummaryWithFavorite) => {
        // Note(Jonas): This used to check commandLoading (from invokeCommand), but this gets stuck at true, so removed for now.
        const key = favoriteStatusKey(app.metadata);
        const isFavorite = favoriteStatus.current[key]?.override ?? app.favorite;
        if (favoriteStatus.current[key]) {
            delete favoriteStatus.current[key]
        } else {
            favoriteStatus.current[key] = {override: !isFavorite, app};
        }
        favoriteStatus.current = {...favoriteStatus.current};
        dispatch(toggleAppFavorite(app, !isFavorite));
        try {
            await invokeCommand(UCloud.compute.apps.toggleFavorite({
                appName: app.metadata.name,
                appVersion: app.metadata.version
            }));
        } catch (e) {
            favoriteStatus.current[key].override = !favoriteStatus.current[key].override;
            favoriteStatus.current = {...favoriteStatus.current};
        }
    }, [favoriteStatus]);

    const main = (
        <Box mx="auto" maxWidth="1340px">
            <Box mt="12px" />
            <FavoriteAppRow
                columns={7}
                favoriteStatus={favoriteStatus}
                onFavorite={onFavorite}
                refreshId={refreshId}
            />
            <Divider mt="18px" />
            {sections.data.sections.map(section =>
                <TagGrid
                    key={section.name + section.type}
                    tag={section.name}
                    items={section.applications}
                    columns={section.columns}
                    favoriteStatus={favoriteStatus}
                    onFavorite={onFavorite}
                    tagBanList={[]}
                    refreshId={refreshId}
                />
            )}
        </Box>
    );
    return (<div className={AppOverviewMarginPaddingHack}><MainContainer main={main} /></div>);
};

const AppOverviewMarginPaddingHack = injectStyleSimple("HACK-HACK-HACK", `
/* HACK */
    margin-top: -12px;
    padding-top: 12px;
/* HACK */
`);

const TagGridTopBoxClass = injectStyle("tag-grid-top-box", k => `
    ${k} {
        border-top-left-radius: 10px;
        border-top-right-radius: 10px;
    }
`);

const TagGridBottomBoxClass = injectStyle("tag-grid-bottom-box", k => `
    ${k} {
        padding: 0px 10px 15px 0px;
        margin-left: 10px;
        margin-right: 10px;
        border-bottom-left-radius: 10px;
        border-bottom-right-radius: 10px;
        overflow-x: scroll;
    }
`);


interface TagGridProps {
    tag: string;
    items: ApplicationSummaryWithFavorite[];
    tagBanList?: string[];
    columns: number;
    favoriteStatus: React.MutableRefObject<FavoriteStatus>;
    onFavorite: (app: ApplicationSummaryWithFavorite) => void;
    refreshId: number;
}

function filterAppsByFavorite(
    items: compute.ApplicationSummaryWithFavorite[],
    showFavorites: boolean,
    tagBanList: string[],
    favoriteStatus: React.MutableRefObject<FavoriteStatus>
): compute.ApplicationSummaryWithFavorite[] {
    let _filteredItems = items
        .filter(it => !it.tags.some(_tag => tagBanList.includes(_tag)))
        .filter(item => {
            const isFavorite = favoriteStatus.current[favoriteStatusKey(item.metadata)]?.override ?? item.favorite;
            return isFavorite === showFavorites;
        });

    if (showFavorites) {
        _filteredItems = _filteredItems.concat(Object.values(favoriteStatus.current).filter(it => it.override).map(it => it.app));
        _filteredItems = _filteredItems.filter(it => favoriteStatus.current[favoriteStatusKey(it.metadata)]?.override !== false);
    }

    // Remove duplicates (This can happen due to favorite cache)
    {
        const observed = new Set<string>();
        const newList: ApplicationSummaryWithFavorite[] = [];
        for (const item of _filteredItems) {
            const key = favoriteStatusKey(item.metadata);
            if (!observed.has(key)) {
                observed.add(key);
                newList.push(item);
            }
        }
        return newList;
    }
}

function FavoriteAppRow({favoriteStatus, onFavorite}: Omit<TagGridProps, "tag" | "items" | "tagBanList">): JSX.Element {
    const items = useSelector<ReduxObject, compute.ApplicationSummaryWithFavorite[]>(it => it.sidebar.favorites);
    const filteredItems = React.useMemo(() =>
        filterAppsByFavorite(items, true, [], favoriteStatus),
        [items, favoriteStatus.current]);

    return <Flex overflowX="scroll" width="100%">
        <Flex mx="auto" mb="16px">
            {filteredItems.map(app =>
                <FavoriteApp key={app.metadata.name + app.metadata.version} name={app.metadata.name} version={app.metadata.version} onFavorite={() => onFavorite(app)} />
            )}
        </Flex>
    </Flex>
}

const SCROLL_SPEED = 156 * 4;
const TagGrid: React.FunctionComponent<TagGridProps> = ({
    tag, items, tagBanList = [], favoriteStatus, onFavorite
}: TagGridProps) => {
    const filteredItems = React.useMemo(() =>
        filterAppsByFavorite(items, false, tagBanList, favoriteStatus),
        [items, favoriteStatus.current]);

    const scrollRef = React.useRef<HTMLDivElement>(null);

    if (filteredItems.length === 0) return null;

    const firstFour = filteredItems.length > 4 ? filteredItems.slice(0, 4) : filteredItems.slice(0, 1);
    const remaining = filteredItems.length > 4 ? filteredItems.slice(4) : filteredItems.slice(1);

    const hasScroll = scrollRef.current && scrollRef.current.scrollWidth > scrollRef.current.clientWidth;

    return (
        <>
            <div className={TagGridTopBoxClass}>
                <Spacer
                    mt="15px" px="10px" alignItems={"center"}
                    left={<Heading.h2>{tag}</Heading.h2>}
                    right={(
                        <ShowAllTagItem tag={tag}>
                            <Heading.h4>Show All</Heading.h4>
                        </ShowAllTagItem>
                    )}
                />
            </div>
            {!hasScroll ? null : <>
                <Relative>
                    <Absolute height={0} width={0} top="152px">
                        <ScrollButton disabled={false} text={"⟨"} onClick={() => {
                            if (scrollRef.current) scrollRef.current.scrollBy({left: -SCROLL_SPEED, behavior: "smooth"});

                        }} />
                    </Absolute>
                </Relative>
                <Relative>
                    <Absolute height={0} width={0} right="0" top="152px">
                        <ScrollButton disabled={false} text={"⟩"} onClick={() => {
                            if (scrollRef.current) scrollRef.current.scrollBy({left: SCROLL_SPEED, behavior: "smooth"});
                        }} />
                    </Absolute>
                </Relative>
            </>}
            <div ref={scrollRef} className={TagGridBottomBoxClass}>
                <Grid
                    p="8px"
                    mx="auto"
                    gridGap="10px"
                    gridTemplateRows={"repeat(1, 1fr)"}
                    gridTemplateColumns={"repeat(auto-fill, 156px)"}
                    style={{gridAutoFlow: "column"}}
                >
                    {remaining.map(app =>
                        <Link key={app.metadata.name + app.metadata.version} to={Pages.run(app.metadata.name, app.metadata.version)}>
                            <AppCard
                                type={ApplicationCardType.EXTRA_TALL}
                                onFavorite={() => onFavorite(app)}
                                app={app}
                                isFavorite={app.favorite}
                                tags={app.tags}
                            />
                        </Link>
                    )}
                </Grid>
            </div>
            <Grid
                py="8px"
                pl="4px"
                mx="auto"
                gridGap="4px"
                gridTemplateRows={`repeat(1, 1fr)`}
                gridTemplateColumns={"repeat(auto-fill, 332px)"}
                style={{gridAutoFlow: "column"}}
                overflowX={"scroll"}
            >
                {firstFour.map(app =>
                    <Link key={app.metadata.name + app.metadata.version} to={Pages.run(app.metadata.name, app.metadata.version)}>
                        <AppCard
                            type={ApplicationCardType.WIDE}
                            onFavorite={() => onFavorite(app)}
                            app={app}
                            isFavorite={app.favorite}
                            tags={app.tags}
                        />
                    </Link>
                )}
            </Grid>
        </>
    );
};

export default ApplicationsOverview;
