import * as React from "react";
import {useCallback, useEffect, useMemo, useRef, useState} from "react";
import {MainContainer} from "@/MainContainer/MainContainer";
import {ProjectBreadcrumbs} from "@/Project/Breadcrumbs";
import * as Heading from "@/ui-components/Heading";
import Box from "@/ui-components/Box";
import Button from "@/ui-components/Button";
import ButtonGroup from "@/ui-components/ButtonGroup";
import Card from "@/ui-components/Card";
import ExternalLink from "@/ui-components/ExternalLink";
import Flex from "@/ui-components/Flex";
import Icon, {IconName} from "@/ui-components/Icon";
import Input, {HiddenInputField} from "@/ui-components/Input";
import Label from "@/ui-components/Label";
import List from "@/ui-components/List";
import Checkbox from "@/ui-components/Checkbox";
import Text from "@/ui-components/Text";
import TextArea from "@/ui-components/TextArea";
import {ThemeColor} from "@/ui-components/theme";
import Tooltip from "@/ui-components/Tooltip";
import {useCloudAPI, useCloudCommand} from "@/Authentication/DataHook";
import {
    ProductCategoryId,
    ProductMetadata,
    Product,
    productCategoryEquals,
    usageExplainer,
    productTypeToIcon,
    productTypeToTitle,
    productTypes,
    explainAllocation,
    normalizeBalanceForBackend,
    browseWallets,
    Wallet,
    WalletAllocation,
    normalizeBalanceForFrontend
} from "@/Accounting";
import styled from "styled-components";
import HighlightedCard from "@/ui-components/HighlightedCard";
import {
    GrantsRetrieveAffiliationsResponse,
    isGrantFinalized,
    retrieveDescription,
    RetrieveDescriptionResponse,
} from "@/Project/Grant/index";
import {useHistory, useParams} from "react-router";
// import {Client} from "@/Authentication/HttpClientInstance";
import {dateToString, getStartOfDay} from "@/Utilities/DateUtilities";
import {UserAvatar} from "@/AvataaarLib/UserAvatar";
import {AvatarType, defaultAvatar} from "@/UserSettings/Avataaar";
import Table, {TableCell, TableHeader, TableHeaderCell, TableRow} from "@/ui-components/Table";
import {addStandardDialog} from "@/UtilityComponents";
import {setLoading, useTitle} from "@/Navigation/Redux/StatusActions";
import {useDispatch} from "react-redux";
import * as UCloud from "@/UCloud";
import grantApi = UCloud.grant.grant;
import ProjectWithTitle = UCloud.grant.ProjectWithTitle;
import ClickableDropdown from "@/ui-components/ClickableDropdown";
import {TextSpan} from "@/ui-components/Text";
import {default as ReactModal} from "react-modal";
import {defaultModalStyle} from "@/Utilities/ModalUtilities";
import {emptyPage, emptyPageV2} from "@/DefaultObjects";
import {Spacer} from "@/ui-components/Spacer";
import {ConfirmationButton} from "@/ui-components/ConfirmationAction";
import {Truncate} from "@/ui-components";
import {snackbarStore} from "@/Snackbar/SnackbarStore";
import {Logo} from "./ProjectBrowser";
import {format} from "date-fns";
import {DatePicker} from "@/ui-components/DatePicker";

import {
    GrantApplication,
    AllocationRequest,
    Comment,
    State,
    deleteGrantApplicationComment,
    Recipient,
    findAffiliations,
    commentOnGrantApplication,
    Document,
    rejectGrantApplication,
    approveGrantApplication,
    transferApplication,
    Client,
    GrantProductCategory,
    fetchGrantApplicationFake,
    fetchGrantGiversFake,
    FetchGrantApplicationRequest,
    FetchGrantApplicationResponse
} from "./GrantApplicationTypes";
import {useAvatars} from "@/AvataaarLib/hook";
import {listProjects, ProjectRole, useProjectManagementStatus, UserInProject, userProjectStatus, viewProject} from "..";

export enum RequestTarget {
    EXISTING_PROJECT = "existing",
    NEW_PROJECT = "new",
    PERSONAL_PROJECT = "personal",
    VIEW_APPLICATION = "view"
}

export const RequestForSingleResourceWrapper = styled.div`
    ${Icon} {
        float: right;
        margin-left: 10px;
    }

    ${Card} {
        height: 100%;

        .dashboard-card-inner {
            padding: 16px;
        }
    }

    table {
        margin: 16px;
    }

    th {
        width: 100%;
        text-align: left;
        padding-right: 30px
    }

    td {
        margin-left: 10px;
        padding-bottom: 16px;
        min-width: 350px;
    }

    tr {
        vertical-align: top;
        height: 40px;
    }

    .unit {
        flex-shrink: 0;
        margin-left: 10px;
        width: 55px;
    }
`;

const ResourceContainer = styled.div`
    display: grid;
    grid-gap: 32px;
    grid-template-columns: repeat(auto-fit, minmax(500px, auto));
    margin: 32px 0;
`;

const RequestFormContainer = styled.div`
    width: 100%;

    ${TextArea} {
        width: 100%;
        height: calc(100% - 40px);
        margin: 10px 0;
    }
`;

function productCategoryId(pid: ProductCategoryId): string {
    return `${pid.name}/${pid.provider}`;
}

function productCategoryStartDate(pid: ProductCategoryId): string {
    return `${productCategoryId(pid)}/start_date`;
}

function productCategoryEndDate(pid: ProductCategoryId): string {
    return `${productCategoryId(pid)}/end_date`;
}

function productCategoryAllocation(pid: ProductCategoryId): string {
    return `${productCategoryId(pid)}/allocation_id`;
}

function parseIntegerFromInput(input?: HTMLInputElement | null): number | undefined {
    if (!input) return undefined;
    const rawValue = input.value;
    const parsed = parseInt(rawValue, 10);
    if (isNaN(parsed)) return undefined;
    return parsed;
}

const milleniumAndCenturyPrefix = "20"

function parseDateFromInput(input?: HTMLInputElement | null): number | undefined {
    if (!input) return undefined;
    const rawValue = input.value;
    const [day, month, year] = rawValue.split("/");
    const d = getStartOfDay(new Date());
    d.setDate(parseInt(day, 10));
    d.setMonth(parseInt(month, 10) - 1);
    d.setFullYear(parseInt(`${milleniumAndCenturyPrefix}${year}`, 10));
    const time = d.getTime();
    if (isNaN(time)) return undefined;
    return time;
}

