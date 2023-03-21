import {Client} from "@/Authentication/HttpClientInstance";
import * as React from "react";
import {useDispatch, useSelector} from "react-redux";
import {
    copyToClipboard,
    isLightThemeStored,
    joinToString,
    stopPropagationAndPreventDefault,
    useFrameHidden
} from "@/UtilityFunctions";
import CONF from "../../site.config.json";
import Box from "./Box";
import ExternalLink from "./ExternalLink";
import Flex from "./Flex";
import Icon, {IconName} from "./Icon";
import Link from "./Link";
import Text, {EllipsedText, TextSpan} from "./Text";
import {ThemeColor} from "./theme";
import Tooltip from "./Tooltip";
import {useCallback} from "react";
import {useProjectId} from "@/Project/Api";
import {useProject} from "@/Project/cache";
import {AutomaticGiftClaim} from "@/Services/Gifts/AutomaticGiftClaim";
import {ResourceInit} from "@/Services/ResourceInit";
import Support from "./SupportBox";
import {VersionManager} from "@/VersionManager/VersionManager";
import Notification from "@/Notifications";
import AppRoutes from "@/Routes";
import {APICallState, useCloudAPI, useCloudCommand} from "@/Authentication/DataHook";
import {emptyPage, emptyPageV2} from "@/DefaultObjects";
import {navigateByFileType, NewsPost} from "@/Dashboard/Dashboard";
import {findAvatar} from "@/UserSettings/Redux/AvataaarActions";
import BackgroundTasks from "@/Services/BackgroundTasks/BackgroundTask";
import ClickableDropdown from "./ClickableDropdown";
import Divider from "./Divider";
import {ThemeToggler} from "./ThemeToggle";
import {AvatarType} from "@/UserSettings/Avataaar";
import {UserAvatar} from "@/AvataaarLib/UserAvatar";
import {api as FileCollectionsApi, FileCollection} from "@/UCloud/FileCollectionsApi";
import {PageV2} from "@/UCloud";
import AdminLinks from "@/Admin/Links";
import {SharesLinks} from "@/Files/Shares";
import {ProviderLogo} from "@/Providers/ProviderLogo";
import Truncate from "./Truncate";
import metadataApi from "@/UCloud/MetadataDocumentApi";
import {FileMetadataAttached} from "@/UCloud/MetadataDocumentApi";
import {fileName} from "@/Utilities/FileUtilities";
import {useNavigate} from "react-router";
import JobsApi, {Job, jobStateToIconAndColor} from "@/UCloud/JobsApi";
import {ProjectLinks} from "@/Project/ProjectLinks";
import {ResourceLinks} from "@/Resource/ResourceOptions";
import {injectStyle, injectStyleSimple} from "@/Unstyled";

const SidebarElementContainerClass = injectStyle("sidebar-element", k => `
    ${k} {
        display: flex;
        margin-left: 22px;
        justify-content: left;
        flex-flow: row;
        align-items: center;
        padding-bottom: 10px;
    }

    ${k} > ${Text} {
        white-space: nowrap;
    }
`);

const SecondarySidebarClass = injectStyle("secondary-sidebar", k => `
    ${k} {
        background-color: #5C89F4;
        transition: transform 0.1s;
        width: 0;
        
        display: flex;
        flex-direction: column;
        transform: translate(-300px, 0);
        box-sizing: border-box;
    }
    
    ${k}, ${k} a, ${k} a:hover {
        color: white;
    }
    
    ${k}[data-open="true"] {
        transform: translate(0, 0);
        padding: 16px;
    }
    
    ${k} header {
        display: flex;
        align-items: center;
    }
    
    ${k} header h1 {
        font-weight: bold;
        font-size: 20px;
        flex-grow: 1;
        margin: 0;
    }
    
    ${k} header div {
        cursor: pointer;
    }
    
    ${k} h2 {
        margin: 0;
        font-size: 16px;
        font-weight: bold;
    }
    
    ${k} ul {
        padding-left: 0;
    }
    
    ${k} > ul > li {
        font-size: 16px;
        font-weight: bold;
        list-style: none;
    }
    
    ${k} > ul span,
    ${k} > ul a {
        border-radius: 10px;
        padding: 5px 10px;
        display: block;
        margin-left: -5px;
    }
    
    ${k} > ul span:hover,
    ${k} > ul a:hover {
        background-color: rgba(255, 255, 255, 0.35);
    }
    
    ${k} > ul > li > ul > li {
        font-size: 14px;
    }
`);


