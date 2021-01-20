import * as UCloud from "UCloud";
import {useCloudAPI, useCloudCommand} from "Authentication/DataHook";
import {Client} from "Authentication/HttpClientInstance";
import {MainContainer} from "MainContainer/MainContainer";
import * as React from "react";
import {snackbarStore} from "Snackbar/SnackbarStore";
import styled from "styled-components";
import {Box, Button, Flex, Icon, Input, Label, Text, Tooltip, Card, Grid, TextArea, Select} from "ui-components";
import * as Heading from "ui-components/Heading";
import Table, {TableCell, TableHeader, TableHeaderCell, TableRow} from "ui-components/Table";
import {TextSpan} from "ui-components/Text";
import {addStandardDialog} from "UtilityComponents";
import {useTitle} from "Navigation/Redux/StatusActions";
import {useSidebarPage, SidebarPages} from "ui-components/Sidebar";
import {MutableRefObject, useCallback, useEffect, useRef, useState} from "react";
import {accounting, compute, PageV2} from "UCloud";
import KubernetesLicense = compute.ucloud.KubernetesLicense;
import licenseApi = compute.ucloud.licenses.maintenance;
import {emptyPageV2} from "DefaultObjects";
import {useRefreshFunction} from "Navigation/Redux/HeaderActions";
import * as Pagination from "Pagination";
import ReactModal from "react-modal";
import {defaultModalStyle} from "Utilities/ModalUtilities";
import {useProjectStatus} from "Project/cache";
import {useProjectId} from "Project";
import {UCLOUD_PROVIDER} from "Accounting";
import Wallet = accounting.Wallet;
import {PaymentModel} from "Accounting";
import {prettierString} from "UtilityFunctions";
import {InputLabel} from "ui-components/Input";
import {creditFormatter} from "Project/ProjectUsage";

const PaymentModelOptions: PaymentModel[] = ["PER_ACTIVATION", "FREE_BUT_REQUIRE_BALANCE"];

const LeftAlignedTableHeader = styled(TableHeader)`
  text-align: left;
`;

const GrantCopies: React.FunctionComponent<{licenseServer: KubernetesLicense, onGrant: () => void}> = props => {
    const [loading, invokeCommand] = useCloudCommand();
    const project = useProjectStatus();
    const projectId = useProjectId();
    const projectName = project.fetch().membership.find(it => it.projectId === projectId)?.title

    const grantCopies = useCallback(async () => {
        if (loading || !projectId) return;
        const wallet: Wallet = {
            id: projectId,
            type: "PROJECT",
            paysFor: {
                provider: UCLOUD_PROVIDER,
                id: props.licenseServer.id
            }
        };

        // NOTE(Dan): We must initialize the wallet first, this is quite likely to fail if we are adding additional
        // copies.
        try {
            await invokeCommand(
                UCloud.accounting.wallets.setBalance({wallet, newBalance: 0, lastKnownBalance: 0}),
                {defaultErrorHandler: false}
            );
        } catch (ignored) {
            // Ignored
        }

        await invokeCommand(UCloud.accounting.wallets.addToBalance({
            credits: 1_000_000 * 1000,
            wallet
        }));
        props.onGrant();
    }, [props.onGrant, loading]);

    return <Grid gridTemplateColumns={"1fr"} gridGap={16}>
        <Heading.h3>Grant copies?</Heading.h3>
        <Box>
            This will add 1000 copies to your currently active project ({projectName}). Users will be able to apply
            from this project to receive access to the license.
        </Box>
        <Button onClick={grantCopies}>Grant copies</Button>
    </Grid>;
};