/* FIXME(Jonas): Copy + pasted from elsewhere (MachineType dropdown) */
const Wrapper = styled.div`
    & > table {
        margin-left: -9px;
    }

    & > table > tbody > ${TableRow}:hover {
        cursor: pointer;
        background-color: var(--lightGray, #f00);
        color: var(--black, #f00);
    }
`;

const GenericRequestCard: React.FunctionComponent<{
    wb: GrantProductCategory;
    grantFinalized: boolean;
    isLocked: boolean;
    showAllocationSelection: boolean;
    wallets: Wallet[];
    allocationRequests: AllocationRequest[];
}> = ({wb, grantFinalized, isLocked, wallets, showAllocationSelection, allocationRequests}) => {
    const [startDate, setStartDate] = useState<Date>(getStartOfDay(new Date()));
    const [endDate, setEndDate] = useState<Date | null>(null);

    if (allocationRequests.length !== 0) console.log(allocationRequests);

    if (wb.metadata.freeToUse) {
        return <RequestForSingleResourceWrapper>
            <HighlightedCard color="blue" isLoading={false}>
                <Flex flexDirection="row" alignItems="center">
                    <Box flexGrow={1}>
                        <Label>
                            <Checkbox
                                size={32}
                                defaultChecked={wb.requestedBalance !== undefined && wb.requestedBalance > 0}
                                disabled={grantFinalized || isLocked}
                                data-target={"checkbox-" + productCategoryId(wb.metadata.category)}
                                onChange={e => {
                                    const checkbox = e.target as HTMLInputElement;
                                    const input = document.querySelector(
                                        `input[data-target="${productCategoryId(wb.metadata.category)}"]`
                                    ) as HTMLInputElement;

                                    if (input) {
                                        const wasChecked = input.value === "1";
                                        input.value = wasChecked ? "0" : "1";
                                        checkbox.checked = !wasChecked;
                                    }
                                }}
                            />
                            {wb.metadata.category.provider} / {wb.metadata.category.name}
                        </Label>
                    </Box>
                    <Icon name={productTypeToIcon(wb.metadata.productType)} size={40} />
                </Flex>

                <Input
                    disabled={grantFinalized || isLocked}
                    data-target={productCategoryId(wb.metadata.category)}
                    autoComplete="off"
                    type="hidden"
                    min={0}
                />
            </HighlightedCard>
        </RequestForSingleResourceWrapper>
    } else {
        const defaultValue = allocationRequests.find(it => it.provider === wb.metadata.category.provider && it.category === wb.metadata.category.name)?.balanceRequested;
        // TODO(Jonas): This is probably not correct
        const isPrice = false;
        const normalizedValue = defaultValue != null ? normalizeBalanceForFrontend(defaultValue, wb.metadata.productType, wb.metadata.chargeType, wb.metadata.unitOfPrice, isPrice) : undefined;
        return <RequestForSingleResourceWrapper>
            <HighlightedCard color="blue" isLoading={false}>
                <table>
                    <tbody>
                        <tr>
                            <th>Product</th>
                            <td>
                                {wb.metadata.category.provider} / {wb.metadata.category.name}
                                <Icon name={productTypeToIcon(wb.metadata.productType)} size={40} />
                            </td>
                        </tr>
                        {wb.currentBalance === undefined ? null : (
                            <tr>
                                <th>Current balance</th>
                                <td>
                                    {usageExplainer(wb.currentBalance, wb.metadata.productType, wb.metadata.chargeType,
                                        wb.metadata.unitOfPrice)}
                                </td>
                            </tr>
                        )}
                        <tr>
                            <th>
                                {wb.metadata.chargeType === "ABSOLUTE" ?
                                    <>Balance requested</> :
                                    <>Quota requested</>
                                }
                            </th>
                            <td>
                                <Flex alignItems={"center"}>
                                    <Input
                                        placeholder={"0"}
                                        disabled={grantFinalized || isLocked}
                                        data-target={productCategoryId(wb.metadata.category)}
                                        autoComplete="off"
                                        defaultValue={normalizedValue}
                                        type="number"
                                        min={0}
                                    />
                                    <div className={"unit"}>
                                        {explainAllocation(wb.metadata.productType, wb.metadata.chargeType,
                                            wb.metadata.unitOfPrice)}
                                    </div>
                                </Flex>
                                <Flex mt="6px" width="280px">
                                    <DatePicker
                                        selectsStart
                                        startDate={startDate}
                                        endDate={endDate}
                                        py="8px"
                                        borderWidth="2px"
                                        required={false /* TODO state.approver */}
                                        disabled={grantFinalized || isLocked}
                                        mr="3px"
                                        backgroundColor={isLocked ? "var(--lightGray)" : undefined}
                                        placeholderText="Start date..."
                                        value={format(startDate, "dd/MM/yy")}
                                        onChange={(date: Date | null) => setStartDate(date!)}
                                        className={productCategoryStartDate(wb.metadata.category)}
                                    />
                                    <DatePicker
                                        selectsEnd
                                        isClearable={endDate != null}
                                        borderWidth="2px"
                                        py="8px"
                                        ml="3px"
                                        backgroundColor={isLocked ? "var(--lightGray)" : undefined}
                                        placeholderText={isLocked ? undefined : "End date..."}
                                        value={endDate ? format(endDate, "dd/MM/yy") : undefined}
                                        startDate={startDate}
                                        disabled={grantFinalized || isLocked}
                                        endDate={endDate}
                                        onChange={(date: Date | null) => setEndDate(date)}
                                        className={productCategoryEndDate(wb.metadata.category)}
                                    />
                                </Flex>
                            </td>
                        </tr>
                    </tbody>
                </table>
                {showAllocationSelection ?
                    <AllocationSelection wallets={wallets} wb={wb} isLocked={isLocked || !isApprover} />
                    : null
                }
            </HighlightedCard>
        </RequestForSingleResourceWrapper>;
    }
};

async function fetchGrantGivers() {
    // const resp = await callAPI();
    return await new Promise(resolve => setTimeout(() => resolve(fetchGrantGiversFake()), 250));
}

export async function fetchGrantApplication(request: FetchGrantApplicationRequest): Promise<FetchGrantApplicationResponse> {
    // const resp = await callAPI();
    return await new Promise(resolve => setTimeout(() => resolve(fetchGrantApplicationFake(request)), 250));
}