const SidebarContainerClass = injectStyleSimple("sidebar-container", () => `
    color: var(--sidebar);
    align-items: center;
    display: flex;
    flex-direction: column;
    height: 100vh;
    width: var(--sidebarWidth);
    background-color: var(--sidebar);
    gap: 18px;
    z-index: 100000;
`);

const SidebarMenuItem = injectStyle("sidebar-item", k => `
    ${k} {
        cursor: pointer;
        display: flex;
        border-radius: 5px;
        width: 32px;
        height: 32px;
        margin-top: 8px;
    }
    
    ${k}:hover, ${k}[data-active="true"] {
        background-color: rgba(255, 255, 255, 0.35);
    }
    
    ${k} > * {
        margin: auto;
    }
`);

interface TextLabelProps {
    icon: IconName;
    children: | string | number | JSX.Element;
    ml?: string;
    height?: string;
    color?: ThemeColor;
    color2?: ThemeColor;
    iconSize?: string;
    textSize?: number;
    space?: string;
    title?: string;
}

export const SidebarTextLabel = ({
                                     icon, children, title, height = "30px", color = "iconColor", color2 = "iconColor2",
                                     iconSize = "18", space = "22px", textSize = 3
                                 }: TextLabelProps): JSX.Element => (
    <div className={SidebarElementContainerClass} title={title} style={{height}}>
        <Icon name={icon} color={color} color2={color2} size={iconSize} mr={space}/>
        <Text fontSize={textSize}>{children}</Text>
    </div>
);

interface SidebarElement {
    icon: IconName;
    to?: string;
}

function SidebarElement({icon, to}: SidebarElement): JSX.Element {
    if (to) {
        return (
            <Link to={to}>
                <Icon name={icon} color="white" color2="white" size={"20"}/>
            </Link>
        );
    } else return <Icon name={icon} color="white" color2="white" size={"20"}/>;
}

interface MenuElement {
    icon: IconName;
    label: string;
    to?: string | (() => string);
    show?: () => boolean;
}

interface SidebarMenuElements {
    items: MenuElement[];
    predicate: () => boolean;
}

export const sideBarMenuElements: {
    guest: SidebarMenuElements;
    general: SidebarMenuElements;
    auditing: SidebarMenuElements;
    admin: SidebarMenuElements;
} = {
    guest: {
        items: [
            {icon: "files", label: "Files", to: "/login"},
            {icon: "projects", label: "Projects", to: "/login"},
            {icon: "apps", label: "Apps", to: "/login"}
        ], predicate: () => !Client.isLoggedIn
    },
    general: {
        items: [
            {icon: "files", label: "Files", to: "/drives/"},
            {icon: "projects", label: "Projects", to: "/projects/", show: () => Client.hasActiveProject},
            {icon: "shareMenu", label: "Shares", to: "/shares/", show: () => !Client.hasActiveProject},
            {icon: "dashboard", label: "Resources"},
            {icon: "appStore", label: "Apps", to: "/applications/overview/"},
            {icon: "results", label: "Runs", to: "/jobs/"}
        ], predicate: () => Client.isLoggedIn
    },
    auditing: {items: [], predicate: () => Client.isLoggedIn},
    admin: {items: [{icon: "admin", label: "Admin"}], predicate: () => Client.userIsAdmin}
};

interface SidebarStateProps {
    loggedIn: boolean;
    avatar: AvatarType;
    activeProject?: string;
}

function hasOrParentHasClass(t: EventTarget | null, classname: string): boolean {
    var target = t;
    while (target && "classList" in target) {
        var classList = target.classList as DOMTokenList;
        if (classList.contains(classname)) return true;
        if ("parentNode" in target) {
            target = target.parentNode as EventTarget | null;
        } else {
            return false;
        }
        if (!target) return false;
    }
    return false;
}

const SIDEBAR_IDENTIFIER = "SIDEBAR_IDENTIFIER";

const SidebarItemsClass = injectStyle("sidebar-items", k => `
    ${k} {
        padding-top: 7px 0;
        flex-grow: 1;
    }
`);

