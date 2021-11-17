import * as React from "react";
import {useCallback, useEffect, useMemo, useState} from "react";
import {
    ProductSupport,
    Resource,
    ResourceApi,
    ResourceIncludeFlags,
    ResourceSpecification,
    ResourceStatus,
    ResourceUpdate
} from "@/UCloud/ResourceApi";
import {SidebarPages} from "@/ui-components/Sidebar";
import {Box, Button, Icon, List, TextArea} from "@/ui-components";
import {ItemRenderer, ItemRow} from "@/ui-components/Browse";
import * as Types from "@/Accounting";
import {
    deposit,
    explainPrice,
    normalizeBalanceForFrontend,
    priceExplainer,
    Product,
    productTypeToIcon, rootDeposit, TransferRecipient, WalletOwner
} from "@/Accounting";
import {ListRowStat} from "@/ui-components/List";
import {ResourceProperties} from "@/Resource/Properties";
import HighlightedCard from "@/ui-components/HighlightedCard";
import {doNothing} from "@/UtilityFunctions";
import * as Heading from "@/ui-components/Heading";
import {ListV2} from "@/Pagination";
import {NoResultsCardBody} from "@/Dashboard/Dashboard";
import {InvokeCommand, useCloudAPI, useCloudCommand} from "@/Authentication/DataHook";
import * as UCloud from "@/UCloud/index";
import {PageV2} from "@/UCloud/index";
import {bulkRequestOf, emptyPageV2} from "@/DefaultObjects";
import {ProductCreationForm} from "@/Admin/Providers/View";
import {BrowseType} from "@/Resource/BrowseType";
import {useToggleSet} from "@/Utilities/ToggleSet";
import {Operation, Operations} from "@/ui-components/Operation";
import {Client} from "@/Authentication/HttpClientInstance";

export interface ProviderSpecification extends ResourceSpecification {
    id: string;
    domain: string;
    https: boolean;
    port?: number | null;
}

export interface ProviderStatus extends ResourceStatus {
}

export interface ProviderSupport extends ProductSupport {
}

export interface ProviderUpdate extends ResourceUpdate {
}

export interface ProviderFlags extends ResourceIncludeFlags {
}

export interface Provider extends Resource<ProviderUpdate, ProviderStatus, ProviderSpecification> {
    refreshToken: string;
    publicKey: string;
}

interface ProductCallbacks {
    createProduct: () => void;
    isCreatingProduct: boolean;
    stopProductCreation: () => void;
    invokeCommand: InvokeCommand;
}