function AllocationSelection({wallets, wb, isLocked}: {
    wallets: Wallet[];
    wb: GrantProductCategory;
    isLocked: boolean;
}): JSX.Element {
    const [allocation, setAllocation] = useState<{wallet: Wallet; allocation: WalletAllocation;} | undefined>(undefined);
    return <div>
        <HiddenInputField value={allocation ? allocation.allocation.id : ""} onChange={() => undefined} data-target={productCategoryAllocation(wb.metadata.category)} />
        {!isLocked ?
            <ClickableDropdown width={"660px"} useMousePositioning chevron trigger={!allocation ? "No allocation selected" : `${allocation.wallet.paysFor.provider} @ ${allocation.wallet.paysFor.name}`}>
                <Table>
                    <TableHeader>
                        <TableHeaderCell width="200px">Provider</TableHeaderCell>
                        <TableHeaderCell width="200px">Name</TableHeaderCell>
                        <TableHeaderCell width="200px">Balance</TableHeaderCell>
                        <TableHeaderCell width="45px">ID</TableHeaderCell>
                    </TableHeader>
                    <tbody>
                        {wallets.map(w =>
                            <AllocationRows
                                key={w.paysFor.provider + w.paysFor.name + w.paysFor.title}
                                wallet={w}
                                onClick={(wallet, allocation) => setAllocation({wallet, allocation})}
                            />
                        )}
                    </tbody>
                </Table>
            </ClickableDropdown> : (allocation ? `${allocation.wallet.paysFor.provider} @ ${allocation.wallet.paysFor.name}` : "No allocation selected")}
    </div>;
}

function AllocationRows({wallet, onClick}: {onClick(wallet: Wallet, allocation: WalletAllocation): void; wallet: Wallet;}) {
    return <>
        {wallet.allocations.map(a =>
            <TableRow onClick={() => onClick(wallet, a)}>
                <TableCell width="200px">{wallet.paysFor.provider}</TableCell>
                <TableCell width="200px">{wallet.paysFor.name}</TableCell>
                <TableCell width="200px">{a.localBalance}</TableCell>
                <TableCell width="45px">{a.id}</TableCell>
            </TableRow>
        )}
    </>
}

function titleFromTarget(target: RequestTarget): string {
    console.log("titleFromTarget");
    switch (target) {
        case RequestTarget.EXISTING_PROJECT:
            return "Viewing Project";
        case RequestTarget.NEW_PROJECT:
            return "Create Project";
        case RequestTarget.PERSONAL_PROJECT:
            return "My Workspace";
        case RequestTarget.VIEW_APPLICATION:
            return "Viewing Application";
    }
}