const UserMenu: React.FunctionComponent<{
    avatar: AvatarType;
    onToggleTheme: (e: React.SyntheticEvent<HTMLDivElement, Event>) => void;
}> = ({avatar, onToggleTheme}) => {
    return <ClickableDropdown
        width="230px"
        left="var(--sidebarWidth)"
        bottom="0"
        colorOnHover={false}
        trigger={Client.isLoggedIn ?
            <UserAvatar avatarStyle={""} height="42px" width="42px" avatar={avatar}/> : null}
    >
        {!CONF.STATUS_PAGE ? null : (
            <>
                <Box>
                    <ExternalLink href={CONF.STATUS_PAGE}>
                        <Flex color="black">
                            <Icon name="favIcon" mr="0.5em" my="0.2em" size="1.3em" color="var(--black)"/>
                            <TextSpan color="var(--black)">Site status</TextSpan>
                        </Flex>
                    </ExternalLink>
                </Box>
                <Divider/>
            </>
        )}
        <Box>
            <Link color="black" to={AppRoutes.users.settings()}>
                <Flex color="black">
                    <Icon name="properties" color="var(--black)" color2="var(--black)" mr="0.5em" my="0.2em"
                          size="1.3em"/>
                    <TextSpan color="var(--black)">Settings</TextSpan>
                </Flex>
            </Link>
        </Box>
        <Flex>
            <Link to={"/users/avatar"}>
                <Flex color="black">
                    <Icon name="user" color="black" color2="gray" mr="0.5em" my="0.2em" size="1.3em"/>
                    <TextSpan color="var(--black)">Edit Avatar</TextSpan>
                </Flex>
            </Link>
        </Flex>
        <Flex onClick={() => Client.logout()} data-component={"logout-button"}>
            <Icon name="logout" color2="var(--black)" mr="0.5em" my="0.2em" size="1.3em"/>
            Logout
        </Flex>
        {!CONF.SITE_DOCUMENTATION_URL ? null : (
            <div>
                <ExternalLink hoverColor="text" href={CONF.SITE_DOCUMENTATION_URL}>
                    <Icon name="docs" color="black" color2="gray" mr="0.5em" my="0.2em" size="1.3em"/>
                    <TextSpan color="var(--black)">{CONF.PRODUCT_NAME} Docs</TextSpan>
                </ExternalLink>
            </div>
        )}
        {!CONF.DATA_PROTECTION_LINK ? null : (
            <div>
                <ExternalLink hoverColor="text" href={CONF.DATA_PROTECTION_LINK}>
                    <Icon name="verified" color="black" color2="gray" mr="0.5em" my="0.2em" size="1.3em"/>
                    <TextSpan color="var(--black)">{CONF.DATA_PROTECTION_TEXT}</TextSpan>
                </ExternalLink>
            </div>
        )}
        <Divider/>
        <Username/>
        <ProjectID/>
        <Divider/>
        <span>
                        <Flex cursor="auto">
                            <ThemeToggler
                                isLightTheme={isLightThemeStored()}
                                onClick={onToggleTheme}
                            />
                        </Flex>
                    </span>
    </ClickableDropdown>;
}

export const Sidebar = ({toggleTheme}: { toggleTheme(): void; }): JSX.Element | null => {
    const sidebarEntries = sideBarMenuElements;
    const {activeProject, loggedIn, avatar} = useSidebarReduxProps();

    const [selectedPage, setSelectedPage] = React.useState("");
    const [hoveredPage, setHoveredPage] = React.useState("");

    const dispatch = useDispatch();
    React.useEffect(() => {
        if (Client.isLoggedIn) {
            findAvatar().then(action => {
                if (action !== null) dispatch(action);
            });
        }
    }, []);

    if (useFrameHidden()) return null;
    if (!loggedIn) return null;

    const sidebar: MenuElement[] = Object.keys(sidebarEntries)
        .map(key => sidebarEntries[key])
        .filter(it => it.predicate())
        .flatMap(category => category.items.filter((it: MenuElement) => it?.show?.() ?? true));


    return (
        <div style={{display: "flex"}}>
            <div className={SidebarContainerClass + " " + SIDEBAR_IDENTIFIER}>
                <Link data-component={"logo"} to="/">
                    <Icon name="logoEsc" mt="10px" size="34px"/>
                </Link>

                <div
                    className={SidebarItemsClass}
                    onMouseLeave={e => {
                        if (!hasOrParentHasClass(e.relatedTarget, SIDEBAR_IDENTIFIER)) setHoveredPage("")
                    }}
                >
                    {sidebar.map(({label, icon, to}) =>
                        <div
                            key={label}
                            data-active={label === selectedPage}
                            onClick={() => setSelectedPage(label)}
                            onMouseEnter={() => setHoveredPage(label)}
                            className={SidebarMenuItem}
                        >
                            <SidebarElement
                                icon={icon}
                                to={typeof to === "function" ? to() : to}
                            />
                        </div>
                    )}
                </div>

                <>
                    {/* (Typically) invisible elements here to run various background tasks */}
                    <AutomaticGiftClaim/>
                    <ResourceInit/>
                    <VersionManager/>
                    <BackgroundTasks/>
                    <Downtimes/>
                </>

                <Notification/>
                <Support/>
                <UserMenu avatar={avatar} onToggleTheme={onToggleTheme}/>
            </div>

            <SecondarySidebar
                data-tag="secondary"
                hovered={hoveredPage}
                clicked={selectedPage}
                clearHover={() => setHoveredPage("")}
                clearClicked={() => setSelectedPage("")}
            />
        </div>
    );

    function onToggleTheme(e: React.SyntheticEvent<HTMLDivElement, Event>): void {
        stopPropagationAndPreventDefault(e);
        toggleTheme();
    }
};

