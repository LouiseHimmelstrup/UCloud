import {bulkRequestOf, defaultSearch, emptyPage, emptyPageV2, useSearch} from "@/DefaultObjects";
import {MainContainer} from "@/MainContainer/MainContainer";
import {setRefreshFunction} from "@/Navigation/Redux/HeaderActions";
import {updatePageTitle} from "@/Navigation/Redux/StatusActions";
import * as React from "react";
import {connect} from "react-redux";
import {Dispatch} from "redux";
import {Box, Button, Flex, Icon, Link, Markdown, Text} from "@/ui-components";
import * as Heading from "@/ui-components/Heading";
import List from "@/ui-components/List";
import {fileName, getParentPath} from "@/Utilities/FileUtilities";
import {DashboardOperations, DashboardProps} from ".";
import {setAllLoading} from "./Redux/DashboardActions";
import {APICallState, InvokeCommand, useCloudAPI, useCloudCommand} from "@/Authentication/DataHook";
import {buildQueryString} from "@/Utilities/URIUtilities";
import {Spacer} from "@/ui-components/Spacer";
import {dateToString} from "@/Utilities/DateUtilities";
import {dispatchSetProjectAction} from "@/Project/Redux";
import Table, {TableCell, TableRow} from "@/ui-components/Table";
import {GrantApplicationFilter, IngoingGrantApplicationsResponse, } from "@/Project/Grant";
import * as UCloud from "@/UCloud";
import {PageV2} from "@/UCloud";
import {api as FilesApi, UFile} from "@/UCloud/FilesApi";
import metadataApi, {FileMetadataAttached} from "@/UCloud/MetadataDocumentApi";
import MetadataNamespaceApi, {FileMetadataTemplateNamespace} from "@/UCloud/MetadataNamespaceApi";
import HighlightedCard from "@/ui-components/HighlightedCard";
import {snackbarStore} from "@/Snackbar/SnackbarStore";
import {useNavigate} from "react-router";
import {
    Product,
    productCategoryEquals,
    ProductMetadata,
    productTypeToIcon,
    productTypeToTitle,
    retrieveUsage,
    UsageChart,
    usageExplainer
} from "@/Accounting";
import {api as JobsApi, Job} from "@/UCloud/JobsApi";
import {Client} from "@/Authentication/HttpClientInstance";
import {browseGrantApplications, GrantApplication} from "@/Project/Grant/GrantApplicationTypes";
import {Connect} from "@/Providers/Connect";
import {NotificationDashboardCard} from "@/Notifications";
import {isAdminOrPI} from "@/Project/Api";
import {useProject} from "@/Project/cache";
import {ProviderTitle} from "@/Providers/ProviderTitle";
import {ProviderLogo} from "@/Providers/ProviderLogo";
import AppRoutes from "@/Routes";
import {StandardButtonSize} from "@/ui-components/Button";
import {injectStyle, injectStyleSimple} from "@/Unstyled";
import {UtilityBar} from "@/Playground/Playground";
import ExperimentalJobs from "@/Applications/Jobs/ExperimentalJobs";
import {ExperimentalGrantApplications} from "@/Project/Grant/ExperimentalGrantApplications";
import ucloudImage from "@/Assets/Images/ucloud-2.png";

