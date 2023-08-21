import * as UCloud from "@/UCloud";
import Fuse from "fuse.js";
import {AppToolLogo} from "@/Applications/AppToolLogo";
import {Tag} from "@/Applications/Card";
import {useCloudAPI, useCloudCommand} from "@/Authentication/DataHook";
import {Client} from "@/Authentication/HttpClientInstance";
import {emptyPage} from "@/DefaultObjects";
import {dialogStore} from "@/Dialog/DialogStore";
import {MainContainer} from "@/MainContainer/MainContainer";
import {useCallback, useEffect} from "react";
import * as React from "react";
import {useState} from "react";
import {snackbarStore} from "@/Snackbar/SnackbarStore";
import {Button, Checkbox, DataList, Flex, Icon, Label, Text, VerticalButtonGroup} from "@/ui-components";
import Box from "@/ui-components/Box";
import ClickableDropdown from "@/ui-components/ClickableDropdown";
import * as Heading from "@/ui-components/Heading";
import Input, {HiddenInputField, InputLabel} from "@/ui-components/Input";
import Table, {TableCell, TableHeaderCell, TableRow} from "@/ui-components/Table";
import {addStandardDialog} from "@/UtilityComponents";
import {PropType, doNothing, stopPropagation} from "@/UtilityFunctions";
import {compute} from "@/UCloud";
import ApplicationSummaryWithFavorite = compute.ApplicationSummaryWithFavorite;
import {ApplicationGroup, clearLogo, listGroups, setGroup, uploadLogo} from "@/Applications/api";
import {useLoading, useTitle} from "@/Navigation/Redux/StatusActions";
import {usePrioritizedSearch} from "@/Utilities/SearchUtilities";
import {useRefreshFunction} from "@/Navigation/Redux/HeaderActions";
import {useParams} from "react-router";
import {ButtonClass} from "@/ui-components/Button";
import {injectStyle, injectStyleSimple} from "@/Unstyled";

interface AppVersion {
    version: string;
    isPublic: boolean;
}

enum AccessEntityType {
    USER = "USER",
    PROJECT_GROUP = "PROJECT_GROUP"
}

const entityTypes = [
    {text: prettifyEntityType(AccessEntityType.USER), value: AccessEntityType.USER},
    {text: prettifyEntityType(AccessEntityType.PROJECT_GROUP), value: AccessEntityType.PROJECT_GROUP},
];

type ApplicationAccessRight = PropType<UCloud.compute.DetailedEntityWithPermission, "permission">;

interface GroupSelectorProps {
    options: ApplicationGroup[];
    onSelect: (id: number) => void;
    placeholder: string;
    width?: number | string;
    clearOnSelect?: boolean;
    rightLabel?: boolean;
    leftLabel?: boolean;
}

const GroupSelectorTriggerClass = injectStyle("group-selector-trigger", k => `
    ${k} {
        display: flex;
        align-items: center;
        justify-content: space-between;
        cursor: pointer;
        font-family: inherit;
        color: var(--black);
        background-color: var(--inputColor);
        margin: 0;
        border-width: 0px;
        
        width: 100%;
        border-radius: 5px;
        padding: 7px 12px;
        height: 42px;
        box-shadow: inset 0 .0625em .125em rgba(10,10,10,.05);         

        --inputBorder: var(--blue);
        --inputDisabledBorder: var(--lightGray);

        border: 1px solid var(--midGray);
    }
    
    ${k}:hover {
        border-color: var(--gray);
    }
`);


const GroupSelectorClass = injectStyle("group-selector", k => `
    ${k} {

    }

    ${k} input {
        margin-bottom: 20px;
    }

    ${k} div {
        overflow-y: scroll;
        max-height: 300px;
        line-height: 2em;
    }

    ${k} div div:hover {
        background-color: var(--lightBlue);
        cursor: pointer;
    }
`);

const GroupSelector: React.FunctionComponent<GroupSelectorProps> = (props) => {
    const [selectedGroup, setSelectedGroup] = useState<ApplicationGroup>();
    return (
        <div
            className={GroupSelectorTriggerClass}
            onClick={() => dialogStore.addDialog(
                <FilterThingamagic options={props.options} setSelectedGroup={setSelectedGroup} />
            , doNothing, true)}
        >
            <Text>{selectedGroup ? selectedGroup.title : "Select group..."}</Text>
            <Icon name="chevronDownLight" ml="-32px" size={14} />
        </div >
    );
}