function useSidebarFilesPage(): [
    APICallState<PageV2<FileCollection>>,
    APICallState<PageV2<FileMetadataAttached>>
] {
    const [drives] = useCloudAPI<PageV2<FileCollection>>(FileCollectionsApi.browse({itemsPerPage: 10/* , filterMemberFiles: "all" */}), emptyPageV2);

    const [favorites] = useCloudAPI<PageV2<FileMetadataAttached>>(
        metadataApi.browse({
            filterActive: true,
            filterTemplate: "Favorite",
            itemsPerPage: 10
        }),
        emptyPageV2
    );

    return [
        drives,
        favorites
    ];
}

function useSidebarRunsPage(): APICallState<PageV2<Job>> {
    /* TODO(Jonas): This should be fetched from the same source as the runs page. */
    const [runs] = useCloudAPI<PageV2<Job>>(JobsApi.browse({itemsPerPage: 10}), emptyPageV2);

    return runs;
}


function SecondarySidebar({
                              hovered,
                              clicked,
                              clearHover,
                              clearClicked
                          }: { hovered: string; clicked: string; clearHover(): void; clearClicked(): void }): JSX.Element {
    const [drives, favorites] = useSidebarFilesPage();
    const recentRuns = useSidebarRunsPage();

    const navigate = useNavigate();
    const [, invokeCommand] = useCloudCommand();

    const rootRef = React.useRef<HTMLDivElement>(null);
    const toggleSize = useCallback(() => {
        if (!rootRef.current) return;
        const attribute = rootRef.current.getAttribute("data-open");
        if (attribute === "false") rootRef.current.style.width = "0";
    }, []);

    const isOpen = clicked !== "" || hovered !== "";
    React.useEffect(() => {
        const current = rootRef.current;
        if (!current) return;
        if (isOpen) current.style.width = "var(--secondarySidebarWidth)";
    }, [isOpen]);

    const active = hovered ? hovered : clicked;
    /* TODO(Jonas): hovering should slide over, while clicking should push */
    return <div
        className={SecondarySidebarClass + " " + SIDEBAR_IDENTIFIER}
        onTransitionEnd={toggleSize}
        onMouseLeave={e => {
            if (!hasOrParentHasClass(e.relatedTarget, SIDEBAR_IDENTIFIER)) clearHover();
        }}
        data-open={isOpen}
        ref={rootRef}
    >
        <header>
            <h1>{active}</h1>
            {clicked !== "" ?
                <div onClick={clearClicked}>Unlock</div> :
                null
            }
        </header>

        {active !== "Files" ? null : (
            <>
                <ul>
                    <li>
                        <Link to={"/files"}>Drives</Link>
                        <ul>
                            {drives.data.items.map(it =>
                                <li>
                                    <Flex key={it.id} ml="4px">
                                        <Link hoverColor="white" to={`/files?path=${it.id}`}>
                                            <Truncate color="var(--white)">
                                                <Icon size={12} mr="4px" name="hdd" color="white" color2="white"/>
                                                {it.specification.title}
                                            </Truncate>
                                        </Link>

                                        <Flex ml="auto" mr="5px" my="auto">
                                            <ProviderLogo providerId={it.specification.product.provider} size={20}/>
                                        </Flex>
                                    </Flex>
                                </li>
                            )}
                        </ul>
                    </li>

                    <li>
                        <span>Favorite files</span>

                        <ul>
                            {favorites.data.items.map(it =>
                                <li>
                                    <Flex
                                        key={it.path}
                                        cursor="pointer"
                                        onClick={() => navigateByFileType(it, invokeCommand, navigate)}
                                    >
                                        <Flex mx="auto" my="auto">
                                            <Icon name="starFilled" size={12} mr="4px" color="white" color2="white"/>
                                        </Flex>
                                        <Truncate color="white">{fileName(it.path)}</Truncate>
                                    </Flex>
                                </li>
                            )}
                        </ul>
                    </li>
                </ul>
            </>
        )}

        {active !== "Projects" ? null : (<ProjectLinks/>)}

        {active !== "Shares" ? null : (<SharesLinks/>)}

        {active !== "Runs" ? null : (
            <Flex flexDirection="column" mr="4px">
                <TextSpan bold color="white">Most recent</TextSpan>
                {recentRuns.data.items.map(it => {
                    const [icon, color] = jobStateToIconAndColor(it.status.state);
                    return <Flex>
                        <Icon name={icon} color={color} mr={"6px"} size={16} my="auto"/>
                        <Truncate key={it.id}
                                  color="white">{it.specification.name ?? it.id} ({it.specification.application.name})</Truncate>
                    </Flex>
                })}
            </Flex>
        )}

        {active !== "Resources" ? null : (<ResourceLinks/>)}

        {active !== "Admin" ? null : (<AdminLinks/>)}
    </div>;
}