// Note: target is baked into the component to make sure we follow the rules of hooks.
//
// We need to take wildly different paths depending on the target which causes us to use very different hooks. Baking
// the target property ensures a remount of the component.
export const GrantApplicationEditor: (target: RequestTarget) =>
    React.FunctionComponent = target => function MemoizedEditor() {
        const [loading, runWork] = useCloudCommand();
        const projectTitleRef = useRef<HTMLInputElement>(null);
        const projectReferenceIdRef = useRef<HTMLInputElement>(null);
        useTitle(titleFromTarget(target));
        const avatars = useAvatars();

        const [grantGivers, setGrantGivers] = useState<UCloud.PageV2<ProjectWithTitle>>(emptyPageV2);
        React.useEffect(() => {
            fetchGrantGivers().then((page: UCloud.PageV2<ProjectWithTitle>) => setGrantGivers(page));
        }, []);

        const {appId} = useParams<{appId?: string}>();

        const documentRef = useRef<HTMLTextAreaElement>(null);

        const [wallets, fetchWallets] = useCloudAPI<UCloud.PageV2<Wallet>>(
            browseWallets({itemsPerPage: 250}), emptyPageV2
        );

        const [grantApplication, setGrant] = useState<GrantApplication>({
            createdAt: 0,
            updatedAt: 0,
            createdBy: "unknown",
            currentRevision: {
                createdAt: 0,
                updatedBy: "",
                revisionNumber: 0,
                document: {
                    allocationRequests: [],
                    form: "",
                    recipient: {
                        username: "",
                        type: "personal_workspace"
                    },
                    referenceId: "",
                    revisionComment: "",
                }
            },
            id: "0",
            status: {
                overallState: State.PENDING,
                comments: [],
                revisions: [],
                stateBreakdown: []
            }
        });

        React.useEffect(() => {
            if (documentRef.current) {
                documentRef.current.value = grantApplication.currentRevision.document.form;
            }
        }, [grantApplication, documentRef.current])

        React.useEffect(() => {
            if (appId) {fetchGrantApplication({id: appId}).then(g => setGrant(g))};
        }, [appId]);

        const grantFinalized = isGrantFinalized();

        const [isLocked, setIsLocked] = useState<boolean>(target === RequestTarget.VIEW_APPLICATION);

        const [isEditingProjectReferenceId, setIsEditingProjectReference] = useState(false);

        const [grantProductCategories, setGrantProductCategories] = useState<{[key: string]: GrantProductCategory[]}>({});

        const [submitLoading, setSubmissionsLoading] = React.useState(false);
        const [grantGiversInUse, setGrantGiversInUse] = React.useState<string[]>([]);
        const [transferringApplication, setTransferringApplication] = useState(false);
        const [approver, setApprovers] = useState<{[project: string]: boolean}>({});

        React.useEffect(() => {
            if (grantApplication.status.stateBreakdown.length > 0) {
                const approver: {[project: string]: boolean} = {};
                const promises = Promise.allSettled(grantApplication.status.stateBreakdown.map(p =>
                    runWork<UserInProject>(viewProject({
                        id: "da469561-0e73-4696-8034-756c98c78a71"
                    }))
                ));
                promises.then(resolvedPromises => {
                    for (const [index, promise] of resolvedPromises.entries()) {
                        if (promise.status === "fulfilled") {
                            approver[grantApplication.status.stateBreakdown[index].id] = (
                                promise.value != null && [ProjectRole.ADMIN, ProjectRole.PI].includes(promise.value.whoami.role)
                            )
                            // const value = promise.value as UserInProject;
                            // console.log(value);
                        }
                    }
                });
            }
        }, [grantApplication.status.stateBreakdown]);

        const submitRequest = useCallback(async () => {
            setSubmissionsLoading(true);
            if (grantGiversInUse.length === 0) {
                snackbarStore.addFailure("No grant giver selected. Please select one to submit.", false);
                setSubmissionsLoading(false);
                return;
            }

            const requestedResourcesByAffiliate = Object.keys(grantProductCategories).flatMap(entry =>
                grantProductCategories[entry].map(wb => {
                    let creditsRequested = parseIntegerFromInput(
                        document.querySelector<HTMLInputElement>(
                            `input[data-target="${productCategoryId(wb.metadata.category)}"]`
                        )
                    );

                    const first = document.getElementsByClassName(
                        productCategoryStartDate(wb.metadata.category)
                    )?.[0] as HTMLInputElement;
                    const start = parseDateFromInput(first);

                    const second = document.getElementsByClassName(
                        productCategoryEndDate(wb.metadata.category)
                    )?.[0] as HTMLInputElement;
                    const end = parseDateFromInput(second);

                    const allocation = document.querySelector<HTMLInputElement>(
                        `input[data-target="${productCategoryAllocation(wb.metadata.category)}"]`
                    );

                    if (target === RequestTarget.VIEW_APPLICATION && !allocation) {
                        snackbarStore.addFailure("Allocation can't be empty.", false);
                        return;
                    }

                    if (creditsRequested) {
                        creditsRequested = normalizeBalanceForBackend(
                            creditsRequested,
                            wb.metadata.productType, wb.metadata.chargeType, wb.metadata.unitOfPrice
                        );
                    }

                    if (creditsRequested === undefined || creditsRequested <= 0) {
                        return null;
                    }

                    return {
                        category: wb.metadata.category.name,
                        provider: wb.metadata.category.provider,
                        grantGiver: entry,
                        sourceAllocation: allocation?.value ?? null, // TODO(Jonas): null on initial request, required on the following.
                        period: {
                            start,
                            end
                        }
                    } as AllocationRequest;
                }).filter(it => it !== null)
            ) as AllocationRequest[];
            setSubmissionsLoading(false);
            console.log("Submit TODO", requestedResourcesByAffiliate);
        }, [grantGiversInUse, grantProductCategories]);

        const updateReferenceID = useCallback(async () => {
            console.log("updateReferenceID TODO");
        }, []);

        const cancelEditOfRefId = useCallback(async () => {
            setIsEditingProjectReference(false);
        }, []);

        const approveRequest = useCallback(async () => {
            runWork(approveGrantApplication({requestId: grantApplication.id}));
        }, [grantApplication.id]);

        const rejectRequest = useCallback(async (notify: boolean) => {
            runWork(rejectGrantApplication({requestId: grantApplication.id}));
        }, [grantApplication.id]);

        const transferRequest = useCallback(async (toProjectId: string) => {
            await runWork(transferApplication({
                applicationId: grantApplication.id,
                transferToProjectId: toProjectId
            }));
        }, [grantApplication]);

        const closeRequest = useCallback(async () => {
            console.log("Close Request: TODO");
        }, []);

        const reload = useCallback(() => {
            fetchGrantGivers().then((page: UCloud.PageV2<ProjectWithTitle>) => setGrantGivers(page));
            if (appId) {fetchGrantApplication({id: appId}).then(g => setGrant(g))};
            fetchWallets(browseWallets({itemsPerPage: 250}));
        }, [appId]);

        const discardChanges = useCallback(() => {
            setIsLocked(true);
            reload();
        }, [reload]);


        const status = useProjectManagementStatus({isRootComponent: true});
        const isAdminOrPi = ["ADMIN", "PI"].includes(status.projectDetails.data.whoami.role);
        const isAdminOrPiForAnything = isAdminOrPi && grantApplication.status.stateBreakdown.some(it => it.id === status.projectId);
        const isPersonalWorkspace = grantApplication.currentRevision.document.recipient.type === "personal_workspace";
        const isGrantRecipient = (isAdminOrPi && grantApplication.createdBy === status.projectId) ||
            (isPersonalWorkspace && grantApplication.createdBy === Client.activeUsername); /* TODO(Jonas): Doesn't handle personal project */

        const grantGiverDropdown = React.useMemo(() =>
            target === RequestTarget.VIEW_APPLICATION || (grantGivers.items.length - grantGiversInUse.length) === 0 ? null :
                <ClickableDropdown
                    fullWidth
                    colorOnHover={false}
                    trigger="Select Grant Giver"
                    chevron
                >
                    <Wrapper>
                        <Table>
                            <TableHeader>
                                <TableRow>
                                    <TableHeaderCell pl="6px">Name</TableHeaderCell>
                                    <TableHeaderCell pl="6px">Description</TableHeaderCell>
                                </TableRow>
                            </TableHeader>
                            <tbody>
                                {grantGivers.items.filter(grantGiver => !grantGiversInUse.includes(grantGiver.projectId)).map(grantGiver =>
                                    <TableRow key={grantGiver.projectId} onClick={() => setGrantGiversInUse(inUse => [...inUse, grantGiver.projectId])}>
                                        <TableCell pl="6px"><Flex><Logo projectId={grantGiver.projectId} size="32px" /><Text mt="3px" ml="8px">{grantGiver.title}</Text></Flex></TableCell>
                                        <TableCell pl="6px"><GrantGiverDescription key={grantGiver.projectId} projectId={grantGiver.projectId} /></TableCell>
                                    </TableRow>
                                )}
                            </tbody>
                        </Table>
                    </Wrapper>
                </ClickableDropdown>,
            [grantGivers, grantGiversInUse]);

        const grantGiverEntries = React.useMemo(() =>
            grantGivers.items.filter(it => grantGiversInUse.includes(it.projectId)).map(it =>
                <GrantGiver
                    key={it.projectId}
                    grantApplication={grantApplication}
                    remove={() => setGrantGiversInUse(inUse => [...inUse.filter(entry => entry !== it.projectId)])}
                    setGrantProductCategories={setGrantProductCategories}
                    wallets={wallets.data.items}
                    project={it}
                    isLocked={isLocked}
                    isApprover={isAdminOrPi && status.projectId === it.projectId}
                />
            ), [grantGivers, grantGiversInUse, isLocked, wallets, isAdminOrPi]);

        const recipient = getDocument(grantApplication).recipient;
        const isApproverForProject = false;
        const allocationsById = React.useMemo(() => {
            const byId: {[id: string]: {allocation: WalletAllocation, wallet: Wallet}} = {};
            for (const wallet of wallets.data.items) {
                for (const allocation of wallet.allocations) {
                    byId[allocation.id] = {allocation, wallet};
                }
            }
            return byId;
        }, []);

        return (
            <MainContainer
                header={target === RequestTarget.EXISTING_PROJECT ?
                    <ProjectBreadcrumbs crumbs={[{title: "Request for Resources"}]} /> : null
                }
                sidebar={null}
                main={
                    <Flex justifyContent="center">
                        <Box maxWidth={1400} width="100%">
                            {target !== RequestTarget.NEW_PROJECT ? null : (
                                <>
                                    <Label mb={16} mt={16}>
                                        Principal Investigator (PI)
                                        <Input
                                            value={
                                                `${Client.userInfo?.firstNames} ${Client.userInfo?.lastName} ` +
                                                `(${Client.username})`
                                            }
                                            disabled
                                        />
                                    </Label>
                                    <Label mb={16} mt={16}>
                                        Project title
                                        <Input ref={projectTitleRef} />
                                    </Label>
                                </>
                            )}

                            {target !== RequestTarget.VIEW_APPLICATION ? null : (
                                <>
                                    <HighlightedCard color="blue" isLoading={false}>
                                        <Heading.h4 mb={16}>Metadata</Heading.h4>

                                        <Text mb={16}>
                                            <i>Application must be resubmitted to change the metadata.</i>
                                        </Text>
                                        <Table>
                                            <tbody>
                                                <TableRow>
                                                    <TableCell>Application Approver(s)</TableCell>
                                                    <TableCell><Box>{grantApplication.status.stateBreakdown.map(state => <Flex><Text>{state.title}</Text><StateIcon state={state.state} /></Flex>)}</Box></TableCell>
                                                </TableRow>
                                                <TableRow>
                                                    <TableCell>Project Title</TableCell>
                                                    <TableCell>{getRecipientId(getDocument(grantApplication).recipient)}</TableCell>
                                                </TableRow>
                                                <TableRow>
                                                    <TableCell>Principal Investigator (PI)</TableCell>
                                                    <TableCell>{"TODO: grantRecipient PI"}</TableCell>
                                                </TableRow>
                                                <TableRow>
                                                    <TableCell verticalAlign="top">
                                                        Project Type
                                                    </TableCell>
                                                    <TableCell>
                                                        <td>{recipientTypeToText(recipient)}</td>
                                                    </TableCell>
                                                </TableRow>
                                                <TableRow>
                                                    <TableCell verticalAlign="top">
                                                        Reference ID
                                                    </TableCell>
                                                    {isAdminOrPi && isAdminOrPiForAnything ?
                                                        <TableCell>
                                                            <table>
                                                                <tbody>
                                                                    <tr>
                                                                        {isEditingProjectReferenceId ? (
                                                                            <>
                                                                                <td>
                                                                                    <Input
                                                                                        placeholder={"e.g. DeiC-SDU-L1-000001"}
                                                                                        ref={projectReferenceIdRef}
                                                                                    />
                                                                                </td>
                                                                                <td>
                                                                                    <Button
                                                                                        color="blue"
                                                                                        onClick={updateReferenceID}
                                                                                        mx="6px"
                                                                                    >
                                                                                        Update Reference ID
                                                                                    </Button>
                                                                                    <Button
                                                                                        color="red"
                                                                                        onClick={cancelEditOfRefId}
                                                                                    >
                                                                                        Cancel
                                                                                    </Button>
                                                                                </td>
                                                                            </>) : (
                                                                            <>
                                                                                <td>
                                                                                    {getReferenceId(grantApplication) ?? "No ID given"}
                                                                                </td>
                                                                                <td>
                                                                                    <Button ml={"4px"} onClick={() => setIsEditingProjectReference(true)}>Edit</Button>
                                                                                </td>
                                                                            </>)
                                                                        }
                                                                    </tr>
                                                                </tbody>
                                                            </table>
                                                        </TableCell> :
                                                        <TableCell>
                                                            {grantApplication.currentRevision.document.referenceId ?? "No ID given"}
                                                        </TableCell>
                                                    }
                                                </TableRow>
                                                <TableRow>
                                                    <TableCell verticalAlign="top" mt={32}>Current Status</TableCell>
                                                    <TableCell>
                                                        {
                                                            /* TODO(Jonas): Maybe not correct enum? */
                                                            grantApplication.status.overallState === State.PENDING ? "In progress" :
                                                                grantApplication.status.overallState === State.APPROVED ? (grantApplication.currentRevision.updatedBy === null ? "Approved" : "Approved by " + grantApplication.currentRevision.updatedBy) :
                                                                    grantApplication.status.overallState === State.REJECTED ? (grantApplication.currentRevision.updatedBy === null ? "Rejected" : "Rejected  by " + grantApplication.currentRevision.updatedBy) :
                                                                        (grantApplication.currentRevision.updatedBy === null ? "Closed" : `Closed by ${grantApplication.currentRevision.updatedBy}`)
                                                        }
                                                        <ButtonGroup>
                                                            {target !== RequestTarget.VIEW_APPLICATION ? null : (
                                                                <>
                                                                    {isAdminOrPi && isAdminOrPiForAnything && !grantFinalized ?
                                                                        <>
                                                                            <Button
                                                                                color="green"
                                                                                onClick={approveRequest}
                                                                                disabled={!isLocked}
                                                                            >
                                                                                Approve
                                                                            </Button>
                                                                            <ClickableDropdown
                                                                                top="-73px"
                                                                                fullWidth={true}
                                                                                trigger={(
                                                                                    <Button
                                                                                        color="red"
                                                                                        disabled={!isLocked}
                                                                                        onClick={() => undefined}
                                                                                    >
                                                                                        Reject
                                                                                    </Button>
                                                                                )}
                                                                            >
                                                                                <OptionItem
                                                                                    onClick={() => rejectRequest(true)}
                                                                                    text={"Reject"}
                                                                                />
                                                                                <OptionItem
                                                                                    onClick={() => rejectRequest(false)}
                                                                                    text={"Reject without notify"}
                                                                                />
                                                                            </ClickableDropdown>
                                                                            {recipient.type !== "existing_project" && localStorage.getItem("enableprojecttransfer") != null ?
                                                                                <Button
                                                                                    color="blue"
                                                                                    onClick={() => setTransferringApplication(true)}
                                                                                    disabled={!isLocked}
                                                                                >
                                                                                    Transfer to other project
                                                                                </Button> : null
                                                                            }
                                                                        </> : null
                                                                    }
                                                                    {/* TODO(Jonas): Correct isAdminOrPi check? */}
                                                                    {!isAdminOrPi && !grantFinalized ?
                                                                        <>
                                                                            <Button
                                                                                color="red"
                                                                                onClick={closeRequest}
                                                                                disabled={!isLocked}
                                                                            >
                                                                                Withdraw
                                                                            </Button>
                                                                        </> : null
                                                                    }
                                                                </>
                                                            )}
                                                        </ButtonGroup>
                                                        {target !== RequestTarget.VIEW_APPLICATION || isLocked ||
                                                            grantFinalized ? null :
                                                            <Text>
                                                                You must finish making changes before you can
                                                                change the status of this application
                                                            </Text>
                                                        }
                                                    </TableCell>
                                                </TableRow>
                                            </tbody>
                                        </Table>
                                    </HighlightedCard>
                                </>
                            )}

                            <Heading.h3 mt={32}>
                                {target === RequestTarget.VIEW_APPLICATION ? "Requested Resources" : "Resources"}
                            </Heading.h3>

                            {target === RequestTarget.VIEW_APPLICATION ? null : grantGiverDropdown}
                            {target === RequestTarget.VIEW_APPLICATION ? null : grantGiverEntries}
                            {target !== RequestTarget.VIEW_APPLICATION ? null :
                                grantApplication.currentRevision.document.allocationRequests.map(it => (
                                    <React.Fragment key={it.sourceAllocation}></React.Fragment>
                                ))
                            }

                            {/* TODO(Jonas): This doesn't group by provider  */}
                            {target !== RequestTarget.VIEW_APPLICATION ? null : (
                                grantGivers.items.filter(it => grantApplication.status.stateBreakdown.map(it => it.id).includes(it.projectId)).map(grantGiver => <>
                                    <GrantGiver
                                        isApprover={isAdminOrPi}
                                        isLocked={isLocked || !isApproverForProject}
                                        remove={() => undefined}
                                        project={grantGiver}
                                        grantApplication={grantApplication}
                                        setGrantProductCategories={setGrantProductCategories}
                                        wallets={wallets.data.items}
                                    />
                                </>))}

                            <CommentApplicationWrapper>
                                <RequestFormContainer>
                                    <Heading.h4>Application</Heading.h4>
                                    <TextArea
                                        disabled={grantFinalized || isLocked || !isGrantRecipient}
                                        rows={25}
                                        ref={documentRef}
                                    />
                                </RequestFormContainer>

                                <Box width="100%">
                                    <Heading.h4>Comments</Heading.h4>
                                    {grantApplication.status.comments.length > 0 ? null : (
                                        <Box mt={16} mb={16}>
                                            No comments have been posted yet.
                                        </Box>
                                    )}

                                    {grantApplication.status.comments.map(it => (
                                        <CommentBox
                                            key={it.id}
                                            comment={it}
                                            avatar={avatars.cache[it.username] ?? defaultAvatar}
                                            reload={reload}
                                        />
                                    ))}

                                    <PostCommentWidget
                                        applicationId={grantApplication.currentRevision.document.referenceId ?? "" /* TODO(Jonas): Is this the samme as .id from before? */}
                                        avatar={avatars.cache[Client.username!] ?? defaultAvatar}
                                        reload={reload}
                                    />
                                </Box>
                            </CommentApplicationWrapper>
                            <Box p={32} pb={16}>
                                {target !== RequestTarget.VIEW_APPLICATION ? (
                                    <Button disabled={grantFinalized || submitLoading} fullWidth onClick={submitRequest}>
                                        Submit Application
                                    </Button>
                                ) : null
                                }
                                {target !== RequestTarget.VIEW_APPLICATION || grantFinalized ? null : (
                                    isLocked ? (
                                        <Button fullWidth onClick={() => setIsLocked(false)} disabled={loading}>
                                            Edit this request
                                        </Button>
                                    ) : (
                                        <ButtonGroup>
                                            <Button
                                                color={"green"}
                                                fullWidth
                                                disabled={loading}
                                                onClick={submitRequest}
                                            >
                                                Save Changes
                                            </Button>
                                            <Button color={"red"} onClick={discardChanges}>Discard changes</Button>
                                        </ButtonGroup>
                                    )
                                )}
                            </Box>
                        </Box>
                    </Flex>
                }
                additional={
                    <TransferApplicationPrompt
                        isActive={transferringApplication}
                        close={() => setTransferringApplication(false)}
                        transfer={transferRequest}
                        grantId={getReferenceId(grantApplication) ?? ""}
                    />}
            />
        );
    };