function FilterThingamagic(props: {options: ApplicationGroup[], setSelectedGroup: (appGroup: ApplicationGroup) => void}): JSX.Element {
    const [filter, setTitleFilter] = React.useState("");
    const results = React.useMemo(() =>
        props.options.filter(it => it.title.toLocaleLowerCase().includes(filter.toLocaleLowerCase()))
        , [props.options, filter]);
    return (
        <div className={GroupSelectorClass}>
            <Input
                placeholder="Enter group.."
                defaultValue={filter}
                type="text"
                autoFocus
                onChange={e => setTitleFilter("value" in (e.target) ? e.target.value as string : "")}
            />
            <div key={results.length}>
                {results.map((appGroup) => (
                    <Flex key={appGroup.id} justifyContent="space-between">
                        <Box onClick={() => {
                            props.setSelectedGroup(appGroup);
                            dialogStore.success();
                        }}>
                            {appGroup.title}
                        </Box>
                        <Button color="red" height="20px" width="20px">
                            <Icon size={14} name="trash" />
                        </Button>
                    </Flex>
                ))}
                {results.length === 0 ? <Box>No results</Box> : null}
                {filter.length > 0 ?
                    <div style={{textAlign: "center", marginTop: "20px"}}>
                        <Button>Create group</Button>
                    </div>
                : null}
            </div>
        </div>

    )

}


function prettifyAccessRight(accessRight: ApplicationAccessRight): "Can launch" {
    switch (accessRight) {
        case "LAUNCH":
            return "Can launch";
    }
}

function prettifyEntityType(entityType: AccessEntityType): string {
    switch (entityType) {
        case AccessEntityType.USER: {
            return "User";
        }
        case AccessEntityType.PROJECT_GROUP: {
            return "Project group";
        }
        default: {
            return "Unknown";
        }
    }
}

function LeftAlignedTableHeader(props: React.PropsWithChildren): JSX.Element {
    return <thead className={LeftAlignedTableHeaderClass}>
        {props.children}
    </thead>
}

const LeftAlignedTableHeaderClass = injectStyleSimple("table-header", `
    text-align: left;
`);