class ProviderApi extends ResourceApi<Provider, Product, ProviderSpecification, ProviderUpdate,
    ProviderFlags, ProviderStatus, ProviderSupport> {
    routingNamespace = "providers";
    title = "Provider";
    page = SidebarPages.Admin;
    productType = undefined;

    renderer: ItemRenderer<Provider> = {
        Icon({resource, size}) {
            return <Icon name={"cubeSolid"} size={size}/>
        },
        MainTitle({resource}) {
            return <>{resource?.specification?.id ?? ""}</>
        },
        Stats({resource}) {
            if (resource == null) return null;
            return <>
                <ListRowStat icon={"globeEuropeSolid"}>
                    {resource.specification.https ? "https://" : "http://"}
                    {resource.specification.domain}
                    {resource.specification.port == null ? null : `:${resource.specification.port}`}
                </ListRowStat>
            </>
        }
    };

    Properties = (props) => {
        return <ResourceProperties
            {...props} api={this}
            showMessages={false} showPermissions={true} showProperties={false}
            noPermissionsWarning={"Only administrators of this project can manage this provider. Note: the provider can still be used by normal users."}
            InfoChildren={props => {
                console.log(props);
                if (props.resource == null) return null;
                const provider = props.resource as Provider;
                return <>
                    <HighlightedCard color={"purple"} title={"Metadata"} icon={"mapMarkedAltSolid"}>
                        <Box mb={"8px"}>
                            <b>Host: </b>
                            {provider.specification.https ? "https://" : "http://"}
                            {provider.specification.domain}
                            {provider.specification.port == null ? null : `:${provider.specification.port}`}
                        </Box>
                        <Box mb={"8px"}>
                            <label htmlFor={"refresh"}><b>Refresh Token:</b></label>
                            <TextArea id={"refresh"} width="100%" value={provider.refreshToken} rows={1}
                                      onChange={doNothing}/>
                        </Box>
                        <Box mb={"8px"}>
                            <label htmlFor={"cert"}><b>Certificate: </b></label>
                            <TextArea id={"cert"} width="100%" value={provider.publicKey} rows={3}
                                      onChange={doNothing}/>
                        </Box>
                    </HighlightedCard>
                </>
            }}
            ContentChildren={props => {
                const provider = props.resource as Provider;
                const [products, fetchProducts] = useCloudAPI<PageV2<Types.Product>>({noop: true}, emptyPageV2);
                const [productGeneration, setProductGeneration] = useState(0);
                const [isCreatingProduct, setIsCreatingProduct] = useState(false);
                const startProductCreation = useCallback(() => {
                    setIsCreatingProduct(true);
                }, [setIsCreatingProduct]);
                const stopProductCreation = useCallback(() => {
                    setIsCreatingProduct(false);
                    fetchProducts(UCloud.accounting.products.browse({filterProvider: provider?.specification?.id}));
                    setProductGeneration(p => p + 1);
                }, [setIsCreatingProduct, provider?.specification?.id]);
                const toggleSet = useToggleSet(products.data.items);

                const loadMore = useCallback(() => {
                    fetchProducts(UCloud.accounting.products.browse({
                        filterProvider: provider?.specification?.id,
                        next: products.data.next
                    }));
                }, [products.data.next, provider?.specification?.id]);

                useEffect(() => {
                    fetchProducts(UCloud.accounting.products.browse({filterProvider: provider?.specification?.id}));
                }, [provider?.specification?.id]);

                const [commandLoading, invokeCommand] = useCloudCommand();

                const callbacks: ProductCallbacks = useMemo(() => ({
                    createProduct: startProductCreation,
                    isCreatingProduct,
                    stopProductCreation,
                    invokeCommand
                }), [isCreatingProduct, stopProductCreation, startProductCreation]);

                if (provider == null) return null;
                return <>
                    <HighlightedCard color={"purple"}>
                        <Operations
                            topbarIcon={"cubeSolid"}
                            location={"TOPBAR"}
                            operations={this.productOperations}
                            selected={toggleSet.checked.items}
                            extra={callbacks}
                            entityNameSingular={"Product"}
                        />

                        <List>
                            <ListV2
                                infiniteScrollGeneration={productGeneration}
                                page={products.data}
                                pageRenderer={p => isCreatingProduct ? null : p.map((item, idx) =>
                                    <ItemRow
                                        key={idx}
                                        item={item}
                                        browseType={BrowseType.Card}
                                        renderer={this.ProductRenderer}
                                        toggleSet={toggleSet}
                                        operations={this.productOperations}
                                        callbacks={callbacks}
                                        itemTitle={"Product"}
                                    />
                                )}
                                loading={products.loading}
                                customEmptyPage={
                                    isCreatingProduct ? <></> :
                                        <NoResultsCardBody title={"No products"}>
                                            <Button onClick={startProductCreation}>New product</Button>
                                        </NoResultsCardBody>
                                }
                                onLoadMore={loadMore}
                            />
                        </List>
                        {!isCreatingProduct ? null :
                            <ProductCreationForm provider={provider} onComplete={stopProductCreation}/>
                        }
                    </HighlightedCard>
                </>;
            }}
        />;
    };

    private ProductRenderer: ItemRenderer<Product, ProductCallbacks> = {
        Icon: ({resource, size}) =>
            <Icon name={resource == null ? "cubeSolid" : productTypeToIcon(resource.productType)}/>,
        MainTitle: ({resource}) => {
            if (resource == null) return null;
            return <>{resource.name} / {resource.category.name}</>;
        },
        Stats: ({resource}) => {
            if (resource == null) return null;
            return <>
                <ListRowStat icon={"grant"}>
                    {normalizeBalanceForFrontend(resource.pricePerUnit, resource.productType, resource.chargeType, resource.unitOfPrice, true)}
                    {explainPrice(resource.productType, resource.chargeType, resource.unitOfPrice)}
                </ListRowStat>
            </>;
        }
    };

    private productOperations: Operation<Product, ProductCallbacks>[] = [
        {
            text: "Create product",
            enabled: (selected, cb) => selected.length === 0 && !cb.isCreatingProduct,
            onClick: (selected, cb) => cb.createProduct(),
            primary: true
        },
        {
            text: "Go back",
            enabled: (selected, cb) => selected.length === 0 && cb.isCreatingProduct,
            onClick: (selected, cb) => cb.stopProductCreation(),
            primary: true,
            icon: "backward"
        },
        {
            text: "Grant credits",
            icon: "grant",
            enabled: (selected, cb) => selected.length > 0 && Client.userIsAdmin,
            onClick: (selected, cb) => {
                const recipient: WalletOwner = Client.projectId ?
                    {type: "project", projectId: Client.projectId} :
                    {type: "user", username: Client.username!};

                cb.invokeCommand(rootDeposit(bulkRequestOf(...selected.map(p => ({
                    amount: 1000000 * 10000,
                    categoryId: p.category,
                    description: "Root deposit",
                    recipient
                })))));
            }
        }
    ];

    constructor() {
        super("providers");
    }
}

export default new ProviderApi();