function StateIcon({state}: {state: State}) {
    let icon: IconName;
    let color: ThemeColor;
    switch (state) {
        case State.APPROVED:
            icon = "check";
            color = "green";
            break;
        case State.PENDING:
            icon = "ellipsis";
            color = "blue";
            break;
        case State.REJECTED:
            icon = "close";
            color = "red";
            break;
    }
    return <Icon ml="8px" name={icon} color={color} />
}

const projectDescriptionCache: {[projectId: string]: string | undefined} = {};

function getReferenceId(grantApplication: GrantApplication): string | null {
    return grantApplication.currentRevision.document.referenceId;
}

function GrantGiverDescription(props: {projectId: string;}): JSX.Element {
    const [description, fetchDescription] = useCloudAPI<RetrieveDescriptionResponse>(
        {noop: true}, {description: projectDescriptionCache[props.projectId] ?? ""}
    );

    useEffect(() => {
        if (projectDescriptionCache[props.projectId] == null) {
            fetchDescription(retrieveDescription({
                projectId: props.projectId,
            }));
        }
    }, [props.projectId]);

    useEffect(() => {
        if (description.data.description != null) {
            projectDescriptionCache[props.projectId] = description.data.description;
        }
    }, [description.data.description, projectDescriptionCache]);

    return <Truncate>{description.data.description}</Truncate>;
}