export const App: React.FunctionComponent = () => {
    const name = useParams<{name: string}>().name!;

    const [commandLoading, invokeCommand] = useCloudCommand();
    const [, setLogoCacheBust] = useState("" + Date.now());
    const [access, setAccess] = React.useState<ApplicationAccessRight>("LAUNCH");
    const [allTags, fetchAllTags] = useCloudAPI<string[]>(
        {noop: true},
        []
    );
    const [selectedTag, setSelectedTag] = useState<string>("");

    const [allGroups, setGroups] = useCloudAPI<ApplicationGroup[]>(
        {noop: true},
        []
    );
    const [selectedGroup, setSelectedGroup] = useState<number | null>();


    const [permissionEntries, fetchPermissionEntries] = useCloudAPI<UCloud.compute.DetailedEntityWithPermission[]>(
        {noop: true},
        []
    );

    const [apps, setAppParameters] = useCloudAPI<Page<ApplicationSummaryWithFavorite>>(
        UCloud.compute.apps.findByName({appName: name, itemsPerPage: 50, page: 0}),
        emptyPage
    );
    const [versions, setVersions] = useState<AppVersion[]>([]);
    const [selectedEntityType, setSelectedEntityType] = useState<AccessEntityType>(AccessEntityType.USER);

    const permissionLevels = [
        {text: prettifyAccessRight("LAUNCH"), value: "LAUNCH"}
    ];

    // Loading of permission entries
    useEffect(() => {
        fetchPermissionEntries(UCloud.compute.apps.listAcl({appName: name}));
        fetchAllTags(UCloud.compute.apps.listTags({}));
        setGroups(listGroups({}));
    }, [name]);

    useEffect(() => {
        if (!allGroups) return;

        if (apps.data.items[0]) {
            setSelectedGroup(apps.data.items[0].metadata.group?.title ?? undefined);
        }
    }, [allGroups, apps]);

    // Loading of application versions
    useEffect(() => {
        const appVersions: AppVersion[] = [];
        apps.data.items.forEach(item => {
            appVersions.push({version: item.metadata.version, isPublic: item.metadata.public});
        });
        setVersions(appVersions);
    }, [apps.data.items]);

    useTitle("Application Studio | Applications");
    usePrioritizedSearch("applications");

    const refresh = useCallback(() => {
        setAppParameters(UCloud.compute.apps.findByName({appName: name, itemsPerPage: 50, page: 0}));
        fetchAllTags(UCloud.compute.apps.listTags({}));
        setGroups(listGroups({}));
    }, [name]);

    useRefreshFunction(refresh);
    useLoading(commandLoading || apps.loading);

    const appTitle = apps.data.items.length > 0 ? apps.data.items[0].metadata.title : name;
    const tags = apps.data.items[0]?.tags ?? [];
    const userEntityField = React.useRef<HTMLInputElement>(null);
    const projectEntityField = React.useRef<HTMLInputElement>(null);
    const groupEntityField = React.useRef<HTMLInputElement>(null);

    if (Client.userRole !== "ADMIN") return null;
    return (
        <MainContainer
            header={(
                <Heading.h1>
                    <AppToolLogo name={name} type={"APPLICATION"} size={"64px"} />
                    {" "}
                    {appTitle}
                </Heading.h1>
            )}
            main={(<>
                <label className={ButtonClass}>
                    Upload Logo
                    <HiddenInputField
                        type="file"
                        onChange={async e => {
                            const target = e.target;
                            if (target.files) {
                                const file = target.files[0];
                                target.value = "";
                                if (file.size > 1024 * 512) {
                                    snackbarStore.addFailure("File exceeds 512KB. Not allowed.", false);
                                } else {
                                    if (await uploadLogo({name, file, type: "APPLICATION"})) {
                                        setLogoCacheBust("" + Date.now());
                                    }
                                }
                                dialogStore.success();
                            }
                        }}
                    />
                </label>

                <Button
                    type="button"
                    color="red"
                    disabled={commandLoading}
                    onClick={async () => {
                        await invokeCommand(clearLogo({type: "APPLICATION", name}));
                        setLogoCacheBust("" + Date.now());
                    }}
                >
                    Remove Logo
                </Button>

                <Flex flexDirection="column">
                    <Box maxWidth="800px" width="100%" ml="auto" mr="auto">
                        <Heading.h2>Tags</Heading.h2>
                        <Box mb={46} mt={26}>
                            {tags.map(tag => (
                                <Flex key={tag} mb={16}>
                                    <Box flexGrow={1}>
                                        <Tag key={tag} label={tag} />
                                    </Box>
                                    <Box>
                                        <Button
                                            color={"red"}
                                            type={"button"}

                                            disabled={commandLoading}
                                            onClick={async () => {
                                                await invokeCommand(UCloud.compute.apps.removeTag({
                                                    applicationName: name,
                                                    tags: [tag]
                                                }));
                                                refresh();
                                            }}
                                        >
                                            <Icon size={16} name="trash" />
                                        </Button>
                                    </Box>
                                </Flex>
                            ))}
                            <form
                                onSubmit={async e => {
                                    e.preventDefault();
                                    if (commandLoading) return;

                                    if (selectedTag === null) return;
                                    if (selectedTag === "") return;

                                    await invokeCommand(UCloud.compute.apps.createTag({
                                        applicationName: name,
                                        tags: [selectedTag]
                                    }));

                                    refresh();
                                }}
                            >
                                <Flex>
                                    <Box flexGrow={1}>
                                        {allTags.data.length > 0 ?
                                            <DataList
                                                rightLabel
                                                options={allTags.data.map(tag => ({value: tag, content: tag}))}
                                                onSelect={item => setSelectedTag(item)}
                                                onChange={item => setSelectedTag(item)}
                                                placeholder={"Enter or choose a tag..."}
                                            />
                                            : <></>
                                        }
                                    </Box>
                                    <Button disabled={commandLoading} type="submit" width={100} attached>
                                        Add tag
                                    </Button>
                                </Flex>
                            </form>
                        </Box>
                    </Box>
                    <Box maxWidth="800px" width="100%" ml="auto" mr="auto" mt="25px">
                        <form
                            onSubmit={async e => {
                                e.preventDefault();
                                if (commandLoading) return;

                                if (!selectedGroup) return;

                                /*await invokeCommand(setGroup({
                                    groupName: selectedGroup,
                                    applicationName: name
                                }));*/

                                snackbarStore.addSuccess(`Added to group ${selectedGroup}`, false);

                                refresh();
                            }}
                        >
                            <Heading.h2>Group</Heading.h2>
                            <Flex>
                                <GroupSelector
                                    rightLabel
                                    options={allGroups.data}
                                    onSelect={item => setSelectedGroup(item)}
                                    placeholder={allGroups.data.find(it => it.id === selectedGroup)?.title ?? "Select group..."}
                                />
                            </Flex>
                        </form>
                    </Box>
                    <Box maxWidth="800px" width="100%" ml="auto" mr="auto" mt="25px">
                        <Heading.h2>Permissions</Heading.h2>
                        <Box mt={16}>
                            <form
                                onSubmit={async e => {
                                    e.preventDefault();
                                    if (commandLoading) return;

                                    if (selectedEntityType === AccessEntityType.USER) {
                                        const userField = userEntityField.current;
                                        if (userField === null) return;

                                        const userValue = userField.value;
                                        if (userValue === "") return;

                                        await invokeCommand(UCloud.compute.apps.updateAcl({
                                            applicationName: name,
                                            changes: [{
                                                entity: {user: userValue},
                                                rights: access,
                                                revoke: false
                                            }]
                                        }));
                                        fetchPermissionEntries(UCloud.compute.apps.listAcl({appName: name}));
                                        userField.value = "";
                                    } else if (selectedEntityType === AccessEntityType.PROJECT_GROUP) {
                                        const projectField = projectEntityField.current;
                                        if (projectField === null) return;

                                        const projectValue = projectField.value;
                                        if (projectValue === "") return;

                                        const groupField = groupEntityField.current;
                                        if (groupField === null) return;

                                        const groupValue = groupField.value;
                                        if (groupValue === "") return;

                                        await invokeCommand(UCloud.compute.apps.updateAcl({
                                            applicationName: name,
                                            changes: [
                                                {
                                                    entity: {project: projectValue, group: groupValue},
                                                    rights: access,
                                                    revoke: false
                                                }
                                            ]
                                        }));
                                        fetchPermissionEntries(UCloud.compute.apps.listAcl({appName: name}));
                                        projectField.value = "";
                                        groupField.value = "";
                                    }
                                }}
                            >
                                <Flex height={45}>
                                    <InputLabel width={350} leftLabel>
                                        <ClickableDropdown
                                            chevron
                                            width="180px"
                                            onChange={(val: AccessEntityType) => setSelectedEntityType(val)}
                                            trigger={
                                                <span style={{minWidth: "200px"}}>
                                                    {prettifyEntityType(selectedEntityType)}
                                                </span>
                                            }
                                            options={entityTypes}
                                        />
                                    </InputLabel>
                                    {selectedEntityType === AccessEntityType.USER ? (
                                        <Input
                                            rightLabel
                                            leftLabel
                                            required
                                            type="text"
                                            inputRef={userEntityField}
                                            placeholder="Username"
                                        />
                                    ) : (
                                        <>
                                            <Input
                                                leftLabel
                                                rightLabel
                                                required
                                                width={180}
                                                type="text"
                                                inputRef={projectEntityField}
                                                placeholder="Project name"
                                            />
                                            <Input
                                                leftLabel
                                                rightLabel
                                                required
                                                width={180}
                                                type="text"
                                                inputRef={groupEntityField}
                                                placeholder="Group name"
                                            />
                                        </>
                                    )}
                                    <InputLabel width={300} rightLabel leftLabel>
                                        <ClickableDropdown
                                            chevron
                                            width="180px"
                                            onChange={(val: ApplicationAccessRight) => setAccess(val)}
                                            trigger={<span style={{minWidth: "250px"}}>{prettifyAccessRight(access)}</span>}
                                            options={permissionLevels}
                                        />
                                    </InputLabel>
                                    <Button attached width="300px" disabled={commandLoading} type={"submit"}>Add
                                        permission</Button>
                                </Flex>
                            </form>
                        </Box>
                        <Flex key={5} mb={16} mt={26}>
                            <Box width={800}>
                                {(permissionEntries.data.length > 0) ? (
                                    <Table>
                                        <LeftAlignedTableHeader>
                                            <TableRow>
                                                <TableHeaderCell width="300px">Name</TableHeaderCell>
                                                <TableHeaderCell>Permission</TableHeaderCell>
                                                <TableHeaderCell width={50}>Delete</TableHeaderCell>
                                            </TableRow>
                                        </LeftAlignedTableHeader>
                                        <tbody>
                                            {permissionEntries.data.map((permissionEntry, index) => (
                                                <TableRow key={index}>
                                                    <TableCell>
                                                        {(permissionEntry.entity.user) ? (
                                                            permissionEntry.entity.user
                                                        ) : (
                                                            `${permissionEntry.entity.project?.title} / ${permissionEntry.entity.group?.title}`
                                                        )}</TableCell>
                                                    <TableCell>{prettifyAccessRight(permissionEntry.permission)}</TableCell>
                                                    <TableCell textAlign="right">
                                                        <Button
                                                            color={"red"}
                                                            type={"button"}
                                                            onClick={() => addStandardDialog({
                                                                title: `Are you sure?`,
                                                                message: (
                                                                    <Box>
                                                                        <Text>
                                                                            Remove permission
                                                                            for {(permissionEntry.entity.user) ? (
                                                                                permissionEntry.entity.user
                                                                            ) : (
                                                                                `${permissionEntry.entity.project?.title} / ${permissionEntry.entity.group?.title}`
                                                                            )}
                                                                        </Text>
                                                                    </Box>
                                                                ),
                                                                onConfirm: async () => {
                                                                    await invokeCommand(UCloud.compute.apps.updateAcl({
                                                                        applicationName: name,
                                                                        changes: [
                                                                            {
                                                                                entity: {
                                                                                    user: permissionEntry.entity.user,
                                                                                    project: permissionEntry.entity.project?.id,
                                                                                    group: permissionEntry.entity.group?.id
                                                                                },
                                                                                rights: permissionEntry.permission,
                                                                                revoke: true
                                                                            }
                                                                        ]
                                                                    }));
                                                                    fetchPermissionEntries(UCloud.compute.apps.listAcl({appName: name}));
                                                                }
                                                            })}
                                                        >
                                                            <Icon size={16} name="trash" />
                                                        </Button>
                                                    </TableCell>
                                                </TableRow>
                                            ))}
                                        </tbody>
                                    </Table>
                                ) : (
                                    <Text textAlign="center">No explicit permissions set for this application</Text>
                                )}
                            </Box>
                        </Flex>
                    </Box>
                    <Box maxWidth="800px" width="100%" ml="auto" mr="auto" mt="25px">
                        <Heading.h2>Versions</Heading.h2>
                        <Box mb={26} mt={26}>
                            <Table>
                                <LeftAlignedTableHeader>
                                    <TableRow>
                                        <TableHeaderCell width={100}>Version</TableHeaderCell>
                                        <TableHeaderCell>Settings</TableHeaderCell>
                                    </TableRow>
                                </LeftAlignedTableHeader>
                                <tbody>
                                    {versions.map(version => (
                                        <TableRow key={version.version}>
                                            <TableCell>
                                                <div className={WordBreakDivClass}>
                                                    {version.version}
                                                </div>
                                            </TableCell>
                                            <TableCell>
                                                <Box mb={26} mt={16}>
                                                    <Label>
                                                        <Flex>
                                                            <Checkbox
                                                                checked={version.isPublic}
                                                                onChange={stopPropagation}
                                                                onClick={() => {
                                                                    Client.post(`/hpc/apps/setPublic`, {
                                                                        appName: name,
                                                                        appVersion: version.version,
                                                                        public: !version.isPublic
                                                                    });

                                                                    setVersions(versions.map(v =>
                                                                        (v.version === version.version) ?
                                                                            {
                                                                                version: v.version,
                                                                                isPublic: !v.isPublic
                                                                            } : v
                                                                    ));
                                                                }}
                                                            />
                                                            <Box ml={8} mt="2px">Public</Box>
                                                        </Flex>
                                                    </Label>
                                                    {version.isPublic ? (
                                                        <Box ml={28}>Everyone can see and launch this version
                                                            of {appTitle}.</Box>
                                                    ) : (
                                                        <Box ml={28}>Access to this version is restricted as defined in
                                                            Permissions.</Box>
                                                    )}
                                                </Box>
                                            </TableCell>
                                        </TableRow>
                                    ))}
                                </tbody>
                            </Table>
                        </Box>
                    </Box>
                </Flex>
            </>)}
        />
    );
};

const WordBreakDivClass = injectStyle("work-break", k => `
    word-break: break-word;
    width: 100%;
`);

export default App;