function Dashboard(props: DashboardProps): JSX.Element {
    useSearch(defaultSearch);

    const [news] = useCloudAPI<Page<NewsPost>>(newsRequest({
        itemsPerPage: 10,
        page: 0,
        withHidden: false,
    }), emptyPage);

    const [recentRuns, fetchRuns] = useCloudAPI<PageV2<Job>>({noop: true}, emptyPage);

    const [products, fetchProducts] = useCloudAPI<PageV2<Product>>({noop: true}, emptyPageV2);
    const [usage, fetchUsage] = useCloudAPI<{charts: UsageChart[]}>({noop: true}, {charts: []});

    const [outgoingApps, fetchOutgoingApps] = useCloudAPI<PageV2<GrantApplication>>(
        {noop: true},
        emptyPageV2
    );

    const [ingoingApps, fetchIngoingApps] = useCloudAPI<IngoingGrantApplicationsResponse>(
        {noop: true},
        emptyPageV2
    );

    const [favoriteFiles, fetchFavoriteFiles] = useCloudAPI<PageV2<FileMetadataAttached>>(
        {noop: true},
        emptyPageV2
    );

    React.useEffect(() => {
        props.onInit();
        reload(true);
        props.setRefresh(() => reload(true));
        return () => props.setRefresh();
    }, []);

    function reload(loading: boolean): void {
        props.setAllLoading(loading);
        fetchProducts(UCloud.accounting.products.browse({
            itemsPerPage: 250,
            filterUsable: true,
            includeBalance: true,
            includeMaxBalance: true
        }));
        fetchOutgoingApps(browseGrantApplications({
            itemsPerPage: 10,
            includeIngoingApplications: false,
            includeOutgoingApplications: true,
            filter: GrantApplicationFilter.ACTIVE
        }));
        fetchIngoingApps(browseGrantApplications({
            itemsPerPage: 10,
            includeIngoingApplications: true,
            includeOutgoingApplications: false,
            filter: GrantApplicationFilter.ACTIVE
        }));
        fetchFavoriteFiles(metadataApi.browse({
            filterActive: true,
            filterTemplate: "Favorite",
            itemsPerPage: 10
        }));
        fetchUsage(retrieveUsage({}));
        fetchRuns(JobsApi.browse({itemsPerPage: 10, sortBy: "MODIFIED_AT"}));
    }

    const main = (<Box mx="auto" maxWidth={"1200px"}>
        <Flex><h3>Dashboard</h3><Box ml="auto" /><UtilityBar searchEnabled={false} /></Flex>
        <div>
            <DashboardNews news={news} />

            <div className={GridClass}>
                <DashboardGrantApplications outgoingApps={outgoingApps} ingoingApps={ingoingApps} />
                <DashboardRuns runs={recentRuns} />
            </div>
            <div style={{marginBottom: "24px"}}>
                <NotificationDashboardCard />
            </div>
            <UsageAndResources charts={usage} products={products} />
            <div className={GridClass}>
                <Connect embedded />
                <DashboardFavoriteFiles
                    favoriteFiles={favoriteFiles}
                    onDeFavorite={() => fetchFavoriteFiles(metadataApi.browse({
                        filterActive: true,
                        filterTemplate: "Favorite",
                        itemsPerPage: 10
                    }))}
                />
            </div>
        </div>
    </Box>);

    return (
        <div className={Gradient}>
            <MainContainer main={main} />
        </div>
    );
}

const GridClass = injectStyle("grid", k => `
@media screen and (min-width: 900px) {
    ${k} {
        display: grid;
        grid-template-columns: repeat(auto-fill, minmax(400px, 1fr));
        grid-auto-rows: minmax(450px, auto);
        margin-top: 24px;
        margin-bottom: 24px;
        gap: 16px;
        gap: 20px;
    }
}   
@media screen and (max-width: 900px) {
    ${k} > * {
        margin-bottom: 24px;
    }   
    ${k} > *:first-child {
        margin-top: 24px;
    }
}
`);

export const Gradient = injectStyleSimple("gradient", `
    background: linear-gradient(var(--gradientStart), var(--gradientEnd));
    background-size: 100%;
	background-repeat: no-repeat;
    min-height: 100vh;
`);

interface DashboardFavoriteFilesProps {
    favoriteFiles: APICallState<PageV2<FileMetadataAttached>>;

    onDeFavorite(): void;
}

