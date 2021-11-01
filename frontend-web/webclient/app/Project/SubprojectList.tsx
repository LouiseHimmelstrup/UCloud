import MainContainer from "@/MainContainer/MainContainer";
import * as React from "react";
import {useTitle} from "@/Navigation/Redux/StatusActions";
import {getQueryParamOrElse} from "@/Utilities/URIUtilities";
import {useHistory, useLocation} from "react-router";
import {Button, Flex, Icon, Input, Text, Tooltip} from "@/ui-components";
import * as Heading from "@/ui-components/Heading";
import {createProject, setProjectArchiveStatus, listSubprojects, renameProject, MemberInProject, ProjectRole, projectRoleToStringIcon, projectRoleToString} from ".";
import List, {ListRow, ListRowStat} from "@/ui-components/List";
import {errorMessageOrDefault, preventDefault, stopPropagationAndPreventDefault} from "@/UtilityFunctions";
import {Operations, Operation} from "@/ui-components/Operation";
import {ItemRenderer, ItemRow, StandardBrowse} from "@/ui-components/Browse";
import {useToggleSet} from "@/Utilities/ToggleSet";
import {useCloudCommand} from "@/Authentication/DataHook";
import {snackbarStore} from "@/Snackbar/SnackbarStore";
import {History} from "history";
import {BrowseType} from "@/Resource/BrowseType";
import {isAdminOrPI} from "@/Utilities/ProjectUtilities";
import {useDispatch} from "react-redux";
import {dispatchSetProjectAction} from "./Redux";

type ProjectOperation = Operation<MemberInProject, {
    startCreation: () => void;
    onSetArchivedStatus: (id: string, archive: boolean) => void;
    startRename: (id: string) => void;
    history: History;
}>;

const subprojectsRenderer: ItemRenderer<MemberInProject> = {
    MainTitle({resource}) {
        if (!resource) return null;
        return <Text>{resource.project.title}</Text>;
    },
    Icon() {
        return <Icon color={"iconColor"} color2={"iconColor2"} name="projects" />;
    },
    ImportantStats({resource}) {
        if (!resource) return null;
        return <>
            {resource.project.archived ? <Icon name="tags" /> : null}
            {resource.role ? <Tooltip
                tooltipContentWidth="80px"
                wrapperOffsetLeft="0"
                wrapperOffsetTop="4px"
                right="0"
                top="1"
                mb="50px"
                trigger={(
                    <Icon
                        size="30"
                        squared={false}
                        name={projectRoleToStringIcon(resource.role)}
                        color="gray"
                        color2="midGray"
                        mr=".5em"
                    />
                )}
            >
                <Text fontSize={2}>{projectRoleToString(resource.role)}</Text>
            </Tooltip> : null}
        </>
    },
    Stats({resource}) {
        return <ListRowStat>{resource?.project.fullPath}</ListRowStat>;
    }
}

const projectOperations: ProjectOperation[] = [
    {
        enabled: () => true,
        onClick: (_, extra) => extra.startCreation(),
        text: "Create subproject",
        canAppearInLocation: loc => loc === "SIDEBAR",
        color: "blue",
        primary: true
    },
    {
        enabled: (selected) => selected.length === 1 && isAdminOrPI(selected[0].role ?? ProjectRole.USER),
        onClick: ([{project}], extra) => extra.history.push(`/subprojects/?subproject=${project.id}`),
        text: "View subprojects",
        icon: "projects",
    },
    {
        enabled: (selected) => selected.length === 1 && !selected[0].project.archived && isAdminOrPI(selected[0].role ?? ProjectRole.USER),
        onClick: ([{project}], extras) => extras.onSetArchivedStatus(project.id, true),
        text: "Archive",
        icon: "tags"
    },
    {
        enabled: (selected) => selected.length === 1 && selected[0].project.archived && isAdminOrPI(selected[0].role ?? ProjectRole.USER),
        onClick: ([{project}], extras) => extras.onSetArchivedStatus(project.id, false),
        text: "Unarchive",
        icon: "tags"
    },
    {
        enabled: (selected) => selected.length === 1 && isAdminOrPI(selected[0].role ?? ProjectRole.USER),
        onClick: ([{project}], extras) => extras.startRename(project.id),
        text: "Rename",
        icon: "rename"
    }
];