function recipientTypeToText(recipient: Recipient): string {
    switch (recipient.type) {
        case "existing_project":
            return "Existing Project";
        case "new_project":
            return "New Project";
        case "personal_workspace":
            return "Personal Workspace";
    }
}

function getRecipientId(recipient: Recipient): string {
    return recipient.type === "existing_project" ? recipient.id :
        recipient.type === "new_project" ? recipient.title :
            recipient.type === "personal_workspace" ? recipient.username : ""
}

function getAllocationRequests(grantApplication: GrantApplication): AllocationRequest[] {
    return getDocument(grantApplication).allocationRequests;
}

async function fetchProducts(
    request: APICallParameters<UCloud.grant.GrantsRetrieveProductsRequest, UCloud.grant.GrantsRetrieveProductsResponse>
): Promise<{availableProducts: Product[]}> {
    switch (request.parameters?.projectId) {
        // UAC
        case "just-some-id-we-cant-consider-valid": {
            return new Promise(resolve => resolve({
                availableProducts: [{
                    balance: 123123,
                    category: {
                        name: "SSG",
                        provider: "UAC",
                    },
                    chargeType: "DIFFERENTIAL_QUOTA",
                    freeToUse: false,
                    hiddenInGrantApplications: false,
                    name: "name",
                    pricePerUnit: 1203,
                    unitOfPrice: "PER_UNIT",
                    type: "compute",
                    productType: "COMPUTE",
                    description: "Didn't appear in the first iteration. Introduced for the second one. Beats all",
                    priority: 0
                }, {
                    balance: 123123,
                    category: {
                        name: "Ballista",
                        provider: "UAC",
                    },
                    chargeType: "DIFFERENTIAL_QUOTA",
                    freeToUse: false,
                    hiddenInGrantApplications: false,
                    name: "name",
                    pricePerUnit: 1203,
                    unitOfPrice: "PER_UNIT",
                    type: "storage",
                    productType: "STORAGE",
                    description: "Efficient and noisy",
                    priority: 0
                }]
            }));
        }
        // HELL
        case "just-some-other-id-we-cant-consider-valid": {
            return new Promise(resolve => resolve({
                availableProducts: [{
                    balance: 123123,
                    category: {
                        name: "SSG",
                        provider: "HELL",
                    },
                    chargeType: "DIFFERENTIAL_QUOTA",
                    freeToUse: false,
                    hiddenInGrantApplications: false,
                    name: "name",
                    pricePerUnit: 1203,
                    unitOfPrice: "PER_UNIT",
                    type: "compute",
                    productType: "COMPUTE",
                    description: "Didn't appear in the first iteration. Introduced for the second one. Beats all",
                    priority: 0
                }, {
                    balance: 123123,
                    category: {
                        name: "PlasmaR",
                        provider: "HELL",
                    },
                    chargeType: "DIFFERENTIAL_QUOTA",
                    freeToUse: false,
                    hiddenInGrantApplications: false,
                    name: "name",
                    pricePerUnit: 1203,
                    unitOfPrice: "PER_UNIT",
                    type: "storage",
                    productType: "STORAGE",
                    description: "Efficient and noisy",
                    priority: 0
                }]
            }));
        }
        // Cultist Base
        case "the-final-one": {
            return new Promise(resolve => resolve({
                availableProducts: [{
                    balance: 123123,
                    category: {
                        name: "SSG",
                        provider: "Cultist Base",
                    },
                    chargeType: "DIFFERENTIAL_QUOTA",
                    freeToUse: false,
                    hiddenInGrantApplications: false,
                    name: "name",
                    pricePerUnit: 1203,
                    unitOfPrice: "PER_UNIT",
                    type: "network_ip",
                    productType: "NETWORK_IP",
                    description: "Didn't appear in the first iteration. Introduced for the second one. Beats all",
                    priority: 0
                }, {
                    balance: 123123,
                    category: {
                        name: "CyberD",
                        provider: "Cultist Base",
                    },
                    chargeType: "DIFFERENTIAL_QUOTA",
                    freeToUse: false,
                    hiddenInGrantApplications: false,
                    name: "name",
                    pricePerUnit: 1203,
                    unitOfPrice: "PER_UNIT",
                    type: "storage",
                    productType: "STORAGE",
                    description: "Efficient and noisy",
                    priority: 0
                }]
            }));
        }
    }
    return new Promise(resolve => resolve({
        availableProducts: []
    }));
}