const DashboardFavoriteFiles = (props: DashboardFavoriteFilesProps): JSX.Element => {
    const [, invokeCommand] = useCloudCommand();

    const [favoriteTemplateId, setId] = React.useState("");
    React.useEffect(() => {
        fetchTemplate();
    }, []);

    const navigate = useNavigate();

    const favorites = props.favoriteFiles.data.items.filter(it => it.metadata.specification.document.favorite);

    return (
        <HighlightedCard
            color="darkBlue"
            isLoading={props.favoriteFiles.loading}
            icon="starFilled"
            title="Favorites"
            error={props.favoriteFiles.error?.why}
        >
            {favorites.length !== 0 ? null : (
                <NoResultsCardBody title={"No favorites"}>
                    <Text textAlign="center" width="100%">
                        As you add favorites, they will appear here.
                    </Text>
                    <Link to={"/drives"} mt={8} width={"100%"}>
                        <Button standardSize={StandardButtonSize.LARGE} fullWidth mt={8}>Explore files</Button>
                    </Link>
                </NoResultsCardBody>
            )}
            <List childPadding="8px">
                {favorites.map(it => (<Flex key={it.path}>
                    <Icon cursor="pointer" mr="6px" name="starFilled" color="blue" onClick={async () => {
                        if (!favoriteTemplateId) return;
                        try {
                            await invokeCommand(
                                metadataApi.delete(bulkRequestOf({
                                    changeLog: "Remove favorite",
                                    id: it.metadata.id
                                })),
                                {defaultErrorHandler: false}
                            );
                            props.onDeFavorite();
                        } catch (e) {
                            snackbarStore.addFailure("Failed to unfavorite", false);
                        }
                    }} />
                    <Text cursor="pointer" fontSize="20px" mb="6px" mt="-3px" onClick={() => navigateByFileType(it, invokeCommand, navigate)}>{fileName(it.path)}</Text>
                </Flex>))}
            </List>
        </HighlightedCard>
    );

    async function fetchTemplate() {
        const page = await invokeCommand<PageV2<FileMetadataTemplateNamespace>>(
            MetadataNamespaceApi.browse(({filterName: "favorite", itemsPerPage: 50}))
        );
        const ns = page?.items?.[0];
        if (ns) {
            setId(ns.id);
        }
    }
}

export async function navigateByFileType(file: FileMetadataAttached, invokeCommand: InvokeCommand, navigate: ReturnType<typeof useNavigate>): Promise<void> {
    const result = await invokeCommand<UFile>(FilesApi.retrieve({id: file.path}))
    if (result?.status.type === "FILE") {
        navigate(buildQueryString("/files", {path: getParentPath(file.path)}));
    } else {
        navigate(buildQueryString("/files", {path: file.path}))
    }
}

export interface NewsPost {
    id: number;
    title: string;
    subtitle: string;
    body: string;
    postedBy: string;
    showFrom: number;
    hideFrom: number | null;
    hidden: boolean;
    category: string;
}

interface NewsRequestProps extends PaginationRequest {
    filter?: string;
    withHidden: boolean;
}

export function newsRequest(payload: NewsRequestProps): APICallParameters<PaginationRequest> {
    return {
        reloadId: Math.random(),
        method: "GET",
        path: buildQueryString("/news/list", payload)
    };
}

export const NoResultsCardBody: React.FunctionComponent<{title: string; children: React.ReactNode}> = props => (
    <Flex
        alignItems="center"
        justifyContent="center"
        height="calc(100% - 60px)"
        minHeight="250px"
        mt="-30px"
        width="100%"
        flexDirection="column"
    >
        <Heading.h4>{props.title}</Heading.h4>
        {props.children}
    </Flex>
);

const ResourceGridClass = injectStyleSimple("grid", `
    display: grid;
    grid-template-columns: repeat(auto-fill, minmax(500px, 1fr));
    grid-auto-rows: minmax(450px, auto);
    gap: 16px;
`);

function UsageAndResources(props: {charts: APICallState<{charts: UsageChart[]}>; products: APICallState<PageV2<Product>>}): JSX.Element {
    const usage = React.useMemo(() => <DashboardProjectUsage charts={props.charts} />, [props.charts]);
    const products = React.useMemo(() => <DashboardResources products={props.products} />, [props.products]);

    return (
        <HighlightedCard
            color="yellow"
        >
            <div className={ResourceGridClass}>
                {usage}
                {products}
            </div>
        </HighlightedCard>
    );
}