export default function SubprojectList(): JSX.Element | null {
    useTitle("Subproject");
    const location = useLocation();
    const subprojectFromQuery = getQueryParamOrElse(location.search, "subproject", "");
    const history = useHistory();

    const dispatch = useDispatch();
    const setProject = React.useCallback((id: string) => dispatchSetProjectAction(dispatch, id), [dispatch]);

    const [, invokeCommand,] = useCloudCommand();

    const [creating, setCreating] = React.useState(false);
    const [renameId, setRenameId] = React.useState("");
    const reloadRef = React.useRef<() => void>(() => undefined);
    const creationRef = React.useRef<HTMLInputElement>(null);
    const renameRef = React.useRef<HTMLInputElement>(null);

    const startCreation = React.useCallback(() => {
        setCreating(true);
    }, []);

    const toggleSet = useToggleSet<MemberInProject>([]);

    const onCreate = React.useCallback(async () => {
        setCreating(false);
        const subprojectName = creationRef.current?.value ?? "";
        if (!subprojectName) {
            snackbarStore.addFailure("Invalid subproject name", false);
            return;
        }
        try {
            await invokeCommand(createProject({
                title: subprojectName,
                parent: subprojectFromQuery
            }));
            reloadRef.current();
            toggleSet.uncheckAll();
        } catch (e) {
            snackbarStore.addFailure(errorMessageOrDefault(e, "Invalid subproject name"), false);
        }
    }, [creationRef, subprojectFromQuery]);

    const onRenameProject = React.useCallback(async (id: string) => {
        const newProjectName = renameRef.current?.value;
        if (!newProjectName) {
            snackbarStore.addFailure("Invalid subproject name", false);
            return;
        }
        try {
            await invokeCommand(renameProject({id, newTitle: newProjectName}));
            reloadRef.current();
            toggleSet.uncheckAll();
            setRenameId("");
        } catch (e) {
            snackbarStore.addFailure(errorMessageOrDefault(e, "Invalid subproject name"), false);

        }
    }, [subprojectFromQuery]);

    const onSetArchivedStatus = React.useCallback(async (id: string, archive: boolean) => {
        try {
            await invokeCommand({
                ...setProjectArchiveStatus({archiveStatus: archive}),
                projectOverride: id
            });
            toggleSet.uncheckAll();
            reloadRef.current();
        } catch (e) {
            snackbarStore.addFailure(errorMessageOrDefault(e, "Invalid subproject name"), false);
        }
    }, []);


    const generateCall = React.useCallback((next?: string) => {
        setCreating(false);
        return ({
            ...listSubprojects({
                itemsPerPage: 50,
                next,
            }),
            projectOverride: subprojectFromQuery
        })
    }, [subprojectFromQuery]);

    const extra = {
        startCreation,
        history,
        onSetArchivedStatus,
        startRename: setRenameId
    };

    return <MainContainer
        main={
            !subprojectFromQuery ? <Text fontSize={"24px"}>Missing subproject</Text> :
            <>
            <Heading.h3 mb={16}>Subprojects</Heading.h3>
                <StandardBrowse
                    reloadRef={reloadRef}
                    generateCall={generateCall}
                    pageRenderer={pageRenderer}
                    toggleSet={toggleSet}
                />
            </>
        }
        sidebar={
            <Operations
                location="SIDEBAR"
                operations={projectOperations}
                selected={toggleSet.checked.items}
                extra={extra}
                entityNameSingular={"Subproject"}
            />
        }
    />;

    function pageRenderer(items: MemberInProject[]): JSX.Element {
        if (items.length === 0) {
            return <>
                <Text fontSize="24px" key="no-entries">No subprojects found for project.</Text>
                {creating ?
                    <List>
                        <ListRow
                            left={
                                <form onSubmit={e => {stopPropagationAndPreventDefault(e); onCreate();}}>
                                    <Flex height="56px">
                                        <Button height="36px" mt="8px" color="green" type="submit">Create</Button>
                                        <Button height="36px" mt="8px" color="red" type="button" onClick={() => setCreating(false)}>Cancel</Button>
                                        <Input noBorder placeholder="Project name..." ref={creationRef} />
                                    </Flex>
                                </form>
                            }
                            right={null}
                        />
                    </List> : null}
            </>;
        }
        return (
            <List bordered onContextMenu={preventDefault}>
                {creating ?
                    <ListRow
                        left={
                            <form onSubmit={e => {stopPropagationAndPreventDefault(e); onCreate();}}>
                                <Flex height="56px">
                                    <Button height="36px" mt="8px" color="green" type="submit">Create</Button>
                                    <Button height="36px" mt="8px" color="red" type="button" onClick={() => setCreating(false)}>Cancel</Button>
                                    <Input noBorder placeholder="Project name..." ref={creationRef} />
                                </Flex>
                            </form>
                        }
                        right={null}
                    /> : null}
                {items.map((it) => it.project.id === renameId ? (
                    <form key={it.project.id} onSubmit={e => {stopPropagationAndPreventDefault(e); onRenameProject(it.project.id)}}>
                        <Flex height="56px">
                            <Button height="36px" mt="8px" color="green" type="submit">Create</Button>
                            <Button height="36px" mt="8px" color="red" type="button" onClick={() => setRenameId("")}>Cancel</Button>
                            <Input noBorder placeholder="Project name..." defaultValue={it.project.title} ref={renameRef} />
                        </Flex>
                    </form>
                ) : (
                    <ItemRow
                        key={it.project.id}
                        item={it}
                        browseType={BrowseType.MainContent}
                        renderer={subprojectsRenderer}
                        toggleSet={toggleSet}
                        operations={projectOperations}
                        callbacks={extra}
                        itemTitle={"Subproject"}
                    />
                ))}
            </List>
        )
    }
}
