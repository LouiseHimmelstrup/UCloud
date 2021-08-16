import * as React from "react";
import {
    ResolvedSupport,
    Resource,
    ResourceApi,
    ResourceBrowseCallbacks,
    SupportByProvider,
    UCLOUD_CORE
} from "UCloud/ResourceApi";
import {useCloudAPI, useCloudCommand} from "Authentication/DataHook";
import {accounting} from "UCloud";
import {PropsWithChildren, ReactElement, useCallback, useEffect, useMemo, useRef, useState} from "react";
import {bulkRequestOf} from "DefaultObjects";
import {useLoading, useTitle} from "Navigation/Redux/StatusActions";
import {useToggleSet} from "Utilities/ToggleSet";
import {useScrollStatus} from "Utilities/ScrollStatus";
import {PageRenderer} from "Pagination/PaginationV2";
import {Box, List} from "ui-components";
import {ListRowStat} from "ui-components/List";
import {Operation, Operations} from "ui-components/Operation";
import {dateToString} from "Utilities/DateUtilities";
import MainContainer from "MainContainer/MainContainer";
import {StickyBox} from "ui-components/StickyBox";
import Product = accounting.Product;
import {NamingField} from "UtilityComponents";
import {ProductSelector} from "Resource/ProductSelector";
import {doNothing, timestampUnixMs, useEffectSkipMount} from "UtilityFunctions";
import {Client} from "Authentication/HttpClientInstance";
import {useSidebarPage} from "ui-components/Sidebar";
import * as Heading from "ui-components/Heading";
import {useHistory, useLocation} from "react-router";
import {ResourceFilter} from "Resource/Filter";
import {useResourceSearch} from "Resource/Search";
import {getQueryParamOrElse} from "Utilities/URIUtilities";
import {useDispatch} from "react-redux";
import * as H from "history";
import {ItemRenderer, ItemRow, StandardBrowse, useRenamingState} from "ui-components/Browse";

export interface ResourceBrowseProps<Res extends Resource, CB> extends BaseResourceBrowseProps<Res> {
    api: ResourceApi<Res, never>;

    onInlineCreation?: (text: string, product: Product, cb: ResourceBrowseCallbacks<Res> & CB) => Res["specification"] | APICallParameters;
    inlinePrefix?: (productWithSupport: ResolvedSupport) => string;
    inlineSuffix?: (productWithSupport: ResolvedSupport) => string;
    inlineCreationMode?: "TEXT" | "NONE";
    inlineProduct?: Product;

    withDefaultStats?: boolean;
    additionalFilters?: Record<string, string>;
    header?: JSX.Element;
    headerSize?: number;
    onRename?: (text: string, resource: Res, cb: ResourceBrowseCallbacks<Res>) => Promise<void>;

    navigateToChildren?: (history: H.History, resource: Res) => void;
    emptyPage?: JSX.Element;
    propsForInlineResources?: Record<string, any>;
    extraCallbacks?: any;
}

export interface BaseResourceBrowseProps<Res extends Resource> {
    embedded?: boolean;
    isSearch?: boolean;

    onSelect?: (resource: Res) => void;
}