function DashboardProjectUsage(props: {charts: APICallState<{charts: UsageChart[]}>}): JSX.Element | null {
    return (<div>
        <div>
            <Link to={AppRoutes.project.usage()}><Heading.h3>Resource usage</Heading.h3></Link>
        </div>
        <div>
            {props.charts.data.charts.length !== 0 ? null : (
                <NoResultsCardBody title={"No usage"}>
                    <Text style={{wordBreak: "break-word"}} textAlign="center">
                        As you use the platform, usage will appear here.
                    </Text>
                </NoResultsCardBody>
            )}
            {props.charts.data.charts.length === 0 ? null : <Heading.h3 color="darkGray">Past 30 days</Heading.h3>}
            <Table>
                <tbody>
                    {props.charts.data.charts.map((it, idx) => (
                        <TableRow key={idx}>
                            <TableCell>
                                <Icon name={productTypeToIcon(it.type)} mr={8} />
                                {productTypeToTitle(it.type)}
                            </TableCell>
                            <TableCell textAlign={"right"}>
                                {usageExplainer(it.periodUsage, it.type, it.chargeType, it.unit)}
                            </TableCell>
                        </TableRow>
                    ))}
                </tbody>
            </Table>
        </div>
    </div>);
}

function DashboardRuns({runs}: {
    runs: APICallState<UCloud.PageV2<Job>>;
}): JSX.Element {
    return <HighlightedCard
        color="gray"
        title={<Link to={"/jobs"}><Heading.h3>Recent runs</Heading.h3></Link>}
        icon="results"
        isLoading={runs.loading}
        error={runs.error?.why}
    >
        <ExperimentalJobs opts={{embedded: true, omitBreadcrumbs: true, omitFilters: true}} />
    </HighlightedCard>;
}

function DashboardResources({products}: {
    products: APICallState<PageV2<Product>>;
}): JSX.Element | null {
    const wallets = React.useMemo(() => {
        const wallets: (ProductMetadata & {balance: number})[] = [];

        for (const product of products.data.items) {
            const metadata: (ProductMetadata & {balance: number}) = {
                category: product.category,
                freeToUse: product.freeToUse,
                productType: product.productType,
                chargeType: product.chargeType,
                hiddenInGrantApplications: product.hiddenInGrantApplications,
                unitOfPrice: product.unitOfPrice,
                balance: product.balance!
            };

            if (!product.freeToUse) {
                if (wallets.find(it => productCategoryEquals(it.category, metadata.category)) === undefined) {
                    wallets.push(metadata);
                }
            }
        }
        return wallets;
    }, [products.data.items]);

    const project = useProject();
    const canApply = !Client.hasActiveProject || isAdminOrPI(project.fetch().status.myRole);

    wallets.sort((a, b) => {
        let compare: number = 0;

        compare = a.category.provider.localeCompare(b.category.provider);
        if (compare !== 0) return compare;

        compare = a.productType.localeCompare(b.productType);
        if (compare !== 0) return compare;

        compare = a.category.name.localeCompare(b.category.name);
        if (compare !== 0) return compare;

        return (a.balance < b.balance) ? 1 : -1;
    });

    const applyLinkButton = <Link to={Client.hasActiveProject ? "/project/grants/existing" : "/project/grants/personal"} mt={8}>
        <Button mt={8}>Apply for resources</Button>
    </Link>;

    return (
        <div>
            <Link to={AppRoutes.project.allocations()}><Heading.h3>Resource allocations</Heading.h3></Link>
            {wallets.length === 0 ? (
                <NoResultsCardBody title={"No available resources"}>
                    {!canApply ? null : <Text>
                        Apply for resources to use storage and compute on UCloud.
                    </Text>}
                    {applyLinkButton}
                </NoResultsCardBody>
            ) :
                <>
                    {/* height is 100% - height of Heading 55px */}
                    <Flex flexDirection="column" height={"calc(100% - 55px)"}>
                        <Box my="5px">
                            <Table>
                                <tbody>
                                    {wallets.slice(0, 7).map((n, i) => (
                                        <TableRow key={i}>
                                            <TableCell>
                                                <Flex alignItems="center" gap="8px">
                                                    <ProviderLogo providerId={n.category.provider} size={32} />
                                                    <ProviderTitle providerId={n.category.provider} /> / {n.category.name}
                                                </Flex>
                                            </TableCell>
                                            <TableCell textAlign={"right"}>
                                                {usageExplainer(n.balance, n.productType, n.chargeType, n.unitOfPrice)}
                                            </TableCell>
                                        </TableRow>
                                    ))}
                                </tbody>
                            </Table>
                        </Box>
                        <Box flexGrow={1} />
                        <Flex mx="auto">{applyLinkButton}</Flex>
                    </Flex>
                </>
            }
        </div>
    );
}