function GrantGiver(props: {
    grantApplication: GrantApplication;
    project: ProjectWithTitle;
    isLocked: boolean;
    remove?: () => void;
    wallets: Wallet[];
    setGrantProductCategories: React.Dispatch<React.SetStateAction<{[key: string]: GrantProductCategory[]}>>;
    isApprover: boolean;
}): JSX.Element {
    const {recipient} = props.grantApplication.currentRevision.document;
    const grantFinalized = isGrantFinalized();

    const recipientId = getRecipientId(recipient);

    /* TODO(Jonas): Why is this done over two parts instead of one?  */
    const [products, setProducts] = useState<{availableProducts: Product[]}>({availableProducts: []});
    React.useEffect(() => {
        const result = fetchProducts(grantApi.retrieveProducts({
            projectId: props.project.projectId,
            recipientType: recipient.type,
            recipientId,
            showHidden: false
        }));
        result.then(it => setProducts(it));
    }, []);

    const productCategories: GrantProductCategory[] = useMemo(() => {
        const result: GrantProductCategory[] = [];
        for (const product of products.availableProducts) {
            const metadata: ProductMetadata = {
                category: product.category,
                freeToUse: product.freeToUse,
                productType: product.productType,
                chargeType: product.chargeType,
                hiddenInGrantApplications: product.hiddenInGrantApplications,
                unitOfPrice: product.unitOfPrice
            };

            if (result.find(it => productCategoryEquals(it.metadata.category, metadata.category)) === undefined) {
                result.push({metadata});
            }
        }

        for (const request of getAllocationRequests(props.grantApplication)) {
            const existing = result.find(it =>
                productCategoryEquals(
                    it.metadata.category,
                    {name: request.category, provider: request.provider})
            );

            if (existing) {
                existing.requestedBalance = request.balanceRequested;
            }
        }
        return result;
    }, [products]);

    React.useEffect(() => {
        props.setGrantProductCategories(gpc => {
            gpc[props.project.projectId] = productCategories;
            return gpc;
        });
        return () => {
            props.setGrantProductCategories(gpc => {
                delete gpc[props.project.projectId];
                return gpc;
            });
        };
    }, [productCategories]);

    return React.useMemo(() =>
        <Box mt="12px">
            <Spacer
                left={<Heading.h3>{props.project.title}</Heading.h3>}
                right={props.remove ? <Flex cursor="pointer" onClick={props.remove}><Icon name="close" color="red" mr="8px" />Remove</Flex> : null}
            />
            {productTypes.map(type => {
                const filteredProductCategories = productCategories.filter(pc => pc.metadata.productType === type);
                const noEntries = filteredProductCategories.length === 0;
                const filteredWallets = props.wallets.filter(it => it.productType === type);
                return (<React.Fragment key={type}>
                    <Heading.h4 mt={32}><Flex>{productTypeToTitle(type)} <ProductLink /></Flex></Heading.h4>
                    {noEntries ? <Heading.h3 mt="12px">No products for type available.</Heading.h3> : <ResourceContainer>
                        {filteredProductCategories.map((pc, idx) =>
                            <GenericRequestCard
                                key={idx}
                                wb={pc}
                                grantFinalized={grantFinalized}
                                isLocked={props.isLocked}
                                allocationRequests={props.grantApplication.currentRevision.document.allocationRequests.filter(it => it.category === pc.metadata.category.name && it.provider === pc.metadata.category.provider)}
                                wallets={filteredWallets.filter(it => it.paysFor.provider === pc.metadata.category.provider)}
                                // TODO(Jonas): This should require that it is being viewed by the grant approver, I think. Should a grant approver creating a <-- ?? why does this just end?
                                showAllocationSelection={props.isApprover}
                            />
                        )}
                    </ResourceContainer>}
                </React.Fragment>);
            })}
        </Box>, [productCategories, props.wallets, props.grantApplication]);
}