export const ResourceBrowse = <Res extends Resource, CB = undefined>(
    {
        onSelect, api, ...props
    }: PropsWithChildren<ResourceBrowseProps<Res, CB>>
): ReactElement | null => {
    const [productsWithSupport, fetchProductsWithSupport] = useCloudAPI<SupportByProvider>({noop: true},
        {productsByProvider: {}})
    const includeOthers = !props.embedded;
    const [selectedProduct, setSelectedProduct] = useState<Product | null>(props.inlineProduct ?? null);
    const [renamingValue, setRenamingValue] = useState("");
    const [commandLoading, invokeCommand] = useCloudCommand();
    const [filters, setFilters] = useState<Record<string, string>>({});
    const [sortDirection, setSortDirection] = useState<"ascending" | "descending">("ascending");
    const [sortColumn, setSortColumn] = useState<string | undefined>(undefined);
    const history = useHistory();
    const location = useLocation();
    const query = getQueryParamOrElse(location.search, "q", "");

    const reloadRef = useRef<() => void>(doNothing);
    const toggleSet = useToggleSet<Res>([]);
    const scrollingContainerRef = useRef<HTMLDivElement>(null);
    const scrollStatus = useScrollStatus(scrollingContainerRef, true);
    const [isCreating, setIsCreating] = useState(false);
    const dispatch = useDispatch();

    useEffect(() => toggleSet.uncheckAll(), [props.additionalFilters]);

    const [inlineInspecting, setInlineInspecting] = useState<Res | null>(null);
    const closeProperties = useCallback(() => setInlineInspecting(null), [setInlineInspecting]);
    useEffect(() => fetchProductsWithSupport(api.retrieveProducts()), []);
    const renaming = useRenamingState<Res>(
        () => renamingValue, [renamingValue],
        (a, b) => a.id === b.id, [],

        async (item, text) => {
            await props.onRename?.(text, item, callbacks);
            callbacks.reload();
        },
        [props.onRename]
    );

    const products: Product[] = useMemo(() => {
        const allProducts: Product[] = [];
        for (const provider of Object.keys(productsWithSupport.data.productsByProvider)) {
            for (const productWithSupport of productsWithSupport.data.productsByProvider[provider]) {
                allProducts.push(productWithSupport.product);
            }
        }
        return allProducts;
    }, [productsWithSupport]);

    const selectedProductWithSupport: ResolvedSupport | null = useMemo(() => {
        if (selectedProduct) {
            return productsWithSupport.data.productsByProvider[selectedProduct.category.provider]
                ?.find(it => it.product.id === selectedProduct.id &&
                    it.product.category.id === selectedProduct.category.id) ?? null;
        }
        return null;
    }, [selectedProduct, productsWithSupport]);

    const generateFetch = useCallback((next?: string): APICallParameters => {
        if (props.isSearch) {
            return api.search({
                itemsPerPage: 50, flags: {includeOthers, ...filters}, query,
                next, sortDirection, sortBy: sortColumn, ...props.additionalFilters
            });
        } else {
            return api.browse({
                next, itemsPerPage: 50, includeOthers,
                ...filters, sortBy: sortColumn, sortDirection, ...props.additionalFilters
            });
        }
    }, [filters, query, props.isSearch, sortColumn, sortDirection, props.additionalFilters]);

    useEffectSkipMount(() => {
        setSelectedProduct(props.inlineProduct ?? null);
    }, [props.inlineProduct]);

    const viewProperties = useCallback((res: Res) => {
        if (props.embedded) {
            setInlineInspecting(res);
        } else {
            history.push(`/${api.routingNamespace}/properties/${encodeURIComponent(res.id)}`);
        }
    }, [setInlineInspecting, props.embedded, history, api]);

    const callbacks: ResourceBrowseCallbacks<Res> & CB = useMemo(() => ({
        api,
        isCreating,
        invokeCommand,
        commandLoading,
        reload: () => reloadRef.current(),
        embedded: props.embedded == true,
        onSelect,
        dispatch,
        history,
        startRenaming: (res, value) => {
            renaming.setRenaming(res);
            setRenamingValue(value);
        },
        startCreation: () => {
            if (props.onInlineCreation != null) {
                setSelectedProduct(props.inlineProduct ?? null);
                setIsCreating(true);
            }
        },
        viewProperties,
        ...props.extraCallbacks
    }), [api, invokeCommand, commandLoading, reloadRef, isCreating, props.onInlineCreation, history, dispatch,
        viewProperties, props.inlineProduct, props.extraCallbacks]);

    const onProductSelected = useCallback(async (product: Product) => {
        if (props.inlineCreationMode !== "NONE") {
            setSelectedProduct(product);
        } else {
            if (!props.onInlineCreation) return;
            const spec = props.onInlineCreation("", product, callbacks);
            setIsCreating(false);
            if ("path" in spec && "method" in spec) {
                await callbacks.invokeCommand(spec);
            } else {
                await callbacks.invokeCommand(api.create(bulkRequestOf(spec as Res["specification"])));
            }
            callbacks.reload();
        }
    }, [setSelectedProduct, props.inlineCreationMode, props.onInlineCreation, callbacks]);

    const inlineInputRef = useRef<HTMLInputElement>(null);
    const onInlineCreate = useCallback(async () => {
        if (inlineInputRef.current && props.onInlineCreation) {
            const prefix = props?.inlinePrefix?.(selectedProductWithSupport!) ?? "";
            const suffix = props?.inlineSuffix?.(selectedProductWithSupport!) ?? "";
            const spec = props.onInlineCreation(
                prefix + inlineInputRef.current.value + suffix,
                selectedProduct!,
                callbacks
            );

            if ("path" in spec && "method" in spec) {
                await callbacks.invokeCommand(spec);
            } else {
                await callbacks.invokeCommand(api.create(bulkRequestOf(spec as Res["specification"])));
            }
            callbacks.reload();
        }
        setIsCreating(false);
    }, [props.onInlineCreation, inlineInputRef, callbacks, setIsCreating, selectedProduct]);

    const operations: Operation<Res, ResourceBrowseCallbacks<Res>>[] = useMemo(() => {
        return api.retrieveOperations();
    }, [callbacks, api]);

    const onSortUpdated = useCallback((dir, column) => {
        setSortColumn(column);
        setSortDirection(dir)
    }, []);

    const modifiedRenderer = useMemo((): ItemRenderer<Res> => {
        const renderer: ItemRenderer<Res> = {...api.renderer};
        const RemainingStats = renderer.Stats;
        const NormalMainTitle = renderer.MainTitle;
        renderer.MainTitle = ({resource}) => {
            if (resource === undefined) {
                return !selectedProduct ?
                    <ProductSelector products={products} onProductSelected={onProductSelected} />
                    :
                    <NamingField
                        confirmText={"Create"}
                        onCancel={() => setIsCreating(false)}
                        onSubmit={onInlineCreate}
                        inputRef={inlineInputRef}
                        prefix={props.inlinePrefix && selectedProductWithSupport ?
                            props.inlinePrefix(selectedProductWithSupport) : null}
                        suffix={props.inlineSuffix && selectedProductWithSupport ?
                            props.inlineSuffix(selectedProductWithSupport) : null}
                    />;
            } else {
                return NormalMainTitle ? <NormalMainTitle resource={resource} /> : null;
            }
        };
        renderer.Stats = props.withDefaultStats !== false ? ({resource}) => (<>
            {!resource ? <>
                <ListRowStat icon={"calendar"}>{dateToString(timestampUnixMs())}</ListRowStat>
                <ListRowStat icon={"user"}>{Client.username}</ListRowStat>
                {!selectedProduct ? null : <>
                    <ListRowStat icon={"cubeSolid"}>{selectedProduct.id} / {selectedProduct.category.id}</ListRowStat>
                </>}
            </> : <>
                <ListRowStat icon={"calendar"}>{dateToString(resource.createdAt)}</ListRowStat>
                <ListRowStat icon={"user"}>{resource.owner.createdBy}</ListRowStat>
                {resource.specification.product.provider === UCLOUD_CORE ? null :
                    <ListRowStat icon={"cubeSolid"}>
                        {resource.specification.product.id} / {resource.specification.product.category}
                    </ListRowStat>
                }
            </>}
            {RemainingStats ? <RemainingStats resource={resource} /> : null}
        </>) : renderer.Stats;
        return renderer;
    }, [api, props.withDefaultStats, props.inlinePrefix, props.inlineSuffix, products, onProductSelected,
        onInlineCreate, inlineInputRef, selectedProductWithSupport]);

    const pageRenderer = useCallback<PageRenderer<Res>>(items => {
        return <List childPadding={"8px"} bordered={false}>
            {!isCreating ? null :
                <ItemRow
                    renderer={modifiedRenderer as ItemRenderer<unknown>}
                    itemTitle={api.title} itemTitlePlural={api.titlePlural} toggleSet={toggleSet}
                    operations={operations} callbacks={callbacks}
                />
            }
            {items.length > 0 || isCreating ? null : props.emptyPage ? props.emptyPage :
                <>
                    No {api.titlePlural.toLowerCase()} available. Click &quot;Create {api.title.toLowerCase()}&quot;
                    to create a new one.
                </>
            }
            {items.map(it =>
                <ItemRow
                    key={it.id}
                    navigate={() => {
                        if (props.navigateToChildren) {
                            props.navigateToChildren?.(history, it)
                        } else {
                            viewProperties(it);
                        }
                    }}
                    renderer={modifiedRenderer as ItemRenderer<unknown>} callbacks={callbacks} operations={operations}
                    item={it} itemTitle={api.title} itemTitlePlural={api.titlePlural} toggleSet={toggleSet}
                    renaming={renaming}
                />
            )}
        </List>
    }, [toggleSet, isCreating, selectedProduct, props.withDefaultStats, selectedProductWithSupport, renaming,
        viewProperties]);

    if (!props.embedded) {
        useTitle(api.titlePlural);
        useLoading(commandLoading);
        useSidebarPage(api.page);
        useResourceSearch(api);
    }

    const main = !inlineInspecting ? <>
        <StandardBrowse generateCall={generateFetch} pageRenderer={pageRenderer} reloadRef={reloadRef}
            setRefreshFunction={props.embedded != true} />
    </> : <>
        <api.Properties api={api} resource={inlineInspecting} reload={reloadRef.current} embedded={true}
            closeProperties={closeProperties} {...props.propsForInlineResources} />
    </>;

    if (props.embedded) {
        return <Box ref={scrollingContainerRef}>
            <StickyBox shadow={!scrollStatus.isAtTheTop} normalMarginX={"20px"}>
                {inlineInspecting ?
                    <Heading.h3 flexGrow={1}>{api.titlePlural}</Heading.h3> :
                    <>
                        <Operations selected={toggleSet.checked.items} location={"TOPBAR"}
                            entityNameSingular={api.title} entityNamePlural={api.titlePlural}
                            extra={callbacks} operations={operations} />
                        {props.header}
                        <ResourceFilter
                            embedded
                            pills={api.filterPills} filterWidgets={api.filterWidgets}
                            sortEntries={api.sortEntries} sortDirection={sortDirection}
                            onSortUpdated={onSortUpdated} properties={filters} setProperties={setFilters}
                            onApplyFilters={reloadRef.current} />
                    </>
                }
            </StickyBox>
            {main}
        </Box>;
    } else {
        return <MainContainer
            header={props.header}
            headerSize={props.headerSize}
            main={main}
            sidebar={
                inlineInspecting ? null :
                    <>
                        <Operations selected={toggleSet.checked.items} location={"SIDEBAR"}
                            entityNameSingular={api.title} entityNamePlural={api.titlePlural}
                            extra={callbacks} operations={operations} />

                        <ResourceFilter pills={api.filterPills} filterWidgets={api.filterWidgets}
                            sortEntries={api.sortEntries} sortDirection={sortDirection}
                            onSortUpdated={onSortUpdated} properties={filters} setProperties={setFilters}
                            onApplyFilters={reloadRef.current} />
                    </>
            }
        />
    }
};