const LicenseServerTagsPrompt: React.FunctionComponent<{
    licenseServer: KubernetesLicense;
    onUpdate?: () => void;
}> = ({licenseServer, onUpdate}) => {
    const [tagList, setTagList] = useState<string[]>(licenseServer.tags);
    useEffect(() => {
        setTagList(licenseServer.tags);
    }, [licenseServer]);

    const [, invokeCommand] = useCloudCommand();
    const newTagField = useRef<HTMLInputElement>(null);

    return (
        <Box>
            <div>
                <Flex alignItems={"center"}>
                    <Heading.h3>
                        <TextSpan color="gray">Tags for</TextSpan> {licenseServer?.id}
                    </Heading.h3>
                </Flex>
                <Box mt={16} mb={30}>
                    <form
                        onSubmit={async e => {
                            e.preventDefault();

                            const tagValue = newTagField.current?.value;
                            if (tagValue === undefined || tagValue === "") return;
                            const newTagList = [...tagList, tagValue]
                            setTagList(newTagList);
                            newTagField.current!.value = "";
                            await invokeCommand(licenseApi.update({...licenseServer, tags: newTagList}));
                            if (onUpdate) onUpdate();
                        }}
                    >
                        <Flex height={45}>
                            <Input
                                rightLabel
                                type="text"
                                ref={newTagField}
                                placeholder="Name of tag"
                            />
                            <Button
                                attached
                                width="200px"
                                type={"submit"}
                            >
                                Add tag
                            </Button>
                        </Flex>
                    </form>
                </Box>
                {tagList.length > 0 ? (
                    <Box maxHeight="80vh">
                        <Table width="500px">
                            <LeftAlignedTableHeader>
                                <TableRow>
                                    <TableHeaderCell>Tag</TableHeaderCell>
                                    <TableHeaderCell width={50}>Delete</TableHeaderCell>
                                </TableRow>
                            </LeftAlignedTableHeader>
                            <tbody>
                                {tagList.map(tagEntry => (
                                    <TableRow key={tagEntry}>
                                        <TableCell>{tagEntry}</TableCell>
                                        <TableCell textAlign="right">
                                            <Button
                                                color={"red"}
                                                type={"button"}
                                                paddingLeft={10}
                                                paddingRight={10}
                                                onClick={async () => {
                                                    const newTagList = tagList.filter(it => it !== tagEntry);
                                                    setTagList(newTagList);
                                                    await invokeCommand(
                                                        licenseApi.update({...licenseServer, tags: newTagList})
                                                    );
                                                    if (onUpdate) onUpdate();
                                                }}
                                            >
                                                <Icon size={16} name="trash" />
                                            </Button>
                                        </TableCell>
                                    </TableRow>
                                ))}
                            </tbody>
                        </Table>
                    </Box>
                ) : (
                        <Text textAlign="center">No tags found</Text>
                    )}
            </div>
        </Box>
    );
}

interface InputHook {
    ref: MutableRefObject<HTMLInputElement | null>;
    hasError: boolean;
    setHasError: (err: boolean) => void;
}

function useInput(): InputHook {
    const ref = useRef(null);
    const [hasError, setHasError] = useState(false);
    return {ref, hasError, setHasError};
}