interface TransferApplicationPromptProps {
    isActive: boolean;
    grantId: string;

    close(): void;

    transfer(toProjectId: string): Promise<void>;
}

function TransferApplicationPrompt({isActive, close, transfer, grantId}: TransferApplicationPromptProps) {
    const [projects, fetchProjects] = useCloudAPI<GrantsRetrieveAffiliationsResponse>({noop: true}, emptyPage);

    const history = useHistory();

    React.useEffect(() => {
        if (grantId) {
            fetchProjects(findAffiliations({page: 0, itemsPerPage: 100, grantId}))
        }
    }, [grantId]);

    const dispatch = useDispatch();

    return (
        <ReactModal
            style={defaultModalStyle}
            isOpen={isActive}
            onRequestClose={close}
            shouldCloseOnEsc
            shouldCloseOnOverlayClick
        >
            <List>
                {!projects.loading && projects.data.items.length === 0 ? <Heading.h3>No projects found.</Heading.h3> : null}
                {projects.data.items.map(it =>
                    <Spacer
                        key={it.projectId}
                        left={<Box key={it.projectId}>{it.title}</Box>}
                        right={<>
                            <ConfirmationButton
                                my="3px"
                                width="115px"
                                height="40px"
                                onAction={async () => {
                                    close();
                                    // Show that we are transferring
                                    dispatch(setLoading(true));
                                    await transfer(it.projectId);
                                    dispatch(setLoading(false));
                                    history.push("/project/grants/ingoing");
                                }}
                                icon="move"
                                actionText="Transfer"
                            />
                        </>}
                    />
                )}
            </List>
        </ReactModal>
    );
}

const OptionItem: React.FunctionComponent<{onClick: () => void; text: string; color?: string}> = props => (
    <Box cursor="pointer" width="auto" onClick={props.onClick}>
        <TextSpan color={props.color}>{props.text}</TextSpan>
    </Box>
);

const CommentApplicationWrapper = styled.div`
    display: grid;
    grid-template-columns: repeat(auto-fill, minmax(600px, 1fr));
    grid-gap: 32px;
    max-width: 1400px;
`;

const CommentBoxWrapper = styled.div`
    display: flex;
    margin: 10px 0;

    .body {
        flex-grow: 1;
        margin: 0 6px;
    }

    time {
        color: var(--gray, #ff0);
    }

    p {
        margin: 0;
    }
`;

const CommentBox: React.FunctionComponent<{
    comment: Comment,
    avatar: AvatarType,
    reload: () => void
}> = ({comment, avatar, reload}) => {
    const [, runCommand] = useCloudCommand();
    const onDelete = useCallback(() => {
        addStandardDialog({
            title: "Confirm comment deletion",
            message: "Are you sure you wish to delete your comment?",
            confirmText: "Delete",
            addToFront: true,
            onConfirm: async () => {
                await runCommand(deleteGrantApplicationComment({commentId: comment.id}));
                reload();
            }
        });
    }, [comment.id]);

    return <CommentBoxWrapper>
        <div className="avatar">
            <UserAvatar avatar={avatar} width={"48px"} />
        </div>

        <div className="body">
            <p><strong>{comment.username}</strong> says:</p>
            <p>{comment.comment}</p>
            <time>{dateToString(comment.createdAt)}</time>
        </div>

        {comment.username === Client.username ? (
            <div>
                <Icon cursor={"pointer"} name={"trash"} color={"red"} onClick={onDelete} />
            </div>
        ) : null}
    </CommentBoxWrapper>;
};

const PostCommentWrapper = styled.form`
    .wrapper {
        display: flex;
    }

    ${TextArea} {
        flex-grow: 1;
        margin-left: 6px;
    }

    .buttons {
        display: flex;
        margin-top: 6px;
        justify-content: flex-end;
    }
`;

const PostCommentWidget: React.FunctionComponent<{
    applicationId: string,
    avatar: AvatarType,
    reload: () => void
}> = ({applicationId, avatar, reload}) => {
    const commentBoxRef = useRef<HTMLTextAreaElement>(null);
    const [loading, runWork] = useCloudCommand();
    const submitComment = useCallback(async (e) => {
        e.preventDefault();

        await runWork(commentOnGrantApplication({
            requestId: applicationId,
            comment: commentBoxRef.current!.value
        }));
        reload();
        if (commentBoxRef.current) commentBoxRef.current!.value = "";
    }, [runWork, applicationId, commentBoxRef.current]);
    return <PostCommentWrapper onSubmit={submitComment}>
        <div className="wrapper">
            <UserAvatar avatar={avatar} width={"48px"} />
            <TextArea rows={3} ref={commentBoxRef} placeholder={"Your comment"} />
        </div>
        <div className="buttons">
            <Button disabled={loading}>Send</Button>
        </div>
    </PostCommentWrapper>;
};

function getDocument(grantApplication: GrantApplication): Document {
    return grantApplication.currentRevision.document;
}

function ProductLink(): JSX.Element {
    return <Tooltip
        trigger={<ExternalLink href="/app/skus"><Box style={{
            cursor: "pointer",
            border: "2px var(--black) solid",
            borderRadius: "9999px",
            width: "35px",
            height: "35px",
            marginLeft: "9px",
            paddingLeft: "10px",
            marginTop: "-2px"
        }}> ?</Box></ExternalLink>}
    >
        <Box width="100px">Click to view details for resources</Box>
    </Tooltip>
}

export default GrantApplicationEditor;