const DashboardGrantApplications: React.FunctionComponent<{
    outgoingApps: APICallState<PageV2<GrantApplication>>,
    ingoingApps: APICallState<PageV2<GrantApplication>>
}> = ({outgoingApps, ingoingApps}) => {
    const none = outgoingApps.data.items.length === 0 && ingoingApps.data.items.length === 0;
    const both = outgoingApps.data.items.length > 0 && ingoingApps.data.items.length > 0;
    const anyOutgoing = outgoingApps.data.items.length > 0;

    const title = (none ? <Link to={`/project/grants/outgoing/`}><Heading.h3>Grant applications</Heading.h3></Link>
        : both ? <Heading.h3>Grant Applications</Heading.h3>
            : <Link to={`/project/grants/${anyOutgoing ? "outgoing" : "ingoing"}/`}>
                <Heading.h3>Grant Applications</Heading.h3>
            </Link>
    );


    const project = useProject();
    const canApply = !Client.hasActiveProject || isAdminOrPI(project.fetch().status.myRole);

    if (!canApply) return null;

    return <HighlightedCard
        title={title}
        color="green"
        isLoading={outgoingApps.loading}
        icon="mail"
        error={outgoingApps.error?.why ?? ingoingApps.error?.why}
    >
        <ExperimentalGrantApplications opts={{embedded: true, omitBreadcrumbs: true, omitFilters: true}} />
    </HighlightedCard>;
};

function DashboardNews({news}: {news: APICallState<Page<NewsPost>>}): JSX.Element | null {
    return (
        <HighlightedCard
            title={<Link to="/news/list/"><Heading.h3>News</Heading.h3></Link>}
            color="orange"
            isLoading={news.loading}
            icon={"favIcon"}
            error={news.error?.why}
        >
            <WithGraphic>
                <div>
                    {news.data.items.length !== 0 ? null : (
                        <NoResultsCardBody title={"No news"}>
                            <Text>
                                As announcements are made, they will be shared here.
                            </Text>
                        </NoResultsCardBody>
                    )}
                    <Box>
                        {news.data.items.slice(0, 1).map(post => (
                            <Box key={post.id} mb={32}>
                                <Link to={AppRoutes.news.detailed(post.id)}>
                                    <Heading.h3>{post.title} </Heading.h3>
                                </Link>

                                <Spacer
                                    left={<Heading.h5>{post.subtitle}</Heading.h5>}
                                    right={<Heading.h5>{dateToString(post.showFrom)}</Heading.h5>}
                                />

                                <Box maxHeight={300} overflow={"auto"}>
                                    <Markdown unwrapDisallowed>
                                        {post.body}
                                    </Markdown>
                                </Box>
                            </Box>
                        ))}
                    </Box>

                    {news.data.items.length === 0 ? null : (
                        <Spacer
                            left={null}
                            right={<Link to="/news/list/">View more</Link>}
                        />)}
                </div>
            </WithGraphic>
        </HighlightedCard>
    );
}

const WithGraphicClass = injectStyle("with-graphic", k => `
    ${k} {
        display: flex;
    }

    ${k} > div {
        width: 50%;
    }

    ${k} > img {
         max-height: 250px;
         margin-left: auto;
         margin-right: auto;
    }

@media screen and (max-width: 900px) {
    ${k} > img {
        display: none;
        width: 0px;
    }

    ${k} > div {
        width: 100%;
    }
}
`);

function WithGraphic({children}: React.PropsWithChildren): JSX.Element {
    return <div className={WithGraphicClass}>
        {children}
        <img src={ucloudImage} />
    </div>
}

const mapDispatchToProps = (dispatch: Dispatch): DashboardOperations => ({
    onInit: () => dispatch(updatePageTitle("Dashboard")),
    setActiveProject: projectId => dispatchSetProjectAction(dispatch, projectId),
    setAllLoading: loading => dispatch(setAllLoading(loading)),
    setRefresh: refresh => dispatch(setRefreshFunction(refresh))
});

export default connect(null, mapDispatchToProps)(Dashboard);