const LicenseServers: React.FunctionComponent = () => {
    const [licenses, fetchLicenses] = useCloudAPI<PageV2<KubernetesLicense>>({noop: true}, emptyPageV2);
    const [loading, invokeCommand] = useCloudCommand();
    const [infScroll, setInfScroll] = useState(0);
    const [editing, setEditing] = useState<KubernetesLicense | null>(null);
    const [granting, setGranting] = useState<KubernetesLicense | null>(null);

    const [isAvailable, setAvailable] = React.useState(true);
    const descriptionRef = React.useRef<HTMLTextAreaElement>(null);
    const [paymentModel, setPaymentModel] = React.useState<PaymentModel>("PER_ACTIVATION");

    const projectId = useProjectId();

    const reload = useCallback(() => {
        fetchLicenses(licenseApi.browse({}));
        setInfScroll(s => s + 1);
    }, []);
    useEffect(reload, [reload]);

    const loadMore = useCallback(() => {
        fetchLicenses(licenseApi.browse({next: licenses.data.next}));
    }, [licenses.data]);

    const nameInput = useInput();
    const portInput = useInput();
    const addressInput = useInput();
    const licenseInput = useInput();
    const pricePerUnitInput = useInput();
    const priorityInput = useInput();
    const reasonInput = useInput();

    useTitle("UCloud/Compute: License servers");
    useSidebarPage(SidebarPages.Admin);
    useRefreshFunction(reload);

    async function submit(e: React.SyntheticEvent): Promise<void> {
        e.preventDefault();

        const name = nameInput.ref.current!.value;
        const port = parseInt(portInput.ref.current!.value, 10);
        const address = addressInput.ref.current!.value;
        const license = licenseInput.ref.current!.value;
        const priority = parseInt(priorityInput.ref.current!.value, 10);
        const pricePerUnit = parseInt(pricePerUnitInput.ref.current!.value, 10) * 1_000_000;
        const reason = reasonInput.ref.current?.value ?? "";

        let error = false;

        if (name === "") {
            nameInput.setHasError(true);
            error = true;
        }
        if (address === "") {
            addressInput.setHasError(true);
            error = true;
        }
        if (isNaN(port)) {
            portInput.setHasError(true);
            error = true;
        }

        if (isNaN(priority)) {
            priorityInput.setHasError(true);
            error = true;
        }

        if (isNaN(pricePerUnit)) {
            pricePerUnitInput.setHasError(true);
            error = true;
        }

        const description = descriptionRef.current?.value ?? "";

        if (!error) {
            if (loading) return;
            const request: KubernetesLicense = {
                id: name,
                port,
                address,
                license: license !== "" ? license : undefined,
                tags: [],

                availability: isAvailable ? {type: "available"} : {type: "unavailable", reason},
                category: {
                    id: name,
                    provider: UCLOUD_PROVIDER
                },
                description,
                paymentModel,
                pricePerUnit,
                priority
            };
            await invokeCommand(licenseApi.create(request));
            snackbarStore.addSuccess(`License server '${name}' successfully added`, true);
            reload();
        }
    }

    if (!Client.userIsAdmin) return null;

    const [openLicenses, setOpenLicenses] = useState<Set<string>>(new Set());
    console.log(openLicenses);

    return (
        <MainContainer
            header={<Heading.h1>License Servers</Heading.h1>}
            headerSize={64}
            main={(
                <>
                    <Box maxWidth={800} mt={30} marginLeft="auto" marginRight="auto">
                        <form onSubmit={e => submit(e)}>
                            <Label mb="1em">
                                Name
                                <Input
                                    ref={nameInput.ref}
                                    error={nameInput.hasError}
                                    placeholder={"Identifiable name for the license server"}
                                />
                            </Label>
                            <Box marginBottom={30}>
                                <Flex height={45}>
                                    <Label mb="1em">
                                        Address
                                        <Input
                                            ref={addressInput.ref}
                                            error={addressInput.hasError}
                                            rightLabel
                                            placeholder={"IP address or URL"}
                                        />
                                    </Label>
                                    <Label mb="1em" width="30%">
                                        Port
                                        <Input
                                            ref={portInput.ref}
                                            error={portInput.hasError}
                                            type={"number"}
                                            min={0}
                                            max={65535}
                                            leftLabel
                                            maxLength={5}
                                            placeholder={"Port"}
                                        />
                                    </Label>
                                </Flex>
                            </Box>
                            <Label mb="1em">
                                Key
                                <Input
                                    ref={licenseInput.ref}
                                    error={licenseInput.hasError}
                                    placeholder="License or key (if needed)"
                                />
                            </Label>

                            <Label>
                                Priority
                                <Input mb="1em" type="number" autoComplete="off" ref={priorityInput.ref} defaultValue={0} />
                            </Label>

                            <Label mb="1em">
                                Availability
                                <Select defaultValue={isAvailable ? "Available" : "Unavailable"}>
                                    <option onClick={() => setAvailable(true)}>Available</option>
                                    <option onClick={() => setAvailable(false)}>Unavailable</option>
                                </Select>
                            </Label>

                            {isAvailable ? null :
                                <Label mb="1em">
                                    Unvailability reason
                                    <Input ref={reasonInput.ref} />
                                </Label>
                            }

                            <Label>
                                Payment Model
                                <Flex mb="1em">
                                    <Select defaultValue={paymentModel[0]}>
                                        {PaymentModelOptions.map(it =>
                                            <option key={it} onClick={() => setPaymentModel(it)}>{prettierString(it)}</option>
                                        )}
                                    </Select>
                                </Flex>
                            </Label>

                            <Label>
                                Price per unit
                                <Flex mb="1em">
                                    <Input autoComplete="off" min={0} defaultValue={1} type="number" ref={pricePerUnitInput.ref} rightLabel />
                                    <InputLabel width="60px" rightLabel>DKK</InputLabel>
                                </Flex>
                            </Label>

                            <TextArea width={1} mb="1em" rows={4} ref={descriptionRef} placeholder="License description..." />

                            <Button type="submit" color="green" disabled={loading}>Add License Server</Button>
                        </form>

                        {projectId == null ?
                            <Text bold mt={8}>
                                You must have an active project in order to grant copies of a license!
                            </Text> : null
                        }

                        <ReactModal
                            isOpen={editing != null}
                            onRequestClose={() => setEditing(null)}
                            shouldCloseOnEsc
                            ariaHideApp={false}
                            style={defaultModalStyle}
                        >
                            {!editing ? null : <LicenseServerTagsPrompt licenseServer={editing} onUpdate={reload} />}
                        </ReactModal>

                        <ReactModal
                            isOpen={granting != null}
                            onRequestClose={() => setGranting(null)}
                            shouldCloseOnEsc
                            ariaHideApp={false}
                            style={defaultModalStyle}
                        >
                            {!granting ? null :
                                <GrantCopies licenseServer={granting} onGrant={() => setGranting(null)} />}
                        </ReactModal>

                        <Box mt={30}>
                            <Pagination.ListV2
                                loading={licenses.loading}
                                page={licenses.data}
                                infiniteScrollGeneration={infScroll}
                                onLoadMore={loadMore}
                                pageRenderer={items => (
                                    items.map(licenseServer => {
                                        const isSelected = openLicenses.has(licenseServer.id);
                                        return (
                                            <ExpandingCard height={isSelected ? "400px" : "96px"} key={licenseServer.id} mb={2} padding={20} borderRadius={5}>
                                                <Flex justifyContent="space-between">
                                                    <Box>
                                                        <Flex>
                                                            {/* <RotatingIcon onClick={() => {
                                                                console.log("foo");
                                                                if (isSelected) {
                                                                    openLicenses.delete(licenseServer.id);
                                                                } else {
                                                                    openLicenses.add(licenseServer.id);
                                                                }
                                                                setOpenLicenses(new Set(openLicenses));
                                                            }} size={14} name="close" rotation={isSelected ? 0 : 45} /> */}
                                                            <Heading.h4>{licenseServer.id}</Heading.h4>
                                                        </Flex>
                                                        <Box>{licenseServer.address}:{licenseServer.port}</Box>
                                                    </Box>
                                                    <Flex>
                                                        <Box>
                                                            {licenseServer.license !== null ? (
                                                                <Tooltip
                                                                    tooltipContentWidth="300px"
                                                                    wrapperOffsetLeft="0"
                                                                    wrapperOffsetTop="4px"
                                                                    right="0"
                                                                    top="1"
                                                                    mb="50px"
                                                                    trigger={(
                                                                        <Icon
                                                                            size="20px"
                                                                            mt="8px"
                                                                            mr="8px"
                                                                            color="gray"
                                                                            name="key"
                                                                            ml="5px"
                                                                        />
                                                                    )}
                                                                >
                                                                    {licenseServer.license}
                                                                </Tooltip>
                                                            ) : <Text />}
                                                        </Box>
                                                        <Box>
                                                            <Icon
                                                                cursor="pointer"
                                                                size="20px"
                                                                mt="6px"
                                                                mr="8px"
                                                                color="gray"
                                                                color2="midGray"
                                                                name="tags"
                                                                onClick={() => setEditing(licenseServer)}
                                                            />
                                                        </Box>

                                                        <Box>
                                                            <Button
                                                                color={"red"}
                                                                type={"button"}
                                                                px={10}

                                                                onClick={() => addStandardDialog({
                                                                    title: `Are you sure?`,
                                                                    message: `Mark license server '${licenseServer.id}' as inactive?`,
                                                                    onConfirm: async () => {
                                                                        // TODO
                                                                        reload();
                                                                    }
                                                                })}
                                                            >
                                                                <Icon size={16} name="trash" />
                                                            TODO
                                                        </Button>
                                                        </Box>

                                                        {!projectId ? null : (
                                                            <Box>
                                                                <Button onClick={() => setGranting(licenseServer)}>
                                                                    Grant copies
                                                            </Button>
                                                            </Box>
                                                        )}
                                                    </Flex>
                                                </Flex>
                                                {/* HIDDEN ON NOT OPEN */}
                                                <Box height="25px" />
                                                {licenseServer.availability.type === "available" ?
                                                    <Heading.h4>Available</Heading.h4> :
                                                    <Heading.h4>Unvailable: {licenseServer.availability.reason}</Heading.h4>
                                                }

                                                <Heading.h4>Description</Heading.h4>
                                                <TextArea width={1} rows={4} defaultValue={licenseServer.description} />
                                                <Flex>
                                                    <Box width="50%">
                                                        <Heading.h4>Price per unit</Heading.h4>
                                                        {creditFormatter(licenseServer.pricePerUnit)}
                                                    </Box>

                                                    <Box width="50%">
                                                        <Heading.h4>Payment model</Heading.h4>
                                                        {prettierString(licenseServer.paymentModel)}
                                                    </Box>
                                                </Flex>

                                                <Button width={1}>Update</Button>
                                            </ExpandingCard>
                                        )
                                    })
                                )}
                            />
                        </Box>
                    </Box>
                </>
            )}
        />
    );
};

const ExpandingCard = styled(Card)`
    transition: height 0.5s;
    overflow: hidden;
`;

const RotatingIcon = styled(Icon)`
    size: 14px;
    margin-right: 8px;
    margin-top: 9px;
    cursor: pointer;
    color: var(--blue, #f00);
    transition: transform 0.2s;
`;

export default LicenseServers;