function Username(): JSX.Element | null {
    if (!Client.isLoggedIn) return null;
    return <Tooltip
        trigger={(
            <EllipsedText
                cursor="pointer"
                onClick={copyUserName}
            >
                <Icon name="id" color="black" color2="gray" mr="0.5em" my="0.2em" size="1.3em"/> {Client.username}
            </EllipsedText>
        )}
    >
        Click to copy {Client.username} to clipboard
    </Tooltip>
}

function ProjectID(): JSX.Element | null {
    const projectId = useProjectId();

    const project = useProject();

    const projectPath = joinToString(
        [...(project.fetch().status.path?.split("/")?.filter(it => it.length > 0) ?? []), project.fetch().specification.title],
        "/"
    );

    const copyProjectPath = useCallback(() => {
        copyToClipboard({value: projectPath, message: "Project copied to clipboard!"});
    }, [projectPath]);

    if (!projectId) return null;
    return <Tooltip
        trigger={
            <EllipsedText
                cursor="pointer"
                onClick={copyProjectPath}
                width="140px"
            >
                <Icon key={projectId} name={"projects"} color2="white" color="black" mr="0.5em" my="0.2em"
                      size="1.3em"/>{projectPath}
            </EllipsedText>
        }
    >
        Click to copy to clipboard
    </Tooltip>
}

function Downtimes(): JSX.Element | null {
    const [downtimes, fetchDowntimes] = useCloudAPI<Page<NewsPost>>({noop: true}, emptyPage);
    const [intervalId, setIntervalId] = React.useState(-1);

    React.useEffect(() => {
        setIntervalId(window.setInterval(() => fetchDowntimes({
            method: "GET", path: "/news/listDowntimes"
        }), 600_000));
        return () => {
            if (intervalId !== -1) clearInterval(intervalId);
        };
    }, []);

    const upcomingDowntime = downtimes.data.items.at(0)?.id ?? -1;

    if (upcomingDowntime === -1) return null;
    return <Link to={AppRoutes.news.detailed(upcomingDowntime)}>
        <Tooltip trigger={<Icon color="yellow" name="warning"/>}>
            Upcoming downtime.<br/>
            Click to view
        </Tooltip>
    </Link>
}

function copyUserName(): void {
    copyToClipboard({
        value: Client.username,
        message: "Username copied to clipboard"
    });
}

function useSidebarReduxProps(): SidebarStateProps {
    return useSelector((it: ReduxObject) => ({
        /* Used to ensure re-rendering of Sidebar after user logs in. */
        loggedIn: Client.isLoggedIn,

        /* Used to ensure re-rendering of Sidebar after project change. */
        activeProject: it.project.project,

        avatar: it.avatar
    }))
}
